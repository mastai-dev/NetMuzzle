# Plan Monetyzacji i Integracji Reklam w NetMuzzle 💰

Dokument opisuje koncepcję, architekturę techniczną oraz wpływ na wydajność planowanego wdrożenia reklam w aplikacji **NetMuzzle**.

---

## 1. Koncepcja Biznesowa: Model "Zero-Spam"

Większość darmowych narzędzi zasypuje użytkownika ciągłymi banerami i pełnoekranowymi reklamami co kilkadziesiąt sekund, co niszczy wrażenia z użytkowania i drenuje baterię.

**Założenie NetMuzzle:**
* Reklama wyświetla się **maksymalnie raz na dobę** przy pierwszym otwarciu aplikacji, LUB
* **Raz po restarcie telefonu** (gdy użytkownik wejdzie do aplikacji po raz pierwszy po ponownym uruchomieniu urządzenia).
* Przy każdorazowym kolejnym wejściu w ciągu dnia (np. szybkie włączenie/wyłączenie firewalla, zmiana konfiguracji reguł) **aplikacja nie wyświetla żadnych reklam**.

---

## 2. Wpływ na Baterię i Procesor (Analiza Techniczna)

### Czy reklamy zwiększą zużycie baterii lub CPU?
**Odpowiedź: W praktyce NIE (wpływ jest bliski zeru).**

* **Brak działania w tle:** Usługa firewalla (`FirewallService` / `VpnService`) jest całkowicie odseparowana od modułu reklamowego. Reklamy ładują się wyłącznie w interfejsie graficznym (`MainActivity`).
* **Zerowy koszt w trybie czuwania:** Gdy aplikacja jest zminimalizowana lub ekran jest wygaszony, moduł reklamowy nie wykonuje żadnych zapytań sieciowych ani operacji procesora.
* **Jednorazowy transfer:** Pobranie pojedynczej planszy reklamowej raz na 24h to transfer rzędu ~80–150 KB, co zajmuje procesorowi około 0,05 sekundy.
* **Rozmiar aplikacji:** Dodanie Google Mobile Ads SDK zwiększy rozmiar pliku `.apk` o ok. 1,5 MB (z ~2,5 MB do ok. 4 MB).

---

## 3. Rekomendowany Format: Google App Open Ad

Najlepszym formatem dla tego modelu jest **App Open Ad** (Reklama przy otwarciu aplikacji) dostarczana przez Google AdMob:
* Elegancka, pełnoekranowa plansza z logo i nazwą *NetMuzzle* na dolnym pasku.
* Wyświetla się natychmiast przy wejściu do aplikacji (jeśli spełniony jest warunek czasowy).
* Po kliknięciu przycisku „X” użytkownik płynnie przechodzi do ekranu zarządzania aplikacjami.
* Cechuje się znacznie wyższą stawką eCPM (zarobkiem za 1000 wyświetleń) niż małe banery dolne.

---

## 4. Architektura Logiki Wyświetlania (Do wdrożenia)

```
                            [ Użytkownik otwiera aplikację ]
                                           │
                ┌──────────────────────────┴──────────────────────────┐
                ▼                                                     ▼
     [ Czy to nowy dzień? ]                               [ Czy to pierwszy start po restarcie? ]
  (Dzisiejsza data != zapisana data)                     (Zmienna w pamięci procesu == false)
                │                                                     │
                └──────────────────────────┬──────────────────────────┘
                                           │ (Jeśli przynajmniej 1 warunek = PRAWDA)
                                           ▼
                                 [ POKAŻ REKLAMĘ ]
                                           │
                                           ▼
                        1. Zapisz dzisiejszą datę w DataStore
                        2. Ustaw flagę sesji = true
```

### Kod sprawdzający warunek (szkic implementacyjny):
```kotlin
object AdDisplayManager {
    // Flaga w pamięci RAM procesu - po restarcie telefonu zawsze ma wartość false
    var wasAdShownThisBoot: Boolean = false

    suspend fun shouldShowAd(preferences: FirewallPreferences): Boolean {
        val today = LocalDate.now().toString()
        val lastAdDate = preferences.getLastAdDateSync()

        val isNewDay = lastAdDate != today
        val isFirstAfterBoot = !wasAdShownThisBoot

        return isNewDay || isFirstAfterBoot
    }

    suspend fun markAdAsShown(preferences: FirewallPreferences) {
        wasAdShownThisBoot = true
        preferences.setLastAdDate(LocalDate.now().toString())
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
