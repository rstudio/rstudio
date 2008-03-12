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

  private Element hiddenDiv;

  /**
   * Creates an HTML panel with the specified HTML contents. Any element within
   * this HTML that has a specified id can contain a child widget.
   * 
   * @param html the panel's HTML
   */
  public HTMLPanel(String html) {
    setElement(DOM.createDiv());
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
    final Element elem = (isAttached()) ? DOM.getElementById(id)
        : attachToDomAndGetElement(id);

    if (elem == null) {
      throw new NoSuchElementException(id);
    }

    super.add(widget, elem);
  }

  @Override
  protected void onLoad() {
    if (hiddenDiv != null) {
      // This widget's element has already been moved, just need to remove the
      // hidden DIV.
      DOM.removeChild(RootPanel.getBodyElement(), hiddenDiv);
      hiddenDiv = null;
    }
    super.onLoad();
  }

  /**
   * Performs a {@link DOM#getElementById(String)} after attaching the widget's
   * element into a hidden DIV in the document's body. Attachment is necessary
   * to be able to use the native getElementById. The hidden DIV will remain
   * attached to the DOM until the Widget itself is fully attached.
   * 
   * @param id the id whose associated element is to be retrieved
   * @return the associated element, or <code>null</code> if none is found
   */
  private Element attachToDomAndGetElement(String id) {
    // If the hidden DIV has not been created, create it.
    if (hiddenDiv == null) {
      hiddenDiv = DOM.createDiv();
      UIObject.setVisible(hiddenDiv, false);
      DOM.appendChild(hiddenDiv, getElement());
      DOM.appendChild(RootPanel.getBodyElement(), hiddenDiv);
    }

    // Now that we're attached to the DOM, we can use getElementById.
    return DOM.getElementById(id);
  }
}
