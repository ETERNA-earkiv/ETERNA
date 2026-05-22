/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE file at the root of the source
 * tree and available online at
 *
 * https://github.com/ETERNA-earkiv/ETERNA
 */
package org.roda.wui.client.common.cards;

import java.util.List;
import java.util.Map;

import org.roda.wui.client.common.labels.Tag;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

/**
 *
 * @author Alexandre Flores <aflores@keep.pt>
 */
public class ThumbnailCard extends Composite {
  public static final ClientMessages messages = GWT.create(ClientMessages.class);
  private static ThumbnailCard.MyUiBinder uiBinder = GWT.create(ThumbnailCard.MyUiBinder.class);

  @UiField
  Label title;
  @UiField
  FocusPanel clickable;
  @UiField
  FlowPanel thumbnail;
  @UiField
  FlowPanel tags;
  @UiField
  FlowPanel attributes;

  private boolean collapsed = true;

  private ClickHandler thumbnailClickHandler;

  public ThumbnailCard(String title, Widget iconThumbnail, List<Tag> tags, Map<String, String> attributes,
    ClickHandler thumbnailClickHandler) {
    initWidget(uiBinder.createAndBindUi(this));
    this.thumbnailClickHandler = thumbnailClickHandler;

    collapse();

    this.clickable.addClickHandler(thumbnailClickHandler);

    this.title.setText(title);
    this.title.addClickHandler(event -> toggleCollapse());

    iconThumbnail.addStyleName("thumbnailCardIconThumbnail");
    this.thumbnail.add(iconThumbnail);

    for (Tag tag : tags) {
      this.tags.add(tag);
    }

    // Label auto-escapes HTML; safe for user-supplied metadata
    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
      FlowPanel attributePanel = new FlowPanel();
      attributePanel.add(new Label(attribute.getKey()));
      attributePanel.add(new Label(attribute.getValue()));
      this.attributes.add(attributePanel);
    }
  }

  /** Make the whole card surface clickable and keyboard-activatable. */
  public ThumbnailCard enableWholeCardClick() {
    // Stop propagation so the root handler doesn't fire twice
    this.clickable.addClickHandler(event -> event.stopPropagation());

    addDomHandler(thumbnailClickHandler, ClickEvent.getType());

    getElement().setTabIndex(0);
    addDomHandler(event -> {
      int keyCode = event.getNativeKeyCode();
      if (keyCode == KeyCodes.KEY_ENTER || keyCode == KeyCodes.KEY_SPACE) {
        event.preventDefault();
        thumbnailClickHandler.onClick(null);
      }
    }, KeyDownEvent.getType());

    return this;
  }

  public void toggleCollapse() {
    if (collapsed) {
      expand();
    } else {
      collapse();
    }
  }

  public void collapse() {
    collapsed = true;
    addStyleName("thumbnailCardExpandable");
    removeStyleName("thumbnailCardCollapsible");
  }

  public void expand() {
    collapsed = false;
    addStyleName("thumbnailCardCollapsible");
    removeStyleName("thumbnailCardExpandable");
  }

  interface MyUiBinder extends UiBinder<Widget, ThumbnailCard> {
  }
}
