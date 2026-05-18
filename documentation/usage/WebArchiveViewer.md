# Web Archive Viewer (WARC and WACZ)

ETERNA can play back archived web pages directly in the archive view. The feature is built on [ReplayWeb.page](https://replayweb.page/) — a Webrecorder tool that replays web archives entirely within your browser. No extra software is needed.

## What is a web archive?

A **web archive** is a snapshot of one or more web pages. The file contains the HTML, images, stylesheets, scripts and other resources needed to reconstruct what the page looked like at the time of capture, along with metadata about when and how the capture took place.

ETERNA supports two file formats:

| Format | Extension | Description |
|--------|-----------|-------------|
| **WARC** | `.warc` | ISO 28500 — the standard format for web archiving. A sequence of records where each record is an HTTP request and its response. |
| **WACZ** | `.wacz` | Webrecorder's packaged format. A ZIP file containing one or more WARC files plus an index and metadata. Faster to navigate for large archives. |

## Opening a web archive

1. Navigate to the archive package (AIP) that contains the web archive file.
2. Open the file list and locate the file — ETERNA recognises it by its MIME type, extension or PRONOM identifier.
3. Click the file. The viewer opens in the same area as other previews (PDF, image, video).

The viewer loads in an embedded frame and fills the preview pane. The first time you open a web archive, it may take a few seconds for content to appear — the browser is building an index of the archive.

## Using the viewer

Once the viewer has loaded, you see the ReplayWeb.page interface. The main parts are:

### The URL bar

This shows the currently replayed archived URL. You can click in the field and enter another URL that exists in the archive — the viewer will jump to the matching archived version. If you enter a URL that is not in the archive, an error message is shown.

### The timestamp picker

Next to the URL bar there is a timestamp showing **when** the current page was captured. If the same URL exists in multiple versions in the archive, you can switch between them using the timestamp picker — this is often called *time travel* and lets you see how the page changed over time.

### Sidebar tabs

The sidebar contains the following tabs (using ReplayWeb.page's official terminology):

- **Pages** — lists the web pages contained in the archive, often with a thumbnail and timestamp. You can filter the list using the search field in the tab; search by page title, URL, or extracted text content (if a full-text index is included in the WACZ file).
- **Resources** — lists every individual resource in the archive (HTML pages, images, scripts, stylesheets, fonts, etc.). Searched by URL with a choice of match type: *exact*, *prefix* or *substring*.
- **Story** — appears only when the archive includes a curated collection. Selected pages are then presented in a specific order along with descriptive text — common in WACZ files produced by Webrecorder Studio and similar tools.

> ReplayWeb.page's official user guide is available at <https://replayweb.page/docs/user-guide/exploring/> and is the reference source for viewer features.

### Metadata about the archive file

Metadata about the web archive itself can be viewed in two ways:

**1. ETERNA's file detail view** (primary source for file and preservation metadata)

In ETERNA's file list and file detail view you see the technical and administrative properties of the WARC/WACZ file:

- File name and size
- MIME type (`application/warc` or `application/wacz`)
- PRONOM format identifier (e.g. `fmt/289` for WARC 1.0)
- Checksum / hash (for fixity)
- Date the file was ingested and included in the archive package
- Creator of the archive package (AIP), permissions and descriptive metadata

Use this view for all preservation- and audit-related information.

**2. The viewer's *Archive Info* panel (internal archive metadata)**

ReplayWeb.page includes a built-in information panel titled **Archive Info** that opens via the information icon in the viewer toolbar. The panel shows metadata about the loaded WARC/WACZ file:

| Field | Meaning |
|-------|---------|
| **Title** | Title set in the archive (for WACZ files the value is taken from `datapackage.json`). |
| **Filename** | Name of the file the viewer loaded. |
| **Source** | URL the archive was fetched from — in ETERNA this points to the `/api/v2/files/...` endpoint. |
| **Archived Item ID** | The viewer's internal identifier for the loaded item. |
| **Date Loaded** | When the archive was last loaded into the viewer (local time in your browser). |
| **Total Size** | Total size of the archive file. |
| **Validation** | Hash verification result for the WACZ package's contents. Shows the number of verified and invalid hashes. *Invalid* hashes mean one or more files in the package do not match the checksums recorded in `datapackage.json`. |
| **Package Hash** | Hash of the whole WACZ package (when signed). *Not Available* when the package lacks an outer signature. |
| **Observer Public Key** | Public key of the observer that signed the WACZ package (when signed). |
| **Loading Mode** | *Download On-Demand* means the viewer reads only the byte ranges it needs from the server (range requests), not the whole file at once. *Full Download* means the entire file was fetched before playback started. |

Use the Archive Info panel when you need to verify the file's integrity or check where the archive came from. ETERNA's file detail view remains the authoritative source for the archive package's administrative metadata (PRONOM, AIP relationships, permissions, etc.).

### Inside the replayed page

The archived page behaves like a normal web page — you can click links, scroll, expand menus and so on. Links pointing to other archived pages inside the same archive are followed and replayed locally. Links pointing outside the archive (for example, to a URL that was not captured) go nowhere — the browser shows either an error from ReplayWeb.page or a blank page.

> **Note:** The page is replayed locally in your browser. No requests are made to the open internet — even if the page contains links to external scripts or images, those are served from the archive only.

## Searching the archive

Many web archives include an internal page list in the Pages tab. You can filter the list by title or URL to quickly find a specific page.

ETERNA's normal search box does **not** index the contents of the web archive — it only looks for the archive file itself and its metadata. To search inside the captured pages, open the archive in the viewer and use its built-in tools.

## Downloading the original file

If you need the original file (.warc or .wacz) — for long-term preservation, further processing or review outside ETERNA — use the standard download button in the file list or file detail view. The file is downloaded exactly as it was stored, without modifications.

## Viewing in full screen

The viewer has a built-in full-screen button — the monitor/screen icon in the toolbar. Click it to let the viewer fill the whole screen; click it again or press `Esc` to return. The browser's own full-screen mode (typically `F11`) also works as an alternative.

## Frequently asked questions

### Why is the viewer blank or not loading?

The viewer requires your browser to support **Service Workers** — all modern browsers do, but the feature may be disabled in private mode or via browser settings. Try:

1. Closing incognito/private mode and opening the archive in a regular window.
2. Reloading the page (`Ctrl + Shift + R` for a hard reload).
3. Verifying that your browser is up to date.

### Why does it take so long to load?

The whole archive file is read into the browser when the viewer starts. Large WARC files (several hundred MB or more) may take time to index, especially over slow networks. The WACZ format is more efficient for large archives because it ships with a prebuilt index.

### Why don't all links work in the archived page?

A web archive only contains what was captured at collection time. If a link points to a page that was **not** captured — for example, an external domain or a page not included in the crawl — the viewer cannot replay it. This does not mean the archive is broken; it means that specific resource was never collected.

### Can I share a link to a specific archived page?

The URL in the address bar points to ETERNA's viewer page with information about which archive file and URL is being replayed. That link works for other users with permission to the same archive package in ETERNA.

### Which characters are supported in URLs?

The viewer supports URLs with Swedish characters (å, ä, ö) and other Unicode characters. The URL is correctly encoded when sent to ETERNA's API.

## Technical limitations

- **File size:** The entire archive file is loaded into browser memory. Archives over ~1 GB may degrade performance or destabilise the browser.
- **Streaming media:** Video and audio in archives may have limited functionality depending on how they were captured.
- **JavaScript-heavy pages:** Pages that fetch a lot of data dynamically (for example, via API calls that were not captured) may look incomplete.
- **Browser support:** Requires a modern browser with Service Worker API support. ETERNA does not show a specific warning on older browsers — the viewer simply remains blank.
