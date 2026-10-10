# Proguard rules for NetMuzzle

# Keep data models
-keep class com.netmuzzle.firewall.model.** { *; }

# Keep VpnService & Receivers
-keep class com.netmuzzle.firewall.service.** { *; }
