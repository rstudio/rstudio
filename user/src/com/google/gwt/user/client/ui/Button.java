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

import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * A standard push-button widget.
 * 
 * <p>
 * <img class='gallery' src='Button.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class="css">
 * <li>.gwt-Button { }</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.ButtonExample}
 * </p>
 */
public class Button extends ButtonBase {

  /**
   * Creates a Button widget that wraps an existing &lt;button&gt; element.
   * 
   * This element must already be attached to the document.
   * 
   * @param element the element to be wrapped
   */
  public static Button wrap(com.google.gwt.dom.client.Element element) {
    // Assert that the element is of the correct type and is attached.
    ButtonElement.as(element);
    assert Document.get().getBody().isOrHasChild(element);

    Button button = new Button((Element) element);

    // Mark it attached and remember it for cleanup.
    button.onAttach();
    RootPanel.detachOnWindowClose(button);

    return button;
  }

  static native void adjustType(Element button) /*-{
    // Check before setting this attribute, as not all browsers define it.
    if (button.type == 'submit') {
      try { 
        button.setAttribute("type", "button"); 
      } catch (e) { 
      }
    }
  }-*/;

  static native void click(Element button) /*-{
    button.click();
  }-*/;

  /**
   * Creates a button with no caption.
   */
  public Button() {
    super(DOM.createButton());
    adjustType(getElement());
    setStyleName("gwt-Button");
  }

  /**
   * Creates a button with the given HTML caption.
   * 
   * @param html the HTML caption
   */
  public Button(String html) {
    this();
    setHTML(html);
  }

  /**
   * Creates a button with the given HTML caption and click listener.
   * 
   * @param html the HTML caption
   * @param listener the click listener
   */
  public Button(String html, ClickListener listener) {
    this(html);
    addClickListener(listener);
  }

  private Button(Element element) {
    super(element);
  }

  /**
   * Programmatic equivalent of the user clicking the button.
   */
  public void click() {
    click(getElement());
  }
}
