package com.netmuzzle.firewall.service.dns

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.system.OsConstants
import android.util.Log
import android.util.LruCache
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class DnsPacketHandler(
    private val vpnService: VpnService,
    private val context: Context,
    private val vpnInterfaceFd: FileDescriptor,
    @Volatile private var blockedDomains: Set<String>,
    @Volatile private var fullBlockedPackages: Set<String>
) {

    companion object {
        private const val TAG = "DnsPacketHandler"
        private const val BUFFER_SIZE = 4096
        private const val DNS_PORT = 53
        private const val UPSTREAM_DNS_IPV4 = "1.1.1.1"
        private const val UPSTREAM_DNS_BACKUP = "8.8.8.8"
        private const val DNS_TIMEOUT_MS = 2500
    }

    private val isRunning = AtomicBoolean(true)
    private val responseCache = LruCache<String, ByteArray>(256)
    private val packageManager: PackageManager = context.packageManager
    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    fun updateRules(newBlockedDomains: Set<String>, newFullBlockedPackages: Set<String>) {
        this.blockedDomains = newBlockedDomains
        this.fullBlockedPackages = newFullBlockedPackages
        responseCache.evictAll()
    }

    fun stop() {
        isRunning.set(false)
    }

    fun runLoop() {
        val inputStream = FileInputStream(vpnInterfaceFd)
        val outputStream = FileOutputStream(vpnInterfaceFd)
        val buffer = ByteArray(BUFFER_SIZE)

        // Gniazdo UDP chronione przed wpadnięciem do własnego tunelu VPN (protect)
        val forwardSocket = DatagramSocket().apply {
            vpnService.protect(this)
            soTimeout = DNS_TIMEOUT_MS
        }
        val upstreamAddress = InetAddress.getByName(UPSTREAM_DNS_IPV4)

        try {
            while (isRunning.get()) {
                val bytesRead = try {
                    inputStream.read(buffer)
                } catch (e: Exception) {
                    if (!isRunning.get()) break
                    Log.w(TAG, "Błąd odczytu z tunelu VPN", e)
                    break
                }

                if (bytesRead <= 0) continue

                try {
                    processIpPacket(buffer, bytesRead, outputStream, forwardSocket, upstreamAddress)
                } catch (e: Exception) {
                    Log.w(TAG, "Błąd przetwarzania pakietu DNS", e)
                }
            }
        } finally {
            try {
                forwardSocket.close()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun processIpPacket(
        packet: ByteArray,
        length: Int,
        outputStream: FileOutputStream,
        forwardSocket: DatagramSocket,
        upstreamAddress: InetAddress
    ) {
        if (length < 20) return
        val version = (packet[0].toInt() shr 4) and 0x0F

        if (version == 4) {
            processIpv4Packet(packet, length, outputStream, forwardSocket, upstreamAddress)
        } else if (version == 6) {
            processIpv6Packet(packet, length, outputStream, forwardSocket, upstreamAddress)
        }
    }

    private fun processIpv4Packet(
        packet: ByteArray,
        length: Int,
        outputStream: FileOutputStream,
        forwardSocket: DatagramSocket,
        upstreamAddress: InetAddress
    ) {
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (length < ihl + 8) return

        val protocol = packet[9].toInt() and 0xFF
        if (protocol != 17) return // Tylko UDP

        val srcPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
        val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
        if (dstPort != DNS_PORT) return

        val udpLen = ((packet[ihl + 4].toInt() and 0xFF) shl 8) or (packet[ihl + 5].toInt() and 0xFF)
        val dnsOffset = ihl + 8
        val dnsLength = minOf(udpLen - 8, length - dnsOffset)
        if (dnsLength < 12) return

        val dnsPayload = packet.copyOfRange(dnsOffset, dnsOffset + dnsLength)
        val domain = extractDomain(dnsPayload) ?: return
        val qType = extractQType(dnsPayload)

        // Sprawdzenie czy pakiet pochodzi z aplikacji z pełną blokadą (Blackhole)
        val isSenderFullBlocked = isSenderInFullBlock(packet, ihl, srcPort, dstPort)
        val isDomainBlocked = isDomainBlocked(domain)

        if (isSenderFullBlocked || isDomainBlocked) {
            // Blokada: natychmiastowa odpowiedź 0.0.0.0 (lub ::)
            val dnsResponse = buildBlockedDnsResponse(dnsPayload, qType)
            val responsePacket = wrapInIpv4Udp(
                dnsPayload = dnsResponse,
                srcIp = packet.copyOfRange(16, 20), // zamiana dest na src
                dstIp = packet.copyOfRange(12, 16), // zamiana src na dest
                srcPort = dstPort,
                dstPort = srcPort
            )
            synchronized(outputStream) {
                outputStream.write(responsePacket)
            }
        } else {
            // Dozwolone: przesyłamy do prawdziwego DNS (1.1.1.1)
            val cachedResponse = responseCache.get(domain + "_" + qType)
            val responseDnsPayload: ByteArray? = if (cachedResponse != null) {
                // Podmień Transaction ID na ID zapytania
                cachedResponse.copyOf().also {
                    it[0] = dnsPayload[0]
                    it[1] = dnsPayload[1]
                }
            } else {
                forwardDnsQuery(dnsPayload, forwardSocket, upstreamAddress)?.also { resp ->
                    responseCache.put(domain + "_" + qType, resp)
                }
            }

            if (responseDnsPayload != null) {
                val responsePacket = wrapInIpv4Udp(
                    dnsPayload = responseDnsPayload,
                    srcIp = packet.copyOfRange(16, 20),
                    dstIp = packet.copyOfRange(12, 16),
                    srcPort = dstPort,
                    dstPort = srcPort
                )
                synchronized(outputStream) {
                    outputStream.write(responsePacket)
                }
            }
        }
    }

    private fun processIpv6Packet(
        packet: ByteArray,
        length: Int,
        outputStream: FileOutputStream,
        forwardSocket: DatagramSocket,
        upstreamAddress: InetAddress
    ) {
        if (length < 48) return // 40 bytes IPv6 header + 8 bytes UDP
        val nextHeader = packet[6].toInt() and 0xFF
        if (nextHeader != 17) return // Tylko UDP

        val srcPort = ((packet[40].toInt() and 0xFF) shl 8) or (packet[41].toInt() and 0xFF)
        val dstPort = ((packet[42].toInt() and 0xFF) shl 8) or (packet[43].toInt() and 0xFF)
        if (dstPort != DNS_PORT) return

        val udpLen = ((packet[44].toInt() and 0xFF) shl 8) or (packet[45].toInt() and 0xFF)
        val dnsOffset = 48
        val dnsLength = minOf(udpLen - 8, length - dnsOffset)
        if (dnsLength < 12) return

        val dnsPayload = packet.copyOfRange(dnsOffset, dnsOffset + dnsLength)
        val domain = extractDomain(dnsPayload) ?: return
        val qType = extractQType(dnsPayload)

        val isDomainBlocked = isDomainBlocked(domain)
        if (isDomainBlocked) {
            val dnsResponse = buildBlockedDnsResponse(dnsPayload, qType)
            val responsePacket = wrapInIpv6Udp(
                dnsPayload = dnsResponse,
                srcIp = packet.copyOfRange(24, 40),
                dstIp = packet.copyOfRange(8, 24),
                srcPort = dstPort,
                dstPort = srcPort
            )
            synchronized(outputStream) {
                outputStream.write(responsePacket)
            }
        } else {
            val responseDnsPayload = forwardDnsQuery(dnsPayload, forwardSocket, upstreamAddress)
            if (responseDnsPayload != null) {
                val responsePacket = wrapInIpv6Udp(
                    dnsPayload = responseDnsPayload,
                    srcIp = packet.copyOfRange(24, 40),
                    dstIp = packet.copyOfRange(8, 24),
                    srcPort = dstPort,
                    dstPort = srcPort
                )
                synchronized(outputStream) {
                    outputStream.write(responsePacket)
                }
            }
        }
    }

    private fun isSenderInFullBlock(packet: ByteArray, ihl: Int, srcPort: Int, dstPort: Int): Boolean {
        if (fullBlockedPackages.isEmpty()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && connectivityManager != null) {
            try {
                val srcIp = InetAddress.getByAddress(packet.copyOfRange(12, 16))
                val dstIp = InetAddress.getByAddress(packet.copyOfRange(16, 20))
                val localAddress = InetSocketAddress(srcIp, srcPort)
                val remoteAddress = InetSocketAddress(dstIp, dstPort)

                val uid = connectivityManager.getConnectionOwnerUid(
                    OsConstants.IPPROTO_UDP,
                    localAddress,
                    remoteAddress
                )
                if (uid > 0) {
                    val packages = packageManager.getPackagesForUid(uid)
                    if (packages != null) {
                        for (pkg in packages) {
                            if (fullBlockedPackages.contains(pkg)) {
                                return true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignorujemy błędy pobierania UID
            }
        }
        return false
    }

    private fun isDomainBlocked(domain: String): Boolean {
        val lower = domain.lowercase()
        if (blockedDomains.contains(lower)) return true

        // Sprawdzamy subdomeny (np. ads.unity.com -> pasuje do unity.com)
        var dotIndex = lower.indexOf('.')
        while (dotIndex != -1 && dotIndex < lower.length - 1) {
            val parentDomain = lower.substring(dotIndex + 1)
            if (blockedDomains.contains(parentDomain)) {
                return true
            }
            dotIndex = lower.indexOf('.', dotIndex + 1)
        }
        return false
    }

    private fun extractDomain(dnsPayload: ByteArray): String? {
        if (dnsPayload.size < 13) return null
        var offset = 12
        val sb = StringBuilder()

        while (offset < dnsPayload.size) {
            val labelLen = dnsPayload[offset].toInt() and 0xFF
            if (labelLen == 0) break // Koniec nazwy domeny
            if (labelLen > 63 || offset + 1 + labelLen > dnsPayload.size) return null

            if (sb.isNotEmpty()) sb.append('.')
            val label = String(dnsPayload, offset + 1, labelLen, Charsets.US_ASCII)
            sb.append(label)
            offset += 1 + labelLen
        }

        return if (sb.isNotEmpty()) sb.toString() else null
    }

    private fun extractQType(dnsPayload: ByteArray): Int {
        var offset = 12
        while (offset < dnsPayload.size) {
            val labelLen = dnsPayload[offset].toInt() and 0xFF
            if (labelLen == 0) {
                offset += 1
                break
            }
            offset += 1 + labelLen
        }
        if (offset + 2 <= dnsPayload.size) {
            return ((dnsPayload[offset].toInt() and 0xFF) shl 8) or (dnsPayload[offset + 1].toInt() and 0xFF)
        }
        return 1 // Domyślnie A
    }

    private fun buildBlockedDnsResponse(queryPayload: ByteArray, qType: Int): ByteArray {
        val idHigh = queryPayload[0]
        val idLow = queryPayload[1]

        // Znajdź koniec sekcji Question
        var questionEnd = 12
        while (questionEnd < queryPayload.size) {
            val len = queryPayload[questionEnd].toInt() and 0xFF
            if (len == 0) {
                questionEnd += 5 // 1 bajt (0x00) + 2 bajty QTYPE + 2 bajty QCLASS
                break
            }
            questionEnd += 1 + len
        }

        val questionBytes = queryPayload.copyOfRange(12, minOf(questionEnd, queryPayload.size))
        val isAaaa = qType == 28
        val rDataSize = if (isAaaa) 16 else 4

        // Długość odpowiedzi: 12 bajtów nagłówka + Question + Answer (12 + rDataSize)
        val answerRecordSize = 2 + 2 + 2 + 4 + 2 + rDataSize
        val response = ByteArray(12 + questionBytes.size + answerRecordSize)
        val bb = ByteBuffer.wrap(response)

        // DNS Header
        bb.put(idHigh)
        bb.put(idLow)
        bb.putShort(0x8180.toShort()) // Flags: Standard response, No error
        bb.putShort(1.toShort())      // QDCOUNT: 1
        bb.putShort(1.toShort())      // ANCOUNT: 1
        bb.putShort(0.toShort())      // NSCOUNT: 0
        bb.putShort(0.toShort())      // ARCOUNT: 0

        // Question section (echoed)
        bb.put(questionBytes)

        // Answer Record:
        bb.put(0xC0.toByte())
        bb.put(0x0C.toByte()) // Wskaźnik kompresji na początek QNAME (offset 12)
        bb.putShort(qType.toShort()) // TYPE
        bb.putShort(1.toShort())     // CLASS IN
        bb.putInt(300)               // TTL (5 minut)
        bb.putShort(rDataSize.toShort()) // RDLENGTH
        // RDATA: same zera (0.0.0.0 lub ::)
        repeat(rDataSize) {
            bb.put(0.toByte())
        }

        return response
    }

    private fun forwardDnsQuery(
        queryPayload: ByteArray,
        forwardSocket: DatagramSocket,
        upstreamAddress: InetAddress
    ): ByteArray? {
        return try {
            val sendPacket = DatagramPacket(queryPayload, queryPayload.size, upstreamAddress, DNS_PORT)
            forwardSocket.send(sendPacket)

            val recvBuffer = ByteArray(2048)
            val recvPacket = DatagramPacket(recvBuffer, recvBuffer.size)
            forwardSocket.receive(recvPacket)

            recvBuffer.copyOfRange(0, recvPacket.length)
        } catch (e: Exception) {
            null
        }
    }

    private fun wrapInIpv4Udp(
        dnsPayload: ByteArray,
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int
    ): ByteArray {
        val udpLength = 8 + dnsPayload.size
        val totalLength = 20 + udpLength
        val packet = ByteArray(totalLength)
        val bb = ByteBuffer.wrap(packet)

        // IPv4 Header (20 bajtów)
        bb.put(0x45.toByte()) // Version 4, IHL 5
        bb.put(0x00.toByte()) // DSCP/ECN
        bb.putShort(totalLength.toShort())
        bb.putShort(0.toShort()) // ID
        bb.putShort(0x4000.toShort()) // Flags: Don't Fragment
        bb.put(64.toByte()) // TTL
        bb.put(17.toByte()) // Protocol: UDP
        bb.putShort(0.toShort()) // Checksum (obliczymy niżej)
        bb.put(srcIp)
        bb.put(dstIp)

        // Obliczenie sumy kontrolnej IP
        val ipChecksum = computeIpChecksum(packet, 0, 20)
        packet[10] = ((ipChecksum shr 8) and 0xFF).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()

        // UDP Header (8 bajtów)
        bb.position(20)
        bb.putShort(srcPort.toShort())
        bb.putShort(dstPort.toShort())
        bb.putShort(udpLength.toShort())
        bb.putShort(0.toShort()) // Suma kontrolna UDP (opcjonalna w IPv4)

        // DNS Payload
        bb.put(dnsPayload)

        return packet
    }

    private fun wrapInIpv6Udp(
        dnsPayload: ByteArray,
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int
    ): ByteArray {
        val udpLength = 8 + dnsPayload.size
        val totalLength = 40 + udpLength
        val packet = ByteArray(totalLength)
        val bb = ByteBuffer.wrap(packet)

        // IPv6 Header (40 bajtów)
        bb.putInt(0x60000000) // Version 6, Traffic Class 0, Flow Label 0
        bb.putShort(udpLength.toShort()) // Payload length
        bb.put(17.toByte()) // Next Header: UDP
        bb.put(64.toByte()) // Hop Limit
        bb.put(srcIp) // 16 bajtów
        bb.put(dstIp) // 16 bajtów

        // UDP Header (8 bajtów)
        bb.putShort(srcPort.toShort())
        bb.putShort(dstPort.toShort())
        bb.putShort(udpLength.toShort())
        bb.putShort(0.toShort())

        // DNS Payload
        bb.put(dnsPayload)

        return packet
    }

    private fun computeIpChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length) {
            val high = data[i].toInt() and 0xFF
            val low = data[i + 1].toInt() and 0xFF
            sum += (high shl 8) or low
            i += 2
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }
}
