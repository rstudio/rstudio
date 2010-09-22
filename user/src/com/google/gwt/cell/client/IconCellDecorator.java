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
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment.VerticalAlignmentConstant;

import java.util.Set;

/**
 * A {@link Cell} decorator that adds an icon to another {@link Cell}.
 *
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 *
 * @param <C> the type that this Cell represents
 */
public class IconCellDecorator<C> implements Cell<C> {

  interface Template extends SafeHtmlTemplates {
    @Template("<div style=\"position:relative;padding-{0}:{1}px;\">{2}<div>{3}</div></div>")
    SafeHtml outerDiv(String direction, int width, SafeHtml icon,
        SafeHtml cellContents);

    @Template("<div style=\"position:absolute;{0}:0px;top:0px;height:100%;width:{1}px;\"></div>")
    SafeHtml imagePlaceholder(String direction, int width);
  }

  private static Template template;

  private final Cell<C> cell;

  private final String direction = LocaleInfo.getCurrentLocale().isRTL()
      ? "right" : "left";

  private final SafeHtml iconHtml;

  private final int imageWidth;

  private final SafeHtml placeHolderHtml;

  /**
   * Construct a new {@link IconCellDecorator}. The icon and the content will be
   * middle aligned by default.
   *
   * @param icon the icon to use
   * @param cell the cell to decorate
   */
  public IconCellDecorator(ImageResource icon, Cell<C> cell) {
    this(icon, cell, HasVerticalAlignment.ALIGN_MIDDLE, 6);
  }

  /**
   * Construct a new {@link IconCellDecorator}.
   *
   * @param icon the icon to use
   * @param cell the cell to decorate
   * @param valign the vertical alignment attribute of the contents
   * @param spacing the pixel space between the icon and the cell
   */
  public IconCellDecorator(ImageResource icon, Cell<C> cell,
      VerticalAlignmentConstant valign, int spacing) {
    if (template == null) {
      template = GWT.create(Template.class);
    }
    this.cell = cell;
    this.iconHtml = getImageHtml(icon, valign, false);
    this.imageWidth = icon.getWidth() + 6;
    this.placeHolderHtml = getImageHtml(icon, valign, true);
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

  public boolean isEditing(Element element, C value, Object key) {
    return cell.isEditing(element, value, key);
  }

  public void onBrowserEvent(Element parent, C value, Object key,
      NativeEvent event, ValueUpdater<C> valueUpdater) {
    cell.onBrowserEvent(getCellParent(parent), value, key, event, valueUpdater);
  }

  public void render(C value, Object key, SafeHtmlBuilder sb) {
    SafeHtmlBuilder cellBuilder = new SafeHtmlBuilder();
    cell.render(value, key, cellBuilder);

    sb.append(template.outerDiv(direction, imageWidth, isIconUsed(value)
        ? getIconHtml(value) : placeHolderHtml, cellBuilder.toSafeHtml()));
  }

  public boolean resetFocus(Element parent, C value, Object key) {
    return cell.resetFocus(getCellParent(parent), value, key);
  }

  public void setValue(Element parent, C value, Object key) {
    cell.setValue(getCellParent(parent), value, key);
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
  // TODO(jlabanca): Move this to a Utility class.
  SafeHtml getImageHtml(ImageResource res, VerticalAlignmentConstant valign,
      boolean isPlaceholder) {
    if (isPlaceholder) {
      return template.imagePlaceholder(direction, res.getWidth());
    } else {
      String vert = valign == HasVerticalAlignment.ALIGN_MIDDLE ? "center"
          : valign.getVerticalAlignString();
      // Templates are having problems with url('data:image/png;base64,...')
      // CHECKSTYLE_OFF
      return SafeHtmlUtils.fromTrustedString("<div style=\"position:absolute;"
          + direction + ":0px;top:0px;height:100%;width:" + res.getWidth()
          + "px;background:url('" + res.getURL() + "') no-repeat scroll "
          + SafeHtmlUtils.htmlEscape(vert) // for safety
          + " center transparent;\"></div>");
      // CHECKSTYLE_ON
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
