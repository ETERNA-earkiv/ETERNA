# PDF Redaction

PDF redaction lets you permanently mask sensitive content in a PDF file. The redacted areas are removed from the file and cannot be recovered.

## Starting a redaction session

Navigate to the file you want to redact and click the **Redact PDF** button in the toolbar. The button is only available for PDF files.

Before the redaction editor opens, a dialog will ask you to provide a reason for the redaction. The reason is recorded in the audit log together with your username and the time of the action.

Enter the reason and click **Confirm** to open the editor. Click **Cancel** to abort without opening the editor.

> **Note:** Depending on your system configuration, providing a reason may be mandatory. When mandatory, the Confirm button is disabled until you have entered a reason.

## Using the redaction editor

The editor toolbar provides all tools you need to mark, apply, and save redactions.

### Navigation

Use the **Previous page** and **Next page** buttons or type a page number directly into the page selector to move between pages. The sidebar shows thumbnail previews of all pages and can be toggled with the sidebar button. Use the zoom selector to adjust the view: choose fit-to-page, fit-to-width, or a fixed percentage.

### Marking content for redaction

Two tools are available for marking content:

- **Mask text** — select this tool, then click and drag over text in the document to highlight it for redaction. Adjacent lines are automatically merged into a single mark.
- **Mask area** — select this tool, then click and drag anywhere on the page to draw a rectangle over any content you want to redact.

Marked areas are shown with a coloured overlay. Marks are not yet permanent at this stage.

### Applying marks

Click **Applicera** to permanently commit the current marks as redactions. Applied redactions are displayed as solid black rectangles in the editor.

You can continue marking and applying on other pages before saving.

### Undoing and redoing

- **Undo** — steps back through applied redaction groups one at a time.
- **Redo** — re-applies previously undone redaction groups.

### Clearing all redactions

Click the **Clear all redactions** button (circular arrow) to discard all marks and applied redactions. A confirmation dialog will appear before anything is removed.

### Saving

When you are done, click **Save**. A dialog asks you to confirm before the export begins. The editor renders each page and replaces the redacted areas with solid black rectangles in the exported file. A progress indicator shows how many pages have been processed. Once complete, the redacted file is saved back to the archive.

## Audit log

Every time the redaction editor is opened, an entry is created in the audit log containing:

- the user who initiated the redaction
- the file that was redacted
- the date and time
- the reason provided

This ensures that all redaction activity is traceable and can be reviewed later.
