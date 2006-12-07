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

/**
 * The base class for all user-interface objects. It simply wraps a DOM element,
 * and cannot receive events. Most interesting user-interface classes derive
 * from {@link com.google.gwt.user.client.ui.Widget}.
 */
public abstract class UIObject {
  private static final String NULL_HANDLE_MSG = "Null widget handle.  If you "
      + "are creating a composite, ensure that initWidget() has been called.";

  public static native boolean isVisible(Element elem) /*-{
    return (elem.style.display != 'none');
  }-*/;

  public static native void setVisible(Element elem, boolean visible) /*-{
    elem.style.display = visible ? '' : 'none';
  }-*/;

  /**
   * This convenience method implements allows one to easily add or remove the
   * style name for any element. This can be useful when you need to add and
   * remove styles from a sub-element within a {@link UIObject}.
   * 
   * @param elem the element whose style is to be modified
   * @param style the style name to be added or removed
   * @param add <code>true</code> to add the given style, <code>false</code>
   *          to remove it
   */
  protected static void setStyleName(Element elem, String style, boolean add) {
    if (elem == null) {
      throw new RuntimeException(NULL_HANDLE_MSG);
    }
    if (style.length() == 0) {
      throw new IllegalArgumentException(
          "Cannot pass is an empty string as a style name.");
    }

    // Get the current style string.
    String oldStyle = DOM.getAttribute(elem, "className");
    int idx;
    if (oldStyle == null) {
      idx = -1;
      oldStyle = "";
    } else {
      idx = oldStyle.indexOf(style);
    }

    // Calculate matching index.
    while (idx != -1) {
      if (idx == 0 || oldStyle.charAt(idx - 1) == ' ') {
        int last = idx + style.length();
        int lastPos = oldStyle.length();
        if ((last == lastPos)
            || ((last < lastPos) && (oldStyle.charAt(last) == ' '))) {
          break;
        }
      }
      idx = oldStyle.indexOf(style, idx + 1);
    }

    if (add) {
      // Only add the style if it's not already present.
      if (idx == -1) {
        DOM.setAttribute(elem, "className", oldStyle + " " + style);
      }
    } else {
      // Don't try to remove the style if it's not there.
      if (idx != -1) {
        String begin = oldStyle.substring(0, idx);
        String end = oldStyle.substring(idx + style.length());
        DOM.setAttribute(elem, "className", begin + end);
      }
    }
  }

  private Element element;

  /**
   * Adds a style name to the widget.
   * 
   * @param style the style name to be added
   * @see #removeStyleName(String)
   */
  public void addStyleName(String style) {
    setStyleName(element, style, true);
  }

  /**
   * Gets the object's absolute left position in pixels, as measured from the
   * browser window's client area.
   * 
   * @return the object's absolute left position
   */
  public int getAbsoluteLeft() {
    return DOM.getAbsoluteLeft(getElement());
  }

  /**
   * Gets the object's absolute top position in pixels, as measured from the
   * browser window's client area.
   * 
   * @return the object's absolute top position
   */
  public int getAbsoluteTop() {
    return DOM.getAbsoluteTop(getElement());
  }

  /**
   * Gets a handle to the object's underlying DOM element.
   * 
   * @return the object's browser element
   */
  public Element getElement() {
    return element;
  }

  /**
   * Gets the object's offset height in pixels. This is the total height of the
   * object, including decorations such as border, margin, and padding.
   * 
   * @return the object's offset height
   */
  public int getOffsetHeight() {
    return DOM.getIntAttribute(element, "offsetHeight");
  }

  /**
   * Gets the object's offset width in pixels. This is the total width of the
   * object, including decorations such as border, margin, and padding.
   * 
   * @return the object's offset width
   */
  public int getOffsetWidth() {
    return DOM.getIntAttribute(element, "offsetWidth");
  }

  /**
   * Gets the style name associated with the object.
   * 
   * @return the object's style name
   * @see #setStyleName(String)
   */
  public String getStyleName() {
    return DOM.getAttribute(element, "className");
  }

  /**
   * Gets the title associated with this object. The title is the 'tool-tip'
   * displayed to users when they hover over the object.
   * 
   * @return the object's title
   */
  public String getTitle() {
    return DOM.getAttribute(element, "title");
  }

  /**
   * Determines whether or not this object is visible.
   * 
   * @return <code>true</code> if the object is visible
   */
  public boolean isVisible() {
    return isVisible(element);
  }

  /**
   * Removes a style name from the widget.
   * 
   * @param style the style name to be added
   * @see #addStyleName(String)
   */
  public void removeStyleName(String style) {
    setStyleName(element, style, false);
  }

  /**
   * Sets the object's height. This height does not include decorations such as
   * border, margin, and padding.
   * 
   * @param height the object's new height, in CSS units (e.g. "10px", "1em")
   */
  public void setHeight(String height) {
    DOM.setStyleAttribute(element, "height", height);
  }

  /**
   * Sets the object's size, in pixels, not including decorations such as
   * border, margin, and padding.
   * 
   * @param width the object's new width, in pixels
   * @param height the object's new height, in pixels
   */
  public void setPixelSize(int width, int height) {
    if (width >= 0) {
      setWidth(width + "px");
    }
    if (height >= 0) {
      setHeight(height + "px");
    }
  }

  /**
   * Sets the object's size. This size does not include decorations such as
   * border, margin, and padding.
   * 
   * @param width the object's new width, in CSS units (e.g. "10px", "1em")
   * @param height the object's new height, in CSS units (e.g. "10px", "1em")
   */
  public void setSize(String width, String height) {
    setWidth(width);
    setHeight(height);
  }

  /**
   * Sets the object's style name, removing all other styles.
   * 
   * <p>
   * The style name is the name referred to in CSS style rules (in HTML, this is
   * referred to as the element's "class"). By convention, style rules are of
   * the form <code>[project]-[widget]</code> (e.g. the {@link Button}
   * widget's style name is <code>.gwt-Button</code>).
   * </p>
   * 
   * <p>
   * For example, if a widget's style name is <code>myProject-MyWidget</code>,
   * then the style rule that applies to it will be
   * <code>.myProject-MyWidget</code>. Note the "dot" prefix -- this is
   * necessary because calling this method sets the underlying element's
   * <code>className</code> property.
   * </p>
   * 
   * <p>
   * An object may have any number of style names, which may be manipulated
   * using {@link #addStyleName(String)} and {@link #removeStyleName(String)}.
   * The attributes of all styles associated with the object will be applied to
   * it.
   * </p>
   * 
   * @param style the style name to be added
   * @see #addStyleName(String)
   * @see #removeStyleName(String)
   */
  public void setStyleName(String style) {
    if (element == null) {
      throw new RuntimeException(NULL_HANDLE_MSG);
    }

    DOM.setAttribute(element, "className", style);
  }

  /**
   * Sets the title associated with this object. The title is the 'tool-tip'
   * displayed to users when they hover over the object.
   * 
   * @param title the object's new title
   */
  public void setTitle(String title) {
    DOM.setAttribute(element, "title", title);
  }

  /**
   * Sets whether this object is visible.
   * 
   * @param visible <code>true</code> to show the object, <code>false</code>
   *          to hide it
   */
  public void setVisible(boolean visible) {
    setVisible(element, visible);
  }

  /**
   * Sets the object's width. This width does not include decorations such as
   * border, margin, and padding.
   * 
   * @param width the object's new width, in CSS units (e.g. "10px", "1em")
   */
  public void setWidth(String width) {
    DOM.setStyleAttribute(element, "width", width);
  }

  /**
   * Adds a set of events to be sunk by this object. Note that only
   * {@link Widget widgets} may actually receive events, but can receive events
   * from all objects contained within them.
   * 
   * @param eventBitsToAdd a bitfield representing the set of events to be added
   *          to this element's event set
   * @see com.google.gwt.user.client.Event
   */
  public void sinkEvents(int eventBitsToAdd) {
    DOM.sinkEvents(getElement(), eventBitsToAdd
        | DOM.getEventsSunk(getElement()));
  }

  /**
   * This method is overridden so that any object can be viewed in the debugger
   * as an HTML snippet.
   * 
   * @return a string representation of the object
   */
  public String toString() {
    if (element == null) {
      return "(null handle)";
    }
    return DOM.toString(element);
  }

  /**
   * Removes a set of events from this object's event list.
   * 
   * @param eventBitsToRemove a bitfield representing the set of events to be
   *          removed from this element's event set
   * @see #sinkEvents
   * @see com.google.gwt.user.client.Event
   */
  public void unsinkEvents(int eventBitsToRemove) {
    DOM.sinkEvents(getElement(), DOM.getEventsSunk(getElement())
        & (~eventBitsToRemove));
  }

  /**
   * Sets this object's browser element. UIObject subclasses must call this
   * method before attempting to call any other methods.
   * 
   * @param elem the object's new element
   */
  protected void setElement(Element elem) {
    this.element = elem;
  }
}
