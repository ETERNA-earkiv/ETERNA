/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.core.model;

import org.roda.core.transaction.TransactionalService;

/**
 * @author Gabriel Barros <gbarros@keep.pt>
 */
public interface TransactionalModelService extends ModelService, TransactionalService {

  /**
   * ETERNA: registrerar en åtgärd som körs först när transaktionen har
   * committats till huvudlagringen (efter {@link #commit()}); vid rollback
   * förkastas den. Används t.ex. för att starta jobb som måste se
   * transaktionens resultat (åtkomstreglernas efterkommande-jobb). Åtgärden
   * får inte kasta; fel loggas av implementationen.
   */
  void addAfterCommitAction(Runnable action);
}
