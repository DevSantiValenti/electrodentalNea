const { chromium } = require("C:/Users/santi/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/.pnpm/playwright@1.61.1/node_modules/playwright");

(async () => {
  const output = "C:/tmp/electrodental-error-view.png";
  const browser = await chromium.launch({ channel: "msedge", headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 980 }, deviceScaleFactor: 1 });
  await page.goto("http://localhost:8080/ruta-para-probar-error", { waitUntil: "networkidle" });
  await page.screenshot({ path: output, fullPage: true });
  await browser.close();
  console.log(output);
})();
