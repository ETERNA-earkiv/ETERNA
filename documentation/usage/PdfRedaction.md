# PDF Redaction

PDF redaction lets you permanently mask sensitive content in a PDF file. The redacted areas are removed from the file and cannot be recovered.

## Starting a redaction session

Navigate to the file you want to redact and click the **Redact PDF** button in the toolbar. The button is only available for PDF files.

Before the redaction editor opens, a dialog will ask you to provide a reason for the redaction. The reason is recorded in the audit log together with your username and the time of the action.

Enter the reason and click **Confirm** to open the editor. Click **Cancel** to abort without opening the editor.

> **Note:** Depending on your system configuration, providing a reason may be mandatory. When mandatory, the Confirm button is disabled until you have entered a reason.

## Using the redaction editor

In the editor, mark the areas you want to redact by drawing rectangles over the sensitive content. When you are satisfied with your selections, save the file to apply the redactions permanently.

## Audit log

Every time the redaction editor is opened, an entry is created in the audit log containing:

- the user who initiated the redaction
- the file that was redacted
- the date and time
- the reason provided

This ensures that all redaction activity is traceable and can be reviewed later.
