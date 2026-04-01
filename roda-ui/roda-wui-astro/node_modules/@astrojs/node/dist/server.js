import "./polyfill.js";
import { existsSync, readFileSync } from "node:fs";
import { NodeApp } from "astro/app/node";
import { setGetEnv } from "astro/env/setup";
import createMiddleware from "./middleware.js";
import { STATIC_HEADERS_FILE } from "./shared.js";
import startServer, { createStandaloneHandler } from "./standalone.js";
setGetEnv((key) => process.env[key]);
function createExports(manifest, options) {
  const app = new NodeApp(manifest, !options.experimentalDisableStreaming);
  let headersMap = void 0;
  if (options.experimentalStaticHeaders) {
    headersMap = readHeadersJson(manifest.outDir);
  }
  if (headersMap) {
    app.setHeadersMap(headersMap);
  }
  options.trailingSlash = manifest.trailingSlash;
  return {
    options,
    handler: options.mode === "middleware" ? createMiddleware(app, options) : createStandaloneHandler(app, options),
    startServer: () => startServer(app, options)
  };
}
function start(manifest, options) {
  if (options.mode !== "standalone" || process.env.ASTRO_NODE_AUTOSTART === "disabled") {
    return;
  }
  let headersMap = void 0;
  if (options.experimentalStaticHeaders) {
    headersMap = readHeadersJson(manifest.outDir);
  }
  const app = new NodeApp(manifest, !options.experimentalDisableStreaming);
  if (headersMap) {
    app.setHeadersMap(headersMap);
  }
  startServer(app, options);
}
function readHeadersJson(outDir) {
  let headersMap = void 0;
  const headersUrl = new URL(STATIC_HEADERS_FILE, outDir);
  if (existsSync(headersUrl)) {
    const content = readFileSync(headersUrl, "utf-8");
    try {
      headersMap = JSON.parse(content);
    } catch (e) {
      console.error("[@astrojs/node] Error parsing _headers.json: " + e.message);
      console.error("[@astrojs/node] Please make sure your _headers.json is valid JSON.");
    }
  }
  return headersMap;
}
export {
  createExports,
  start
};
