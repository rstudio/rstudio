/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safecss.shared.SafeStyles;
import com.google.gwt.safecss.shared.SafeStylesBuilder;
import com.google.gwt.safecss.shared.SafeStylesUtils;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.client.SafeHtmlTemplates.Template;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

import java.util.Set;

/**
 * A {@link Cell} decorator that adds an icon to another {@link Cell}.
 * 
 * @param <C> the type that this Cell represents
 */
public class IconCellDecorator<C> implements Cell<C> {

  interface Template extends SafeHtmlTemplates {
    @Template("<div style=\"{0}position:relative;zoom:1;\">{1}<div>{2}</div></div>")
    SafeHtml outerDiv(SafeStyles padding, SafeHtml icon, SafeHtml cellContents);

    /**
     * The wrapper around the image vertically aligned to the bottom.
     */
    @Template("<div style=\"{0}position:absolute;bottom:0px;line-height:0px;\">{1}</div>")
    SafeHtml imageWrapperBottom(SafeStyles styles, SafeHtml image);

    /**
     * The wrapper around the image vertically aligned to the middle.
     */
    @Template("<div style=\"{0}position:absolute;top:50%;line-height:0px;\">{1}</div>")
    SafeHtml imageWrapperMiddle(SafeStyles styles, SafeHtml image);

    /**
     * The wrapper around the image vertically aligned to the top.
     */
    @Template("<div style=\"{0}position:absolute;top:0px;line-height:0px;\">{1}</div>")
    SafeHtml imageWrapperTop(SafeStyles styles, SafeHtml image);
  }

  /**
   * The default spacing between the icon and the text in pixels.
   */
  private static final int DEFAULT_SPACING = 6;

  private static Template template;

  private final Cell<C> cell;

  private final String direction = LocaleInfo.getCurrentLocale().isRTL()
      ? "right" : "left";

  private final SafeHtml iconHtml;

  private final int imageWidth;

  private final SafeStyles outerDivPadding;
  
  private final SafeHtml placeHolderHtml;

  /**
   * Construct a new {@link IconCellDecorator}. The icon and the content will be
   * middle aligned by default.
   * 
   * @param icon the icon to use
   * @param cell the cell to decorate
   */
  public IconCellDecorator(ImageResource icon, Cell<C> cell) {
    this(icon, cell, HasVerticalAlignment.ALIGN_MIDDLE, DEFAULT_SPACING);
  }

  /**
   * Construct a new {@link IconCellDecorator}.
   * 
   * @param icon the icon to use
   * @param cell the cell to decorate
   * @param valign the vertical alignment attribute of the contents
   * @param spacing the pixel space between the icon and the cell
   */
  public IconCellDecorator(ImageResource icon, Cell<C> cell, VerticalAlignmentConstant valign,
      int spacing) {
    if (template == null) {
      template = GWT.create(Template.class);
    }
    this.cell = cell;
    this.iconHtml = getImageHtml(icon, valign, false);
    this.imageWidth = icon.getWidth() + spacing;
    this.placeHolderHtml = getImageHtml(icon, valign, true);
    this.outerDivPadding =
        SafeStylesUtils.fromTrustedString("padding-" + direction + ": " + imageWidth + "px;");
  }

  public boolean dependsOnSelection() {
    return cell.dependsOnSelection();
  }

  public Set<String> getConsumedEvents() {
    return cell.getConsumedEvents();
  }

  public boolean handlesSelection() {
    return cell.handlesSelection();
  }

  public boolean isEditing(Context context, Element parent, C value) {
    return cell.isEditing(context, getCellParent(parent), value);
  }

  public void onBrowserEvent(Context context, Element parent, C value,
      NativeEvent event, ValueUpdater<C> valueUpdater) {
    cell.onBrowserEvent(context, getCellParent(parent), value, event,
        valueUpdater);
  }

  public void render(Context context, C value, SafeHtmlBuilder sb) {
    SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
    cell.render(context, value, cellBuilder);
    sb.append(template.outerDiv(outerDivPadding, isIconUsed(value) ? getIconHtml(value)
        : placeHolderHtml, cellBuilder.toSafeHtml()));
  }

  public boolean resetFocus(Context context, Element parent, C value) {
    return cell.resetFocus(context, getCellParent(parent), value);
  }

  public void setValue(Context context, Element parent, C value) {
    cell.setValue(context, getCellParent(parent), value);
  }

  /**
   * Get the safe HTML string that represents the icon. Override this method to
   * change the icon based on the value.
   * 
   * @param value the value being rendered
   * @return the HTML string that represents the icon
   */
  protected SafeHtml getIconHtml(C value) {
    return iconHtml;
  }

  /**
   * Check if the icon should be used for the value. If the icon should not be
   * used, a placeholder of the same size will be used instead. The default
   * implementations returns true.
   * 
   * @param value the value being rendered
   * @return true to use the icon, false to use a placeholder
   */
  protected boolean isIconUsed(C value) {
    return true;
  }

  /**
   * Get the HTML representation of an image. Visible for testing.
   * 
   * @param res the {@link ImageResource} to render as HTML
   * @param valign the vertical alignment
   * @param isPlaceholder if true, do not include the background image
   * @return the rendered HTML
   */
  SafeHtml getImageHtml(ImageResource res, VerticalAlignmentConstant valign, boolean isPlaceholder) {
    // Get the HTML for the image.
    SafeHtml image;
    if (isPlaceholder) {
      image = SafeHtmlUtils.fromSafeConstant("<div></div>");
    } else {
      AbstractImagePrototype proto = AbstractImagePrototype.create(res);
      image = SafeHtmlUtils.fromTrustedString(proto.getHTML());
    }

    // Create the wrapper based on the vertical alignment.
    SafeStylesBuilder cssStyles =
        new SafeStylesBuilder().appendTrustedString(direction + ":0px;");
    if (HasVerticalAlignment.ALIGN_TOP == valign) {
      return template.imageWrapperTop(cssStyles.toSafeStyles(), image);
    } else if (HasVerticalAlignment.ALIGN_BOTTOM == valign) {
      return template.imageWrapperBottom(cssStyles.toSafeStyles(), image);
    } else {
      int halfHeight = (int) Math.round(res.getHeight() / 2.0);
      cssStyles.appendTrustedString("margin-top:-" + halfHeight + "px;");
      return template.imageWrapperMiddle(cssStyles.toSafeStyles(), image);
    }
  }

  /**
   * Get the parent element of the decorated cell.
   * 
   * @param parent the parent of this cell
   * @return the decorated cell's parent
   */
  private Element getCellParent(Element parent) {
    return parent.getFirstChildElement().getChild(1).cast();
  }
}
