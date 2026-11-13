# Swarnalok homepage for WordPress (Flatsome)

Install kit for swarnalok.com, built for Flatsome UX Builder and the "Simple Custom CSS and JS" plugin.

| File | Where it goes |
|---|---|
| `swarnalok.css` | Custom CSS & JS → Add CSS. Linking: Internal · Where on page: Header · Where in site: In Frontend |
| `swarnalok.js` | Custom CSS & JS → Add JS. Linking: Internal · Where on page: Footer · Where in site: In Frontend |
| `upload/sw-*.webp` (ring + 4 model photos) | Media → Add New. If uploaded in a month other than Nov 2026, update the `/wp-content/uploads/2026/11/` paths in `flatsome-homepage.txt` |
| `flatsome-homepage.txt` | Pages → Home → Text tab (or UX Builder → Edit as code). Page template: "Page - Full Width" |
| `policies/*.html` | One page each, with these slugs: `terms-and-conditions`, `privacy-policy`, `kyc-aml-policy`, `anti-fraud-policy`, `pricing-payment-policy` |
| `preview.html` | Local preview only |

Footer copyright (Flatsome → Theme Options → Footer): `© 2026 Swarnalok. A unit of Jumpking International LLP.`

## The homepage structure

Every block is a Flatsome `[section]` → `[row width="custom" custom_width="1600px"]` → `[col span=".." span__sm="12"]` → `[ux_html]`, so it stays editable in UX Builder. HTML inside each element is kept on one line so WordPress doesn't insert `<p>`/`<br>` tags.

## Live price

`swarnalok.js` fetches the gold price every minute (paused when the tab is hidden):

- Primary: `https://api.gold-api.com/price/XAU/INR` (INR per troy ounce, live FX)
- Backup: `https://api.gold-api.com/price/XAU` × `https://open.er-api.com/v6/latest/USD`

Formula: `24K per 10 g = spot per gram × 10 × (1 + customsDuty) − ₹5,000`. Other purities are proportional (22K = 0.9167). `customsDuty` is `0.06` (5% basic customs duty + 1% AIDC); change it at the top of `swarnalok.js` if the duty changes.

goldprice.org's own feed refuses requests from other websites, so it can't be used directly. These APIs give the same international spot price.

Any element with `data-sw="p24_10"`, `p22_10`, `p18_10`, `p24_1`, `p22_1`, `p18_1`, `updated` or `status` updates automatically, on any page.

## Sticky Call / WhatsApp button

Created by `swarnalok.js` on every page, bottom left. Hover (desktop) or tap (phone) shows Call and WhatsApp. WhatsApp asks for grams and the Bengaluru area, then opens a pre-filled chat with +91 98153 03553. Opening hours (Mon to Sat, 12:30 to 18:00, India time) show as "Open now" or "Opens at 12:30".

## Rebuild

Edit the sections in `build.py`, then run `python3 build.py` to regenerate `flatsome-homepage.txt` and `preview.html`.

## Before going live

Have a lawyer review the five policy pages. They are written for an Indian old-gold buyer (PMLA 2002, DPDP Act 2023, Bharatiya Nyaya Sanhita 2023, income-tax cash limits), but they are not legal advice. Name a Grievance Officer in the Terms and Privacy pages.
