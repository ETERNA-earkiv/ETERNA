import fs from "node:fs";
import path from "node:path";
import { hasFileExtension, isInternalPath } from "@astrojs/internal-helpers/path";
import send from "send";
import { resolveClientDir } from "./shared.js";
function createStaticHandler(app, options) {
  const client = resolveClientDir(options);
  return (req, res, ssr) => {
    if (req.url) {
      let fullUrl = req.url;
      if (req.url.includes("#")) {
        fullUrl = fullUrl.slice(0, req.url.indexOf("#"));
      }
      const [urlPath, urlQuery] = fullUrl.split("?");
      const filePath = path.join(client, app.removeBase(urlPath));
      let isDirectory = false;
      try {
        isDirectory = fs.lstatSync(filePath).isDirectory();
      } catch {
      }
      const { trailingSlash = "ignore" } = options;
      const hasSlash = urlPath.endsWith("/");
      let pathname = urlPath;
      if (app.headersMap && app.headersMap.length > 0) {
        const routeData = app.match(req, true);
        if (routeData && routeData.prerender) {
          const matchedRoute = app.headersMap.find((header) => header.pathname.includes(pathname));
          if (matchedRoute) {
            for (const header of matchedRoute.headers) {
              res.setHeader(header.key, header.value);
            }
          }
        }
      }
      switch (trailingSlash) {
        case "never": {
          if (isDirectory && urlPath !== "/" && hasSlash) {
            pathname = urlPath.slice(0, -1) + (urlQuery ? "?" + urlQuery : "");
            res.statusCode = 301;
            res.setHeader("Location", pathname);
            return res.end();
          }
          if (isDirectory && !hasSlash) {
            pathname = `${urlPath}/index.html`;
          }
          break;
        }
        case "ignore": {
          if (isDirectory && !hasSlash) {
            pathname = `${urlPath}/index.html`;
          }
          break;
        }
        case "always": {
          if (!hasSlash && !hasFileExtension(urlPath) && !isInternalPath(urlPath)) {
            pathname = urlPath + "/" + (urlQuery ? "?" + urlQuery : "");
            res.statusCode = 301;
            res.setHeader("Location", pathname);
            return res.end();
          }
          break;
        }
      }
      pathname = prependForwardSlash(app.removeBase(pathname));
      const stream = send(req, pathname, {
        root: client,
        dotfiles: pathname.startsWith("/.well-known/") ? "allow" : "deny"
      });
      let forwardError = false;
      stream.on("error", (err) => {
        if (forwardError) {
          console.error(err.toString());
          res.writeHead(500);
          res.end("Internal server error");
          return;
        }
        ssr();
      });
      stream.on("headers", (_res) => {
        if (pathname.startsWith(`/${options.assets}/`)) {
          _res.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        }
      });
      stream.on("file", () => {
        forwardError = true;
      });
      stream.pipe(res);
    } else {
      ssr();
    }
  };
}
function prependForwardSlash(pth) {
  return pth.startsWith("/") ? pth : "/" + pth;
}
export {
  createStaticHandler
};
