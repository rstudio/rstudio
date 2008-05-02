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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;

/**
 * A form of popup that has a caption area at the top and can be dragged by the
 * user. Unlike a PopupPanel, calls to {@link #setWidth(String)} and
 * {@link #setHeight(String)} will set the width and height of the dialog box
 * itself, even if a widget has not been added as yet.
 * <p>
 * <img class='gallery' src='DialogBox.png'/>
 * </p>
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-DialogBox { the outside of the dialog }</li>
 * <li>.gwt-DialogBox .Caption { the caption }</li>
 * <li>.gwt-DialogBox .content { the content }</li>
 * </ul>
 * <p>
 * The styles that apply to {@link DecoratorPanel} also apply to DialogBox.
 * </p>
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.DialogBoxExample}
 * </p>
 */
public class DialogBox extends DecoratedPopupPanel implements HasHTML, HasText,
    MouseListener {
  /**
   * The default style name.
   */
  private static final String DEFAULT_STYLENAME = "gwt-DialogBox";
  
  private HTML caption = new HTML();
  private boolean dragging;
  private int dragStartX, dragStartY;

  /**
   * Creates an empty dialog box. It should not be shown until its child widget
   * has been added using {@link #add(Widget)}.
   */
  public DialogBox() {
    this(false);
  }

  /**
   * Creates an empty dialog box specifying its "auto-hide" property. It should
   * not be shown until its child widget has been added using
   * {@link #add(Widget)}.
   * 
   * @param autoHide <code>true</code> if the dialog should be automatically
   *          hidden when the user clicks outside of it
   */
  public DialogBox(boolean autoHide) {
    this(autoHide, true);
  }

  /**
   * Creates an empty dialog box specifying its "auto-hide" property. It should
   * not be shown until its child widget has been added using
   * {@link #add(Widget)}.
   * 
   * @param autoHide <code>true</code> if the dialog should be automatically
   *          hidden when the user clicks outside of it
   * @param modal <code>true</code> if keyboard and mouse events for widgets
   *          not contained by the dialog should be ignored
   */
  public DialogBox(boolean autoHide, boolean modal) {
    super(autoHide, modal);

    // Add the caption to the top row of the decorator panel.  We need to
    // logically adopt the caption so we can catch mouse events. 
    Element td = getCellElement(0, 1);
    DOM.appendChild(td, caption.getElement());
    adopt(caption);
    caption.setStyleName("Caption");
    caption.addMouseListener(this);

    // Set the style name
    setStyleName(DEFAULT_STYLENAME);
  }

  public String getHTML() {
    return caption.getHTML();
  }

  public String getText() {
    return caption.getText();
  }

  @Override
  public boolean onEventPreview(Event event) {
    // We need to preventDefault() on mouseDown events (outside of the
    // DialogBox content) to keep text from being selected when it
    // is dragged.
    if (DOM.eventGetType(event) == Event.ONMOUSEDOWN) {
      if (DOM.isOrHasChild(caption.getElement(), DOM.eventGetTarget(event))) {
        DOM.eventPreventDefault(event);
      }
    }

    return super.onEventPreview(event);
  }

  public void onMouseDown(Widget sender, int x, int y) {
    dragging = true;
    DOM.setCapture(caption.getElement());
    dragStartX = x;
    dragStartY = y;
  }

  public void onMouseEnter(Widget sender) {
  }

  public void onMouseLeave(Widget sender) {
  }

  public void onMouseMove(Widget sender, int x, int y) {
    if (dragging) {
      int absX = x + getAbsoluteLeft();
      int absY = y + getAbsoluteTop();
      setPopupPosition(absX - dragStartX, absY - dragStartY);
    }
  }

  public void onMouseUp(Widget sender, int x, int y) {
    dragging = false;
    DOM.releaseCapture(caption.getElement());
  }

  /**
   * Sets the html string inside the caption.
   * 
   * Use {@link #setWidget(Widget)} to set the contents inside the
   * {@link DialogBox}.
   * 
   * @param html the object's new HTML
   */
  public void setHTML(String html) {
    caption.setHTML(html);
  }

  /**
   * Sets the text inside the caption.
   * 
   * Use {@link #setWidget(Widget)} to set the contents inside the
   * {@link DialogBox}.
   * 
   * @param text the object's new text
   */
  public void setText(String text) {
    caption.setText(text);
  }
  
  @Override
  protected void doAttachChildren() {
    super.doAttachChildren();
    
    // See comment in doDetachChildren for an explanation of this call
    caption.onAttach();
  }

  @Override
  protected void doDetachChildren() {
    super.doDetachChildren();
    
    // We need to detach the caption specifically because it is not part of the
    // iterator of Widgets that the {@link SimplePanel} super class returns.
    // This is similar to a {@link ComplexPanel}, but we do not want to expose
    // the caption widget, as its just an internal implementation.
    caption.onDetach();
  }

  /**
   * <b>Affected Elements:</b>
   * <ul>
   * <li>-caption = text at the top of the {@link DialogBox}.</li>
   * <li>-content = the container around the content.</li>
   * </ul>
   * 
   * @see UIObject#onEnsureDebugId(String)
   */
  @Override
  protected void onEnsureDebugId(String baseID) {
    super.onEnsureDebugId(baseID);
    caption.ensureDebugId(baseID + "-caption");
    ensureDebugId(getCellElement(1, 1), baseID, "content");
  }
}
