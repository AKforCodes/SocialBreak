/* iOS main a8b6ff4: InstagramProfileScrollMemoryInjector + InstagramVoiceNoticeInjector.
 * Kid-only; voice notice limited to an audio-only DM recording. No message/audio data leaves the page. */
(function(config){
if(config.hosts.indexOf(location.hostname.toLowerCase()) < 0) return;
        (function(){
          if (window.__NAMESPACE__102) return;
          window.__NAMESPACE__102 = true;

          var MEM = {};            // path -> { y, at, key }
          var TTL = 120000;        // a stored position older than 2 minutes is stale
          var last = null;         // path we were on at the previous tick
          var lastY = 0;           // its scroll offset, sampled continuously
          var lastKey = null;      // home only: identity of the first post, sampled continuously

          // Reserved first segments that are routes, not usernames. A username cannot contain "/",
          // so a single-segment path that is not one of these is a profile.
          var RESERVED = ['p','reel','reels','stories','explore','direct','accounts','tv','about',
                          'legal','directory','web','challenge','your_activity','settings'];

          function pathNow(){
            var p = location.pathname.replace(/\/+$/, '');
            return p === '' ? '/' : p;
          }
          function isProfile(p){
            if (!p || p === '/') return false;
            var segs = p.split('/').filter(function(s){ return s.length; });
            if (segs.length !== 1) return false;
            return RESERVED.indexOf(segs[0].toLowerCase()) === -1;
          }
          function isTracked(p){ return p === '/' || isProfile(p); }
          function scroller(){ return document.scrollingElement || document.documentElement; }

          // Home only: the first post's href stands in for "is this the same feed I left?". Same
          // signal the native saveHomeScroll uses, so both halves agree on what a refresh looks like.
          function feedKey(p){
            if (p !== '/') return null;
            try {
              var a = document.querySelector('article a[href*="/p/"], article a[href*="/reel/"]');
              return a ? a.getAttribute('href') : null;
            } catch (e) { return null; }
          }

          // Sample the CURRENT page's offset continuously. Cheap: two property reads plus, on home,
          // one querySelector.
          setInterval(function(){
            try {
              var p = pathNow();
              if (p === last) {
                if (isTracked(p)) { lastY = scroller().scrollTop; lastKey = feedKey(p); }
                return;
              }
              // Path changed — bank where we were, then consider restoring where we arrived.
              if (last && isTracked(last) && lastY > 200) MEM[last] = { y: lastY, at: Date.now(), key: lastKey };
              last = p; lastY = scroller().scrollTop; lastKey = feedKey(p);
              if (isTracked(p)) tryRestore(p);
            } catch (e) {}
          }, 250);

          function tryRestore(p){
            var s = MEM[p];
            if (!s || Date.now() - s.at > TTL) return;
            // The swipe controller banked this trip itself and its restore is already scheduled.
            // Only a FRESH save means that: its restore bails without clearing the slot when the feed
            // has changed under it, so an old one would otherwise mute us for the rest of the session.
            if (p === '/') {
              var ns = window.__NAMESPACE__100;
              if (ns && ns.at && Date.now() - ns.at < 8000) { delete MEM[p]; return; }
            }
            var tries = 0;
            (function poll(){
              try {
                if (pathNow() !== p) return;                 // navigated away again
                var se = scroller();
                if (se.scrollTop > 200) { delete MEM[p]; return; }   // IG (or the user) already moved it
                if (p === '/' && s.key) {
                  var now = feedKey('/');
                  if (!now) { if (tries++ < 30) setTimeout(poll, 100); return; }  // feed not built yet
                  if (now !== s.key) { delete MEM[p]; return; }      // refreshed feed → leave it at the top
                }
                if (se.scrollHeight > s.y + window.innerHeight * 0.5) {
                  se.scrollTop = s.y;
                  window.scrollTo(0, s.y);
                  delete MEM[p];                              // one restore per arrival
                  return;
                }
              } catch (e) {}
              if (tries++ < 30) setTimeout(poll, 100);        // ~3s for the page to fill
            })();
          }
        })();
        (function(){
          if (window.__NAMESPACE__101) return;
          window.__NAMESPACE__101 = true;
          if (!window.MediaRecorder) return;

          function pill(text, stay){
            try {
              var id = '__NAMESPACE__103';
              var el = document.getElementById(id);
              if (!el) {
                el = document.createElement('div');
                el.id = id;
                el.style.cssText = 'position:fixed;left:50%;transform:translateX(-50%);bottom:88px;z-index:2147483647;'
                  + 'max-width:80%;padding:9px 15px;border-radius:18px;font:600 13px -apple-system,system-ui,sans-serif;'
                  + 'color:#fff;background:rgba(20,20,22,0.92);box-shadow:0 6px 22px rgba(0,0,0,0.35);'
                  + 'text-align:center;pointer-events:none;opacity:0;transition:opacity .2s ease;';
                document.documentElement.appendChild(el);
              }
              el.textContent = text;
              el._noticeActive = true;
              requestAnimationFrame(function(){ if (el._noticeActive) el.style.opacity = '1'; });
              clearTimeout(el._t);
              if (stay > 0) el._t = setTimeout(function(){ el._noticeActive = false; el.style.opacity = '0'; }, stay);
            } catch (e) {}
          }
          function hidePill(){ var el = document.getElementById('__NAMESPACE__103'); if (el) { el._noticeActive = false; el.style.opacity = '0'; } }

          var Orig = window.MediaRecorder;
          function Wrapped(stream, opts){
            var mr = new Orig(stream, opts);
            if (!/^\/direct(?:\/|$)/.test(location.pathname) || (stream.getVideoTracks && stream.getVideoTracks().length > 0)) return mr;
            var _start = mr.start, _stop = mr.stop, warnT = null;
            mr.start = function(){
              pill('Voice notes are capped at 1 minute', 3500);
              clearTimeout(warnT);
              // Warn ~10s before Instagram's 60s cut, and leave it up until the take ends.
              warnT = setTimeout(function(){ pill('About 10s left — send before 1:00', 0); }, 50000);
              return _start.apply(mr, arguments);
            };
            mr.stop = function(){ clearTimeout(warnT); hidePill(); return _stop.apply(mr, arguments); };
            mr.addEventListener('stop', function(){ clearTimeout(warnT); hidePill(); });
            return mr;
          }
          try {
            Wrapped.isTypeSupported = Orig.isTypeSupported ? Orig.isTypeSupported.bind(Orig) : undefined;
            Wrapped.prototype = Orig.prototype;
          } catch (e) {}
          window.MediaRecorder = Wrapped;
        })();
})(/*CONFIG*/null);
