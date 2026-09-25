# Przewodnik Publikacji NetMuzzle w Google Play Store 🚀

Kompletny podręcznik krok po kroku, jak opublikować aplikację **NetMuzzle** w sklepie Google Play, spełniając wszystkie restrykcyjne wymagania firmy Google dotyczące usług VPN oraz widoczności pakietów.

---

## SPIS TREŚCI
1. [Wymagania formalne (Konto Dewelopera)](#1-wymagania-formalne-konto-dewelopera)
2. [Hosting Polityki Prywatności (GitHub Pages)](#2-hosting-polityki-prywatności-github-pages)
3. [Klucz Podpisywania Produkcyjnego (Keystore)](#3-klucz-podpisywania-produkcyjnego-keystore)
4. [Pobranie Paczki .AAB z GitHub Actions](#4-pobranie-paczki-aab-z-github-actions)
5. [Wypełnienie Deklaracji w Google Play Console (Gotowe Formułki)](#5-wypełnienie-deklaracji-w-google-play-console-gotowe-formułki)
6. [Materiały Graficzne i Karta w Sklepie (Store Listing)](#6-materiały-graficzne-i-karta-w-sklepie-store-listing)
7. [Zasada 20 Testerów (Closed Testing) i Zgłoszenie do Produkcji](#7-zasada-20-testerów-closed-testing-i-zgłoszenie-do-produkcji)

---

## 1. Wymagania formalne (Konto Dewelopera)

1. Załóż konto w [Google Play Console](https://play.google.com/console/signup).
2. Opłać jednorazową opłatę w wysokości **25 USD** (karta płatnicza).
3. Zweryfikuj tożsamość:
   * Google wymaga przesłania zdjęcia dowodu osobistego lub paszportu oraz potwierdzenia adresu (np. wyciąg bankowy/rachunek).
   * Weryfikacja trwa zazwyczaj od 24h do 48h.

---

## 2. Hosting Polityki Prywatności (GitHub Pages)

Google Play **bezwzględnie wymaga** działającego linku URL do polityki prywatności. W folderze `docs/` znajduje się zarówno strona główna (Landing Page), jak i dedykowana Polityka Prywatności (`privacy.html`).

### Jak włączyć stronę w 30 sekund:
1. Wejdź w swoje repozytorium na GitHubie: **`https://github.com/mastai-dev/NetMuzzle`**.
2. Kliknij **Settings** (Ustawienia repozytorium na górze).
3. W menu po lewej stronie kliknij **Pages**.
4. W sekcji **Build and deployment**:
   * **Source:** wybierz *Deploy from a branch*.
   * **Branch:** wybierz `main` oraz folder **`/docs`**.
   * Kliknij **Save**.
5. Po minucie Twoje strony będą aktywne pod adresami:
   * 🌐 **Strona główna (Landing Page):** `https://mastai-dev.github.io/NetMuzzle/`
   * 🛡️ **Polityka Prywatności (do wklejenia w Google Play):**
     👉 **`https://mastai-dev.github.io/NetMuzzle/privacy.html`**

---

## 3. Klucz Podpisywania Produkcyjnego (Keystore)

Aplikacje w Google Play muszą być podpisane prywatnym kluczem deweloperskim.

### Krok A: Wygenerowanie pliku klucza
Otwórz terminal PowerShell w katalogu projektu i wykonaj polecenie (wymaga Javy 17, którą masz zainstalowaną):

```powershell
keytool -genkey -v -keystore netmuzzle-release.jks -alias netmuzzle -keyalg RSA -keysize 2048 -validity 10000
```
* Program zapyta Cię o hasło (np. utwórz mocne hasło i **zapisz je w bezpiecznym miejscu**).
* Następnie zapyta o imię, nazwisko, firmę itp.
* Powstanie plik `netmuzzle-release.jks`.

> ⚠️ **BARDZO WAŻNE:** Nigdy nie usuwaj pliku `.jks` ani nie gub hasła! Bez tego klucza Google Play nie pozwoli Ci w przyszłości wydać żadnej aktualizacji aplikacji.

---

## 4. Pobranie Paczki .AAB z GitHub Actions

Google Play nie przyjmuje plików `.apk` – wymaga formatu **Android App Bundle (`.aab`)**.
Skonfigurowałem GitHub Actions, aby automatycznie generował ten plik:

1. Wejdź w zakładkę **Actions** w swoim repozytorium GitHub.
2. Kliknij najnowszy zakończony proces.
3. W sekcji **Artifacts** pobierz paczkę: **`NetMuzzle-GooglePlay-Bundle-AAB`**.
4. Wewnątrz znajdziesz plik `app-release.aab`, który wgrywa się bezpośrednio do konsoli Google Play.

---

## 5. Wypełnienie Deklaracji w Google Play Console (Gotowe Formułki)

Ponieważ NetMuzzle to firewall korzystający z `VpnService` oraz uprawnienia widoczności aplikacji, Google wymaga wypełnienia specjalnych deklaracji w zakładce **Zawartość aplikacji (App Content)**:

### A. Deklaracja VpnService (Polityka usług VPN)
* **Pytanie o główną funkcję:** Zaznacz kategorię: **Device Security / Antivirus / Firewall**.
* **Uzasadnienie (Wklej poniższy tekst po angielsku dla weryfikatorów Google):**
  > *"NetMuzzle is a minimalist on-device application firewall. It uses the Android VpnService exclusively to create a local blackhole loopback for applications selected by the user, blocking their unauthorized network traffic and trackers. No network data is inspected, analyzed, logged, or transmitted outside the device. Unblocked applications bypass the service entirely at the kernel level."*

### B. Deklaracja uprawnienia QUERY_ALL_PACKAGES
* **Kategoria aplikacji:** Wybierz: **Security / Device Management (Firewall)**.
* **Uzasadnienie (Wklej poniższy tekst):**
  > *"The core purpose of NetMuzzle is to allow users to selectively block internet access for specific apps on their device. To provide this functionality, the app requires QUERY_ALL_PACKAGES to list installed applications so the user can manage network permissions per application. The package list is strictly processed locally on the device and is never shared, collected, or uploaded."*

### C. Deklaracja typu usługi pierwszoplanowej (Android 14 Foreground Service)
* **Wybrany typ:** `specialUse`
* **Uzasadnienie (Wklej poniższy tekst):**
  > *"NetMuzzle runs an ongoing background firewall service to ensure user-defined network blocking rules remain active while the screen is off or other apps are running. The persistent notification allows the user to monitor status and instantly toggle or stop the firewall at any time."*

### D. Bezpieczeństwo danych (Data Safety Section)
* **Czy aplikacja zbiera lub udostępnia dane użytkownika?** -> Zaznacz: **NIE**.
* Aplikacja nie zbiera lokalizacji, kontaktów, identyfikatorów, danych finansowych ani danych diagnostycznych.

### E. Klasyfikacja wiekowa (IARC)
* Wypełnij prosty kwestionariusz (wybierz kategorię *Narzędzia / Użyteczność*).
* Aplikacja nie zawiera przemocy, nagości ani wulgaryzmów -> otrzyma kategorię **PEGI 3** / **Everyone**.

---

## 6. Materiały Graficzne i Karta w Sklepie (Store Listing)

Przed zgłoszeniem do publikacji przygotuj:
1. **Ikona aplikacji:** Format PNG, dokładnie **512 × 512 px**, do 1 MB.
2. **Grafika promująca (Feature Graphic):** Format JPG lub PNG, dokładnie **1024 × 500 px**.
3. **Zrzuty ekranu:** Co najmniej 2 zrzuty ekranu z działającej aplikacji (w formacie 16:9 lub standardowych proporcjach współczesnych telefonów).
4. **Teksty promocyjne:**
   * **Tytuł aplikacji:** `NetMuzzle - App Firewall`
   * **Krótki opis (do 80 znaków):**
     * *„Lekki firewall no-root. Odcinaj internet wybranym aplikacjom i chroń baterię.”*
   * **Pełny opis:**
     * Opisz brak konieczności posiadania roota, podejście „czarnej dziury” (0% zużycia CPU dla dopuszczonych aplikacji), szczelność DNS oraz brak reklam i śledzenia.

---

## 7. Zasada 20 Testerów (Closed Testing) i Zgłoszenie do Produkcji

> **Dotyczy kont prywatnych założonych po 13 listopada 2023 r.:**

Google wymaga przeprowadzenia **zamkniętych testów (Closed Testing)** przed udostępnieniem aplikacji publicznie:
1. W Google Play Console utwórz ścieżkę **Testy zamknięte (Closed Testing)** i wgraj swój plik `.aab`.
2. Dodaj listę adresów e-mail (konta Google) co najmniej **20 testerów** (znajomi, rodzina lub grupy testerskie na Reddicie/Facebooku, np. *r/AndroidClosedTesting*).
3. Wyślij testerom link z konsoli, aby dołączyli do programu testów i pobrali aplikację.
4. Testy muszą trwać nieprzerwanie przez **14 dni** (testerzy muszą mieć zainstalowaną aplikację).
5. Po upływie 14 dni w konsoli odblokuje się przycisk **„Zgłoś wniosek o dostęp do wersji produkcyjnej” (Apply for Production)**.
6. Zespół Google zweryfikuje aplikację (zazwyczaj 2–5 dni roboczych), po czym NetMuzzle pojawi się oficjalnie w wyszukiwarce Google Play dla wszystkich użytkowników!
