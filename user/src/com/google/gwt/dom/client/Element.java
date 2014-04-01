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
package com.google.gwt.dom.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * All HTML element interfaces derive from this class.
 */
public class Element extends Node {

  /**
   * Fast helper method to convert small doubles to 32-bit int.
   *
   * <p>Note: you should be aware that this uses JavaScript rounding and thus
   * does NOT provide the same semantics as <code>int b = (int) someDouble;</code>.
   * In particular, if x is outside the range [-2^31,2^31), then toInt32(x) would return a value
   * equivalent to x modulo 2^32, whereas (int) x would evaluate to either MIN_INT or MAX_INT.
   */
  private static native int toInt32(double val) /*-{
    return val | 0;
  }-*/;

  /**
   * Constant returned from {@link #getDraggable()}.
   */
  public static final String DRAGGABLE_AUTO = "auto";

  /**
   * Constant returned from {@link #getDraggable()}.
   */
  public static final String DRAGGABLE_FALSE = "false";

  /**
   * Constant returned from {@link #getDraggable()}.
   */
  public static final String DRAGGABLE_TRUE = "true";

  /**
   * Assert that the given {@link Node} is an {@link Element} and automatically
   * typecast it.
   */
  public static Element as(JavaScriptObject o) {
    assert is(o);
    return (Element) o;
  }

  /**
   * Assert that the given {@link Node} is an {@link Element} and automatically
   * typecast it.
   */
  public static Element as(Node node) {
    assert is(node);
    return (Element) node;
  }

  /**
   * Determines whether the given {@link JavaScriptObject} can be cast to an
   * {@link Element}. A <code>null</code> object will cause this method to
   * return <code>false</code>.
   */
  public static boolean is(JavaScriptObject o) {
    if (Node.is(o)) {
      return is((Node) o);
    }
    return false;
  }

  /**
   * Determine whether the given {@link Node} can be cast to an {@link Element}.
   * A <code>null</code> node will cause this method to return
   * <code>false</code>.
   */
  public static boolean is(Node node) {
    return (node != null) && (node.getNodeType() == Node.ELEMENT_NODE);
  }

  protected Element() {
  }

  /**
   * Adds a name to this element's class property. If the name is already
   * present, this method has no effect.
   * 
   * @param className the class name to be added
   * @return <code>true</code> if this element did not already have the specified class name
   * @see #setClassName(String)
   */
  public final boolean addClassName(String className) {
    className = trimClassName(className);

    // Get the current style string.
    String oldClassName = getClassName();
    int idx = indexOfName(oldClassName, className);

    // Only add the style if it's not already present.
    if (idx == -1) {
      if (oldClassName.length() > 0) {
        setClassName(oldClassName + " " + className);
      } else {
        setClassName(className);
      }
      return true;
    }
    return false;
  }

  /**
   * Removes keyboard focus from this element.
   */
  public final native void blur() /*-{
    this.blur();
  }-*/;

  /**
   * Dispatched the given event with this element as its target. The event will
   * go through all phases of the browser's normal event dispatch mechanism.
   * 
   * Note: Because the browser's normal dispatch mechanism is used, exceptions
   * thrown from within handlers triggered by this method cannot be caught by
   * wrapping this method in a try/catch block. Such exceptions will be caught
   * by the
   * {@link com.google.gwt.core.client.GWT#setUncaughtExceptionHandler(com.google.gwt.core.client.GWT.UncaughtExceptionHandler) uncaught exception handler}
   * as usual.
   * 
   * @param evt the event to be dispatched
   */
  public final void dispatchEvent(NativeEvent evt) {
    DOMImpl.impl.dispatchEvent(this, evt);
  }

  /**
   * Gives keyboard focus to this element.
   */
  public final native void focus() /*-{
    this.focus();
  }-*/;

  /**
   * Gets an element's absolute bottom coordinate in the document's coordinate
   * system.
   */
  public final int getAbsoluteBottom() {
    return getAbsoluteTop() + getOffsetHeight();
  }

  /**
   * Gets an element's absolute left coordinate in the document's coordinate
   * system.
   */
  public final int getAbsoluteLeft() {
    return DOMImpl.impl.getAbsoluteLeft(this);
  }

  /**
   * Gets an element's absolute right coordinate in the document's coordinate
   * system.
   */
  public final int getAbsoluteRight() {
    return getAbsoluteLeft() + getOffsetWidth();
  }

  /**
   * Gets an element's absolute top coordinate in the document's coordinate
   * system.
   */
  public final int getAbsoluteTop() {
    return DOMImpl.impl.getAbsoluteTop(this);
  }

  /**
   * Retrieves an attribute value by name.  Attribute support can be
   * inconsistent across various browsers.  Consider using the accessors in
   * {@link Element} and its specific subclasses to retrieve attributes and
   * properties.
   * 
   * @param name The name of the attribute to retrieve
   * @return The Attr value as a string, or the empty string if that attribute
   *         does not have a specified or default value
   */
  public final String getAttribute(String name) {
    return DOMImpl.impl.getAttribute(this, name);
  }

  /**
   * The class attribute of the element. This attribute has been renamed due to
   * conflicts with the "class" keyword exposed by many languages.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-class">W3C
   *      HTML Specification</a>
   */
  public final native String getClassName() /*-{
     return this.className || "";
   }-*/;

  /**
   * Returns the inner height of an element in pixels, including padding but not
   * the horizontal scrollbar height, border, or margin.
   * 
   * @return the element's client height
   */
  public final int getClientHeight() {
    return toInt32(getSubPixelClientHeight());
  }

  /**
   * Returns the inner width of an element in pixels, including padding but not
   * the vertical scrollbar width, border, or margin.
   * 
   * @return the element's client width
   */
  public final int getClientWidth() {
    return toInt32(getSubPixelClientWidth());
  }

  /**
   * Specifies the base direction of directionally neutral text and the
   * directionality of tables.
   */
  public final native String getDir() /*-{
     return this.dir;
   }-*/;

  /**
   * Returns the draggable attribute of this element.
   * 
   * @return one of {@link #DRAGGABLE_AUTO}, {@link #DRAGGABLE_FALSE}, or
   *         {@link #DRAGGABLE_TRUE}
   */
  public final native String getDraggable() /*-{
    return this.draggable || null;
  }-*/;

  /**
   * Returns a NodeList of all descendant Elements with a given tag name, in the
   * order in which they are encountered in a preorder traversal of this Element
   * tree.
   * 
   * @param name The name of the tag to match on. The special value "*" matches
   *          all tags
   * @return A list of matching Element nodes
   */
  public final native NodeList<Element> getElementsByTagName(String name) /*-{
     return this.getElementsByTagName(name);
   }-*/;

  /**
   * The first child of element this element. If there is no such element, this
   * returns null.
   */
  public final Element getFirstChildElement() {
    return DOMImpl.impl.getFirstChildElement(this);
  }

  /**
   * The element's identifier.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-id">W3C
   *      HTML Specification</a>
   */
  public final native String getId() /*-{
     return this.id;
   }-*/;

  /**
   * All of the markup and content within a given element.
   */
  public final String getInnerHTML() {
    return DOMImpl.impl.getInnerHTML(this);
  }

  /**
   * The text between the start and end tags of the object.
   */
  public final String getInnerText() {
    return DOMImpl.impl.getInnerText(this);
  }

  /**
   * Language code defined in RFC 1766.
   */
  public final native String getLang() /*-{
     return this.lang;
   }-*/;

  /**
   * The element immediately following this element. If there is no such
   * element, this returns null.
   */
  public final Element getNextSiblingElement() {
    return DOMImpl.impl.getNextSiblingElement(this);
  }

  /**
   * The height of an element relative to the layout.
   */
  public final int getOffsetHeight() {
    return toInt32(getSubPixelOffsetHeight());
  }

  /**
   * The number of pixels that the upper left corner of the current element is
   * offset to the left within the offsetParent node.
   */
  public final int getOffsetLeft() {
    return toInt32(getSubPixelOffsetLeft());
  }

  /**
   * Returns a reference to the object which is the closest (nearest in the
   * containment hierarchy) positioned containing element.
   */
  public final native Element getOffsetParent() /*-{
     return this.offsetParent;
   }-*/;

  /**
   * The number of pixels that the upper top corner of the current element is
   * offset to the top within the offsetParent node.
   */
  public final int getOffsetTop() {
    return toInt32(getSubPixelOffsetTop());
  }

  /**
   * The width of an element relative to the layout.
   */
  public final int getOffsetWidth() {
    return toInt32(getSubPixelOffsetWidth());
  }

  /**
   * The element immediately preceeding this element. If there is no such
   * element, this returns null.
   */
  public final Element getPreviousSiblingElement() {
    return DOMImpl.impl.getPreviousSiblingElement(this);
  }

  /**
   * Gets a boolean property from this element.
   * 
   * @param name the name of the property to be retrieved
   * @return the property value
   */
  public final native boolean getPropertyBoolean(String name) /*-{
     return !!this[name];
   }-*/;

  /**
   * Gets a double property from this element.
   * 
   * @param name the name of the property to be retrieved
   * @return the property value
   */
  public final native double getPropertyDouble(String name) /*-{
     return parseFloat(this[name]) || 0.0;
   }-*/;

  /**
   * Gets an integer property from this element.
   * 
   * @param name the name of the property to be retrieved
   * @return the property value
   */
  public final native int getPropertyInt(String name) /*-{
     return parseInt(this[name]) | 0;
   }-*/;

  /**
   * Gets a JSO property from this element.
   *
   * @param name the name of the property to be retrieved
   * @return the property value
   */
  public final native JavaScriptObject getPropertyJSO(String name) /*-{
    return this[name] || null;
  }-*/;

  /**
   * Gets an object property from this element.
   *
   * @param name the name of the property to be retrieved
   * @return the property value
   */
  public final native Object getPropertyObject(String name) /*-{
    return this[name] || null;
  }-*/;

  /**
   * Gets a property from this element.
   * 
   * @param name the name of the property to be retrieved
   * @return the property value
   */
  public final native String getPropertyString(String name) /*-{
     return (this[name] == null) ? null : String(this[name]);
   }-*/;

  /**
   * The height of the scroll view of an element.
   */
  public final int getScrollHeight() {
    return toInt32(getSubPixelScrollHeight());
  }

  /**
   * The number of pixels that an element's content is scrolled from the left.
   * 
   * <p>
   * If the element is in RTL mode, this method will return a negative value of
   * the number of pixels scrolled from the right.
   * </p>
   */
  public final int getScrollLeft() {
    return DOMImpl.impl.getScrollLeft(this);
  }

  /**
   * The number of pixels that an element's content is scrolled from the top.
   */
  public final int getScrollTop() {
    return toInt32(getSubPixelScrollTop());
  }

  /**
   * The width of the scroll view of an element.
   */
  public final int getScrollWidth() {
    return toInt32(getSubPixelScrollWidth());
  }

  /**
   * Gets a string representation of this element (as outer HTML).
   * 
   * We do not override {@link #toString()} because it is final in
   * {@link com.google.gwt.core.client.JavaScriptObject}.
   * 
   * @return the string representation of this element
   */
  public final String getString() {
    return DOMImpl.impl.toString(this);
  }

  /**
   * Gets this element's {@link Style} object.
   */
  public final native Style getStyle() /*-{
     return this.style;
   }-*/;

  /**
   * The index that represents the element's position in the tabbing order.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-tabindex">W3C HTML Specification</a>
   */
  public final int getTabIndex() {
    return DOMImpl.impl.getTabIndex(this);
  }

  /**
   * Gets the element's full tag name, including the namespace-prefix if
   * present.
   * 
   * @return the element's tag name
   */
  public final String getTagName() {
    return DOMImpl.impl.getTagName(this);
  }

  /**
   * The element's advisory title.
   */
  public final native String getTitle() /*-{
     return this.title;
   }-*/;

  /**
   * Determines whether an element has an attribute with a given name.
   *
   * <p>
   * Note that IE, prior to version 8, will return false-positives for names
   * that collide with element properties (e.g., style, width, and so forth).
   * </p>
   * 
   * @param name the name of the attribute
   * @return <code>true</code> if this element has the specified attribute
   */
  public final boolean hasAttribute(String name) {
    return DOMImpl.impl.hasAttribute(this, name);
  }

  /**
   * Checks if this element's class property contains specified class name.
   *
   * @param className the class name to be added
   * @return <code>true</code> if this element has the specified class name
   */
  public final boolean hasClassName(String className) {
    className = trimClassName(className);
    int idx = indexOfName(getClassName(), className);
    return idx != -1;
  }

  /**
   * Determines whether this element has the given tag name.
   * 
   * @param tagName the tag name, including namespace-prefix (if present)
   * @return <code>true</code> if the element has the given tag name
   */
  public final boolean hasTagName(String tagName) {
    assert tagName != null : "tagName must not be null";
    return tagName.equalsIgnoreCase(getTagName());
  }

  /**
   * Removes an attribute by name.
   */
  public final native void removeAttribute(String name) /*-{
     this.removeAttribute(name);
   }-*/;

  /**
   * Removes a name from this element's class property. If the name is not
   * present, this method has no effect.
   * 
   * @param className the class name to be removed
   * @return <code>true</code> if this element had the specified class name
   * @see #setClassName(String)
   */
  public final boolean removeClassName(String className) {
    className = trimClassName(className);

    // Get the current style string.
    String oldStyle = getClassName();
    int idx = indexOfName(oldStyle, className);

    // Don't try to remove the style if it's not there.
    if (idx != -1) {
      // Get the leading and trailing parts, without the removed name.
      String begin = oldStyle.substring(0, idx).trim();
      String end = oldStyle.substring(idx + className.length()).trim();

      // Some contortions to make sure we don't leave extra spaces.
      String newClassName;
      if (begin.length() == 0) {
        newClassName = end;
      } else if (end.length() == 0) {
        newClassName = begin;
      } else {
        newClassName = begin + " " + end;
      }

      setClassName(newClassName);
      return true;
    }
    return false;
  }

  /**
   * Returns the index of the first occurrence of name in a space-separated list of names,
   * or -1 if not found.
   *
   * @param nameList list of space delimited names
   * @param name a non-empty string.  Should be already trimmed.
   */
  static int indexOfName(String nameList, String name) {
    int idx = nameList.indexOf(name);

    // Calculate matching index.
    while (idx != -1) {
      if (idx == 0 || nameList.charAt(idx - 1) == ' ') {
        int last = idx + name.length();
        int lastPos = nameList.length();
        if ((last == lastPos)
            || ((last < lastPos) && (nameList.charAt(last) == ' '))) {
          break;
        }
      }
      idx = nameList.indexOf(name, idx + 1);
    }

    return idx;
  }

  private static String trimClassName(String className) {
    assert (className != null) : "Unexpectedly null class name";
    className = className.trim();
    assert !className.isEmpty() : "Unexpectedly empty class name";
    return className;
  }

  /**
   * Add the class name if it doesn't exist or removes it if does.
   *
   * @param className the class name to be toggled
   */
  public final void toggleClassName(String className) {
    boolean added = addClassName(className);
    if (!added) {
      removeClassName(className);
    }
  }

  /**
   * Replace one class name with another.
   *
   * @param oldClassName the class name to be replaced
   * @param newClassName the class name to replace it
   */
  public final void replaceClassName(String oldClassName, String newClassName) {
    removeClassName(oldClassName);
    addClassName(newClassName);
  }

  /**
   * Scrolls this element into view.
   * 
   * <p>
   * This method crawls up the DOM hierarchy, adjusting the scrollLeft and
   * scrollTop properties of each scrollable element to ensure that the
   * specified element is completely in view. It adjusts each scroll position by
   * the minimum amount necessary.
   * </p>
   */
  public final void scrollIntoView() {
    DOMImpl.impl.scrollIntoView(this);
  }

  /**
   * Adds a new attribute. If an attribute with that name is already present in
   * the element, its value is changed to be that of the value parameter.
   * 
   * @param name The name of the attribute to create or alter
   * @param value Value to set in string form
   */
  public final native void setAttribute(String name, String value) /*-{
     this.setAttribute(name, value);
   }-*/;

  /**
   * The class attribute of the element. This attribute has been renamed due to
   * conflicts with the "class" keyword exposed by many languages.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-class">W3C
   *      HTML Specification</a>
   */
  public final native void setClassName(String className) /*-{
     this.className = className || "";
   }-*/;

  /**
   * Specifies the base direction of directionally neutral text and the
   * directionality of tables.
   */
  public final native void setDir(String dir) /*-{
     this.dir = dir;
   }-*/;

  /**
   * Changes the draggable attribute to one of {@link #DRAGGABLE_AUTO},
   * {@link #DRAGGABLE_FALSE}, or {@link #DRAGGABLE_TRUE}.
   * 
   * @param draggable a String constants
   */
  public final void setDraggable(String draggable) {
    DOMImpl.impl.setDraggable(this, draggable);
  }

  /**
   * The element's identifier.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-id">W3C
   *      HTML Specification</a>
   */
  public final native void setId(String id) /*-{
     this.id = id;
   }-*/;

  /**
   * All of the markup and content within a given element.
   */
  public final native void setInnerHTML(String html) /*-{
     this.innerHTML = html || '';
   }-*/;

  /**
   * All of the markup and content within a given element.
   */
  public final void setInnerSafeHtml(SafeHtml html) {
    setInnerHTML(html.asString());
  }

  /**
   * The text between the start and end tags of the object.
   */
  public final void setInnerText(String text) {
    DOMImpl.impl.setInnerText(this, text);
  }

  /**
   * Language code defined in RFC 1766.
   */
  public final native void setLang(String lang) /*-{
     this.lang = lang;
   }-*/;

  /**
   * Sets a boolean property on this element.
   * 
   * @param name the name of the property to be set
   * @param value the new property value
   */
  public final native void setPropertyBoolean(String name, boolean value) /*-{
     this[name] = value;
   }-*/;

  /**
   * Sets a double property on this element.
   * 
   * @param name the name of the property to be set
   * @param value the new property value
   */
  public final native void setPropertyDouble(String name, double value) /*-{
     this[name] = value;
   }-*/;

  /**
   * Sets an integer property on this element.
   * 
   * @param name the name of the property to be set
   * @param value the new property value
   */
  public final native void setPropertyInt(String name, int value) /*-{
     this[name] = value;
   }-*/;

  /**
   * Sets a JSO property on this element.
   *
   * @param name the name of the property to be set
   * @param value the new property value
   */
  public final native void setPropertyJSO(String name, JavaScriptObject value) /*-{
    this[name] = value;
  }-*/;

  /**
   * Sets an object property on this element.
   *
   * @param name the name of the property to be set
   * @param value the new property value
   */
  public final native void setPropertyObject(String name, Object value) /*-{
    this[name] = value;
  }-*/;

  /**
   * Sets a property on this element.
   * 
   * @param name the name of the property to be set
   * @param value the new property value
   */
  public final native void setPropertyString(String name, String value) /*-{
     this[name] = value;
   }-*/;

  /**
   * The number of pixels that an element's content is scrolled to the left.
   */
  public final void setScrollLeft(int scrollLeft) {
    DOMImpl.impl.setScrollLeft(this, scrollLeft);
  }

  /**
   * The number of pixels that an element's content is scrolled to the top.
   */
  public final native void setScrollTop(int scrollTop) /*-{
     this.scrollTop = scrollTop;
   }-*/;

  /**
   * The index that represents the element's position in the tabbing order.
   * 
   * @see <a href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-tabindex">W3C HTML Specification</a>
   */
  public final native void setTabIndex(int tabIndex) /*-{
    this.tabIndex = tabIndex;
  }-*/;

  /**
   * The element's advisory title.
   */
  public final native void setTitle(String title) /*-{
     // Setting the title to null results in the string "null" being displayed
     // on some browsers.
     this.title = title || '';
   }-*/;

  private final native double getSubPixelClientHeight() /*-{
    return this.clientHeight;
  }-*/;

  private final native double getSubPixelClientWidth() /*-{
    return this.clientWidth;
  }-*/;

  private final native double getSubPixelOffsetHeight() /*-{
     return this.offsetHeight || 0;
   }-*/;

  private final native double getSubPixelOffsetLeft() /*-{
     return this.offsetLeft || 0;
  }-*/;

  private final native double getSubPixelOffsetTop() /*-{
    return this.offsetTop || 0;
  }-*/;

  private final native double getSubPixelOffsetWidth() /*-{
    return this.offsetWidth || 0;
  }-*/;

  private final native double getSubPixelScrollHeight() /*-{
    return this.scrollHeight || 0;
  }-*/;

  private final native double getSubPixelScrollTop() /*-{
    return this.scrollTop || 0;
  }-*/;

  private final native double getSubPixelScrollWidth() /*-{
    return this.scrollWidth || 0;
  }-*/;
}
