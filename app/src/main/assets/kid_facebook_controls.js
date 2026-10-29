/* iOS main a8b6ff4 FacebookConfig + FacebookSelectorLabels.
 * Kid-only Android import: parent flags, shared lifecycle hooks, opaque identifiers, no diagnostic emitters.
 * Personal mode continues to use FacebookScriptGenerator. */
(function(config){
if(config.hosts.indexOf(location.hostname.toLowerCase()) < 0) return;
var K=Symbol.for(config.bridge); var S=window[K] || (window[K]={});
(function() {
    'use strict';

    if (S.fbk) return;
    S.fbk = true;

    var hideHome = config.effective.fb_hideHome === true;
    var hidePeople = config.effective.fb_hidePeople === true;
    var hideMessenger = config.effective.fb_hideMessenger === true;
    var hideNotifications = config.effective.fb_hideNotifications === true;
    var hideMarketplace = config.effective.fb_hideMarketplace === true;
    var marketplaceMode = config.effective.fb_marketplaceMode === true;
    var onlyMyNetwork = false;

    // ==================== [FB-I18N-2026-09-17] STRUCTURAL NAV MATCHING ====================
    // Facebook's bottom nav / tab bar is anchors whose href PATH is an untranslated route
    // ('/marketplace', '/friends', '/notifications', '/menu'…). Classifying a tab by its path
    // works in every language; comparing its TEXT works only in English. Everything below
    // matches by path first and only uses text as an EXTRA POSITIVE signal — never as the
    // complement of one, which is what broke Marketplace mode (see the P0 note further down).
    var SL_NAV_ALLOW = ['/marketplace','/item','/commerce','/messages','/t/','/menu','/bookmarks','/settings','/search','/login','/checkpoint','/recover','/profile.php','/help'];
    var SL_NAV_DENY = ['/home.php','/home','/friends','/notifications','/watch','/reel','/reels','/video','/videos','/groups','/events','/gaming','/pages','/stories','/dating','/live','/games','/jobs','/fundraisers','/memories','/birthdays','/photo','/story.php','/weather'];
    var SL_NAV_HOME = ['/','','/home.php','/home'];
    var SL_NAV_ALLOW_WORDS = ['marketplace','messages','message','messenger','chats','menu','search'];
    var SL_NAV_DENY_WORDS = ['home','news feed','friends','notification','reels','watch','video'];
    var SL_APP_LINK_SEL = 'a[href*="apps.apple.com" i],a[href*="itunes.apple.com" i],a[href*="play.google.com" i],a[href*="fb://" i],a[href*="fb-messenger://" i],a[href*="://m.me/" i],a[href*="fb.me/" i]';

    // The route an element navigates to: its own href, else the href of the anchor it wraps
    // or sits inside. null = "we can't tell" — every caller treats null as DON'T TOUCH.
    function _slFbNavPath(el) {
        try {
            var a = (el && el.getAttribute && el.getAttribute('href')) ? el : null;
            if (!a && el) {
                a = (el.querySelector && el.querySelector('a[href]')) ||
                    (el.closest && el.closest('a[href]')) || null;
            }
            if (!a) return null;
            var h = a.getAttribute('href') || '';
            if (!h || h.charAt(0) === '#' || h.indexOf('javascript') === 0) return null;
            try { return new URL(h, window.location.origin).pathname.toLowerCase(); }
            catch (e) { return h.toLowerCase().split('?')[0]; }
        } catch (e) { return null; }
    }
    function _slFbPathIn(p, list) {
        if (p === null || p === undefined) return false;
        for (var i = 0; i < list.length; i++) { if (p.indexOf(list[i]) === 0) return true; }
        return false;
    }
    function _slFbPathIsHome(p) {
        if (p === null || p === undefined) return false;
        for (var i = 0; i < SL_NAV_HOME.length; i++) { if (p === SL_NAV_HOME[i]) return true; }
        return false;
    }
    // aria-label + text of a nav control, lowercased. Nav labels are short, so this is an
    // exact-ish surface — we never run these word lists over post text.
    function _slFbNavMeta(el) {
        try {
            return (((el.getAttribute && el.getAttribute('aria-label')) || '') + ' ' +
                    ((el.textContent || ''))).trim().toLowerCase();
        } catch (e) { return ''; }
    }
    // Plain "does this (already lowercased) string contain any of these words" test.
    function _slFbWordIn(s, words) {
        if (!s || !words || !words.length) return false;
        for (var i = 0; i < words.length; i++) {
            if (words[i] && s.indexOf(words[i]) !== -1) return true;
        }
        return false;
    }
    function _slFbMetaHasWord(el, words) {
        if (!words || !words.length) return false;
        var m = _slFbNavMeta(el);
        if (!m) return false;
        for (var i = 0; i < words.length; i++) {
            if (words[i] && m.indexOf(words[i]) !== -1) return true;
        }
        return false;
    }
    function _slFbNavControls() {
        return document.querySelectorAll('[role="tab"], [role="tablist"] a, nav a, [role="navigation"] a');
    }
    // Hide every nav control whose ROUTE is one of `paths` (or, if `home`, the home route).
    // `words` is an optional extra positive signal. mode 'v' = visibility:hidden (keeps the
    // nav slot rendering clean-white per [FB-REELS-NAV-WHITE-2026-07-25]), else display:none.
    function _slFbHideNavByPath(paths, words, mode, home) {
        var tabs = _slFbNavControls();
        for (var i = 0; i < tabs.length; i++) {
            var el = tabs[i];
            var p = _slFbNavPath(el);
            var hit = _slFbPathIn(p, paths) || (home === true && _slFbPathIsHome(p));
            if (!hit) hit = _slFbMetaHasWord(el, words);
            if (!hit) continue;
            if (mode === 'v') el.style.setProperty('visibility', 'hidden', 'important');
            else el.style.setProperty('display', 'none', 'important');
        }
    }
    // Does this subtree advertise the native app / a store listing? That is the app-promo
    // fingerprint in EVERY language — no text needed.
    function _slFbHasAppLink(root) {
        try { return !!(root && root.querySelector && root.querySelector(SL_APP_LINK_SEL)); }
        catch (e) { return false; }
    }

    // Detect if marketplace mode changed since last load — force reload
    try {
        var prevMode = sessionStorage.getItem('__NAMESPACE__5');
        var currMode = marketplaceMode ? '1' : '0';
        if (prevMode !== null && prevMode !== currMode) {
            sessionStorage.setItem('__NAMESPACE__5', currMode);
            if (marketplaceMode) {
                window.location.replace('https://m.facebook.com/marketplace/');
            } else {
                window.location.replace('https://m.facebook.com/');
            }
            return;
        }
        sessionStorage.setItem('__NAMESPACE__5', currMode);
    } catch(e) {}


    // [FB-MARKETPLACE-POPUP-DISMISS-2026-07-06] Facebook shows an
    // "Experience the best of Marketplace on Facebook app" app-promo
    // bottom sheet on Marketplace listings. Auto-dismiss it by clicking
    // its own "Not now" — scoped so we only click the button whose popup
    // actually mentions Marketplace (never hijacks an unrelated Not now).
    // Revert: delete this block + the two timers below.
    // [FB-I18N-2026-09-17] The dismiss LABEL ('not now') is English-only (capture pending),
    // but the SCOPE is now structural too: the promo is identified either by its English
    // phrase (unchanged) OR by the fact that the ancestor's own subtree links to the native
    // app / a store listing. Without that second test the whole function was dead in every
    // other language even once the label is captured, since the scope phrase was English as
    // well. The scope stays tight — app-link OR the phrase, inside 8 hops of the button.
    var SL_MKT_SCOPE = ['best of Marketplace'];
    var SL_APP_DISMISS = ['not now','stay on mobile site','maybe later','stay in browser','continue in browser'];
    function _slDismissMktPopup() {
        try {
            var btns = document.querySelectorAll('div[role="button"], button, a, span');
            for (var i = 0; i < btns.length; i++) {
                var t = (btns[i].textContent || '').trim().toLowerCase();
                if (SL_APP_DISMISS.indexOf(t) === -1) continue;
                var el = btns[i], hops = 0, ok = false;
                while (el && hops < 8) {
                    var txt = el.textContent || '';
                    for (var s = 0; s < SL_MKT_SCOPE.length; s++) {
                        if (txt.indexOf(SL_MKT_SCOPE[s]) !== -1) { ok = true; break; }
                    }
                    if (ok || _slFbHasAppLink(el)) { ok = true; break; }
                    el = el.parentElement; hops++;
                }
                if (ok) { btns[i].click(); return; }
            }
        } catch (e) {}
    }
    setInterval(_slDismissMktPopup, 700);
    setTimeout(_slDismissMktPopup, 400);



    var style = document.createElement('style');
    style.id = '__NAMESPACE__3';
    style.textContent = `
        /* Enable text selection & copy/paste */
        *, *::before, *::after {
            -webkit-user-select: text !important;
            user-select: text !important;
            -webkit-touch-callout: default !important;
        }

        /* [FB-LOGIN-UPSELL-HIDE-2026-08-09] "Get Facebook for iOS and
           browse faster." banner on the login page (user screenshot).
           Structural, not text-based: any link pointing at the App
           Store / the native app is an upsell we never want anywhere
           in the FB webview. */
        a[href*="apps.apple.com" i],
        a[href*="itunes.apple.com" i],
        a[href*="app_store" i],
        a[href*="fb://" i],
        #mobile_login_bar { display: none !important; }

        /* Reels and Watch blocking.
           [FB-I18N-2026-09-17] The three aria-label rules here (and the "video" one below)
           are ENGLISH ONLY — capture pending, since FB only renders the tab bar behind a
           login. The href rules beside them are the language-independent half, and
           _slFbHideNavByPath() in blockContent() does the same job for the tab itself by its
           route. Do NOT replace the aria rules with translations without a live capture. */
        [aria-label*="reels" i], div[role="tab"][aria-label*="reels" i] { display: none !important; }
        a[href*="/reel/"], a[href*="/reels"] { display: none !important; }
        div[role="tab"][aria-label*="watch" i] { display: none !important; }
        /* [FB-REELS-NAV-WHITE-2026-07-25] Newer FB labels the reels tab "Video",
           which our reels/watch selectors miss → it shows as a grey box in the nav.
           Match it and use visibility:hidden (NOT display:none) so the slot stays
           but renders as the nav bar's clean WHITE - no grey placeholder, and the
           other tabs don't shift. */
        [role="tab"][aria-label*="video" i] { visibility: hidden !important; }
        a[href*="/gaming"] { display: none !important; }

        /* [FB-INLINE-REELS-HIDE-V3-2026-06-18] V2 hid the
           aria element directly via CSS, which made
           getBoundingClientRect() return 0 on all ancestors
           and broke the JS walker's size-based card
           detection - left the "Reels" header + 3-dot menu +
           empty body stub visible and clickable. V3 lets the
           walker run FIRST against an unmodified DOM, mark
           the card container with [data-__NAMESPACE__0="1"],
           and CSS hides it via the attribute selector. The
           walker uses a "Reels header sibling" heuristic
           instead of size - locates the ancestor where the
           video aria-element and a child with literal "Reels"
           text live as siblings. That's the card root. */
        [data-__NAMESPACE__0="1"] {
            display: none !important;
        }

        /* [FB-ONLY-MY-NETWORK-2026-07-19] Posts from outside your network
           (flagged by a Follow/Join header CTA) collapse entirely. */
        [data-__NAMESPACE__1="1"] { display: none !important; }

        /* App banner blocking */
        [data-sigil*="app-install"],
        [data-sigil*="install-banner"],
        [data-sigil="app_banner"],
        [data-gt*="app_install"] { display: none !important; }

        a[href*="itunes.apple.com"],
        a[href*="play.google.com"] { display: none !important; }

        /* [FB-INSTALL-BANNER-2026-06-18] "Facebook is better on
           the app" install-promo modal (per user screenshot).
           FB renders this as a bottom-anchored dialog containing
           a "Get app" button that links to the App Store. The
           existing app-install selectors above don't catch the
           modern dialog wrapper. :has() walks up from the
           apps.apple.com / play.google.com link to the role=dialog
           container and hides it. */
        [role="dialog"]:has(a[href*="apps.apple.com"]),
        [role="dialog"]:has(a[href*="play.google.com"]),
        [role="dialog"]:has(a[href*="itunes.apple.com"]),
        div[aria-modal="true"]:has(a[href*="apps.apple.com"]),
        div[aria-modal="true"]:has(a[href*="play.google.com"]) {
            display: none !important;
        }

        [class*="upsell"], [class*="Upsell"] { display: none !important; }

        body { overflow: auto !important; }

        ${marketplaceMode ? `
            /* MARKETPLACE MODE: Hide back button and navigation escape routes */
            [data-sigil="MBackNavBarClick"],
            [data-sigil*="back"],
            header a[aria-label*="back" i],
            header a[aria-label*="Back" i],
            [class*="backButton" i],
            [class*="back-button" i],
            [class*="BackButton" i],
            [class*="navBack" i],
            [class*="nav-back" i] {
                display: none !important;
                pointer-events: none !important;
                visibility: hidden !important;
            }
            /* Hide Facebook logo that links to home */
            header a[href="/"] {
                pointer-events: none !important;
            }
        ` : ''}
    `;

    (document.head || document.documentElement).appendChild(style);

    // [FB-LOGIN-UPSELL-HIDE-2026-08-09 r2] The login page's "Get
    // Facebook for iOS and browse faster." banner survived the CSS
    // href selectors — its anchor doesn't point at the App Store
    // directly. Text-prefix match instead (same capture-don't-translate
    // pattern as TikTok's "Not now" dismissal; webviews are pinned
    // English). Hides the nearest block container so the icon + hairline
    // row go with it. Marks processed nodes; observer + interval keep it
    // applied across FB's SPA re-renders.
    // ⭐ [FB-I18N-2026-09-17] The ONLY Facebook string this port could capture for all nine
    // languages: m.facebook.com honours ?locale=<fb_locale> while LOGGED OUT, and this banner
    // is on the logged-out login page. Values are FB's own translations, read out by a
    // structural anchor (the bloks text node whose enclosing action logs
    // upsell_type="open_app_upsell") — see docs/facebook-web-locale-vocab-2026-09-17.json.
    // COLLISION: full sentences, matched as a PREFIX on a single <a>/[role=link]'s own text.
    // They cannot collide with a post, a group name or a profile name. SAFE.
    var SL_FB_UPSELL = ['Get Facebook for iOS and browse faster.','Descarga Facebook para iOS y navega más rápido.','Installez Facebook pour iOS et naviguez plus vite.','Hol dir Facebook für iOS, damit du schneller browsen kannst.','Baixe o Facebook para iOS e navegue com mais velocidade.','iOS用Facebookで高速ブラウジング。','احصل على فيسبوك لنظام iOS واستمتع بتصفّح أسرع.','iOS용 Facebook을 다운로드하여 더 빠르게 이용해보세요.','Scarica Facebook per iOS per navigare più velocemente.'];
    function slHideFbUpsell() {
        document.querySelectorAll('a:not([data-__NAMESPACE__2]), [role="link"]:not([data-__NAMESPACE__2])').forEach(function(a) {
            a.setAttribute('data-__NAMESPACE__2', '1');
            var t = (a.textContent || '').trim();
            // The three original English prefixes stay byte-for-byte ('Get Facebook for' is
            // SHORTER than the captured English sentence, so dropping it would narrow English
            // behaviour — it must remain). 'Get the…' / 'Open the…' are capture pending.
            var hit = t.indexOf('Get Facebook for') === 0 ||
                      t.indexOf('Get the Facebook app') === 0 ||
                      t.indexOf('Open the Facebook app') === 0;
            for (var i = 0; !hit && i < SL_FB_UPSELL.length; i++) {
                if (t.indexOf(SL_FB_UPSELL[i]) === 0) hit = true;
            }
            if (hit) {
                var row = a.closest('div') || a;
                row.style.setProperty('display', 'none', 'important');
            }
        });
    }
    slHideFbUpsell();
    try {
        var slUpsellObserver = new MutationObserver(function() { slHideFbUpsell(); });
        var slUpsellMount = function() {
            if (document.body) {
                slUpsellObserver.observe(document.body, { childList: true, subtree: true });
            }
        };
        if (document.body) { slUpsellMount(); }
        else { document.addEventListener('DOMContentLoaded', slUpsellMount); }
    } catch (e) {}
    setInterval(slHideFbUpsell, 2000);

    // ==================== MARKETPLACE MODE ====================
    // [FB-I18N-2026-09-17] '/menu' + '/bookmarks' ADDED. ⚠️ Judgement call — revert by
    // deleting those two entries.
    // WHY: the P0 fix below stops Marketplace mode from hiding the Menu tab in other
    // languages, but keeping the tab visible is hollow if tapping it bounces straight back to
    // Marketplace — which is what this allow-list did, in EVERY language including English.
    // '/settings' was already allowed, and on FB mobile web '/menu' (and '/bookmarks') is the
    // only route TO settings and log out, so this opens no new destination: every other menu
    // entry is still blocked by this same allow-list, the click interceptor and
    // blockedPathPatterns. It just makes the documented escape route actually work.
    var ALLOWED_PATHS_MARKETPLACE = ['/marketplace', '/messages', '/login', '/checkpoint', '/recover', '/settings', '/menu', '/bookmarks'];

    function isAllowedInMarketplaceMode(url) {
        if (!marketplaceMode) return true;
        var path = '';
        try {
            var parsed = new URL(url, window.location.origin);
            path = parsed.pathname.toLowerCase();
        } catch(e) {
            path = (url || '').toLowerCase();
        }
        if (path === '/' || path === '') return false;
        for (var i = 0; i < ALLOWED_PATHS_MARKETPLACE.length; i++) {
            if (path.indexOf(ALLOWED_PATHS_MARKETPLACE[i]) === 0) return true;
        }
        if (path.match(/^\/item\//) || path.match(/^\/commerce\//)) return true;
        if (path.match(/^\/profile\.php/)) return true;
        return false;
    }

    function enforceMarketplaceMode() {
        if (!marketplaceMode) return;
        var currentPath = window.location.pathname.toLowerCase();
        if (currentPath.indexOf('/login') === 0 || currentPath.indexOf('/checkpoint') === 0 ||
            currentPath.indexOf('/recover') === 0) return;
        if (!isAllowedInMarketplaceMode(window.location.href)) {
            window.location.replace('https://m.facebook.com/marketplace/');
        }
    }

    // ==================== APP URL INTERCEPTION ====================
    // Intercept messenger/app URLs and redirect to web messages
    document.addEventListener('click', function(e) {
        var el = e.target;
        while (el && el !== document.body) {
            var href = el.getAttribute ? (el.getAttribute('href') || '') : '';

            // Catch messenger app schemes — redirect to web messages
            if (href.startsWith('fb://') || href.startsWith('fb-messenger://') ||
                href.startsWith('facebook://') || href.startsWith('messenger://') ||
                href.includes('://m.me/') || href.includes('messenger.com')) {
                e.preventDefault();
                e.stopPropagation();
                window.location.href = '/messages/';
                return false;
            }

            // Catch links to messenger.com and redirect to web messages
            if (href.includes('www.messenger.com') || href.includes('m.messenger.com')) {
                e.preventDefault();
                e.stopPropagation();
                window.location.href = '/messages/';
                return false;
            }

            if (href.includes('itunes.apple.com') || href.includes('apps.apple.com') ||
                href.includes('play.google.com')) {
                e.preventDefault();
                e.stopPropagation();
                return false;
            }

            if (href.includes('/reel') || href.includes('/watch')) {
                e.preventDefault();
                e.stopPropagation();
                return false;
            }

            // Marketplace mode: block clicks to non-allowed pages
            if (marketplaceMode && href && !href.startsWith('#') && !href.startsWith('javascript')) {
                if (!isAllowedInMarketplaceMode(href)) {
                    e.preventDefault();
                    e.stopPropagation();
                    return false;
                }
            }

            el = el.parentElement;
        }
    }, true);

    // Also intercept clicks on back-arrow elements in marketplace mode
    if (marketplaceMode) {
        document.addEventListener('click', function(e) {
            var el = e.target;
            // Walk up to find the clickable parent
            var clickable = el.closest('a, button, [role="button"]');
            if (!clickable) return;
            var rect = clickable.getBoundingClientRect();
            // If it's in the top-left corner area, block it unless it's marketplace/messages
            if (rect.top < 60 && rect.left < 100) {
                var href = (clickable.getAttribute('href') || '').toLowerCase();
                if (!href.includes('/marketplace') && !href.includes('/messages') && !href.includes('/search')) {
                    e.preventDefault();
                    e.stopPropagation();
                    return false;
                }
            }
        }, true);
    }

    // [FB-MP-INBOX-ESCAPE-2026-07-10] Marketplace mode hides FB's back
    // button + nav, so opening the (Messenger-based) marketplace inbox
    // could STRAND the user on /messages/ with no route back to
    // Marketplace — they had to restart the app just to reach their feed.
    // Inject an always-present floating "‹ Marketplace" pill on any
    // non-marketplace (and non-auth) page that navigates to /marketplace/
    // — a guaranteed escape. It's a bare <div> (no href), so neither the
    // click-blocker nor the top-left back-blocker above touches it.
    if (marketplaceMode) {
        function _slSyncMpEscape() {
            var path = window.location.pathname.toLowerCase();
            var hide = path.indexOf('/marketplace') === 0 ||
                       path.indexOf('/login') === 0 ||
                       path.indexOf('/checkpoint') === 0 ||
                       path.indexOf('/recover') === 0;
            var el = document.getElementById('__NAMESPACE__6');
            if (hide) { if (el) el.remove(); return; }
            if (el) return;
            el = document.createElement('div');
            el.id = '__NAMESPACE__6';
            el.textContent = '‹ Marketplace';
            el.style.cssText = 'position:fixed;left:12px;bottom:90px;z-index:2147483647;' +
                'background:#1877F2;color:#fff;font:600 14px -apple-system,BlinkMacSystemFont,sans-serif;' +
                'padding:10px 16px;border-radius:22px;box-shadow:0 3px 12px rgba(0,0,0,.35);cursor:pointer;';
            el.addEventListener('click', function() {
                window.location.href = 'https://m.facebook.com/marketplace/';
            });
            (document.body || document.documentElement).appendChild(el);
        }
        _slSyncMpEscape();
        setInterval(_slSyncMpEscape, 1500);
        window.addEventListener('popstate', function() { setTimeout(_slSyncMpEscape, 100); });
    }

    function neutralizeAppLinks() {
        document.querySelectorAll('a').forEach(function(link) {
            var href = link.getAttribute('href') || '';
            // Rewrite app scheme URLs to web messages
            if (href.startsWith('fb://') || href.startsWith('fb-messenger://') ||
                href.startsWith('facebook://') || href.startsWith('messenger://') ||
                href.includes('://m.me/')) {
                link.setAttribute('href', '/messages/');
                link.removeAttribute('target');
            }
            // Rewrite messenger.com links to web messages
            if (href.includes('messenger.com') && !href.includes('m.facebook.com') && !href.includes('/messages/')) {
                link.setAttribute('href', '/messages/');
                link.removeAttribute('target');
            }

        });
    }

    // [FB-I18N-2026-09-17] The CTA labels are English-only (capture pending — FB only renders
    // this sheet behind a login, and there is no public per-locale FB i18n bundle). What now
    // carries this in every language is STRUCTURAL: the CSS above hides any [role=dialog] /
    // [aria-modal] that :has() an App Store / Play link, and _slFbHideInstallBanner below
    // fingerprints the install button by its LINK rather than its text. This text pass is the
    // remaining English-only belt.
    var SL_APP_CTA = ['open app','use app','get the app','use the app','open in app','continue in app','open in facebook'];
    function removeAppBanners() {
        document.querySelectorAll('button, div[role="button"], a').forEach(function(el) {
            var text = (el.textContent || '').trim().toLowerCase();
            if (SL_APP_CTA.indexOf(text) !== -1) {
                var container = el.closest('[role="dialog"]') || el.closest('[data-sigil="MBottomSheet"]') || el.closest('[class*="modal" i]');
                if (container) {
                    container.style.setProperty('display', 'none', 'important');
                } else {
                    el.style.setProperty('display', 'none', 'important');
                }
            }
        });



        if (document.body) {
            var bodyOverflow = window.getComputedStyle(document.body).overflow;
            if (bodyOverflow === 'hidden') {
                document.body.style.setProperty('overflow', 'auto', 'important');
            }
        }
    }

    function blockContent() {
        neutralizeAppLinks();
        removeAppBanners();
        enforceMarketplaceMode();
        _slFbFilterNonNetwork();   // [FB-ONLY-MY-NETWORK-2026-07-19]

        // On messages page — aggressively block messenger app prompts
        var onMessagesPage = window.location.pathname.includes('/messages') ||
            false;
        if (onMessagesPage) {
            // ⚠️ [FB-I18N-2026-09-17] REWRITTEN — the 2026-09-17 audit flagged this as too
            // broad even in English. It clicked ANY button on /messages whose text was
            // 'cancel' or 'ok', which is not a Messenger app promo at all: it auto-dismissed
            // the USER'S OWN dialogs (delete-conversation confirm, leave-group, a photo
            // picker…) the moment they appeared, and 'ok' could confirm one.
            //
            // Two fixes:
            //   1. The bare 'cancel' and 'ok' matches are REMOVED, not translated. (FB's
            //      localized Cancel labels ARE captured — FacebookSelectorLabels.dialogCancel
            //      — and deliberately left unwired for exactly this reason.)
            //   2. STRUCTURAL SCOPE: we only click inside a container that is provably the
            //      Messenger/app interstitial — a dialog / bottom sheet / modal whose own
            //      subtree links to the native app or a store listing (_slFbHasAppLink).
            //      That test is language-independent, so the scope now holds in every
            //      language; only the button LABEL is still English (capture pending).
            var SL_MSGR_DISMISS = ['stay on mobile site','not now','stay on this page','continue to mobile site','use messenger on this browser','see messages on facebook'];
            document.querySelectorAll('[role="dialog"], [aria-modal="true"], [data-sigil="MBottomSheet"], [class*="modal" i]').forEach(function(box) {
                if (!_slFbHasAppLink(box)) return;
                box.querySelectorAll('a, button, [role="button"]').forEach(function(el) {
                    if (el._slClicked) return;
                    var text = (el.textContent || '').trim().toLowerCase();
                    if (SL_MSGR_DISMISS.indexOf(text) === -1) return;
                    el._slClicked = true;
                    el.click();
                });
            });

        }

        // REELS AND WATCH BLOCKING
        document.querySelectorAll('[aria-label*="reels" i]').forEach(function(el) {
            el.style.display = 'none';
        });
        document.querySelectorAll('a[href*="/reel/"], a[href*="/reels"]').forEach(function(el) {
            el.style.display = 'none';
            var container = el.closest('[data-mcomponent="MContainer"]');
            if (container) container.style.display = 'none';
        });
        document.querySelectorAll('[aria-label*="watch" i]').forEach(function(el) {
            if (el.getAttribute('role') === 'tab') el.style.display = 'none';
        });
        // [FB-REELS-NAV-WHITE-2026-07-25] Newer FB labels the reels tab "Video" —
        // visibility:hidden (not display:none) keeps the nav slot but renders it
        // clean white, so there's no grey placeholder and the tabs don't shift.
        document.querySelectorAll('[role="tab"][aria-label*="video" i]').forEach(function(el) {
            el.style.setProperty('visibility', 'hidden', 'important');
        });
        // [FB-I18N-2026-09-17] STRUCTURAL twin for the three aria-label passes above. The
        // reels / Watch / Video tab aria-labels are localized ("Reels" stays, but
        // "Watch"/"Video" become "Vidéo", "動画", "فيديو"…), so on a non-English Facebook the
        // reels tab stayed fully visible AND tappable. Matching the tab's ROUTE instead works
        // in every language with nothing to capture. visibility:hidden (not display:none)
        // keeps the nav slot rendering clean-white per [FB-REELS-NAV-WHITE-2026-07-25], and a
        // visibility:hidden element is not hit-testable, so the tab is dead either way. In
        // English this is a no-op — the aria passes have already hidden these tabs.
        _slFbHideNavByPath(['/reel', '/reels'], [], 'v');
        _slFbHideNavByPath(['/watch'], [], 'v');
        _slFbHideNavByPath(['/video', '/videos'], [], 'v');

        // VIDEO HANDLING
        document.querySelectorAll('video').forEach(function(video) {
            if (!video.dataset.__NAMESPACE__115) {
                video.dataset.__NAMESPACE__115 = 'true';
                video.addEventListener('click', function(ev) {
                    ev.stopPropagation();
                    video.paused ? video.play() : video.pause();
                }, true);
            }
        });

        // ==================== MARKETPLACE MODE: HIDE NON-MARKETPLACE NAV ====================
        if (marketplaceMode) {
            // Strategy 1: data-sigil based
            document.querySelectorAll('[data-sigil*="back"], [data-sigil="MBackNavBarClick"]').forEach(function(el) {
                if (el.closest('[data-marketplace]')) return;
                el.style.setProperty('display', 'none', 'important');
                el.style.setProperty('pointer-events', 'none', 'important');
            });

            // Strategy 2: header links
            document.querySelectorAll('header a, [role="banner"] a').forEach(function(el) {
                var href = (el.getAttribute('href') || '').toLowerCase();
                var ariaLabel = (el.getAttribute('aria-label') || '').toLowerCase();
                // [FB-I18N-2026-09-17] `ariaLabel.includes('facebook')` missed Arabic, where FB
                // renders its own wordmark as فيسبوك (✅ captured). `'back'` is English-only
                // (capture pending) — the data-sigil + position strategies below carry it.
                if (href === '/' || href === 'https://m.facebook.com/' ||
                    _slFbWordIn(ariaLabel, ['back']) ||
                    _slFbWordIn(ariaLabel, ['facebook','فيسبوك'])) {
                    if (!href.includes('/marketplace') && !href.includes('/messages')) {
                        el.style.setProperty('pointer-events', 'none', 'important');
                        el.style.setProperty('visibility', 'hidden', 'important');
                    }
                }
            });

            // Strategy 3: Top bar first element
            var topBar = document.querySelector('div[data-sigil="top-bar"], header, [role="banner"]');
            if (topBar) {
                var firstLink = topBar.querySelector('a, [role="button"]');
                if (firstLink) {
                    var href = (firstLink.getAttribute('href') || '').toLowerCase();
                    // [FB-I18N-2026-09-17] Route-based keep guard, so a localized
                    // Marketplace/Menu/Search control in the first top-bar slot survives.
                    var keepFirst = href.includes('/marketplace') || href.includes('/messages') ||
                                    href.includes('/search') ||
                                    _slFbPathIn(_slFbNavPath(firstLink), SL_NAV_ALLOW) ||
                                    _slFbMetaHasWord(firstLink, SL_NAV_ALLOW_WORDS);
                    if (!keepFirst) {
                        firstLink.style.setProperty('pointer-events', 'none', 'important');
                        firstLink.style.setProperty('visibility', 'hidden', 'important');
                    }
                }
            }

            // Strategy 4: Position-based — find ALL clickable elements in the
            // top-left corner of the screen and hide/disable them if they
            // navigate away from marketplace
            document.querySelectorAll('a, [role="button"], button').forEach(function(el) {
                if (el._slMktBackChecked) return;
                var rect = el.getBoundingClientRect();
                if (rect.width < 5 || rect.height < 5) return;
                if (rect.top > 60 || rect.left > 100) return;
                if (rect.bottom < 0 || rect.right < 0) return;
                var href = (el.getAttribute('href') || '').toLowerCase();
                var ariaLabel = (el.getAttribute('aria-label') || '').toLowerCase();
                if (href.includes('/marketplace') || href.includes('/messages') || href.includes('/search')) return;
                // [FB-I18N-2026-09-17] Same fail-closed family as the P0 above: these two
                // English aria tests are KEEP guards, so in another language the user's own
                // Marketplace / Search button in the top-left got hidden. Route-based keep
                // guard first (language-independent), then the English words.
                if (_slFbPathIn(_slFbNavPath(el), SL_NAV_ALLOW)) return;
                if (_slFbMetaHasWord(el, SL_NAV_ALLOW_WORDS)) return;
                if (ariaLabel.includes('marketplace') || ariaLabel.includes('search')) return;
                // This is a back/home button in the top-left — kill it
                el._slMktBackChecked = true;
                el.style.setProperty('pointer-events', 'none', 'important');
                el.style.setProperty('visibility', 'hidden', 'important');
            });

            // Strategy 5: SVG arrows in top area
            document.querySelectorAll('svg').forEach(function(svg) {
                var rect = svg.getBoundingClientRect();
                if (rect.top > 60 || rect.left > 100 || rect.width < 5) return;
                var parent = svg.closest('a') || svg.closest('[role="button"]') || svg.closest('button');
                if (!parent) return;
                var href = (parent.getAttribute('href') || '').toLowerCase();
                if (href.includes('/marketplace') || href.includes('/messages') || href.includes('/search')) return;
                parent.style.setProperty('pointer-events', 'none', 'important');
                parent.style.setProperty('visibility', 'hidden', 'important');
            });

            // ⭐ [FB-I18N-2026-09-17] P0 FIX — Marketplace mode used to hide Menu, settings
            // and logout for every non-English account.
            //
            // THE BUG: this pass classified the bottom-nav tabs by their TEXT / aria-label
            // against English literals ('marketplace', 'menu', 'messenger'…) and then hid
            // THE COMPLEMENT — everything that matched none of them. On a Spanish/Japanese/
            // Arabic Facebook every text test failed, so isMarketplace/isMessages/isMenu/
            // isSearch were all false for EVERY tab and the whole nav bar was set to
            // display:none. Marketplace mode also hides the back button and the FB logo, so
            // losing Menu meant losing the route to settings and log out — the user had no
            // way out of the app.
            //
            // THE FIX, two parts:
            //   1. STRUCTURAL. A tab is identified by the href ROUTE it navigates to
            //      (SL_NAV_ALLOW / SL_NAV_DENY, and FB's own data-sigil elsewhere), which is
            //      the same string in every language. No translation needed, nothing to
            //      capture.
            //   2. FAIL OPEN. We now hide only what we can POSITIVELY identify as a route
            //      out of Marketplace. A tab we cannot classify is LEFT VISIBLE. The worst
            //      case flips from "the user is trapped with no menu" to "one extra tab is
            //      reachable" — and the click-blocker + enforceMarketplaceMode above still
            //      bounce them back, so the lockdown itself does not leak.
            //      (Text is used only as an extra POSITIVE signal, never as a complement.)
            document.querySelectorAll('[role="tab"], nav a, [role="navigation"] a').forEach(function(el) {
                if (el._slMktChecked) return;
                var p = _slFbNavPath(el);
                // KEEP: an allowed route, or a tab that names one (Marketplace / Messages /
                // Menu / Search). Menu + Bookmarks + Settings are in SL_NAV_ALLOW precisely
                // so the escape route can never be hidden again.
                if (_slFbPathIn(p, SL_NAV_ALLOW)) return;
                if (_slFbMetaHasWord(el, SL_NAV_ALLOW_WORDS)) return;
                // HIDE: only a positively identified way out (home / friends / notifications
                // / watch / reels / groups / …).
                var deny = _slFbPathIsHome(p) || _slFbPathIn(p, SL_NAV_DENY) ||
                           _slFbMetaHasWord(el, SL_NAV_DENY_WORDS);
                if (!deny) return;   // ⭐ unclassifiable tab → stays visible. Fail OPEN.
                el._slMktChecked = true;
                el.style.setProperty('display', 'none', 'important');
            });

            // Hide the home feed if visible
            var feedStream = document.getElementById('m_newsfeed_stream');
            if (feedStream) feedStream.style.setProperty('display', 'none', 'important');
            var recentCapsule = document.getElementById('recent_capsule_container');
            if (recentCapsule) recentCapsule.style.setProperty('display', 'none', 'important');
            var storiesTray = document.getElementById('stories_tray');
            if (storiesTray) storiesTray.style.setProperty('display', 'none', 'important');
            document.querySelectorAll('[role="feed"]').forEach(function(el) {
                if (!el.closest('[data-marketplace]') && !window.location.pathname.includes('/marketplace')) {
                    el.style.setProperty('display', 'none', 'important');
                }
            });
        }

        // ==================== STANDARD OPTIONAL TAB HIDING ====================
        if (!marketplaceMode) {
            if (hideHome) {
                // Redirect away from home page to a page that works without app
                var path = window.location.pathname;
                if (path === '/' || path === '' || path === '/home.php' || path === '/home') {
                    window.location.replace('https://m.facebook.com/notifications');
                    return;
                }
                // Hide the home tab/button in navigation
                document.querySelectorAll('[role="tab"], nav a, [role="navigation"] a').forEach(function(el) {
                    var href = (el.getAttribute('href') || '').toLowerCase();
                    var ariaLabel = (el.getAttribute('aria-label') || '').toLowerCase();
                    var text = (el.textContent || '').trim().toLowerCase();
                    if (href === '/' || href === '/home.php' || ariaLabel.includes('home') ||
                        ariaLabel.includes('news feed') || text === 'home' || text === 'news feed') {
                        el.style.setProperty('display', 'none', 'important');
                    }
                });
                // [FB-I18N-2026-09-17] STRUCTURAL twin: the home tab by its ROUTE, so the hide
                // lands on a Spanish "Inicio" / Japanese「ホーム」tab too. The href tests above
                // only caught the literal '/' and '/home.php' spellings.
                _slFbHideNavByPath(['/home'], [], 'd', true);
                // Also hide home icon in header
                document.querySelectorAll('a[href="/"], a[href="/home.php"]').forEach(function(el) {
                    var rect = el.getBoundingClientRect();
                    // Only hide if it's in a nav area (not inline content links)
                    if (rect.height < 60) {
                        el.style.setProperty('display', 'none', 'important');
                    }
                });
            }
            // [FB-I18N-2026-09-17] Each of these four toggles matched ONE English aria-label
            // and so did nothing at all on a non-English Facebook (a user who turned People
            // off still saw the Friends tab). Each original selector is kept byte-for-byte —
            // English behaviour is unchanged — and a STRUCTURAL route pass is added beside it.
            // These hides are positive by construction, so they always failed toward SHOWING
            // a tab; the route pass is what makes them actually work in every language, with
            // no capture needed.
            if (hidePeople) {
                document.querySelectorAll('[aria-label*="friends" i]').forEach(function(el) {
                    if (el.getAttribute('role') === 'tab') el.style.display = 'none';
                });
                _slFbHideNavByPath(['/friends'], [], 'd');
            }
            if (hideMessenger) {
                document.querySelectorAll('[aria-label*="messages" i]').forEach(function(el) {
                    if (el.getAttribute('role') === 'tab') el.style.display = 'none';
                });
                _slFbHideNavByPath(['/messages', '/t/'], [], 'd');
            }
            if (hideNotifications) {
                document.querySelectorAll('[aria-label*="notification" i]').forEach(function(el) {
                    if (el.getAttribute('role') === 'tab') el.style.display = 'none';
                });
                _slFbHideNavByPath(['/notifications'], [], 'd');
            }
            if (hideMarketplace) {
                document.querySelectorAll('[aria-label*="marketplace" i]').forEach(function(el) {
                    el.style.display = 'none';
                });
                _slFbHideNavByPath(['/marketplace'], [], 'd');
            }
        }
    }

    // NAVIGATION INTERCEPTION
    var originalPushState = history.pushState;
    history.pushState = function() {
        var url = arguments[2];
        if (url && (url.includes('/reel') || url.includes('/watch'))) return;
        if (marketplaceMode && url && !isAllowedInMarketplaceMode(url)) return;
        // Block navigation to home when hideHome is on
        if (hideHome && !marketplaceMode && url) {
            try {
                var parsed = new URL(url, window.location.origin);
                if (parsed.pathname === '/' || parsed.pathname === '/home.php') return;
            } catch(e) {}
        }
        return originalPushState.apply(this, arguments);
    };

    var originalReplaceState = history.replaceState;
    history.replaceState = function() {
        var url = arguments[2];
        if (marketplaceMode && url && !isAllowedInMarketplaceMode(url)) return;
        return originalReplaceState.apply(this, arguments);
    };

    if (marketplaceMode) {
        window.addEventListener('popstate', function() {
            setTimeout(enforceMarketplaceMode, 50);
            setTimeout(enforceMarketplaceMode, 200);
        });

        var originalBack = history.back;
        history.back = function() {
            originalBack.apply(this, arguments);
            setTimeout(enforceMarketplaceMode, 50);
            setTimeout(enforceMarketplaceMode, 200);
        };

        var originalGo = history.go;
        history.go = function(delta) {
            if (delta && delta < 0) {
                originalGo.apply(this, arguments);
                setTimeout(enforceMarketplaceMode, 50);
                setTimeout(enforceMarketplaceMode, 200);
            } else {
                originalGo.apply(this, arguments);
            }
        };
    }

    blockContent();

    var pendingUpdate = false;
    var isVisible = !document.hidden;

    var observer = new MutationObserver(function() {
        if (!isVisible || pendingUpdate) return;
        pendingUpdate = true;
        setTimeout(function() {
            blockContent();
            pendingUpdate = false;
        }, 300);
    });
    if (document.body) {
        observer.observe(document.body, { childList: true, subtree: true, attributes: false });
    } else {
        document.addEventListener('DOMContentLoaded', function() {
            if (document.body) observer.observe(document.body, { childList: true, subtree: true, attributes: false });
            blockContent();
        });
    }

    var intervalId = null;
    function startInterval() {
        if (!intervalId) intervalId = setInterval(blockContent, 1000);
    }
    function stopInterval() {
        if (intervalId) { clearInterval(intervalId); intervalId = null; }
    }

    document.addEventListener('visibilitychange', function() {
        isVisible = !document.hidden;
        if (isVisible) { blockContent(); startInterval(); }
        else { stopInterval(); }
    });

    startInterval();

    S.pause = function() { stopInterval(); observer.disconnect(); };
    S.resume = function() {
        blockContent(); startInterval();
        if (document.body) observer.observe(document.body, { childList: true, subtree: true, attributes: false });
    };

    // [FB-INSTALL-BANNER-TEXT-MATCH-2026-06-18] CSS-only :has()
    // catches the "Facebook is better on the app" install banner
    // when FB renders the Get app button as an <a> with a real
    // App Store href. Some FB rollouts JS-handle the button
    // instead — the href is "#" or absent and only resolves on
    // click. The JS below catches that case.
    // [FB-INSTALL-INTERSTITIAL-FIX-V2-2026-07-18] The prior version scanned the
    // first 800 h1-h4/div/span for the "better on the app" TITLE, then bailed if
    // not found. But FB portals this bottom-sheet modal to the END of <body>, so
    // on a full feed the title sits PAST index 800 — never found, early return,
    // popup persists (the reported bug, still hitting inside SocialBreak).
    // Re-anchored on the modal's BUTTONS instead: the install button ("Get app"
    // etc.) is the promo's fingerprint and there are few clickables to scan (no
    // cap, position-independent). Find it → locate its modal container → click
    // the container's OWN "Not now" (clears the modal AND the dimmed backdrop
    // cleanly). Fallback: hide the container. Scoping "Not now" to the install
    // button's own modal means we can never hijack an unrelated dialog's dismiss.
    // ⭐ [FB-I18N-2026-09-17] Made STRUCTURAL. The install button used to be found only by its
    // English text, so on a non-English Facebook `getApp` was always null and the whole
    // function returned immediately — the "Facebook is better on the app" sheet was
    // undismissable and unhideable. An install button is a link to the App Store / Play /
    // fb:// in EVERY language, so that link is now the primary fingerprint and the text list
    // is the fallback. The dismiss labels below are still English (capture pending), but the
    // `modal.style.display = none` fallback at the end means the sheet goes away regardless.
    var SL_APP_INSTALL_CTA = ['get app','get the app','install app','open in app','open app','use app'];
    function _slFbHideInstallBanner() {
        var getApp = null, modal = null;
        // Structural first — but SCOPED. A bare document-wide query for an app/store link
        // would also hit a link inside an ordinary post, and the modal walk below would then
        // hide a chunk of the feed. So the link only counts when it sits inside a promo
        // CONTAINER (dialog / aria-modal / FB bottom sheet / modal class). That container is
        // also the modal, so no walk is needed.
        try {
            var boxes = document.querySelectorAll('[role="dialog"], [aria-modal="true"], [data-sigil="MBottomSheet"], [class*="modal" i]');
            for (var q = 0; q < boxes.length && !getApp; q++) {
                var lnk = null;
                try { lnk = boxes[q].querySelector(SL_APP_LINK_SEL); } catch (e) {}
                if (lnk) { getApp = lnk; modal = boxes[q]; }
            }
        } catch (e) {}
        // Text fallback — the original English path, byte-for-byte, for the JS-handled button
        // whose href is '#' or absent ([FB-INSTALL-BANNER-TEXT-MATCH-2026-06-18]).
        if (!getApp) {
            var clickables = document.querySelectorAll('div[role="button"], a[role="button"], span[role="button"], button, a');
            for (var i = 0; i < clickables.length; i++) {
                var t = (clickables[i].textContent || '').trim().toLowerCase();
                if (SL_APP_INSTALL_CTA.indexOf(t) !== -1) { getApp = clickables[i]; break; }
            }
        }
        if (!getApp) return;

        // The modal container holding the install button (role=dialog, aria-modal,
        // else nearest positioned ancestor within a generous 14-level walk).
        if (!modal) modal = getApp.closest('[role="dialog"]') || getApp.closest('[aria-modal="true"]');
        if (!modal) {
            var p = getApp.parentElement;
            for (var j = 0; j < 14 && p && p !== document.body; j++) {
                var pos = '';
                try { pos = window.getComputedStyle(p).position; } catch (e) {}
                if (pos === 'fixed' || pos === 'absolute' || pos === 'sticky') { modal = p; break; }
                p = p.parentElement;
            }
        }

        // Dismiss via the modal's OWN "Not now" — scoped to this modal so we can
        // never click an unrelated dialog's button. Cleanest exit (FB tears down
        // the dimmed backdrop too).
        var scope = modal || document;
        var btns = scope.querySelectorAll('div[role="button"], a[role="button"], span[role="button"], button, a');
        for (var b = 0; b < btns.length; b++) {
            var bt = (btns[b].textContent || '').trim().toLowerCase();
            // [FB-I18N-2026-09-17] English-only (capture pending). Harmless when it misses:
            // the fallback below hides the sheet anyway, which is why this whole function now
            // works in every language.
            if (SL_APP_DISMISS.indexOf(bt) !== -1) {
                try { btns[b].click(); return; } catch (_) {}
            }
        }

        // Fallback — hide the modal container outright.
        if (modal) modal.style.setProperty('display', 'none', 'important');
    }
    _slFbHideInstallBanner();
    setInterval(_slFbHideInstallBanner, 600);

    // [FB-INLINE-REELS-WALKER-V4-2026-06-18] V3's text-based
    // "Reels header sibling" heuristic failed in production:
    // user reported the card still rendered. V4 switches to a
    // multi-strategy walker that ALSO re-tries on every
    // interval until the aria element is visually hidden.
    //
    // Why removing the per-aria-element flag works: V3 set
    // `_slFbReelChecked = true` on first scan even when the
    // mark didn't actually hide the card. Next intervals
    // skipped the (still-visible) card. V4 uses
    // `offsetParent === null` as the "is this aria element
    // actually hidden?" check — true skip condition is
    // "already off-screen" not "we tried once."
    //
    // Strategy stack (per aria element each interval):
    //   1. Size-based: find first ancestor ≥ 250pt × 250pt
    //      (V3 removed the aggressive CSS hide that broke
    //      getBoundingClientRect, so this works again)
    //   2. Fallback: walk fixed 10 levels up (V3 used 7
    //      which under-shot — debug logger path showed 8+
    //      ancestors before truncation, real card root is
    //      typically at depth 9-10)
    //
    // First card-sized ancestor gets marked → CSS rule
    // collapses it → next interval the aria element is in a
    // hidden subtree → offsetParent null → skipped going
    // forward.
    // [FB-INLINE-REELS-WALKER-V6-2026-06-18] V5 picked the
    // TALLEST card-sized ancestor — which turned out to be
    // the entire feed column (1971pt tall). Hiding it grey-
    // screened the user's whole feed. V5 telemetry revealed
    // the exact DOM:
    //   depth 0-1: 369x419  ← aria area
    //   depth 2-4: 393x479  ← THE CARD
    //   depth 5-7: 393x1971 ← feed of all posts (DON'T HIDE)
    //
    // V6 detects the height-jump that marks the feed boundary
    // (card 479 → feed 1971 = 4x jump). Walk up tracking the
    // last card-sized ancestor. When height jumps ≥ 1.5x,
    // we've hit the feed wrapper — stop and mark the LAST
    // safe card-sized level.
    // ⭐ [FB-I18N-2026-09-17] STRUCTURAL companion to the aria walker below.
    //
    // The walker's only anchor is the localized aria-label "View reel" / "reel video", so on
    // a non-English Facebook it found nothing and inline reel cards rendered in full. The
    // aria strings are capture pending (FB only serves the feed behind a login), but the
    // cards have a language-independent fingerprint: a feed post that links to /reel/.
    //
    // ⚠️ Why this is a SEPARATE pass and not just extra selectors on the walker: the CSS at
    // the top of this script already sets `a[href*="/reel/"] { display:none }`, so those
    // anchors have offsetParent === null and the walker's "already hidden → skip" guard
    // would throw every one of them away. This pass looks at the [role="feed"] DIRECT CHILD
    // instead (the post card, which is still visible) and never consults offsetParent.
    //
    // Guards, same shape as the ad/non-network walkers: only a direct child of [role="feed"],
    // never the feed itself or anything wrapping it, and a single-post height window.
    function _slFbHideInlineReelsStructural() {
        try {
            var feeds = document.querySelectorAll('[role="feed"]');
            for (var f = 0; f < feeds.length; f++) {
                var kids = feeds[f].children;
                for (var k = 0; k < kids.length; k++) {
                    var card = kids[k];
                    if (!card || card.hasAttribute('data-__NAMESPACE__0')) continue;
                    if (card.getAttribute && card.getAttribute('role') === 'feed') continue;
                    if (card.querySelector && card.querySelector('[role="feed"]')) continue;
                    var hasReel = false;
                    try {
                        hasReel = !!card.querySelector('a[href*="/reel/"],a[href*="/reels/"]');
                    } catch (e) {}
                    if (!hasReel) continue;
                    var h = card.getBoundingClientRect().height;
                    if (h < 120 || h > 900) continue;   // single post only — never a multi-post block
                    card.setAttribute('data-__NAMESPACE__0', '1');
                }
            }
        } catch (e) {}
    }

    function _slFbHideInlineReels() {
        _slFbHideInlineReelsStructural();   // [FB-I18N-2026-09-17]
        // [FB-I18N-2026-09-17] English-only aria (capture pending) — the structural pass
        // above is what carries this feature in the other eight languages.
        var sel = '[aria-label*="View reel" i],[aria-label*="reel video" i]';
        var hits = document.querySelectorAll(sel);
        for (var i = 0; i < hits.length; i++) {
            var el = hits[i];
            // Skip if already in a hidden subtree.
            if (el.offsetParent === null) continue;

            var p = el.parentElement;
            var bestCard = null;
            var prevH = 0;
            var debugPath = [];
            for (var j = 0; j < 15 && p && p !== document.body; j++) {
                var rect = p.getBoundingClientRect();
                debugPath.push({
                    depth: j,
                    tag: p.tagName ? p.tagName.toLowerCase() : '?',
                    cls: ((p.className || '') + '').slice(0, 50),
                    w: Math.round(rect.width),
                    h: Math.round(rect.height)
                });
                // Card-sized = width 280-500 + height 200-1000.
                // Upper bound of 1000pt is conservative — real
                // FB feed cards are 400-800pt tall on iPhone.
                if (rect.width >= 280 && rect.width <= 500 &&
                    rect.height >= 200 && rect.height <= 1000) {
                    // Detect height jump = feed boundary.
                    // V5 telemetry showed 479 → 1971 = 4.1x.
                    // 1.5x catches that cleanly without
                    // false-positives on normal card growth.
                    if (prevH > 0 && rect.height > prevH * 1.5) {
                        break;
                    }
                    bestCard = p;
                    prevH = rect.height;
                } else if (prevH > 0 && rect.height > prevH * 1.5) {
                    // Height-jump fired but width also exceeded
                    // — also a feed boundary. Stop.
                    break;
                }
                p = p.parentElement;
            }

            if (bestCard) {
                bestCard.setAttribute('data-__NAMESPACE__0', '1');
                try {
                } catch (e) {}
                continue;
            }

            // Fallback: walk fixed 4 levels up (V5 telemetry
            // confirmed the card lives ~3-4 levels above the
            // aria element).
            var fb = el;
            for (var m = 0; m < 4 && fb.parentElement; m++) {
                fb = fb.parentElement;
            }
            if (fb && fb !== document.body) {
                fb.setAttribute('data-__NAMESPACE__0', '1');
            }
        }
    }
    _slFbHideInlineReels();
    setInterval(_slFbHideInlineReels, 800);

    // [FB-ONLY-MY-NETWORK-2026-07-19] Hide any feed post carrying a "Follow"
    // (page/person you don't follow) or "Join" (group you're not in) header CTA —
    // leaving only posts from people you follow + groups you're in. FB injects
    // those CTAs ONLY on out-of-network content, so their presence in a card's
    // header is the signal. We match the EXACT button text on a leaf-ish element
    // (so a sentence containing "join" never triggers), walk up to the post card,
    // and require the CTA to sit in the card's header band (top ~150px) so a stray
    // "Follow" deeper in the card (e.g. a comment) can't nuke the whole post.
    function _slFbFindPostCard(el) {
        var p = el.parentElement, best = null, prevH = 0;
        for (var j = 0; j < 15 && p && p !== document.body; j++) {
            var r = p.getBoundingClientRect();
            var cardSized = r.width >= 280 && r.width <= 520 && r.height >= 120 && r.height <= 1600;
            if (cardSized) {
                if (prevH > 0 && r.height > prevH * 1.5) break; // hit the feed wrapper
                best = p; prevH = r.height;
            } else if (prevH > 0 && r.height > prevH * 1.5) {
                break;
            }
            p = p.parentElement;
        }
        return best;
    }
    // [FB-NONNET-SAFE-2026-07-25] Fail-safe: never mark the feed, a wrapper that
    // contains it, or a short header — the causes of the blackscreen. Same guard
    // as the ad-hide walker.
    function _slFbNonNetSafe(el) {
        if (!el || el === document.body || el === document.documentElement) return 'no-el';
        if (el.getAttribute && el.getAttribute('role') === 'feed') return 'is-feed';
        if (el.querySelector && el.querySelector('[role="feed"]')) return 'wraps-feed';
        var h = el.getBoundingClientRect().height;
        if (h < 180) return 'too-short(' + Math.round(h) + ')';
        if (h > 800) return 'too-tall(' + Math.round(h) + ')';   // [FB-ADHIDE-TUNE-2026-07-25] single post ~480-670; >800 = multi-post
        return 'ok';
    }
    function _slFbFilterNonNetwork() {
        if (!onlyMyNetwork) return;
        var els = document.querySelectorAll('a[role="button"], div[role="button"], span[role="button"], a, span');
        for (var i = 0; i < els.length; i++) {
            var el = els[i];
            if (el.children && el.children.length > 1) continue; // leaf-ish only
            var t = (el.textContent || '').trim().toLowerCase();
            // [FB-I18N-2026-09-17] English-only, capture pending — AND deliberately left as a
            // one-word EXACT match on a leaf-ish element. ⚠️ When these are captured, do NOT
            // let a short stem join a contains-match here: Facebook post text, group names and
            // profile names are user content, and a stem like de "Ton" ⊂ "Button" or fr "son"
            // ⊂ "Person" would nuke innocent posts (the exact trap IG hit on 2026-09-17).
            // Moot today: [FB-ADHIDE-SHELVED-2026-07-25] hard-forces onlyMyNetwork = false, so
            // this walker returns at the top.
            if (['follow','join'].indexOf(t) === -1) continue;
            var card = _slFbFindPostCard(el);
            if (!card || card.hasAttribute('data-__NAMESPACE__1')) continue;
            // Header-band guard: the CTA must sit near the card's top.
            var cr = card.getBoundingClientRect(), er = el.getBoundingClientRect();
            if (er.top - cr.top > 150) continue;
            // [FB-NONNET-LOGONLY-2026-07-25] Log the Follow/Join element's FULL
            // ancestor chain (sizes) so we can find the real single-post boundary,
            // and DON'T hide anything — the feed can't break while we capture.
            var np = el, npath = [];
            for (var nj = 0; nj < 12 && np && np !== document.body; nj++) {
                var nr = np.getBoundingClientRect();
                npath.push(nj + ':' + (np.tagName || '?').toLowerCase() + ' w' + Math.round(nr.width) + ' h' + Math.round(nr.height));
                np = np.parentElement;
            }
            // (hiding disabled during capture — re-enabled after re-tune)
        }
    }
    _slFbFilterNonNetwork();
    setInterval(_slFbFilterNonNetwork, 800);

    // [FB-REEL-SCROLL-JUMP-FIX-2026-06-18] V6 walker correctly
    // marks the card (telemetry confirmed: depth 4, height 479)
    // but the hide fires up to 800ms after the card renders.
    // That delay = card visually exists → user scrolls → walker
    // collapses 479pt → siblings shift → scroll position lands
    // wrong = user sees "jumps to previous places."
    //
    // MutationObserver fires synchronously when nodes are added
    // to the DOM, BEFORE the browser paints. Running the walker
    // here catches Reels cards within microseconds of insertion
    // — they never visually exist → no layout shift, no jumps.
    //
    // Debounced via a token so rapid sequential mutations
    // (FB's React commit phase often fires many at once) only
    // trigger one walker pass per microtask. The 800ms interval
    // above stays as a safety net.
    (function() {
        if (!('MutationObserver' in window)) return;
        var scheduled = false;
        var mo = new MutationObserver(function() {
            if (scheduled) return;
            scheduled = true;
            // Use Promise.resolve to run after current
            // mutation batch but still before next paint.
            Promise.resolve().then(function() {
                scheduled = false;
                try { _slFbHideInlineReels(); } catch (e) {}
            });
        });
        function start() {
            if (document.body) {
                mo.observe(document.body, {
                    childList: true,
                    subtree: true
                });
            } else {
                document.addEventListener('DOMContentLoaded', start, { once: true });
            }
        }
        start();
    })();

    // [FB-REEL-CLICK-BLOCK-2026-06-18] Document-level capture-
    // phase click interceptor. The walker may not catch a card
    // on the first frame before user taps. This intercepts the
    // click BEFORE FB's own handlers see it — preventing FB
    // from starting the reel-viewer transition at all (which
    // was the cause of the white-screen state when the URL
    // change got blocked mid-flight).
    //
    // Walks UP from click target looking for either:
    //   (a) The aria element itself or a descendant
    //   (b) An ancestor marked data-__NAMESPACE__0 (walker
    //       found and tagged the card)
    // Either match → cancel the click entirely.
    document.addEventListener('click', function(e) {
        try {
            var p = e.target;
            for (var i = 0; i < 14 && p && p !== document.body; i++) {
                if (p.hasAttribute && p.hasAttribute('data-__NAMESPACE__0')) {
                    e.preventDefault();
                    e.stopPropagation();
                    e.stopImmediatePropagation();
                    return;
                }
                var aria = (p.getAttribute && p.getAttribute('aria-label')) || '';
                // [FB-I18N-2026-09-17] English-only (capture pending). The card branch above
                // (data-__NAMESPACE__0) is now set structurally too, so a localized reel card
                // is still click-blocked via that attribute.
                if (/(?:View reel|reel video)/i.test(aria)) {
                    e.preventDefault();
                    e.stopPropagation();
                    e.stopImmediatePropagation();
                    return;
                }
                p = p.parentElement;
            }
        } catch (err) {}
    }, true);

    // [FB-REEL-NAV-BLOCK-V3-2026-06-18] V2's `window.location.
    // replace('/')` force-redirect caused an infinite reload
    // loop. FB internally fires replaceState/pushState during
    // normal page activity (analytics, viewport tracking,
    // prefetch) — and some of those URLs happen to include
    // `/reel/` + `referral_source=native_in_feed_unit` WITHOUT
    // a user click. V2 caught these auto-fired calls, force-
    // reloaded home, FB reinitialized, fired another auto-
    // call, loop. Symptoms: page constantly refreshing,
    // white-screen flashes, severe lag.
    //
    // V3 fix: silently swallow the blocked nav (just return,
    // don't reload). The document-level click interceptor
    // above is the primary defense — it catches user clicks
    // BEFORE FB starts the reel viewer transition, so the
    // white-screen case (transition started, then nav blocked)
    // shouldn't happen anymore. If it does occur, FB's React
    // tree might briefly render a broken view, but that's
    // better than an infinite reload loop.
    (function() {
        function isReelNav(url) {
            return url.indexOf('/reel/') !== -1 &&
                   url.indexOf('referral_source=native_in_feed_unit') !== -1;
        }
        var origReplace = history.replaceState;
        history.replaceState = function() {
            var u = String(arguments[2] || '');
            if (isReelNav(u)) {
                return;
            }
            return origReplace.apply(this, arguments);
        };
        var origPush = history.pushState;
        history.pushState = function() {
            var u = String(arguments[2] || '');
            if (isReelNav(u)) {
                return;
            }
            return origPush.apply(this, arguments);
        };
    })();

    // [FB-REEL-HARD-BLOCK-2026-07-10] You should NEVER end up on a reel —
    // if we ever land on one, jump straight to home. Reels reached via SPA
    // history swaps (the shorts tab: replaceState to /reel/...?
    // in_reels_tab_context=TRUE&referral_source=fb_shorts_tab) bypass the
    // Swift path block AND both history guards above (guard 1's replaceState
    // only checks marketplace; guard 2 only swallows native_in_feed_unit).
    // TWO layers so a reel is never watchable:
    //   1. CSS: blank the page (opacity 0 + no taps) the instant the live
    //      URL is a reel — nothing is visible even in the frame before we
    //      move, and even if the redirect is throttled.
    //   2. Redirect home. A small bump-counter breaks any runaway auto-nav
    //      loop (rare, non-user) by STAYING blanked rather than thrashing —
    //      still never a visible reel, and no reload loop (the V2 failure
    //      the inline guard above documents). native_in_feed_unit reels
    //      never reach here (guard 2 swallows them), so normal use only
    //      ever redirects.
    (function() {
        function isReelUrl() {
            var u = (location.href || '').toLowerCase();
            return u.indexOf('/reel/') !== -1 ||
                   u.indexOf('/reels/') !== -1 ||
                   u.indexOf('in_reels_tab_context=true') !== -1 ||
                   u.indexOf('fb_shorts_tab') !== -1;
        }
        var style = document.createElement('style');
        style.textContent = 'html._slReelBlock body{opacity:0 !important;pointer-events:none !important;}';
        (document.head || document.documentElement).appendChild(style);
        var bumps = [];
        try { bumps = JSON.parse(sessionStorage.getItem('__NAMESPACE__4') || '[]'); } catch (e) {}
        function guard() {
            if (!isReelUrl()) {
                document.documentElement.classList.remove('_slReelBlock');
                return;
            }
            document.documentElement.classList.add('_slReelBlock'); // blank NOW
            var now = Date.now();
            bumps = bumps.filter(function(t) { return now - t < 4000; });
            if (bumps.length >= 4) return; // runaway loop - stay blanked, don't reload
            bumps.push(now);
            try { sessionStorage.setItem('__NAMESPACE__4', JSON.stringify(bumps)); } catch (e) {}
            window.location.replace('https://m.facebook.com/');
        }
        var _op = history.pushState;
        history.pushState = function() { var r = _op.apply(this, arguments); setTimeout(guard, 0); return r; };
        var _or = history.replaceState;
        history.replaceState = function() { var r = _or.apply(this, arguments); setTimeout(guard, 0); return r; };
        window.addEventListener('popstate', function() { setTimeout(guard, 0); });
        setInterval(guard, 300);
        guard();
    })();

})();
})(/*CONFIG*/null);
