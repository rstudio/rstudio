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
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.impl.StringCase;
import com.google.gwt.debug.client.DebugInfo;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;

/**
 * The superclass for all user-interface objects. It simply wraps a DOM element,
 * and cannot receive events. Most interesting user-interface classes derive
 * from {@link com.google.gwt.user.client.ui.Widget}.
 * 
 * <h3>Styling With CSS</h3>
 * <p>
 * All <code>UIObject</code> objects can be styled using CSS. Style names that
 * are specified programmatically in Java source are implicitly associated with
 * CSS style rules. In terms of HTML and CSS, a GWT style name is the element's
 * CSS "class". By convention, GWT style names are of the form
 * <code>[project]-[widget]</code>.
 * </p>
 * 
 * <p>
 * For example, the {@link Button} widget has the style name
 * <code>gwt-Button</code>, meaning that within the <code>Button</code>
 * constructor, the following call occurs:
 * 
 * <pre class="code">
 * setStyleName("gwt-Button");</pre>
 * 
 * A corresponding CSS style rule can then be written as follows:
 * 
 * <pre class="code">
 * // Example of how you might choose to style a Button widget 
 * .gwt-Button {
 *   background-color: yellow;
 *   color: black;
 *   font-size: 24pt;
 * }</pre>
 * 
 * Note the dot prefix in the CSS style rule. This syntax is called a <a
 * href="http://www.w3.org/TR/REC-CSS2/selector.html#class-html">CSS class
 * selector</a>.
 * </p>
 * 
 * <h3>Style Name Specifics</h3>
 * <p>
 * Every <code>UIObject</code> has a <i>primary style name</i> that identifies
 * the key CSS style rule that should always be applied to it. Use
 * {@link #setStylePrimaryName(String)} to specify an object's primary style
 * name. In most cases, the primary style name is set in a widget's constructor
 * and never changes again during execution. In the case that no primary style
 * name is specified, it defaults to the first style name that is added.
 * </p>
 * 
 * <p>
 * More complex styling behavior can be achieved by manipulating an object's
 * <i>secondary style names</i>. Secondary style names can be added and removed
 * using {@link #addStyleName(String)}, {@link #removeStyleName(String)}, or
 * {@link #setStyleName(String, boolean)}. The purpose of secondary style names
 * is to associate a variety of CSS style rules over time as an object
 * progresses through different visual states.
 * </p>
 * 
 * <p>
 * There is an important special formulation of secondary style names called
 * <i>dependent style names</i>. A dependent style name is a secondary style
 * name prefixed with the primary style name of the widget itself. See
 * {@link #addStyleName(String)} for details.
 * </p>
 * 
 * <h3>Use in UiBinder Templates</h3>
 * <p>
 * Setter methods that follow JavaBean property conventions are exposed as
 * attributes in {@link com.google.gwt.uibinder.client.UiBinder UiBinder}
 * templates. For example, because UiObject implements {@link #setWidth(String)}
 * you can set the width of any widget like so:
 * 
 * <pre>
 * &lt;g:Label width='15em'>Hello there&lt;/g:Label></pre>
 * 
 * Generally speaking, values are parsed as if they were Java literals, so
 * methods like {@link #setVisible(boolean)} are also available:
 * 
 * <pre>
 * &lt;g:Label width='15em' visible='false'>Hello there&lt;/g:Label></pre>
 * Enum properties work this way too. Imagine a Bagel widget with a handy Type
 * enum and a setType(Type) method:
 * 
 * <pre>
 * enum Type { poppy, sesame, raisin, jalapeno }
 * 
 * &lt;my:Bagel type='poppy' /></pre>
 * 
 * There is also special case handling for two common method signatures,
 * <code>(int, int)</code> and <code>(double, {@link 
 * com.google.gwt.dom.client.Style.Unit Unit})</code>
 * 
 * <pre>
 * &lt;g:Label pixelSize='100, 100'>Hello there&lt;/g:Label></pre>
 * 
 * Finally, a few UiObject methods get special handling. The debug id (see
 * {@link #ensureDebugId}) of any UiObject can be set via the
 * <code>debugId</code> attribute, and addtional style names and dependent style
 * names can be set with the <code>addStyleNames</code> and
 * <code>addStyleDependentNames</code> attributes.<pre>
 * &lt;g:Label debugId='helloLabel' 
 *     addStyleNames='pretty rounded big'>Hello there&lt;/g:Label></pre>
 * 
 * Style names can be space or comma separated.
 */
public abstract class UIObject implements HasVisibility {

  /**
   * Stores a regular expression object to extract float values from the
   * leading portion of an input string.
   */
  private static JavaScriptObject numberRegex;

  /*
   * WARNING: For historical reasons, there are two Element classes being used
   * in this code. The dom.Element (com.google.gwt.dom.client.Element) class is
   * explicitly imported, while user.Element
   * (com.google.gwt.user.client.Element) is fully-qualified in the code.
   * 
   * All new methods should use dom.Element, because user.Element extends it but
   * adds no methods.
   */

  /**
   * The implementation of the set debug id method, which does nothing by
   * default.
   */
  public static class DebugIdImpl {
    @SuppressWarnings("unused")
    // parameters
    public void ensureDebugId(UIObject uiObject, String id) {
    }

    @SuppressWarnings("unused")
    // parameters
    public void ensureDebugId(Element elem, String baseID, String id) {
    }
  }

  /**
   * The implementation of the setDebugId method, which sets the id of the
   * {@link Element}s in this {@link UIObject}.
   */
  public static class DebugIdImplEnabled extends DebugIdImpl {
    @Override
    public void ensureDebugId(UIObject uiObject, String id) {
      uiObject.onEnsureDebugId(id);
    }

    @Override
    public void ensureDebugId(Element elem, String baseID, String id) {
      assert baseID != null;
      baseID = (baseID.length() > 0) ? baseID + "-" : "";
      String prefix = DebugInfo.getDebugIdPrefix();
      String debugId = ((prefix == null) ? "" : prefix) + baseID + id;
      String attribute = DebugInfo.getDebugIdAttribute();
      if (DebugInfo.isDebugIdAsProperty()) {
        elem.setPropertyString(attribute, debugId);
      } else {
        elem.setAttribute(attribute, debugId);
      }
    }
  }

  public static final String DEBUG_ID_PREFIX = DebugInfo.DEFAULT_DEBUG_ID_PREFIX;

  static final String MISSING_ELEMENT_ERROR = "This UIObject's element is not set; "
      + "you may be missing a call to either Composite.initWidget() or "
      + "UIObject.setElement()";

  static final String SETELEMENT_TWICE_ERROR = "Element may only be set once";

  private static DebugIdImpl debugIdImpl = GWT.create(DebugIdImpl.class);

  private static final String EMPTY_STYLENAME_MSG = "Style names cannot be empty";

  private static final String NULL_HANDLE_MSG = "Null widget handle. If you "
      + "are creating a composite, ensure that initWidget() has been called.";

  /**
   * <p>
   * Ensure that elem has an ID property set, which allows it to integrate with
   * third-party libraries and test tools. If elem already has an ID, this
   * method WILL override it. The ID that you specify will be prefixed by the
   * static string {@link #DEBUG_ID_PREFIX}.
   * </p>
   * 
   * <p>
   * This method will be compiled out and will have no effect unless you inherit
   * the DebugID module in your gwt.xml file by adding the following line:
   * 
   * <pre class="code">
   * &lt;inherits name="com.google.gwt.user.Debug"/&gt;</pre>
   * </p>
   * 
   * @param elem the target {@link Element}
   * @param id the ID to set on the element
   */
  public static void ensureDebugId(Element elem, String id) {
    ensureDebugId(elem, "", id);
  }

  /**
   * Returns whether the given element is visible in a way consistent with
   * {@link #setVisible(Element, boolean)}.
   *
   * <p>
   * Warning: implemented with a heuristic. The value returned takes into
   * account only the "display" style, ignoring CSS and Aria roles, thus may not
   * accurately reflect whether the element is actually visible in the browser.
   * </p>
   */
  public static native boolean isVisible(Element elem) /*-{
    return (elem.style.display != 'none');
  }-*/;

  /**
   * Shows or hides the given element. Also updates the "aria-hidden" attribute
   * by setting it to true when it is hidden and removing it when it is shown.
   *
   * <p>
   * Warning: implemented with a heuristic based on the "display" style:
   * clears the "display" style to its default value if {@code visible} is true,
   * else forces the style to "none". If the "display" style is set to "none"
   * via CSS style sheets, the element remains invisible after a call to
   * {@code setVisible(elem, true)}.
   * </p>
   */
  public static native void setVisible(Element elem, boolean visible) /*-{
    elem.style.display = visible ? '' : 'none';
    if (visible) {
      elem.removeAttribute('aria-hidden');
    } else {
      elem.setAttribute('aria-hidden', 'true');
    }
  }-*/;

  /**
   * Set the debug id of a specific element. The id will be appended to the end
   * of the base debug id, with a dash separator. The base debug id is the ID of
   * the main element in this UIObject.
   * 
   * @param elem the element
   * @param baseID the base ID used by the main element
   * @param id the id to append to the base debug id
   */
  protected static void ensureDebugId(Element elem, String baseID, String id) {
    debugIdImpl.ensureDebugId(elem, baseID, id);
  }

  /**
   * Gets all of the element's style names, as a space-separated list.
   * 
   * @param elem the element whose style is to be retrieved
   * @return the objects's space-separated style names
   */
  protected static String getStyleName(Element elem) {
    return elem.getClassName();
  }

  /**
   * Gets the element's primary style name.
   * 
   * @param elem the element whose primary style name is to be retrieved
   * @return the element's primary style name
   */
  protected static String getStylePrimaryName(Element elem) {
    String fullClassName = getStyleName(elem);

    // The primary style name is always the first token of the full CSS class
    // name. There can be no leading whitespace in the class name, so it's not
    // necessary to trim() it.
    int spaceIdx = fullClassName.indexOf(' ');
    if (spaceIdx >= 0) {
      return fullClassName.substring(0, spaceIdx);
    }
    return fullClassName;
  }

  /**
   * Clears all of the element's style names and sets it to the given style.
   * 
   * @param elem the element whose style is to be modified
   * @param styleName the new style name
   */
  protected static void setStyleName(Element elem, String styleName) {
    elem.setClassName(styleName);
  }

  /**
   * This convenience method adds or removes a style name for a given element.
   * This method is typically used to add and remove secondary style names, but
   * it can be used to remove primary stylenames as well, but that is not
   * recommended. See {@link #setStyleName(String)} for a description of how
   * primary and secondary style names are used.
   * 
   * @param elem the element whose style is to be modified
   * @param style the secondary style name to be added or removed
   * @param add <code>true</code> to add the given style, <code>false</code> to
   *          remove it
   */
  protected static void setStyleName(Element elem, String style, boolean add) {
    if (elem == null) {
      throw new RuntimeException(NULL_HANDLE_MSG);
    }

    style = style.trim();
    if (style.length() == 0) {
      throw new IllegalArgumentException(EMPTY_STYLENAME_MSG);
    }

    if (add) {
      elem.addClassName(style);
    } else {
      elem.removeClassName(style);
    }
  }

  /**
   * Sets the element's primary style name and updates all dependent style
   * names.
   * 
   * @param elem the element whose style is to be reset
   * @param style the new primary style name
   * @see #setStyleName(Element, String, boolean)
   */
  protected static void setStylePrimaryName(Element elem, String style) {
    if (elem == null) {
      throw new RuntimeException(NULL_HANDLE_MSG);
    }

    // Style names cannot contain leading or trailing whitespace, and cannot
    // legally be empty.
    style = style.trim();
    if (style.length() == 0) {
      throw new IllegalArgumentException(EMPTY_STYLENAME_MSG);
    }

    updatePrimaryAndDependentStyleNames(elem, style);
  }

  /**
   * Replaces all instances of the primary style name with newPrimaryStyleName.
   */
  private static native void updatePrimaryAndDependentStyleNames(Element elem,
      String newPrimaryStyle) /*-{
    var classes = (elem.className || "").split(/\s+/);
    if (!classes) {
      return;
    }

    var oldPrimaryStyle = classes[0];
    var oldPrimaryStyleLen = oldPrimaryStyle.length;

    classes[0] = newPrimaryStyle;
    for (var i = 1, n = classes.length; i < n; i++) {
      var name = classes[i];
      if (name.length > oldPrimaryStyleLen
          && name.charAt(oldPrimaryStyleLen) == '-'
          && name.indexOf(oldPrimaryStyle) == 0) {
        classes[i] = newPrimaryStyle + name.substring(oldPrimaryStyleLen);
      }
    }
    elem.className = classes.join(" ");
  }-*/;

  private Element element;

  /**
   * Adds a dependent style name by specifying the style name's suffix. The
   * actual form of the style name that is added is:
   * 
   * <pre class="code">
   * getStylePrimaryName() + '-' + styleSuffix
   * </pre>
   * 
   * @param styleSuffix the suffix of the dependent style to be added.
   * @see #setStylePrimaryName(String)
   * @see #removeStyleDependentName(String)
   * @see #setStyleDependentName(String, boolean)
   * @see #addStyleName(String)
   */
  public void addStyleDependentName(String styleSuffix) {
    setStyleDependentName(styleSuffix, true);
  }

  /**
   * Adds a secondary or dependent style name to this object. A secondary style
   * name is an additional style name that is, in HTML/CSS terms, included as a
   * space-separated token in the value of the CSS <code>class</code> attribute
   * for this object's root element.
   * 
   * <p>
   * The most important use for this method is to add a special kind of
   * secondary style name called a <i>dependent style name</i>. To add a
   * dependent style name, use {@link #addStyleDependentName(String)}, which
   * will prefix the 'style' argument with the result of
   * {@link #getStylePrimaryName()} (followed by a '-'). For example, suppose
   * the primary style name is <code>gwt-TextBox</code>. If the following method
   * is called as <code>obj.setReadOnly(true)</code>:
   * </p>
   * 
   * <pre class="code">
   * public void setReadOnly(boolean readOnly) {
   *   isReadOnlyMode = readOnly;
   *   
   *   // Create a dependent style name.
   *   String readOnlyStyle = "readonly";
   *    
   *   if (readOnly) {
   *     addStyleDependentName(readOnlyStyle);
   *   } else {
   *     removeStyleDependentName(readOnlyStyle);
   *   }
   * }</pre>
   * 
   * <p>
   * then both of the CSS style rules below will be applied:
   * </p>
   * 
   * <pre class="code">
   *
   * // This rule is based on the primary style name and is always active.
   * .gwt-TextBox {
   *   font-size: 12pt;
   * }
   * 
   * // This rule is based on a dependent style name that is only active
   * // when the widget has called addStyleName(getStylePrimaryName() +
   * // "-readonly").
   * .gwt-TextBox-readonly {
   *   background-color: lightgrey;
   *   border: none;
   * }</pre>
   *
   * <p>
   * The code can also be simplified with
   * {@link #setStyleDependentName(String, boolean)}:
   * </p>
   *
   * <pre class="code">
   * public void setReadOnly(boolean readOnly) {
   *   isReadOnlyMode = readOnly;
   *   setStyleDependentName("readonly", readOnly);
   * }</pre>
   * 
   * <p>
   * Dependent style names are powerful because they are automatically updated
   * whenever the primary style name changes. Continuing with the example above,
   * if the primary style name changed due to the following call:
   * </p>
   * 
   * <pre class="code">setStylePrimaryName("my-TextThingy");</pre>
   * 
   * <p>
   * then the object would be re-associated with following style rules, removing
   * those that were shown above.
   * </p>
   * 
   * <pre class="code">
   * .my-TextThingy {
   *   font-size: 20pt;
   * }
   * 
   * .my-TextThingy-readonly {
   *   background-color: red;
   *   border: 2px solid yellow;
   * }</pre>
   * 
   * <p>
   * Secondary style names that are not dependent style names are not
   * automatically updated when the primary style name changes.
   * </p>
   * 
   * @param style the secondary style name to be added
   * @see UIObject
   * @see #removeStyleName(String)
   */
  public void addStyleName(String style) {
    setStyleName(style, true);
  }

  /**
   * Ensure that the main {@link Element} for this {@link UIObject} has an ID
   * property set, which allows it to integrate with third-party libraries and
   * test tools. Complex {@link Widget}s will also set the IDs of their
   * important sub-elements.
   * 
   * If the main element already has an ID, this method WILL override it.
   * 
   * The ID that you specify will be prefixed by the static string
   * {@link #DEBUG_ID_PREFIX}.
   * 
   * This method will be compiled out and will have no effect unless you inherit
   * the DebugID module in your gwt.xml file by adding the following line:
   * 
   * <pre class="code">
   * &lt;inherits name="com.google.gwt.user.Debug"/&gt;</pre>
   * 
   * @param id the ID to set on the main element
   */
  public final void ensureDebugId(String id) {
    debugIdImpl.ensureDebugId(this, id);
  }

  /**
   * Gets the object's absolute left position in pixels, as measured from the
   * browser window's client area.
   * 
   * @return the object's absolute left position
   */
  public int getAbsoluteLeft() {
    return getElement().getAbsoluteLeft();
  }

  /**
   * Gets the object's absolute top position in pixels, as measured from the
   * browser window's client area.
   * 
   * @return the object's absolute top position
   */
  public int getAbsoluteTop() {
    return getElement().getAbsoluteTop();
  }

  /**
   * Gets a handle to the object's underlying DOM element.
   * 
   * This method should not be overridden. It is non-final solely to support
   * legacy code that depends upon overriding it. If it is overridden, the
   * subclass implementation must not return a different element than was
   * previously set using {@link #setElement(Element)}.
   * 
   * @return the object's browser element
   */
  public com.google.gwt.user.client.Element getElement() {
    assert (element != null) : MISSING_ELEMENT_ERROR;
    return DOM.asOld(element);
  }

  /**
   * Gets the object's offset height in pixels. This is the total height of the
   * object, including decorations such as border and padding, but not margin.
   * 
   * @return the object's offset height
   */
  public int getOffsetHeight() {
    return getElement().getPropertyInt("offsetHeight");
  }

  /**
   * Gets the object's offset width in pixels. This is the total width of the
   * object, including decorations such as border and padding, but not margin.
   * 
   * @return the object's offset width
   */
  public int getOffsetWidth() {
    return getElement().getPropertyInt("offsetWidth");
  }

  /**
   * Gets all of the object's style names, as a space-separated list. If you
   * wish to retrieve only the primary style name, call
   * {@link #getStylePrimaryName()}.
   * 
   * @return the objects's space-separated style names
   * @see #getStylePrimaryName()
   */
  public String getStyleName() {
    return getStyleName(getStyleElement());
  }

  /**
   * Gets the primary style name associated with the object.
   * 
   * @return the object's primary style name
   * @see #setStyleName(String)
   * @see #addStyleName(String)
   * @see #removeStyleName(String)
   */
  public String getStylePrimaryName() {
    return getStylePrimaryName(getStyleElement());
  }

  /**
   * Gets the title associated with this object. The title is the 'tool-tip'
   * displayed to users when they hover over the object.
   * 
   * @return the object's title
   */
  public String getTitle() {
    return getElement().getPropertyString("title");
  }

  @Override
  public boolean isVisible() {
    return isVisible(getElement());
  }

  /**
   * Removes a dependent style name by specifying the style name's suffix.
   * 
   * @param styleSuffix the suffix of the dependent style to be removed
   * @see #setStylePrimaryName(Element, String)
   * @see #addStyleDependentName(String)
   * @see #setStyleDependentName(String, boolean)
   */
  public void removeStyleDependentName(String styleSuffix) {
    setStyleDependentName(styleSuffix, false);
  }

  /**
   * Removes a style name. This method is typically used to remove secondary
   * style names, but it can be used to remove primary stylenames as well. That
   * use is not recommended.
   * 
   * @param style the secondary style name to be removed
   * @see #addStyleName(String)
   * @see #setStyleName(String, boolean)
   */
  public void removeStyleName(String style) {
    setStyleName(style, false);
  }

  /**
   * Sets the object's height. This height does not include decorations such as
   * border, margin, and padding.
   * 
   * @param height the object's new height, in CSS units (e.g. "10px", "1em")
   */
  public void setHeight(String height) {
    // This exists to deal with an inconsistency in IE's implementation where
    // it won't accept negative numbers in length measurements
    assert extractLengthValue(StringCase.toLower(height.trim())) >= 0 : "CSS heights should not be negative";
    getElement().getStyle().setProperty("height", height);
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
   * Adds or removes a dependent style name by specifying the style name's
   * suffix. The actual form of the style name that is added is:
   * 
   * <pre class="code">
   * getStylePrimaryName() + '-' + styleSuffix
   * </pre>
   * 
   * @param styleSuffix the suffix of the dependent style to be added or removed
   * @param add <code>true</code> to add the given style, <code>false</code> to
   *          remove it
   * @see #setStylePrimaryName(Element, String)
   * @see #addStyleDependentName(String)
   * @see #setStyleName(String, boolean)
   * @see #removeStyleDependentName(String)
   */
  public void setStyleDependentName(String styleSuffix, boolean add) {
    setStyleName(getStylePrimaryName() + '-' + styleSuffix, add);
  }

  /**
   * Adds or removes a style name. This method is typically used to remove
   * secondary style names, but it can be used to remove primary stylenames as
   * well. That use is not recommended.
   * 
   * @param style the style name to be added or removed
   * @param add <code>true</code> to add the given style, <code>false</code> to
   *          remove it
   * @see #addStyleName(String)
   * @see #removeStyleName(String)
   */
  public void setStyleName(String style, boolean add) {
    setStyleName(getStyleElement(), style, add);
  }

  /**
   * Clears all of the object's style names and sets it to the given style. You
   * should normally use {@link #setStylePrimaryName(String)} unless you wish to
   * explicitly remove all existing styles.
   * 
   * @param style the new style name
   * @see #setStylePrimaryName(String)
   */
  public void setStyleName(String style) {
    setStyleName(getStyleElement(), style);
  }

  /**
   * Sets the object's primary style name and updates all dependent style names.
   * 
   * @param style the new primary style name
   * @see #addStyleName(String)
   * @see #removeStyleName(String)
   */
  public void setStylePrimaryName(String style) {
    setStylePrimaryName(getStyleElement(), style);
  }

  /**
   * Sets the title associated with this object. The title is the 'tool-tip'
   * displayed to users when they hover over the object.
   * 
   * @param title the object's new title
   */
  public void setTitle(String title) {
    if (title == null || title.length() == 0) {
      getElement().removeAttribute("title");
    } else {
      getElement().setAttribute("title", title);
    }
  }

  @Override
  public void setVisible(boolean visible) {
    setVisible(getElement(), visible);
  }

  /**
   * Sets the object's width. This width does not include decorations such as
   * border, margin, and padding.
   * 
   * @param width the object's new width, in CSS units (e.g. "10px", "1em")
   */
  public void setWidth(String width) {
    // This exists to deal with an inconsistency in IE's implementation where
    // it won't accept negative numbers in length measurements
    assert extractLengthValue(StringCase.toLower(width.trim())) >= 0 : "CSS widths should not be negative";
    getElement().getStyle().setProperty("width", width);
  }

  /**
   * Sinks a named event. Note that only {@link Widget widgets} may actually
   * receive events, but can receive events from all objects contained within
   * them.
   * 
   * @param eventTypeName name of the event to sink on this element
   * @see com.google.gwt.user.client.Event
   */
  public void sinkBitlessEvent(String eventTypeName) {
    DOM.sinkBitlessEvent(getElement(), eventTypeName);
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
  @Override
  public String toString() {
    if (element == null) {
      return "(null handle)";
    }
    return getElement().getString();
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
   * Template method that returns the element to which style names will be
   * applied. By default it returns the root element, but this method may be
   * overridden to apply styles to a child element.
   * 
   * @return the element to which style names will be applied
   */
  protected com.google.gwt.user.client.Element getStyleElement() {
    return getElement();
  }

  /**
   * Called when the user sets the id using the {@link #ensureDebugId(String)}
   * method. Subclasses of {@link UIObject} can override this method to add IDs
   * to their sub elements. If a subclass does override this method, it should
   * list the IDs (relative to the base ID), that will be applied to each sub
   * {@link Element} with a short description. For example:
   * <ul>
   * <li>-mysubelement = Applies to my sub element.</li>
   * </ul>
   * 
   * Subclasses should make a super call to this method to ensure that the ID of
   * the main element is set.
   * 
   * This method will not be called unless you inherit the DebugID module in
   * your gwt.xml file by adding the following line:
   * 
   * <pre class="code">
   * &lt;inherits name="com.google.gwt.user.Debug"/&gt;</pre>
   * 
   * @param baseID the base ID used by the main element
   */
  protected void onEnsureDebugId(String baseID) {
    ensureDebugId(getElement(), "", baseID);
  }

  /**
   * EXPERIMENTAL and subject to change. Do not use this in production code.
   * <p>
   * To be overridden by {@link IsRenderable} subclasses that initialize
   * themselves by by calling
   * <code>setElement(PotentialElement.build(this))</code>.
   * <p>
   * The receiver must:
   * <ul>
   * <li> create a real {@link Element} to replace its {@link PotentialElement}
   * <li> call {@link #setElement()} with the new Element
   * <li> and return the new Element
   * </ul>
   * <p>
   * This method is called when the receiver's element is about to be
   * added to a parent node, as a side effect of {@link DOM#appendChild}.
   * <p>
   * Note that this method is normally called only on the top element
   * of an IsRenderable tree. Children instead will receive {@link
   * IsRenderable#render} and {@link IsRenderable#claimElement(Element)}.
   *
   * @see PotentialElement
   * @see IsRenderable
   */
  protected Element resolvePotentialElement() {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets this object's browser element. UIObject subclasses must call this
   * method before attempting to call any other methods, and it may only be
   * called once.
   * 
   * @param elem the object's element
   */
  protected final void setElement(Element elem) {
    setElement(DOM.asOld(elem));
  }

  /**
   * Sets this object's browser element. UIObject subclasses must call this
   * method before attempting to call any other methods, and it may only be
   * called once.
   * 
   * This method exists for backwards compatibility with pre-1.5 code. As of GWT
   * 1.5, {@link #setElement(Element)} is the preferred method.
   * 
   * @param elem the object's element
   * @deprecated Use and override {@link #setElement(Element)} instead.
   */
  @Deprecated
  protected void setElement(com.google.gwt.user.client.Element elem) {
    assert (element == null || PotentialElement.isPotential(element)) : SETELEMENT_TWICE_ERROR;
    this.element = elem;
  }

  /**
   * Replaces this object's browser element.
   * 
   * This method exists only to support a specific use-case in Image, and should
   * not be used by other classes.
   * 
   * @param elem the object's new element
   */
  void replaceElement(Element elem) {
    if (element != null) {
      // replace this.element in its parent with elem.
      replaceNode(element, elem);
    }

    this.element = elem;
  }

  /**
   * Intended to be used to pull the value out of a CSS length. If the
   * value is "auto" or "inherit", 0 will be returned.
   * 
   * @param s The CSS length string to extract
   * @return The leading numeric portion of <code>s</code>, or 0 if "auto" or
   *         "inherit" are passed in.
   */
  private native double extractLengthValue(String s) /*-{
    if (s == "auto" || s == "inherit" || s == "") {
      return 0;
    } else {
      // numberRegex is similar to java.lang.Number.floatRegex, but divides
      // the string into a leading numeric portion followed by an arbitrary
      // portion.
      var numberRegex = @com.google.gwt.user.client.ui.UIObject::numberRegex;
      if (!numberRegex) {
        numberRegex = @com.google.gwt.user.client.ui.UIObject::numberRegex =
          /^(\s*[+-]?((\d+\.?\d*)|(\.\d+))([eE][+-]?\d+)?)(.*)$/;
      }

      // Extract the leading numeric portion of s
      s = s.replace(numberRegex, "$1");
      return parseFloat(s);
    }
  }-*/;

  private native void replaceNode(Element node, Element newNode) /*-{
    var p = node.parentNode;
    if (!p) {
      return;
    }
    p.insertBefore(newNode, node);
    p.removeChild(node);
  }-*/;
}
