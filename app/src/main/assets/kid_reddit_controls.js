/* Generated from SocialBreak RedditConfig + RedditSelectorLabels.
 * Android: runtime parent flags, opaque names, no logger, preserve Reddit sensitive-content gates.
 * Parent setting updates reload the page to restore nodes hidden by the iOS wrapper. */
(function(config){
if(config.hosts.indexOf(location.hostname.toLowerCase()) < 0) return;
if(/^\/(login|register|account|oauth)(\/|$)/.test(location.pathname)) return;
(function() {
  'use strict';
  if (window.__NAMESPACE__0) return;
  window.__NAMESPACE__0 = true;

  var AD_BLOCK = config.rules.reddit_ad_blocker === true;
  var HIDE_SUGGESTED = config.rules.reddit_hide_suggested === true;
  var ONLY_MY_COMMUNITIES = config.rules.reddit_only_my_communities === true;
  var BLOCK_POPULAR = config.rules.reddit_block_popular === true;
  var BLOCK_NEWS = config.rules.reddit_block_news === true;
  var BLOCK_LATEST = config.rules.reddit_block_latest === true;
  var REDDIT_LOGGER = false;

  // ---- CSS layer ----
  var AD_SELECTORS = [
    'shreddit-ad-post',
    'shreddit-comments-page-ad',
    'shreddit-comment-tree-ad',
    'shreddit-post[promoted]',
    'shreddit-post[is-ad]',
    'shreddit-post[is-ad-blank-loggedout]',
    '[data-testid="ad-post"]',
    'ad-slot-loader'
  ];
  // Reddit's mobile-web app-promo ("XPromo" cross-promo) elements.
  var PROMO_SELECTORS = [
    '[bundlename*="xpromo" i]',
    '[class*="xpromo" i]',
    'shreddit-app-selector'
  ];
  var css = PROMO_SELECTORS.map(function(s){return s + ':not(xpromo-nsfw-blocking-modal):not(:has(xpromo-nsfw-blocking-modal))';}).join(',') + '{display:none !important;}';
  if (AD_BLOCK) {
    css += AD_SELECTORS.join(',') + '{display:none !important;}';
    css += 'article:has(shreddit-ad-post){display:none !important;}';
    css += 'article:has(shreddit-post[promoted]){display:none !important;}';
  }
  var style = document.getElementById('__NAMESPACE__0CSS') || document.createElement('style');
  style.id = '__NAMESPACE__0CSS';
  style.textContent = css;
  (document.head || document.documentElement).appendChild(style);

  function hideCard(el) {
    if (!el) return;
    var card = el.closest('article') || el.closest('shreddit-post') || el;
    if (!card || !card.style) return;
    if (card.getAttribute && card.getAttribute('data-__NAMESPACE__-5') === '1') return;
    card.style.setProperty('display', 'none', 'important');
    if (card.setAttribute) card.setAttribute('data-__NAMESPACE__-5', '1');
  }

  // ---- App-promo CTAs (always) ----
  // "Open App" header button + any "Open in app" / "Continue in app"
  // interstitial CTA. Text-matched (short/exact), each checked once. If
  // the CTA sits inside a modal we hide the whole modal + restore scroll;
  // otherwise we hide just the button.
  var PROMO_CTAS = ['open app','open in app','open in the app','continue in app','continue in the app','get the app','use app','view in app','app öffnen','ouvrir l\'appli','ouvrir l’appli','abrir app','apri app','アプリを開く','앱 열기','فتح التطبيق'];
  function killAppPromo() {
    var nodes = document.querySelectorAll('a:not([data-__NAMESPACE__-6]),button:not([data-__NAMESPACE__-6]),[role="button"]:not([data-__NAMESPACE__-6])');
    for (var i = 0; i < nodes.length; i++) {
      var el = nodes[i];
      el.setAttribute('data-__NAMESPACE__-6', '1');
      var t = (el.textContent || '').trim().toLowerCase();
      if (t.length <= 22 && PROMO_CTAS.indexOf(t) !== -1) {
        var modal = el.closest('[role="dialog"], dialog, shreddit-app-selector, [class*="xpromo" i]');
        var target = modal || el;
        // [RD-BANNER-HIDE-RAIL-2026-09-15] Same rail — `closest` is a parent walk too, and a
        // promo CTA rendered inside a page-level wrapper would take the page with it.
        if (safeToHide(target)) target.style.setProperty('display', 'none', 'important');
        if (modal) {
          if (document.documentElement) document.documentElement.style.removeProperty('overflow');
          if (document.body) document.body.style.removeProperty('overflow');
        }
      }
    }
  }

  // ---- App-install bottom banner ("View in Reddit App" + Open) ----
  // Reddit renders this OUTSIDE the XPromo classes the CSS covers, and
  // its button text is a generic "Open", so the CTA scan above misses
  // it. Match the distinctive label and hide the whole fixed/sticky bar
  // it lives in. Every element is marked (__NAMESPACE__-2) so it's inspected
  // only once — bounded cost on the infinite feed.
  var BANNER_LABELS = ['view in reddit app','view in the reddit app','view in app','open in the reddit app','open in reddit app','continue in the reddit app','get the reddit app','open reddit app'];
  // ⭐ [RD-BANNER-HIDE-RAIL-2026-09-15] Never hide anything structural.
  //
  // Malek: "scrolling Reddit, after I go down the page a certain amount it just blank screens
  // until I restart the app." Reproduced on the sim and caught with a setProperty hook — the
  // stack was unambiguous:
  //
  //     hideFixedAncestor  ←  killAppBanner  ←  sweep
  //     APP-ROOT style set → display: none !important;
  //
  // `<shreddit-app>` — Reddit's entire application root — had been given an inline
  // `display:none !important`. The DOM was intact (165 posts still in it, JS still running, no
  // crash), the document just had nothing left to lay out: scrollHeight fell from 10382 to 812,
  // exactly one viewport. Nothing ever restores it, which is the "until I restart the app" part.
  //
  // The culprit is the fallback below: "no fixed ancestor found → hide 3 levels above the
  // label". It counts levels and stops at document.body, so three levels above anything high in
  // Reddit's tree IS the app root. An app-install banner is a small bar; a container holding the
  // whole feed never is, and the old code had no way to tell those apart.
  //
  // So both branches now go through a rail: refuse structural roots by name, refuse anything
  // that CONTAINS the feed, and refuse anything the size of the page. A banner fails none of
  // those tests; the app root fails all three.
  function safeToHide(node) {
    if (node && (node.matches('xpromo-nsfw-blocking-modal') || node.closest('xpromo-nsfw-blocking-modal') || node.querySelector('xpromo-nsfw-blocking-modal'))) return false;
    if (!node || node === document.body || node === document.documentElement) return false;
    var tag = (node.tagName || '').toLowerCase();
    if (tag === 'shreddit-app' || tag === 'main' || tag === 'shreddit-feed' || tag === 'body' || tag === 'html') return false;
    if (node.id === 'main-content' || node.id === 'subgrid-container') return false;
    // Contains the feed → it is a container, not a banner.
    try { if (node.querySelector('shreddit-feed, shreddit-post, article')) return false; } catch (e) {}
    // Page-sized → it is the page.
    try {
      var r = node.getBoundingClientRect();
      if (r.height > window.innerHeight * 0.6 && r.width > window.innerWidth * 0.8) return false;
    } catch (e) {}
    return true;
  }

  function hideFixedAncestor(el) {
    var node = el;
    for (var up = 0; up < 9 && node && node !== document.body && node !== document.documentElement; up++) {
      var cs = null;
      try { cs = window.getComputedStyle(node); } catch (e) {}
      if (cs && (cs.position === 'fixed' || cs.position === 'sticky')) {
        if (safeToHide(node)) node.style.setProperty('display', 'none', 'important');
        return;
      }
      node = node.parentElement;
    }
    // No fixed ancestor found — hide up to 3 levels above the label, and only while each step
    // stays something a banner could plausibly be.
    var f = el;
    for (var k = 0; k < 3 && f.parentElement && f.parentElement !== document.body; k++) {
      if (!safeToHide(f.parentElement)) break;
      f = f.parentElement;
    }
    if (f && f.style && safeToHide(f)) f.style.setProperty('display', 'none', 'important');
  }
  function killAppBanner() {
    var els = document.querySelectorAll('a:not([data-__NAMESPACE__-2]),button:not([data-__NAMESPACE__-2]),span:not([data-__NAMESPACE__-2]),div:not([data-__NAMESPACE__-2])');
    for (var i = 0; i < els.length; i++) {
      var el = els[i];
      el.setAttribute('data-__NAMESPACE__-2', '1');
      var t = (el.textContent || '').trim().toLowerCase();
      if (t.length > 40) continue;
      if (BANNER_LABELS.indexOf(t) !== -1) { hideFixedAncestor(el); continue; }
      // Generic "Open"/"View" button — only act if the surrounding bar
      // mentions the Reddit app, so unrelated Open buttons are untouched.
      if (t === 'open' || t === 'view') {
        var bar = el.closest('div, section, aside');
        var ctx = bar ? (bar.textContent || '').toLowerCase() : '';
        if (ctx.indexOf('reddit app') !== -1 || ctx.indexOf('view in') !== -1) {
          hideFixedAncestor(el);
        }
      }
    }
  }

  // ---- Discovery-page blocks (Popular / News / Latest) ----
  // Hide the nav entry points to Reddit's discovery pages and, if the
  // user lands ON one, bounce them back to Home. Nav-scoped (never
  // touches in-feed post content). "Latest" is a feed/sort with no
  // stable URL, so it's entry-point-hide only.
  // [RD-NAVHIDE-SHADOW-2026-08-20] Reddit moved the left drawer into
  // faceplate web components with open shadow roots, so a plain
  // document.querySelectorAll no longer sees the Popular/News/Explore
  // rows — the entry-point hide had silently died in EVERY language
  // (the pages themselves stayed blocked via goHome). Collect the
  // document plus every open shadow root once per pass and run both
  // matchers over each root. Element-capped, and only paid while a
  // BLOCK_* flag is on.
  function collectRoots() {
    var roots = [document], seen = 0;
    for (var r = 0; r < roots.length; r++) {
      var all;
      try { all = roots[r].querySelectorAll('*'); } catch (e) { continue; }
      for (var i = 0; i < all.length; i++) {
        if (++seen > 20000) return roots;
        if (all[i].shadowRoot) roots.push(all[i].shadowRoot);
      }
    }
    return roots;
  }
  // closest() can't cross a shadow boundary — hop root hosts manually.
  function composedInFeed(el) {
    var n = el;
    while (n) {
      if (n.closest && n.closest('shreddit-post, article')) return true;
      n = n.getRootNode ? n.getRootNode().host : null;
    }
    return false;
  }
  function hideNavByHref(roots, hrefs) {
    for (var r = 0; r < roots.length; r++) {
      for (var h = 0; h < hrefs.length; h++) {
        var links;
        try { links = roots[r].querySelectorAll('a[href*="' + hrefs[h] + '"]'); } catch (e) { continue; }
        for (var i = 0; i < links.length; i++) {
          var a = links[i];
          if (a.style && !composedInFeed(a)) {
            a.style.setProperty('display', 'none', 'important');
            var li = a.closest('li');
            if (li && li.style) li.style.setProperty('display', 'none', 'important');
          }
        }
      }
    }
  }
  function hideNavByText(roots, labels) {
    // Left-drawer nav rows are icon + short text ("Popular", "News",
    // "Explore"). Reddit sometimes wraps them in custom elements / divs,
    // so match the exact text on a BROAD set, skip feed content, and hide
    // the clickable ROW (anchor/li/menuitem/button ancestor), not just the
    // text node.
    for (var r = 0; r < roots.length; r++) {
      var els;
      try { els = roots[r].querySelectorAll('a, button, li, [role="menuitem"], [role="tab"], [role="button"], span, div, faceplate-tracker'); } catch (e) { continue; }
      for (var i = 0; i < els.length; i++) {
        var el = els[i];
        var t = (el.textContent || '').trim().toLowerCase();
        if (t.length > 16 || labels.indexOf(t) === -1) continue;
        if (composedInFeed(el)) continue;
        var row = el.closest('a, li, [role="menuitem"], [role="tab"], button, [role="button"]') || el;
        if (row.style) row.style.setProperty('display', 'none', 'important');
      }
    }
  }
  function killBlockedPages() {
    if (!BLOCK_POPULAR && !BLOCK_NEWS && !BLOCK_LATEST) return;
    var path = (location.pathname || '').toLowerCase();
    function goHome(prefixes) {
      for (var i = 0; i < prefixes.length; i++) {
        if (path.indexOf(prefixes[i]) === 0) { location.replace('/'); return; }
      }
    }
    var roots = collectRoots();
    if (BLOCK_POPULAR) {
      hideNavByHref(roots, ['/r/popular', '/r/all']);
      hideNavByText(roots, ['popular', 'all', 'beliebt', 'populaires', 'popolari', '人気', '인기', 'الشائع']);
      goHome(['/r/popular', '/r/all']);
    }
    if (BLOCK_NEWS) {
      hideNavByHref(roots, ['/news', '/r/news']);  // [LOGGER-CONFIRMED] News page is /news/, not /r/news/
      hideNavByText(roots, ['news', 'nachrichten', 'actualités', 'noticias', 'feed notizie', 'notícias', 'ニュース', '뉴스', 'أخبار']);
      goHome(['/news', '/r/news']);
    }
    if (BLOCK_LATEST) {  // [relabeled "Hide Explore" in UI] targets the Explore page
      hideNavByHref(roots, ['/explore']);
      hideNavByText(roots, ['explore', 'entdecken', 'explorer', 'explorar', 'esplora', '探索する', '둘러보기', 'استكشاف']);
      goHome(['/explore']);
    }
  }

  // ---- Only My Communities: hide posts from non-joined subreddits ----
  // Inverted search — find every "Join" affordance in the feed and hide
  // the post it belongs to (walk UP via closest). Works no matter where
  // the Join button sits relative to <shreddit-post>. A member's post
  // shows "Joined"/no button, so exact "join" only → your own subs stay.
  function killNonJoinedPosts() {
    if (!ONLY_MY_COMMUNITIES) return;
    // [LOGGER-CONFIRMED] Reddit's membership button is the custom element
    // <shreddit-join-button> — its inner <button> is in shadow DOM, so a
    // plain button/a text scan finds nothing ("JOIN: none matched"). It
    // only renders for subreddits you're NOT a member of, so hide the post
    // that contains one. Skip the joined/leave state defensively, and also
    // cover anything whose aria-label starts with "Join " as a fallback.
    var els = document.querySelectorAll('shreddit-join-button, [aria-label^="Join" i], [aria-label^="join" i]');
    for (var i = 0; i < els.length; i++) {
      var el = els[i];
      var al = (el.getAttribute('aria-label') || '').trim().toLowerCase();
      if (al.indexOf('joined') === 0 || al.indexOf('leave') === 0) continue;  // already a member
      // [RD-I18N-WIRE-2026-08-20] Structural already-member belt: the
      // text guard above FAILS OPEN in a non-English session (a
      // localized "Joined"/"Leave" aria wouldn't match, so the user's
      // OWN communities could get hidden). If the custom element
      // exposes a subscribed/joined state attribute, treat attribute
      // presence as already-member. Emitted ALONGSIDE the text guard
      // (text guard stays first) so English DOM behavior is unchanged.
      // The attribute list is BEST-EFFORT until verified against the
      // live shreddit-join-button DOM.
      if (el.hasAttribute && (el.hasAttribute('subscribed') || el.hasAttribute('is-subscribed') || el.hasAttribute('data-subscribed') || el.hasAttribute('is-joined') || el.hasAttribute('data-joined') || (el.getAttribute('subscription-state') || el.getAttribute('data-subscription-state') || '').toLowerCase() === 'subscribed')) continue;
      var post = el.closest('article') || el.closest('shreddit-post');
      if (post) hideCard(post);
    }
  }

  function sweep() {
    killAppPromo();
    killAppBanner();
    killBlockedPages();
    killNonJoinedPosts();

    if (AD_BLOCK) {
      for (var i = 0; i < AD_SELECTORS.length; i++) {
        var nodes = document.querySelectorAll(AD_SELECTORS[i]);
        for (var j = 0; j < nodes.length; j++) hideCard(nodes[j]);
      }
    }

    // Per-post markers — process each post once.
    if (AD_BLOCK || HIDE_SUGGESTED) {
      var posts = document.querySelectorAll('shreddit-post:not([data-__NAMESPACE__-3])');
      for (var k = 0; k < posts.length; k++) {
        var p = posts[k];
        p.setAttribute('data-__NAMESPACE__-3', '1');
        // recommendation reason, e.g. "Because you've shown interest…"
        if (HIDE_SUGGESTED) {
          var full = (p.textContent || '');
          if (full.indexOf("Because you’ve ") !== -1 || full.indexOf("Because you've ") !== -1) { hideCard(p); continue; }
        }
        // ONLY MY COMMUNITIES is handled by killNonJoinedPosts() (an
        // inverted button->post search) so it works no matter where the
        // "Join" button sits relative to <shreddit-post>.
        var labels = p.querySelectorAll('a, span, faceplate-tracker, time');
        var done = false;
        for (var m = 0; m < labels.length; m++) {
          var t = (labels[m].textContent || '').trim();
          if (AD_BLOCK && (t === 'Ad' || t === 'Promoted' || t === 'Werbung' || t === 'Publicité' || t === 'Anuncio' || t === 'Annuncio' || t === 'Anúncio' || t === '広告' || t === '광고' || t === 'إعلان')) { hideCard(p); done = true; break; }
          if (HIDE_SUGGESTED && t === 'Suggested') { hideCard(p); done = true; break; }
        }
        if (done) continue;
      }
    }

    // "Suggested communities" carousel / section.
    if (HIDE_SUGGESTED) {
      var REC_SELECTORS = ['shreddit-recommended-communities','recommended-communities','community-recommendation','[data-testid*="recommend" i]'];
      for (var r = 0; r < REC_SELECTORS.length; r++) {
        var rn = document.querySelectorAll(REC_SELECTORS[r]);
        for (var s = 0; s < rn.length; s++) { if (rn[s].style) rn[s].style.setProperty('display','none','important'); }
      }
      var heads = document.querySelectorAll('h2:not([data-__NAMESPACE__-4]),h3:not([data-__NAMESPACE__-4])');
      for (var h = 0; h < heads.length; h++) {
        heads[h].setAttribute('data-__NAMESPACE__-4','1');
        var ht = (heads[h].textContent || '').trim();
        if (ht === 'Suggested communities' || ht === 'Recommended for you') {
          // [RD-BANNER-HIDE-RAIL-2026-09-15] The same rail. `closest('section')` is bounded,
          // but the `|| parentElement` fallback is not: a heading rendered without a section
          // wrapper would hand us whatever container it happens to sit in. Same failure shape as
          // the app-banner one that blanked the page, so it gets the same guard rather than an
          // argument about whether it can happen.
          var sec = heads[h].closest('section') || heads[h].parentElement;
          if (sec && sec.style && safeToHide(sec)) sec.style.setProperty('display','none','important');
        }
      }
    }
  }

  var pending = false;
  function schedule() {
    if (pending) return;
    pending = true;
    var run = function() { pending = false; sweep(); };
    if (window.requestAnimationFrame) { window.requestAnimationFrame(run); }
    else { window.setTimeout(run, 16); }
  }

  sweep();
  var obs = new MutationObserver(schedule);
  function start() {
    if (document.body) { obs.observe(document.body, { childList: true, subtree: true }); sweep(); }
    else requestAnimationFrame(start);
  }
  start();
  window.addEventListener('load', sweep);

  // ---- Admin DOM logger ([REDDIT-JS]) ----
  // When the "Reddit DOM Logger" admin toggle is on, dump the structure
  // (tag + class path + outerHTML) of the Join buttons, the Popular/News/
  // Explore nav rows, and promoted posts to the Xcode console via the
  // redditDebug handler. Filter the Xcode console by "[REDDIT-JS]".

})();

})(/*CONFIG*/null);
