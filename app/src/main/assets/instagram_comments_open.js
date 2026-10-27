// Adapted from iOS InstagramCommentKeepPlayingInjector (4a2f179):
// leave the playing post mounted and open its real comments in a second browser.
(function(){
  if (!/^(www\.)?instagram\.com$/.test(location.hostname)) return;
  var key='__NAMESPACE__commentsInstalled', bridge='__NAMESPACE__comments';
  if(window[key]) return; window[key]=true;
  function visible(el){var r=el.getBoundingClientRect();return r.width>0&&r.height>0;}
  function control(t){
    if(!t || !t.closest) return null;
    var c=t.closest('[aria-label*="omment" i],a[href*="/comments"]');
    if(c) return c;
    var b=t.closest('button,[role="button"],a');
    if(!b) return null;
    if(b.querySelector('[aria-label*="omment" i]')) return b;
    if(/^[\d.,]+\s*[KkMm]?$/.test((b.textContent||'').trim())){
      var prev=b.previousElementSibling, par=b.parentElement;
      if((prev&&prev.querySelector('[aria-label*="omment" i]')) ||
         (par&&par.firstElementChild!==b&&par.firstElementChild.querySelector('[aria-label*="omment" i]'))) return b;
    }
    return null;
  }
  function code(t){
    var api=window['__NAMESPACE__experience'];
    if(!api || !api.commentsAllowed() || !window[bridge] || !window[bridge].postMessage) return '';
    if(/\/comments\/?$/.test(location.pathname)) return '';
    var c=control(t), article=c&&c.closest('article');
    if(!article || !visible(c)) return '';
    // Preserve iOS's explicit permalink priority. A single combined selector
    // returns DOM order and can mistake /reels/audio/<id>/ for a post named "audio".
    var candidates=Array.from(article.querySelectorAll('a[href*="/p/"]'))
      .concat(Array.from(article.querySelectorAll('a[href*="/reel/"]')),
              Array.from(article.querySelectorAll('a[href*="/reels/"]')));
    function postCode(path){
      var m=path.match(/^\/(?:[^/]+\/)?(?:p|reel|reels)\/([A-Za-z0-9_-]+)(?:\/|$)/);
      return m && !/^(audio|videos|tagged)$/.test(m[1]) ? m[1] : '';
    }
    for(var i=0;i<candidates.length;i++){
      var u=new URL(candidates[i].href,location.href);
      if(!/^(www\.)?instagram\.com$/.test(u.hostname)) continue;
      var id=postCode(u.pathname);if(id)return id;
    }
    return postCode(location.pathname);
  }
  // Stop IG's press handlers too, while leaving touch scrolling and click synthesis intact.
  ['pointerdown','pointerup','mousedown','mouseup','touchstart','touchend'].forEach(function(name){
    window.addEventListener(name,function(e){if(code(e.target)){e.stopImmediatePropagation();}}, {capture:true,passive:true});
  });
  var last='',at=0;
  window.addEventListener('click',function(e){
    var id=code(e.target); if(!id) return;
    e.preventDefault(); e.stopImmediatePropagation();
    if(id===last&&Date.now()-at<900) return;
    last=id;at=Date.now();window[bridge].postMessage(id);
  },true);
})();
