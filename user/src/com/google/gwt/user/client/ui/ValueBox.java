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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.text.shared.Parser;
import com.google.gwt.text.shared.Renderer;

/**
 * A text box able to parse its displayed value.
 * 
 * @param <T> the value type
 */
public class ValueBox<T> extends ValueBoxBase<T> {

  /**
   * Creates a ValueBox widget that wraps an existing &lt;input type='text'&gt;
   * element.
   * 
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * @param element the element to be wrapped
   */
  public static <T> ValueBox<T> wrap(Element element, Renderer<T> renderer, Parser<T> parser) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    ValueBox<T> valueBox = new ValueBox<T>(element, renderer, parser);

    // Mark it attached and remember it for cleanup.
    valueBox.onAttach();
    RootPanel.detachOnWindowClose(valueBox);

    return valueBox;
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be an &lt;input&gt; element whose type is
   * 'text'.
   * 
   * @param element the element to be used
   */
  protected ValueBox(Element element, Renderer<T> renderer, Parser<T> parser) {
    super(element, renderer, parser);
    // BiDi input is not expected - disable direction estimation.
    setDirectionEstimator(false);
    if (LocaleInfo.getCurrentLocale().isRTL()) {
      setDirection(Direction.LTR);
    }
    assert InputElement.as(element).getType().equalsIgnoreCase("text");
  }

  /**
   * Gets the maximum allowable length.
   * 
   * @return the maximum length, in characters
   */
  public int getMaxLength() {
    return getInputElement().getMaxLength();
  }

  /**
   * Gets the number of visible characters.
   * 
   * @return the number of visible characters
   */
  public int getVisibleLength() {
    return getInputElement().getSize();
  }

  /**
   * Sets the maximum allowable length.
   * 
   * @param length the maximum length, in characters
   */
  public void setMaxLength(int length) {
    getInputElement().setMaxLength(length);
  }

  /**
   * Sets the number of visible characters.
   * 
   * @param length the number of visible characters
   */
  public void setVisibleLength(int length) {
    getInputElement().setSize(length);
  }

  private InputElement getInputElement() {
    return getElement().cast();
  }
}
