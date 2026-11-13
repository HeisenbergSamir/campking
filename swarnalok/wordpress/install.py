"""Install the Swarnalok homepage, policies and sitewide assets on swarnalok.com via the REST API.

Run from this folder:  python3 install.py inspect   (read-only report)
                       python3 install.py install   (make the changes)
Auth is an Application Password that the session's network proxy adds to requests;
this script never handles the password itself. Nothing is deleted: the old front
page is set to draft so it can be restored from Pages > Drafts.
"""
import json, subprocess, sys, pathlib, re

SITE = "https://swarnalok.com"
API = SITE + "/wp-json/wp/v2"
HERE = pathlib.Path(__file__).parent
POLICIES = [
    ("terms-and-conditions", "Terms and Conditions"),
    ("privacy-policy", "Privacy Policy"),
    ("kyc-aml-policy", "KYC and Anti-Money Laundering Policy"),
    ("anti-fraud-policy", "Stolen Property and Anti-Fraud Policy"),
    ("pricing-payment-policy", "Pricing and Payment Policy"),
]
HOME_TITLE = "Swarnalok"
LOG = []

def call(method, url, data=None, file=None, mime=None, name=None):
    cmd = ["curl", "-sS", "-m", "60", "-X", method, url, "-w", "\n%{http_code}"]
    if data is not None:
        cmd += ["-H", "Content-Type: application/json", "--data-binary", json.dumps(data)]
    if file:
        cmd += ["-H", f"Content-Type: {mime}", "-H", f'Content-Disposition: attachment; filename="{name}"', "--data-binary", "@" + str(file)]
    out = subprocess.run(cmd, capture_output=True, text=True).stdout
    body, _, code = out.rpartition("\n")
    try:
        js = json.loads(body) if body.strip() else None
    except json.JSONDecodeError:
        js = {"raw": body[:300]}
    return int(code or 0), js

def ok(code, js, what):
    if code >= 300:
        raise SystemExit(f"FAILED {what}: {code} {json.dumps(js)[:400]}")
    LOG.append(what)
    print("ok ", what)
    return js

def find_page(slug):
    code, js = call("GET", f"{API}/pages?slug={slug}&status=publish,draft,private&context=edit")
    return js[0] if code == 200 and js else None

def upload(path, name, mime):  # images only; WordPress rejects .css/.js uploads
    # reuse an earlier upload with the same name
    code, js = call("GET", f"{API}/media?search={pathlib.Path(name).stem}&per_page=20")
    if code == 200:
        for m in js or []:
            if m.get("source_url", "").rsplit("/", 1)[-1] == name:
                print("ok  reuse", name); return m["source_url"]
    code, js = call("POST", f"{API}/media", file=path, mime=mime, name=name)
    return ok(code, js, f"upload {name}")["source_url"]

def inspect():
    code, me = call("GET", f"{API}/users/me?context=edit")
    print("user:", code, me.get("slug") if isinstance(me, dict) else me, me.get("roles") if isinstance(me, dict) else "")
    code, st = call("GET", f"{API}/settings")
    print("settings:", code, {k: st.get(k) for k in ("title", "show_on_front", "page_on_front", "page_for_posts")} if code == 200 else st)
    code, pages = call("GET", f"{API}/pages?per_page=100&status=publish,draft,private&context=edit&_fields=id,slug,status,title,template")
    print("pages:", code)
    for p in pages or []:
        print("  ", p["id"], p["status"], p["slug"], "|", p["title"]["raw"], "|", p.get("template"))
    code, sb = call("GET", f"{API}/sidebars?context=edit")
    print("sidebars:", code, [(s["id"], s["name"], len(s.get("widgets", []))) for s in sb] if code == 200 else sb)
    code, schema = call("OPTIONS", f"{API}/pages")
    try:
        print("page templates:", schema["schema"]["properties"]["template"]["enum"])
    except Exception:
        print("page templates: n/a")

def install():
    inspect()
    # 1. images and sitewide files
    urls = {f.name: upload(f, f.name, "image/webp") for f in sorted((HERE / "upload").glob("sw-*.webp"))}
    inline = "<style id=\"swarnalok-css\">" + (HERE / "swarnalok.css").read_text() + "</style><script id=\"swarnalok-js\">" + (HERE / "swarnalok.js").read_text() + "</script>"

    # 2. policy pages
    for slug, title in POLICIES:
        html = (HERE / "policies" / f"{slug}.html").read_text()
        page = find_page(slug)
        data = {"title": title, "slug": slug, "status": "publish", "content": html}
        if page:
            ok(*call("POST", f"{API}/pages/{page['id']}", data=data), f"update page {slug}")
        else:
            ok(*call("POST", f"{API}/pages", data=data), f"create page {slug}")

    # 3. homepage content: real image URLs, plus the sitewide CSS/JS loaded from the uploaded files
    home = (HERE / "flatsome-homepage.txt").read_text()
    for name, url in urls.items():
        home = home.replace("/wp-content/uploads/2026/11/" + name, url)
    # the turning ring loads frames by prefix: all 48 must share one folder and name pattern
    spin = urls.get("sw-spin-00.webp", "")
    prefix = spin[: -len("00.webp")]
    if not all(urls.get(f"sw-spin-{i:02d}.webp") == f"{prefix}{i:02d}.webp" for i in range(48)):
        raise SystemExit("ring frames were renamed on upload; fix their names in Media, then re-run")
    home = home.replace("/wp-content/uploads/2026/11/sw-spin-", prefix)

    code, st = call("GET", f"{API}/settings")
    old_front = st.get("page_on_front") if code == 200 and st.get("show_on_front") == "page" else 0

    existing = find_page("swarnalok-home")
    data = {"title": HOME_TITLE, "slug": "swarnalok-home", "status": "publish", "content": home}
    code, schema = call("OPTIONS", f"{API}/pages")
    try:
        temps = schema["schema"]["properties"]["template"]["enum"]
        if "page-blank.php" in temps:
            data["template"] = "page-blank.php"
    except Exception:
        pass
    if existing:
        new = ok(*call("POST", f"{API}/pages/{existing['id']}", data=data), "update new homepage")
    else:
        new = ok(*call("POST", f"{API}/pages", data=data), "create new homepage")

    # 4. make it the front page, then unpublish (draft) the old front page
    ok(*call("POST", f"{API}/settings", data={"show_on_front": "page", "page_on_front": new["id"]}), f"front page = {new['id']}")
    if old_front and old_front != new["id"]:
        ok(*call("POST", f"{API}/pages/{old_front}", data={"status": "draft"}), f"old front page {old_front} set to draft")

    # 5. sitewide button on every page: add the same loader as a Custom HTML widget in a footer sidebar, if available
    code, sb = call("GET", f"{API}/sidebars?context=edit")
    target = None
    if code == 200:
        for s in sb:
            if s.get("status") == "active" and re.search(r"footer", s["id"] + s["name"], re.I):
                target = s["id"]; break
    if target:
        code, widgets = call("GET", f"{API}/widgets?sidebar={target}&context=edit")
        mine = [w for w in (widgets or []) if "swarnalok-js" in json.dumps(w.get("instance", {}))] if code == 200 else []
        body = {"id_base": "custom_html", "sidebar": target, "instance": {"raw": {"title": "", "content": inline}}}
        if mine:
            ok(*call("POST", f"{API}/widgets/{mine[0]['id']}", data=body), f"update sitewide widget in {target}")
        else:
            ok(*call("POST", f"{API}/widgets", data=body), f"sitewide widget in {target}")
    else:
        print("note: no footer sidebar found; adding CSS/JS to the homepage only.")
        home2 = "[ux_html]" + inline + "[/ux_html]\n" + home
        ok(*call("POST", f"{API}/pages/{new['id']}", data={"content": home2}), "homepage with inline CSS/JS")

    print("\nDONE. New front page:", new.get("link"))
    print("Steps:", *LOG, sep="\n  - ")

if __name__ == "__main__":
    {"inspect": inspect, "install": install}[sys.argv[1] if len(sys.argv) > 1 else "inspect"]()
