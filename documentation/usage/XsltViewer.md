# XML Viewer with XSLT Stylesheets

ETERNA can render XML files as a formatted HTML page using stylesheets, directly in the interface. The toolbar lets you switch between XML and a rendered view using XSLT files from the package, or — if you have the *Apply custom XSLT* permission — temporarily apply a stylesheet from your local computer without modifying the archive package.

## What is a stylesheet?

A **stylesheet** (XSLT — *eXtensible Stylesheet Language Transformations*) is a file that converts an XML file into another format, typically HTML. Among other things, ETERNA uses XSLT to make structured archive metadata (METS, EAD, and other standards) readable for humans.

## Opening an XML file

1. Navigate to the archive package (AIP) that contains the XML file.
2. Open the file list and locate the file. ETERNA recognises XML by MIME type (`text/xml`, `application/xml`, `*+xml`) or by file extension (`.xml`).
3. Click the file. The viewer opens in the same area as other previews.

If a stylesheet is available in the archive package, the rendered HTML view loads immediately. Otherwise the raw XML is shown — you can still inspect it, print the file, or upload a local stylesheet (if you have permission).

## Using the toolbar

The buttons above the preview pane are:

### Toggle: rendered ↔ raw XML

The first button toggles between the **rendered HTML view** and the **raw XML view**. The label shows what the next click will do (*View original XML* / *View rendered view*). Use this when you need to verify the underlying XML or compare it with the rendered output.

### Print

The print button opens the currently displayed rendered HTML in a new browser window and triggers the print dialog. The print output uses tighter margins and hides the browser's default headers and footers where the browser allows it.

> **Note:** Print is available only when the rendered view is active. Printing the raw XML view is not supported — use the browser's own print function if needed (`Ctrl + P`).

**Tips for cleaner prints** (Chrome / Edge): in the print dialog, expand *More settings* and turn off **Headers and footers** to remove the page URL and page numbers. Adjust the margins from the same panel if needed.

### Stylesheet selector

The dropdown shows all stylesheets that apply to the current XML file. Sources are merged into a single list:

| Prefix | Source | Description |
|--------|--------|-------------|
| (no prefix) | **AIP representation data folder** | An `.xsl` / `.xslt` file shipped alongside the XML in the same representation. ETERNA prefers stylesheets that share the XML's base filename (`Foo.xml` → `Foo.xslt`). |
| (no prefix) | **AIP documentation (representation level)** | A stylesheet shipped in a documentation folder at the AIP's representation level. |
| (no prefix) | **AIP documentation (root level)** | A stylesheet shipped in the AIP's `documentation` folder. |
| **Lokal:** | **Local** | A stylesheet uploaded from the local computer for one-off rendering. Not saved in ETERNA or in the AIP. |
| **Global:** | **Global** | A stylesheet installed by the administrator and bound to the XML's namespace. |

If no stylesheet matches, the dropdown shows *(None)* and the raw XML view is selected by default.

### Use a local stylesheet (privileged)

Users with the **Apply custom XSLT** permission also see a *Select file* button below the dropdown. Use it to upload an `.xsl` or `.xslt` file from your own computer for a one-off rendering:

- The file is sent to the server, applied to the XML, and the result is displayed.
- The uploaded stylesheet is **not** saved to the archive package — a note in the toolbar reminds you of this.
- If you later select the same filename again, the previously rendered output is reused without re-uploading, as long as the viewer session is active.

**Limits:**

- Maximum file size: **1 MB**.
- The file must be well-formed XML. External entity references (DTDs) are not allowed.
- Files that exceed the limit or fail validation are rejected with an error message; the rendered view is left unchanged.

## Audit trail

Server-side rendering is recorded in ETERNA's audit log:

| Event | Logged values |
|---|---|
| Listing available stylesheets | File UUID |
| Rendering with a global / AIP-bundled stylesheet | File UUID, stylesheet ID, language |
| Rendering with an uploaded stylesheet | File UUID, uploaded filename, file size, language |

The **contents** of an uploaded stylesheet are never logged — only its filename and size.

Purely client-side actions (toggling rendered/raw, printing, re-selecting a cached upload) are **not** audited individually — consistent with ETERNA's server-based audit model. Contact your administrator if a stricter audit trail is required for these actions.

## Frequently asked questions

### Why is *View rendered view* greyed out?

No matching stylesheet was found for this XML file, so there is nothing to render. Upload a local stylesheet (if you have permission), or ask your administrator to install a global stylesheet for the XML namespace.

### Why does my uploaded stylesheet disappear when I open the file later?

Local stylesheet uploads are intentionally session-bound. They exist to inspect ad hoc without modifying the archive. To make a stylesheet permanent: place it next to the XML in the representation, in the AIP's documentation folder, or have an administrator install it as a global stylesheet.

### I get *XSLT file too large* — what's the limit?

1 MB. Larger stylesheets are rejected on upload. Most rendering stylesheets are far smaller than that; if you hit the limit it usually comes from inlined static content that should be referenced instead.

### My XSLT works in a desktop tool but not in ETERNA — why?

ETERNA's transformer disables external resources (DTDs, external entities, `document()` calls reaching outside the package) for security. Stylesheets that fetch external files at transformation time are not supported. Inline what you need, or use a global stylesheet installed by your administrator under ETERNA's configuration.

### Can I print the raw XML?

The print button is only enabled in the rendered view. To print raw XML, switch to *View original XML* and use the browser's own print function (`Ctrl + P` / `⌘ + P`).

## Technical limitations

- The viewer renders XML server-side, so very large XML files may take time or time out after 30 seconds for local uploads.
- The rendered HTML runs in a sandboxed iframe with no JavaScript execution, form submission, or top-level navigation. Interactive elements in the stylesheet output are intentionally inert.
- Print output depends on the browser's print engine and the stylesheet's CSS; complex layouts may need a print-specific stylesheet for best results.
