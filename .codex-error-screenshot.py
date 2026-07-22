from pathlib import Path

from playwright.sync_api import sync_playwright


output = Path(r"D:\Proyecto Electrodental NUEVO\electrodental-error-view.png")
output.parent.mkdir(parents=True, exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1440, "height": 980}, device_scale_factor=1)
    page.goto("http://localhost:8080/ruta-para-probar-error", wait_until="networkidle")
    page.screenshot(path=str(output), full_page=True)
    browser.close()

print(output)
