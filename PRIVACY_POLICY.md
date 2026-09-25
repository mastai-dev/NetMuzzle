# Polityka Prywatności aplikacji NetMuzzle

*Data ostatniej aktualizacji: 25 września 2026 r.*

## 1. Wstęp
Aplikacja **NetMuzzle** została stworzona z myślą o bezwzględnym poszanowaniu prywatności użytkownika. Aplikacja nie zbiera, nie przetwarza, nie profiluje ani nie przesyła żadnych danych osobowych, telemetrycznych ani diagnostycznych.

## 2. Gromadzenie Danych
* **Zero telemetrii i analityki:** Aplikacja nie zawiera żadnych bibliotek reklamowych (AdMob itp.), analitycznych (Google Analytics, Firebase itp.) ani śledzących.
* **Brak serwerów zewnętrznych:** Aplikacja nie komunikuje się z żadnym serwerem w sieci. Wszystkie operacje odbywają się w 100% lokalnie na Twoim urządzeniu.

## 3. Wykorzystanie usługi VpnService
Aplikacja korzysta z systemowego interfejsu `VpnService` wyłącznie w charakterze **lokalnej czarnej dziury (Blackhole Sink)**:
* Do wirtualnego tunelu VPN kierowany jest wyłącznie ruch aplikacji dodanych przez użytkownika do czarnej listy.
* Pakiety wpadające do tunelu są natychmiastowo odrzucane na poziomie urządzenia.
* Aplikacja **nie analizuje, nie czyta, nie podsłuchuje ani nie przesyła** zawartości pakietów sieciowych ani zapytań DNS.

## 4. Uprawnienie QUERY_ALL_PACKAGES
Uprawnienie to jest wykorzystywane wyłącznie w celu wyświetlenia użytkownikowi listy zainstalowanych aplikacji, aby umożliwić wybór programów do zablokowania. Lista aplikacji nigdy nie opuszcza urządzenia.

## 5. Przechowywanie danych
Konfiguracja blokad jest zapisywana lokalnie w pamięci urządzenia za pomocą komponentu Android Jetpack DataStore. Odinstalowanie aplikacji bezpowrotnie usuwa wszystkie zapisane reguły.

---
*Wersja angielska dostępna online: [NetMuzzle Privacy Policy](https://mastai-dev.github.io/LightVPN/)*
