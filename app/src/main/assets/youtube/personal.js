/* Personal YouTube port, iOS YouTubeConfig/YouTubeContainer, September 2026.
 * All long-lived hooks read the current config; reinjection never stacks listeners.
 * Family accounts deliberately use the existing generator and native budgets. */
(function () {
    'use strict';
    if (!/^(m\.|www\.)?youtube\.com$/.test(location.hostname)) return;
    var key = Symbol.for('__NS__'), S = window[key] || (window[key] = {});
    var config = __CONFIG__;
    var version = 'android-ios-port-7_' + JSON.stringify(config);
    if (S.ytV === version) return;
    S.ytV = version;
    if (S.youtube) { S.youtube.configure(config); return; }
    var C = config, role = S.youtubeRole || 'feed', userPaused = false, suspended = false;
    var lastTouch = 0, interval = null, lastRoute = '', channel = '', allowedShort = '';
    var lastQuality = '', lastReport = '', style;
    var NS = '__NS__', bridge = window[NS + 'yt'];
    function post(type, data) {
        try { if (bridge) bridge.postMessage(JSON.stringify({type: type, data: data})); } catch (_) {}
    }
    function all(s, root) { return Array.from((root || document).querySelectorAll(s)); }
    function video() { return document.querySelector('video'); }
    function player() { return document.getElementById('movie_player'); }
    function isChannel(p) { return /^\/(@[^/]+|channel\/|c\/|user\/)/.test(p); }
    function shortId(p) { return (p.match(/^\/shorts\/([^/?#]+)/) || [])[1] || ''; }
    function watchURL(raw) {
        try {
            var u = new URL(raw, location.href);
            if (u.protocol !== 'https:' || !/^(m\.|www\.)?youtube\.com$/.test(u.hostname)) return null;
            return u.pathname === '/watch' && /^[\w-]{1,128}$/.test(u.searchParams.get('v') || '') ? u.href : null;
        } catch (_) { return null; }
    }
    function cssRule(selector, body) { return selector + '{' + (body || 'display:none!important') + '}\n'; }
    function css() {
        if (!document.documentElement) return;
        if (!style || !style.isConnected) {
            style = document.createElement('style'); style.id = NS + 'ytcss';
            (document.head || document.documentElement).appendChild(style);
        }
        var s = cssRule('ytm-mealbar-promo-renderer,[class*="upsell" i]');
        if (C.shorts) {
            var guard = C.profiles ? 'html:not([' + NS + '-channel]) ' : '';
            ['ytm-reel-shelf-renderer','ytm-shorts-lockup-view-model','ytm-rich-shelf-renderer:has(a[href*="/shorts/"])',
             'ytm-rich-section-renderer:has(ytm-shorts-lockup-view-model)','ytm-rich-section-renderer:has(ytm-reel-shelf-renderer)',
             'ytm-shelf-renderer:has(a[href*="/shorts/"])','ytm-video-with-context-renderer:has([data-style="SHORTS"])',
             'ytm-compact-video-renderer:has([data-style="SHORTS"])','ytm-rich-item-renderer:has([data-style="SHORTS"])'].forEach(function(x) { s += cssRule(guard + x); });
            s += cssRule('ytm-pivot-bar-item-renderer[tab-identifier="FEshorts"],ytm-pivot-bar-item-renderer[tab-identifier="shorts"],ytm-pivot-bar-item-renderer:has(.pivot-shorts),ytm-bottom-navigation-item-renderer[data-tab-identifier="shorts"],ytm-bottom-navigation-item-renderer[data-tab-identifier="FEshorts"]');
        }
        if (C.searchOnly) {
            s += cssRule('html[' + NS + '-home] ytm-rich-item-renderer,html[' + NS + '-home] ytm-rich-section-renderer,html[' + NS + '-home] ytm-chip-cloud-renderer');
            s += cssRule('html[' + NS + '-home] ytm-single-column-browse-results-renderer > .tab-content > ytm-section-list-renderer > lazy-list > *');
            s += cssRule('ytm-watch-next-secondary-results-renderer,ytm-compact-autoplay-renderer,.related-chips-slot-wrapper,ytm-autonav-toggle-button-renderer');
        }
        if (C.watchRecs) s += cssRule('ytm-item-section-renderer[section-identifier="related-items"],.related-items-container');
        if (C.comments) s += cssRule('ytm-comment-section-renderer,ytm-comments-entry-point-header-renderer,ytm-comments-entry-point-teaser-renderer,ytm-item-section-renderer[section-identifier="comment-item-section"],ytm-item-section-renderer[section-identifier="comments-entry-point"],ytm-engagement-panel-section-list-renderer[data-panel-identifier="engagement-panel-comments-section"],ytd-comments,#comments,ytd-comments-entry-point-header-renderer,ytd-engagement-panel-section-list-renderer[target-id="engagement-panel-comments-section"]');
        if (C.ads) {
            s += cssRule('ytm-promoted-sparkles-web-renderer,ytm-promoted-video-renderer,.ytm-promoted-sparkles-text-search-renderer,ytm-in-feed-ad-layout-renderer,ytm-ad-slot-renderer,ytm-companion-ad-renderer,ytm-feed-ad-rich-item-renderer,ytm-statement-banner-renderer');
            ['AD','Advertisement','ANUNCIO','ANNONCE','ANZEIGE','Werbung','ANÚNCIO','Publicidade','広告','إعلان','광고','ANNUNCIO','Pubblicità'].forEach(function(label) {
                s += cssRule('.ytm-badge-and-byline-item-byline:has([aria-label="' + label + '" i])');
            });
        }
        if (role === 'player') {
            s += cssRule('#movie_player,#player-container-id','touch-action:pan-x pinch-zoom!important');
            s += cssRule('video:fullscreen','object-fit:contain!important;background:#000!important;width:100vw!important;height:100vh!important;transform:none!important');
            s += cssRule('ytm-mobile-topbar-renderer,.mobile-topbar-renderer,.mobile-topbar-header-background,ytm-miniplayer-bar,ytm-miniplayer');
            s += cssRule('ytm-app,#app,body,ytm-watch,.page-container','padding-top:0!important;margin-top:0!important');
            s += cssRule('ytm-watch [slot="player"],#player-container-id,ytm-watch #player','margin-top:0!important;top:0!important');
        }
        if (S.youtube && S.youtube.pip) {
            s += cssRule('html,body','overflow:hidden!important;background:#000!important');
            s += cssRule('#player-container-id,#movie_player,video','position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;z-index:2147483647!important;object-fit:contain!important');
        }
        style.textContent = s;
    }
    // Strip only known YouTube player response fields, leaving the rest of the response intact.
    function stripAds(obj) {
        if (!C.ads || !obj || typeof obj !== 'object') return obj;
        ['adPlacements','adSlots','playerAds','adBreakParams','adBreakHeartbeatParams','adPlaybackContext','adTagParameters','adSafetyConfig','adSlotLoggingData','instreamAdBreakParams'].forEach(function(k) { delete obj[k]; });
        if (obj.playerConfig) { delete obj.playerConfig.adRequestConfig; delete obj.playerConfig.adsRequestConfig; }
        if (obj.responseContext) delete obj.responseContext.adSignalsInfo;
        if (obj.playerResponse) stripAds(obj.playerResponse);
        return obj;
    }
    function apiURL(u) { return /\/youtubei\/v1\/(player|next|get_watch|reel_watch_sequence)(\?|$)/.test(u || ''); }
    var originalParse = JSON.parse;
    JSON.parse = function() { return stripAds(originalParse.apply(this, arguments)); };
    ['ytInitialPlayerResponse','ytInitialData'].forEach(function(k) {
        var descriptor = Object.getOwnPropertyDescriptor(window, k);
        if (descriptor && (!descriptor.configurable || descriptor.get || descriptor.set)) return;
        var value = stripAds(window[k]);
        try { Object.defineProperty(window, k, {configurable:true, enumerable:true, get:function(){return value;}, set:function(v){value=stripAds(v);}}); } catch (_) {}
    });
    var originalFetch = window.fetch;
    if (originalFetch) window.fetch = function(input) {
        var u = typeof input === 'string' ? input : input && input.url;
        return originalFetch.apply(this, arguments).then(function(response) {
            if (!C.ads || !apiURL(u) || !response.ok) return response;
            return response.clone().text().then(function(txt) {
                if (!C.ads) return response;
                // MWEB sometimes sends XSSI-prefixed JSON: rename exact ad keys without parsing.
                var cleaned = txt.replace(/"(adPlacements|adSlots|playerAds|adPlaybackContext|adSignalsInfo|adSlotLoggingData)"\s*:/g, '"$1_":');
                if (cleaned === txt) return response;
                var headers = new Headers(response.headers); headers.delete('content-length'); headers.delete('content-encoding');
                return new Response(cleaned, {status:response.status, statusText:response.statusText, headers:headers});
            }).catch(function(){return response;});
        });
    };
    var originalResponseJSON = Response.prototype.json;
    Response.prototype.json = function() { return originalResponseJSON.apply(this, arguments).then(stripAds); };
    var xhrOpen = XMLHttpRequest.prototype.open;
    XMLHttpRequest.prototype.open = function(method, url) { this[NS] = String(url); return xhrOpen.apply(this, arguments); };
    var xhrSend = XMLHttpRequest.prototype.send;
    XMLHttpRequest.prototype.send = function() {
        var xhr = this;
        if (apiURL(xhr[NS])) xhr.addEventListener('readystatechange', function scrub() {
            if (xhr.readyState !== 4) return;
            xhr.removeEventListener('readystatechange', scrub);
            if (!C.ads || (xhr.responseType && xhr.responseType !== 'text')) return;
            try {
                var out = JSON.stringify(stripAds(originalParse(xhr.responseText)));
                Object.defineProperty(xhr,'responseText',{configurable:true,get:function(){return out;}});
                Object.defineProperty(xhr,'response',{configurable:true,get:function(){return out;}});
            } catch (_) {}
        });
        return xhrSend.apply(this, arguments);
    };
    function route() {
        var p = location.pathname, id = shortId(p);
        if (C.shorts && (id || p === '/shorts')) {
            if (C.profiles && id && (id === allowedShort || (!allowedShort && isChannel(channel)))) allowedShort = id;
            else {
                var target = allowedShort && C.profiles ? channel || '/' : id ? '/watch?v=' + encodeURIComponent(id) : '/';
                allowedShort = ''; location.replace('https://m.youtube.com' + target); return;
            }
        } else { channel = isChannel(p) ? p : ''; allowedShort = ''; }
        if (document.documentElement) {
            document.documentElement.toggleAttribute(NS + '-channel', C.profiles && isChannel(p));
            document.documentElement.toggleAttribute(NS + '-home', p === '/');
        }
        if (role === 'feed' && lastRoute !== location.href && watchURL(location.href)) post('open', location.href);
        if (lastRoute !== location.href) { lastRoute = location.href; lastQuality = ''; }
    }
    ['pushState','replaceState'].forEach(function(name) {
        var original = history[name];
        history[name] = function(state, unused, url) {
            if (role === 'feed' && url && watchURL(url) && bridge) { post('open', watchURL(url)); return; }
            var result = original.apply(this, arguments); route(); return result;
        };
    });
    window.addEventListener('popstate', route);
    document.addEventListener('click', function(e) {
        lastTouch = Date.now();
        var target = e.target.closest ? e.target : e.target.parentElement;
        if (!target || !bridge) return;
        if (role === 'player') {
            var link=target.closest('a[href]');
            if(link) {try {var u=new URL(link.href);if(u.protocol==='https:' && /^(m\.|www\.)?youtube\.com$/.test(u.hostname) && isChannel(u.pathname)) {
                e.preventDefault();e.stopImmediatePropagation();post('browse',u.href);
            }}catch(_){} }
            return;
        }
        // Preview controls, overflow menus, channel links and captions retain their own actions.
        if (target.closest('button,[role="button"],input,[class*="unmute" i],[class*="caption" i]')) return;
        var a = target.closest('a[href]');
        if (!a) {
            var card = target.closest('ytm-video-with-context-renderer,ytm-compact-video-renderer,ytm-rich-item-renderer');
            if (card) a = card.querySelector('a[href*="/watch?"]');
        }
        var url = a && watchURL(a.href);
        if (url) { e.preventDefault(); e.stopImmediatePropagation(); post('open', url); }
    }, true);
    var fullScreenTouch=null;
    document.addEventListener('touchstart',function(e){
        lastTouch=Date.now(); fullScreenTouch=null;
        var v=video(),t=e.touches&&e.touches[0];
        if(role!=='player'||!v||!t||document.fullscreenElement)return;
        var r=v.getBoundingClientRect();
        if(t.clientY>=r.top && t.clientY<=r.bottom && t.clientX>=r.left && t.clientX<=r.right)
            fullScreenTouch={x:t.clientX,y:t.clientY,scroll:window.scrollY};
    },{capture:true,passive:true});
    document.addEventListener('touchend',function(e){
        var start=fullScreenTouch,t=e.changedTouches&&e.changedTouches[0];fullScreenTouch=null;
        if(!start||!t||Math.abs(window.scrollY-start.scroll)>4)return;
        var dy=start.y-t.clientY,dx=Math.abs(start.x-t.clientX),v=video();
        // A real touchend retains Chromium's user activation for requestFullscreen.
        if(v && dy>64 && dy>dx*1.5 && v.requestFullscreen){
            e.preventDefault();v.requestFullscreen().catch(function(){});
        }
    },{capture:true,passive:false});
    document.addEventListener('touchcancel',function(){fullScreenTouch=null;},{passive:true});
    document.addEventListener('pause', function(e) {
        if (e.target.tagName === 'VIDEO' && Date.now() - lastTouch < 300 && !suspended) userPaused = true;
    }, true);
    document.addEventListener('play', function(e) {
        if (e.target.tagName !== 'VIDEO') return;
        if (C.feedAutoplay && !/^\/(watch|shorts)(\/|$)/.test(location.pathname)) { e.target.pause(); return; }
        if(userPaused && Date.now()-lastTouch>300){e.target.pause();return;}
        userPaused = false;
    }, true);
    // Dynamic visibility override: disabling background play restores the real browser signal.
    var hidden = Object.getOwnPropertyDescriptor(Document.prototype,'hidden');
    var visibility = Object.getOwnPropertyDescriptor(Document.prototype,'visibilityState');
    function keepAlive() { return role === 'player' && (C.background || (S.youtube && S.youtube.pip)) && !userPaused && !suspended; }
    var originalFocus=document.hasFocus;
    document.hasFocus=function(){return keepAlive() || originalFocus.call(document);};
    // MWEB treats an Android Activity smaller than one third of the display as
    // its own PiP session and pauses it on every timeupdate. Android owns this
    // PiP session; preserve screen-sized outer metrics only while it is active.
    // innerWidth/innerHeight stay real so the video and captions still lay out.
    ['outerWidth','outerHeight'].forEach(function(name){
        var descriptor=Object.getOwnPropertyDescriptor(window,name);
        if(!descriptor || !descriptor.get || !descriptor.configurable)return;
        Object.defineProperty(window,name,{configurable:true,get:function(){
            if(role==='player' && S.youtube && S.youtube.pip)return name==='outerWidth'?screen.width:screen.height;
            return descriptor.get.call(window);
        }});
    });
    try {
        if (hidden && hidden.get) Object.defineProperty(document,'hidden',{configurable:true,get:function(){return keepAlive() ? false : hidden.get.call(document);}});
        if (visibility && visibility.get) Object.defineProperty(document,'visibilityState',{configurable:true,get:function(){return keepAlive() ? 'visible' : visibility.get.call(document);}});
    } catch (_) {}
    ['visibilitychange','webkitvisibilitychange','pagehide','pageshow','freeze','resume','blur','focus'].forEach(function(name){
        function guard(e){if(keepAlive())e.stopImmediatePropagation();}
        document.addEventListener(name,guard,true);window.addEventListener(name,guard,true);
    });
    var openLabels = ['open app','open the app','abrir app','ouvrir app','app öffnen','アプリを開く','apri app','فتح تطبيق','앱 열기'];
    function hideAppPrompts() {
        all('ytm-mobile-topbar-renderer a,ytm-mobile-topbar-renderer button,ytm-mobile-topbar-renderer [role="button"]').forEach(function(e) {
            if (openLabels.indexOf((e.textContent || '').trim().toLowerCase()) >= 0) e.style.setProperty('display','none','important');
        });
    }
    function quality() {
        if (role !== 'player' || C.quality === 'auto') return;
        var p = player(); if (!p || !p.getAvailableQualityLevels || !p.setPlaybackQualityRange) return;
        var levels = p.getAvailableQualityLevels() || [], marker = location.href + levels.join(',') + C.quality;
        if (!levels.length || marker === lastQuality) return;
        var rank = ['hd2160','hd1440','hd1080','hd720','large','medium','small','tiny'];
        var q = levels.indexOf(C.quality) >= 0 ? C.quality : rank.find(function(x){return levels.indexOf(x)>=0;});
        if (!q) return;
        try { p.setPlaybackQualityRange('tiny', q); if (p.setPlaybackQuality) p.setPlaybackQuality(q); lastQuality = marker; } catch (_) {}
    }
    var captionVideo = null, track = null, cue = null, captionText = '';
    function captions(v) {
        if (!C.captions || role !== 'player') { if (track) track.mode = 'disabled'; return; }
        if (captionVideo !== v) { captionVideo=v; track=null; cue=null; captionText=''; }
        if (!track) { try { track=v.addTextTrack('captions','SocialBreak',document.documentElement.lang || 'en'); } catch (_) { return; } }
        // Android PiP captures the full player, including HTML captions. Only mirror for native video fullscreen.
        track.mode = document.fullscreenElement === v ? 'showing' : 'hidden';
        var text = all('.ytp-caption-segment').map(function(x){return x.textContent;}).join(' ').trim();
        if (text === captionText) return;
        captionText=text;
        if (cue) { try { track.removeCue(cue); } catch (_) {} cue=null; }
        if (text) { try { cue=new VTTCue(v.currentTime,v.currentTime+8,text);track.addCue(cue); } catch (_) {} }
    }
    var sleepButton;
    function sleepControl(v) {
        if (!C.sleep || role !== 'player') { if (sleepButton) sleepButton.remove(); sleepButton=null; return; }
        var row = document.querySelector('#player-control-overlay .player-controls-top,.player-controls-top,.ytp-chrome-top-buttons');
        if (!row || !v) return;
        if (!sleepButton || !sleepButton.isConnected) {
            sleepButton=document.createElement('button'); sleepButton.type='button'; sleepButton.textContent='☾';
            sleepButton.setAttribute('aria-label','Sleep timer');
            sleepButton.style.cssText='position:fixed;z-index:2147483646;width:40px;height:40px;border:0;border-radius:20px;background:rgba(0,0,0,.3);color:white;font:26px sans-serif;padding:0';
            sleepButton.addEventListener('click',function(e){e.preventDefault();e.stopPropagation();post('sleep',true);});
            document.body.appendChild(sleepButton);
        }
        var r=v.getBoundingClientRect(), cs=getComputedStyle(row);
        var shown=cs.display!=='none' && cs.visibility!=='hidden' && Number(cs.opacity || 1)>.05;
        sleepButton.style.display=shown?'block':'none';
        sleepButton.style.left=Math.max(r.left+8,r.right-48)+'px';sleepButton.style.top=(r.top+8)+'px';
    }
    var dockFrame = 0, lastDock = '';
    function reportDock() {
        if (role !== 'feed') return;
        var nav = document.querySelector('ytm-pivot-bar-renderer,ytm-bottom-navigation-bar-renderer');
        var r = nav && nav.getBoundingClientRect(), cs = nav && getComputedStyle(nav);
        var inset = r && cs.display !== 'none' && cs.visibility !== 'hidden' && r.bottom > 0 && r.top < innerHeight
            ? Math.max(0, innerHeight - Math.max(0, r.top)) : 0;
        var value = JSON.stringify({inset:inset,viewport:innerHeight});
        if (value !== lastDock) { lastDock = value; post('dock',JSON.parse(value)); }
    }
    document.addEventListener('scroll', function() {
        if (!dockFrame) dockFrame = requestAnimationFrame(function(){dockFrame=0;reportDock();});
    }, {capture:true,passive:true});
    window.addEventListener('resize',reportDock);
    function tick() {
        try {
            bridge = window[NS + 'yt'];
            route(); reportDock(); if (!style || !style.isConnected) css(); hideAppPrompts(); quality();
            var v=video();
            if (C.feedAutoplay && !/^\/(watch|shorts)(\/|$)/.test(location.pathname)) all('video').forEach(function(x){if(!x.paused)x.pause();});
            if (v) {
                captions(v); sleepControl(v);
                if(keepAlive() && hidden && hidden.get && hidden.get.call(document) && v.paused && !v.ended && v.readyState>=2) v.play().catch(function(){});
            }
            if (C.ads) {
                stripAds(window.ytInitialPlayerResponse);
                var p=player();
                if (p && (p.classList.contains('ad-showing') || p.classList.contains('ad-interrupting'))) {
                    all('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button,.ytp-ad-skip-button-container button,.ytp-ad-overlay-close-button',p).forEach(function(b){if(b.offsetHeight)b.click();});
                    var skips=['skip','saltar','ignorer','überspringen','スキップ','pular','salta','تخطّي','건너뛰기'];
                    all('button[aria-label],[role="button"][aria-label]',p).forEach(function(b){
                        var label=(b.getAttribute('aria-label')||'').toLowerCase();
                        if(b.offsetHeight && skips.some(function(word){return label.indexOf(word)>=0;}))b.click();
                    });
                }
            }
            var input=document.querySelector('input[name="search_query"],input#search,ytm-searchbox input,input.ytSearchboxComponentInput,form[role="search"] input');
            var search=!!input && document.activeElement===input || location.pathname==='/results' || new URL(location.href).searchParams.has('search_query');
            var title='', id='';
            try { var data=player() && player().getVideoData(); if(data){title=data.title || '';id=data.video_id || '';} } catch (_) {}
            if (!title) title=(document.title || '').replace(/\s*-\s*YouTube\s*$/,'');
            var chromeNode=document.querySelector('#player-control-overlay,.ytp-chrome-top');
            var chromeStyle=chromeNode && getComputedStyle(chromeNode);
            var chrome=!chromeStyle || (chromeStyle.display!=='none' && chromeStyle.visibility!=='hidden' && Number(chromeStyle.opacity || 1)>.05);
            var report=JSON.stringify({sleepMounted:!!(sleepButton&&sleepButton.isConnected),chrome:chrome,url:location.href,search:search,home:location.pathname==='/',title:title,id:id,ready:!!(v && v.readyState>=3 && v.videoWidth>0 && v.currentTime>.05),playing:!!(v&&!v.paused&&!v.ended),scroll:window.scrollY});
            if(report!==lastReport){lastReport=report;post('state',originalParse(report));}
        } catch (_) {}
    }
    S.youtube = {
        configure:function(c){
            if(C.quality!=='auto' && c.quality==='auto') {try {var p=player();if(p&&p.setPlaybackQuality)p.setPlaybackQuality('auto');if(p&&p.setPlaybackQualityRange)p.setPlaybackQualityRange('tiny','hd2160');}catch(_){}}
            C=c;lastQuality='';css();route();tick();
        },
        role:function(value){role=value;bridge=window[NS+'yt'];css();tick();},
        pause:function(){userPaused=true;suspended=true;all('video').forEach(function(v){v.pause();});},
        resume:function(){suspended=false;},
        play:function(){suspended=false;userPaused=false;var v=video();if(v)v.play().catch(function(){});},
        search:function(){var i=document.querySelector('input[name="search_query"],input#search');if(i){i.focus();return;}location.href='https://m.youtube.com/results?search_query=';},
        setPip:function(on){this.pip=on;css();},
        sleepState:function(label){if(sleepButton){sleepButton.textContent=label || '☾';sleepButton.style.fontSize=label?'12px':'26px';sleepButton.setAttribute('aria-label',label?'Sleep timer: '+label+' remaining':'Sleep timer');}},
        volume:function(value){var v=video();if(v)v.volume=Math.max(0,Math.min(1,value));},
        config:function(){return C;}
    };
    S.pause=function(){if(!C.background)S.youtube.pause();};
    S.resume=function(){S.youtube.resume();tick();};
    css(); route(); tick(); interval=setInterval(tick,400);
    window.addEventListener('pagehide',function(){clearInterval(interval);interval=null;});
    window.addEventListener('pageshow',function(){if(!interval)interval=setInterval(tick,400);tick();});
})();
