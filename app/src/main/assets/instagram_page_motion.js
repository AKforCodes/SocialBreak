// Android adaptation of iOS IGSwipe v2: existing SPA links, carousel arbitration,
// cached page cards and readiness before revealing. No offscreen page loads.
(function () {
  var C = /*CONFIG*/null;
  if (!C || !/^(www\.)?instagram\.com$/.test(location.hostname)) return;
  var key = '__NAMESPACE__experience';
  if (window[key]) { window[key].updateConfig(C); return; }
  var active = true;
  var nativeBarEnabled = false, hiddenBar = null, hiddenVisibility = '', hiddenPriority = '', knownTabs = [];
  var rootBounded = false, rootOverflow = '', rootOverflowPriority = '';
  function boundTabScroll(enabled) {
    if (enabled === rootBounded) return;
    var root = document.documentElement;
    if (enabled) {
      rootOverflow = root.style.getPropertyValue('overflow-x');
      rootOverflowPriority = root.style.getPropertyPriority('overflow-x');
      // Instagram's mobile Search can overflow the viewport by 2–3px. That root
      // drift invalidates PixelCopy before a page swipe starts. Clip only the root;
      // nested photo carousels and other horizontal rails keep their own scrolling.
      root.style.setProperty('overflow-x', 'clip', 'important');
      window.scrollTo(0, scrollY);
    } else if (rootOverflow) root.style.setProperty('overflow-x', rootOverflow, rootOverflowPriority);
    else root.style.removeProperty('overflow-x');
    rootBounded = enabled;
  }
  function navVisible(el) {
    if (visible(el)) return true;
    var r=el.getBoundingClientRect();
    return !!(nativeBarEnabled && hiddenBar && hiddenBar.contains(el) && r.width>1 && r.height>1 && getComputedStyle(el).display!=='none');
  }
  function auth() { return /^\/(accounts|challenge|two_factor|oauth)(\/|$)/.test(location.pathname); }
  function path() { return location.pathname.replace(/\/+$/, '') || '/'; }
  function visible(el) {
    if (!el || el.closest('[hidden],[aria-hidden="true"]')) return false;
    var r = el.getBoundingClientRect(), s = getComputedStyle(el);
    return r.width > 1 && r.height > 1 && s.display !== 'none' && s.visibility !== 'hidden';
  }
  function links() {
    if (nativeBarEnabled && hiddenBar && hiddenBar.isConnected) {
      return Array.from(hiddenBar.querySelectorAll('a[href]')).filter(function(a){return navVisible(a)&&a.origin===location.origin;});
    }
    var list = Array.from(document.querySelectorAll('nav a[href], [role="navigation"] a[href]'));
    // Instagram's mobile bottom bar is often plain DIVs, with no nav role.
    // Recognise its shallow bottom container (fixed or a flex-layout sibling); never turn feed/profile
    // links elsewhere on the page into invented swipe destinations.
    Array.from(document.querySelectorAll('a[href]')).forEach(function(a) {
      if (!navVisible(a) || a.closest('article')) return;
      var r = a.getBoundingClientRect();
      if (r.top < innerHeight - 120 || r.bottom > innerHeight + 2 || r.height > 100) return;
      for (var n=a.parentElement, depth=0; n && depth<16; n=n.parentElement, depth++) {
        var nr=n.getBoundingClientRect();
        if (nr.top > innerHeight*.65 && nr.height <= 144 && nr.width >= innerWidth*.6) {
          var anchors=Array.from(n.querySelectorAll('a[href]'));
          var core=anchors.filter(function(el){return el.origin === location.origin && /^\/(?:explore\/?|direct\/inbox\/?)?$/.test(el.pathname);});
          if (new Set(core.map(function(el){return el.pathname;})).size >= 2) {list=list.concat(anchors);break;}
        }
      }
    });
    return list.filter(function (a, i) { return list.indexOf(a) === i && navVisible(a) && a.origin === location.origin; });
  }
  function tabs() {
    if (auth()) return [];
    var all = links(), result = [];
    function add(name, match, allowed) {
      var a = allowed && all.find(match);
      if (a) result.push({name:name, path:new URL(a.href).pathname.replace(/\/+$/, '') || '/',
        avatar:name==='profile'?(a.querySelector('img').currentSrc||a.querySelector('img').src):''});
    }
    add('home', function(a){return a.pathname === '/';}, !C.blockFeed && !C.dmsOnly);
    add('search', function(a){return /^\/explore\/?$/.test(a.pathname);}, !C.dmsOnly);
    add('messages', function(a){return /^\/direct\/inbox\/?$/.test(a.pathname);}, true);
    add('profile', function(a){return /^\/[^/]+\/$/.test(a.pathname) && !!a.querySelector('img') && !/^\/(reels|stories|explore|accounts)\//.test(a.pathname);}, !C.dmsOnly);
    if (nativeBarEnabled && !knownTabs.length && path()==='/direct/inbox') {
      knownTabs=[{name:'home',path:'/'},{name:'search',path:'/explore'},{name:'messages',path:'/direct/inbox'}];
    }
    if (result.length >= 2) knownTabs = result;
    if (nativeBarEnabled) knownTabs.forEach(function(tab){if(!result.some(function(t){return t.name===tab.name;}))result.push(tab);});
    result.sort(function(a,b){return ['home','search','messages','profile'].indexOf(a.name)-['home','search','messages','profile'].indexOf(b.name);});
    result = result.filter(function(t){return t.name==='messages' || !C.dmsOnly &&
      (t.name==='home' ? !C.blockFeed : t.name==='search' || t.name==='profile');});
    return result;
  }
  function blockedSurface() {
    return auth() || /^\/(stories|reels?|direct\/t|p|create)(\/|$)/.test(path()) ||
      !!Array.from(document.querySelectorAll('[role="dialog"], [aria-modal="true"]')).find(visible) ||
      !!(document.activeElement && document.activeElement.matches('input:not([readonly]),textarea,[contenteditable="true"]'));
  }
  function thread(p) { return /^\/direct\/t\/[^/]+/.test(p); }
  function dmViewerOpen() {
    return thread(path()) && Array.from(document.querySelectorAll('img,video')).some(function(el){
      var r=el.getBoundingClientRect();
      return visible(el) && r.height>innerHeight*.6 && r.width>innerWidth*.8;
    });
  }
  function detailRoute(p) {
    if (thread(p) || /\/(p|reel|reels|tv)\/[^/]+/.test(p)) return true;
    return /^\/[^/]+(?:\/(?:tagged|saved|followers|following))?$/.test(p) &&
      !/^\/(accounts|challenge|two_factor|oauth|stories|direct|explore|reels?|tv|p|create|about|legal|web)(\/|$)/.test(p) &&
      !tabs().some(function(t){return t.path===p;});
  }
  function postDialog() {
    if (!/\/(p|reel|reels|tv)\/[^/]+/.test(path())) return null;
    return Array.from(document.querySelectorAll('[role="dialog"],[aria-modal="true"]')).find(function(el) {
      var r=el.getBoundingClientRect();
      var media=el.querySelector('article') || Array.from(el.querySelectorAll('img,video')).some(function(node){
        var mr=node.getBoundingClientRect();
        return visible(node) && mr.width>=innerWidth*.6 && mr.height>=innerHeight*.25;
      });
      return visible(el) && r.width>=innerWidth*.7 && r.height>=innerHeight*.65 && !!media;
    }) || null;
  }
  function otherDialog() {
    var post=postDialog();
    return Array.from(document.querySelectorAll('[role="dialog"],[aria-modal="true"]')).some(function(el){return visible(el)&&el!==post;});
  }
  function pageRoot() { return postDialog() || document.querySelector('main,[role="main"]') || document.body; }
  function backAllowed(p) {
    return active && (!C.dmsOnly || p==='/direct/inbox' || thread(p)) && !/^\/(accounts|challenge|two_factor|oauth|create)(\/|$)/.test(p) &&
      !(C.blockFeed && p==='/') && !(C.blockStories && /^\/stories(?:\/|$)/.test(p));
  }
  function backDestination() {
    if (!detailRoute(path()) || !backAllowed(path()) || dmViewerOpen() || otherDialog() ||
        !thread(path()) && document.activeElement?.matches('input:not([readonly]),textarea,select,[contenteditable="true"]')) return null;
    // Like iOS, a chat exits to the inbox, including a directly opened thread.
    // Use SPA routing rather than unknown history (which may leave Instagram).
    if (thread(path())) return {path:'/direct/inbox',steps:0};
    // Retain the observed chain across our own Back traversals. Never guess what
    // precedes a fresh deep link, or push the departing post onto a returning profile.
    for (var i=observedHistory.length-2;i>=Math.max(0,observedHistory.length-13);i--) {
      if (observedHistory[i]!==path()) return backAllowed(observedHistory[i]) ?
        {path:observedHistory[i],steps:observedHistory.length-1-i} : null;
    }
    return null;
  }
  var previousPath = path(), storyOrigin = '', inboxOrigin = '', routeRevision = 0, routeAt = performance.now(), homeScroll = null;
  // Only entries observed in this document qualify for a Home return. Keep
  // Search's own subroute pushes, so returning skips those without crossing a
  // document boundary or guessing what is behind an initial deep link.
  var observedHistory = [path()];
  var detailPositions = new Map();
  function detailPosition() {
    var root=pageRoot(), link=root.querySelector('a[href*="/p/"],a[href*="/reel/"]');
    var scrollers=[root].concat(Array.from(root.querySelectorAll('main,[role="main"],div,section'))).filter(function(el){
      return visible(el) && el.clientHeight>200 && el.scrollHeight>el.clientHeight+40 && /auto|scroll/.test(getComputedStyle(el).overflowY);
    });
    return {anchor:link ? link.pathname : String(pageSurface().content),x:scrollX,y:scrollY,scrollers:scrollers};
  }
  function saveDetailPosition() {
    if (path()!==previousPath || !ready(path()) || !(detailRoute(path()) || tabs().some(function(t){return t.name==='profile'&&t.path===path();}))) return;
    var p=detailPosition();
    detailPositions.delete(path());
    detailPositions.set(path(),{anchor:p.anchor,x:p.x,y:p.y,offsets:p.scrollers.map(function(el){return [el.scrollLeft,el.scrollTop];})});
    if(detailPositions.size>12)detailPositions.delete(detailPositions.keys().next().value);
  }
  function restoreDetailPosition(want, revision, attempt) {
    var saved=detailPositions.get(want);
    if(!saved || path()!==want || routeRevision!==revision || !backAllowed(want))return;
    if(!ready(want)) {
      if(attempt<16)setTimeout(function(){restoreDetailPosition(want,revision,attempt+1);},100);
      return;
    }
    var p=detailPosition();
    if(p.anchor!==saved.anchor || p.scrollers.length!==saved.offsets.length)return;
    window.scrollTo(saved.x,saved.y);
    p.scrollers.forEach(function(el,i){el.scrollLeft=saved.offsets[i][0];el.scrollTop=saved.offsets[i][1];});
  }
  var surfaceIds = new WeakMap(), nextSurfaceId = 0, routeSurface = null;
  var historyReturn = null;
  function releaseHistoryReturn(pending) {
    if (pending.restoration !== null) history.scrollRestoration = pending.restoration;
  }
  function routeChanged(leavingSurface) {
    var now = path();
    if (now !== previousPath) {
      window[key]?.zoom?.cancel();
      var ts = tabs(), tabPath = function(p){return p.startsWith('/explore/') ? '/explore' : p;};
      // A route flip can precede React replacing the old page by hundreds of ms.
      // Do not reveal or cache those old pixels as the destination tab.
      routeSurface = (detailRoute(now) || detailRoute(previousPath) || tabPath(now) !== tabPath(previousPath) &&
        ts.some(function(t){return t.path === tabPath(previousPath);}) &&
        ts.some(function(t){return t.path === tabPath(now);})) ? leavingSurface || null : null;
      routeRevision++; routeAt = performance.now();
      if (now === '/' && homeScroll) {
        var revision=routeRevision;
        setTimeout(function(){restoreHomeScroll(revision,0);},350);
      }
    }
    if (now === '/direct/inbox' && previousPath !== now) {
      // An earlier Search → DMs visit cannot describe the history entry after
      // Profile → DMs. Otherwise a swipe toward Search pops back to Profile.
      inboxOrigin = previousPath === '/' || previousPath === '/explore' ? previousPath : '';
    }
    if (/^\/stories(?:\/|$)/.test(now) && !/^\/stories(?:\/|$)/.test(previousPath)) storyOrigin = previousPath;
    previousPath = now;
    if (nativeBarEnabled) refreshNativeBar();
  }
  ['pushState','replaceState'].forEach(function(name) {
    var original = history[name];
    history[name] = function() {
      var leavingSurface = pageSurface();
      saveDetailPosition();
      var result = original.apply(this, arguments);
      if (name === 'pushState') observedHistory.push(path());
      else observedHistory[observedHistory.length - 1] = path();
      if (observedHistory.length > 64) observedHistory.shift();
      routeChanged(leavingSurface); return result;
    };
  });
  addEventListener('popstate', function(event) {
    var pendingBack=historyReturn;
    if (event.isTrusted && pendingBack?.back && active && navigationRequest===pendingBack.request &&
        path()!==pendingBack.want && backAllowed(pendingBack.want)) {
      var ancestor=pendingBack.trail.slice(0,-1).lastIndexOf(path());
      if (ancestor>=0) {
        // Chromium can skip a same-document entry. Keep the cached destination
        // honest: re-enter that observed page through IG's SPA, retaining its
        // logical ancestry for the next Back. Never repair an unknown landing.
        observedHistory=pendingBack.trail.slice(0,ancestor+1);
        historyReturn=null;
        softRoute(pendingBack.want);
        observedHistory=pendingBack.trail;
        releaseHistoryReturn(pendingBack);
        var restoredRevision=routeRevision;
        setTimeout(function(){restoreDetailPosition(pendingBack.want,restoredRevision,0);},350);
        return;
      }
    }
    // A real Back/Forward traversal has an unknown cursor. A synthetic router
    // notification follows our recorded pushState and must retain that trail.
    if (event.isTrusted) {
      var trail=historyReturn?.trail;
      if (trail && trail[trail.length-1]===path()) observedHistory=trail;
      else if (observedHistory[observedHistory.length-2]===path()) observedHistory.pop();
      else observedHistory=[path()];
    }
    routeChanged(historyReturn && historyReturn.surface);
    if (event.isTrusted && historyReturn) {
      var pending = historyReturn;
      historyReturn = null;
      // Browser scroll restoration occurs after popstate. Keep it manual until
      // that task finishes; restoreHomeScroll validates the feed before scrolling.
      setTimeout(function() {
        releaseHistoryReturn(pending);
        if (pending.next && pending.next.request === navigationRequest && active) navigate(pending.next.want, pending.next.explicitTab);
      }, 0);
    }
  });
  function story() { return /^\/stories(?:\/|$)/.test(path()); }
  function storyAllowed() { return story() && !C.blockStories && !C.dmsOnly; }
  function storyBehind() { return storyOrigin || (!C.blockFeed && !C.dmsOnly ? '/' : ''); }
  function closeStory() {
    if (!storyAllowed()) return false;
    var close = Array.from(document.querySelectorAll('[aria-label="Close" i], [aria-label="Cerrar" i], [aria-label="Fermer" i]')).find(visible);
    if (close) { (close.closest('button,[role="button"],a') || close).click(); return true; }
    // Only pop a history entry when we observed entering the story from this document.
    if (storyOrigin) { history.back(); return true; }
    return false;
  }
  function horizontalTrack(el) {
    return el.clientWidth > 60 && el.scrollWidth > el.clientWidth + 24 &&
      // IG also scrolls its tracks in JS, setting overflow:hidden on touch-down.
      /^(auto|scroll|hidden)$/.test(getComputedStyle(el).overflowX);
  }
  function carouselAt(target, x, y) {
    // IG puts its next/back overlays BESIDE the scrolling track. Walking only
    // the hit element's ancestors misses them, and deeply nested media players.
    // Like iOS's carousel-owner check, require the track to contain the finger;
    // a carousel elsewhere in this post must not disable swipes on its caption.
    var post = target.closest('article');
    if (!post) return [];
    return Array.from(post.querySelectorAll('div,section,ul,ol')).filter(function(el) {
      if (!horizontalTrack(el)) return false;
      var r = el.getBoundingClientRect();
      return x >= r.left && x < r.right && y >= r.top && y < r.bottom && visible(el);
    });
  }
  function inspect(x, y) {
    if (window[key]?.zoom?.busy) return null;
    if (storyAllowed()) {
      var hit = document.elementFromPoint(x, y);
      if (!hit || hit.closest('input,textarea,select,[contenteditable="true"]') ||
          (document.activeElement && document.activeElement.matches('input,textarea,[contenteditable="true"]')) ||
          Array.from(document.querySelectorAll('[role="dialog"]')).some(function(el){return visible(el) && el.getBoundingClientRect().height < innerHeight*.85;})) return null;
      return {kind:'story',path:path(),behind:storyBehind()};
    }
    var t = document.elementFromPoint(x, y), n = t;
    if (!t || t.closest('input,textarea,select,[contenteditable="true"],[role="slider"]')) return null;
    // Instagram's message bubble owns rightward swipe-to-reply, as on iOS.
    if (thread(path()) && t.closest('[aria-label="Double tap to like"]')) return null;
    var tracks=carouselAt(t,x,y), back=backDestination();
    for (var depth=0;n && depth<16;n=n.parentElement,depth++) {
      if (horizontalTrack(n) && /auto|scroll/.test(getComputedStyle(n).overflowX) && !tracks.includes(n)) tracks.push(n);
    }
    if (back) {
      // iOS: interior drags go to the previous photo first. Only the first slide
      // or the left edge can swipe out of a detail page over horizontal media.
      if (!thread(path()) && tracks.length && x>40 && tracks.some(function(el){return Math.abs(el.scrollLeft)>8;})) return null;
      return {kind:'back',path:path(),left:back.path,right:'',revision:routeRevision};
    }
    if (blockedSurface() || t.closest('video')) return null;
    if (tracks.length) return null;
    n=t;
    // Run once at touch-down. Photo carousels, stories trays, and horizontal rails own their gestures.
    for (var depth = 0; n && depth < 16; depth++, n = n.parentElement) {
      var s = getComputedStyle(n);
      if (n.scrollWidth > n.clientWidth + 12 && /auto|scroll/.test(s.overflowX)) return null;
    }
    // Mobile inbox replaces the tab bar with a Back header. Preserve the
    // observed return route, rather than depending on links it just unmounted.
    if (C.dmsOnly) return null;
    if (!nativeBarEnabled && inboxReturnAllowed()) return {path:path(),left:inboxOrigin,right:''};
    var ts = tabs(), i = ts.findIndex(function(t){return t.path === path() || t.name==='search'&&path().startsWith('/explore/');});
    if (i < 0) return null;
    return {path:path(), left:i > 0 ? ts[i-1].path : '', right:i+1 < ts.length ? ts[i+1].path : ''};
  }
  var navigationRequest = 0;
  function navigateBack(want, from, revision) {
    var back=backDestination();
    if (historyReturn || window[key]?.zoom?.busy || path()!==from || routeRevision!==revision || !back || back.path!==want) return false;
    if (thread(path())) { document.activeElement?.blur(); softRoute(want); return true; }
    var request=++navigationRequest;
    historyReturn={request:request,next:null,surface:pageSurface(),restoration:null,back:true,want:want,
      trail:observedHistory.slice(0,observedHistory.length-back.steps)};
    history.go(-back.steps);
    setTimeout(function(){
      if(historyReturn?.request===request){releaseHistoryReturn(historyReturn);historyReturn=null;}
    },1600);
    return true;
  }
  function onTab(want) { return path() === want || want === '/explore' && path().startsWith('/explore/'); }
  function softRoute(want) {
    history.pushState(null, '', want === '/' ? '/' : want + '/');
    dispatchEvent(new PopStateEvent('popstate', {state:null}));
  }
  function tabReturnSteps(want) {
    function tabPath(p) { return p.startsWith('/explore/') ? '/explore' : p; }
    var current = tabPath(path());
    if (current === '/' || !(current === '/direct/inbox' || tabs().some(function(t){return t.path === current;}))) return 0;
    for (var i = observedHistory.length - 2; i >= 0; i--) {
      if (tabPath(observedHistory[i]) === want) return observedHistory.length - 1 - i;
      if (tabPath(observedHistory[i]) !== current) return 0;
    }
    return 0;
  }
  function navigationAllowed(explicitTab) {
    // Post viewers own swipe gestures, but the visible native tab bar must
    // still work. Never extend this exception to dialogs, stories or auth.
    return active && (!blockedSurface() || explicitTab && /^\/p\//.test(path()) && barSurface());
  }
  function navigate(want, explicitTab) {
    if (!navigationAllowed(explicitTab)) return false;
    if (path() === '/' && want !== '/') saveHomeScroll();
    var target = tabs().find(function(t){return t.path === want;});
    if (!target && !(inboxReturnAllowed() && want === inboxOrigin)) return false;
    if (historyReturn) {
      // A browser history traversal is asynchronous and cannot be canceled by
      // pushState. Serialize it, then apply the latest authorized tab choice.
      historyReturn.next = {want:want, request:++navigationRequest, explicitTab:explicitTab};
      return true;
    }
    if (onTab(want)) return true;
    var request = ++navigationRequest, revision = routeRevision;
    var steps = want === '/' || inboxReturnAllowed() && want === inboxOrigin ? tabReturnSteps(want) : 0;
    if (steps) {
      // Like iOS, restore the prior Home entry; its anchor asks IG for a fresh feed.
      historyReturn = {request:request, next:null, surface:pageSurface(),
        trail:observedHistory.slice(0,observedHistory.length-steps),
        restoration:want === '/' ? history.scrollRestoration : null};
      if (want === '/') history.scrollRestoration = 'manual';
      history.go(-steps);
    } else {
      var a = links().find(function(a){return (a.pathname.replace(/\/+$/, '') || '/') === want;});
      if (!a) {
        if (nativeBarEnabled || inboxReturnAllowed() && want === inboxOrigin) { softRoute(want); return true; }
        return false;
      }
      // A mounted anchor need not have a working SPA handler yet. Cancel its
      // browser default even if a page handler stops propagation. React can
      // still route; otherwise nudge its SPA immediately, never a hard load.
      var preventDocumentLoad = function(event) { if (event.target === a) event.preventDefault(); };
      window.addEventListener('click', preventDocumentLoad, true);
      try { a.click(); } finally { window.removeEventListener('click', preventDocumentLoad, true); }
      if (revision === routeRevision && request === navigationRequest && navigationAllowed(explicitTab) && !onTab(want)) softRoute(want);
    }
    setTimeout(function(){
      if (historyReturn && historyReturn.request === request) {
        var pending = historyReturn;
        historyReturn = null; releaseHistoryReturn(pending);
        if (pending.next) {
          if (pending.next.request === navigationRequest && active) navigate(pending.next.want, pending.next.explicitTab);
          return;
        }
      }
      // Retry a dropped click once, without overriding later navigation or rule changes.
      if (request === navigationRequest && revision === routeRevision && navigationAllowed(explicitTab) &&
          !onTab(want) && tabs().some(function(t){return t.path===want;})) softRoute(want);
    }, 1600);
    return true;
  }
  function inboxReturnAllowed() {
    return path() === '/direct/inbox' && !C.dmsOnly &&
      (inboxOrigin === '/' && !C.blockFeed || inboxOrigin === '/explore');
  }
  // iOS saves the Home position before routing away, then restores it only if
  // the same first post returns. Instagram's Home anchor otherwise resets to 0,
  // making a correct cached screenshot jump to different live content on reveal.
  function homePosition() {
    var article=Array.from(document.querySelectorAll('article')).find(visible);
    var post=article && article.querySelector('a[href*="/p/"],a[href*="/reel/"]');
    if (!post) return null;
    var scrollers=[];
    for(var n=article.parentElement;n && n!==document.scrollingElement;n=n.parentElement) {
      if(n.clientHeight>200 && n.scrollHeight>n.clientHeight+40 && /auto|scroll/.test(getComputedStyle(n).overflowY)) scrollers.push(n);
    }
    return {post:post.pathname,scrollers:scrollers};
  }
  function saveHomeScroll() {
    var p=homePosition(); if(!p) {homeScroll=null;return;}
    homeScroll={post:p.post,y:scrollY,offsets:p.scrollers.map(function(n){return n.scrollTop;}),at:Date.now()};
  }
  function restoreHomeScroll(revision, attempt) {
    if(!homeScroll || revision!==routeRevision || path()!=='/' || Date.now()-homeScroll.at>120000) return;
    var p=homePosition();
    if(!p) {if(attempt<30)setTimeout(function(){restoreHomeScroll(revision,attempt+1);},100);return;}
    if(p.post===homeScroll.post && p.scrollers.length===homeScroll.offsets.length) {
      window.scrollTo(0,homeScroll.y);
      p.scrollers.forEach(function(n,i){n.scrollTop=homeScroll.offsets[i];});
    } else {
      // Chromium can restore the destination entry's old offset independently
      // of our saved one. A refreshed feed must start at its own top instead.
      window.scrollTo(0,0);
      p.scrollers.forEach(function(n){n.scrollTop=0;});
    }
    homeScroll=null;
  }
  function ready(want) {
    if (auth() || !(path() === want || want==='/explore'&&path().startsWith('/explore/')) || document.readyState === 'loading') return false;
    var main = postDialog() || document.querySelector('main, [role="main"]') || (path() === '/direct/inbox' || thread(path()) ? document.body : null);
    if (!main || !visible(main)) return false;
    var surface = pageSurface();
    if (routeSurface && surface.root === routeSurface.root && surface.content === routeSurface.content) return false;
    // Explore intentionally masks media. Like iOS, its rendered search field is ready.
    if (path().startsWith('/explore') && Array.from(main.querySelectorAll('input')).some(visible)) return true;
    // Infinite feeds keep a loading sentinel below the viewport. It must not
    // prevent caching or revealing the already-painted page above it.
    if (Array.from(main.querySelectorAll('[aria-busy="true"], [role="progressbar"]')).some(function(el){
      var r = el.getBoundingClientRect();
      return visible(el) && r.bottom > 0 && r.top < innerHeight && r.right > 0 && r.left < innerWidth;
    })) return false;
    var content = Array.from(main.querySelectorAll('article, a[href^="/direct/t/"], input, h1, h2, img, video')).some(visible);
    if (!content && main.innerText.trim().length < 4) return false;
    // Only wait for images currently in view. Never block the reveal on below-fold lazy media.
    return !Array.from(main.querySelectorAll('img')).some(function(img){
      var r = img.getBoundingClientRect();
      return visible(img) && r.bottom > 0 && r.top < innerHeight && r.width > 48 && !img.complete;
    });
  }
  var timers = [];
  function _z9qx(fn, ms) { var id = setInterval(function(){if(active && !auth()) fn();}, ms); timers.push(id); return id; }
  function setNetwork(low, saver) {
    C.low = !!low; C.dataSaver = !!saver;
    window.__NAMESPACE____slDataSaver = C.dataSaver;
    if (window.__NAMESPACE____slSetLowBW) window.__NAMESPACE____slSetLowBW(C.low);
  }
  // A URL alone does not identify the pixels: scroll, a same-URL modal, or a fresh
  // document can change while Android's asynchronous PixelCopy is in flight.
  function pageSurface() {
    var main = pageRoot();
    if (main && !surfaceIds.has(main)) surfaceIds.set(main, ++nextSurfaceId);
    var content = main ? main.textContent.slice(0,512) : '';
    Array.from((main || document).querySelectorAll('img')).filter(visible).slice(0,8).forEach(function(img) {
      content += '|' + (img.currentSrc || img.src);
    });
    content += '|' + Array.from(document.querySelectorAll('[role="dialog"],[aria-modal="true"]')).filter(visible).length;
    var hash=0; for(var i=0;i<content.length;i++) hash=(Math.imul(hash,31)+content.charCodeAt(i))|0;
    return {root:main ? surfaceIds.get(main) : 0, content:hash};
  }
  function revealState(want) {
    var surface = pageSurface();
    return {ready:ready(want), settled:performance.now()-routeAt>=320,
      identity:routeRevision+':'+surface.root+':'+surface.content};
  }
  function captureIdentity() {
    var offsets = [Math.round(scrollX), Math.round(scrollY)], atTop = scrollY < 40;
    Array.from(document.querySelectorAll('main, [role="main"], div, section')).forEach(function(el) {
      if (el.clientHeight > 200 && el.scrollHeight > el.clientHeight + 40 && el.scrollTop > 0 && visible(el)) {
        offsets.push(Math.round(el.scrollLeft), Math.round(el.scrollTop));
        if (el.scrollTop >= 40) atTop = false;
      }
    });
    return {path:path(),document:String(performance.timeOrigin),revision:routeRevision,
      scroll:offsets.join(','),viewport:innerWidth+','+innerHeight,content:pageSurface().content,atTop:atTop,
      zoom:window[key]?.zoom?.revision || 0};
  }
  function snapshotState() {
    if (window[key]?.zoom?.busy || auth() || story() || dmViewerOpen() ||
        otherDialog() ||
        !ready(path())) return null;
    var state=captureIdentity();
    state.tab=tabs().find(function(t){return t.path===state.path || t.name==='search'&&state.path.startsWith('/explore/');})?.name || '';
    // Search and inbox reopen at the top. Keep their settled top frame even if
    // the user scrolls before leaving; the outgoing gesture still uses fresh pixels.
    state.cacheable = (!(state.tab==='search'||state.tab==='messages') || state.atTop) && performance.now()-routeAt>=350;
    return state;
  }
  function restoreWebBar() {
    if (hiddenBar) {
      if (hiddenVisibility) hiddenBar.style.setProperty('visibility',hiddenVisibility,hiddenPriority);
      else hiddenBar.style.removeProperty('visibility');
      hiddenBar=null;
    }
  }
  function barSurface() {
    if (auth() || story() || /^\/(direct\/t|reels?|tv|challenge|create)(\/|$)/.test(path()) ||
        document.querySelector('input[name="username"],form#loginForm,input[type="password"]') ||
        Array.from(document.querySelectorAll('[role="dialog"], [aria-modal="true"]')).some(visible)) return false;
    return path()==='/' || /^\/(explore|direct\/inbox|p)(\/|$)/.test(path()) || /^\/[^/]+(?:\/(?:tagged|saved|followers|following))?$/.test(path());
  }
  function refreshNativeBar() {
    var items=tabs(), show=nativeBarEnabled && barSurface() && items.length>0;
    boundTabScroll(show);
    if (!show) restoreWebBar();
    else {
      if (hiddenBar && !hiddenBar.isConnected) restoreWebBar();
      if (!hiddenBar) {
        var anchors=links();
        for (var i=0;i<anchors.length&&!hiddenBar;i++) {
          for(var n=anchors[i].parentElement,d=0;n&&d<16;n=n.parentElement,d++) {
            var r=n.getBoundingClientRect();
            if(r.height>0&&r.height<=144&&r.width>=innerWidth*.6&&r.top>innerHeight*.65&&
              anchors.filter(function(a){return n.contains(a);}).length>=2) {
              hiddenBar=n;hiddenVisibility=n.style.getPropertyValue('visibility');hiddenPriority=n.style.getPropertyPriority('visibility');
              n.style.setProperty('visibility','hidden','important');break;
            }
          }
        }
      }
    }
    var selected=items.find(function(t){return path()===t.path || t.name==='search'&&path().startsWith('/explore/');});
    var profile=items.find(function(t){return t.name==='profile';});
    return {show:show,path:path(),selected:selected?selected.name:'',tabs:items,avatar:profile?profile.avatar:''};
  }
  function navTap(name) {
    if (!nativeBarEnabled || !barSurface()) return false;
    var tab=tabs().find(function(t){return t.name===name;});
    if (!tab) return false;
    if(document.activeElement&&document.activeElement.blur) document.activeElement.blur();
    return navigate(tab.path, true);
  }
  window[key] = {inspect:inspect, navigate:navigate, navigateBack:navigateBack, ready:ready, path:path, closeStory:closeStory,
    zoomAllowed:function(){return active && !auth() && !story() && !/^\/(reels?|create)(\/|$)/.test(path()) &&
      !(C.blockFeed && path()==='/') && !(C.dmsOnly && !path().startsWith('/direct/'));},
    storyReady:function(){return !story() && ready(path());},
    commentsAllowed:function(){return !C.hideComments && !C.dmsOnly && !auth() && !/^\/(direct|stories)(\/|$)/.test(path());},
    updateConfig:function(next){navigationRequest++;C=next; window[key]?.zoom?.validate();setNetwork(C.low,C.dataSaver);if(nativeBarEnabled)refreshNativeBar();}, setNetwork:setNetwork, setActive:function(value){active=!!value;if(!active){navigationRequest++;window[key]?.zoom?.cancel();}},
    setNativeBar:function(value){nativeBarEnabled=!!value;if(!value)restoreWebBar();return refreshNativeBar();},
    nativeBar:refreshNativeBar,navTap:navTap,
    snapshotState:snapshotState,captureIdentity:captureIdentity,revealState:revealState,
    current:function(){var state=snapshotState();return state ? state.path : '';}};
  function warmMountedFeed() {
    if (!active || auth() || C.low || C.dataSaver || C.blockFeed || C.dmsOnly || path() !== '/') return;
    Array.from(document.querySelectorAll('main article img[loading="lazy"]')).slice(0, 40).forEach(function(img){
      if (img.closest('[hidden],[aria-hidden="true"]')) return;
      var r=img.getBoundingClientRect();
      if (r.width > 0 && r.top >= 0 && r.top < innerHeight * 2) {
        img.loading='eager'; img.decoding='async';
      }
    });
  }
  _z9qx(warmMountedFeed, 750);
  var installed = false;
  function install() {
    if (auth() || installed) return;
    installed = true;
    (function(){
  if (window.__NAMESPACE___cpz1) return;
  window.__NAMESPACE___cpz1 = true;
  try {
    var s = document.createElement('style');
    s.id = '__NAMESPACE__cosmetic';
    s.textContent = `
      body { -webkit-font-smoothing: antialiased; }
      button, a, [role="button"] { -webkit-tap-highlight-color: transparent; }
      /* [IG-FOCUS-RING-NONKEYBOARD-2026-09-11] ⚠️ NO BACKTICKS ANYWHERE IN THIS BLOCK — this CSS
         sits inside a JS template literal, so a backtick in a COMMENT closes the string early and
         takes the whole 294 KB Instagram script down with it (that is exactly what happened here:
         the script failed to PARSE, so not one injector ran — no favourites star, no 2x, no emoji
         fix, nothing — while the native side still logged "composed 287KB" and looked healthy). Malek keeps catching a blue ring around whatever sits
         at the top of a page — most recently the DM inbox header. It is a real focus ring: something
         we drive programmatically (a .click() or .focus() in one of our injectors, the swipe chain's
         after-nav script, the explore search field) leaves focus on an element, IG re-renders, and
         the ring lands on whatever inherits it.
         Chasing each individual caller has now failed twice — [IG-EXPLORE-FOCUS-STICKS-2026-09-10]
         patched the explore intercept, [IG-EXPLORE-FOCUS-STICKS-SWIPE-2026-09-10] patched the swipe
         chain, and here it is again from a third path. So fix the CLASS instead of the instance.
         :focus-visible is exactly the browser's own answer to "did a human navigate here by
         keyboard?". Touch and programmatic focus never set it, keyboard focus always does — so this
         removes the rings nobody asked for and keeps every one that matters for accessibility.
         Outline only: box-shadow is load-bearing on IG's real controls. */
      :focus:not(:focus-visible) { outline: none !important; }
      main header section:has([title="Change profile photo"]) div[role="button"] > div > div > div > div {
        margin-top: 5px;
        margin-left: -5px;
        padding-inline: 14px;
        padding-block: 0px !important;
        min-height: 37px;
        border-radius: 25px;
      }
    `;
    (document.head || document.documentElement).appendChild(s);
  } catch (e) {}
})();
(function(){
  if (window.__NAMESPACE___slFeedBoxFit) return;
  window.__NAMESPACE___slFeedBoxFit = true;

  // Uniform box shape: height / width. 1.25 = 4:5 (IG's default feed portrait).
  var BOX_RATIO = 1.25;
  var PB = (BOX_RATIO * 100).toFixed(3) + '%';

  function isHome(){ var p = location.pathname; return p === '/' || p === ''; }

  // The aspect spacer: an ancestor whose COMPUTED height is 0 (its height
  // comes from padding-bottom %) but which renders a real height.
  function findSpacer(video, art){
    var el = video.parentElement, depth = 0;
    while (el && el !== art && depth < 9){
      try {
        var st = getComputedStyle(el);
        var rh = el.getBoundingClientRect().height;
        if (st.height === '0px' && rh > 50) return el;
      } catch (e) {}
      el = el.parentElement; depth++;
    }
    return null;
  }

  var videoStyles = new Map(), boxStyles = new Map();
  function saveStyle(map, el, names) {
    if (!map.has(el)) map.set(el, names.map(function(n){return [n,el.style.getPropertyValue(n),el.style.getPropertyPriority(n)];}));
  }
  function restoreStyle(map, el) {
    var original = map.get(el); if (!original) return;
    original.forEach(function(v){if(v[1]) el.style.setProperty(v[0],v[1],v[2]); else el.style.removeProperty(v[0]);});
    map.delete(el);
  }
  function fillVideo(v){
    saveStyle(videoStyles,v,['object-fit']);
    if (v.style.objectFit !== 'cover') v.style.setProperty('object-fit','cover','important');
  }
  function clearCover(v){ restoreStyle(videoStyles,v); }
  function apply(){
    videoStyles.forEach(function(_,el){if(!el.isConnected) restoreStyle(videoStyles,el);});
    boxStyles.forEach(function(_,el){if(!el.isConnected) restoreStyle(boxStyles,el);});
    if (!isHome()){
      boxStyles.forEach(function(_,el){restoreStyle(boxStyles,el);});
      var all = document.querySelectorAll('video');
      for (var c = 0; c < all.length; c++) clearCover(all[c]);
      return;
    }
    var loose = document.querySelectorAll('video');
    for (var d = 0; d < loose.length; d++){
      try { if (!loose[d].closest('article')) clearCover(loose[d]); } catch (e) {}
    }
    var arts = document.querySelectorAll('article');
    for (var i = 0; i < arts.length; i++){
      var art = arts[i];
      // [IG-ADHIDE-SCROLLFIX-2026-08-24] never style inside ad-hide-collapsed
      // articles (probe showed wasted churn fighting 0-height posts).
      if (art.dataset && art.dataset.slBetaHidden === '1') continue;
      var video = art.querySelector('video');
      if (!video) continue;
      // [IG-DMVIDEO-FIT-2026-08-24] Never crop a video that's in an IMMERSIVE viewer
      // (a DM'd/shared reel or story opened as an overlay) or otherwise near-fullscreen.
      // Those must FIT (contain) like the native app — cropping them to fill was mangling
      // landscape reels. Feed posts sit in a ~4:5 box (well under fullscreen). On skip we
      // also CLEAR any cover we'd previously set (a feed reel that then went fullscreen
      // kept the inline crop), so it reverts to fit.
      try {
        var _vr = video.getBoundingClientRect();
        var _immersive = art.closest('[role="dialog"]') || _vr.height > (window.innerHeight || 0) * 0.85;
        if (_immersive) {
          clearCover(video);
          continue;
        }
      } catch (e) {}
      var sp = findSpacer(video, art);
      if (!sp) { fillVideo(video); continue; }
      try {
        // Re-apply only when IG has drifted the box off our uniform target
        // (React reconciliation can strip our inline style) — comparing the
        // rendered height avoids needless writes every tick.
        var w = sp.getBoundingClientRect().width;
        var targetH = w * BOX_RATIO;
        var curH = sp.getBoundingClientRect().height;
        if (Math.abs(curH - targetH) > 2){
          saveStyle(boxStyles,sp,['padding-bottom','padding-top','height']);
          sp.style.setProperty('padding-bottom', PB, 'important');
          sp.style.setProperty('padding-top', '0px', 'important');
          sp.style.setProperty('height', '0px', 'important');
        }
        fillVideo(video);
      } catch (e) {}
    }
  }

  document.addEventListener('loadedmetadata', function(){ try { apply(); } catch (e) {} }, true);
  apply();
  _z9qx(apply, 700);
})();
(function(){
  if (window.__NAMESPACE___slLowBWInj) return; window.__NAMESPACE___slLowBWInj = true;
  window.__NAMESPACE____slDataSaver = C.dataSaver;   // [DATASAVER-NO-PRELOAD-2026-09-10]
  // [DATASAVER-IG-IMAGES-VERDICT-2026-09-10] ⛔ READ THIS BEFORE TRYING TO MAKE IG IMAGES SMALLER.
  // Malek: "I turned Data Saver on and the images were the same quality." He is right, and it is
  // not fixable from here. Measured on a logged-in feed (2026-09-10, probe below):
  //   • instagram.com's web feed ships every image as a PLAIN src — `imgs=23 srcset=0 picture=0`.
  //     The srcset/picture re-pick under this comment therefore has nothing to act on ON THE FEED.
  //     (It is kept: other surfaces and platforms DO ship srcset, and there it works.)
  //   • The CDN URL's size token is covered by the `oh=` signature. Rewriting a real URL's
  //     s150x150 → s320x320 returned **403**. We cannot ask for a smaller variant ourselves.
  //   • Pinning `window.devicePixelRatio` to 2 at document start (so IG's own client picks a
  //     smaller variant) was tried and REVERTED: the feed photo came back 804x804 at dpr=2 and
  //     804x804 at dpr=3 — IG serves a fixed 804-class feed asset regardless of DPR. Zero saving,
  //     and overriding DPR is a page-wide behaviour change. Don't re-try it without new evidence.
  // What Data Saver actually saves on Instagram is VIDEO (no autoplay + no pre-buffer, below) and
  // images NOT FETCHED AHEAD (the feed preloader stands down). The settings copy says exactly that.
  var MAXW = 640;
  var USE_EST = false;
  var DEBUG = false;
  var lastReport = '';
  function report() {}
  var est = null;            // KB/s, from Resource Timing
  var estLow = false;        // hysteresis-held page-side verdict
  var lastState = null;

  window.__NAMESPACE____slLowBWNative = !!C.low;
  window.__NAMESPACE____slSetLowBW = function (b) { window.__NAMESPACE____slLowBWNative = !!b; tick(); };

  function estimate(){
    if (!USE_EST) return;
    try {
      var es = performance.getEntriesByType('resource');
      var rates = [];
      for (var i = Math.max(0, es.length - 80); i < es.length; i++) {
        var e = es[i];
        if (!e.transferSize || e.transferSize < 25000) continue;
        if (!/cdninstagram|fbcdn/.test(e.name)) continue;
        var d = e.responseEnd - e.responseStart;   // transfer time of THIS download only
        if (!(d > 0)) continue;
        rates.push(e.transferSize / d);            // bytes per ms == KB/s, per download
      }
      if (rates.length >= 4) {
        rates.sort(function(a, b){ return a - b; });
        est = rates[Math.floor(rates.length / 2)];   // median, parallelism-proof
        if (est < 150) estLow = true; else if (est > 400) estLow = false;
      }
    } catch (e) {}
  }
  function low(){ return window.__NAMESPACE____slLowBWNative || estLow; }

  function pick(srcset){
    var best = null, smallest = null;
    srcset.split(',').forEach(function(c){
      var p = c.trim().split(/\s+/); if (p.length < 2) return;
      if (!/^\d+w$/.test(p[1])) return; var w = parseInt(p[1], 10); if (!w) return;
      if (!smallest || w < smallest.w) smallest = { url: p[0], w: w };
      if (w <= MAXW && (!best || w > best.w)) best = { url: p[0], w: w };
    });
    return best || smallest;
  }

  function downscale(){
    if (!low()) return;
    var imgs = document.querySelectorAll('img[srcset]:not([data-__NAMESPACE__lbw])');
    for (var i = 0; i < imgs.length; i++) {
      var img = imgs[i];
      // Leave images that already finished loading at full size alone.
      if (img.complete && img.naturalWidth > 0) { img.setAttribute('data-__NAMESPACE__lbw', 'keep'); continue; }
      var ss = img.getAttribute('srcset') || '';
      var c = pick(ss);
      if (!c) continue;
      img.setAttribute('data-__NAMESPACE__lbw', String(c.w));
      // [LOW-BANDWIDTH-RECOVER-2026-09-04] Keep the originals so a recovered connection can
      // swap the full-quality variants back in (see restore()).
      img.setAttribute('data-__NAMESPACE__srcset', ss);
      var sz = img.getAttribute('sizes'); if (sz) img.setAttribute('data-__NAMESPACE__sizes', sz);
      img.removeAttribute('sizes');
      img.removeAttribute('srcset');
      if (img.src !== c.url) img.src = c.url;
    }
    // [DATASAVER-PICTURE-2026-09-10] <picture><source srcset> was never touched — when IG wraps
    // the media in a <picture>, the browser picks from the SOURCE, and rewriting the inner <img>
    // does nothing at all. Same candidate pick, applied to the source element.
    var srcs = document.querySelectorAll('picture source[srcset]:not([data-__NAMESPACE__lbw])');
    for (var j = 0; j < srcs.length; j++) {
      var so = srcs[j];
      var sss = so.getAttribute('srcset') || '';
      var sc = pick(sss);
      if (!sc) { so.setAttribute('data-__NAMESPACE__lbw', 'none'); continue; }
      so.setAttribute('data-__NAMESPACE__lbw', String(sc.w));
      so.setAttribute('data-__NAMESPACE__srcset', sss);
      var ssz = so.getAttribute('sizes'); if (ssz) so.setAttribute('data-__NAMESPACE__sizes', ssz);
      so.removeAttribute('sizes');
      so.setAttribute('srcset', sc.url);
    }
    // [DATASAVER-LAZY-2026-09-10] Images we have NOT fetched are the only image saving available
    // (see the verdict note at the top — we cannot make a fetched one smaller). Anything still
    // below the fold goes back to loading="lazy": IG's own gate, un-done by our feed preloader
    // when ig_feedV2 is on, so on a weak link we stop paying for posts you may never scroll to.
    var lz = document.querySelectorAll('img[loading="eager"]');
    for (var e = 0; e < lz.length; e++) {
      try {
        var im2 = lz[e];
        if (im2.complete && im2.naturalWidth > 0) continue;      // already paid for
        var rr = im2.getBoundingClientRect();
        if (rr.top > (window.innerHeight || 0)) im2.setAttribute('loading', 'lazy');
      } catch (e2) {}
    }
    // [DATASAVER-VIDEO-PRELOAD-2026-09-10] The bytes Data Saver is really about. Autoplay is
    // already gated natively (mediaTypesRequiringUserActionForPlayback) but WebKit still
    // PRE-BUFFERS every <video> that mounts. preload="none" means a video costs nothing until
    // it is actually played, and an offscreen one that somehow started gets paused.
    var vids = document.querySelectorAll('video');
    for (var v = 0; v < vids.length; v++) {
      var vid = vids[v];
      if (vid.getAttribute('data-__NAMESPACE__lbwv') !== '1') {
        vid.setAttribute('data-__NAMESPACE__lbwv', '1');
        try { vid.preload = 'none'; vid.setAttribute('preload', 'none'); } catch (e) {}
      }
      try {
        if (!vid.paused && vid.dataset.slUserPlay !== '1') {
          var r = vid.getBoundingClientRect();
          if (r.bottom < -40 || r.top > (window.innerHeight || 0) + 40) vid.pause();
        }
      } catch (e) {}
    }
  }

  // [LOW-BANDWIDTH-RECOVER-2026-09-04] Connection recovered → give every downscaled image its
  // original srcset/sizes back. The browser re-selects the larger candidate and refetches it;
  // the small variant stays on screen until the big one lands (no blank flash).
  function restore(){
    // [DATASAVER-PICTURE-2026-09-10] give <source> and <video> their normal behaviour back too
    try {
      var ss2 = document.querySelectorAll('picture source[data-__NAMESPACE__srcset]');
      for (var k = 0; k < ss2.length; k++) {
        var so2 = ss2[k];
        so2.setAttribute('srcset', so2.getAttribute('data-__NAMESPACE__srcset'));
        var z = so2.getAttribute('data-__NAMESPACE__sizes'); if (z) so2.setAttribute('sizes', z);
        so2.removeAttribute('data-__NAMESPACE__srcset'); so2.removeAttribute('data-__NAMESPACE__sizes');
        so2.setAttribute('data-__NAMESPACE__lbw', 'restored');
      }
      var vv = document.querySelectorAll('video[data-__NAMESPACE__lbwv="1"]');
      for (var q = 0; q < vv.length; q++) {
        vv[q].removeAttribute('data-__NAMESPACE__lbwv');
        try { vv[q].preload = 'metadata'; vv[q].setAttribute('preload', 'metadata'); } catch (e) {}
      }
    } catch (e) {}
    var imgs = document.querySelectorAll('img[data-__NAMESPACE__srcset]');
    var n = 0;
    for (var i = 0; i < imgs.length; i++) {
      var img = imgs[i];
      var ss = img.getAttribute('data-__NAMESPACE__srcset'); if (!ss) continue;
      img.setAttribute('srcset', ss);
      var sz = img.getAttribute('data-__NAMESPACE__sizes'); if (sz) img.setAttribute('sizes', sz);
      img.removeAttribute('data-__NAMESPACE__srcset'); img.removeAttribute('data-__NAMESPACE__sizes');
      img.setAttribute('data-__NAMESPACE__lbw', 'restored');
      n++;
    }
    if (n) { try {  } catch (e) {} }
  }

  function tick(){
    estimate();
    var s = low();
    // [IG-LOWBW-PUBLISH-2026-09-10] Publish the verdict so other injectors can follow it — the
    // feed preloader shrinks its lookahead on a weak link instead of warming 4 screens ahead.
    window.__NAMESPACE____slLowBW = s;
    if (s !== lastState) {
      var wasLow = lastState;
      lastState = s;
      try {  } catch (e) {}
      if (wasLow === true && !s) restore();
    }
    downscale();
    report();
  }

  // [DATASAVER-VIDEO-PRELOAD-2026-09-10] A video the USER started is theirs — never pause it,
  // even if it scrolls off screen for a moment.
  try {
    document.addEventListener('play', function (e) {
      var t = e.target;
      if (t && t.tagName === 'VIDEO' && navigator.userActivation && navigator.userActivation.isActive) {
        t.dataset.slUserPlay = '1';
      }
    }, true);
  } catch (e) {}

  tick();
  var mo = new MutationObserver(function(){ if (active && low()) downscale(); });
  try { if (document.body) mo.observe(document.body, { childList: true, subtree: true, attributes: true, attributeFilter: ['srcset'] }); } catch (e) {}
  _z9qx(tick, 1500);
})();
(function() {
  if (window.__NAMESPACE___z0r1) return;
  window.__NAMESPACE___z0r1 = true;
  try {
    var style = document.createElement('style');
    style.textContent = `
      /* Feed page — native-style top nav (centered logo, corner icons) */
      nav header:has([aria-label="Instagram"]) { padding-top: 5px; position: relative; }
      nav header:has([aria-label="Instagram"]) h1 {
        max-width: min-content; padding-left: 5px; position: absolute;
        left: 50%; translate: -50% 0; user-select: none;
      }
      nav header:has([aria-label="Instagram"]) h1 [role="button"][aria-haspopup="menu"] {
        -webkit-tap-highlight-color: transparent; -webkit-touch-callout: none;
        user-select: none; border-radius: 5px;
      }
      nav header:has([aria-label="Instagram"]) span:has(a svg:is([aria-label="Home"],[aria-label="Inicio"],[aria-label="Accueil"],[aria-label="Startseite"],[aria-label="Página inicial"],[aria-label="ホーム"],[aria-label="الصفحة الرئيسية"],[aria-label="홈"])) {
        position: absolute; left: 5px; top: 3px; height: 100%; aspect-ratio: 1;
        display: flex; align-items: center; justify-content: center;
      }
      nav header:has([aria-label="Instagram"]) span:has(a svg:is([aria-label="Notifications"],[aria-label="Notificaciones"],[aria-label="Benachrichtigungen"],[aria-label="Notificações"],[aria-label="お知らせ"],[aria-label="الإشعارات"],[aria-label="알림"],[aria-label="Notifiche"])) {
        position: absolute; right: 5px; top: 3px; height: 100%; aspect-ratio: 1;
        display: flex; align-items: center; justify-content: center;
      }
      [role="dialog"]:has([href*="/?variant"]) {
        translate: -7px 0; animation: _f1z 100ms ease forwards;
      }
      [role="dialog"]:has([href*="/?variant"]) div > a { padding: 2px; }
      [role="dialog"]:has([href*="/?variant"]) div > hr { border-top: 1px solid rgba(255, 255, 255, 0.1); }
      @keyframes _f1z { from { opacity: 0; } to { opacity: 1; } }
      main [data-pagelet="story_tray"] { margin-top: 5px; }

      /* Own profile — tidy top bar (Options right, Threads left) */
      section nav header:has(a[href^="/accounts/settings"]) > div {
        height: fit-content; padding-block: 15px; padding-inline: 23px;
      }
      section nav header:has(a[href^="/accounts/settings"]) div:has(>a svg:is([aria-label*="Options" i],[aria-label*="opciones" i],[aria-label*="options" i],[aria-label*="Optionen" i],[aria-label*="opções" i],[aria-label*="オプション" i],[aria-label*="خيارات" i],[aria-label*="옵션" i],[aria-label*="opzioni" i])) {
        order: 3; justify-content: flex-end; flex-basis: unset; width: fit-content;
      }
      section nav header:has(a[href^="/accounts/settings"]) div:has(>span a[href*="threads.com"]) {
        order: -1; justify-content: flex-start; flex-basis: unset; width: fit-content; margin-bottom: -5px;
      }
    `;
    (document.head || document.documentElement).appendChild(style);
  } catch (e) {}
})();
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', install, {once:true});
  else install();
  _z9qx(install, 750);
})();
