// Ported from iOS IGCommentSheet.trimChromeJS at 4a2f179. See docs/instagram-story-comments-2026-09-18.
(function(){
  if (window.__NAMESPACE_____NAMESPACE___slCmt) return; window.__NAMESPACE_____NAMESPACE___slCmt = true;
  var BAR_H = 44, GAP = 6, PH = 'What do you think of this?';
  var EMOJIS = ['\u2764\ufe0f','\ud83d\ude4c','\ud83d\udd25','\ud83d\udc4f','\ud83d\ude22','\ud83d\ude0d','\ud83d\ude2e','\ud83d\ude02'];
  var css = [
    'header, nav, [role="navigation"], header[role="banner"] { display:none !important; }',
    'article [role="presentation"], article video, article ul[role="listbox"], article img[decoding="auto"][srcset] { display:none !important; }',
    'html, body, main, [role="main"], main > div, main > section { background-color: var(--__NAMESPACE__sl-bg, transparent) !important; }',
    'html, body { overflow:hidden !important; height:100% !important; padding-bottom:0 !important; margin:0 !important; overscroll-behavior:none; }',
    '::-webkit-scrollbar { display:none; }',
    '#__NAMESPACE___slEmojiBar { position:fixed; left:0; right:0; height:' + BAR_H + 'px; display:flex; align-items:center; justify-content:space-evenly; padding:0 4px; z-index:2147483000; background:var(--__NAMESPACE__sl-bg,#fff); touch-action:none; -webkit-user-select:none; user-select:none; -webkit-tap-highlight-color:transparent; }',
    '#__NAMESPACE___slEmojiBar span { font-size:29px; line-height:1; display:block; transition:transform .12s; }',
    '#__NAMESPACE___slEmojiBar span.__NAMESPACE___slDown { transform:scale(1.28); }',
    '.__NAMESPACE___slPill { border:1px solid var(--__NAMESPACE__sl-line, #dbdbdb) !important; border-radius:24px !important; padding:0 16px !important; min-height:46px !important; display:flex !important; align-items:center !important; background:transparent !important; box-sizing:border-box !important; }',
    '.__NAMESPACE___slPill textarea, .__NAMESPACE___slPill [role="textbox"] { font-size:16px !important; line-height:20px !important; background:transparent !important; }',
    '[__NAMESPACE__data-sl-cont], #__NAMESPACE___slEmojiBar { transform: translateY(calc(-1 * var(--__NAMESPACE__sl-off, 0px))); will-change: transform; }',
    '[__NAMESPACE__data-sl-cont]::after { content:""; position:absolute; left:0; right:0; top:100%; height:400px; background:var(--__NAMESPACE__sl-bg,#fff); }',
    '[__NAMESPACE__data-sl-list] { padding-bottom: var(--__NAMESPACE__sl-off, 0px) !important; box-sizing: border-box !important; }',
    'body[__NAMESPACE__data-sl-modal] #__NAMESPACE___slEmojiBar, body[__NAMESPACE__data-sl-modal] [__NAMESPACE__data-sl-cont], body[__NAMESPACE__data-sl-freeze] #__NAMESPACE___slEmojiBar, body[__NAMESPACE__data-sl-freeze] [__NAMESPACE__data-sl-cont] { visibility:hidden !important; pointer-events:none !important; }'
  ].join('\n');
  function set(el, k, v){ if (el.style.getPropertyValue(k) !== v) el.style.setProperty(k, v, 'important'); }
  function textbox(){ return document.querySelector('form textarea, textarea, [role="textbox"], [contenteditable="true"]'); }
  function footer(el){ return el && el.closest && el.closest('[__NAMESPACE__data-sl-cont],#__NAMESPACE___slEmojiBar'); }
  function modal(){
    var cont = document.querySelector('[__NAMESPACE__data-sl-cont]');
    return Array.from(document.querySelectorAll('[role="dialog"],[role="alertdialog"],[aria-modal="true"]')).find(function(el){
      var r=el.getBoundingClientRect(), s=getComputedStyle(el);
      return r.width>0 && r.height>0 && s.display!=='none' && s.visibility!=='hidden' &&
        !(cont && (el.contains(cont) || cont.contains(el)));
    });
  }
  // The footer is a control surface, not the list or the sheet's grabber.
  window.__NAMESPACE__commentCanDrag = function(x,y){
    var el=document.elementFromPoint(x,y);
    if(!el || modal() || footer(el) || el.closest('textarea,input,[contenteditable="true"],[role="textbox"]')) return false;
    for(var n=el;n && n!==document.body && n!==document.documentElement;n=n.parentElement){
      if(n.scrollHeight>n.clientHeight+8 && /auto|scroll/.test(getComputedStyle(n).overflowY)) return n.scrollTop<=0;
    }
    return scrollY<=0;
  };
  // Chromium/Instagram can deliver an activation after a drag. Suppress only
  // that gesture; stationary taps and intentional context menus still work.
  var touch=null, suppressUntil=0;
  document.addEventListener('touchstart',function(e){
    suppressUntil=0;
    var t=e.touches[0];touch=t ? {x:t.clientX,y:t.clientY,moved:e.touches.length!==1} : null;
  },true);
  document.addEventListener('touchmove',function(e){
    var t=e.touches[0];
    if(touch && (!t || e.touches.length!==1 || Math.hypot(t.clientX-touch.x,t.clientY-touch.y)>10)) touch.moved=true;
  },{capture:true,passive:true});
  function endTouch(e){ if(touch && (touch.moved || e.type==='touchcancel')) suppressUntil=performance.now()+650;touch=null; }
  document.addEventListener('touchend',endTouch,true);
  document.addEventListener('touchcancel',endTouch,true);
  function suppressDragAction(e){
    if((touch && touch.moved) || performance.now()<suppressUntil || (e.type==='contextmenu' && footer(e.target))){
      e.preventDefault();e.stopImmediatePropagation();
    }
  }
  document.addEventListener('click',suppressDragAction,true);
  document.addEventListener('contextmenu',suppressDragAction,true);
  function chain(tb){
    var ih = window.innerHeight, n = tb, prev = null;
    while (n && n !== document.body) {
      var r = n.getBoundingClientRect();
      if (r.height > ih * 0.55 && r.top <= 2 && r.width > window.innerWidth * 0.8) return { root: n, cont: prev };
      prev = n; n = n.parentElement;
    }
    return null;
  }
  function insert(e){
    var tb = textbox(); if (!tb) return;
    try { tb.focus(); } catch(_) {}
    var ok = false;
    try { ok = document.execCommand('insertText', false, e); } catch(_) {}
    if (!ok) {
      try {
        var d = Object.getOwnPropertyDescriptor(Object.getPrototypeOf(tb), 'value');
        if (d && d.set) { d.set.call(tb, (tb.value || '') + e); } else { tb.value = (tb.value || '') + e; }
        tb.dispatchEvent(new Event('input', { bubbles: true }));
      } catch(_) {}
    }
  }
  function ensureBar(){
    var bar = document.getElementById('__NAMESPACE___slEmojiBar');
    if (bar) return bar;
    bar = document.createElement('div'); bar.id = '__NAMESPACE___slEmojiBar';
    EMOJIS.forEach(function(e){
      var s = document.createElement('span'); s.textContent = e;
      s.setAttribute('role','button');s.setAttribute('aria-label',e);s.tabIndex=0;
      var press=null,lastTouch=-1000;
      function clear(){press=null;s.classList.remove('__NAMESPACE___slDown');}
      s.addEventListener('touchstart', function(ev){
        ev.preventDefault();ev.stopPropagation();clear();lastTouch=performance.now();
        if(ev.touches.length===1 && !modal()){
          var t=ev.touches[0];press={x:t.clientX,y:t.clientY,at:lastTouch};s.classList.add('__NAMESPACE___slDown');
        }
      }, { passive: false });
      s.addEventListener('touchmove',function(ev){
        ev.preventDefault();ev.stopPropagation();var t=ev.touches[0];
        if(press && (!t || ev.touches.length!==1 || Math.hypot(t.clientX-press.x,t.clientY-press.y)>10))clear();
      },{passive:false});
      s.addEventListener('touchend', function(ev){
        ev.preventDefault();ev.stopPropagation();var tap=press && performance.now()-press.at<500;clear();lastTouch=performance.now();
        if(tap && !modal())insert(e);
      }, { passive: false });
      s.addEventListener('touchcancel',function(ev){ev.stopPropagation();clear();lastTouch=performance.now();});
      s.addEventListener('click',function(ev){ev.preventDefault();ev.stopPropagation();if(performance.now()-lastTouch>650 && !modal())insert(e);});
      s.addEventListener('keydown',function(ev){if((ev.key==='Enter'||ev.key===' ')&&!modal()){ev.preventDefault();insert(e);}});
      bar.appendChild(s);
    });
    document.body.appendChild(bar);
    return bar;
  }
  function hideTabBar(){
    var start = document.querySelector('[aria-label="Reels" i]') || document.querySelector('a[href="/reels/"]')
             || document.querySelector('[aria-label="Home" i]')  || document.querySelector('a[href="/"]');
    if (start) {
      var n = start;
      for (var i = 0; i < 11 && n && n !== document.body; i++) {
        var home  = n.querySelector('[aria-label="Home" i], a[href="/"]');
        var reels = n.querySelector('[aria-label="Reels" i], a[href="/reels/"]');
        if (home && reels) { set(n, 'display', 'none'); break; }
        n = n.parentElement;
      }
    }
    var els = document.body ? document.body.querySelectorAll('div,nav,footer,section') : [];
    var H = window.innerHeight;
    for (var k = 0; k < els.length; k++) {
      var el = els[k];
      if (el.id === '__NAMESPACE___slEmojiBar' || el.hasAttribute('__NAMESPACE__data-sl-cont')) continue;
      var cs = getComputedStyle(el);
      if (cs.position !== 'fixed' && cs.position !== 'sticky') continue;
      var r = el.getBoundingClientRect();
      if (r.height <= 0 || r.height > 110 || r.bottom < H - 2 || r.width < window.innerWidth * 0.8) continue;
      if (el.querySelector('input,textarea,[contenteditable="true"],[role="textbox"]')) continue;
      set(el, 'display', 'none');
    }
  }
  function apply(){
    window.__NAMESPACE_____NAMESPACE___slCmtRuns = (window.__NAMESPACE_____NAMESPACE___slCmtRuns || 0) + 1;
    try {
      window.__NAMESPACE_____NAMESPACE___slCmtStage = 'style';
      if (!document.getElementById('__NAMESPACE___slCmtStyle')) {
        var st = document.createElement('style'); st.id = '__NAMESPACE___slCmtStyle'; st.textContent = css;
        (document.head || document.documentElement).appendChild(st);
      }
      // The shared page rules can add inline overflow:auto, which wins over
      // the stylesheet. Like iOS, only the inner comments list may scroll.
      set(document.documentElement,'overflow','hidden');
      if(document.body)set(document.body,'overflow','hidden');
      hideTabBar();
      window.__NAMESPACE_____NAMESPACE___slCmtStage = 'textbox';
      var tb = textbox();
      var bar = document.getElementById('__NAMESPACE___slEmojiBar');
      if (!tb || !document.body) { window.__NAMESPACE_____NAMESPACE___slCmtStage = 'no-tb'; if (bar) bar.style.display = 'none'; return; }
      var c = chain(tb); if (!c || !c.cont) { window.__NAMESPACE_____NAMESPACE___slCmtStage = 'no-chain ' + (tb.tagName) + ' ' + Math.round(tb.getBoundingClientRect().top); if (bar) bar.style.display = 'none'; return; }
      window.__NAMESPACE_____NAMESPACE___slCmtStage = 'restyle';
      var root = c.root, cont = c.cont;
      var hasModal=!!modal();
      if(hasModal!==document.body.hasAttribute('__NAMESPACE__data-sl-modal')){
        if(hasModal)document.body.setAttribute('__NAMESPACE__data-sl-modal','1');else document.body.removeAttribute('__NAMESPACE__data-sl-modal');
      }
      if (tb.placeholder !== undefined && tb.placeholder !== PH) tb.placeholder = PH;
      var form = tb.closest('form') || tb.parentElement;
      if (form && !form.classList.contains('__NAMESPACE___slPill')) form.classList.add('__NAMESPACE___slPill');
      if (!cont.hasAttribute('__NAMESPACE__data-sl-cont')) cont.setAttribute('__NAMESPACE__data-sl-cont', '1');
      set(cont, 'position', 'fixed'); set(cont, 'left', '0'); set(cont, 'right', '0'); set(cont, 'bottom', '0');
      set(cont, 'width', '100%'); set(cont, 'z-index', '2147482999'); set(cont, 'background-color', 'var(--__NAMESPACE__sl-bg, #fff)');
      set(cont, 'padding-top', (BAR_H + GAP) + 'px'); set(cont, 'padding-bottom', '6px'); set(cont, 'margin', '0');
      set(cont, 'box-sizing', 'border-box');
      var ch = cont.offsetHeight;
      set(root, 'height', '100vh'); set(root, 'min-height', '100vh'); set(root, 'max-height', '100vh');
      set(root, 'box-sizing', 'border-box'); set(root, 'padding-bottom', ch + 'px');
      // IG sizes the list scroller with an inline px height computed for ITS composer + tab bar; make it
      // fill exactly the space above ours.
      for (var ci = 0; ci < root.children.length; ci++) {
        var kid = root.children[ci];
        if (kid === cont || kid.id === '__NAMESPACE___slEmojiBar') continue;
        if (kid.getBoundingClientRect().height < 100) continue;
        if (!kid.hasAttribute('__NAMESPACE__data-sl-list')) kid.setAttribute('__NAMESPACE__data-sl-list', '1');
        set(kid, 'height', 'calc(100vh - ' + ch + 'px)'); set(kid, 'max-height', 'none'); set(kid, 'min-height', '0'); set(kid, 'flex', 'none');
      }
      bar = ensureBar();
      var b = (ch - BAR_H) + 'px';
      if (bar.style.bottom !== b) bar.style.bottom = b;
      if (bar.style.display !== 'flex') bar.style.display = 'flex';
      window.__NAMESPACE_____NAMESPACE___slCmtStage = 'done';
    } catch(e) { window.__NAMESPACE_____NAMESPACE___slCmtErr = String(e && e.stack || e); }
  }
  var pending = false;
  function schedule(){ if (pending) return; pending = true; requestAnimationFrame(function(){ pending = false; apply(); }); }
  apply();
  document.addEventListener('DOMContentLoaded', schedule, { once: true });
  window.addEventListener('resize', schedule);
  // [IG-CMT-COMPOSER-REMEASURE-2026-09-11] Malek: "the bottom part is at the wrong height and gets cut
  // off", reproducibly on a reel opened from a DM.
  //
  // `ch = cont.offsetHeight` is what the root's padding-bottom and the list's `calc(100vh - ch)` are
  // built from, so if the composer is measured while it is still SHORT, the list is sized too tall and
  // the composer's own content spills past the bottom of the viewport. The composer contains the user's
  // avatar <img>: until that image has data, the row is shorter than its final height.
  //
  // Nothing here caught that. The MutationObserver watches childList/attributes, and an <img> finishing
  // its load mutates NEITHER — no node added, no attribute changed, just a reflow. `resize` doesn't fire
  // either. So the one measurement taken before the avatar landed was the one we kept.
  //
  // ResizeObserver watches the box itself, which is the actual thing we care about, and `load` on the
  // composer's images covers browsers/paths where the observer isn't available. Both funnel through the
  // existing rAF-coalesced `schedule()`, and `apply()`'s writes are already no-op guarded, so a
  // re-measure that finds the same height costs nothing and cannot feed the MutationObserver.
  try {
    var ro = new ResizeObserver(function(){ schedule(); });
    var seen = null;
    var watch = function(){
      var c = document.querySelector('[__NAMESPACE__data-sl-cont]');
      if (c && c !== seen) { if (seen) { try { ro.unobserve(seen); } catch(e){} } ro.observe(c); seen = c; }
    };
    watch();
    document.addEventListener('load', function(e){
      var t = e.target;
      if (t && t.tagName === 'IMG' && t.closest && t.closest('[__NAMESPACE__data-sl-cont]')) schedule();
      watch();
    }, true);
    setTimeout(watch, 400); setTimeout(watch, 1200);
  } catch (e) {}
  try { new MutationObserver(schedule).observe(document.documentElement, { childList: true, subtree: true, attributes: true, attributeFilter: ['style', 'class'] }); } catch(e) {}
})();
