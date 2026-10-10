package com.netmuzzle.firewall.service.dns

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.system.OsConstants
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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
        private const val DNS_TIMEOUT_MS = 2000
        private const val DEFAULT_CACHE_TTL_MS = 60_000L // 60 sekund TTL dla pamięci podręcznej DNS
    }

    private data class CacheEntry(val data: ByteArray, val timestamp: Long, val ttlMs: Long)

    private val isRunning = AtomicBoolean(true)
    private val handlerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val responseCache = LruCache<String, CacheEntry>(256)
    private val uidPackageCache = LruCache<Int, List<String>>(128)

    private val packageManager: PackageManager = context.packageManager
    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    fun updateRules(newBlockedDomains: Set<String>, newFullBlockedPackages: Set<String>) {
        this.blockedDomains = newBlockedDomains
        this.fullBlockedPackages = newFullBlockedPackages
        clearCache()
    }

    fun clearCache() {
        responseCache.evictAll()
        uidPackageCache.evictAll()
    }

    fun stop() {
        isRunning.set(false)
        handlerScope.cancel()
    }

    fun runLoop() {
        val inputStream = FileInputStream(vpnInterfaceFd)
        val outputStream = FileOutputStream(vpnInterfaceFd)
        val buffer = ByteArray(BUFFER_SIZE)

        try {
            while (isRunning.get()) {
                val bytesRead = try {
                    inputStream.read(buffer)
                } catch (e: Exception) {
                    if (!isRunning.get()) break
                    Log.w(TAG, "Błąd odczytu z tunelu VPN", e)
                    break
                }

                // KRYTYCZNE ZABEZPIECZENIE: bytesRead < 0 oznacza koniec strumienia (EOF).
                // Przerwij pętlę natychmiast, aby zapobiec zapętleniu 100% CPU (busy-wait).
                if (bytesRead < 0) {
                    Log.i(TAG, "Koniec strumienia tunelu VPN (EOF), kończenie pętli.")
                    break
                }

                if (bytesRead == 0) {
                    continue
                }

                try {
                    processIpPacket(buffer, bytesRead, outputStream)
                } catch (e: Exception) {
                    Log.w(TAG, "Błąd przetwarzania pakietu DNS", e)
                }
            }
        } finally {
            handlerScope.cancel()
            try {
                inputStream.close()
            } catch (_: Exception) {}
            try {
                outputStream.close()
            } catch (_: Exception) {}
        }
    }

    private fun getFromResponseCache(key: String): ByteArray? {
        val entry = responseCache.get(key) ?: return null
        if (System.currentTimeMillis() - entry.timestamp > entry.ttlMs) {
            responseCache.remove(key)
            return null
        }
        return entry.data
    }

    private fun putInResponseCache(key: String, data: ByteArray, ttlMs: Long = DEFAULT_CACHE_TTL_MS) {
        responseCache.put(key, CacheEntry(data, System.currentTimeMillis(), ttlMs))
    }

    private fun processIpPacket(
        packet: ByteArray,
        length: Int,
        outputStream: FileOutputStream
    ) {
        if (length < 20) return
        val version = (packet[0].toInt() shr 4) and 0x0F

        if (version == 4) {
            processIpv4Packet(packet, length, outputStream)
        } else if (version == 6) {
            processIpv6Packet(packet, length, outputStream)
        }
    }

    private fun processIpv4Packet(
        packet: ByteArray,
        length: Int,
        outputStream: FileOutputStream
    ) {
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (length < ihl) return

        val protocol = packet[9].toInt() and 0xFF

        // Natychmiastowe odrzucenie połączeń TCP w tunelu za pomocą TCP RST (np. próba obejścia przez DoH na 1.1.1.1:443)
        // Zapobiega to 30-sekundowemu zawieszaniu się aplikacji i drenowaniu baterii w oczekiwaniu na timeout TCP.
        if (protocol == 6) {
            handleTcpIpv4Packet(packet, length, ihl, outputStream)
            return
        }

        if (protocol != 17) return // Tylko UDP

        if (length < ihl + 8) return

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

        // Sprawdzenie nadawcy i blokad (zoptymalizowane z pamięcią podręczną UID)
        val senderPackages = getSenderPackages(packet, srcPort, dstPort, isIpv6 = false)
        val senderPackage = senderPackages?.firstOrNull()
        FloatingWidgetManager.registerGameQuery(senderPackage)

        val isSenderFullBlocked = if (senderPackages != null && fullBlockedPackages.isNotEmpty()) {
            senderPackages.any { fullBlockedPackages.contains(it) }
        } else {
            false
        }
        val isDomainBlocked = isDomainBlocked(domain)
        val isPausedForSender = FloatingWidgetManager.isAdBlockPausedFor(senderPackage)

        // Raportowanie do Inspektora Ruchu jeśli aktywny
        if (TrafficInspectorManager.isSniffing.value) {
            TrafficInspectorManager.onDomainQueried(domain, senderPackage, isDomainBlocked && !isPausedForSender)
        }

        if (isSenderFullBlocked || (isDomainBlocked && !isPausedForSender)) {
            // Blokada: natychmiastowa odpowiedź 0.0.0.0 (lub :: dla AAAA, NODATA dla innych)
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
            val cacheKey = "${domain}_$qType"
            val cachedResponse = getFromResponseCache(cacheKey)

            if (cachedResponse != null) {
                // Trafienie w pamięć podręczną: natychmiastowa odpowiedź z podmienionym Transaction ID
                val responseDnsPayload = cachedResponse.copyOf().also {
                    it[0] = dnsPayload[0]
                    it[1] = dnsPayload[1]
                }
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
            } else {
                // Pudło w pamięci podręcznej: asynchroniczne wysłanie do 1.1.1.1 bez blokowania pętli TUN!
                val srcIpCopy = packet.copyOfRange(16, 20)
                val dstIpCopy = packet.copyOfRange(12, 16)

                handlerScope.launch(Dispatchers.IO) {
                    val responseDnsPayload = forwardDnsQuery(dnsPayload)
                    if (responseDnsPayload != null) {
                        putInResponseCache(cacheKey, responseDnsPayload)
                        val responsePacket = wrapInIpv4Udp(
                            dnsPayload = responseDnsPayload,
                            srcIp = srcIpCopy,
                            dstIp = dstIpCopy,
                            srcPort = dstPort,
                            dstPort = srcPort
                        )
                        try {
                            synchronized(outputStream) {
                                outputStream.write(responsePacket)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Błąd zapisu odpowiedzi DNS do tunelu", e)
                        }
                    }
                }
            }
        }
    }

    private fun handleTcpIpv4Packet(
        packet: ByteArray,
        length: Int,
        ihl: Int,
        outputStream: FileOutputStream
    ) {
        if (length < ihl + 20) return // TCP header minimum 20 bajtów

        val srcPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
        val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
        val flags = packet[ihl + 13].toInt() and 0xFF

        // Jeśli to już jest pakiet RST, nie odpowiadamy RST na RST
        if ((flags and 0x04) != 0) return

        val seqNum = ((packet[ihl + 4].toLong() and 0xFF) shl 24) or
                ((packet[ihl + 5].toLong() and 0xFF) shl 16) or
                ((packet[ihl + 6].toLong() and 0xFF) shl 8) or
                (packet[ihl + 7].toLong() and 0xFF)

        val isSyn = (flags and 0x02) != 0
        val ackNum = if (isSyn) seqNum + 1 else seqNum

        val srcIp = packet.copyOfRange(16, 20) // dest staje się src
        val dstIp = packet.copyOfRange(12, 16) // src staje się dest

        val totalLength = 40 // 20 bajtów IP + 20 bajtów TCP
        val rstPacket = ByteArray(totalLength)
        val bb = ByteBuffer.wrap(rstPacket)

        // IP Header (20 bajtów)
        bb.put(0x45.toByte())
        bb.put(0x00.toByte())
        bb.putShort(totalLength.toShort())
        bb.putShort(0.toShort())
        bb.putShort(0x4000.toShort()) // Don't Fragment
        bb.put(64.toByte()) // TTL
        bb.put(6.toByte()) // Protocol: TCP
        bb.putShort(0.toShort()) // Checksum placeholder
        bb.put(srcIp)
        bb.put(dstIp)

        val ipChecksum = computeIpChecksum(rstPacket, 0, 20)
        rstPacket[10] = ((ipChecksum shr 8) and 0xFF).toByte()
        rstPacket[11] = (ipChecksum and 0xFF).toByte()

        // TCP Header (20 bajtów, offset 20)
        bb.position(20)
        bb.putShort(dstPort.toShort()) // zamiana portów
        bb.putShort(srcPort.toShort())
        bb.putInt(0) // Sequence number
        bb.putInt(ackNum.toInt()) // Acknowledgment number
        bb.put(0x50.toByte()) // Data offset: 5 (20 bajtów), rezerwa 0
        bb.put(0x14.toByte()) // Flags: RST (0x04) + ACK (0x10) = 0x14
        bb.putShort(0.toShort()) // Window size: 0
        bb.putShort(0.toShort()) // Checksum placeholder
        bb.putShort(0.toShort()) // Urgent pointer: 0

        val tcpChecksum = computeTcpIpv4Checksum(srcIp, dstIp, rstPacket, 20, 20)
        rstPacket[36] = ((tcpChecksum shr 8) and 0xFF).toByte()
        rstPacket[37] = (tcpChecksum and 0xFF).toByte()

        try {
            synchronized(outputStream) {
                outputStream.write(rstPacket)
            }
        } catch (_: Exception) {}
    }

    private fun handleTcpIpv6Packet(
        packet: ByteArray,
        length: Int,
        ipHeaderLen: Int,
        outputStream: FileOutputStream
    ) {
        if (length < ipHeaderLen + 20) return // TCP header minimum 20 bajtów

        val srcPort = ((packet[ipHeaderLen].toInt() and 0xFF) shl 8) or (packet[ipHeaderLen + 1].toInt() and 0xFF)
        val dstPort = ((packet[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or (packet[ipHeaderLen + 3].toInt() and 0xFF)
        val flags = packet[ipHeaderLen + 13].toInt() and 0xFF

        // Jeśli to już jest pakiet RST, nie odpowiadamy RST na RST
        if ((flags and 0x04) != 0) return

        val seqNum = ((packet[ipHeaderLen + 4].toLong() and 0xFF) shl 24) or
                ((packet[ipHeaderLen + 5].toLong() and 0xFF) shl 16) or
                ((packet[ipHeaderLen + 6].toLong() and 0xFF) shl 8) or
                (packet[ipHeaderLen + 7].toLong() and 0xFF)

        val isSyn = (flags and 0x02) != 0
        val ackNum = if (isSyn) seqNum + 1 else seqNum

        val srcIp = packet.copyOfRange(24, 40) // dest staje się src
        val dstIp = packet.copyOfRange(8, 24)  // src staje się dest

        val totalLength = 40 + 20 // 40 bajtów IPv6 + 20 bajtów TCP
        val rstPacket = ByteArray(totalLength)
        val bb = ByteBuffer.wrap(rstPacket)

        // IPv6 Header (40 bajtów)
        bb.putInt(0x60000000) // Wersja 6, Traffic Class 0, Flow Label 0
        bb.putShort(20.toShort()) // Payload length (20 bajtów nagłówka TCP)
        bb.put(6.toByte()) // Next Header: TCP
        bb.put(64.toByte()) // Hop Limit: 64
        bb.put(srcIp)
        bb.put(dstIp)

        // TCP Header (20 bajtów, offset 40)
        bb.position(40)
        bb.putShort(dstPort.toShort()) // zamiana portów
        bb.putShort(srcPort.toShort())
        bb.putInt(0) // Sequence number
        bb.putInt(ackNum.toInt()) // Acknowledgment number
        bb.put(0x50.toByte()) // Data offset: 5 (20 bajtów), rezerwa 0
        bb.put(0x14.toByte()) // Flags: RST (0x04) + ACK (0x10) = 0x14
        bb.putShort(0.toShort()) // Window size: 0
        bb.putShort(0.toShort()) // Checksum placeholder
        bb.putShort(0.toShort()) // Urgent pointer: 0

        val tcpChecksum = computeTcpIpv6Checksum(srcIp, dstIp, rstPacket, 40, 20)
        rstPacket[56] = ((tcpChecksum shr 8) and 0xFF).toByte()
        rstPacket[57] = (tcpChecksum and 0xFF).toByte()

        try {
            synchronized(outputStream) {
                outputStream.write(rstPacket)
            }
        } catch (_: Exception) {}
    }

    private fun processIpv6Packet(
        packet: ByteArray,
        length: Int,
        outputStream: FileOutputStream
    ) {
        if (length < 40) return
        val nextHeader = packet[6].toInt() and 0xFF

        // Natychmiastowe odrzucenie połączeń TCP w tunelu IPv6 za pomocą TCP RST (np. próba obejścia przez DoH na porcie 443)
        if (nextHeader == 6) {
            handleTcpIpv6Packet(packet, length, 40, outputStream)
            return
        }

        if (nextHeader != 17) return // Tylko UDP
        if (length < 48) return // 40 bytes IPv6 header + 8 bytes UDP

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

        val senderPackages = getSenderPackages(packet, srcPort, dstPort, isIpv6 = true)
        val senderPackage = senderPackages?.firstOrNull()
        FloatingWidgetManager.registerGameQuery(senderPackage)

        val isSenderFullBlocked = if (senderPackages != null && fullBlockedPackages.isNotEmpty()) {
            senderPackages.any { fullBlockedPackages.contains(it) }
        } else {
            false
        }
        val isDomainBlocked = isDomainBlocked(domain)
        val isPausedForSender = FloatingWidgetManager.isAdBlockPausedFor(senderPackage)

        if (TrafficInspectorManager.isSniffing.value) {
            TrafficInspectorManager.onDomainQueried(domain, senderPackage, isDomainBlocked && !isPausedForSender)
        }

        if (isSenderFullBlocked || (isDomainBlocked && !isPausedForSender)) {
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
            val cacheKey = "${domain}_$qType"
            val cachedResponse = getFromResponseCache(cacheKey)

            if (cachedResponse != null) {
                val responseDnsPayload = cachedResponse.copyOf().also {
                    it[0] = dnsPayload[0]
                    it[1] = dnsPayload[1]
                }
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
            } else {
                val srcIpCopy = packet.copyOfRange(24, 40)
                val dstIpCopy = packet.copyOfRange(8, 24)

                handlerScope.launch(Dispatchers.IO) {
                    val responseDnsPayload = forwardDnsQuery(dnsPayload)
                    if (responseDnsPayload != null) {
                        putInResponseCache(cacheKey, responseDnsPayload)
                        val responsePacket = wrapInIpv6Udp(
                            dnsPayload = responseDnsPayload,
                            srcIp = srcIpCopy,
                            dstIp = dstIpCopy,
                            srcPort = dstPort,
                            dstPort = srcPort
                        )
                        try {
                            synchronized(outputStream) {
                                outputStream.write(responsePacket)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Błąd zapisu odpowiedzi DNS IPv6 do tunelu", e)
                        }
                    }
                }
            }
        }
    }

    private fun getSenderPackages(packet: ByteArray, srcPort: Int, dstPort: Int, isIpv6: Boolean): List<String>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && connectivityManager != null) {
            try {
                val (srcIp, dstIp) = if (isIpv6) {
                    Pair(
                        InetAddress.getByAddress(packet.copyOfRange(8, 24)),
                        InetAddress.getByAddress(packet.copyOfRange(24, 40))
                    )
                } else {
                    Pair(
                        InetAddress.getByAddress(packet.copyOfRange(12, 16)),
                        InetAddress.getByAddress(packet.copyOfRange(16, 20))
                    )
                }
                val localAddress = InetSocketAddress(srcIp, srcPort)
                val remoteAddress = InetSocketAddress(dstIp, dstPort)

                val uid = connectivityManager.getConnectionOwnerUid(
                    OsConstants.IPPROTO_UDP,
                    localAddress,
                    remoteAddress
                )
                if (uid > 0) {
                    val cached = uidPackageCache.get(uid)
                    if (cached != null) return cached

                    val packages = packageManager.getPackagesForUid(uid)
                    if (!packages.isNullOrEmpty()) {
                        val list = packages.toList()
                        uidPackageCache.put(uid, list)
                        return list
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        } else {
            // Fallback dla Androida 8.0-9.0 (API 26-28): odczyt /proc/net/udp lub /proc/net/udp6
            try {
                val uid = getUidFromProcNet(srcPort, isIpv6)
                if (uid != null && uid > 0) {
                    val cached = uidPackageCache.get(uid)
                    if (cached != null) return cached

                    val packages = packageManager.getPackagesForUid(uid)
                    if (!packages.isNullOrEmpty()) {
                        val list = packages.toList()
                        uidPackageCache.put(uid, list)
                        return list
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun getUidFromProcNet(srcPort: Int, isIpv6: Boolean): Int? {
        val path = if (isIpv6) "/proc/net/udp6" else "/proc/net/udp"
        val hexPort = String.format(java.util.Locale.US, "%04X", srcPort)
        val file = java.io.File(path)
        if (!file.exists() || !file.canRead()) return null

        try {
            file.useLines { lines ->
                for (line in lines) {
                    val tokens = line.trim().split(Regex("\\s+"))
                    if (tokens.size >= 8) {
                        val localAddr = tokens[1]
                        val colonIdx = localAddr.indexOf(':')
                        if (colonIdx != -1 && localAddr.substring(colonIdx + 1).equals(hexPort, ignoreCase = true)) {
                            val uidStr = tokens[7]
                            val uid = uidStr.toIntOrNull()
                            if (uid != null) return uid
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
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
        val isA = qType == 1
        val isAaaa = qType == 28

        if (isA || isAaaa) {
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
        } else {
            // Zgodność z RFC: dla innych typów rekordów (np. HTTPS typ 65, TXT, SVCB) zwracamy poprawną pustą odpowiedź NODATA (NOERROR, ANCOUNT=0)
            val response = ByteArray(12 + questionBytes.size)
            val bb = ByteBuffer.wrap(response)

            bb.put(idHigh)
            bb.put(idLow)
            bb.putShort(0x8180.toShort()) // Flags: Standard response, No error (NODATA)
            bb.putShort(1.toShort())      // QDCOUNT: 1
            bb.putShort(0.toShort())      // ANCOUNT: 0
            bb.putShort(0.toShort())      // NSCOUNT: 0
            bb.putShort(0.toShort())      // ARCOUNT: 0

            bb.put(questionBytes)
            return response
        }
    }

    private fun forwardDnsQuery(queryPayload: ByteArray): ByteArray? {
        val primary = trySendRecv(queryPayload, UPSTREAM_DNS_IPV4)
        if (primary != null) return primary

        val backup = trySendRecv(queryPayload, UPSTREAM_DNS_BACKUP)
        if (backup != null) return backup

        // Fallback do natywnych serwerów DNS sieci Wi-Fi/LTE (np. router lokalny, captive portal)
        val networkDnsServers = getActiveNetworkDnsServers()
        for (dnsIp in networkDnsServers) {
            if (dnsIp != UPSTREAM_DNS_IPV4 && dnsIp != UPSTREAM_DNS_BACKUP) {
                val response = trySendRecv(queryPayload, dnsIp)
                if (response != null) return response
            }
        }
        return null
    }

    private fun getActiveNetworkDnsServers(): List<String> {
        return try {
            val cm = connectivityManager ?: return emptyList()
            val activeNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm.activeNetwork ?: return emptyList()
            } else {
                return emptyList()
            }
            val linkProps = cm.getLinkProperties(activeNetwork) ?: return emptyList()
            linkProps.dnsServers.mapNotNull { it.hostAddress }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun trySendRecv(queryPayload: ByteArray, serverIp: String): ByteArray? {
        var socket: DatagramSocket? = null
        return try {
            val serverAddress = InetAddress.getByName(serverIp)
            socket = DatagramSocket().apply {
                vpnService.protect(this)
                soTimeout = DNS_TIMEOUT_MS
            }
            val sendPacket = DatagramPacket(queryPayload, queryPayload.size, serverAddress, DNS_PORT)
            socket.send(sendPacket)

            val recvBuffer = ByteArray(2048)
            val recvPacket = DatagramPacket(recvBuffer, recvBuffer.size)

            val expectedId0 = queryPayload[0]
            val expectedId1 = queryPayload[1]
            val startTime = System.currentTimeMillis()

            // Odbieramy odpowiedź i weryfikujemy Transaction ID, eliminując ryzyko DNS cross-talk / cache poisoning
            while (System.currentTimeMillis() - startTime < DNS_TIMEOUT_MS) {
                socket.receive(recvPacket)
                if (recvPacket.length >= 12) {
                    if (recvBuffer[0] == expectedId0 && recvBuffer[1] == expectedId1) {
                        return recvBuffer.copyOfRange(0, recvPacket.length)
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
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
        bb.putShort(0.toShort()) // Checksum placeholder

        // DNS Payload
        bb.put(dnsPayload)

        // Obliczenie sumy kontrolnej UDP dla IPv6 (obowiązkowa w RFC 8200)
        val checksum = computeUdpIpv6Checksum(srcIp, dstIp, packet, 40, udpLength)
        packet[46] = ((checksum shr 8) and 0xFF).toByte()
        packet[47] = (checksum and 0xFF).toByte()

        return packet
    }

    private fun computeTcpIpv4Checksum(
        srcIp: ByteArray,
        dstIp: ByteArray,
        packet: ByteArray,
        offset: Int,
        tcpLength: Int
    ): Int {
        var sum = 0L

        // Pseudo-header IPv4: Src IP (4 bajty)
        sum += (((srcIp[0].toInt() and 0xFF) shl 8) or (srcIp[1].toInt() and 0xFF)).toLong()
        sum += (((srcIp[2].toInt() and 0xFF) shl 8) or (srcIp[3].toInt() and 0xFF)).toLong()

        // Dest IP (4 bajty)
        sum += (((dstIp[0].toInt() and 0xFF) shl 8) or (dstIp[1].toInt() and 0xFF)).toLong()
        sum += (((dstIp[2].toInt() and 0xFF) shl 8) or (dstIp[3].toInt() and 0xFF)).toLong()

        // Zero (1 bajt) + Protocol: 6 (1 bajt)
        sum += 6L

        // TCP Length (2 bajty)
        sum += tcpLength.toLong()

        // TCP Header + Data
        var i = offset
        val end = offset + tcpLength
        while (i < end - 1) {
            sum += (((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)).toLong()
            i += 2
        }
        if (i < end) {
            sum += ((packet[i].toInt() and 0xFF) shl 8).toLong()
        }

        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val checksum = (sum.inv() and 0xFFFF).toInt()
        return if (checksum == 0) 0xFFFF else checksum
    }

    private fun computeUdpIpv6Checksum(
        srcIp: ByteArray,
        dstIp: ByteArray,
        udpPacket: ByteArray,
        udpOffset: Int,
        udpLength: Int
    ): Int {
        var sum = 0L

        // Pseudo-header IPv6: Src IP (16 bajtów)
        for (i in 0 until 16 step 2) {
            sum += (((srcIp[i].toInt() and 0xFF) shl 8) or (srcIp[i + 1].toInt() and 0xFF)).toLong()
        }
        // Dest IP (16 bajtów)
        for (i in 0 until 16 step 2) {
            sum += (((dstIp[i].toInt() and 0xFF) shl 8) or (dstIp[i + 1].toInt() and 0xFF)).toLong()
        }
        // Length (32 bity w nagłówku rzekomym)
        sum += (udpLength shr 16).toLong()
        sum += (udpLength and 0xFFFF).toLong()
        // Next Header: 17 (UDP)
        sum += 17L

        // Nagłówek UDP + Payload
        var i = udpOffset
        val end = udpOffset + udpLength
        while (i < end - 1) {
            sum += (((udpPacket[i].toInt() and 0xFF) shl 8) or (udpPacket[i + 1].toInt() and 0xFF)).toLong()
            i += 2
        }
        if (i < end) {
            sum += ((udpPacket[i].toInt() and 0xFF) shl 8).toLong()
        }

        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val checksum = (sum.inv() and 0xFFFF).toInt()
        return if (checksum == 0) 0xFFFF else checksum
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

    private fun computeTcpIpv6Checksum(
        srcIp: ByteArray,
        dstIp: ByteArray,
        packet: ByteArray,
        offset: Int,
        tcpLength: Int
    ): Int {
        var sum = 0L

        // Pseudo-header IPv6: Src IP (16 bajtów)
        for (i in 0 until 16 step 2) {
            sum += (((srcIp[i].toInt() and 0xFF) shl 8) or (srcIp[i + 1].toInt() and 0xFF)).toLong()
        }
        // Dest IP (16 bajtów)
        for (i in 0 until 16 step 2) {
            sum += (((dstIp[i].toInt() and 0xFF) shl 8) or (dstIp[i + 1].toInt() and 0xFF)).toLong()
        }
        // Length (32 bity w nagłówku rzekomym)
        sum += (tcpLength shr 16).toLong()
        sum += (tcpLength and 0xFFFF).toLong()
        // Next Header: 6 (TCP)
        sum += 6L

        // Nagłówek TCP + Dane
        var i = offset
        val end = offset + tcpLength
        while (i < end - 1) {
            sum += (((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)).toLong()
            i += 2
        }
        if (i < end) {
            sum += ((packet[i].toInt() and 0xFF) shl 8).toLong()
        }

        while (sum shr 16 != 0L) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        val checksum = (sum.inv() and 0xFFFF).toInt()
        return if (checksum == 0) 0xFFFF else checksum
    }
}
