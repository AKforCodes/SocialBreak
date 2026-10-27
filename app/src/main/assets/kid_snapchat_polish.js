/* Kid-only Android adaptation: iOS a8b6ff4 SnapchatExpandMessagesInjector, SnapchatReplyLayoutInjector and captured SnapchatSelectorLabels. Diagnostics removed; quote styling scoped to the composer. */
(function(config){
 if(config.hosts.indexOf(location.hostname.toLowerCase()) < 0) return;
 if(location.hostname !== 'web.snapchat.com' && !/^\/web(?:\/|$)/.test(location.pathname)) return;
 (function () {
  if (window.__NAMESPACE__202) return;
  window.__NAMESPACE__202 = true;
  try {
    var st = document.createElement('style');
    st.id = '__NAMESPACE__203';
    st.textContent = `
      /* [SNAP-I18N-WIRE-2026-08-20] every "Send a chat" placeholder selector below is
         emitted by SnapchatSelectorLabels — en output byte-identical, a captured locale
         swaps in a :is(english, localized) twin. */
      /* The composer's own container must never exceed the viewport. */
      div:has(> :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])),
      div:has(> :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) {
        max-width: 100% !important;
        box-sizing: border-box !important;
      }

      /* THE FIX. Flex items default to min-width:auto and refuse to shrink below their
         content width - which is what pushes the quoted card off screen. */
      div:has(> :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) > *,
      div:has(> :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) > * > * {
        min-width: 0 !important;
      }

      /* Let a long quoted message wrap instead of demanding one enormous line. */
      div:has(> :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) span,
      div:has(> :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) p {
        overflow-wrap: anywhere !important;
        word-break: break-word !important;
      }

      /* [SNAP-REPLY-LAYOUT-2026-08-09 #2] The composer block was also sitting INSET — it
         began around x=150 with the camera button stranded outside it, instead of spanning
         the pane the way it does with no reply active. Wrapping fixed the overflow; this
         puts the block back to full width so the reply preview reads like the real app's
         rather than a floating card. */
      div:has(> :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])),
      div:has(> div > :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])),
      div:has(> div > div > :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) {
        width: 100% !important;
        margin-left: 0 !important;
        margin-right: 0 !important;
        flex: 1 1 auto !important;
      }

      /* [SNAP-REPLY-QUOTE-LI-2026-08-10] THE quoted-message overflow, measured rather than
         guessed (see __slSnapReplyDump).

         Snapchat renders the quoted message as an LI inside a UL, and that LI carries a
         hard-coded min-width:300px / width:300px. In the composer the UL is only ~219px
         wide, so the LI juts ~81px past it - visible as the quoted card running out of the
         rounded box, and as a dark tail on a SAVED message because the saved highlight is
         the LI's own background painting across its full 300px. A quoted snap card
         ("Opened") overflows for the same reason.

         min-width is what does it: it beats max-width in CSS, so every "max-width:100%"
         attempt was ignored, and the element is an LI, which is why rules scoped to DIVs
         changed nothing. Anchored on the direct-child chain so it can only ever match the
         composer's own subtree - a descendant :has() here matches every ancestor including
         the conversation wrapper, which reformats the whole thread. */
      div:has(> :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) li,
      div:has(> div > :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) li,
      div:has(> div > div > :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) li,
      div:has(> :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) li header,
      div:has(> div > :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) li header,
      div:has(> div > div > :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) li header {
        min-width: 0 !important;
        width: auto !important;
        max-width: 100% !important;
      }

      /* The 300px is repeated on nested nodes inside the quote, so freeing the LI alone
         just moves the overflow one level down (measured: the LI came into line and a
         HEADER inside it took over at the same 300px / 81px). Everything INSIDE the quoted
         LI may shrink - deliberately scoped to the LI's subtree, not the whole composer:
         the composer's own author/timestamp row lives outside it, and relaxing that one is
         what previously collapsed "ME 12:44 PM" into "ME12:44 PM". The width property is left alone
         here so avatars and thumbnails keep their intrinsic sizing. */
      div:has(> :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) li *,
      div:has(> div > :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) li *,
      div:has(> div > div > :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) li * {
        min-width: 0 !important;
        max-width: 100% !important;
      }

      /* Belt and braces: the quote is a preview, not the whole message. Two lines, then
         ellipsis - matching how the real app previews what you're replying to. */
      div:has(> :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])) [class] > span:not(:has(*)) {
        display: -webkit-box !important;
        -webkit-line-clamp: 2 !important;
        -webkit-box-orient: vertical !important;
        overflow: hidden !important;
      }
    `;
    (document.head || document.documentElement).appendChild(st);
  } catch (e) {}

  // [SNAP-REPLY-DUMP-2026-08-10] Diagnostic: what is actually forcing the reply preview wide.
  // A quoted SAVED message (and a quoted snap card) overflows the composer, and blind CSS
  // could not fix it — three shotgun selectors each traded the overflow for a worse
  // regression. This reports the measurements instead of guessing:
  //   chain — the composer input's ancestors with their boxes, so the level where the width
  //           first exceeds the viewport is visible directly.
  //   over  — every element inside the composer whose right edge passes the viewport, with
  //           the properties that decide a flex item's width.
  // Verbose-only and fired once per reply (see SnapchatSwipeReplyInjector), never on a timer.
  // NOTE: no backticks anywhere below - this whole script is a JS template literal.

})();
(function () {
  if (window.__NAMESPACE__201) return;
  window.__NAMESPACE__201 = true;

  function convOpen() { return !!document.querySelector(':is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i])'); } // [SNAP-I18N-WIRE-2026-08-20] composer anchor from the store (en byte-identical)
  function dbg() {}

  function unclamp(el) {
    el.style.setProperty('-webkit-line-clamp', 'none', 'important');
    el.style.setProperty('overflow', 'visible', 'important');
    el.style.setProperty('max-height', 'none', 'important');
    el.setAttribute('data-__NAMESPACE__204', '1');
  }

  // One sweep: reveal every truncated message currently on screen. Detection alone (a computed
  // line-clamp that is actually overflowing, on substantial text, while a conversation is open)
  // is precise — the DOM probe confirmed it selects exactly the message bubbles and nothing
  // else — so there is no row-membership gate to get in the way.
  // ⭐ [SNAP-EXPAND-KEEPSCROLL-2026-09-12] THE "chat opens at the top instead of the bottom" bug.
  //
  // Reported by Malek's tester: "sometimes when he goes into a chat it jumps to the top, not the
  // bottom." Intermittent, which is the tell — it only happens in chats that HAVE long clamped
  // messages, because this injector is what causes it.
  //
  // Snapchat opens a conversation scrolled to the newest message. Our sweep then un-clamps every
  // truncated message, and un-clamping makes each one TALLER. All that new height is ABOVE the
  // viewport, but `scrollTop` does not move — so the same scrollTop now points much further back
  // in the conversation. Expand enough long messages and you are looking at the top of the chat.
  // Nothing "jumped"; the content grew underneath a stationary scroll position.
  //
  // FIX: measure the scroller once before expanding, then put the view back.
  //   • Were we at the bottom (within 80px, i.e. Snapchat's own open-at-newest position)?
  //     → pin to the new bottom. This is the reported case.
  //   • Scrolled up reading history? → add exactly the height we just created, so the message
  //     under your eye stays under your eye.
  // One scrollTop write per sweep, and only when something actually expanded. Every element is
  // marked `data-__NAMESPACE__204` and skipped forever after, so this cannot become a loop or fight
  // React — the concern that has bitten every other timer we run on this page.
  function scrollerFor(el) {
    var p = el && el.parentElement;
    for (var h = 0; p && h < 12; h++) {
      if (p === document.body || p === document.documentElement) break;
      try {
        var cs = getComputedStyle(p);
        if (/(auto|scroll)/.test(cs.overflowY) && p.scrollHeight > p.clientHeight + 4) return p;
      } catch (e) {}
      p = p.parentElement;
    }
    return null;
  }

  function sweep() {
    if (!convOpen()) return;
    var nodes = document.querySelectorAll('span,div,p');
    // Collect first, expand second — the measurement has to happen before ANY height changes.
    var todo = [];
    for (var i = 0; i < nodes.length; i++) {
      var el = nodes[i];
      if (el.getAttribute('data-__NAMESPACE__204')) continue;
if (el.closest('nav,aside,[role="navigation"],.sl-snap-railcol,textarea,[contenteditable="true"],div:has(> :is([placeholder*="Send a chat" i],[placeholder*="Sende eine Nachricht" i],[placeholder*="Envoyer un Chat" i],[placeholder*="Enviar un chat" i],[placeholder*="Invia una chat" i],[placeholder*="Enviar um chat" i],[placeholder*="チャットを送信" i],[placeholder*="أرسل دردشة" i]))')) continue;
      var t = (el.textContent || '').trim();
      if (t.length < 40) continue;
      var cs = getComputedStyle(el);
      var lc = cs.webkitLineClamp || cs.getPropertyValue('-webkit-line-clamp');
      if (!lc || lc === 'none') continue;
      if (el.scrollHeight <= el.clientHeight + 2) continue;   // not actually truncated
      todo.push(el);
    }
    if (!todo.length) return;

    // Preserve each scroller independently. An anchor prevents growth BELOW the reader
    // from shifting their position, unlike adding every newly created pixel to scrollTop.
    var positions = new Map();
    todo.forEach(function(el) {
      var sc = scrollerFor(el);
      if (!sc || positions.has(sc)) return;
      var bounds = sc.getBoundingClientRect(), anchor = null;
      var leaves = sc.querySelectorAll('span,p,div');
      for (var i = 0; i < leaves.length; i++) {
        var item = leaves[i];
        if (item.childElementCount || !item.textContent.trim()) continue;
        var box = item.getBoundingClientRect();
        if (box.bottom > bounds.top && box.top < bounds.bottom) { anchor = item; break; }
      }
      positions.set(sc, {top:sc.scrollTop,height:sc.scrollHeight,
        bottom:sc.scrollHeight-sc.scrollTop-sc.clientHeight <= 80,
        anchor:anchor,anchorTop:anchor ? anchor.getBoundingClientRect().top : 0});
    });
    todo.forEach(unclamp);
    positions.forEach(function(before, sc) {
      var grew = sc.scrollHeight-before.height;
      if (grew <= 0) return;
      sc.scrollTop = before.bottom ? sc.scrollHeight : before.top +
        (before.anchor && before.anchor.isConnected ? before.anchor.getBoundingClientRect().top-before.anchorTop : grew);
    });
    void 0;
  }

  // Poll on an interval. The script injects at document-start — long before a conversation is
  // opened and, at that instant, before document.body even exists — so a synchronous observer
  // setup or a couple of early setTimeouts would miss the chat entirely (they did: the sweeps
  // fired on the chat LIST, found nothing, and never ran again). A steady poll always catches
  // the conversation whenever it is opened; each element is un-clamped once (marked), so the
  // sweep is idempotent and cheap. Once body exists, also attach an observer for instant
  // response to newly-arriving messages, on top of the poll.
  setInterval(function () { try { sweep(); } catch (e) {} }, 1000);
  var scheduled = false, attached = false;
  function schedule() {
    if (scheduled) return; scheduled = true;
    requestAnimationFrame(function () { scheduled = false; try { sweep(); } catch (e) {} });
  }
  var attachTimer = setInterval(function () {
    if (attached || !document.body) return;
    attached = true; clearInterval(attachTimer);
    try { new MutationObserver(schedule).observe(document.body, { subtree: true, childList: true }); } catch (e) {}
  }, 500);
})();
})(/*CONFIG*/null);
