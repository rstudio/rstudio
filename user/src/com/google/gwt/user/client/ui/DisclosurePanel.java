/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.user.client.ui;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A widget that consists of a header and a content panel that discloses the
 * content when a user clicks on the header.
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class="css">
 * <li>.gwt-DisclosurePanel-open { the panel when it is open }</li>
 * <li>.gwt-DisclosurePanel-closed { the panel when it is closed }</li>
 * <li>.header { the header section }</li>
 * <li>.content { the content section }</li>
 * </ul>
 * 
 * <p>
 * <img class='gallery' src='DisclosurePanel.png'/>
 * </p>
 * 
 * <p>
 * The header and content sections can be easily selected using css with a child
 * selector:<br/> .gwt-DisclosurePanel-open .header { ... }
 * </p>
 */
public final class DisclosurePanel extends Composite implements
    FiresDisclosureEvents, HasWidgets {

  /**
   * Used to wrap widgets in the header to provide click support. Effectively
   * wraps the widget in an <code>anchor</code> to get automatic keyboard
   * access.
   */
  private final class ClickableHeader extends SimplePanel {

    private ClickableHeader() {
      // Anchor is used to allow keyboard access.
      super(DOM.createAnchor());
      Element elem = getElement();
      DOM.setElementProperty(elem, "href", "javascript:void(0);");
      // Avoids layout problems from having blocks in inlines.
      DOM.setStyleAttribute(elem, "display", "block");
      sinkEvents(Event.ONCLICK);
      setStyleName(STYLENAME_HEADER);
    }

    public final void onBrowserEvent(Event event) {
      // no need to call super.
      switch (DOM.eventGetType(event)) {
        case Event.ONCLICK:
          // Prevent link default action.
          DOM.eventPreventDefault(event);
          setOpen(!isOpen);
      }
    }
  }

  /**
   * The default header widget used within a {@link DisclosurePanel}.
   */
  private class DefaultHeader extends Widget implements HasText,
      DisclosureHandler {

    /**
     * imageTD holds the image for the icon, not null. labelTD holds the text
     * for the label.
     */
    private final Element labelTD;

    private final Image iconImage = new Image(ICON_IMAGE, -ICON_SIZE[0], 0,
        ICON_SIZE[0], ICON_SIZE[1]);

    private DefaultHeader(String text) {
      // I do not need any Widgets here, just a DOM structure.
      Element root = DOM.createTable();
      Element tbody = DOM.createTBody();
      Element tr = DOM.createTR();
      final Element imageTD = DOM.createTD();
      labelTD = DOM.createTD();

      setElement(root);

      DOM.appendChild(root, tbody);
      DOM.appendChild(tbody, tr);
      DOM.appendChild(tr, imageTD);
      DOM.appendChild(tr, labelTD);

      // set image TD to be same width as image.
      DOM.setElementProperty(imageTD, "align", "center");
      DOM.setElementProperty(imageTD, "valign", "middle");
      DOM.setStyleAttribute(imageTD, "width", ICON_SIZE[0] + "px");

      DOM.appendChild(imageTD, iconImage.getElement());

      setText(text);

      addEventHandler(this);
      setStyle();
    }

    public final String getText() {
      return DOM.getInnerText(labelTD);
    }

    public final void onClose(DisclosureEvent event) {
      setStyle();
    }

    public final void onOpen(DisclosureEvent event) {
      setStyle();
    }

    public final void setText(String text) {
      DOM.setInnerText(labelTD, text);
    }

    private void setStyle() {
      iconImage.setUrlAndVisibleRect(ICON_IMAGE, (isOpen) ? ICON_SIZE[0] : 0,
          0, ICON_SIZE[0], ICON_SIZE[1]);
    }
  }

  // Stylename constants.
  private static final String STYLENAME_DEFAULT = "gwt-DisclosurePanel";

  private static final String STYLENAME_POSTFIX_OPEN = "-open";

  private static final String STYLENAME_POSTFIX_CLOSED = "-closed";

  private static final String STYLENAME_HEADER = "header";

  private static final String STYLENAME_CONTENT = "content";

  // Icon properties.
  private static final String ICON_IMAGE = "disclosure.png";

  private static final int[] ICON_SIZE = new int[] {16, 16};

  /**
   * top level widget. The first child will be a reference to {@link #header}.
   * The second child will either not exist or be a non-null to reference to
   * {@link #content}.
   */
  private final VerticalPanel mainPanel = new VerticalPanel();

  /**
   * holds the header widget.
   */
  private final ClickableHeader header = new ClickableHeader();

  /**
   * the content widget, this can be null.
   */
  private Widget content;

  private boolean isOpen = false;

  private String baseStylename = STYLENAME_DEFAULT;

  /**
   * null until #{@link #addEventHandler(DisclosureHandler)} is called (lazily
   * initialized).
   */
  private ArrayList /* <DisclosureHandler> */handlers;

  /**
   * Creates an empty DisclosurePanel that is initially closed.
   */
  public DisclosurePanel() {
    init(false);
  }

  /**
   * Creates a DisclosurePanel that will be initially closed using the specified
   * text in the header.
   * 
   * @param headerText the text to be displayed in the header.
   */
  public DisclosurePanel(String headerText) {
    this(headerText, false);
  }

  /**
   * Creates a DisclosurePanel with the specified header text and an initial
   * open/close state.
   * 
   * @param headerText the text to be displayed in the header.
   * @param isOpen the initial open/close state of the content panel.
   */
  public DisclosurePanel(String headerText, boolean isOpen) {
    init(isOpen);
    setHeader(new DefaultHeader(headerText));
  }

  /**
   * Creates a DisclosurePanel that will be initially closed using a widget as
   * the header.
   * 
   * @param header the widget to be used as a header.
   */
  public DisclosurePanel(Widget header) {
    this(header, false);
  }

  /**
   * Creates a DisclosurePanel using a widget as the header and an initial
   * open/close state.
   * 
   * @param header the widget to be used as a header
   * @param isOpen the initial open/close state of the content panel
   */
  public DisclosurePanel(Widget header, boolean isOpen) {
    init(isOpen);
    setHeader(header);
  }

  public void add(Widget w) {
    if (this.getContent() == null) {
      setContent(w);
    } else {
      throw new IllegalStateException(
          "A DisclosurePanel can only contain two Widgets.");
    }
  }

  /**
   * Attaches an event handler to the panel to receive {@link DisclosureEvent}
   * notification.
   * 
   * @param handler the handler to be added (should not be null)
   */
  public final void addEventHandler(DisclosureHandler handler) {
    if (handlers == null) {
      handlers = new ArrayList();
    }
    handlers.add(handler);
  }

  public void clear() {
    setContent(null);
  }

  /**
   * Gets the widget that was previously set in {@link #setContent(Widget)}.
   * 
   * @return the panel's current content widget
   */
  public final Widget getContent() {
    return content;
  }

  /**
   * Gets the widget that is currently being used as a header.
   * 
   * @return the widget currently being used as a header
   */
  public final Widget getHeader() {
    return header.getWidget();
  }

  /**
   * Gets a {@link HasText} instance to provide access to the headers's text, if
   * the header widget does provide such access.
   * 
   * @return a reference to the header widget if it implements {@link HasText},
   *         <code>null</code> otherwise
   */
  public final HasText getHeaderTextAccessor() {
    Widget widget = header.getWidget();
    return (widget instanceof HasText) ? (HasText) widget : null;
  }

  /**
   * Determines whether the panel is open.
   * 
   * @return <code>true</code> if panel is in open state
   */
  public final boolean isOpen() {
    return isOpen;
  }

  public Iterator iterator() {
    return WidgetIterators.createWidgetIterator(this,
        new Widget[] {getContent()});
  }

  public boolean remove(Widget w) {
    if (w == getContent()) {
      setContent(null);
      return true;
    }
    return false;
  }

  /**
   * Removes an event handler from the panel.
   * 
   * @param handler the handler to be removed
   */
  public final void removeEventHandler(DisclosureHandler handler) {
    if (handlers == null) {
      return;
    }
    handlers.remove(handler);
  }

  /**
   * Sets the content widget which can be opened and closed by this panel. If
   * there is a preexisting content widget, it will be detached.
   * 
   * @param content the widget to be used as the content panel
   */
  public final void setContent(Widget content) {
    final Widget currentContent = this.content;

    // Remove existing content widget.
    if (currentContent != null) {
      mainPanel.remove(currentContent);
      currentContent.removeStyleName(STYLENAME_CONTENT);
    }

    // Add new content widget if != null.
    this.content = content;
    if (content != null) {
      mainPanel.add(content);
      content.addStyleName(STYLENAME_CONTENT);
      setContentDisplay();
    }
  }

  /**
   * Sets the widget used as the header for the panel.
   * 
   * @param headerWidget the widget to be used as the header
   */
  public final void setHeader(Widget headerWidget) {
    header.setWidget(headerWidget);
  }

  /**
   * Changes the visible state of this <code>DisclosurePanel</code>.
   * 
   * @param isOpen <code>true</code> to open the panel, <code>false</code>
   *          to close
   */
  public final void setOpen(boolean isOpen) {
    if (this.isOpen != isOpen) {
      this.isOpen = isOpen;
      setContentDisplay();
      fireEvent();
    }
  }

  /**
   * Sets the base stylename on this panel.
   * <p>
   * The style name is the prefix that is used to create CSS class selectors to
   * style this widget. The default style name is ".gwt-DisclosurePanel". The
   * appropriate CSS selectors are .STYLENAME-open and .STYLENAME-closed for the
   * open and closed states, respectively.
   * </p>
   * 
   * @param styleName the prefix to uses in CSS class names.
   * @see com.google.gwt.user.client.ui.UIObject#setStyleName(java.lang.String)
   */
  public final void setStyleName(String styleName) {
    removeStyleName(baseStylename
        + ((isOpen) ? STYLENAME_POSTFIX_OPEN : STYLENAME_POSTFIX_CLOSED));
    baseStylename = styleName;
    setContentDisplay();
  }

  private void fireEvent() {
    if (handlers == null) {
      return;
    }

    DisclosureEvent event = new DisclosureEvent(this);
    for (Iterator it = handlers.iterator(); it.hasNext();) {
      DisclosureHandler handler = (DisclosureHandler) it.next();
      if (isOpen) {
        handler.onOpen(event);
      } else {
        handler.onClose(event);
      }
    }
  }

  private void init(boolean isOpen) {
    initWidget(mainPanel);
    mainPanel.add(header);
    this.isOpen = isOpen;
    setContentDisplay();
  }

  private void setContentDisplay() {
    // UIObject#replaceStylename has been suggested and would replace this.
    if (isOpen) {
      removeStyleName(baseStylename + STYLENAME_POSTFIX_CLOSED);
      addStyleName(baseStylename + STYLENAME_POSTFIX_OPEN);
    } else {
      removeStyleName(baseStylename + STYLENAME_POSTFIX_OPEN);
      addStyleName(baseStylename + STYLENAME_POSTFIX_CLOSED);
    }

    if (content != null) {
      content.setVisible(isOpen);
    }
  }
}
