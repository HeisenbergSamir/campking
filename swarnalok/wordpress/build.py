"""Build the Swarnalok homepage for Flatsome.

Outputs:
  flatsome-homepage.txt  paste into the page's Text tab (or UX Builder > Edit as code)
  preview.html           local preview with a mock Flatsome grid, header and footer
Run: python3 build.py
"""
import re, pathlib

HERE = pathlib.Path(__file__).parent
IMG_SITE = "/wp-content/uploads/2026/11/"      # where the two images land after upload in Nov 2026
IMG_PREVIEW = "upload/"

PHONE, PHONE_LABEL = "+919815303553", "+91 98153 03553"
EMAIL = "hello@jumpkingindia.com"

# ---- icons ----
I = {
 "pin": '<svg viewBox="0 0 24 24"><path d="M12 2a7 7 0 00-7 7c0 5.2 7 13 7 13s7-7.8 7-13a7 7 0 00-7-7zm0 9.5A2.5 2.5 0 1112 6.5a2.5 2.5 0 010 5z"/></svg>',
 "near": '<svg viewBox="0 0 24 24"><path d="M12 21s-7-6.3-7-11.5A7 7 0 0119 9.5C19 14.7 12 21 12 21z"/><circle cx="12" cy="9.5" r="2.5"/></svg>',
 "rate": '<svg viewBox="0 0 24 24"><path d="M3 17l6-6 4 4 8-8"/><path d="M15 7h6v6"/></svg>',
 "eye": '<svg viewBox="0 0 24 24"><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></svg>',
 "clock": '<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>',
 "shield": '<svg viewBox="0 0 24 24"><path d="M12 3l8 3v6c0 5-3.4 8.4-8 9.5C7.4 20.4 4 17 4 12V6z"/><path d="M8.5 12l2.5 2.5 4.5-5"/></svg>',
 "sign": '<svg viewBox="0 0 32 32"><path d="M8 3h11l5 5v21H8z"/><path d="M19 3v5h5M12 13h8M12 17h8M12 25c2-2.500 3 1.500 5-.500s2.500 1 4 0"/></svg>',
 "clean": '<svg viewBox="0 0 32 32"><path d="M5 19h22v6a2 2 0 01-2 2H7a2 2 0 01-2-2z"/><path d="M9 19c0-4.500 3.500-5 3.500-9M17 19c0-4.500 3.500-5 3.500-9"/><circle cx="12.500" cy="6" r="1.300"/><circle cx="20.500" cy="5" r="1"/></svg>',
 "melt": '<svg viewBox="0 0 32 32"><path d="M7 15h18l-2.500 12h-13z"/><path d="M16 3c1 3.500 4.500 4.500 4.500 8a4.500 4.500 0 01-9 0c0-2 1-3 2-4 0 1.500.500 2 1 2 .500-2 .500-4 1.500-6z"/></svg>',
 "xrf": '<svg viewBox="0 0 32 32"><circle cx="16" cy="16" r="10"/><circle cx="16" cy="16" r="3.500"/><path d="M16 2v5M16 25v5M2 16h5M25 16h5"/></svg>',
 "scale": '<svg viewBox="0 0 32 32"><path d="M16 4v24M9 28h14M5 8h22"/><path d="M9 8l-4.500 9a4.500 4.500 0 009 0zM23 8l-4.500 9a4.500 4.500 0 009 0z"/></svg>',
 "bank": '<svg viewBox="0 0 32 32"><path d="M4 12L16 4l12 8z"/><path d="M7 12v12M12.500 12v12M19.500 12v12M25 12v12M4 28h24"/></svg>',
 "ring": '<svg viewBox="0 0 64 64"><ellipse class="g" cx="32" cy="38" rx="19" ry="17"/><ellipse class="g" cx="32" cy="38" rx="14" ry="12.500"/><path class="g" d="M26 20l6-10 6 10"/></svg>',
 "pan": '<svg viewBox="0 0 64 64"><rect x="6" y="14" width="52" height="36" rx="5"/><path d="M14 24h18M14 31h12M14 40h22"/><rect x="40" y="22" width="11" height="14" rx="2"/></svg>',
 "aadhaar": '<svg viewBox="0 0 64 64"><rect x="6" y="14" width="52" height="36" rx="5"/><circle cx="21" cy="29" r="5"/><path d="M13 42c2-5 14-5 16 0M36 25h14M36 32h14M36 39h9"/></svg>',
 "acct": '<svg viewBox="0 0 64 64"><path d="M8 26L32 10l24 16z"/><path d="M14 26v20M26 26v20M38 26v20M50 26v20M8 52h48"/></svg>',
 "phone": '<svg viewBox="0 0 24 24"><path d="M6.6 10.8a15.1 15.1 0 006.6 6.6l2.2-2.2a1 1 0 011-.25 11.4 11.4 0 003.6.57 1 1 0 011 1V20a1 1 0 01-1 1A17 17 0 013 4a1 1 0 011-1h3.5a1 1 0 011 1c0 1.25.2 2.45.57 3.57a1 1 0 01-.25 1z"/></svg>',
}

def img(name, alt, w, h, cls=""):
    return f'<img class="{cls}" src="{{IMG}}{name}" alt="{alt}" width="{w}" height="{h}" loading="lazy" decoding="async">'

# ---- page model: list of sections; each section = (section attrs, [rows]); row = (row attrs, [cols]); col = (span, html) ----
S = []
WHITE, WARM, DARK, TERRA = "#ffffff", "#F7F2EE", "#1E1512", "#A9512F"

# 1. HERO
S.append((f'bg_color="{WHITE}" padding="56px" padding__sm="24px" class="sw sw-hero"', [
  ('width="custom" custom_width="1600px" v_align="middle"', [
    (6, f"""<div class="sw-hero-copy">
<p class="sw-kicker">Old gold buyers · Bengaluru North</p>
<h1 class="sw-h1">We only<br>buy gold.</h1>
<p class="sw-lead">Live price. Paid in 30 minutes.</p>
<ul class="sw-places"><li><span class="sw-pin">{I["pin"]}</span>Jakkur</li><li>Yelahanka</li><li>Hebbal</li><li>Thanisandra</li></ul>
<div class="sw-actions"><a class="sw-btn" href="#sw-price">Today's price</a><a class="sw-link" href="tel:{PHONE}">Call {PHONE_LABEL}</a></div>
</div>"""),
    (6, f"""<div class="sw-visual">
<figure class="sw-photo">{img("sw-hero-model.webp","A woman wearing a gold pendant and ring",1200,1500)}</figure>
<div class="sw-ratecard"><small><span class="sw-pulse" data-sw="status"></span>Live · 24K · 10 g</small><strong data-sw="p24_10">…</strong></div>
<div class="sw-ring-wrap" data-sw-parallax="0.18"><div class="sw-ring-float sw-spin" data-frames="{{IMG}}sw-spin-"><img src="{{IMG}}sw-spin-00.webp" alt="" width="800" height="688"><canvas width="800" height="688" aria-hidden="true"></canvas></div></div>
</div>"""),
  ]),
]))

# 2. PROMISES
S.append((f'bg_color="{WARM}" padding="26px" padding__sm="22px" class="sw sw-warm sw-band"', [
  ('width="custom" custom_width="1600px"', [
    (12, f"""<ul class="sw-proof">
<li><i>{I["near"]}</i><b>10 min away</b></li>
<li><i>{I["rate"]}</i><b>Live rate</b></li>
<li><i>{I["eye"]}</i><b>X-ray tested</b></li>
<li><i>{I["clock"]}</i><b>Paid in 30 min</b></li>
<li><i>{I["shield"]}</i><b>Bank transfer only</b></li>
</ul>"""),
  ]),
]))

# 3. LIVE RATES
S.append((f'bg_color="{WHITE}" padding="120px" padding__sm="70px" class="sw"', [
  ('width="custom" custom_width="1600px"', [
    (12, """<span id="sw-price" class="sw-anchor"></span>
<div class="sw-center sw-stack"><h2 class="sw-h2 sw-rise">Today's price.</h2></div>
<div class="sw-rates">
<div class="sw-rate is-main sw-rise"><h3>24K</h3><strong data-sw="p24_10">…</strong><span>per 10 g</span></div>
<div class="sw-rate sw-rise"><h3>22K</h3><strong data-sw="p22_10">…</strong><span>per 10 g</span></div>
<div class="sw-rate sw-rise"><h3>18K</h3><strong data-sw="p18_10">…</strong><span>per 10 g</span></div>
</div>
<div class="sw-rate-meta"><span><span class="sw-pulse" data-sw="status"></span><span data-sw="updated">Fetching live price</span></span><span>International rate + customs − ₹5,000 per 10 g</span></div>"""),
  ]),
]))

# 4. MODEL
S.append((f'bg_color="{DARK}" padding="0px" padding__sm="0px" class="sw sw-dark sw-model"', [
  ('width="full-width" v_align="middle" col_style="collapse"', [
    (6, img("sw-model-main.webp","A model in black",1200,1500,"sw-model-img")),
    (6, f"""<div class="sw-model-copy">
<p class="sw-eyebrow">Worn with love</p>
<h2 class="sw-h2 sw-rise">Valued with<br>honesty.</h2>
<div class="sw-details">{img("sw-model-ear.webp","Gold hoop earring and chain",800,1000,"sw-rise")}{img("sw-model-hands.webp","Gold rings and bracelet",800,1000,"sw-rise")}</div>
</div>"""),
  ]),
]))

# 5. PURITY
S.append((f'bg_color="{TERRA}" padding="130px" padding__sm="80px" class="sw sw-terra"', [
  ('width="custom" custom_width="1600px"', [
    (12, """<div class="sw-center sw-stack">
<h2 class="sw-h2 sw-rise">We don't guess.</h2>
<p class="sw-num" data-count="91.6" data-decimals="1" data-suffix="%" aria-label="91.6 percent">91.6%</p>
<p class="sw-sub sw-rise">X-ray tested in front of you.</p>
</div>"""),
  ]),
]))

# 6. STEPS
steps = [("sign", "0:00", "Consent"), ("clean", "0:05", "Clean"), ("melt", "0:10", "Melt"),
         ("xrf", "0:18", "Test"), ("scale", "0:22", "Weigh"), ("bank", "0:28", "Paid")]
tiles = "".join(f'<li class="sw-step sw-rise{" is-last" if i == 5 else ""}"><i>{I[ic]}</i><small>{t}</small><h3>{h}</h3></li>' for i, (ic, t, h) in enumerate(steps))
S.append((f'bg_color="{WHITE}" padding="130px" padding__sm="80px" class="sw"', [
  ('width="custom" custom_width="1600px"', [
    (12, f"""<div class="sw-center sw-stack"><h2 class="sw-h2 sw-rise">Six steps.<br>30 minutes.</h2></div>
<ol class="sw-steps">{tiles}</ol>"""),
  ]),
]))

# 7. COMPARE
S.append((f'bg_color="{WARM}" padding="130px" padding__sm="80px" class="sw sw-warm"', [
  ('width="custom" custom_width="1600px"', [
    (12, """<div class="sw-center sw-stack"><h2 class="sw-h2 sw-rise">We buy.<br>We don't sell.</h2></div>
<div class="sw-vs">
<div class="sw-rise"><h3>Jewellery shop</h3><ul><li>Wants to sell you new</li><li>Often deducts wastage</li><li>Often cash or credit</li></ul></div>
<div class="is-us sw-rise"><h3>Swarnalok</h3><ul><li>Nothing to sell you</li><li>₹0 deductions</li><li>Bank transfer</li></ul></div>
</div>"""),
  ]),
]))

# 8. CALCULATOR
S.append((f'bg_color="{WHITE}" padding="130px" padding__sm="80px" class="sw"', [
  ('width="custom" custom_width="1600px"', [
    (12, """<span id="sw-value" class="sw-anchor"></span>
<div class="sw-center sw-stack"><h2 class="sw-h2 sw-rise">What is yours worth?</h2></div>
<form class="sw-calc" onsubmit="return false">
<output id="sw-estimate" for="sw-grams">…</output>
<div class="sw-seg" role="radiogroup" aria-label="Purity"><input type="radio" name="sw-k" id="sw-k24" value="24"><label for="sw-k24">24K</label><input type="radio" name="sw-k" id="sw-k22" value="22" checked><label for="sw-k22">22K</label><input type="radio" name="sw-k" id="sw-k18" value="18"><label for="sw-k18">18K</label><input type="radio" name="sw-k" id="sw-k14" value="14"><label for="sw-k14">14K</label></div>
<div class="sw-stepper"><button type="button" id="sw-minus" aria-label="Less weight">−</button><label class="sw-grams"><input id="sw-grams" type="number" inputmode="decimal" min="0" step="0.5" value="20" aria-label="Weight in grams"><span>grams</span></label><button type="button" id="sw-plus" aria-label="More weight">+</button></div>
<button class="sw-btn" type="button" id="sw-calc-wa">Get this on WhatsApp</button>
<p class="sw-fine">Estimate. Final price after the X-ray test.</p>
</form>"""),
  ]),
]))

# 9. BRING
S.append((f'bg_color="{WARM}" padding="130px" padding__sm="80px" class="sw sw-warm"', [
  ('width="custom" custom_width="1600px"', [
    (12, f"""<div class="sw-center sw-stack"><h2 class="sw-h2 sw-rise">Bring four things.</h2></div>
<ul class="sw-bring"><li class="sw-rise"><i>{I["ring"]}</i>Gold</li><li class="sw-rise"><i>{I["pan"]}</i>PAN</li><li class="sw-rise"><i>{I["aadhaar"]}</i>Aadhaar</li><li class="sw-rise"><i>{I["acct"]}</i>Bank account</li></ul>"""),
  ]),
]))

# 10. VISIT
S.append((f'bg_color="{DARK}" padding="130px" padding__sm="80px" class="sw sw-dark"', [
  ('width="custom" custom_width="1600px"', [
    (12, f"""<div class="sw-visit">
<div class="sw-copy sw-stack"><h2 class="sw-h2 sw-rise">Come by.<br>Leave paid.</h2>
<div class="sw-actions"><a class="sw-btn" href="tel:{PHONE}">{I["phone"]} Call {PHONE_LABEL}</a><a class="sw-link" href="#" data-sw-open-wa>WhatsApp</a></div></div>
<dl><div><dt>Hours</dt><dd>Mon to Sat · 12:30 to 18:00<br><span class="sw-open" data-sw-hours></span></dd></div><div><dt>Phone</dt><dd>{PHONE_LABEL}</dd></div><div><dt>Email</dt><dd>{EMAIL}</dd></div><div><dt>Office</dt><dd>Jakkur, Bengaluru 560064</dd></div></dl>
</div>"""),
  ]),
]))

# 11. LEGAL
S.append((f'bg_color="{WARM}" padding="30px" padding__sm="26px" class="sw sw-warm sw-legal"', [
  ('width="custom" custom_width="1600px"', [
    (12, """<nav aria-label="Policies"><a href="/terms-and-conditions/">Terms</a><a href="/privacy-policy/">Privacy</a><a href="/kyc-aml-policy/">KYC and AML</a><a href="/anti-fraud-policy/">Anti-fraud</a><a href="/pricing-payment-policy/">Pricing and payment</a></nav>
<p class="sw-copy-line"><span>© <span class="sw-year">2026</span> Swarnalok. A unit of Jumpking International LLP.</span><span>Live rates are estimates.</span></p>"""),
  ]),
]))


def minify(html):
    html = re.sub(r">\s+<", "><", html.strip())
    return re.sub(r"\s*\n\s*", " ", html)

def flatsome(imgbase):
    out = []
    for sattrs, rows in S:
        out.append(f"[section {sattrs}]")
        for rattrs, cols in rows:
            out.append(f"[row {rattrs}]")
            for span, html in cols:
                out.append(f'[col span="{span}" span__sm="12"]')
                out.append("[ux_html]" + minify(html.replace("{IMG}", imgbase)) + "[/ux_html]")
                out.append("[/col]")
            out.append("[/row]")
        out.append("[/section]")
    return "\n".join(out)

def preview():
    """Render the same tree as plain HTML with a minimal Flatsome-like grid."""
    parts = []
    for sattrs, rows in S:
        a = dict(re.findall(r'(\w+)="([^"]*)"', sattrs))
        pad, pad_sm = a.get("padding", "0"), a.get("padding__sm", a.get("padding", "0"))
        sid = ""
        body = []
        for rattrs, cols in rows:
            ra = dict(re.findall(r'(\w+)="([^"]*)"', rattrs))
            al = " align-middle" if ra.get("v_align") == "middle" else ""
            al += " row-full" if ra.get("width") == "full-width" else ""
            al += " row-collapse" if ra.get("col_style") == "collapse" else ""
            body.append(f'<div class="row{al}">' + "".join(
                f'<div class="col large-{span}">{html.replace("{IMG}", IMG_PREVIEW)}</div>' for span, html in cols) + "</div>")
        parts.append(f'<section class="section {a.get("class","")}"{sid} style="background:{a.get("bg_color","#fff")};--pad:{pad};--pad-sm:{pad_sm}">{"".join(body)}</section>')
    css = (HERE / "swarnalok.css").read_text()
    js = (HERE / "swarnalok.js").read_text()
    return f'''<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>Swarnalok</title><meta name="claude:viewport-fit-sides" content="cover">
<style>
/* mock of the Flatsome frame, for preview only */
body{{margin:0;background:#fff;overflow-x:hidden}}*{{box-sizing:border-box}}
.mock-head{{position:sticky;top:env(safe-area-inset-top,0px);z-index:50;background:rgba(255,255,255,.85);backdrop-filter:blur(18px);border-bottom:1px solid rgba(0,0,0,.08);font-family:"Inter",sans-serif}}
.mock-head div{{max-width:1600px;margin:auto;height:64px;display:flex;align-items:center;justify-content:space-between;padding:0 max(16px,env(safe-area-inset-left,0px))}}
.mock-head b{{font:500 .85rem "Inter Tight",sans-serif;letter-spacing:.42em;display:flex;align-items:center;gap:10px}}
.mock-head b i{{width:16px;height:16px;border-radius:50%;background:radial-gradient(circle at 34% 30%,#FFE9DC 0,#E08A62 30%,#A9512F 70%,#6E2E18 100%)}}
.mock-head nav{{display:flex;gap:28px;font-size:.82rem}}@media(max-width:760px){{.mock-head nav{{display:none}}}}
.section{{position:relative;padding-block:var(--pad)}}@media(max-width:549px){{.section{{padding-block:var(--pad-sm)}}}}
.row{{display:flex;flex-wrap:wrap;max-width:1600px;margin:0 auto;width:100%;padding:0 max(1px,env(safe-area-inset-left,0px))}}
.row.align-middle{{align-items:center}}.row-full{{max-width:none!important;padding:0!important}}.row-collapse>.col{{padding:0}}
.col{{padding:0 15px 30px;width:100%;min-width:0}}
@media(min-width:850px){{.large-5{{flex-basis:41.66%;max-width:41.66%}}.large-6{{flex-basis:50%;max-width:50%}}.large-7{{flex-basis:58.33%;max-width:58.33%}}.large-12{{flex-basis:100%;max-width:100%}}}}
@media(max-width:549px){{.col{{padding:0 max(15px,env(safe-area-inset-left,0px)) 30px}}}}
</style>
<style>{css}</style></head><body>
<header class="mock-head"><div><b><i></i>SWARNALOK</b><nav><span>Today's price</span><span>Process</span><span>Visit</span></nav></div></header>
{"".join(parts)}
<script>{js}</script>
</body></html>'''

(HERE / "flatsome-homepage.txt").write_text(flatsome(IMG_SITE) + "\n")
(HERE / "preview.html").write_text(preview())
print("built", len(S), "sections")
