/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.common.dialogs;

/**
 * A selectable row (tree node or search result) whose highlight the {@link SelectAipTreeDialog} toggles.
 *
 * <p>
 * Kept as a top-level type rather than nested in the dialog so that {@link SelectAipTreeNode} can implement
 * it without creating a cyclic reference between the dialog and the node (the dialog implements the node's
 * {@link SelectAipTreeNode.SelectionListener}).
 * </p>
 */
public interface Selectable {
  void setSelected(boolean selected);
}
