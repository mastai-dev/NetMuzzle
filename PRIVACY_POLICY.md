# Polityka Prywatności aplikacji NetMuzzle

*Data ostatniej aktualizacji: 25 września 2026 r.*  
*Autor i właściciel praw autorskich:* **Marcin Stankiewicz**

---

## 1. Wstęp i Przejrzystość Open Source
Aplikacja **NetMuzzle** została stworzona przez **Marcina Stankiewicza** z myślą o bezwzględnym poszanowaniu prywatności użytkownika. 

Aplikacja jest projektem **Open Source** (udostępnianym na otwartej licencji MIT) – cały kod źródłowy jest publicznie dostępny w serwisie GitHub, co pozwala każdemu na niezależny audyt bezpieczeństwa i weryfikację, że aplikacja nie zawiera żadnych ukrytych funkcji ani złośliwego kodu.

## 2. Prawa do aplikacji i Model finansowania
* **Darmowa aplikacja:** NetMuzzle jest bezpłatna do pobrania i użytku.
* **Potencjalne reklamy w przyszłości:** W obecnej wersji aplikacja jest całkowicie wolna od reklam. W przyszłości mogą zostać wprowadzone opcjonalne lub nienachalne reklamy wspierające rozwój projektu. Ewentualne reklamy nigdy nie wpłyną na mechanizm firewalla i nie będą używane do profilowania ani analizowania ruchu sieciowego użytkownika.

## 3. Gromadzenie Danych
* **Zero telemetrii:** Aplikacja nie zbiera danych telemetrycznych, statystyk użycia ani raportów o awariach powiązanych z tożsamością użytkownika.
* **Brak serwerów pośredniczących:** Aplikacja nie komunikuje się z żadnym zewnętrznym serwerem. Wszystkie operacje odbywają się w 100% lokalnie na Twoim urządzeniu.

## 4. Wykorzystanie usługi VpnService
Aplikacja korzysta z systemowego interfejsu `VpnService` wyłącznie w charakterze **lokalnej czarnej dziury (Blackhole Sink)**:
* Do wirtualnego tunelu VPN kierowany jest wyłącznie ruch aplikacji dodanych przez użytkownika do czarnej listy.
* Pakiety wpadające do tunelu są natychmiastowo odrzucane na poziomie urządzenia.
* Aplikacja **nie analizuje, nie czyta, nie podsłuchuje ani nie przesyła** zawartości pakietów sieciowych ani zapytań DNS.

## 5. Uprawnienie QUERY_ALL_PACKAGES
Uprawnienie to jest wykorzystywane wyłącznie w celu wyświetlenia użytkownikowi listy zainstalowanych aplikacji, aby umożliwić wybór programów do zablokowania. Lista aplikacji nigdy nie opuszcza urządzenia.

## 6. Przechowywanie danych
Konfiguracja blokad jest zapisywana lokalnie w pamięci urządzenia za pomocą komponentu Android Jetpack DataStore. Odinstalowanie aplikacji bezpowrotnie usuwa wszystkie zapisane reguły.

---
*Repozytorium kodu i angielska wersja polityki prywatności: [NetMuzzle na GitHubie](https://github.com/mastai-dev/LightVPN)*
