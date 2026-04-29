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
}
