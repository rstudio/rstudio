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
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

/**
 * An {@link AbstractCell} used to render an image. A loading indicator is used
 * until the image is fully loaded. The String value is the url of the image.
 */
public class ImageLoadingCell extends AbstractCell<String> {

  /**
   * The renderers used by this cell.
   */
  public interface Renderers {

    /**
     * Get the renderer used to render an error message when the image does not
     * load. By default, the broken image is rendered.
     *
     * @return the {@link SafeHtmlRenderer} used when the image doesn't load
     */
    SafeHtmlRenderer<String> getErrorRenderer();

    /**
     * Get the renderer used to render the image. This renderer must render an
     * <code>img</code> element, which triggers the <code>load</code> or <code>
     * error</code> event that this cell handles.
     *
     * @return the {@link SafeHtmlRenderer} used to render the image
     */
    SafeHtmlRenderer<String> getImageRenderer();

    /**
     * Get the renderer used to render a loading message. By default, an
     * animated loading icon is rendered.
     *
     * @return the {@link SafeHtmlRenderer} used to render the loading html
     */
    SafeHtmlRenderer<String> getLoadingRenderer();
  }

  interface Template extends SafeHtmlTemplates {
    @Template("<div style='height:0px;width:0px;overflow:hidden;'>{0}</div>")
    SafeHtml image(SafeHtml imageHtml);

    @Template("<img src=\"{0}\"/>")
    SafeHtml img(String url);

    @Template("<div>{0}</div>")
    SafeHtml loading(SafeHtml loadingHtml);
  }

  private static Template template;

  /**
   * The default {@link SafeHtmlRenderer SafeHtmlRenderers}.
   */
  public static class DefaultRenderers implements Renderers {

    private static SafeHtmlRenderer<String> IMAGE_RENDERER;
    private static SafeHtmlRenderer<String> LOADING_RENDERER;

    public DefaultRenderers() {
      if (IMAGE_RENDERER == null) {
        IMAGE_RENDERER = new AbstractSafeHtmlRenderer<String>() {
          public SafeHtml render(String object) {
            return template.img(object);
          }
        };
      }
      if (LOADING_RENDERER == null) {
        Resources resources = GWT.create(Resources.class);
        ImageResource res = resources.loading();
        final SafeHtml loadingHtml = AbstractImagePrototype.create(res).getSafeHtml();
        LOADING_RENDERER = new AbstractSafeHtmlRenderer<String>() {
          public SafeHtml render(String object) {
            return loadingHtml;
          }
        };
      }
    }

    /**
     * Returns the renderer for a broken image.
     *
     * @return a {@link SafeHtmlRenderer SafeHtmlRenderer<String>} instance
     */
    public SafeHtmlRenderer<String> getErrorRenderer() {
      // Show the broken image on error.
      return getImageRenderer();
    }

    /**
     * Returns the renderer for an image.
     *
     * @return a {@link SafeHtmlRenderer SafeHtmlRenderer<String>} instance
     */
    public SafeHtmlRenderer<String> getImageRenderer() {
      return IMAGE_RENDERER;
    }

    /**
     * Returns the renderer for a loading image.
     *
     * @return a {@link SafeHtmlRenderer SafeHtmlRenderer<String>} instance
     */
    public SafeHtmlRenderer<String> getLoadingRenderer() {
      return LOADING_RENDERER;
    }
  }

  /**
   * The images used by the {@link DefaultRenderers}.
   */
  interface Resources extends ClientBundle {
    ImageResource loading();
  }

  private final SafeHtmlRenderer<String> errorRenderer;
  private final SafeHtmlRenderer<String> imageRenderer;
  private final SafeHtmlRenderer<String> loadingRenderer;

  /**
   * <p>
   * Construct an {@link ImageResourceCell} using the {@link DefaultRenderers}.
   * </p>
   * <p>
   * The {@link DefaultRenderers} will be constructed using
   * {@link GWT#create(Class)}, which allows you to replace the class using a
   * deferred binding.
   * </p>
   */
  public ImageLoadingCell() {
    this(GWT.<DefaultRenderers> create(DefaultRenderers.class));
  }

  /**
   * Construct an {@link ImageResourceCell} using the specified
   * {@link SafeHtmlRenderer SafeHtmlRenderers}.
   * 
   * @param renderers an instance of {@link ImageLoadingCell.Renderers Renderers}
   */
  public ImageLoadingCell(Renderers renderers) {
    super(BrowserEvents.LOAD, BrowserEvents.ERROR);
    if (template == null) {
      template = GWT.create(Template.class);
    }
    this.errorRenderer = renderers.getErrorRenderer();
    this.imageRenderer = renderers.getImageRenderer();
    this.loadingRenderer = renderers.getLoadingRenderer();
  }

  @Override
  public void onBrowserEvent(Context context, Element parent, String value,
      NativeEvent event, ValueUpdater<String> valueUpdater) {
    // The loading indicator can fire its own load or error event, so we check
    // that the event actually occurred on the main image.
    String type = event.getType();
    if (BrowserEvents.LOAD.equals(type) && eventOccurredOnImage(event, parent)) {
      // Remove the loading indicator.
      parent.getFirstChildElement().getStyle().setDisplay(Display.NONE);

      // Show the image.
      Element imgWrapper = parent.getChild(1).cast();
      imgWrapper.getStyle().setProperty("height", "auto");
      imgWrapper.getStyle().setProperty("width", "auto");
      imgWrapper.getStyle().setProperty("overflow", "auto");
    } else if (BrowserEvents.ERROR.equals(type) && eventOccurredOnImage(event, parent)) {
      // Replace the loading indicator with an error message.
      parent.getFirstChildElement().setInnerHTML(
          errorRenderer.render(value).asString());
    }
  }

  @Override
  public void render(Context context, String value, SafeHtmlBuilder sb) {
    // We can't use ViewData because we don't know the caching policy of the
    // browser. The browser may fetch the image every time we render.
    if (value != null) {
      sb.append(template.loading(loadingRenderer.render(value)));
      sb.append(template.image(imageRenderer.render(value)));
    }
  }

  /**
   * Check whether or not an event occurred within the wrapper around the image
   * element.
   *
   * @param event the event
   * @param parent the parent element
   * @return true if the event targets the image
   */
  private boolean eventOccurredOnImage(NativeEvent event, Element parent) {
    EventTarget eventTarget = event.getEventTarget();
    if (!Element.is(eventTarget)) {
      return false;
    }
    Element target = eventTarget.cast();

    // Make sure the target occurred within the div around the image.
    Element imgWrapper = parent.getFirstChildElement().getNextSiblingElement();
    return imgWrapper.isOrHasChild(target);
  }
}
