/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.data.exceptions;

import java.io.Serial;

/**
 * ETERNA: optimistisk låsning — den inskickade versionen av ett dokument är
 * äldre än den lagrade (någon annan har sparat emellan). REST-lagret svarar
 * 409 Conflict; klienten bör läsa om dokumentet och göra om ändringen.
 */
public class VersionConflictException extends RODAException {

  @Serial
  private static final long serialVersionUID = -3160458824359214937L;

  public VersionConflictException() {
    super();
  }

  public VersionConflictException(String message) {
    super(message);
  }

  public VersionConflictException(String message, Throwable cause) {
    super(message, cause);
  }

  public VersionConflictException(Throwable cause) {
    super(cause);
  }
}
