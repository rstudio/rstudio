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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * An absolute panel positions all of its children absolutely, allowing them to
 * overlap.
 * 
 * <p>
 * Note that this panel will not automatically resize itself to allow enough
 * room for its absolutely-positioned children. It must be explicitly sized in
 * order to make room for them.
 * </p>
 * 
 * <p>
 * Once a widget has been added to an absolute panel, the panel effectively
 * "owns" the positioning of the widget. Any existing positioning attributes on
 * the widget may be modified by the panel.
 * </p>
 * 
 * <h3>Use in UiBinder Templates</h3>
 * <p>
 * AbsolutePanel elements in {@link com.google.gwt.uibinder.client.UiBinder
 * UiBinder} templates lay out their children with absolute position, using
 * &lt;g:at> elements. Each at element should have <code>left</code> and
 * <code>top</code> attributes in pixels. They also can contain widget children
 * directly, with no position specified.
 * 
 * <p>
 * For example:
 * 
 * <pre>
 * &lt;g:AbsolutePanel>
 *   &lt;g:at left='10' top='20'>
 *     &lt;g:Label>Lorem ipsum...&lt;/g:Label>
 *   &lt;/g:at>
 *   &lt;g:Label>...dolores est.&lt;/g:Label>
 * &lt;/g:AbsolutePanel>
 * </pre>
 */
public class AbsolutePanel extends ComplexPanel implements
    InsertPanel.ForIsWidget {

  /**
   * Changes a DOM element's positioning to static.
   * 
   * @param elem the DOM element
   */
  private static void changeToStaticPositioning(Element elem) {
    DOM.setStyleAttribute(elem, "left", "");
    DOM.setStyleAttribute(elem, "top", "");
    DOM.setStyleAttribute(elem, "position", "");
  }

  /**
   * Creates an empty absolute panel.
   */
  public AbsolutePanel() {
    this(DOM.createDiv());

    // Setting the panel's position style to 'relative' causes it to be treated
    // as a new positioning context for its children.
    DOM.setStyleAttribute(getElement(), "position", "relative");
    DOM.setStyleAttribute(getElement(), "overflow", "hidden");
  }

  /**
   * Creates an AbsolutePanel with the given element. This is protected so that
   * it can be used by {@link RootPanel} or a subclass that wants to substitute
   * another element. The element is presumed to be a &lt;div&gt;.
   * 
   * @param elem the element to be used for this panel
   */
  protected AbsolutePanel(Element elem) {
    setElement(elem);
  }

  @Override
  public void add(Widget w) {
    super.add(w, getElement());
  }
  
  /**
   * Adds a widget to the panel at the specified position. Setting a position of
   * <code>(-1, -1)</code> will cause the child widget to be positioned
   * statically.
   * 
   * @param w the widget to be added
   * @param left the widget's left position
   * @param top the widget's top position
   */
  public void add(Widget w, int left, int top) {
    // In order to avoid the potential for a flicker effect, it is necessary
    // to set the position of the widget before adding it to the AbsolutePanel.
    // The Widget should be removed from its parent before any positional
    // changes are made to prevent flickering.
    w.removeFromParent();
    int beforeIndex = getWidgetCount();
    setWidgetPositionImpl(w, left, top);
    insert(w, beforeIndex);
    verifyPositionNotStatic(w);
  }
  
  /**
   * Overloaded version for IsWidget.
   * 
   * @see #add(Widget,int,int)
   */
  public void add(IsWidget w, int left, int top) {
    this.add(w.asWidget(),left,top);
  }

  /**
   * Gets the position of the left outer border edge of the widget relative to
   * the left outer border edge of the panel.
   * 
   * @param w the widget whose position is to be retrieved
   * @return the widget's left position
   */
  public int getWidgetLeft(Widget w) {
    checkWidgetParent(w);
    return DOM.getAbsoluteLeft(w.getElement())
        - DOM.getAbsoluteLeft(getElement());
  }

  /**
   * Gets the position of the top outer border edge of the widget relative to
   * the top outer border edge of the panel.
   * 
   * @param w the widget whose position is to be retrieved
   * @return the widget's top position
   */
  public int getWidgetTop(Widget w) {
    checkWidgetParent(w);
    return DOM.getAbsoluteTop(w.getElement())
        - DOM.getAbsoluteTop(getElement());
  }

  public void insert(Widget w, int beforeIndex) {
    insert(w, getElement(), beforeIndex, true);
  }
  
  /**
   * Convenience overload to allow {@link IsWidget} to be used directly.
   */
  public void insert(IsWidget w, int beforeIndex) {
    insert(asWidgetOrNull(w), beforeIndex);
  }

  /**
   * Inserts a child widget at the specified position before the specified
   * index. Setting a position of <code>(-1, -1)</code> will cause the child
   * widget to be positioned statically. If the widget is already a child of
   * this panel, it will be moved to the specified index.
   * 
   * @param w the child widget to be inserted
   * @param left the widget's left position
   * @param top the widget's top position
   * @param beforeIndex the index before which it will be inserted
   * @throws IndexOutOfBoundsException if <code>beforeIndex</code> is out of
   *           range
   */
  public void insert(Widget w, int left, int top, int beforeIndex) {
    // In order to avoid the potential for a flicker effect, it is necessary
    // to set the position of the widget before adding it to the AbsolutePanel.
    // The Widget should be removed from its parent before any positional
    // changes are made to prevent flickering.
    w.removeFromParent();
    setWidgetPositionImpl(w, left, top);
    insert(w, beforeIndex);
    verifyPositionNotStatic(w);
  }

  /**
   * Overrides {@link ComplexPanel#remove(Widget)} to change the removed
   * Widget's element back to static positioning.This is done so that any
   * positioning changes to the widget that were done by the panel are undone
   * when the widget is disowned from the panel.
   */
  @Override
  public boolean remove(Widget w) {
    boolean removed = super.remove(w);
    if (removed) {
      changeToStaticPositioning(w.getElement());
    }
    return removed;
  }

  /**
   * Sets the position of the specified child widget. Setting a position of
   * <code>(-1, -1)</code> will cause the child widget to be positioned
   * statically.
   * 
   * @param w the child widget to be positioned
   * @param left the widget's left position
   * @param top the widget's top position
   */
  public void setWidgetPosition(Widget w, int left, int top) {
    checkWidgetParent(w);
    setWidgetPositionImpl(w, left, top);
    verifyPositionNotStatic(w);
  }

  protected void setWidgetPositionImpl(Widget w, int left, int top) {
    Element h = w.getElement();
    if (left == -1 && top == -1) {
      changeToStaticPositioning(h);
    } else {
      DOM.setStyleAttribute(h, "position", "absolute");
      DOM.setStyleAttribute(h, "left", left + "px");
      DOM.setStyleAttribute(h, "top", top + "px");
    }
  }

  private void checkWidgetParent(Widget w) {
    if (w.getParent() != this) {
      throw new IllegalArgumentException(
          "Widget must be a child of this panel.");
    }
  }

  /**
   * Verify that the given widget is not statically positioned on the page
   * (relative to the document window), unless the widget is in fact directly
   * attached to the document BODY. Note that the current use of this method is
   * not comprehensive, since we can only verify the offsetParent if both parent
   * (AbsolutePanel) and child widget are both visible and attached to the DOM
   * when this test is executed.
   *
   * @param child the widget whose position and placement should be tested
   */
  private void verifyPositionNotStatic(Widget child) {
    // Only verify widget position in Development Mode
    if (GWT.isProdMode()) {
      return;
    }
    
    // Make sure we can actually perform a check
    if (!isAttached()) {
      return;
    }

    // Non-visible or detached elements have no offsetParent
    if (child.getElement().getOffsetParent() == null) {
      return;
    }
    
    // Check if offsetParent == parent
    if (child.getElement().getOffsetParent() == getElement()) {
      return;
    }

    /*
     * When this AbsolutePanel is the document BODY, e.g. RootPanel.get(), then
     * no explicit position:relative is needed as children are already
     * positioned relative to their parent. For simplicity we test against
     * parent, not offsetParent, since in IE6+IE7 (but not IE8+) standards mode,
     * the offsetParent, for elements whose parent is the document BODY, is the
     * HTML element, not the BODY element.
     */
    if ("body".equals(getElement().getNodeName().toLowerCase())) {
      return;
    }

    /*
     * Warn the developer, but allow the execution to continue in case legacy
     * apps depend on broken CSS.
     */
    String className = getClass().getName();
    GWT.log("Warning: " + className + " descendants will be incorrectly "
        + "positioned, i.e. not relative to their parent element, when "
        + "'position:static', which is the CSS default, is in effect. One "
        + "possible fix is to call "
        + "'panel.getElement().getStyle().setPosition(Position.RELATIVE)'.",
        // Stack trace provides context for the developer
        new IllegalStateException(className
            + " is missing CSS 'position:{relative,absolute,fixed}'"));
  }
}
