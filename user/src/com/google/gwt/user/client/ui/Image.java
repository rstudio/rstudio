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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.HasAllMouseHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasErrorHandlers;
import com.google.gwt.event.dom.client.HasLoadHandlers;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelEvent;
import com.google.gwt.event.dom.client.MouseWheelHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.impl.ClippedImageImpl;

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
 * <dl>
 * <dt>.gwt-Image</dt>
 * </dd>The outer element</dd>
 * </dl>
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
@SuppressWarnings("deprecation")
public class Image extends Widget implements SourcesLoadEvents,
    HasLoadHandlers, HasErrorHandlers, SourcesClickEvents, HasClickHandlers,
    HasAllMouseHandlers, SourcesMouseEvents {

  /**
   * The attribute that is set when an image fires a native load or error event
   * before it is attached.
   */
  private static final String UNHANDLED_EVENT_ATTR = "__gwtLastUnhandledEvent";

  /**
   * Implementation of behaviors associated with the clipped state of an image.
   */
  private static class ClippedState extends State {

    private static final ClippedImageImpl impl = GWT.create(ClippedImageImpl.class);

    private int height = 0;
    private int left = 0;
    private int top = 0;
    private String url = null;
    private int width = 0;

    ClippedState(Image image, String url, int left, int top, int width,
        int height) {
      this.left = left;
      this.top = top;
      this.width = width;
      this.height = height;
      this.url = url;
      image.replaceElement(impl.createStructure(url, left, top, width, height));
      // Todo(ecc) This is wrong, we should not be sinking these here on such a
      // common widget.After the branch is stable, this should be fixed.
      image.sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS | Event.ONMOUSEWHEEL
          | Event.ONLOAD);
    }

    @Override
    public int getHeight(Image image) {
      return height;
    }

    @Override
    public ImageElement getImageElement(Image image) {
      return impl.getImgElement(image).cast();
    }

    @Override
    public int getOriginLeft() {
      return left;
    }

    @Override
    public int getOriginTop() {
      return top;
    }

    @Override
    public String getUrl(Image image) {
      return url;
    }

    @Override
    public int getWidth(Image image) {
      return width;
    }

    @Override
    public void setUrl(Image image, String url) {
      image.changeState(new UnclippedState(image));
      // Need to make sure we change the state before an onload event can fire,
      // or handlers will be fired while we are in the old state.
      image.setUrl(url);
    }

    @Override
    public void setUrlAndVisibleRect(Image image, String url, int left,
        int top, int width, int height) {
      if (!this.url.equals(url) || this.left != left || this.top != top
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

    @Override
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
    @Override
    protected String getStateName() {
      return "clipped";
    }
  }

  /**
   * Abstract class which is used to hold the state associated with an image
   * object.
   */
  private abstract static class State {

    public abstract int getHeight(Image image);

    public abstract ImageElement getImageElement(Image image);

    public abstract int getOriginLeft();

    public abstract int getOriginTop();

    public abstract String getUrl(Image image);

    public abstract int getWidth(Image image);

    public void onLoad(Image image) {
      // If an onload event fired while the image wasn't attached, we need to
      // synthesize one now.
      String unhandledEvent = getImageElement(image).getPropertyString(
          UNHANDLED_EVENT_ATTR);
      if ("load".equals(unhandledEvent)) {
        fireSyntheticLoadEvent(image);
      }
    }

    public abstract void setUrl(Image image, String url);

    public abstract void setUrlAndVisibleRect(Image image, String url,
        int left, int top, int width, int height);

    public abstract void setVisibleRect(Image image, int left, int top,
        int width, int height);

    /**
     * We need to synthesize a load event in case the image loads synchronously,
     * before our handlers can be attached.
     * 
     * @param image the image on which to dispatch the event
     */
    protected void fireSyntheticLoadEvent(final Image image) {
      /*
       * We use a deferred command here to simulate the native version of the
       * event as closely as possible. In the native event case, it is unlikely
       * that a second load event would occur while you are in the load event
       * handler.
       */
      DeferredCommand.addCommand(new Command() {
        public void execute() {
          NativeEvent evt = Document.get().createLoadEvent();
          getImageElement(image).dispatchEvent(evt);
        }
      });
    }

    // This method is used only by unit tests.
    protected abstract String getStateName();
  }

  /**
   * Implementation of behaviors associated with the unclipped state of an
   * image.
   */
  private static class UnclippedState extends State {

    UnclippedState(Element element) {
      // This case is relatively unusual, in that we swapped a clipped image
      // out, so does not need to be efficient.
      Event.sinkEvents(element, Event.ONCLICK | Event.MOUSEEVENTS
          | Event.ONLOAD | Event.ONERROR | Event.ONMOUSEWHEEL);
    }

    UnclippedState(Image image) {
      image.replaceElement(Document.get().createImageElement());
      // We are working around an IE race condition that can make the image
      // incorrectly cache itself if the load event is assigned at the same time
      // as the image is added to the dom.
      Event.sinkEvents(image.getElement(), Event.ONLOAD);

      // Todo(ecc) this could be more efficient overall.
      image.sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS | Event.ONLOAD
          | Event.ONERROR | Event.ONMOUSEWHEEL);
    }

    UnclippedState(Image image, String url) {
      this(image);
      setUrl(image, url);
    }

    @Override
    public int getHeight(Image image) {
      return getImageElement(image).getHeight();
    }

    @Override
    public ImageElement getImageElement(Image image) {
      return image.getElement().cast();
    }

    @Override
    public int getOriginLeft() {
      return 0;
    }

    @Override
    public int getOriginTop() {
      return 0;
    }

    @Override
    public String getUrl(Image image) {
      return getImageElement(image).getSrc();
    }

    @Override
    public int getWidth(Image image) {
      return getImageElement(image).getWidth();
    }

    @Override
    public void setUrl(Image image, String url) {
      image.clearUnhandledEvent();
      getImageElement(image).setSrc(url);
    }

    @Override
    public void setUrlAndVisibleRect(Image image, String url, int left,
        int top, int width, int height) {
      image.changeState(new ClippedState(image, url, left, top, width, height));
    }

    @Override
    public void setVisibleRect(Image image, int left, int top, int width,
        int height) {
      image.changeState(new ClippedState(image, getUrl(image), left, top,
          width, height));
    }

    // This method is used only by unit tests.
    @Override
    protected String getStateName() {
      return "unclipped";
    }
  }

  /**
   * This map is used to store prefetched images. If a reference is not kept to
   * the prefetched image objects, they can get garbage collected, which
   * sometimes keeps them from getting fully fetched.
   */
  private static HashMap<String, Element> prefetchImages = new HashMap<String, Element>();

  /**
   * Causes the browser to pre-fetch the image at a given URL.
   * 
   * @param url the URL of the image to be prefetched
   */
  public static void prefetch(String url) {
    ImageElement img = Document.get().createImageElement();
    img.setSrc(url);
    prefetchImages.put(url, img);
  }

  /**
   * Creates a Image widget that wraps an existing &lt;img&gt; element.
   * 
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * @param element the element to be wrapped
   */
  public static Image wrap(Element element) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    Image image = new Image(element);
    image.changeState(new UnclippedState(element));

    // Mark it attached and remember it for cleanup.
    image.onAttach();
    RootPanel.detachOnWindowClose(image);

    return image;
  }

  private State state;

  /**
   * Creates an empty image.
   */
  public Image() {
    changeState(new UnclippedState(this));
    setStyleName("gwt-Image");
  }

  /**
   * Creates an image whose size and content are defined by an ImageResource.
   * 
   * @param resource the ImageResource to be displayed
   */
  public Image(ImageResource resource) {
    this(resource.getURL(), resource.getLeft(), resource.getTop(),
        resource.getWidth(), resource.getHeight());
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

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be an &lt;img&gt; element.
   * 
   * @param element the element to be used
   */
  protected Image(Element element) {
    ImageElement.as(element);
    setElement(element);
  }

  public HandlerRegistration addClickHandler(ClickHandler handler) {
    return addHandler(handler, ClickEvent.getType());
  }

  /**
   * @deprecated Use {@link #addClickHandler} instead
   */
  @Deprecated
  public void addClickListener(ClickListener listener) {
    ListenerWrapper.WrappedClickListener.add(this, listener);
  }

  public HandlerRegistration addErrorHandler(ErrorHandler handler) {
    return addHandler(handler, ErrorEvent.getType());
  }

  public HandlerRegistration addLoadHandler(LoadHandler handler) {
    return addHandler(handler, LoadEvent.getType());
  }

  /**
   * @deprecated use {@link com.google.gwt.event.dom.client.LoadHandler} and
   *             {@link com.google.gwt.event.dom.client.ErrorHandler} instead
   */
  @Deprecated
  public void addLoadListener(LoadListener listener) {
    ListenerWrapper.WrappedLoadListener.add(this, listener);
  }

  public HandlerRegistration addMouseDownHandler(MouseDownHandler handler) {
    return addDomHandler(handler, MouseDownEvent.getType());
  }

  /**
   * @deprecated Use {@link #addMouseOverHandler} {@link #addMouseMoveHandler},
   *             {@link #addMouseDownHandler}, {@link #addMouseUpHandler} and
   *             {@link #addMouseOutHandler} instead
   */
  @Deprecated
  public void addMouseListener(MouseListener listener) {
    ListenerWrapper.WrappedMouseListener.add(this, listener);
  }

  public HandlerRegistration addMouseMoveHandler(MouseMoveHandler handler) {
    return addDomHandler(handler, MouseMoveEvent.getType());
  }

  public HandlerRegistration addMouseOutHandler(MouseOutHandler handler) {
    return addDomHandler(handler, MouseOutEvent.getType());
  }

  public HandlerRegistration addMouseOverHandler(MouseOverHandler handler) {
    return addDomHandler(handler, MouseOverEvent.getType());
  }

  public HandlerRegistration addMouseUpHandler(MouseUpHandler handler) {
    return addDomHandler(handler, MouseUpEvent.getType());
  }

  public HandlerRegistration addMouseWheelHandler(MouseWheelHandler handler) {
    return addDomHandler(handler, MouseWheelEvent.getType());
  }

  /**
   * @deprecated Use {@link #addMouseWheelHandler} instead
   */
  @Deprecated
  public void addMouseWheelListener(MouseWheelListener listener) {
    ListenerWrapper.WrappedMouseWheelListener.add(this, listener);
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

  @Override
  public void onBrowserEvent(Event event) {
    // We have to clear the unhandled event before firing handlers because the
    // handlers could trigger onLoad, which would refire the event.
    if (event.getTypeInt() == Event.ONLOAD) {
      clearUnhandledEvent();
    }

    super.onBrowserEvent(event);
  }

  /**
   * @deprecated Use the {@link HandlerRegistration#removeHandler} method on the
   *             object returned by {@link #addClickHandler} instead
   */
  @Deprecated
  public void removeClickListener(ClickListener listener) {
    ListenerWrapper.WrappedClickListener.remove(this, listener);
  }

  /**
   * @deprecated Use the {@link HandlerRegistration#removeHandler} method on the
   *             object returned by an add*Handler method instead
   */
  @Deprecated
  public void removeLoadListener(LoadListener listener) {
    ListenerWrapper.WrappedLoadListener.remove(this, listener);
  }

  /**
   * @deprecated Use the {@link HandlerRegistration#removeHandler} method on the
   *             object returned by an add*Handler method instead
   */
  @Deprecated
  public void removeMouseListener(MouseListener listener) {
    ListenerWrapper.WrappedMouseListener.remove(this, listener);
  }

  /**
   * @deprecated Use the {@link HandlerRegistration#removeHandler} method on the
   *             object returned by {@link #addMouseWheelHandler} instead
   */
  @Deprecated
  public void removeMouseWheelListener(MouseWheelListener listener) {
    ListenerWrapper.WrappedMouseWheelListener.remove(this, listener);
  }

  /**
   * Sets the url and the visibility rectangle for the image at the same time,
   * based on an ImageResource instance. A single load event will be fired if
   * either the incoming url or visiblity rectangle co-ordinates differ from the
   * image's current url or current visibility rectangle co-ordinates. If the
   * image is currently in the unclipped state, a call to this method will cause
   * a transition to the clipped state.
   * 
   * @param resource the ImageResource to display
   */
  public void setResource(ImageResource resource) {
    setUrlAndVisibleRect(resource.getURL(), resource.getLeft(),
        resource.getTop(), resource.getWidth(), resource.getHeight());
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

  @Override
  protected void onLoad() {
    super.onLoad();

    // Issue 863: the state may need to fire a synthetic event if the native
    // onload event fired while the image was detached.
    state.onLoad(this);
  }

  private void changeState(State newState) {
    state = newState;
  }

  /**
   * Clear the unhandled event.
   */
  private void clearUnhandledEvent() {
    if (state != null) {
      state.getImageElement(this).setPropertyString(UNHANDLED_EVENT_ATTR, "");
    }
  }
}
