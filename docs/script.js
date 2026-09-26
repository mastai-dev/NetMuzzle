/* ==========================================================================
   NetMuzzle - Interactive Script & Bilingual Localization (EN / PL)
   ========================================================================== */

// Translations dictionary
const translations = {
    en: {
        nav_features: "Features",
        nav_comparison: "Comparison",
        nav_performance: "Battery & CPU",
        nav_privacy: "Privacy & Trust",
        nav_faq: "FAQ",
        btn_download: "Download APK",

        hero_badge: "Android No-Root Firewall & Game AdBlock • v1.1.0 Released",
        hero_title: 'Muzzle the apps you don\'t trust. <span class="gradient-text">Zero battery drain.</span>',
        hero_desc: "Selectively cut off internet access or block intrusive mobile game ads with zero lag and zero CPU overhead. No root, zero DNS leaks, and full control over rewarded ads.",
        hero_cta_primary: "Download Free APK (~2 MB)",
        hero_cta_github: "Source Code on GitHub",

        stat_cpu: "CPU for Allowed Apps",
        stat_size: "Featherweight App",
        stat_offline: "On-Device & Offline",

        mock_search: "Search applications…",
        mock_all: "All (84)",
        mock_blocked_chip: "Protected (2)",
        mock_try_hint: "👆 Interactive live demo: click switches above!",

        badge_why: "Why NetMuzzle?",
        features_title: "Engineered differently. Built for zero waste.",
        features_subtitle: "Most Android firewalls route 100% of your network traffic into user-space, burning CPU and battery. NetMuzzle takes the opposite, pure architectural route.",

        f0_title: "Game AdBlock Shield (v1.1.0)",
        f0_desc: "Selectively blocks mobile game ads (Unity Ads, Google AdMob, AppLovin, IronSource) without cutting off game servers or multiplayer, and with 0% extra battery drain.",
        f0_tag: "Selective DNS Shield",

        f1_title: "0% CPU Blackhole Routing",
        f1_desc: "Instead of filtering every packet in memory, NetMuzzle tells the Linux kernel to send only blocked apps into a dead TUN loopback. 99% of your traffic never touches our app. Zero processing overhead.",
        f1_tag: "True Battery Friendly",

        f2_title: "Zero DNS Leaks (IPv4 & IPv6)",
        f2_desc: "Blocked apps are given dead local DNS servers (10.0.0.1 and fd00::2). DNS lookups fail instantaneously on-device, cutting off connection attempts immediately without leaking domain queries to your ISP.",
        f2_tag: "Watertight Privacy",

        f5_title: "3-Mode Capsule & Rewarded Ads",
        f5_desc: "Fine-tune apps between Bypass, Block Ads, and Full Muzzle. Unblock video rewards (e.g. Unity Ads) whenever you want bonus gems or coins in games, plus add custom ad servers.",
        f5_tag: "Ultimate Control",

        f3_title: "Zero-Spam Ads Policy",
        f3_desc: "No annoying banners, no popups during configuration, and no video interruptions. Ads occur at most once a day or after a phone reboot. When you toggle or manage rules, you enjoy absolute peace.",
        f3_tag: "Honest & Respectful",

        f4_title: "100% Open Source Trust",
        f4_desc: "Created by Marcin Stankiewicz under the MIT license. Every single line of Kotlin code is open to public inspection. No tracking, zero third-party telemetry SDKs, and zero telemetry servers.",
        f4_tag: "MIT Licensed",

        badge_comp: "Side-by-Side",
        comp_title: "How NetMuzzle compares",
        comp_subtitle: "See why the blackhole sink architecture outperforms traditional packet inspection solutions.",
        th_feature: "Architecture & Features",
        th_traditional: "Traditional Firewalls",
        th_vpn: "Commercial VPNs",
        row_cpu: "CPU Usage (Allowed Traffic)",
        row_battery: "Battery Drain Impact",
        row_dns: "DNS Leak Resistance",
        row_root: "Root Privileges Required",
        row_servers: "External Servers Needed",
        row_open: "Open Source & Auditable",

        badge_perf: "Deep Dive",
        perf_title: "The Secret Behind Zero Battery Drain",
        perf_p1: "When you open YouTube, Netflix, or stream 4K video, other firewalls force every packet through their user-space Java/Kotlin loops to check rules. This generates heat and drains your battery.",
        perf_p2: "NetMuzzle uses inverted selective routing: We instruct Android's Linux kernel via addAllowedApplication() to route only the blacklisted packages into the TUN interface.",
        perf_p3: "The other 99% of your network traffic passes through Wi-Fi/LTE directly in the kernel without waking up NetMuzzle. And for blocked apps, zero packets are read or processed – they vanish in the local blackhole.",
        bullet_1_title: "No Packet Parsing:",
        bullet_1_desc: "No CPU cycles wasted parsing TCP/UDP headers.",
        bullet_2_title: "Immediate UnknownHostException:",
        bullet_2_desc: "Blocked apps fail quickly instead of retrying TCP for 60s.",
        bullet_3_title: "Featherweight Memory:",
        bullet_3_desc: "Uses under 18 MB of RAM in background standby.",

        diag_header: "Traffic Routing Architecture",
        diag_allowed: "Allowed Apps (99%)",
        diag_blocked: "Muzzled Apps (Blocked)",

        badge_trust: "Privacy & Transparency",
        trust_title: "No backdoors. No analytics. No secrets.",
        trust_subtitle: "A firewall is only as good as the trust you place in its creator. NetMuzzle is designed to be completely auditable and privacy-respecting from the ground up.",
        trust_author_title: "Created by Marcin Stankiewicz",
        trust_author_desc: "A passion project built for simplicity and efficiency. No corporate tracking or data monetization agendas.",
        trust_license_title: "MIT Open Source License",
        trust_license_desc: "Full freedom to inspect, verify, build from source, or audit the Kotlin codebase on GitHub.",
        trust_offline_title: "Zero Analytics SDKs",
        trust_offline_desc: "No Google Firebase, no telemetry, no Facebook SDK, no crashlytics. The app is completely silent.",

        badge_faq: "Got Questions?",
        faq_title: "Frequently Asked Questions",
        faq_1_q: "Does NetMuzzle slow down my internet connection?",
        faq_1_a: "No, absolutely not. Unblocked applications completely bypass the NetMuzzle VPN interface at the Linux kernel level and communicate directly with your Wi-Fi or cellular network at native hardware speeds.",
        faq_2_q: "How does the once-a-day ad policy work?",
        faq_2_a: "We believe apps should not annoy users. An ad may only appear upon your first app opening of the day, or once upon your first opening after restarting your device. Subsequent toggles or visits throughout the day will never display ads.",
        faq_3_q: "Does NetMuzzle require Root access?",
        faq_3_a: "No root is needed. NetMuzzle uses Android's official VpnService API to create a local virtual blackhole on your device without modifying system partitions.",
        faq_4_q: "What happens if I uninstall the app?",
        faq_4_a: "When uninstalled, the Android operating system immediately closes the VPN session and clears all app rules. Your network routing returns 100% to factory defaults with zero residual files left on your device.",

        cta_bottom_title: "Take control of your device's network traffic today.",
        cta_bottom_desc: "Download the ultra-lightweight NetMuzzle APK or inspect the full source code on GitHub.",
        btn_download_now: "Download NetMuzzle APK",
        btn_star_github: "⭐ Star on GitHub",

        footer_tagline: "The ultra-lightweight Android app firewall. Muzzle unwanted traffic with zero CPU overhead.",
        footer_col_project: "Project",
        footer_releases: "Releases (APK)",
        footer_col_legal: "Legal & Privacy",
        footer_privacy: "Privacy Policy",
        footer_ads_policy: "Ads Policy"
    },

    pl: {
        nav_features: "Funkcje",
        nav_comparison: "Porównanie",
        nav_performance: "Wydajność i Bateria",
        nav_privacy: "Prywatność i Zaufanie",
        nav_faq: "Pytania (FAQ)",
        btn_download: "Pobierz APK",

        hero_badge: "Android Firewall i Bloker Reklam w Grach • Wydanie v1.1.0",
        hero_title: 'Załóż kaganiec na aplikacje i zablokuj reklamy. <span class="gradient-text">Zero drenażu baterii.</span>',
        hero_desc: "Selektywnie odcinaj internet lub wycinaj irytujące reklamy w grach bez lagów i bez obciążania procesora. Bez roota, zero wycieków DNS i pełna kontrola nad reklamami z nagrodami.",
        hero_cta_primary: "Pobierz darmowy plik APK (~2 MB)",
        hero_cta_github: "Kod źródłowy na GitHubie",

        stat_cpu: "CPU dla dozwolonego ruchu",
        stat_size: "Waga aplikacji",
        stat_offline: "Lokalnie na urządzeniu",

        mock_search: "Szukaj aplikacji…",
        mock_all: "Wszystkie (84)",
        mock_blocked_chip: "Chronione (2)",
        mock_try_hint: "👆 Interaktywny podgląd: poklikaj przełączniki wyżej!",

        badge_why: "Dlaczego NetMuzzle?",
        features_title: "Inna architektura. Zero marnowania energii.",
        features_subtitle: "Tradycyjne firewalle przepuszczają 100% ruchu przez aplikację, nagrzewając telefon. NetMuzzle działa odwrotnie – wpuszcza do tunelu wyłącznie programy z czarnej listy.",

        f0_title: "Blokada reklam w grach (Nowość v1.1.0)",
        f0_desc: "Wycina natrętne reklamy w grach mobilnych (Unity Ads, Google AdMob, AppLovin, IronSource) bez zrywania połączenia z serwerami gier, bez lagów i bez drenażu baterii.",
        f0_tag: "Selektywny filtr DNS",

        f1_title: "0% Zużycia CPU (Czarna Dziura)",
        f1_desc: "Zamiast parsować każdy pakiet w pętli procesora, NetMuzzle instruuje jądro systemu Linux, by tylko zablokowane programy wpadały do martwego tunelu. 99% ruchu omija aplikację z zerowym narzutem.",
        f1_tag: "Prawdziwa energooszczędność",

        f2_title: "Zero Wycieków DNS (IPv4 & IPv6)",
        f2_desc: "Zablokowane programy otrzymują martwe lokalne serwery DNS (10.0.0.1 oraz fd00::2). Próby połączeń są natychmiast ucinane na poziomie urządzenia bez wysyłania zapytań do Twojego operatora.",
        f2_tag: "Pancerna szczelność",

        f5_title: "Kapsuła 3 trybów i reklamy z nagrodami",
        f5_desc: "Wybieraj między trybami: Zezwalaj, Blokuj Ads oraz Kaganiec. Odblokuj reklamy wideo (np. Unity Ads), gdy chcesz odebrać darmowe monety lub życia w grze!",
        f5_tag: "Pełna kontrola gracza",

        f3_title: "Polityka „Zero-Spam” dla reklam",
        f3_desc: "Żadnych stałych banerów na ekranie, żadnych wyskakujących okien podczas konfiguracji. Reklama może pojawić się maksymalnie raz na dobę lub raz po restarcie telefonu. Podczas normalnego używania masz święty spokój.",
        f3_tag: "Szacunek do użytkownika",

        f4_title: "100% Zaufania i Open Source",
        f4_desc: "Projekt stworzony przez Marcina Stankiewicza na licencji MIT. Każda linijka kodu jest publiczna na GitHubie. Zero ukrytej telemetrii, zero serwerów śledzących, zero bibliotek analitycznych.",
        f4_tag: "Licencja MIT",

        badge_comp: "Zestawienie",
        comp_title: "Jak NetMuzzle wypada na tle innych",
        comp_subtitle: "Zobacz, dlaczego architektura selektywnej czarnej dziury deklasuje tradycyjne programy filtrujące pakiety.",
        th_feature: "Architektura i Funkcje",
        th_traditional: "Tradycyjne Firewalle",
        th_vpn: "Komercyjne VPN-y",
        row_cpu: "Zużycie CPU (Dopuszczony ruch)",
        row_battery: "Wpływ na drenaż baterii",
        row_dns: "Odporność na wycieki DNS",
        row_root: "Wymóg posiadania Roota",
        row_servers: "Wymagane serwery zewnętrzne",
        row_open: "Otwarte źródła (Open Source)",

        badge_perf: "Pod Lupą",
        perf_title: "Sekret zerowego drenażu baterii",
        perf_p1: "Gdy oglądasz YouTube lub pobierasz pliki, inne firewalle zmuszają każdy pakiet do przejścia przez pętlę ich aplikacji, co drenuje baterię i obciąża procesor.",
        perf_p2: "NetMuzzle stosuje odwrócony routing: przez funkcję addAllowedApplication() jądro Linuksa kieruje do tunelu WYŁĄCZNIE aplikacje z czarnej listy.",
        perf_p3: "Pozostałe 99% ruchu przechodzi przez Wi-Fi i LTE bezpośrednio na poziomie jądra, nie wybudzając NetMuzzle. A pakiety zablokowanych apek natychmiast giną w lokalnej próżni.",
        bullet_1_title: "Brak analizy pakietów:",
        bullet_1_desc: "Procesor nie marnuje ani jednego cyklu na czytanie nagłówków TCP/UDP.",
        bullet_2_title: "Błyskawiczny UnknownHostException:",
        bullet_2_desc: "Zablokowana apka od razu wie o braku sieci, zamiast ponawiać próby przez 60 sekund.",
        bullet_3_title: "Piórkowa waga w pamięci:",
        bullet_3_desc: "Zużywa poniżej 18 MB RAM podczas czuwania w tle.",

        diag_header: "Architektura Routingu Sieciowego",
        diag_allowed: "Dopuszczone aplikacje (99%)",
        diag_blocked: "Zablokowane z kagańcem",

        badge_trust: "Prywatność i Przejrzystość",
        trust_title: "Bez tylnych furtek. Bez analityki. Bez tajemnic.",
        trust_subtitle: "Firewall jest wart tyle, ile zaufanie do jego twórcy. NetMuzzle od pierwszej linijki kodu powstał z myślą o pełnej jawności i bezpieczeństwie.",
        trust_author_title: "Autor: Marcin Stankiewicz",
        trust_author_desc: "Niezależny projekt stworzony z pasji do prostoty i optymalizacji. Brak korporacyjnego śledzenia i sprzedaży danych.",
        trust_license_title: "Otwarta Licencja MIT",
        trust_license_desc: "Pełna swoboda weryfikacji, kompilacji ze źródeł i audytowania kodu w serwisie GitHub.",
        trust_offline_title: "Zero SDK Śledzących",
        trust_offline_desc: "Brak Google Firebase, brak telemetrii, brak Facebook SDK. Aplikacja nie wysyła niczego w sieć.",

        badge_faq: "Masz Pytania?",
        faq_title: "Często Zadawane Pytania",
        faq_1_q: "Czy NetMuzzle spowalnia moje połączenie z internetem?",
        faq_1_a: "Nie, w żadnym stopniu. Niezablokowane aplikacje całkowicie omijają tunel NetMuzzle na poziomie jądra systemu Linux i łączą się z Wi-Fi lub siecią komórkową z maksymalną przepustowością sprzętową.",
        faq_2_q: "Jak dokładnie działa polityka wyświetlania reklam raz na dobę?",
        faq_2_a: "Szanujemy Twój czas i spokój. Reklama może pojawić się wyłącznie przy pierwszym uruchomieniu aplikacji danego dnia lub raz po restarcie telefonu. Każde kolejne wejście w ciągu dnia nie wyświetla absolutnie żadnych reklam.",
        faq_3_q: "Czy NetMuzzle wymaga zrootowanego telefonu?",
        faq_3_a: "Nie. NetMuzzle korzysta z oficjalnego systemowego interfejsu VpnService w Androidzie, tworząc wirtualną pętlę zwrotną bez modyfikowania systemu operacyjnego.",
        faq_4_q: "Co się stanie, jeśli odinstaluję aplikację?",
        faq_4_a: "W momencie odinstalowania system Android natychmiast zamyka tunel VPN i kasuje reguły. Połączenia w telefonie od razu wracają w 100% do stanu fabrycznego bez żadnych pozostałości.",

        cta_bottom_title: "Przejmij pełną kontrolę nad siecią w swoim telefonie.",
        cta_bottom_desc: "Pobierz ultra-lekki plik APK NetMuzzle lub sprawdź otwarty kod źródłowy na GitHubie.",
        btn_download_now: "Pobierz plik APK NetMuzzle",
        btn_star_github: "⭐ Daj gwiazdkę na GitHubie",

        footer_tagline: "Ultra-lekki firewall na Androida. Załóż kaganiec na niechciany ruch bez obciążania procesora.",
        footer_col_project: "Projekt",
        footer_releases: "Pakiety wydań (APK)",
        footer_col_legal: "Prawne i Prywatność",
        footer_privacy: "Polityka Prywatności",
        footer_ads_policy: "Polityka Reklamowa"
    }
};

// State
let currentLang = 'en';
let mockMasterState = true;
let mockBlockedApps = new Set([1, 2]); // Initial blocked IDs: TikTok (1), Puzzle (2)

// Set Language function
function setLanguage(lang) {
    if (!translations[lang]) return;
    currentLang = lang;

    // Toggle active buttons
    document.querySelectorAll('.lang-btn').forEach(btn => btn.classList.remove('active'));
    const activeBtn = document.getElementById(`btn-${lang}`);
    if (activeBtn) activeBtn.classList.add('active');

    // Update DOM texts
    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (translations[lang][key]) {
            el.innerHTML = translations[lang][key];
        }
    });

    // Update html lang attr
    document.documentElement.lang = lang;

    // Save to localStorage
    try {
        localStorage.setItem('netmuzzle_lang', lang);
    } catch(e) {}

    // Update mock phone texts in current language
    updateMockStatusUI();
}

// Interactive Live Mockup Logic
function toggleMockMaster() {
    const toggle = document.getElementById('mock-master-toggle');
    mockMasterState = toggle.checked;
    updateMockStatusUI();
}

function toggleMockApp(appId) {
    if (mockBlockedApps.has(appId)) {
        mockBlockedApps.delete(appId);
        document.getElementById(`app-item-${appId}`).classList.remove('blocked');
    } else {
        mockBlockedApps.add(appId);
        document.getElementById(`app-item-${appId}`).classList.add('blocked');
    }
    updateMockStatusUI();
}

function updateMockStatusUI() {
    const card = document.getElementById('mock-status-card');
    const title = document.getElementById('mock-card-title');
    const desc = document.getElementById('mock-card-desc');
    const statusText = document.getElementById('mock-status-text');
    const chip = document.getElementById('mock-blocked-chip');

    const count = mockBlockedApps.size;
    const isPl = currentLang === 'pl';

    chip.textContent = isPl ? `Zablokowane (${count})` : `Blocked (${count})`;

    if (!mockMasterState) {
        card.className = 'mock-status-card disabled';
        statusText.textContent = isPl ? 'WYŁĄCZONY' : 'DISABLED';
        statusText.style.color = '#64748B';
        title.textContent = isPl ? 'WYŁĄCZONY' : 'DISABLED';
        desc.textContent = isPl ? 'Ruch sieciowy nie jest blokowany' : 'Network traffic is unblocked';
    } else if (count === 0) {
        card.className = 'mock-status-card standby';
        statusText.textContent = isPl ? 'CZUWANIE' : 'STANDBY';
        statusText.style.color = '#F59E0B';
        title.textContent = isPl ? 'TRYB CZUWANIA' : 'STANDBY MODE';
        desc.textContent = isPl ? 'Brak apek na czarnej liście (ruch wolny)' : 'No apps blocked (traffic unblocked)';
    } else {
        card.className = 'mock-status-card';
        statusText.textContent = isPl ? 'AKTYWNY' : 'ACTIVE';
        statusText.style.color = '#00E5FF';
        title.textContent = isPl ? 'BLOKADA AKTYWNA' : 'BLOCK ACTIVE';
        desc.textContent = isPl ? `Zablokowano ruch dla ${count} aplikacji` : `Muzzled traffic for ${count} apps`;
    }
}

// FAQ Accordion Toggle
function toggleFaq(btn) {
    const item = btn.parentElement;
    const answer = item.querySelector('.faq-answer');
    const isActive = item.classList.contains('active');

    // Close all other items
    document.querySelectorAll('.faq-item').forEach(el => {
        el.classList.remove('active');
        el.querySelector('.faq-answer').style.maxHeight = null;
    });

    if (!isActive) {
        item.classList.add('active');
        answer.style.maxHeight = answer.scrollHeight + 30 + "px";
    }
}

// Initialize on Load
document.addEventListener('DOMContentLoaded', () => {
    // Detect language preference
    let savedLang = 'en';
    try {
        savedLang = localStorage.getItem('netmuzzle_lang');
    } catch(e) {}

    if (!savedLang) {
        if (navigator.language && navigator.language.startsWith('pl')) {
            savedLang = 'pl';
        } else {
            savedLang = 'en';
        }
    }

    setLanguage(savedLang);
});
