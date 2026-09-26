# NetMuzzle 🛡️

<p align="center">
  <a href="README.md">English</a> • <strong>Polski</strong>
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/mastai-dev/NetMuzzle/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="96" height="96" alt="NetMuzzle Logo" style="border-radius: 20px;" />
</p>

<p align="center">
  <strong>Ultra-lekki firewall i pogromca reklam w grach na Androida (No-Root)</strong><br>
  <em>Bez roota. Zero drenażu baterii. Brak pętli analizy pakietów. Czysta wydajność jądra Linux.</em>
</p>

<p align="center">
  <a href="https://github.com/mastai-dev/NetMuzzle/releases/latest"><img src="https://img.shields.io/badge/Wydanie-v1.2.1-00E5FF?style=for-the-badge&logo=android&logoColor=white" alt="Najnowsze Wydanie"></a>
  <a href="https://github.com/mastai-dev/NetMuzzle/releases"><img src="https://img.shields.io/badge/Rozmiar%20APK-~1.2%20MB-success?style=for-the-badge" alt="Rozmiar APK"></a>
  <a href="https://android.com"><img src="https://img.shields.io/badge/Android-8.0%2B%20(Oreo%20do%2014%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Wsparcie Android"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/Licencja-MIT-blue.svg?style=for-the-badge" alt="Licencja"></a>
  <a href="https://github.com/mastai-dev/NetMuzzle/actions"><img src="https://img.shields.io/badge/Status%20CI-Kompilacja%20OK-brightgreen?style=for-the-badge&logo=githubactions&logoColor=white" alt="Status Budowania"></a>
</p>

<p align="center">
  <a href="https://github.com/mastai-dev/NetMuzzle/releases/download/v1.2.1/NetMuzzle-v1.2.1.apk">
    <img src="https://img.shields.io/badge/⬇️_Pobierz_Plik_APK-v1.2.1_(Bezpośrednio)-00E5FF?style=for-the-badge&labelColor=0f172a" alt="Pobierz APK">
  </a>
  <a href="https://mastai-dev.github.io/NetMuzzle/">
    <img src="https://img.shields.io/badge/🌐_Oficjalna_Strona-Interaktywny_Podgląd-7C3AED?style=for-the-badge&labelColor=0f172a" alt="Oficjalna Strona">
  </a>
</p>

---

## ⚡ Dlaczego NetMuzzle?

Większość tradycyjnych firewalli na Androida (takich jak NetGuard czy popularne blokery VPN) **przepuszcza 100% ruchu urządzenia do przestrzeni użytkownika**, odczytując i zapisując każdy pakiet w pętli procesora. To ciągłe filtrowanie przegrzewa procesor, drenuje baterię i powoduje opóźnienia (*lagi*) w grach online.

**NetMuzzle działa dokładnie odwrotnie, stawiając na bezkompromisową architekturę:**

1. 🚀 **0% Zużycia Procesora (Selektywna Czarna Dziura):**  
   Aplikacje dopuszczone **całkowicie omijają silnik VPN** bezpośrednio na poziomie jądra systemu Linux (`addAllowedApplication`). 99% ruchu sieciowego w ogóle nie dotyka naszej aplikacji.
2. 🎮 **Tarcza Reklam w Grach:**  
   Graj w gry mobilne bez natrętnych 30-sekundowych reklam wideo! Nasza lekka tarcza DNS wycina domeny reklamowe (Unity Ads, Google AdMob, AppLovin, IronSource, Mintegral itp.), podczas gdy serwery gry, tabele wyników i tryb wieloosobowy działają z pełną prędkością łącza.
3. 💊 **Nowoczesna Tekstowa Kapsuła 3-Stanowa (Nowość w v1.2.0):**  
   Przejrzyste, czytelne etykiety (`Zezwalaj` | `Tylko Ads` | `Kaganiec`) z neonowym podświetleniem aktywnego trybu wewnątrz każdego kafelka aplikacji.
4. 🔍 **Pasek 4 Filtrów z Licznikami (Nowość w v1.2.0):**  
   Błyskawiczne filtrowanie aplikacji według kategorii: `Wszystkie`, `🎮 Gry`, `🛡️ Blokada Ads` oraz `🚫 Kaganiec` z dynamicznymi licznikami aplikacji.
5. 🎁 **Kontrola nad Reklamami z Nagrodami (Rewarded Ads):**  
   Potrzebujesz darmowych monet, diamentów lub dodatkowego życia w grze? Wystarczy jeden klik w ustawieniach, aby tymczasowo odblokować sieć (np. Unity Ads) i odebrać nagrodę za film!
6. 🔒 **Pancerna Szczelność (Zero Wycieków DNS):**  
   Zablokowane programy otrzymują martwe lokalne serwery DNS (`10.0.0.1` oraz `fd00::2`). Zapytania natychmiast kończą się błędem wewnątrz telefonu i nie wyciekają do Twojego operatora komórkowego ani dostawcy internetu.

---

## 📊 Porównanie: NetMuzzle na tle konkurencji

| Cecha / Parametr | Tradycyjne firewalle VPN (np. NetGuard) | Filtry Root / Hosts (AdAway, iptables) | **NetMuzzle 🛡️ (v1.2.1)** |
| :--- | :---: | :---: | :---: |
| **Wymaga uprawnień Roota?** | ❌ Nie | ⚠️ Tak (Utrata gwarancji) | 🟢 **Nie (Bez roota)** |
| **Zużycie baterii i procesora** | 🔴 5% – 15% (analiza pakietów) | 🟢 Znikome | 🟢 **Dokładnie 0% (Czarna Dziura)** |
| **Gry mobilne online** | 🔴 Lagi i zrywanie połączenia | 🟡 Wymaga żmudnych reguł | 🟢 **Zero lagów (Game Shield)** |
| **Wsparcie dla reklam z nagrodami** | ❌ Wszystko albo nic | ❌ Ręczna edycja pliku hosts | 🟢 **1-klikowe przełączniki sieci** |
| **Waga pliku instalacyjnego** | 🟡 15 MB – 45 MB | 🟡 10 MB – 25 MB | 🟢 **Ultra-lekka (~1.2 MB)** |
| **3-stanowa kapsuła tekstowa** | ❌ Tylko 2 stany (Wł/Wył) | ❌ Tylko globalnie | 🟢 **Zezwalaj / Tylko Ads / Kaganiec** |
| **Kategorie filtrowania** | ❌ Tylko wyszukiwarka | ❌ Tylko globalnie | 🟢 **Wszystkie / Gry / Ads / Kaganiec** |
| **Wycieki zapytań w tle** | 🟡 Ryzyko ominięcia proxy | 🟢 Zablokowane | 🟢 **Pętla Loopback Sink** |
| **Prywatność i Open Source** | 🟡 Pakiety telemetryczne / Zamknięty kod | 🟢 Open Source | 🟢 **100% Open Source (MIT)** |

---

## ✨ Przegląd Kluczowych Funkcji

### 💊 Nowoczesna Tekstowa Kapsuła 3-Stanowa
Steruj dostępem do sieci dla każdej aplikacji z osobna za pomocą dotykowego selektora:
* **Zezwalaj (Bypass):** Aplikacja łączy się bezpośrednio przez Wi-Fi / LTE z pominięciem interfejsu VPN.
* **Tylko Ads (Tarcza do gier):** Serwery gier i rozgrywka działają bez przeszkód, a sieci reklamowe (Unity Ads, AdMob itp.) są ucinane na poziomie telefonu z neonowym, błękitnym podświetleniem.
* **Kaganiec (Pełna blokada):** Aplikacja traci łączność ze światem – pakiety wpadają do czarnej dziury o zerowym narzucie CPU z wyrazistym, czerwonym podświetleniem.

### 🔍 Przewijany Pasek 4 Filtrów
Wygodne zarządzanie zainstalowanymi programami dzięki dedykowanym licznikom:
* **Wszystkie (`Wszystkie (%d)`):** Pełna lista zainstalowanych programów.
* **🎮 Gry (`Gry (%d)`):** Szybki podgląd wyłącznie wykrytych na telefonie gier.
* **🛡️ Blokada Ads (`Tylko Ads (%d)`):** Lista wszystkich aplikacji i gier z aktywnym filtrem reklam.
* **🚫 Kaganiec (`Kaganiec (%d)`):** Lista programów z całkowitą blokadą łączności.

### ⚙️ Menedżer Filtrów Reklamowych
* **Wbudowane sieci reklamowe:** Gotowe reguły dla Unity Ads, Google AdMob, AppLovin, IronSource, Vungle (Liftoff), Mintegral, InMobi, Chartboost oraz Pangle.
* **Elastyczne wyłączanie:** Odznacz Unity Ads, odbierz bonus w grze i włącz blokadę z powrotem.
* **Własne domeny reklamowe:** Możliwość dodawania własnych serwerów reklamowych lub trackerów bezpośrednio z poziomu aplikacji.

### ⚡ Wyrazisty Główny Włącznik (Hero Switch)
* Nowoczesny, interaktywny włącznik na górze ekranu głównego, pozwalający jednym kliknięciem aktywować lub wstrzymać ochronę.

### ❓ Wbudowany Samouczek i Pomoc
* Czytelne okno przewodnika wyjaśniające znaczenie ikon (w tym plakietki gry), działanie filtrów oraz energooszczędność.

### 🌍 Pełna Obsługa Języka Polskiego i Angielskiego
* Automatyczne dopasowanie do języka systemowego Twojego smartfona.

---

## 🏗️ Architektura: Jak działa silnik 0% CPU?

```
                                [ Ruch wychodzący z telefonu ]
                                              │
         ┌────────────────────────────────────┴────────────────────────────────────┐
         ▼                                                                         ▼
[ ODBLOKOWANE APLIKACJE (99%) ]                                           [ CHRONIONE PROGRAMY ]
         │                                                                         │
  Poziom jądra Linuxa                                     ┌────────────────────────┴────────────────────────┐
  (addAllowedApplication)                                 ▼                                                 ▼
         │                                      [ TRYB BLOKUJ ADS ]                                 [ TRYB KAGANIEC ]
         ▼                                                │                                                 │
   Wi-Fi / 5G / LTE                               Tylko Port UDP 53                                         │
  (Natywna prędkość)                                      │                                                 ▼
   ZERO ZUŻYCIA CPU                                       ▼                                         Wirtualny TUN Sink
                                                   DnsPacketHandler                                   (10.0.0.1/32)
                                                 ┌────────┴────────┐                                        │
                                                 ▼                 ▼                                        ▼
                                            [ Domena Ads ]   [ Serwer Gry ]                            Pakiety giną
                                                 │                 │                                natychmiast w OS
                                                 ▼                 ▼                                 (0% CPU, 0 LAG)
                                            Zablokowano       Przepuszczono
                                             (0.0.0.0)      przez protect(socket)
```

> [!NOTE]
> Gdy wszystkie aplikacje mają status *Zezwalaj* lub *Kaganiec*, wątek DNS zostaje automatycznie zatrzymany. Aplikacja przełącza się w stan pasywnej czarnej dziury bez żadnego przetwarzania pakietów w tle.

---

## 🚀 Szybki Start / Instalacja

1. **Pobierz:** Ściągnij najnowszy plik `NetMuzzle-v1.2.1.apk` (~1.2 MB) z zakładki [Releases](https://github.com/mastai-dev/NetMuzzle/releases/latest).
2. **Zainstaluj:** Uruchom pobrany plik na telefonie (Android 8.0 Oreo do Android 14+). Zezwól na instalację z nieznanych źródeł, jeśli system o to zapyta.
3. **Zezwól na VPN:** Otwórz NetMuzzle i potwierdź jednorazową systemową zgodę na połączenie VPN. Całość działa w 100% lokalnie na Twoim urządzeniu.
4. **Wybierz reguły:** Ustaw gry na **Tylko Ads**, wścibskie programy na **Kaganiec**, włącz główny przełącznik i ciesz się czystym telefonem bez reklam!

---

## 🛡️ Prywatność i Uczciwa Polityka Reklamowa

* **Zero Telemetrii:** Brak Google Firebase, Crashlytics, SDK Facebooka i modułów śledzących.
* **100% Działania Offline:** Silnik zapory nie wymaga połączenia z zewnętrznymi serwerami do filtrowania ruchu.
* **Przejrzysty model "Zero-Spam":**
  * Darmowe oprogramowanie wymaga utrzymania. NetMuzzle wyświetla planszę wejściową (Google App Open Ad) **nie częściej niż raz na 2 godziny**.
  * Restart telefonu przerywa ten okres (pierwsze wejście po restarcie wyświetla reklamę).
  * Żadnych stałych banerów na ekranie, żadnych wyskakujących okienek podczas konfiguracji reguł.

---

## 🛠️ Kompilacja ze Źródeł (Dla Deweloperów)

Aby zbudować NetMuzzle lokalnie na swoim komputerze:

```bash
# Sklonuj repozytorium
git clone https://github.com/mastai-dev/NetMuzzle.git
cd NetMuzzle

# Zbuduj zoptymalizowaną paczkę APK (wymaga Java 17 / Microsoft OpenJDK 17)
./gradlew.bat assembleRelease
```

Skompresowany plik APK (zoptymalizowany przez R8) znajdziesz w katalogu:  
`app/build/outputs/apk/release/app-release.apk`

---

## 👤 Autor i Społeczność Open Source

* **Autor i Główny Twórca:** [Marcin Stankiewicz](https://github.com/mastai-dev)
* **Licencja:** [MIT License](LICENSE) — w 100% darmowy i otwarty kod źródłowy.
* **Zgłaszanie Uwag i Błędów:** Zapraszamy do zgłaszania propozycji w [GitHub Issue Tracker](https://github.com/mastai-dev/NetMuzzle/issues).

<p align="center">
  ⭐ <strong>Jeśli NetMuzzle chroni Twój telefon i pozwala grać bez reklam, zostaw gwiazdkę na GitHubie!</strong> ⭐
</p>
