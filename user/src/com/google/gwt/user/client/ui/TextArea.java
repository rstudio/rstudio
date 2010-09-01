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
import com.google.gwt.dom.client.TextAreaElement;

/**
 * A text box that allows multiple lines of text to be entered.
 *
 * <p>
 * <img class='gallery' src='doc-files/TextArea.png'/>
 * </p>
 *
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-TextArea { primary style }</li>
 * <li>.gwt-TextArea-readonly { dependent style set when the text area is read-only }</li>
 * </ul>
 *
 * <p>
 * <h3>Built-in Bidi Text Support</h3>
 * This widget is capable of automatically adjusting its direction according to
 * the input text. This feature is controlled by {@link #setDirectionEstimator},
 * and is available by default when at least one of the application's locales is
 * right-to-left.
 * </p>
 *
 * <p>
 * <h3>Example</h3> {@example com.google.gwt.examples.TextBoxExample}
 * </p>
 */
public class TextArea extends TextBoxBase {

  /**
   * Creates a TextArea widget that wraps an existing &lt;textarea&gt;
   * element.
   *
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   *
   * @param element the element to be wrapped
   */
  public static TextArea wrap(Element element) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    TextArea textArea = new TextArea(element);

    // Mark it attached and remember it for cleanup.
    textArea.onAttach();
    RootPanel.detachOnWindowClose(textArea);

    return textArea;
  }

  /**
   * Creates an empty text area.
   */
  public TextArea() {
    super(Document.get().createTextAreaElement());
    setStyleName("gwt-TextArea");
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be a &lt;textarea&gt; element.
   *
   * @param element the element to be used
   */
  protected TextArea(Element element) {
    super(element.<Element>cast());
    TextAreaElement.as(element);
  }

  /**
   * Gets the requested width of the text box (this is not an exact value, as
   * not all characters are created equal).
   *
   * @return the requested width, in characters
   */
  public int getCharacterWidth() {
    return getTextAreaElement().getCols();
  }

  @Override
  public int getCursorPos() {
    return getImpl().getTextAreaCursorPos(getElement());
  }

  @Override
  public int getSelectionLength() {
    return getImpl().getTextAreaSelectionLength(getElement());
  }

  /**
   * Gets the number of text lines that are visible.
   *
   * @return the number of visible lines
   */
  public int getVisibleLines() {
    return getTextAreaElement().getRows();
  }

  /**
   * Sets the requested width of the text box (this is not an exact value, as
   * not all characters are created equal).
   *
   * @param width the requested width, in characters
   */
  public void setCharacterWidth(int width) {
    getTextAreaElement().setCols(width);
  }

  /**
   * Sets the number of text lines that are visible.
   *
   * @param lines the number of visible lines
   */
  public void setVisibleLines(int lines) {
    getTextAreaElement().setRows(lines);
  }

  private TextAreaElement getTextAreaElement() {
    return getElement().cast();
  }
}
