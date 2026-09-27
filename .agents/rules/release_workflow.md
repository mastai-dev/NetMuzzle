# Procedura publikacji wydań (Release Workflow)

Przy każdym przygotowywaniu, podbijaniu lub wrzucaniu nowej wersji aplikacji na GitHub:

1. **Zawsze zapytaj użytkownika:**
   > „Czy ta nowa wersja jest wymagana do pobrania (obowiązkowa / blokująca starsze wersje), czy opcjonalna?”

2. **W zależności od decyzji użytkownika:**
   - **JEŻELI WYMAGANA (TAK):**
     - Podbij `versionCode` i `versionName` w `app/build.gradle.kts`.
     - W `version.json` ustaw:
       - `min_version_code` na nowy `versionCode` (to natychmiast zablokuje interfejs w starszych wersjach i wymusi aktualizację).
       - `latest_version_code` na nowy `versionCode`.
       - `latest_version_name` na nowy `versionName`.
       - `force_update` = `true`.
     - Zaktualizuj odpowiednie tagi i nazwy assetów w `.github/workflows/build.yml`.

   - **JEŻELI OPCJONALNA (NIE):**
     - Podbij `versionCode` i `versionName` w `app/build.gradle.kts`.
     - W `version.json`:
       - **NIE podbijaj** `min_version_code` (pozostaje na dotychczasowej wartości, więc starsze wersje działają normalnie i nie są blokowane).
       - Zaktualizuj jedynie `latest_version_code` i `latest_version_name` na nowy numer wydania.
     - Zaktualizuj odpowiednie tagi i nazwy assetów w `.github/workflows/build.yml`.
