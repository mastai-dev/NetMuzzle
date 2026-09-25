# Proguard rules for LightVPN

# Keep data models
-keep class com.lightvpn.firewall.model.** { *; }

# Keep VpnService & Receivers
-keep class com.lightvpn.firewall.service.** { *; }
