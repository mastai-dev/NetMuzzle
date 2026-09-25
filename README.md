# LightVPN - Minimalistyczny Firewall Android (No-Root)

Ultra-lekki, energooszczędny firewall na system Android, blokujący ruch sieciowy wybranym aplikacjom bez uprawnień roota i z zerowym narzutem na procesor.

---

## 🚀 Kluczowa Koncepcja Architektoniczna

Większość tradycyjnych firewalli na Androida przechwytuje 100% ruchu urządzenia i analizuje każdy pakiet w przestrzeni użytkownika (*userspace*), powodując wysokie zużycie baterii i obciążenie procesora.

**LightVPN stosuje podejście „Czarnej Dziury” (Blackhole Sink):**
* Wykorzystujemy metodę `VpnService.Builder.addAllowedApplication(packageName)`.
* Do interfejsu VPN trafiają **wyłącznie aplikacje dodane do czarnej listy**.
* Cały pozostały ruch internetowy (99% normalnego użytkowania) omija naszą aplikację **na poziomie jądra systemu Linux**, co redukuje zużycie procesora przez LightVPN do **0%**.
* Wewnątrz tunelu TUN pakiety nie są nigdzie przekazywane ani czytane – natychmiast giną.

---

## 🛡️ Rozwiązane Problemy i Szczelność Ochrony

1. **Szczelność DNS i brak wycieków (Zero DNS Leaks):**
   * Do interfejsu przypisano fikcyjne serwery DNS (`10.0.0.1` dla IPv4 oraz `fd00::2` dla IPv6).
   * Zablokowane aplikacje natychmiast otrzymują wyjątek `UnknownHostException` i nie marnują baterii na ponawianie prób połączenia TCP przez dziesiątki sekund.
2. **Pancerna obsługa IPv6:**
   * Poza trasą `::/0` przypisany jest unikalny adres lokalny ULA (`fd00::1/128`), co zapobiega crashom `IllegalArgumentException` i blokuje obejście firewalla przez IPv6.
3. **Inteligentny Tryb Czuwania (Standby Mode):**
   * Jeśli lista zablokowanych aplikacji jest pusta, tunel VPN **nie jest ustanawiany** (`vpnInterface = null`).
   * Zapobiega to przypadkowemu odcięciu połączenia całemu telefonowi.
4. **Bezszwowa aktualizacja reguł (Seamless Handover):**
   * Dodawanie i usuwanie aplikacji w trakcie działania firewalla odbywa się atomowo – nowy deskryptor TUN jest powoływany przed zamknięciem starego, nie rozłączając pozostałych połączeń.
5. **Autostart po restarcie telefonu (Boot on Load):**
   * Opcjonalny autostart po restarcie (`RECEIVE_BOOT_COMPLETED`), weryfikujący czy usługa była aktywna przed wyłączeniem telefonu.
6. **Kafel w Szybkich Ustawieniach (Quick Settings Tile):**
   * Przełącznik ochrony bezpośrednio z górnej belki Androida (`FirewallTileService`).
7. **Wykrywanie odinstalowania aplikacji (`PackageReceiver`):**
   * Automatyczne oczyszczanie czarnej listy po odinstalowaniu zablokowanego programu.

---

## 📁 Struktura Projektu

```text
LightVPN/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Deklaracje uprawnień, VpnService, Tile i Receiverów
│   │   ├── java/com/lightvpn/firewall/
│   │   │   ├── LightVpnApp.kt           # Klasa Application i kanały powiadomień
│   │   │   ├── model/
│   │   │   │   ├── AppInfo.kt           # Model aplikacji (nazwa, ikona, package, status)
│   │   │   │   └── FirewallState.kt     # Stany VPN (DISABLED, STANDBY, ACTIVE)
│   │   │   ├── data/
│   │   │   │   ├── FirewallPreferences.kt # Zarządzanie konfiguracją w Jetpack DataStore
│   │   │   │   └── AppListRepository.kt   # Pobieranie i filtrowanie zainstalowanych apek
│   │   │   ├── service/
│   │   │   │   ├── FirewallService.kt   # Silnik VpnService czarnej dziury
│   │   │   │   ├── BootReceiver.kt      # Autostart po restarcie telefonu
│   │   │   │   ├── PackageReceiver.kt   # Reakcja na odinstalowanie pakietów
│   │   │   │   └── FirewallTileService.kt # Kafel Szybkich Ustawień Androida
│   │   │   └── ui/
│   │   │       ├── MainActivity.kt      # Aktywność główna z launcherami zgody VPN
│   │   │       ├── screens/FirewallScreen.kt # UI w Jetpack Compose Material 3
│   │   │       ├── theme/               # Kolory, typografia, ciemny motyw
│   │   │       └── viewmodel/FirewallViewModel.kt # Reaktywny ViewModel (Flow/StateFlow)
│   │   └── res/                         # Zasoby (ikony wektorowe, kolory, teksty)
│   └── build.gradle.kts                 # Konfiguracja modułu aplikacji
├── build.gradle.kts                     # Konfiguracja główna Gradle
└── settings.gradle.kts                  # Rejestracja modułów
```

---

## 🛠️ Wymagania i Kompilacja

* **Android Studio:** Hedgehog (2023.1.1) lub nowsze (np. Iguana, Jellyfish, Koala).
* **JDK:** Wersja 17 lub nowsza.
* **Min SDK:** 26 (Android 8.0 Oreo).
* **Target SDK:** 34 (Android 14).

Aby otworzyć projekt:
1. Uruchom Android Studio.
2. Wybierz **File -> Open** i wskaż katalog `c:\antigravity\LightVPN`.
3. Poczekaj na automatyczną synchronizację Gradle (*Sync Project with Gradle Files*).
4. Podłącz telefon z włączonym debugowaniem USB lub uruchom emulator i kliknij **Run** (Zielony trójkąt).
