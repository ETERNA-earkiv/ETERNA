# XML Viewer with XSLT Stylesheets

ETERNA can render XML files into a styled HTML preview using XSLT stylesheets, directly in the archive view. The view also lets you switch back to the raw XML, print the rendered output, or — for users with the *Apply custom XSLT* permission — temporarily apply a stylesheet from your own computer without changing the archive package.

## What is a stylesheet?

A **stylesheet** (XSLT — *eXtensible Stylesheet Language Transformations*) is a recipe that converts an XML file into another format, typically HTML. ETERNA uses XSLT to turn structured archive metadata (METS, EAD, custom record schemas, etc.) into a human-readable view.

## Opening an XML file

1. Navigate to the archive package (AIP) that contains the XML file.
2. Open the file list and locate the file. ETERNA recognises XML by MIME type (`text/xml`, `application/xml`, `*+xml`) or by file extension (`.xml`).
3. Click the file. The viewer opens in the same area as other previews.

If a matching stylesheet is available, the rendered HTML view loads immediately. If no stylesheet matches, the raw XML is shown instead — you can still inspect it, print the file, or upload a local stylesheet (if you have permission).

## Using the viewer

The toolbar above the preview pane contains the following controls:

### Toggle: rendered ↔ raw XML

The first button toggles between the **rendered HTML view** and the **raw XML view**. The label updates to show what the next click will do (*View original XML* / *View rendered view*). Use this when you need to verify the underlying XML or compare it with the rendered output.

### Print

The print button opens the currently displayed rendered HTML in a new browser window and triggers the print dialog. The print output uses a tighter margin and hides the browser's default headers and footers where the browser allows it.

> **Note:** Print is available only when the rendered view is active. Printing the raw XML view is not supported — use the browser's own print function if needed.

**Tips for cleaner prints** (Chrome / Edge): in the print dialog, expand *More settings* and turn **Headers and footers** off to remove the page URL and page numbers from the output. Adjust the margins from the same panel if needed.

### Stylesheet selector

The dropdown shows all stylesheets that apply to the current XML file. Sources are merged from three places and shown in a single list:

| Prefix | Source | Description |
|--------|--------|-------------|
| (no prefix) | **Local** | An `.xsl` / `.xslt` file shipped alongside the XML in the same representation. ETERNA prefers stylesheets that share the XML's base filename (`Foo.xml` → `Foo.xslt`). |
| (no prefix) | **AIP documentation** | A stylesheet shipped in the AIP's `documentation` folder. |
| **Global:** | **Global** | A stylesheet installed by the administrator and matched to the XML's namespace. |

If no stylesheet matches, the dropdown shows *(None)* and the raw XML view is selected by default.

### Use a local stylesheet (privileged)

Users with the **Apply custom XSLT** permission also see a *Select file* button below the dropdown. Use it to upload an `.xsl` or `.xslt` file from your computer for a one-off rendering:

- The file is sent to the server, applied to the XML, and the result is displayed.
- The uploaded stylesheet is **not** saved to the archive package — a note next to the button reminds you of this.
- Subsequent re-selections of the same filename re-use the previously rendered output without uploading again, within the current viewer session.

**Limits:**

- Maximum file size: **1 MB**.
- The file must be well-formed XML. External entity references (DTDs) are not allowed.
- Files larger than the limit, or files that fail validation, are rejected with an error message; the rendered view remains unchanged.

## Audit trail

Server-side rendering events are recorded in ETERNA's audit log:

| Event | Logged values |
|---|---|
| Listing available stylesheets | File UUID |
| Rendering with a global / AIP-bundled stylesheet | File UUID, stylesheet identifier, language |
| Rendering with an uploaded stylesheet | File UUID, uploaded filename, file size, language |

The **contents** of an uploaded stylesheet are never written to the audit log — only its filename and size.

Purely client-side actions (toggling rendered/raw, printing, re-selecting a cached upload) are **not** audited individually — consistent with ETERNA's server-based audit model. If you need a stricter audit trail for these actions, contact your administrator.

## Frequently asked questions

### Why is *View rendered view* greyed out?

No matching stylesheet was found for this XML file, so there is nothing to render yet. Upload a local stylesheet (if you have permission), or ask your administrator to install a global stylesheet for the XML namespace.

### Why does my uploaded stylesheet disappear when I open the file later?

Local stylesheet uploads are intentionally session-only — they exist for ad-hoc inspection without changing the archive. To make a stylesheet permanent, place it next to the XML in the representation, in the AIP's documentation folder, or have an administrator install it as a global stylesheet.

### I get *XSLT file too large* — what's the limit?

1 MB. Stylesheets larger than that are rejected on upload. Most rendering stylesheets are far smaller than this; if you hit the limit, the stylesheet is likely embedding large amounts of static content that should be referenced rather than inlined.

### My XSLT works in a desktop tool but fails in ETERNA — why?

ETERNA's transformer disables external resources (DTDs, external entities, `document()` calls reaching outside the package) for security. Stylesheets that depend on fetching external files at transformation time are not supported. Inline what you need, or use a global stylesheet placed under ETERNA's configuration.

### Can I print the raw XML?

The print button is only enabled for the rendered HTML view. To print raw XML, switch to *View original XML* and use the browser's own print function (`Ctrl + P` / `⌘ + P`).

## Technical limitations

- The viewer renders the XML server-side; very large XML files may take longer to render or time out at 30 seconds for custom uploads.
- The rendered HTML runs in a sandboxed iframe with no JavaScript execution, no form submission, and no top-level navigation. Interactive features inside the stylesheet output are intentionally inert.
- Print output depends on the browser's print engine and the stylesheet's CSS — complex layouts may need a print-specific stylesheet for best results.
