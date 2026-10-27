// iOS InstagramFeedPlayIconHideInjector: fade the Home autoplay-pending glyph only.
// Never call play/pause, remove controls, or disable the underlying tap target.
(function () {
  if (!/^(www\.)?instagram\.com$/.test(location.hostname)) return;
  var config = /*CONFIG*/null, key = '__NAMESPACE__957';
  if (window[key]) { window[key].update(config); return; }
  if (!config.enabled) return;
  var mark = 'data-__NAMESPACE__958', states = new WeakMap(), observed = new Set();
  var visibility = new IntersectionObserver(function (entries) {
    entries.forEach(function (entry) {
      var state=states.get(entry.target);
      if (entry.isIntersecting && state && !state.manual) pending(entry.target,state);
    });
  });
  function home() { return config.enabled && !config.manualPlayback && location.pathname === '/'; }
  function eligible(v) { return home() && !v.closest('[role="dialog"],[aria-modal="true"]'); }
  function refresh(v, state) {
    var article = v.closest('article');
    if (!article) return;
    var pending = eligible(v) && v.paused && !v.ended && !v.error && !state.manual &&
      performance.now() < state.until;
    if (pending) article.setAttribute(mark, ''); else article.removeAttribute(mark);
  }
  function pending(v, state) {
    clearTimeout(state.timer);
    state.until = performance.now() + 3000;
    // If autoplay is denied or the media fails to start, expose the play affordance again.
    state.timer = setTimeout(function () { refresh(v, state); }, 3010);
    refresh(v, state);
  }
  function wire(v) {
    if (!observed.has(v)) { observed.add(v); visibility.observe(v); }
    var state = states.get(v);
    if (state) { refresh(v, state); return; }
    state = { manual:false, until:0, tap:-Infinity, pointer:null, timer:0 };
    states.set(v, state);
    var article = v.closest('article');
    if (!article) return;
    // Unlike a scroll starting on the article, a short tap can deliberately pause it.
    article.addEventListener('pointerdown', function (e) {
      if (!e.isTrusted) return;
      state.pointer = { id:e.pointerId, x:e.clientX, y:e.clientY, at:performance.now() };
    }, { passive:true, capture:true });
    article.addEventListener('pointermove', function (e) {
      var p = state.pointer;
      if (p && (Math.abs(e.clientX-p.x)>10 || Math.abs(e.clientY-p.y)>10)) state.pointer=null;
    }, { passive:true, capture:true });
    article.addEventListener('pointercancel', function () { state.pointer=null; }, { passive:true });
    article.addEventListener('pointerup', function (e) {
      var p=state.pointer;
      if (p && p.id===e.pointerId && performance.now()-p.at<500) state.tap=performance.now();
      state.pointer=null;
    }, { passive:true, capture:true });
    article.addEventListener('keydown', function (e) {
      if (e.isTrusted && (e.key===' ' || e.key==='Enter')) state.tap=performance.now();
    }, true);
    v.addEventListener('pause', function () {
      state.manual = performance.now()-state.tap<500;
      pending(v, state);
    });
    function playing() { state.manual=false; state.until=0; refresh(v,state); }
    v.addEventListener('play', playing);
    v.addEventListener('playing', playing);
    v.addEventListener('error', function () { state.until=0; refresh(v,state); });
    v.addEventListener('ended', function () { state.until=0; refresh(v,state); });
    v.addEventListener('emptied', function () { state.manual=false; pending(v,state); });
    pending(v,state);
  }
  function sweep() {
    if (!document.documentElement) return;
    observed.forEach(function (v) {
      if (!v.isConnected) { visibility.unobserve(v); observed.delete(v); clearTimeout(states.get(v).timer); }
    });
    if (!document.getElementById(key)) {
      var style=document.createElement('style'); style.id=key;
      // Preserve layout, pointer events and accessibility. Mute/replay controls are untouched.
      var playLabels=['Play','Reproducir','Lire','Abspielen','Wiedergabe','تشغيل','Reproduzir','Riproduci','再生'];
      style.textContent=playLabels.map(function(label) {
        return 'article['+mark+'] [aria-label="'+label+'" i],article['+mark+'] [aria-label="'+label+'" i] svg';
      }).join(',')+'{opacity:0!important;transition:none!important}';
      (document.head || document.documentElement).appendChild(style);
    }
    document.querySelectorAll('article['+mark+']').forEach(function (a) {
      if (!home() || !a.querySelector('video') || a.closest('[role="dialog"],[aria-modal="true"]')) a.removeAttribute(mark);
    });
    document.querySelectorAll('article video').forEach(wire);
  }
  window[key] = {
    update:function (next) { config=next; sweep(); },
    setManualPlayback:function (value) { if(config.manualPlayback!==value) { config.manualPlayback=value; sweep(); } }
  };
  new MutationObserver(sweep).observe(document, { childList:true, subtree:true });
  ['pushState','replaceState'].forEach(function (name) {
    var original=history[name];
    history[name]=function () { var result=original.apply(this,arguments); sweep(); return result; };
  });
  window.addEventListener('popstate',sweep);
  document.addEventListener('visibilitychange',sweep);
  sweep();
})();
