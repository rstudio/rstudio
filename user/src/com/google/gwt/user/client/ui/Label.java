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
import com.google.gwt.i18n.client.HasDirection;
import com.google.gwt.i18n.client.BidiUtils;

/**
 * A widget that contains arbitrary text, <i>not</i> interpreted as HTML.
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-Label { }</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.HTMLExample}
 * </p>
 */
public class Label extends Widget implements SourcesClickEvents,
    SourcesMouseEvents, SourcesMouseWheelEvents, HasHorizontalAlignment,
    HasText, HasWordWrap, HasDirection {

  private ClickListenerCollection clickListeners;
  private HorizontalAlignmentConstant horzAlign;
  private MouseListenerCollection mouseListeners;
  private MouseWheelListenerCollection mouseWheelListeners;

  /**
   * Creates an empty label.
   */
  public Label() {
    setElement(DOM.createDiv());
    setStyleName("gwt-Label");
  }

  /**
   * This constructor is used to let the HTML constructors avoid work.
   * 
   * @param element element
   */
  Label(Element element) {
    setElement(element);
  }

  /**
   * Creates a label with the specified text.
   * 
   * @param text the new label's text
   */
  public Label(String text) {
    this();
    setText(text);
  }

  /**
   * Creates a label with the specified text.
   * 
   * @param text the new label's text
   * @param wordWrap <code>false</code> to disable word wrapping
   */
  public Label(String text, boolean wordWrap) {
    this(text);
    setWordWrap(wordWrap);
  }

  public void addClickListener(ClickListener listener) {
    if (clickListeners == null) {
      clickListeners = new ClickListenerCollection();
      sinkEvents(Event.ONCLICK);
    }
    clickListeners.add(listener);
  }

  public void addMouseListener(MouseListener listener) {
    if (mouseListeners == null) {
      mouseListeners = new MouseListenerCollection();
      sinkEvents(Event.MOUSEEVENTS);
    }
    mouseListeners.add(listener);
  }

  public void addMouseWheelListener(MouseWheelListener listener) {
    if (mouseWheelListeners == null) {
      mouseWheelListeners = new MouseWheelListenerCollection();
      sinkEvents(Event.ONMOUSEWHEEL);
    }
    mouseWheelListeners.add(listener);
  }

  public Direction getDirection() {
    return BidiUtils.getDirectionOnElement(getElement());
  }
  
  public HorizontalAlignmentConstant getHorizontalAlignment() {
    return horzAlign;
  }

  public String getText() {
    return DOM.getInnerText(getElement());
  }

  public boolean getWordWrap() {
    return !DOM.getStyleAttribute(getElement(), "whiteSpace").equals("nowrap");
  }

  @Override
  public void onBrowserEvent(Event event) {
    switch (DOM.eventGetType(event)) {
      case Event.ONCLICK:
        if (clickListeners != null) {
          clickListeners.fireClick(this);
        }
        break;

      case Event.ONMOUSEDOWN:
      case Event.ONMOUSEUP:
      case Event.ONMOUSEMOVE:
      case Event.ONMOUSEOVER:
      case Event.ONMOUSEOUT:
        if (mouseListeners != null) {
          mouseListeners.fireMouseEvent(this, event);
        }
        break;

      case Event.ONMOUSEWHEEL:
        if (mouseWheelListeners != null) {
          mouseWheelListeners.fireMouseWheelEvent(this, event);
        }
        break;
    }
  }

  public void removeClickListener(ClickListener listener) {
    if (clickListeners != null) {
      clickListeners.remove(listener);
    }
  }

  public void removeMouseListener(MouseListener listener) {
    if (mouseListeners != null) {
      mouseListeners.remove(listener);
    }
  }

  public void removeMouseWheelListener(MouseWheelListener listener) {
    if (mouseWheelListeners != null) {
      mouseWheelListeners.remove(listener);
    }
  }
  
  public void setDirection(Direction direction) {
    BidiUtils.setDirectionOnElement(getElement(), direction);
  }
  
  public void setHorizontalAlignment(HorizontalAlignmentConstant align) {
    horzAlign = align;
    DOM.setStyleAttribute(getElement(), "textAlign", align.getTextAlignString());
  }

  public void setText(String text) {
    DOM.setInnerText(getElement(), text);
  }

  public void setWordWrap(boolean wrap) {
    DOM.setStyleAttribute(getElement(), "whiteSpace", wrap ? "normal"
        : "nowrap");
  }
}
