# NetMuzzle 🛡️ - Minimalistyczny Firewall Android (No-Root)

**NetMuzzle** to ultra-lekka, energooszczędna aplikacja na system Android, która oferuje elastyczną ochronę sieciową:
* 🌐 **Bypass (Zezwalaj):** Aplikacja działa bezpośrednio, omijając VPN.
* 🛡️ **Tylko Ads (Game Shield):** Selektywna blokada sieci reklamowych (Unity Ads, AdMob, AppLovin itp.) bez odcinania serwerów gry, rozgrywki multiplayer i z **zerowym narzutem na baterię**.
* 🚫 **Kaganiec (Blackhole):** Bezkompromisowe, całkowite odcięcie wybranej aplikacji od internetu (0% CPU).

---

## 🚀 Kluczowa Koncepcja: Czarna Dziura oraz DNS-Shield

Tradycyjne firewalle na Androida przekierowują 100% ruchu urządzenia do przestrzeni użytkownika (*userspace*), analizując każdy pakiet w pętli `read/write`. Skutkuje to drenowaniem baterii i nagrzewaniem procesora.

**NetMuzzle działa odwrotnie i bezkompromisowo:**
1. **Tryb Czarnej Dziury (Blackhole):** Gdy blokujesz całą aplikację, jej pakiety wpadają do zamkniętego tunelu i giną natychmiast na poziomie jądra. Zużycie procesora wynosi dokładnie **0%**.
2. **Tryb Blokady Reklam w Grach (DNS-Shield):** Ruch właściwy gry (grafika, pakiety sieciowe, multiplayer) **omija tunel VPN** i leci bezpośrednio przez Wi-Fi/LTE. Do tunelu trafiają wyłącznie mikro-pakiety zapytań DNS (UDP 53), gdzie domeny reklamowe dostają natychmiast `0.0.0.0`, a zapytania do serwerów gry są przepuszczane.
3. **Pełna kontrola nad reklamami z nagrodami (Rewarded Ads):** Wbudowany menedżer filtrów pozwala odblokować konkretnego dostawcę (np. Unity Ads), jeśli gracz chce odebrać nagrodę za obejrzenie filmu, a także dodać własne domeny reklamowe.

---

## 🛡️ Szczelność Ochrony i Zastosowane Rozwiązania

1. **Zero DNS Leaks (Natychmiastowe ucięcie połączenia):**
   * Do tunelu przypisano lokalne, martwe serwery DNS (`10.0.0.1` oraz `fd00::2`).
   * Zablokowane aplikacje natychmiast otrzymują błąd `UnknownHostException` i nie marnują baterii na ponawianie prób połączenia TCP przez kolejne minuty.
2. **Pełne wsparcie dla IPv6:**
   * Poza trasą `::/0` interfejs posiada unikalny adres lokalny IPv6 (`fd00::1/128`), co zapobiega crashom i zamyka furtkę ominięcia blokady przez IPv6.
3. **Inteligentny Tryb Czuwania (Standby Mode):**
   * Jeśli lista zablokowanych aplikacji jest pusta, tunel VPN nie jest powoływany. Chroni to przed przypadkowym odcięciem internetu w całym telefonie.
4. **Bezszwowa podmiana reguł (Seamless Handover):**
   * Zmiana stanu dowolnej aplikacji w trakcie działania ochrony podmienia konfigurację atomowo, bez rwania sesji pozostałych programów.
5. **Autostart po restarcie telefonu (Boot on Load):**
   * Opcjonalny autostart po włączeniu telefonu (`RECEIVE_BOOT_COMPLETED`), sterowany z poziomu ustawień.
6. **Kafel w Szybkich Ustawieniach (Quick Settings Tile):**
   * Szybkie włączanie i wyłączanie blokady prosto z górnej belki powiadomień.
7. **Automatyczne czyszczenie odinstalowanych aplikacji:**
   * Odbiornik `PackageReceiver` automatycznie usuwa odinstalowane aplikacje z konfiguracji.

---

## 📁 Struktura Projektu

```text
NetMuzzle/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # Deklaracje uprawnień, VpnService, Tile i Receiverów
│   │   ├── java/com/netmuzzle/firewall/
│   │   │   ├── NetMuzzleApp.kt          # Klasa Application i kanały powiadomień
│   │   │   ├── model/
│   │   │   │   ├── AppInfo.kt           # Model aplikacji (nazwa, ikona, package, status)
│   │   │   │   └── FirewallState.kt     # Stany (DISABLED, STANDBY, ACTIVE)
│   │   │   ├── data/
│   │   │   │   ├── FirewallPreferences.kt # Zarządzanie konfiguracją w Jetpack DataStore
│   │   │   │   └── AppListRepository.kt   # Pobieranie i filtrowanie zainstalowanych apek
│   │   │   ├── service/
│   │   │   │   ├── FirewallService.kt   # Silnik Czarnej Dziury VpnService
│   │   │   │   ├── BootReceiver.kt      # Autostart po restarcie telefonu
│   │   │   │   ├── PackageReceiver.kt   # Reakcja na odinstalowanie pakietów
│   │   │   │   └── FirewallTileService.kt # Kafel Szybkich Ustawień Androida
│   │   │   └── ui/
│   │   │       ├── MainActivity.kt      # Aktywność główna z obsługą zgody systemowej
│   │   │       ├── screens/FirewallScreen.kt # UI w Jetpack Compose Material 3
│   │   │       ├── theme/               # Nowoczesna, ciemna stylistyka (Neon Cyan & Slate)
│   │   │       └── viewmodel/FirewallViewModel.kt # Reaktywny ViewModel (StateFlow)
│   │   └── res/                         # Zasoby (ikony wektorowe, kolory, teksty)
│   └── build.gradle.kts                 # Konfiguracja modułu aplikacji z kompresją R8
├── build.gradle.kts                     # Konfiguracja nadrzędna Gradle
└── settings.gradle.kts                  # Rejestracja modułów
```

---

## 🛠️ Pobieranie i Kompilacja

### Opcja A: Automatyczne budowanie w chmurze (GitHub Actions)
Każdy `git push` na gałąź `main` automatycznie buduje zoptymalizowaną paczkę w zakładce **Actions**:
* **`NetMuzzle-Release-APK`** (~2-3 MB) – maksymalnie odchudzona, skompresowana przez R8 wersja gotowa do instalacji.
* **`NetMuzzle-Debug-APK`** – wersja deweloperska.

### Opcja B: Kompilacja lokalna z terminala
Mając zainstalowaną Javę 17 (Microsoft OpenJDK 17):
```powershell
.\gradlew.bat assembleRelease
```
Gotowy plik APK znajdzie się w katalogu: `app/build/outputs/apk/release/app-release.apk`.

---

## 👤 Autor i Prawa Autorskie

* **Autor i twórca:** Marcin Stankiewicz
* **Licencja:** [MIT License](LICENSE) (Projekt Open Source)
* **Model dystrybucji:** Aplikacja w 100% darmowa. W przyszłości mogą pojawić się nienachalne reklamy wspierające rozwój, jednak podstawowa funkcja blokady oraz pełna prywatność zawsze pozostaną bezpłatne i nienaruszone.
* **Przejrzystość (Open Source):** Cały kod źródłowy jest publicznie dostępny, co gwarantuje pełne bezpieczeństwo – każdy może sprawdzić, że aplikacja nie posiada żadnych ukrytych mechanizmów telemetrycznych ani serwerów śledzących.
