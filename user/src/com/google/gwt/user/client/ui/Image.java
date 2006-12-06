/*
 * Copyright 2006 Google Inc.
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

import java.util.HashMap;

/**
 * A widget that displays the image at a given URL.
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class="css">
 * <li>.gwt-Image { }</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.ImageExample}
 * </p>
 */
public class Image extends Widget implements SourcesClickEvents,
    SourcesMouseEvents, SourcesLoadEvents {

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

  /**
   * Creates an empty image.
   */
  public Image() {
    setElement(DOM.createImg());
    sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS | Event.ONLOAD | Event.ONERROR);
    setStyleName("gwt-Image");
  }

  /**
   * Creates an image with a specified URL.
   * 
   * @param url the URL of the image to be displayed
   */
  public Image(String url) {
    this();
    setUrl(url);
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
   * Gets the URL of the image.
   * 
   * @return the image URL
   */
  public String getUrl() {
    return DOM.getAttribute(getElement(), "src");
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
   * Sets the URL of the image to be displayed.
   * 
   * @param url the image URL
   */
  public void setUrl(String url) {
    DOM.setAttribute(getElement(), "src", url);
  }
}