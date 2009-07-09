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

import java.util.NoSuchElementException;

/**
 * A panel that contains HTML, and which can attach child widgets to identified
 * elements within that HTML.
 */
public class HTMLPanel extends ComplexPanel {

  private static Element hiddenDiv;

  /**
   * A helper method for creating unique IDs for elements within dynamically-
   * generated HTML. This is important because no two elements in a document
   * should have the same id.
   * 
   * @return a new unique identifier
   */
  public static String createUniqueId() {
    return DOM.createUniqueId();
  }

  /**
   * Creates an HTML panel with the specified HTML contents inside a DIV
   * element. Any element within this HTML that has a specified id can contain a
   * child widget.
   * 
   * @param html the panel's HTML
   */
  public HTMLPanel(String html) {
    this("div", html);
  }

  /**
   * Creates an HTML panel whose root element has the given tag, and with the
   * specified HTML contents. Any element within this HTML that has a specified
   * id can contain a child widget.
   * 
   * @param tag the tag of the root element
   * @param html the panel's HTML
   */
  public HTMLPanel(String tag, String html) {
    setElement(DOM.createElement(tag));
    DOM.setInnerHTML(getElement(), html);
  }
  
  /**
   * Adds a child widget to the panel, contained within the HTML element
   * specified by a given id.
   * 
   * @param widget the widget to be added
   * @param id the id of the element within which it will be contained
   */
  public void add(Widget widget, String id) {
    final Element elem = getElementById(id);

    if (elem == null) {
      throw new NoSuchElementException(id);
    }

    super.add(widget, elem);
  }

  /**
   * Adds a child widget to the panel, replacing the HTML element specified by a
   * given id.
   * 
   * @param widget the widget to be added
   * @param id the id of the element to be replaced by the widget
   */
  public void addAndReplaceElement(Widget widget, String id) {
    final Element toReplace = getElementById(id);

    if (toReplace == null) {
      throw new NoSuchElementException(id);
    }

    // Logic pulled from super.add(), replacing the element rather than adding.
    widget.removeFromParent();
    getChildren().add(widget);
    toReplace.getParentNode().replaceChild(widget.getElement(), toReplace);
    adopt(widget);
  }

  /**
   * Finds an {@link Element element} within this panel by its id.
   * 
   * This method uses
   * {@link com.google.gwt.dom.client.Document#getElementById(String)}, so the
   * id must still be unique within the document.
   * 
   * @param id the id of the element to be found
   * @return the element with the given id, or <code>null</code> if none is found
   */
  public Element getElementById(String id) {
    return isAttached() ? DOM.getElementById(id) : attachToDomAndGetElement(id);
  }

  /**
   * Performs a {@link DOM#getElementById(String)} after attaching the panel's
   * element into a hidden DIV in the document's body. Attachment is necessary
   * to be able to use the native getElementById. The panel's element will be
   * re-attached to its original parent (if any) after the method returns.
   * 
   * @param id the id whose associated element is to be retrieved
   * @return the associated element, or <code>null</code> if none is found
   */
  private Element attachToDomAndGetElement(String id) {
    // If the hidden DIV has not been created, create it.
    if (hiddenDiv == null) {
      hiddenDiv = DOM.createDiv();
      UIObject.setVisible(hiddenDiv, false);
      RootPanel.getBodyElement().appendChild(hiddenDiv);
    }

    // Hang on to the panel's original parent and sibling elements so that it
    // can be replaced.
    Element origParent = DOM.getParent(getElement());
    Element origSibling = DOM.getNextSibling(getElement());

    // Attach the panel's element to the hidden div.
    DOM.appendChild(hiddenDiv, getElement());

    // Now that we're attached to the DOM, we can use getElementById.
    Element child = DOM.getElementById(id);

    // Put the panel's element back where it was.
    if (origParent != null) {
      DOM.insertBefore(origParent, getElement(), origSibling);
    } else {
      DOM.removeChild(hiddenDiv, getElement());
    }

    return child;
  }
}
