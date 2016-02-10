/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.logical.shared.HasOpenHandlers;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.safehtml.shared.annotations.SuppressIsSafeUriCastCheck;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

import java.util.Iterator;

/**
 * A widget that consists of a header and a content panel that discloses the
 * content when a user clicks on the header.
 * 
 * <h3>CSS Style Rules</h3> 
 * <dl class="css"> 
 * <dt>.gwt-DisclosurePanel 
 * <dd>the panel's primary style 
 * <dt>.gwt-DisclosurePanel-open 
 * <dd> dependent style set when panel is open 
 * <dt>.gwt-DisclosurePanel-closed 
 * <dd> dependent style set when panel is closed
 * </dl>
 * <p>
 * <img class='gallery' src='doc-files/DisclosurePanel.png'/>
 * </p>
 * 
 * <p>
 * The header and content sections can be easily selected using css with a child
 * selector:<br/>
 * .gwt-DisclosurePanel-open .header { ... }
 * </p>
 * <h3>Use in UiBinder Templates</h3>
 * <p>
 * DisclosurePanel elements in  
 * {@link com.google.gwt.uibinder.client.UiBinder UiBinder} templates can 
 * have one widget child and one of two types of header elements. A 
 * &lt;g:header> element can hold text (not html), or a &lt;g:customHeader> element
 * can hold a widget. (Note that the tags of the header elements are not
 * capitalized. This is meant to signal that the header is not a runtime object, 
 * and so cannot have a <code>ui:field</code> attribute.) 
 * <p>
 * For example:<pre>
 * &lt;g:DisclosurePanel>
 *   &lt;g:header>Text header&lt;/g:header>
 *   &lt;g:Label>Widget body&lt;/g:Label>
 * &lt;/g:DisclosurePanel>
 *
 * &lt;g:DisclosurePanel>
 *   &lt;g:customHeader>
 *     &lt;g:Label>Widget header&lt;/g:Label>
 *   &lt;/g:customHeader>
 *   &lt;g:Label>Widget body&lt;/g:Label>
 * &lt;/g:DisclosurePanel>
 * </pre>
 */
@SuppressWarnings("deprecation")
public final class DisclosurePanel extends Composite implements
    FiresDisclosureEvents, HasWidgets.ForIsWidget, HasAnimation,
    HasOpenHandlers<DisclosurePanel>, HasCloseHandlers<DisclosurePanel> {
  interface DefaultImages extends ClientBundle {
    @ImageOptions(flipRtl = true)
    ImageResource disclosurePanelClosed();

    ImageResource disclosurePanelOpen();
  }

  private static final DefaultImages DEFAULT_IMAGES = GWT.create(DefaultImages.class);

  /**
   * Used to wrap widgets in the header to provide click support. Effectively
   * wraps the widget in an <code>anchor</code> to get automatic keyboard
   * access.
   */
  private final class ClickableHeader extends SimplePanel {
    @SuppressIsSafeUriCastCheck //TODO(bangert): refactor away setproperty.
    private ClickableHeader() {
      // Anchor is used to allow keyboard access.
      super(DOM.createAnchor());
      Element elem = getElement();
      elem.setPropertyString("href", "javascript:void(0);");
      // Avoids layout problems from having blocks in inlines.
      elem.getStyle().setProperty("display", "block");
      sinkEvents(Event.ONCLICK);
      setStyleName(STYLENAME_HEADER);
    }

    @Override
    public void onBrowserEvent(Event event) {
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
   * An {@link Animation} used to open the content.
   */
  private static class ContentAnimation extends Animation {
    /**
     * Whether the item is being opened or closed.
     */
    private boolean opening;

    /**
     * The {@link DisclosurePanel} being affected.
     */
    private DisclosurePanel curPanel;

    /**
     * Open or close the content.
     * 
     * @param panel the panel to open or close
     * @param animate true to animate, false to open instantly
     */
    public void setOpen(DisclosurePanel panel, boolean animate) {
      // Immediately complete previous open
      cancel();

      // Open the new item
      if (animate) {
        curPanel = panel;
        opening = panel.isOpen;
        run(ANIMATION_DURATION);
      } else {
        panel.contentWrapper.setVisible(panel.isOpen);
        if (panel.isOpen) {
          // Special treatment on the visible case to ensure LazyPanel works
          panel.getContent().setVisible(true);
        }
      }
    }

    @Override
    protected void onComplete() {
      if (!opening) {
        curPanel.contentWrapper.setVisible(false);
      }
      curPanel.contentWrapper.getElement().getStyle().setProperty("height", "auto");
      curPanel = null;
    }

    @Override
    protected void onStart() {
      super.onStart();
      if (opening) {
        curPanel.contentWrapper.setVisible(true);
        // Special treatment on the visible case to ensure LazyPanel works
        curPanel.getContent().setVisible(true);
      }
    }

    @Override
    protected void onUpdate(double progress) {
      int scrollHeight = curPanel.contentWrapper.getElement().getPropertyInt("scrollHeight");
      int height = (int) (progress * scrollHeight);
      if (!opening) {
        height = scrollHeight - height;
      }
      height = Math.max(height, 1);
      curPanel.contentWrapper.getElement().getStyle().setProperty("height", height + "px");
      curPanel.contentWrapper.getElement().getStyle().setProperty("width", "auto");
    }
  }

  /**
   * The default header widget used within a {@link DisclosurePanel}.
   */
  private class DefaultHeader extends Widget implements HasText,
      OpenHandler<DisclosurePanel>, CloseHandler<DisclosurePanel> {

    /**
     * imageTD holds the image for the icon, not null. labelTD holds the text
     * for the label.
     */
    private final Element labelTD;

    private final Image iconImage;
    private final Imager imager;

    private DefaultHeader(final DisclosurePanelImages images, String text) {
      this(new Imager() {
        public Image makeImage() {
          return images.disclosurePanelClosed().createImage();
        }

        public void updateImage(boolean open, Image image) {
          if (open) {
            images.disclosurePanelOpen().applyTo(image);
          } else {
            images.disclosurePanelClosed().applyTo(image);
          }
        }
      }, text);
    }

    private DefaultHeader(Imager imager, String text) {
      this.imager = imager;
      iconImage = imager.makeImage();

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
      imageTD.setPropertyString("align", "center");
      imageTD.setPropertyString("valign", "middle");
      imageTD.getStyle().setProperty("width", iconImage.getWidth() + "px");

      DOM.appendChild(imageTD, iconImage.getElement());

      setText(text);

      addOpenHandler(this);
      addCloseHandler(this);
      setStyle();
    }

    private DefaultHeader(final ImageResource openImage,
        final ImageResource closedImage, String text) {
      this(new Imager() {
        public Image makeImage() {
          return new Image(closedImage);
        }

        public void updateImage(boolean open, Image image) {
          if (open) {
            image.setResource(openImage);
          } else {
            image.setResource(closedImage);
          }
        }
      }, text);
    }

    public final String getText() {
      return labelTD.getInnerText();
    }

    public final void onClose(CloseEvent<DisclosurePanel> event) {
      setStyle();
    }

    public final void onOpen(OpenEvent<DisclosurePanel> event) {
      setStyle();
    }

    public final void setText(String text) {
      labelTD.setInnerText(text);
    }

    private void setStyle() {
      imager.updateImage(isOpen, iconImage);
    }
  }

  private interface Imager {
    Image makeImage();

    void updateImage(boolean open, Image image);
  }

  /**
   * The duration of the animation.
   */
  private static final int ANIMATION_DURATION = 350;

  // Stylename constants.
  private static final String STYLENAME_DEFAULT = "gwt-DisclosurePanel";

  private static final String STYLENAME_SUFFIX_OPEN = "open";

  private static final String STYLENAME_SUFFIX_CLOSED = "closed";

  private static final String STYLENAME_HEADER = "header";

  private static final String STYLENAME_CONTENT = "content";

  /**
   * The {@link Animation} used to open and close the content.
   */
  private static ContentAnimation contentAnimation;

  /**
   * top level widget. The first child will be a reference to {@link #header}.
   * The second child will be a reference to {@link #contentWrapper}.
   */
  private final VerticalPanel mainPanel = new VerticalPanel();

  /**
   * The wrapper around the content widget.
   */
  private final SimplePanel contentWrapper = new SimplePanel();

  /**
   * holds the header widget.
   */
  private final ClickableHeader header = new ClickableHeader();

  private boolean isAnimationEnabled = false;

  private boolean isOpen = false;

  /**
   * Creates an empty DisclosurePanel that is initially closed.
   */
  public DisclosurePanel() {
    initWidget(mainPanel);
    mainPanel.add(header);
    mainPanel.add(contentWrapper);
    contentWrapper.getElement().getStyle().setProperty("padding", "0px");
    contentWrapper.getElement().getStyle().setProperty("overflow", "hidden");
    setStyleName(STYLENAME_DEFAULT);
    setContentDisplay(false);
  }

  /**
   * Creates a DisclosurePanel with the specified header text, an initial
   * open/close state and a bundle of images to be used in the default header
   * widget.
   * 
   * @param images a bundle that provides disclosure panel specific images
   * @param headerText the text to be displayed in the header
   * @param isOpen the initial open/close state of the content panel
   * 
   * @deprecated use
   *             {@link #DisclosurePanel(ImageResource, ImageResource, String)}
   *             and {@link #setOpen(boolean)}
   */
  @Deprecated
  public DisclosurePanel(DisclosurePanelImages images, String headerText,
      boolean isOpen) {
    this();
    setOpen(isOpen);
    setHeader(new DefaultHeader(images, headerText));
  }

  /**
   * Creates a DisclosurePanel with the specified header text, an initial
   * open/close state and a bundle of images to be used in the default header
   * widget.
   * 
   * @param openImage the open state image resource
   * @param closedImage the closed state image resource
   * @param headerText the text to be displayed in the header
   */
  public DisclosurePanel(ImageResource openImage, ImageResource closedImage,
      String headerText) {
    this();
    setHeader(new DefaultHeader(openImage, closedImage, headerText));
  }

  /**
   * Creates a DisclosurePanel that will be initially closed using the specified
   * text in the header.
   * 
   * @param headerText the text to be displayed in the header
   */
  public DisclosurePanel(String headerText) {
    this(DEFAULT_IMAGES.disclosurePanelOpen(),
        DEFAULT_IMAGES.disclosurePanelClosed(), headerText);
  }

  /**
   * Creates a DisclosurePanel with the specified header text and an initial
   * open/close state.
   * 
   * @param headerText the text to be displayed in the header
   * @param isOpen the initial open/close state of the content panel
   * @deprecated use {@link #DisclosurePanel(String)} and
   *             {@link #setOpen(boolean)}
   */
  @Deprecated
  public DisclosurePanel(String headerText, boolean isOpen) {
    this(DEFAULT_IMAGES.disclosurePanelOpen(),
        DEFAULT_IMAGES.disclosurePanelClosed(), headerText);
    this.setOpen(isOpen);
  }

  /**
   * Creates a DisclosurePanel that will be initially closed using a widget as
   * the header.
   * 
   * @param header the widget to be used as a header
   * @deprecated use {@link #DisclosurePanel()} and {@link #setHeader(Widget)}
   */
  @Deprecated
  public DisclosurePanel(Widget header) {
    this();
    setHeader(header);
  }

  /**
   * Creates a DisclosurePanel using a widget as the header and an initial
   * open/close state.
   * 
   * @param header the widget to be used as a header
   * @param isOpen the initial open/close state of the content panel
   * @deprecated use {@link #DisclosurePanel()}, {@link #setOpen(boolean)} and
   *             {@link #setHeader(Widget)} instead
   */
  @Deprecated
  public DisclosurePanel(Widget header, boolean isOpen) {
    this();
    setHeader(header);
    setOpen(isOpen);
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
   * Overloaded version for IsWidget.
   * 
   * @see #add(Widget)
   */
  public void add(IsWidget w) {
    this.add(asWidgetOrNull(w));
  }

  public HandlerRegistration addCloseHandler(
      CloseHandler<DisclosurePanel> handler) {
    return addHandler(handler, CloseEvent.getType());
  }

  /**
   * Attaches an event handler to the panel to receive {@link DisclosureEvent}
   * notification.
   * 
   * @param handler the handler to be added (should not be null)
   * @deprecated Use {@link DisclosurePanel#addOpenHandler(OpenHandler)} and
   *             {@link DisclosurePanel#addCloseHandler(CloseHandler)} instead
   */
  @Deprecated
  public void addEventHandler(final DisclosureHandler handler) {
    ListenerWrapper.WrappedOldDisclosureHandler.add(this, handler);
  }

  public HandlerRegistration addOpenHandler(OpenHandler<DisclosurePanel> handler) {
    return addHandler(handler, OpenEvent.getType());
  }

  public void clear() {
    setContent(null);
  }

  /**
   * Gets the widget that was previously set in {@link #setContent(Widget)}.
   * 
   * @return the panel's current content widget
   */
  public Widget getContent() {
    return contentWrapper.getWidget();
  }

  /**
   * Gets the widget that is currently being used as a header.
   * 
   * @return the widget currently being used as a header
   */
  public Widget getHeader() {
    return header.getWidget();
  }

  /**
   * Gets a {@link HasText} instance to provide access to the headers's text, if
   * the header widget does provide such access.
   * 
   * @return a reference to the header widget if it implements {@link HasText},
   *         <code>null</code> otherwise
   */
  public HasText getHeaderTextAccessor() {
    Widget widget = header.getWidget();
    return (widget instanceof HasText) ? (HasText) widget : null;
  }

  public boolean isAnimationEnabled() {
    return isAnimationEnabled;
  }

  /**
   * Determines whether the panel is open.
   * 
   * @return <code>true</code> if panel is in open state
   */
  public boolean isOpen() {
    return isOpen;
  }

  public Iterator<Widget> iterator() {
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
   * Overloaded version for IsWidget.
   * 
   * @see #remove(Widget)
   */
  public boolean remove(IsWidget w) {
    return this.remove(asWidgetOrNull(w));
  }

  /**
   * Removes an event handler from the panel.
   * 
   * @param handler the handler to be removed
   * @deprecated Use the {@link HandlerRegistration#removeHandler} method on the
   *             object returned by an add*Handler method instead
   */
  @Deprecated
  public void removeEventHandler(DisclosureHandler handler) {
    ListenerWrapper.WrappedOldDisclosureHandler.remove(this, handler);
  }

  public void setAnimationEnabled(boolean enable) {
    isAnimationEnabled = enable;
  }

  /**
   * Sets the content widget which can be opened and closed by this panel. If
   * there is a preexisting content widget, it will be detached.
   * 
   * @param content the widget to be used as the content panel
   */
  public void setContent(Widget content) {
    final Widget currentContent = getContent();

    // Remove existing content widget.
    if (currentContent != null) {
      contentWrapper.setWidget(null);
      currentContent.removeStyleName(STYLENAME_CONTENT);
    }

    // Add new content widget if != null.
    if (content != null) {
      contentWrapper.setWidget(content);
      content.addStyleName(STYLENAME_CONTENT);
      setContentDisplay(false);
    }
  }

  /**
   * Sets the widget used as the header for the panel.
   * 
   * @param headerWidget the widget to be used as the header
   */
  public void setHeader(Widget headerWidget) {
    header.setWidget(headerWidget);
  }

  /**
   * Changes the visible state of this <code>DisclosurePanel</code>.
   * 
   * @param isOpen <code>true</code> to open the panel, <code>false</code> to
   *          close
   */
  public void setOpen(boolean isOpen) {
    if (this.isOpen != isOpen) {
      this.isOpen = isOpen;
      setContentDisplay(true);
      fireEvent();
    }
  }

  /**
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-header = the clickable header.</li>
   * </ul>
   * 
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);
    header.ensureDebugId(baseID + "-header");
  }

  private void fireEvent() {
    if (isOpen) {
      OpenEvent.fire(this, this);
    } else {
      CloseEvent.fire(this, this);
    }
  }

  private void setContentDisplay(boolean animate) {
    if (isOpen) {
      removeStyleDependentName(STYLENAME_SUFFIX_CLOSED);
      addStyleDependentName(STYLENAME_SUFFIX_OPEN);
    } else {
      removeStyleDependentName(STYLENAME_SUFFIX_OPEN);
      addStyleDependentName(STYLENAME_SUFFIX_CLOSED);
    }

    if (getContent() != null) {
      if (contentAnimation == null) {
        contentAnimation = new ContentAnimation();
      }
      contentAnimation.setOpen(this, animate && isAnimationEnabled);
    }
  }
}
