# Move Swarnalok work to your Windows PC and publish the Soft Premium theme

## Context
The cloud session can't reach your Chrome, and swarnalok.com rejects the login sent from the cloud (Hostinger strips the login header). Running Claude on your own Windows PC fixes both:
- it can use your WordPress MCP plugin directly;
- if needed, it can see your browser.

Everything built so far is on GitHub branch `claude/swarnalok-gold-website-i8zobp` (repo `HeisenbergSamir/campking`), so nothing is lost.

Your choices:
- **Computer:** Windows
- **Tool:** Claude desktop app (Code tab)
- **Theme to go live first:** Soft Premium (`swarnalok/v6/soft.html`)
- **Publish method:** the "Enable Abilities for MCP" plugin already on your site

## Part A: one-time setup on your PC (you, about 15 minutes)
1. **Install** these on your PC:
   - Git for Windows (git-scm.com)
   - Node.js LTS (nodejs.org), which the MCP bridge needs
   - the Claude desktop app (claude.ai/download), signed in with this account
2. **Get the project.** In PowerShell:
   ```
   git clone -b claude/swarnalok-gold-website-i8zobp https://github.com/HeisenbergSamir/campking.git C:\Swarnalok\campking
   ```
3. **Hostinger fix.** In hPanel → File Manager → `public_html/.htaccess`, add at the very top:
   ```
   RewriteEngine On
   RewriteRule .* - [E=HTTP_AUTHORIZATION:%{HTTP:Authorization}]
   ```
   This lets WordPress see the login header. It is a standard fix on Hostinger/LiteSpeed.
4. **Connect the site's MCP.** Use either option.
   - **Easiest:** the claude.ai custom connector.
     1. Go to claude.ai → Settings → Connectors → Add custom connector.
     2. Paste `https://swarnalok.com/wp-json/mcp/mcp-oauth-server`.
     3. Log in to WordPress and approve.
     4. The desktop app then has it.
   - **Or:** an Application Password with the plugin's "Claude Desktop" config snippet.
     1. In wp-admin → Settings → Enable Abilities for MCP → Connection, do Step 1 (create the password) and Step 2 (Generate Credentials).
     2. Copy the "Claude Desktop" JSON into Claude desktop → Settings → Developer → Edit Config.
     3. Restart the app.
     4. Keep this file private and never commit it.
5. **Open the project.** In the Claude desktop app → Code tab, open the folder `C:\Swarnalok\campking`, start a session and paste the handoff prompt below.

## Part B: what Claude does in the local session
1. **Check the connection.**
   - List the MCP abilities and confirm it can create and update pages, upload media and change settings.
   - Note whether it can write Custom CSS & JS entries.
2. **Turn Soft Premium into Flatsome code.**
   - **Generator:** create `swarnalok/wordpress/build_soft.py`. It reuses `minify()` and `flatsome()` from `swarnalok/wordpress/build.py`, plus the Soft theme sections, CSS and JS from `swarnalok/v6/build.py`. That file needs a small refactor so it exposes each section's HTML and the theme CSS.
   - **Output:**
     - `flatsome-soft.txt`: `[section]` → `[row width="custom" custom_width="1600px"]` → `[col]` → `[ux_html]` for each section.
     - `soft-site.css`: the theme CSS plus the dock/back-to-top styles.
     - `soft-site.js`: grain, bold band, model reveal and steps logic, guarded so it only runs when those elements exist. The engine `swarnalok/wordpress/swarnalok.js` stays the shared live-price, WhatsApp and calculator code.
   - **3D ring:** load three.js from jsdelivr's `+esm` URLs, for example `three@0.160.0/+esm` and `…/examples/jsm/environments/RoomEnvironment.js/+esm`. That way no import map is needed in WordPress. The scroll-turning frames stay as the fallback.
   - **Page template:** use Flatsome "Page – No Header / No Footer", so the page keeps its own ticker, header and footer exactly as in the preview. Policy pages keep the normal Flatsome layout.
3. **Upload media through MCP.**
   - Upload `v5/img/m-pendant.webp`, `m-black.webp`, `r-ring.webp` and `spin/f00–f47.webp`.
   - Rewrite the image paths in the generated code to the uploaded addresses. The prefix check already exists in `swarnalok/wordpress/install.py`; reuse it.
4. **Add the sitewide CSS and JS.**
   - First choice: two Custom CSS & JS entries (CSS in Header, JS in Footer), if MCP can write them.
   - Otherwise: a Custom HTML widget in the footer, as `install.py` does.
   - Last resort: Claude drives your Chrome on `wp-admin/edit.php?post_type=custom-css-js` to paste them.
5. **Create the 5 policy pages** from `swarnalok/wordpress/policies/*.html`, with the slugs already linked in the footer.
6. **Go live.**
   - Create the new homepage and set it as the static front page.
   - Move the old homepage to **Draft**. Nothing is deleted.
   - Purge the LiteSpeed cache.
7. **Commit** the generator and outputs to the branch and push.

`swarnalok/wordpress/install.py` is the fallback publisher if MCP can't do something. It now needs the Application Password in environment variables `WP_USER` and `WP_APP_PASSWORD`, sent with `curl -u`.

## Handoff prompt to paste in the local session
> Continue the Swarnalok website from branch `claude/swarnalok-gold-website-i8zobp`. Publish the Soft Premium theme (`swarnalok/v6/soft.html`) to swarnalok.com as the new homepage using the WordPress MCP connector, following `swarnalok/LOCAL-SETUP.md` Part B. Use Flatsome section/row/col/ux_html elements, put sitewide CSS/JS in Custom CSS & JS, create the 5 policy pages, set the front page, move the old homepage to draft (don't delete), then check the live site on phone and desktop widths.

## Verification
On the live site https://swarnalok.com, at phone (390px) and desktop (1440px) widths:
- **Layout:** no sideways scroll and no console errors.
- **Live price:** the top ticker and the price tiles show the live price and count up.
- **Formula:** international rate + customs 6% − ₹5,000 = the 24K price, and the numbers add up.
- **3D ring:** turns and drifts on scroll; the frames fallback works if WebGL is off.
- **Tapping things:**
  - each area chip opens the WhatsApp form with that area selected;
  - Send opens WhatsApp with the pre-filled message to +91 98153 03553;
  - price tiles select the purity in the calculator;
  - the Call, Email and Maps links open correctly.
- **Policy pages:** all 5 load.
- **Footer:** shows "A unit of Jumpking International LLP".
- **Old homepage:** listed under Pages → Drafts.

## Optional cleanup
In the cloud environment "swarnalok" you can delete the saved swarnalok.com network secret, since it's no longer needed.
