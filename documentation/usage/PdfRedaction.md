# PDF Redaction

PDF redaction lets you permanently mask sensitive content in a PDF file. The redacted areas are removed from the file and cannot be recovered.

## Starting a redaction session

Navigate to the file you want to redact and click the **Redact PDF** button in the toolbar. The button is only available for PDF files. The redaction editor opens immediately.

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

When you are done, click **Save**. A dialog will first ask you to provide a reason for the redaction. The reason is recorded in the audit log together with your username and the time of the action.

Enter the reason and click **Confirm** to proceed. Click **Cancel** to return to the editor without saving.

> **Note:** Depending on your system configuration, providing a reason may be mandatory. By default it is mandatory. When mandatory, the Confirm button is disabled until you have entered a reason.

After confirming the reason, a second dialog asks you to confirm the export. The editor renders each page and replaces the redacted areas with solid black rectangles in the exported file. A progress indicator shows how many pages have been processed. Once complete, the redacted file is saved back to the archive.

## Audit log

Every time the redaction editor is opened, an entry is created in the audit log containing:

- the user who initiated the redaction
- the file that was redacted
- the date and time
- the reason provided

Each save is also recorded as a separate entry with the action **Upload File Resource**. To find all redaction saves in the audit log, type `Maskerad PDF sparad` in the search box or filter on that value in the right sidebar. The status field on the entry shows whether the save succeeded or failed.

This ensures that all redaction activity is traceable and can be reviewed later.
