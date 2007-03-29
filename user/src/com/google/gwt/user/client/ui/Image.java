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
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.impl.ClippedImageImpl;
import com.google.gwt.core.client.GWT;

import java.util.HashMap;

/**
 * A widget that displays the image at a given URL. The image can be in
 * 'unclipped' mode (the default) or 'clipped' mode. In clipped mode, a viewport
 * is overlaid on top of the image so that a subset of the image will be
 * displayed. In unclipped mode, there is no viewport - the entire image will be
 * visible. Whether an image is in clipped or unclipped mode depends on how the
 * image is constructed, and how it is transformed after construction. Methods
 * will operate differently depending on the mode that the image is in. These
 * differences are detailed in the documentation for each method.
 * 
 * <p>
 * If an image transitions between clipped mode and unclipped mode, any
 * {@link Element}-specific attributes added by the user (including style
 * attributes, style names, and style modifiers), except for event listeners,
 * will be lost.
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class="css">
 * <li>.gwt-Image { }</li>
 * </ul>
 * 
 * Tranformations between clipped and unclipped state will result in a loss of
 * any style names that were set/added; the only style names that are preserved
 * are those that are mentioned in the static CSS style rules. Due to
 * browser-specific HTML constructions needed to achieve the clipping effect,
 * certain CSS attributes, such as padding and background, may not work as
 * expected when an image is in clipped mode. These limitations can usually be
 * easily worked around by encapsulating the image in a container widget that
 * can itself be styled.
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.ImageExample}
 * </p>
 */
public class Image extends Widget implements SourcesClickEvents,
    SourcesMouseEvents, SourcesLoadEvents {

  /**
   * Abstract class which is used to hold the state associated with an image
   * object.
   */
  private abstract static class State {

    public abstract int getHeight(Image image);

    public abstract int getOriginLeft();

    public abstract int getOriginTop();

    public abstract String getUrl(Image image);

    public abstract int getWidth(Image image);

    public abstract void setUrl(Image image, String url);

    public abstract void setUrlAndVisibleRect(Image image, String url,
        int left, int top, int width, int height);

    public abstract void setVisibleRect(Image image, int left, int top,
        int width, int height);

    // This method is used only by unit tests.
    protected abstract String getStateName();
  }

  /**
   * Implementation of behaviors associated with the unclipped state of an
   * image.
   */
  private static class UnclippedState extends State {

    UnclippedState(Image image) {
      image.setElement(DOM.createImg());
      image.sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS | Event.ONLOAD
          | Event.ONERROR);
    }

    UnclippedState(Image image, String url) {
      this(image);
      setUrl(image, url);
    }

    public int getHeight(Image image) {
      return DOM.getIntAttribute(image.getElement(), "height");
    }

    public int getOriginLeft() {
      return 0;
    }

    public int getOriginTop() {
      return 0;
    }

    public String getUrl(Image image) {
      return DOM.getAttribute(image.getElement(), "src");
    }

    public int getWidth(Image image) {
      return DOM.getIntAttribute(image.getElement(), "width");
    }

    public void setUrl(Image image, String url) {
      DOM.setAttribute(image.getElement(), "src", url);
    }

    public void setUrlAndVisibleRect(Image image, String url, int left,
        int top, int width, int height) {
      image.changeState(new ClippedState(image, url, left, top, width, height));
    }

    public void setVisibleRect(Image image, int left, int top, int width,
        int height) {
      image.changeState(new ClippedState(image, getUrl(image), left, top,
          width, height));
    }

    // This method is used only by unit tests.
    protected String getStateName() {
      return "unclipped";
    }
  }

  /**
   * Implementation of behaviors associated with the clipped state of an image.
   */
  private static class ClippedState extends State {

    private static final ClippedImageImpl impl = (ClippedImageImpl) GWT.create(ClippedImageImpl.class);

    private int left = 0;
    private int top = 0;
    private int width = 0;
    private int height = 0;
    private String url = null;

    ClippedState(Image image, String url, int left, int top, int width,
        int height) {
      this.left = left;
      this.top = top;
      this.width = width;
      this.height = height;
      this.url = url;
      image.setElement(impl.createStructure(url, left, top, width, height));
      image.sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS);
      fireSyntheticLoadEvent(image);
    }

    private void fireSyntheticLoadEvent(final Image image) {
      /*
       * We need to synthesize a load event, because the native events that are
       * fired would correspond to the loading of clear.cache.gif, which is
       * incorrect. A native event would not even fire in Internet Explorer,
       * because the root element is a wrapper element around the <img> element.
       * Since we are synthesizing a load event, we do not need to sink the
       * onload event.
       * 
       * We use a deferred command here to simulate the native version of the
       * load event as closely as possible. In the native event case, it is
       * unlikely that a second load event would occur while you are in the load
       * event handler.
       */
      DeferredCommand.add(new Command() {
        public void execute() {
          if (image.loadListeners != null) {
            image.loadListeners.fireLoad(image);
          }
        }
      });
    }

    public int getHeight(Image image) {
      return height;
    }

    public int getOriginLeft() {
      return left;
    }

    public int getOriginTop() {
      return top;
    }

    public String getUrl(Image image) {
      return url;
    }

    public int getWidth(Image image) {
      return width;
    }

    public void setUrl(Image image, String url) {
      image.changeState(new UnclippedState(image, url));
    }

    public void setUrlAndVisibleRect(Image image, String url, int left,
        int top, int width, int height) {
      if (!url.equals(url) || this.left != left || this.top != top
          || this.width != width || this.height != height) {

        this.url = url;
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;

        impl.adjust(image.getElement(), url, left, top, width, height);
        fireSyntheticLoadEvent(image);
      }
    }

    public void setVisibleRect(Image image, int left, int top, int width,
        int height) {
      /*
       * In the event that the clipping rectangle has not changed, we want to
       * skip all of the work required with a getImpl().adjust, and we do not
       * want to fire a load event.
       */
      if (this.left != left || this.top != top || this.width != width
          || this.height != height) {

        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;

        impl.adjust(image.getElement(), url, left, top, width, height);
        fireSyntheticLoadEvent(image);
      }
    }

    /* This method is used only by unit tests */
    protected String getStateName() {
      return "clipped";
    }
  }

  /**
   * This map is used to store prefetched images. If a reference is not kept to
   * the prefetched image objects, they can get garbage collected, which
   * sometimes keeps them from getting fully fetched.
   */
  private static HashMap prefetchImages = new HashMap();

  /**
   * Causes the browser to pre-fetch the image at a given URL.
   * 
   * @param url the URL of the image to be prefetched
   */
  public static void prefetch(String url) {
    Element img = DOM.createImg();
    DOM.setAttribute(img, "src", url);
    prefetchImages.put(url, img);
  }

  private ClickListenerCollection clickListeners;
  private LoadListenerCollection loadListeners;
  private MouseListenerCollection mouseListeners;

  private State state;

  /**
   * Creates an empty image.
   */
  public Image() {
    changeState(new UnclippedState(this));
    setStyleName("gwt-Image");
  }

  /**
   * Creates an image with a specified URL. The load event will be fired once
   * the image at the given URL has been retrieved by the browser.
   * 
   * @param url the URL of the image to be displayed
   */
  public Image(String url) {
    changeState(new UnclippedState(this, url));
    setStyleName("gwt-Image");
  }

  /**
   * Creates a clipped image with a specified URL and visibility rectangle. The
   * visibility rectangle is declared relative to the the rectangle which
   * encompasses the entire image, which has an upper-left vertex of (0,0). The
   * load event will be fired immediately after the object has been constructed
   * (i.e. potentially before the image has been loaded in the browser). Since
   * the width and height are specified explicitly by the user, this behavior
   * will not cause problems with retrieving the width and height of a clipped
   * image in a load event handler.
   * 
   * @param url the URL of the image to be displayed
   * @param left the horizontal co-ordinate of the upper-left vertex of the
   *          visibility rectangle
   * @param top the vertical co-ordinate of the upper-left vertex of the
   *          visibility rectangle
   * @param width the width of the visibility rectangle
   * @param height the height of the visibility rectangle
   */
  public Image(String url, int left, int top, int width, int height) {
    changeState(new ClippedState(this, url, left, top, width, height));
    setStyleName("gwt-Image");
  }

  public void addClickListener(ClickListener listener) {
    if (clickListeners == null) {
      clickListeners = new ClickListenerCollection();
    }
    clickListeners.add(listener);
  }

  public void addLoadListener(LoadListener listener) {
    if (loadListeners == null) {
      loadListeners = new LoadListenerCollection();
    }
    loadListeners.add(listener);
  }

  public void addMouseListener(MouseListener listener) {
    if (mouseListeners == null) {
      mouseListeners = new MouseListenerCollection();
    }
    mouseListeners.add(listener);
  }

  /**
   * Gets the height of the image. When the image is in the unclipped state, the
   * height of the image is not known until the image has been loaded (i.e. load
   * event has been fired for the image).
   * 
   * @return the height of the image, or 0 if the height is unknown
   */
  public int getHeight() {
    return state.getHeight(this);
  }

  /**
   * Gets the horizontal co-ordinate of the upper-left vertex of the image's
   * visibility rectangle. If the image is in the unclipped state, then the
   * visibility rectangle is assumed to be the rectangle which encompasses the
   * entire image, which has an upper-left vertex of (0,0).
   * 
   * @return the horizontal co-ordinate of the upper-left vertex of the image's
   *         visibility rectangle
   */
  public int getOriginLeft() {
    return state.getOriginLeft();
  }

  /**
   * Gets the vertical co-ordinate of the upper-left vertex of the image's
   * visibility rectangle. If the image is in the unclipped state, then the
   * visibility rectangle is assumed to be the rectangle which encompasses the
   * entire image, which has an upper-left vertex of (0,0).
   * 
   * @return the vertical co-ordinate of the upper-left vertex of the image's
   *         visibility rectangle
   */
  public int getOriginTop() {
    return state.getOriginTop();
  }

  /**
   * Gets the URL of the image. The URL that is returned is not necessarily the
   * URL that was passed in by the user. It may have been transformed to an
   * absolute URL.
   * 
   * @return the image URL
   */
  public String getUrl() {
    return state.getUrl(this);
  }

  /**
   * Gets the width of the image. When the image is in the unclipped state, the
   * width of the image is not known until the image has been loaded (i.e. load
   * event has been fired for the image).
   * 
   * @return the width of the image, or 0 if the width is unknown
   */
  public int getWidth() {
    return state.getWidth(this);
  }

  public void onBrowserEvent(Event event) {
    switch (DOM.eventGetType(event)) {
      case Event.ONCLICK: {
        if (clickListeners != null) {
          clickListeners.fireClick(this);
        }
        break;
      }
      case Event.ONMOUSEDOWN:
      case Event.ONMOUSEUP:
      case Event.ONMOUSEMOVE:
      case Event.ONMOUSEOVER:
      case Event.ONMOUSEOUT: {
        if (mouseListeners != null) {
          mouseListeners.fireMouseEvent(this, event);
        }
        break;
      }
      case Event.ONLOAD: {
        if (loadListeners != null) {
          loadListeners.fireLoad(this);
        }
        break;
      }
      case Event.ONERROR: {
        if (loadListeners != null) {
          loadListeners.fireError(this);
        }
        break;
      }
    }
  }

  public void removeClickListener(ClickListener listener) {
    if (clickListeners != null) {
      clickListeners.remove(listener);
    }
  }

  public void removeLoadListener(LoadListener listener) {
    if (loadListeners != null) {
      loadListeners.remove(listener);
    }
  }

  public void removeMouseListener(MouseListener listener) {
    if (mouseListeners != null) {
      mouseListeners.remove(listener);
    }
  }

  /**
   * Sets the URL of the image to be displayed. If the image is in the clipped
   * state, a call to this method will cause a transition of the image to the
   * unclipped state. Regardless of whether or not the image is in the clipped
   * or unclipped state, a load event will be fired.
   * 
   * @param url the image URL
   */
  public void setUrl(String url) {
    state.setUrl(this, url);
  }

  /**
   * Sets the url and the visibility rectangle for the image at the same time. A
   * single load event will be fired if either the incoming url or visiblity
   * rectangle co-ordinates differ from the image's current url or current
   * visibility rectangle co-ordinates. If the image is currently in the
   * unclipped state, a call to this method will cause a transition to the
   * clipped state.
   * 
   * @param url the image URL
   * @param left the horizontal coordinate of the upper-left vertex of the
   *          visibility rectangle
   * @param top the vertical coordinate of the upper-left vertex of the
   *          visibility rectangle
   * @param width the width of the visibility rectangle
   * @param height the height of the visibility rectangle
   */
  public void setUrlAndVisibleRect(String url, int left, int top, int width,
      int height) {
    state.setUrlAndVisibleRect(this, url, left, top, width, height);
  }

  /**
   * Sets the visibility rectangle of an image. The visibility rectangle is
   * declared relative to the the rectangle which encompasses the entire image,
   * which has an upper-left vertex of (0,0). Provided that any of the left,
   * top, width, and height parameters are different than the those values that
   * are currently set for the image, a load event will be fired. If the image
   * is in the unclipped state, a call to this method will cause a transition of
   * the image to the clipped state. This transition will cause a load event to
   * fire.
   * 
   * @param left the horizontal coordinate of the upper-left vertex of the
   *          visibility rectangle
   * @param top the vertical coordinate of the upper-left vertex of the
   *          visibility rectangle
   * @param width the width of the visibility rectangle
   * @param height the height of the visibility rectangle
   */
  public void setVisibleRect(int left, int top, int width, int height) {
    state.setVisibleRect(this, left, top, width, height);
  }

  private void changeState(State newState) {
    state = newState;
  }
}
