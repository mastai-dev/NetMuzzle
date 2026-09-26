# Plan Monetyzacji i Integracji Reklam w NetMuzzle 💰

Dokument opisuje koncepcję, architekturę techniczną oraz wpływ na wydajność planowanego wdrożenia reklam w aplikacji **NetMuzzle**.

---

## 1. Koncepcja Biznesowa: Model "Zero-Spam" (Interwał 2h + Restart)

Większość darmowych narzędzi zasypuje użytkownika ciągłymi banerami i pełnoekranowymi reklamami co kilkadziesiąt sekund, co niszczy wrażenia z użytkowania i drenuje baterię.

**Założenie NetMuzzle:**
* Reklama pojawia się **wyłącznie przy wejściu do aplikacji** (format App Open Ad) nie częściej niż **raz na 2 godziny** (cooldown 120 minut).
* **Restart telefonu przerywa/resetuje ten okres** – po ponownym uruchomieniu urządzenia pierwsze wejście w aplikację od razu wyświetla reklamę, bez względu na to, ile czasu upłynęło od ostatniego wyświetlenia przed wyłączeniem telefonu.
* Przy wejściach w trakcie trwania 2-godzinnego okresu ochronnego (np. szybkie włączenie/wyłączenie firewalla, zmiana konfiguracji reguł czy odblokowanie gry) **aplikacja nie wyświetla żadnych reklam**.

---

## 2. Wpływ na Baterię i Procesor (Analiza Techniczna)

### Czy reklamy zwiększą zużycie baterii lub CPU?
**Odpowiedź: W praktyce NIE (wpływ jest bliski zeru).**

* **Brak działania w tle:** Usługa firewalla (`FirewallService` / `VpnService`) jest całkowicie odseparowana od modułu reklamowego. Reklamy ładują się wyłącznie w interfejsie graficznym (`MainActivity`) i nigdy w tle.
* **Zerowy koszt w trybie czuwania:** Gdy aplikacja jest zminimalizowana lub ekran jest wygaszony, moduł reklamowy nie wykonuje żadnych zapytań sieciowych ani operacji procesora.
* **Znikomy transfer:** Pobranie pojedynczej planszy reklamowej (maksymalnie raz na 2h, pod warunkiem że użytkownik w ogóle wejdzie do aplikacji) to transfer rzędu ~80–150 KB, co zajmuje procesorowi około 0,05 sekundy. Przy typowym użytkowaniu (1–3 wejścia na dzień) daje to 1–2 wyświetlenia na dobę.
* **Rozmiar aplikacji:** Dodanie Google Mobile Ads SDK zwiększy rozmiar pliku `.apk` o ok. 1,5 MB (z ~1,2 MB do ok. 2,7 MB).

---

## 3. Rekomendowany Format: Google App Open Ad

Najlepszym formatem dla tego modelu jest **App Open Ad** (Reklama przy otwarciu aplikacji) dostarczana przez Google AdMob:
* Elegancka, pełnoekranowa plansza z logo i nazwą *NetMuzzle* na dolnym pasku.
* Wyświetla się natychmiast przy wejściu do aplikacji (jeśli minęły 2 godziny LUB jest to pierwsze wejście po restarcie telefonu).
* Po kliknięciu przycisku „X” użytkownik płynnie przechodzi do ekranu zarządzania aplikacjami.
* Cechuje się znacznie wyższą stawką eCPM (zarobkiem za 1000 wyświetleń) niż małe, uciążliwe banery dolne.

---

## 4. Architektura Logiki Wyświetlania (Do wdrożenia)

```
                            [ Użytkownik otwiera aplikację ]
                                           │
                ┌──────────────────────────┴──────────────────────────┐
                ▼                                                     ▼
    [ Czy to 1. wejście po restarcie? ]               [ Czy minęły min. 2h od ostatniej reklamy? ]
  (!wasAdShownThisBoot LUB bootTimestamp)              (currentTimeMillis - lastAdTimestamp >= 2h)
                │                                                     │
                └──────────────────────────┬──────────────────────────┘
                                           │ (Jeśli przynajmniej 1 warunek = PRAWDA)
                                           ▼
                                  [ POKAŻ REKLAMĘ ]
                                           │
                                           ▼
                    1. Zapisz timestamp teraz (System.currentTimeMillis()) w DataStore
                    2. Ustaw flagę sesji wasAdShownThisBoot = true
```

### Kod sprawdzający warunek (szkic implementacyjny):
```kotlin
object AdDisplayManager {
    // Okres ochronny: 2 godziny (120 minut) w milisekundach
    private const val AD_COOLDOWN_MS = 2 * 60 * 60 * 1000L

    // Flaga w pamięci procesu - po restarcie telefonu zawsze ma wartość false
    var wasAdShownThisBoot: Boolean = false

    suspend fun shouldShowAd(preferences: FirewallPreferences): Boolean {
        // Warunek 1: Restart telefonu przerywa okres 2h - pierwsze wejście zawsze z reklamą
        if (!wasAdShownThisBoot) {
            return true
        }

        // Warunek dodatkowy w oparciu o czas uruchomienia systemu Android:
        // SystemClock.elapsedRealtime() to czas działania telefonu od bootu w ms.
        // Jeśli od ostatniej reklamy upłynęło więcej czasu niż czas od restartu urządzenia,
        // oznacza to, że w międzyczasie nastąpił restart telefonu.
        val lastAdTimestamp = preferences.getLastAdTimestampSync()
        val now = System.currentTimeMillis()
        val timeSinceBoot = android.os.SystemClock.elapsedRealtime()
        val timeSinceLastAd = now - lastAdTimestamp

        val phoneWasRebootedSinceLastAd = timeSinceLastAd > timeSinceBoot && lastAdTimestamp > 0
        if (phoneWasRebootedSinceLastAd) {
            return true
        }

        // Warunek 2: Upływ co najmniej 2 godzin od ostatniej wyświetlonej reklamy
        return timeSinceLastAd >= AD_COOLDOWN_MS
    }

    suspend fun markAdAsShown(preferences: FirewallPreferences) {
        wasAdShownThisBoot = true
        preferences.setLastAdTimestamp(System.currentTimeMillis())
    }
}
```

---

## 5. Jak zacząć zarabiać z Google AdMob (Krok po kroku)

1. **Rejestracja w AdMob:**
   * Załóż darmowe konto na [admob.google.com](https://admob.google.com).
   * Dodaj aplikację *NetMuzzle* (platforma: Android).
2. **Pobranie identyfikatorów jednostek reklamowych:**
   * Utwórz jednostkę typu *App Open*.
   * Skopiuj wygenerowany **App ID** (np. `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY`) oraz **Ad Unit ID**.
3. **Wypłaty środków:**
   * Wypłaty są realizowane automatycznie raz w miesiącu bezpośrednio na polski rachunek bankowy (przelew IBAN).
   * Próg wypłaty: **300 PLN** (lub równowartość 100 USD / 70 EUR).
4. **Zgoda na reklamy (RODO w UE):**
   * Zgodnie z prawem Unii Europejskiej przed wyświetleniem pierwszej reklamy należy zainicjalizować bezpłatny komponent Google UMP (User Messaging Platform), który wyświetla proste okienko zgody na personalizację reklam.
