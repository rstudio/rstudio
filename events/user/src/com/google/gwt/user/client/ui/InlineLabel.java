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

/**
 * A widget that contains arbitrary text, <i>not</i> interpreted as HTML.
 * 
 * This widget uses a &lt;span&gt; element, causing it to be displayed with
 * inline layout.
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-InlineLabel { }</li>
 * </ul>
 */
public class InlineLabel extends Label {

  /**
   * Creates a InlineLabel widget that wraps an existing &lt;div&gt; or
   * &lt;span&gt; element.
   * 
   * This element must already be attached to the document. If the element is
   * removed from the document, you must call
   * {@link RootPanel#detachNow(Widget)}.
   * 
   * @param element the element to be wrapped
   */
  public static InlineLabel wrap(Element element) {
    // Assert that the element is attached.
    assert Document.get().getBody().isOrHasChild(element);

    InlineLabel label = new InlineLabel(element);

    // Mark it attached and remember it for cleanup.
    label.onAttach();
    RootPanel.detachOnWindowClose(label);

    return label;
  }

  /**
   * Creates an empty label.
   */
  public InlineLabel() {
    super(Document.get().createSpanElement());
    setStyleName("gwt-InlineLabel");
  }

  /**
   * Creates a label with the specified text.
   * 
   * @param text the new label's text
   */
  public InlineLabel(String text) {
    this();
    setText(text);
  }

  /**
   * This constructor may be used by subclasses to explicitly use an existing
   * element. This element must be either a &lt;div&gt; &lt;span&gt; element.
   * 
   * @param element the element to be used
   */
  protected InlineLabel(Element element) {
    super(element);
    assert element.getTagName().equalsIgnoreCase("div")
        || element.getTagName().equalsIgnoreCase("span");
  }
}
