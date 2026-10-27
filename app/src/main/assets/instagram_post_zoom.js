// iOS InstagramPinchZoomInjector / InstagramVideoZoomInjector, adapted for Android.
(function () {
  var experience = window['__NAMESPACE__experience'];
  if (!experience || experience.zoom || window !== window.top) return;
  var overlay = null, mirror = null, source = null, active = false, revision = 0;
  var frame = 0, watchdog = 0, releaseTimer = 0, lastDim = -1, lastBusy = false;
  var startDistance = 1, startX = 0, startY = 0, scale = 1, tx = 0, ty = 0;
  var sourceRect = null, sourceUrl = '', drawVideo = null;
  function report(dim) {
    var q = Math.round(Math.max(0, Math.min(.72, dim)) * 20) / 20;
    var busy = !!overlay;
    if (q === lastDim && busy === lastBusy) return;
    lastDim = q; lastBusy = busy;
    try { window['__NAMESPACE__zoom'].postMessage(JSON.stringify({dim:q,active:busy})); } catch (_) {}
  }
  function permitted(el) {
    if (!experience.zoomAllowed() || !el || !el.isConnected || el.closest('[hidden],[aria-hidden="true"],dialog,[role="dialog"],[aria-modal="true"]')) return false;
    var r = el.getBoundingClientRect();
    if (r.width < 140 || r.height < 140) return false;
    for (var n = el; n && n !== document.body; n = n.parentElement) {
      var s = getComputedStyle(n);
      if (s.display === 'none' || s.visibility === 'hidden' || Number(s.opacity) === 0) return false;
      if (n === el && /50%/.test(s.borderRadius)) return false;
    }
    return el.tagName === 'IMG' ? el.complete && el.naturalWidth > 0 && !!(el.currentSrc || el.src) :
      el.tagName === 'VIDEO' && el.readyState >= 2 && el.videoWidth > 0;
  }
  function mediaAt(x, y) {
    var stack = document.elementsFromPoint(x, y);
    // Hit-test through IG's tap-catching overlays, without searching unrelated posts.
    for (var i = 0; i < Math.min(stack.length, 12); i++) {
      var n = stack[i], candidates = n.matches('img,video') ? [n] : Array.from(n.querySelectorAll(':scope > img,:scope > video'));
      for (var j = 0; j < candidates.length; j++) {
        var el = candidates[j], r = el.getBoundingClientRect();
        if (x >= r.left && x < r.right && y >= r.top && y < r.bottom && permitted(el)) return el;
      }
    }
    return null;
  }
  function valid() {
    if (!overlay || !permitted(source)) return false;
    var r = source.getBoundingClientRect();
    return Math.abs(r.left-sourceRect.left)<2 && Math.abs(r.top-sourceRect.top)<2 &&
      Math.abs(r.width-sourceRect.width)<2 && Math.abs(r.height-sourceRect.height)<2 &&
      (source.tagName !== 'IMG' || (source.currentSrc || source.src) === sourceUrl);
  }
  function clear() {
    clearTimeout(watchdog); clearTimeout(releaseTimer); cancelAnimationFrame(frame);
    document.removeEventListener('touchmove', move, true);
    if (overlay) { overlay.remove(); revision++; }
    overlay = mirror = source = drawVideo = null; active = false; frame = 0;
    report(0);
  }
  function release(animated) {
    if (!overlay) return;
    if (!animated || !valid()) { clear(); return; }
    active = false;
    document.removeEventListener('touchmove', move, true);
    mirror.style.transition = 'transform .22s cubic-bezier(.2,.8,.25,1)';
    overlay.style.transition = 'background .22s linear';
    mirror.style.transform = 'translate(0px,0px) scale(1)';
    overlay.style.background = 'rgba(0,0,0,0)';
    report(0);
    // Remain uncacheable throughout the spring-back, including the underlying socket.
    releaseTimer = setTimeout(clear, 260);
  }
  function apply() {
    mirror.style.transform = 'translate('+tx+'px,'+ty+'px) scale('+scale+')';
    var dim = Math.min((scale-1)*.45,.72);
    overlay.style.background = 'rgba(0,0,0,'+dim+')'; report(dim);
  }
  function tick() {
    if (!valid()) { clear(); return; }
    if (drawVideo) { try { drawVideo(); } catch (_) { clear(); return; } }
    frame = requestAnimationFrame(tick);
  }
  function build(el) {
    source = el; sourceRect = el.getBoundingClientRect(); sourceUrl = el.currentSrc || el.src || '';
    var r = sourceRect, cs = getComputedStyle(el);
    overlay = document.createElement('div'); overlay.setAttribute('data-sl-post-zoom','');
    overlay.setAttribute('aria-hidden','true');
    overlay.style.cssText = 'position:fixed;inset:0;z-index:2147483600;pointer-events:none;background:transparent;';
    var socket = document.createElement('div');
    var rectStyle = 'position:fixed;left:'+r.left+'px;top:'+r.top+'px;width:'+r.width+'px;height:'+r.height+'px;';
    socket.style.cssText = rectStyle+'background:#3a3a3c;border-radius:'+cs.borderRadius+';visibility:hidden;';
    overlay.appendChild(socket);
    mirror = document.createElement(el.tagName === 'IMG' ? 'img' : 'canvas');
    mirror.style.cssText = rectStyle+'object-fit:'+cs.objectFit+';object-position:'+cs.objectPosition+';border-radius:'+cs.borderRadius+';transform-origin:center;will-change:transform;';
    if (el.tagName === 'IMG') {
      var copy = mirror;
      copy.onload = function () { if (mirror === copy && copy.naturalWidth) socket.style.visibility = 'visible'; };
      copy.onerror = function () { if (mirror === copy) clear(); };
      copy.src = sourceUrl;
      if (copy.complete && copy.naturalWidth) socket.style.visibility = 'visible';
      setTimeout(function () { if (mirror === copy && (!copy.complete || !copy.naturalWidth)) clear(); }, 250);
    } else {
      // Display-only canvas: cross-origin video can be mirrored without reading pixels
      // or creating another player. The original keeps its playback, audio and listeners.
      var ratio = Math.min(devicePixelRatio || 1, 2, 2048/Math.max(r.width,r.height));
      mirror.width = Math.max(1,Math.round(r.width*ratio)); mirror.height = Math.max(1,Math.round(r.height*ratio));
      var ctx = mirror.getContext('2d'), canvas = mirror;
      if (!ctx) throw Error('No video canvas');
      drawVideo = function () {
        var w=el.videoWidth,h=el.videoHeight,cw=canvas.width,ch=canvas.height;
        var fit=cs.objectFit, factor=fit==='cover'?Math.max(cw/w,ch/h):Math.min(cw/w,ch/h);
        var dw=fit==='fill'?cw:w*factor,dh=fit==='fill'?ch:h*factor;
        ctx.fillStyle='black';ctx.fillRect(0,0,cw,ch);
        ctx.drawImage(el,(cw-dw)/2,(ch-dh)/2,dw,dh);
        socket.style.visibility='visible';
      };
      drawVideo();
    }
    overlay.appendChild(mirror); document.body.appendChild(overlay); revision++;
    apply(); tick();
  }
  function distance(t) { return Math.hypot(t[0].clientX-t[1].clientX,t[0].clientY-t[1].clientY) || 1; }
  function start(e) {
    if (e.touches.length !== 2) { if (active && e.touches.length>2) clear(); return; }
    if (overlay) return;
    try {
      var t=e.touches, x=(t[0].clientX+t[1].clientX)/2,y=(t[0].clientY+t[1].clientY)/2;
      var el=mediaAt(x,y); if (!el) return;
      startDistance=distance(t); startX=x; startY=y; scale=1;tx=ty=0;active=true;
      build(el);
      document.addEventListener('touchmove',move,{capture:true,passive:false});
      watchdog=setTimeout(clear,12000);
      e.preventDefault();e.stopImmediatePropagation();
    } catch (_) { clear(); }
  }
  function move(e) {
    if (!active) return;
    if (e.touches.length !== 2 || !valid()) { clear(); return; }
    var t=e.touches;
    scale=Math.max(1,Math.min(6,distance(t)/startDistance));
    tx=(t[0].clientX+t[1].clientX)/2-startX;ty=(t[0].clientY+t[1].clientY)/2-startY;
    apply();e.preventDefault();e.stopPropagation();
  }
  document.addEventListener('touchstart',start,{capture:true,passive:false});
  document.addEventListener('touchend',function(e){if(active){e.preventDefault();e.stopPropagation();release(true);}},{capture:true,passive:false});
  document.addEventListener('touchcancel',function(){clear();},true);
  document.addEventListener('visibilitychange',function(){if(document.hidden)clear();},true);
  window.addEventListener('pagehide',clear,true); window.addEventListener('resize',clear);
  experience.zoom = {cancel:clear,validate:function(){if(overlay&&!valid())clear();},
    get busy(){return !!overlay;},get revision(){return revision;}};
})();
