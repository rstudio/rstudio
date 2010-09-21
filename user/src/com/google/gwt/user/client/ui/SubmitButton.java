/*
 * Copyright 2009 Google Inc.
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
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * A standard push-button widget which will automatically submit its enclosing
 * {@link FormPanel} if any.
 * 
 * <h3>CSS Style Rules</h3>
 * <dl>
 * <dt>.gwt-SubmitButton</dt>
 * <dd>the outer element</dd>
 * </dl>
 */
public class SubmitButton extends Button {

  /**
   * Creates a SubmitButton widget that wraps an existing &lt;button&gt;
   * element.
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

    SubmitButton button = new SubmitButton(element);
    assert "submit".equalsIgnoreCase(button.getButtonElement().getType());

    // Mark it attached and remember it for cleanup.
    button.onAttach();
    RootPanel.detachOnWindowClose(button);

    return button;
  }

  /**
   * Creates a button with no caption.
   */
  public SubmitButton() {
    super(Document.get().createSubmitButtonElement());
    setStyleName("gwt-SubmitButton");
  }

  /**
   * Creates a button with the given HTML caption.
   *
   * @param html the HTML caption
   */
  public SubmitButton(SafeHtml html) {
    this(html.asString());
  }

  /**
   * Creates a button with the given HTML caption.
   *
   * @param html the HTML caption
   */
  public SubmitButton(String html) {
    this();
    setHTML(html);
  }

  /**
   * Creates a button with the given HTML caption and click listener.
   *
   * @param html the HTML caption
   * @param handler the click handler
   */
  public SubmitButton(SafeHtml html, ClickHandler handler) {
    this(html.asString(), handler);
  }

  /**
   * Creates a button with the given HTML caption and click listener.
   *
   * @param html the HTML caption
   * @param handler the click handler
   */
  public SubmitButton(String html, ClickHandler handler) {
    this(html);
    addClickHandler(handler);
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be a &lt;button&gt; element with type submit.
   * 
   * @param element the element to be used
   */
  protected SubmitButton(com.google.gwt.dom.client.Element element) {
    super(element);
    assert "submit".equalsIgnoreCase(element.<ButtonElement> cast().getType());
  }
}
