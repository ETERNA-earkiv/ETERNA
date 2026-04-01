import { AsyncLocalStorage } from "node:async_hooks";
import { createReadStream } from "node:fs";
import path from "node:path";
import { Readable } from "node:stream";
import { NodeApp } from "astro/app/node";
import { resolveClientDir } from "./shared.js";
async function readErrorPageFromDisk(client, status) {
  const filePaths = [`${status}.html`, `${status}/index.html`];
  for (const filePath of filePaths) {
    const fullPath = path.join(client, filePath);
    try {
      const stream = createReadStream(fullPath);
      await new Promise((resolve, reject) => {
        stream.once("open", () => resolve());
        stream.once("error", reject);
      });
      const webStream = Readable.toWeb(stream);
      return new Response(webStream, {
        headers: { "Content-Type": "text/html; charset=utf-8" }
      });
    } catch {
    }
  }
  return void 0;
}
function createAppHandler(app, options) {
  const als = new AsyncLocalStorage();
  const logger = app.getAdapterLogger();
  process.on("unhandledRejection", (reason) => {
    const requestUrl = als.getStore();
    logger.error(`Unhandled rejection while rendering ${requestUrl}`);
    console.error(reason);
  });
  const client = resolveClientDir(options);
  const prerenderedErrorPageFetch = async (url) => {
    if (url.includes("/404")) {
      const response = await readErrorPageFromDisk(client, 404);
      if (response) return response;
    }
    if (url.includes("/500")) {
      const response = await readErrorPageFromDisk(client, 500);
      if (response) return response;
    }
    if (options.experimentalErrorPageHost) {
      const originUrl = new URL(options.experimentalErrorPageHost);
      const errorPageUrl = new URL(url);
      errorPageUrl.protocol = originUrl.protocol;
      errorPageUrl.host = originUrl.host;
      return fetch(errorPageUrl);
    }
    return new Response(null, { status: 404 });
  };
  return async (req, res, next, locals) => {
    let request;
    try {
      request = NodeApp.createRequest(req, {
        allowedDomains: app.getAllowedDomains?.() ?? []
      });
    } catch (err) {
      logger.error(`Could not render ${req.url}`);
      console.error(err);
      res.statusCode = 500;
      res.end("Internal Server Error");
      return;
    }
    const routeData = app.match(request, true);
    if (routeData) {
      const response = await als.run(
        request.url,
        () => app.render(request, {
          addCookieHeader: true,
          locals,
          routeData,
          prerenderedErrorPageFetch
        })
      );
      await NodeApp.writeResponse(response, res);
    } else if (next) {
      return next();
    } else {
      const response = await app.render(req, { addCookieHeader: true, prerenderedErrorPageFetch });
      await NodeApp.writeResponse(response, res);
    }
  };
}
export {
  createAppHandler
};
