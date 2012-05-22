/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.cell.client;

import static com.google.gwt.dom.client.BrowserEvents.CLICK;
import static com.google.gwt.dom.client.BrowserEvents.KEYDOWN;
import static com.google.gwt.dom.client.BrowserEvents.MOUSEDOWN;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CommonResources;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.ImportedWithPrefix;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HasEnabled;

/**
 * Base class for button Cells.
 * 
 * @param <C> the type that this Cell represents
 */
public class ButtonCellBase<C> extends AbstractCell<C> implements IsCollapsible, HasEnabled {

  /**
   * The appearance used to render this Cell.
   * 
   * @param <C> the type that this Cell represents
   */
  public interface Appearance<C> {

    /**
     * Called when the user pushes the button down.
     * 
     * @param parent the parent Element
     */
    void onPush(Element parent);

    /**
     * Called when the user releases the button from being pushed.
     * 
     * @param parent the parent Element
     */
    void onUnpush(Element parent);

    /**
     * Render the button and its contents.
     * 
     * @param cell the cell that is being rendered
     * @param context the {@link Context} of the cell
     * @param value the value that generated the content
     * @param sb the {@link SafeHtmlBuilder} to render into
     */
    void render(ButtonCellBase<C> cell, Context context, C value, SafeHtmlBuilder sb);

    /**
     * Explicitly focus/unfocus this cell.
     * 
     * @param parent the parent element
     * @param focused whether this cell should take focus or release it
     */
    void setFocus(Element parent, boolean focused);
  }

  /**
   * The decoration applied to the button.
   * 
   * <dl>
   * <dt>DEFAULT</dt>
   * <dd>A general button used in any context.</dd>
   * <dt>PRIMARY</dt>
   * <dd>A primary button that stands out against other buttons.</dd>
   * <dt>NEGATIVE</dt>
   * <dd>A button that results in a negative action, such as delete or cancel</dd>
   * </dl>
   */
  public static enum Decoration {
    DEFAULT, PRIMARY, NEGATIVE;
  }

  /**
   * The default implementation of the {@link ButtonCellBase.Appearance}.
   * 
   * @param <C> the type that this Cell represents
   */
  public static class DefaultAppearance<C> implements Appearance<C> {

    /**
     * The resources used by this appearance.
     */
    public interface Resources extends ClientBundle {

      /**
       * The background image applied to the button.
       */
      @ImageOptions(repeatStyle = RepeatStyle.Horizontal, flipRtl = true)
      ImageResource buttonCellBaseBackground();

      @Source(Style.DEFAULT_CSS)
      Style buttonCellBaseStyle();
    }

    /**
     * The Styles used by this appearance.
     */
    @ImportedWithPrefix("gwt-ButtonCellBase")
    public interface Style extends CssResource {
      String DEFAULT_CSS = "com/google/gwt/cell/client/ButtonCellBase.css";

      /**
       * Applied to the button.
       */
      String buttonCellBase();

      /**
       * Applied to the button when it has a collapsed left side.
       */
      String buttonCellBaseCollapseLeft();

      /**
       * Applied to the button when it has a collapsed right side.
       */
      String buttonCellBaseCollapseRight();

      /**
       * Applied to default buttons.
       */
      String buttonCellBaseDefault();

      /**
       * Applied to negative buttons.
       */
      String buttonCellBaseNegative();

      /**
       * Applied to primary buttons.
       */
      String buttonCellBasePrimary();

      /**
       * Applied to the button when being pushed.
       */
      String buttonCellBasePushing();
    }

    /**
     * The templates used by this appearance.
     */
    interface Template extends SafeHtmlTemplates {
      /**
       * Positions the icon next to the text.
       * 
       * NOTE: zoom:0 is a workaround for an IE7 bug where the button contents
       * wrap even when they do not need to.
       */
      @SafeHtmlTemplates.Template("<div class=\"{0}\""
          + " style=\"{1}position:relative;zoom:0;\">{2}{3}</div>")
      SafeHtml iconContentLayout(
          String classes, SafeStyles styles, SafeHtml icon, SafeHtml cellContents);

      /**
       * The wrapper around the icon that aligns it vertically with the text.
       */
      @SafeHtmlTemplates.Template("<div style=\"{0}position:absolute;top:50%;line-height:0px;\">"
          + "{1}</div>")
      SafeHtml iconWrapper(SafeStyles styles, SafeHtml image);
    }

    private static final int DEFAULT_ICON_PADDING = 3;
    private static Resources defaultResources;
    private static Template template;

    private static Resources getDefaultResources() {
      if (defaultResources == null) {
        defaultResources = GWT.create(Resources.class);
      }
      return defaultResources;
    }

    private SafeHtml iconSafeHtml = SafeHtmlUtils.EMPTY_SAFE_HTML;
    private ImageResource lastIcon;
    private final SafeHtmlRenderer<C> renderer;
    private final Style style;

    /**
     * Construct a new {@link ButtonCellBase.DefaultAppearance} using the default styles.
     * 
     * @param renderer the {@link SafeHtmlRenderer} used to render the contents
     */
    public DefaultAppearance(SafeHtmlRenderer<C> renderer) {
      this(renderer, getDefaultResources());
    }

    /**
     * Construct a new {@link ButtonCellBase.DefaultAppearance} using the specified resources.
     * 
     * @param renderer the {@link SafeHtmlRenderer} used to render the contents
     * @param resources the resources and styles to apply to the button
     */
    public DefaultAppearance(SafeHtmlRenderer<C> renderer, Resources resources) {
      this.renderer = renderer;
      this.style = resources.buttonCellBaseStyle();
      this.style.ensureInjected();
      if (template == null) {
        template = GWT.create(Template.class);
      }
    }

    /**
     * Return the {@link SafeHtmlRenderer} used by this Appearance to render the
     * contents of the button.
     * 
     * @return a {@link SafeHtmlRenderer} instance
     */
    public SafeHtmlRenderer<C> getRenderer() {
      return renderer;
    }

    @Override
    public void onPush(Element parent) {
      parent.getFirstChildElement().addClassName(style.buttonCellBasePushing());
    }

    @Override
    public void onUnpush(Element parent) {
      parent.getFirstChildElement().removeClassName(style.buttonCellBasePushing());
    }

    @Override
    public void render(ButtonCellBase<C> cell, Context context, C value, SafeHtmlBuilder sb) {
      // Determine the classes from the state of the button.
      SafeHtmlBuilder classes = new SafeHtmlBuilder();
      classes.appendEscaped(style.buttonCellBase());
      Decoration decoration = cell.getDecoration();
      if (decoration == Decoration.PRIMARY) {
        classes.appendEscaped(" " + style.buttonCellBasePrimary());
      } else if (decoration == Decoration.NEGATIVE) {
        classes.appendEscaped(" " + style.buttonCellBaseNegative());
      } else {
        classes.appendEscaped(" " + style.buttonCellBaseDefault());
      }
      if (cell.isCollapseLeft()) {
        classes.appendEscaped(" " + style.buttonCellBaseCollapseLeft());
      }
      if (cell.isCollapseRight()) {
        classes.appendEscaped(" " + style.buttonCellBaseCollapseRight());
      }

      // Update the cached HTML string used for the icon if the icon changes.
      ImageResource icon = cell.getIcon();
      if (icon != lastIcon) {
        if (icon == null) {
          iconSafeHtml = SafeHtmlUtils.EMPTY_SAFE_HTML;
        } else {
          AbstractImagePrototype proto = AbstractImagePrototype.create(icon);
          SafeHtml iconOnly = SafeHtmlUtils.fromTrustedString(proto.getHTML());
          int halfHeight = (int) Math.round(icon.getHeight() / 2.0);
          SafeStylesBuilder styles = new SafeStylesBuilder();
          styles.marginTop(-halfHeight, Unit.PX);
          if (LocaleInfo.getCurrentLocale().isRTL()) {
            styles.right(0, Unit.PX);
          } else {
            styles.left(0, Unit.PX);
          }
          iconSafeHtml = template.iconWrapper(styles.toSafeStyles(), iconOnly);
        }
      }

      // Figure out the attributes.
      char accessKey = cell.getAccessKey();
      StringBuilder attributes = new StringBuilder();
      if (!cell.isEnabled()) {
        attributes.append("disabled=disabled ");
      }
      if (accessKey != 0) {
        attributes.append("accessKey=\"").append(SafeHtmlUtils.htmlEscape("" + accessKey));
        attributes.append("\" ");
      }

      // Create the button.
      SafeStylesBuilder styles = new SafeStylesBuilder();
      int iconWidth = (icon == null) ? 0 : icon.getWidth();
      int iconPadding = iconWidth + DEFAULT_ICON_PADDING;
      if (LocaleInfo.getCurrentLocale().isRTL()) {
        styles.paddingRight(iconPadding, Unit.PX);
      } else {
        styles.paddingLeft(iconPadding, Unit.PX);
      }
      SafeHtml safeValue = renderer.render(value);
      SafeHtml content = template.iconContentLayout(
          CommonResources.getInlineBlockStyle(), styles.toSafeStyles(), iconSafeHtml, safeValue);
      int tabIndex = cell.getTabIndex();
      StringBuilder openTag = new StringBuilder();
      openTag.append("<button type=\"button\"");
      openTag.append(" class=\"" + classes.toSafeHtml().asString() + "\"");
      openTag.append(" tabindex=\"" + tabIndex + "\" ");
      openTag.append(attributes.toString()).append(">");
      sb.appendHtmlConstant(openTag.toString());
      sb.append(content);
      sb.appendHtmlConstant("</button>");
    }

    @Override
    public void setFocus(Element parent, boolean focused) {
      Element focusable = parent.getFirstChildElement().cast();
      if (focused) {
        focusable.focus();
      } else {
        focusable.blur();
      }
    }
  }

  /**
   * The {@link NativePreviewHandler} used to unpush a button onmouseup, even if
   * the event doesn't occur over the button.
   */
  private class UnpushHandler implements NativePreviewHandler {

    private final Element parent;
    private final HandlerRegistration reg;

    public UnpushHandler(Element parent) {
      this.parent = parent;
      this.reg = Event.addNativePreviewHandler(this);
    }

    @Override
    public void onPreviewNativeEvent(NativePreviewEvent event) {
      if (BrowserEvents.MOUSEUP.equals(event.getNativeEvent().getType())) {
        // Unregister self.
        reg.removeHandler();

        // Unpush the element.
        appearance.onUnpush(parent);
      }
    }
  }

  private char accessKey;
  private final Appearance<C> appearance;
  private Decoration decoration = Decoration.DEFAULT;
  private ImageResource icon;
  private boolean isCollapsedLeft;
  private boolean isCollapsedRight;
  private boolean isEnabled = true;

  /**
   * The tab index applied to the buttons. If the button is used in a list of
   * table, a tab index of -1 should be used so it doesn't interrupt the tab
   * sequence.
   */
  private int tabIndex = -1;

  /**
   * Construct a new {@link ButtonCellBase} using the specified
   * {@link Appearance} to render the contents.
   * 
   * @param appearance the appearance of the cell
   */
  public ButtonCellBase(Appearance<C> appearance) {
    super(CLICK, KEYDOWN, MOUSEDOWN);
    this.appearance = appearance;
  }

  /**
   * Get the access key.
   * 
   * @return the access key, or 0 if one is not defined
   */
  public char getAccessKey() {
    return accessKey;
  }

  /**
   * Get the decoration style of the button.
   */
  public Decoration getDecoration() {
    return decoration;
  }

  /**
   * Get the icon displayed next to the button text.
   * 
   * @return the icon resource
   */
  public ImageResource getIcon() {
    return icon;
  }

  /**
   * Return the tab index that is given to all rendered cells.
   * 
   * @return the tab index
   */
  public int getTabIndex() {
    return tabIndex;
  }

  @Override
  public boolean isCollapseLeft() {
    return this.isCollapsedLeft;
  }

  @Override
  public boolean isCollapseRight() {
    return this.isCollapsedRight;
  }

  @Override
  public boolean isEnabled() {
    return isEnabled;
  }

  @Override
  public void onBrowserEvent(Context context, Element parent, C value, NativeEvent event,
      ValueUpdater<C> valueUpdater) {
    // Ignore all events if disabled.
    if (!isEnabled()) {
      return;
    }

    // Let AbstractCell handle the enter key.
    super.onBrowserEvent(context, parent, value, event, valueUpdater);

    // Ignore events that occur outside of the button element.
    Element target = event.getEventTarget().cast();
    if (!parent.getFirstChildElement().isOrHasChild(target)) {
      return;
    }

    String eventType = event.getType();
    if (CLICK.equals(eventType)) {
      // Click the button.
      onEnterKeyDown(context, parent, value, event, valueUpdater);
    } else if (MOUSEDOWN.equals(eventType)) {
      // Push.
      appearance.onPush(parent);

      /*
       * Add a preview handler to unpush the button onmouseup or onblur, even if
       * the event doesn't occur over the button.
       */
      new UnpushHandler(parent);

      // We will be handling all user visual feedback manually. Prevent browser
      // doing default handling (i.e. double-click highlights button text).
      event.preventDefault();
    }
  }

  @Override
  public void render(Context context, C value, SafeHtmlBuilder sb) {
    appearance.render(this, context, value, sb);
  }

  /**
   * Sets the cell's 'access key'. This key is used (in conjunction with a
   * browser-specific modifier key) to automatically focus the cell.
   * 
   * <p>
   * The change takes effect the next time the Cell is rendered.
   * 
   * @param key the cell's access key
   */
  public void setAccessKey(char key) {
    this.accessKey = key;
  }

  /**
   * {@inheritDoc}
   * 
   * <p>
   * The change takes effect the next time the Cell is rendered.
   */
  @Override
  public void setCollapseLeft(boolean isCollapsed) {
    this.isCollapsedLeft = isCollapsed;
  }

  /**
   * {@inheritDoc}
   * 
   * <p>
   * The change takes effect the next time the Cell is rendered.
   */
  @Override
  public void setCollapseRight(boolean isCollapsed) {
    this.isCollapsedRight = isCollapsed;
  }

  /**
   * Set the {@link Decoration} of the button.
   * 
   * <p>
   * This change takes effect the next time the cell is rendered.
   * 
   * @param decoration the button decoration
   */
  public void setDecoration(Decoration decoration) {
    this.decoration = decoration;
  }

  /**
   * {@inheritDoc}
   * 
   * <p>
   * The change takes effect the next time the Cell is rendered.
   */
  @Override
  public void setEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  /**
   * Explicitly focus/unfocus this cell. Only one UI component can have focus at
   * a time, and the component that does will receive all keyboard events.
   * 
   * @param parent the parent element
   * @param focused whether this cell should take focus or release it
   */
  public void setFocus(Element parent, boolean focused) {
    appearance.setFocus(parent, focused);
  }

  /**
   * Set the icon to display next to the button text.
   * 
   * @param icon the icon resource, or null not to show an icon
   */
  public void setIcon(ImageResource icon) {
    this.icon = icon;
  }

  /**
   * Set the tab index to apply to the button. By default, the tab index is set
   * to -1 so that the button does not interrupt the tab chain when in a table
   * or list. The change takes effect the next time the Cell is rendered.
   * 
   * @param tabIndex the tab index
   */
  public void setTabIndex(int tabIndex) {
    this.tabIndex = tabIndex;
  }

  @Override
  protected void onEnterKeyDown(Context context, Element parent, C value, NativeEvent event,
      ValueUpdater<C> valueUpdater) {
    if (valueUpdater != null) {
      valueUpdater.update(value);
    }
  }
}
