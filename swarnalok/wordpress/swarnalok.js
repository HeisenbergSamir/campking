/*!
 * Swarnalok site script (sitewide). Paste into Custom CSS & JS as a JavaScript
 * file: Linking type "Internal", Where on page "Footer", Where in site "In Frontend".
 * Contents: live gold buying price, value calculator, sticky Call / WhatsApp button.
 */
(function () {
  'use strict';
  if (window.SW && window.SW.loaded) return; // already running on this page

  // ---------- settings ----------
  var CFG = {
    phone: '+919815303553',
    phoneLabel: '+91 98153 03553',
    whatsapp: '919815303553',
    email: 'hello@jumpkingindia.com',
    hours: { days: [1, 2, 3, 4, 5, 6], open: '12:30', close: '18:00', label: 'Mon to Sat, 12:30 to 18:00' },
    // Price = international spot price, plus Indian customs duty, less a fixed margin per 10 g.
    customsDuty: 0.06,          // 5% basic customs duty + 1% AIDC. Change here if the duty changes.
    marginPer10g: 5000,         // ₹ deducted from the 10 g price
    refreshMs: 60000,           // fetch a fresh price every minute
    purity: { '24': 1, '22': 0.9167, '18': 0.75, '14': 0.585 },
    areas: ['Jakkur', 'Yelahanka', 'Hebbal', 'Thanisandra', 'Sahakar Nagar', 'Kogilu', 'Amruthahalli',
            'Kodigehalli', 'Hegde Nagar', 'Nagavara', 'Vidyaranyapura', 'Judicial Layout', 'Bagalur Road',
            'Hennur', 'RT Nagar', 'Other area in Bengaluru']
  };
  var OZ = 31.1034768; // grams per troy ounce

  var SW = window.SW = window.SW || {};
  SW.loaded = true;
  SW.config = CFG;

  var inr = function (n) { return '₹' + Math.round(n).toLocaleString('en-IN'); };
  var $ = function (s, r) { return (r || document).querySelector(s); };
  var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };

  // ---------- live price ----------
  function getJSON(url) {
    return fetch(url, { cache: 'no-store' }).then(function (r) {
      if (!r.ok) throw new Error(url + ' ' + r.status);
      return r.json();
    });
  }

  // Spot price of 1 gram of 24K gold in INR, with no duty, GST, shipping or insurance.
  function fetchSpotPerGram() {
    return getJSON('https://api.gold-api.com/price/XAU/INR').then(function (d) {
      if (!(d && d.price > 0)) throw new Error('bad price');
      return { perGram: d.price / OZ, at: d.updatedAt ? new Date(d.updatedAt) : new Date() };
    }).catch(function () {
      // backup: USD price × USD→INR rate
      return Promise.all([getJSON('https://api.gold-api.com/price/XAU'), getJSON('https://open.er-api.com/v6/latest/USD')])
        .then(function (r) {
          var usd = r[0] && r[0].price, fx = r[1] && r[1].rates && r[1].rates.INR;
          if (!(usd > 0 && fx > 0)) throw new Error('bad backup price');
          return { perGram: usd * fx / OZ, at: new Date() };
        });
    });
  }

  function buildRates(spot) {
    var per10g24 = spot.perGram * 10 * (1 + CFG.customsDuty) - CFG.marginPer10g;
    var out = { at: spot.at, spotPerGram: spot.perGram };
    Object.keys(CFG.purity).forEach(function (k) {
      out['p' + k + '_10'] = per10g24 * CFG.purity[k];
      out['p' + k + '_1'] = per10g24 * CFG.purity[k] / 10;
    });
    return out;
  }

  function paint() {
    var r = SW.rates;
    $$('[data-sw]').forEach(function (el) {
      var key = el.getAttribute('data-sw');
      if (key === 'updated') {
        el.textContent = r ? 'Updated ' + r.at.toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }) : 'Fetching live price';
        return;
      }
      if (key === 'status') { el.classList.toggle('is-live', !!r && !SW.rateError); return; }
      if (r && r[key] != null) el.textContent = inr(r[key]);
      else if (!r) el.textContent = SW.rateError ? 'Call us' : '…';
    });
    calc();
  }

  function refresh() {
    return fetchSpotPerGram().then(function (spot) {
      SW.rates = buildRates(spot); SW.rateError = false;
    }).catch(function () {
      SW.rateError = true; // keep the last good price on screen, never a fixed old number
    }).then(function () {
      paint();
      document.dispatchEvent(new CustomEvent('sw:rates', { detail: SW.rates }));
    });
  }

  var timer;
  function schedule() { clearInterval(timer); timer = setInterval(function () { if (!document.hidden) refresh(); }, CFG.refreshMs); }
  document.addEventListener('visibilitychange', function () { if (!document.hidden) refresh(); });

  // ---------- value calculator ----------
  function calc() {
    var box = $('.sw-calc'); if (!box) return;
    var k = ($('input[name="sw-k"]:checked', box) || {}).value || '22';
    var g = Math.max(0, parseFloat(($('#sw-grams', box) || {}).value) || 0);
    var out = $('#sw-estimate', box);
    if (!SW.rates) { out.textContent = SW.rateError ? 'Call for price' : '…'; return; }
    out.textContent = inr(g * SW.rates['p' + k + '_1']);
  }
  function initCalc() {
    var box = $('.sw-calc'); if (!box) return;
    var g = $('#sw-grams', box);
    box.addEventListener('input', calc);
    box.addEventListener('submit', function (e) { e.preventDefault(); });
    $('#sw-minus', box).addEventListener('click', function () { g.value = Math.max(0, (parseFloat(g.value) || 0) - 1); calc(); });
    $('#sw-plus', box).addEventListener('click', function () { g.value = (parseFloat(g.value) || 0) + 1; calc(); });
    var wa = $('#sw-calc-wa', box);
    if (wa) wa.addEventListener('click', function () {
      var k = ($('input[name="sw-k"]:checked', box) || {}).value || '22';
      SW.openWhatsApp({ grams: g.value, karat: k });
    });
  }

  // ---------- opening hours (India time) ----------
  function openState() {
    var now = new Date(Date.now() + (330 + new Date().getTimezoneOffset()) * 60000); // IST
    var mins = now.getHours() * 60 + now.getMinutes();
    var o = CFG.hours.open.split(':'), c = CFG.hours.close.split(':');
    var om = +o[0] * 60 + +o[1], cm = +c[0] * 60 + +c[1];
    var isDay = CFG.hours.days.indexOf(now.getDay()) > -1;
    if (isDay && mins >= om && mins < cm) return { open: true, text: 'Open now · till 18:00' };
    if (isDay && mins < om) return { open: false, text: 'Opens today at 12:30' };
    return { open: false, text: 'Opens ' + (now.getDay() === 6 ? 'Monday' : 'tomorrow') + ' at 12:30' };
  }
  function paintHours() {
    var s = openState();
    $$('[data-sw-hours]').forEach(function (el) { el.textContent = s.text; el.classList.toggle('is-open', s.open); });
  }

  // ---------- sticky Call / WhatsApp button ----------
  var ICON_PHONE = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6.6 10.8a15.1 15.1 0 006.6 6.6l2.2-2.2a1 1 0 011-.25 11.4 11.4 0 003.6.57 1 1 0 011 1V20a1 1 0 01-1 1A17 17 0 013 4a1 1 0 011-1h3.5a1 1 0 011 1c0 1.25.2 2.45.57 3.57a1 1 0 01-.25 1z"/></svg>';
  var ICON_WA = '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2a10 10 0 00-8.6 15.1L2 22l5-1.3A10 10 0 1012 2zm0 18.2a8.2 8.2 0 01-4.2-1.15l-.3-.18-3 .78.8-2.9-.2-.3A8.2 8.2 0 1112 20.2zm4.5-6.1c-.25-.13-1.47-.73-1.7-.81-.23-.08-.4-.12-.56.13-.17.25-.65.81-.8.98-.14.17-.3.19-.54.06a6.7 6.7 0 01-3.35-2.93c-.25-.44.25-.4.72-1.34.08-.17.04-.31-.02-.44-.06-.12-.56-1.35-.77-1.85-.2-.48-.41-.42-.56-.42h-.48a.92.92 0 00-.67.31 2.8 2.8 0 00-.87 2.08 4.9 4.9 0 001.02 2.6 11.2 11.2 0 004.3 3.8c1.6.69 2.23.75 3.03.63.49-.07 1.47-.6 1.68-1.18.2-.58.2-1.08.15-1.18-.06-.1-.23-.17-.48-.29z"/></svg>';
  var BUBBLES = ['Sell old gold today?', 'Live price on call', '10 min from you', 'Paid in 30 minutes'];

  function buildDock() {
    if ($('.sw-dock')) return;
    var areas = CFG.areas.map(function (a) { return '<option>' + a + '</option>'; }).join('');
    var dock = document.createElement('div');
    dock.className = 'sw-dock';
    dock.innerHTML =
      '<div class="sw-dock__panel" id="sw-dock-panel" role="dialog" aria-label="Contact Swarnalok" hidden>' +
        '<div class="sw-dock__menu">' +
          '<p class="sw-dock__title">Talk to a buyer</p>' +
          '<p class="sw-dock__hours" data-sw-hours></p>' +
          '<a class="sw-dock__opt" href="tel:' + CFG.phone + '"><span class="sw-dock__ic is-call">' + ICON_PHONE + '</span><span><b>Call now</b><small>' + CFG.phoneLabel + '</small></span></a>' +
          '<button class="sw-dock__opt" type="button" data-sw-wa-step><span class="sw-dock__ic is-wa">' + ICON_WA + '</span><span><b>WhatsApp</b><small>Get a price in minutes</small></span></button>' +
        '</div>' +
        '<form class="sw-dock__form" hidden novalidate>' +
          '<button class="sw-dock__back" type="button" aria-label="Back">‹ Back</button>' +
          '<p class="sw-dock__title">Your gold</p>' +
          '<label class="sw-dock__f"><span>How many grams?</span><input id="sw-wa-grams" type="number" inputmode="decimal" min="0.5" step="0.5" placeholder="e.g. 20" required></label>' +
          '<label class="sw-dock__f"><span>Coming from (Bengaluru)</span><select id="sw-wa-area" required><option value="">Choose your area</option>' + areas + '</select></label>' +
          '<input id="sw-wa-karat" type="hidden" value="">' +
          '<p class="sw-dock__err" role="alert" hidden></p>' +
          '<button class="sw-dock__send" type="submit">' + ICON_WA + ' Send on WhatsApp</button>' +
        '</form>' +
      '</div>' +
      '<button class="sw-dock__btn" type="button" aria-expanded="false" aria-controls="sw-dock-panel" aria-label="Call or WhatsApp Swarnalok">' + ICON_PHONE + '</button>' +
      '<span class="sw-dock__bubble" aria-hidden="true"><span>' + BUBBLES[0] + '</span></span>';
    document.body.appendChild(dock);

    var btn = $('.sw-dock__btn', dock), panel = $('.sw-dock__panel', dock), menu = $('.sw-dock__menu', dock), form = $('.sw-dock__form', dock);
    var bubble = $('.sw-dock__bubble span', dock), err = $('.sw-dock__err', dock);
    var hoverable = window.matchMedia('(hover: hover) and (pointer: fine)').matches, closeT, openedAt = 0;

    function open(toForm) {
      if (panel.hidden) openedAt = Date.now();
      panel.hidden = false; dock.classList.add('is-open'); btn.setAttribute('aria-expanded', 'true');
      menu.hidden = !!toForm; form.hidden = !toForm; err.hidden = true; paintHours();
      if (toForm) setTimeout(function () { $('#sw-wa-grams').focus(); }, 50);
    }
    function close() { panel.hidden = true; dock.classList.remove('is-open'); btn.setAttribute('aria-expanded', 'false'); menu.hidden = false; form.hidden = true; }
    SW.openWhatsApp = function (pre) {
      pre = pre || {};
      if (pre.grams) $('#sw-wa-grams').value = pre.grams;
      $('#sw-wa-karat').value = pre.karat || '';
      open(true);
    };

    // a click right after hover-open keeps the menu open instead of toggling it shut
    btn.addEventListener('click', function () { var isOpen = dock.classList.contains('is-open') && !menu.hidden; if (isOpen && Date.now() - openedAt > 600) close(); else open(false); });
    if (hoverable) {
      dock.addEventListener('mouseenter', function () { clearTimeout(closeT); if (!dock.classList.contains('is-open')) open(false); });
      dock.addEventListener('mouseleave', function () { if (form.hidden) closeT = setTimeout(close, 350); });
    }
    $('[data-sw-wa-step]', dock).addEventListener('click', function () { open(true); });
    $('.sw-dock__back', dock).addEventListener('click', function () { open(false); });
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape') close(); });
    document.addEventListener('click', function (e) { if (!dock.contains(e.target) && !e.target.closest('[data-sw-open-wa]')) close(); });
    $$('[data-sw-open-wa]').forEach(function (b) { b.addEventListener('click', function (e) { e.preventDefault(); SW.openWhatsApp(); }); });

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      var g = parseFloat($('#sw-wa-grams').value), area = $('#sw-wa-area').value, k = $('#sw-wa-karat').value;
      if (!(g > 0)) { err.textContent = 'Please enter the weight in grams.'; err.hidden = false; return; }
      if (!area) { err.textContent = 'Please choose your area in Bengaluru.'; err.hidden = false; return; }
      var lines = ['Hello Swarnalok, I would like to sell my old gold.', 'Weight: about ' + g + ' g' + (k ? ' (' + k + 'K)' : ''), 'Coming from: ' + area + ', Bengaluru'];
      if (SW.rates && k) lines.push('Estimate on your website: ' + inr(g * SW.rates['p' + k + '_1']));
      lines.push('Please share today\'s price and a visit time.');
      window.open('https://wa.me/' + CFG.whatsapp + '?text=' + encodeURIComponent(lines.join('\n')), '_blank', 'noopener');
      close();
    });

    // rotating bubble text
    var bi = 0;
    if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      setInterval(function () {
        if (dock.classList.contains('is-open')) return;
        bubble.classList.add('is-out');
        setTimeout(function () { bi = (bi + 1) % BUBBLES.length; bubble.textContent = BUBBLES[bi]; bubble.classList.remove('is-out'); }, 300);
      }, 3600);
    }
  }

  // ---------- small extras ----------
  function initRail() {
    $$('.sw-year').forEach(function (e) { e.textContent = new Date().getFullYear(); });
  }

  function init() {
    buildDock(); initCalc(); initRail(); paintHours(); paint();
    refresh().then(schedule);
    setInterval(paintHours, 60000);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})();
