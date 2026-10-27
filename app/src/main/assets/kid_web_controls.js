/* Kid-only Android adaptation of iOS main a8b6ff4 platform controls.
 * InstagramCommentHideInjector, InstagramHideCreateInjector, YouTubeConfig, TikTokConfig.
 * Re-injection removes old effects/listeners; never reads messages, contacts or account identity.
 */
(function (config) {
    'use strict';
    var ns = config.namespace, old = window[ns];
    if (old) old.dispose();
    if (config.hosts.indexOf(location.hostname.toLowerCase()) < 0) return;
    // Authentication must remain usable. Account restriction is applied to the native account UI.
    if (/^\/(accounts\/(login|signup|password)|login|signup|oauth|checkpoint|challenge)(\/|$)/i.test(location.pathname)) return;
    if (config.platform === 'SNAPCHAT' && location.hostname !== 'web.snapchat.com' && !/^\/web(?:\/|$)/.test(location.pathname)) return;
    var r = config.rules, platform = config.platform, style = document.createElement('style');
    style.id = config.styleId;
    var budgetStyle = document.createElement('style');
    (document.head || document.documentElement).appendChild(budgetStyle);
    var css = [], marked = new Set(), attr = config.markAttribute, timer, disposed = false;
    function hide(selector) { css.push(selector + '{display:none!important;pointer-events:none!important}'); }
    function mark(el) { if (el && !el.hasAttribute(attr)) { el.setAttribute(attr, '1'); marked.add(el); } }
    hide('[' + attr + ']');
    var prefix = {INSTAGRAM:'ig',YOUTUBE:'yt',TIKTOK:'tt',TWITTER:'tw',FACEBOOK:'fb',LINKEDIN:'li',SNAPCHAT:'snap',REDDIT:'reddit'}[platform];
    if (r[prefix + '_grayscale']) css.push('html{filter:grayscale(1)!important}');
    var commentStems = ['Comment', '댓글', 'コメント', 'coment', 'commenter', 'kommentar', 'comentar', 'commenta', 'تعليق'];
    function commentSelector(tag) { return commentStems.map(function(s) { return tag + '[aria-label*="' + s + '" i]'; }).join(','); }
    if (platform === 'INSTAGRAM') {
        if (r.ig_hide_comments) {
            hide(commentSelector('textarea'));
            hide('form:has(' + commentSelector('textarea') + ')');
            hide('a:has(' + commentSelector('svg') + '),button:has(' + commentSelector('svg') + '),[role="button"]:has(' + commentSelector('svg') + ')');
        }
        if (r.block_posting) hide('a[href^="/create"],[aria-label="New post"],[aria-label="Create"],a:has(svg[aria-label="New post"]),[role="button"]:has(svg[aria-label="New post"])');
    }
    if (platform === 'YOUTUBE') {
        if (config.effective.yt_blockAds) hide('.ytm-badge-and-byline-item-byline:has(' + config.labels.ytAdBadges.map(function(label) {
            return '[aria-label="' + label + '" i]';
        }).join(',') + ')');
        hide(config.labels.ytOpenApp.map(function(label) { return 'ytm-mobile-topbar-renderer [aria-label="' + label.replace(/"/g, '\\"') + '" i]'; }).join(','));
        if (r.yt_hide_comments) hide('ytm-comment-section-renderer,ytm-comments-entry-point-header-renderer,ytm-comments-entry-point-teaser-renderer,ytm-item-section-renderer[section-identifier="comment-item-section"],ytm-item-section-renderer[section-identifier="comments-entry-point"],ytm-engagement-panel-section-list-renderer[data-panel-identifier="engagement-panel-comments-section"],ytd-comments,#comments,ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-comments-section"],#comments-button');
        if (r.yt_hide_watch_recommendations) hide('ytm-item-section-renderer[section-identifier="related-items"],.related-items-container,ytd-watch-next-secondary-results-renderer');
        if (r.yt_block_end_screens) hide('.ytp-endscreen-content,.ytp-ce-element,.ytp-suggestion-set');
    }
    if (platform === 'TIKTOK') {
        if (r.tt_block_explore) hide('a[href*="/explore"],a[href*="/discover"],[data-e2e="nav-explore"],[data-e2e="nav-discover"]');
        if (r.tt_block_live) hide('a[href*="/live"],[data-e2e="nav-live"]');
        if (r.tt_block_shop) hide('a[href*="/shop"],a[href*="/browse"],[data-e2e="nav-shop"]');
        if (r.tt_block_upload || r.block_posting) hide('a[href*="/upload"],a[href*="/creator"],[data-e2e="nav-upload"],[data-e2e="nav-create"],[data-e2e="upload-icon"],[class*="CreateButton" i],[class*="BottomNavCreate" i]');
        if (r.tt_block_search) hide('[data-e2e="nav-search"],a[href*="/search"],[data-e2e="search-box"],[class*="SearchContainer" i],[class*="SearchBar" i],form[action*="/search"]');
        if (r.tt_block_suggested) hide('[data-e2e="suggested-accounts"],[class*="SuggestedAccount" i],[data-e2e="recommend-list"]');
        if (r.tt_block_comments) hide('[data-e2e="comment-list"],[class*="CommentListContainer" i],[class*="CommentList" i],[data-e2e="browse-comment"],[data-e2e="comment-icon"],[class*="comment-list" i]');
    }
    style.textContent = css.join('\n');
    (document.head || document.documentElement).appendChild(style);

    function blocked(url) {
        try {
            var u = new URL(url, location.href);
            if (config.hosts.indexOf(u.hostname.toLowerCase()) < 0) return false;
            var path = decodeURIComponent(u.pathname).toLowerCase();
            return config.blockedPaths.some(function(p) { return path === p || path.indexOf(p + '/') === 0; }) ||
                (platform === 'TIKTOK' && r.tt_block_live && /^\/@[^/]+\/live(?:\/|$)/.test(path));
        } catch (_) { return false; }
    }
    function pauseFeed(event) {
        if (platform !== 'YOUTUBE' || !r.yt_stop_feed_autoplay || /^\/(watch|shorts)(\/|$)/.test(location.pathname)) return;
        if (event && event.target && event.target.tagName === 'VIDEO') event.target.pause();
        else document.querySelectorAll('video').forEach(function(v) { if (!v.paused) v.pause(); });
    }
    function allowance() {
        if (!config.allowance) return;
        var pattern = platform === 'INSTAGRAM' ? /^\/reels?\/([A-Za-z0-9_-]+)/ : /^\/shorts\/([A-Za-z0-9_-]+)/;
        var match = location.pathname.match(pattern), id = match ? match[1] : '';
        var onShortForm = platform === 'INSTAGRAM' ? /^\/reels?(\/|$)/.test(location.pathname) : /^\/shorts(\/|$)/.test(location.pathname);
        var allowed = false;
        try { allowed = window[config.bridge].shortFormAllowanceTick(id, document.visibilityState === 'visible' && onShortForm); } catch (_) {}
        var nextStyle = allowed ? '' : (platform === 'INSTAGRAM'
            ? 'a[href^="/reel/"],a[href^="/reels/"],a[href="/reels/"]{display:none!important}'
            : 'ytm-reel-shelf-renderer,ytm-shorts-lockup-view-model,ytm-rich-shelf-renderer:has(a[href*="/shorts/"]),ytm-rich-section-renderer:has(ytm-shorts-lockup-view-model),a[href^="/shorts/"]{display:none!important}');
        if (budgetStyle.textContent !== nextStyle) budgetStyle.textContent = nextStyle;
        if (!allowed && onShortForm) {
            document.querySelectorAll('video,audio').forEach(function(v) { v.pause(); });
            location.replace('/');
        }
    }
    function scan() {
        if (disposed) return;
        allowance();
        marked.forEach(function(el) { if (!el.isConnected) marked.delete(el); });
        if (blocked(location.href)) {
            document.querySelectorAll('video,audio').forEach(function(v) { v.pause(); });
            location.replace(platform === 'TIKTOK' ? '/following' : '/');
            return;
        }
        pauseFeed();
        if (platform === 'YOUTUBE' && config.effective.yt_blockAds) {
            // Only player ad controls, never arbitrary user content/buttons called "Skip".
            document.querySelectorAll('.ad-showing button,.ytp-ad-player-overlay button,[class*="ad-skip"] button').forEach(function(button) {
                var label = (button.getAttribute('aria-label') || button.textContent || '').trim().toLowerCase();
                if (!button.disabled && button.getClientRects().length && config.labels.ytSkip.some(function(s) { return s.toLowerCase() === label; })) button.click();
            });
        }
        if (platform === 'LINKEDIN') {
            document.querySelectorAll('div,section,aside,span,p,h1,h2,h3,button,a').forEach(function(el) {
                if (el.children.length > 3 || el.closest('.feed-shared-update-v2,.occludable-update,[role="textbox"],.msg-s-message-list')) return;
                var text = (el.textContent || '').trim().toLowerCase();
                if (!text || text.length > 70 || !config.labels.liOpenApp.some(function(s) { return text === s.toLowerCase(); })) return;
                var node = el;
                for (var hop = 0; hop < 8 && node && node !== document.body && node !== document.documentElement; hop++, node = node.parentElement) {
                    if (/^(fixed|sticky)$/.test(getComputedStyle(node).position)) { mark(node); return; }
                }
                var container = el.closest('[role="dialog"],[class*="bottom-sheet"],[class*="banner"],[class*="modal"],[class*="overlay"],[class*="promo"],[class*="upsell"]');
                if (container) mark(container);
            });
        }
        if (platform !== 'INSTAGRAM' || !r.ig_hide_comments) return;
        document.querySelectorAll('span,a,div[role="button"]').forEach(function(el) {
            if (el.childElementCount > 1) return;
            var t = (el.textContent || '').trim().toLowerCase();
            if (t.length > 0 && t.length <= 40 && commentStems.some(function(s) { return t.indexOf(s.toLowerCase()) >= 0; }) && (/\d/.test(t) || /\b(view|all)\b/.test(t))) mark(el);
        });
        document.querySelectorAll(commentSelector('svg')).forEach(function(svg) {
            for (var node = svg, up = 0; up < 5 && node; up++, node = node.parentElement) {
                var sib = node.nextElementSibling, text = sib && (sib.textContent || '').trim();
                if (text && text.length <= 8 && /^[\d.,]+\s*[kKmM]?$/.test(text)) { mark(sib); break; }
            }
        });
        if (/\/(p|reel|tv)\//.test(location.pathname)) document.querySelectorAll('ul').forEach(function(ul) {
            if (ul.querySelectorAll('time').length >= 2) mark(ul);
        });
    }
    // iOS Sep 13: nested rich-text composer targets must not accidentally send on Enter.
    // Capture only chat fields, never search or login. IME commit remains native after stopping send.
    var snapChatLabels = ['Send a chat', 'Sende eine Nachricht', 'Envoyer un Chat', 'Enviar un chat', 'Invia una chat', 'Enviar um chat', 'チャットを送信', 'أرسل دردشة'];
    function newline(e) {
        if (platform !== 'INSTAGRAM' && platform !== 'SNAPCHAT') return;
        if (!(e.key === 'Enter' || e.keyCode === 13 || e.which === 13) || e.shiftKey || e.metaKey || e.ctrlKey || e.altKey) return;
        var target = e.target && e.target.nodeType === 3 ? e.target.parentElement : e.target;
        var root = target && target.closest && target.closest('textarea,[contenteditable="true"],[role="textbox"]');
        if (!root) return;
        if (platform === 'INSTAGRAM' && !/^\/direct(?:\/|$)/.test(location.pathname)) return;
        if (platform === 'SNAPCHAT') {
            var selector = snapChatLabels.map(function(label) { return '[placeholder*="' + label + '" i]'; }).join(',');
            var row = root.closest('form,[class*="chat" i]') || root.parentElement;
            if (!root.matches(selector) && !(row && row.querySelector(selector))) return;
        }
        e.stopPropagation(); e.stopImmediatePropagation();
        if (e.isComposing || root.tagName === 'TEXTAREA') return;
        e.preventDefault(); root.focus();
        if (!document.execCommand('insertLineBreak')) document.execCommand('insertText', false, '\n');
        root.dispatchEvent(new Event('input', {bubbles:true}));
    }
    function click(e) {
        var target = e.target instanceof Element ? e.target : e.target.parentElement;
        var link = target && target.closest('a[href]');
        if ((link && blocked(link.href)) || (platform === 'INSTAGRAM' && r.block_posting && target && target.closest('[aria-label="New post"],[aria-label="Create"]'))) {
            e.preventDefault(); e.stopImmediatePropagation();
        }
    }
    var scheduled = false;
    var observer = new MutationObserver(function() {
        if (!scheduled) { scheduled = true; requestAnimationFrame(function() { scheduled = false; scan(); }); }
    });
    observer.observe(document.documentElement, {childList:true,subtree:true});
    document.addEventListener('click', click, true);
    document.addEventListener('keydown', newline, true);
    document.addEventListener('play', pauseFeed, true);
    document.addEventListener('visibilitychange', allowance);
    window.addEventListener('pagehide', allowance);
    window.addEventListener('popstate', scan);
    timer = setInterval(scan, 400); // Includes pushState navigation without stacking history wrappers.
    window[ns] = {dispose:function() {
        disposed = true; clearInterval(timer); observer.disconnect(); style.remove(); budgetStyle.remove();
        document.removeEventListener('click', click, true); document.removeEventListener('keydown', newline, true); document.removeEventListener('play', pauseFeed, true);
        window.removeEventListener('popstate', scan);
        document.removeEventListener('visibilitychange', allowance); window.removeEventListener('pagehide', allowance);
        marked.forEach(function(el) { el.removeAttribute(attr); }); marked.clear(); delete window[ns];
    }};
    scan();
})(/*CONFIG*/null);
