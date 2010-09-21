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
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * A standard push-button widget.
 * 
 * <p>
 * <img class='gallery' src='doc-files/Button.png'/>
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-Button</dt>
 * <dd>the outer element</dd>
 * </dl>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.ButtonExample}
 * </p>
 */
public class Button extends ButtonBase {

  /**
   * Creates a Button widget that wraps an existing &lt;button&gt; element.
   * 
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * @param element the element to be wrapped
   */
  public static Button wrap(com.google.gwt.dom.client.Element element) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    Button button = new Button(element);

    // Mark it attached and remember it for cleanup.
    button.onAttach();
    RootPanel.detachOnWindowClose(button);

    return button;
  }

  /**
   * Creates a button with no caption.
   */
  public Button() {
    super(Document.get().createPushButtonElement());
    setStyleName("gwt-Button");
  }

  /**
   * Creates a button with the given HTML caption.
   *
   * @param html the HTML caption
   */
  public Button(SafeHtml html) {
    this(html.asString());
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
   * @deprecated Use {@link Button#Button(String, ClickHandler)} instead
   */
  @Deprecated
  public Button(String html, ClickListener listener) {
    this(html);
    addClickListener(listener);
  }

  /**
   * Creates a button with the given HTML caption and click listener.
   *
   * @param html the html caption
   * @param handler the click handler
   */
  public Button(SafeHtml html, ClickHandler handler) {
    this(html.asString(), handler);
  }

  /**
   * Creates a button with the given HTML caption and click listener.
   *
   * @param html the HTML caption
   * @param handler the click handler
   */
  public Button(String html, ClickHandler handler) {
    this(html);
    addClickHandler(handler);
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be a &lt;button&gt; element.
   * 
   * @param element the element to be used
   */
  protected Button(com.google.gwt.dom.client.Element element) {
    super(element.<Element> cast());
    ButtonElement.as(element);
  }

  /**
   * Programmatic equivalent of the user clicking the button.
   */
  public void click() {
    getButtonElement().click();
  }

  /**
   * Get the underlying button element.
   * 
   * @return the {@link ButtonElement}
   */
  protected ButtonElement getButtonElement() {
    return getElement().cast();
  }
}
