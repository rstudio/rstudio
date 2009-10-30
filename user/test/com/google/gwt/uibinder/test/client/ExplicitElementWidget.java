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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.dom.client.Document;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.Widget;

/**
 * Used to test UiBinder, a widget that can be based on arbitrary element
 * tags at the whim of its user.
 */
public class ExplicitElementWidget extends Widget implements HasHTML {
  @UiConstructor
  ExplicitElementWidget(String tag) {
    setElement(Document.get().createElement(tag));
  }
  
  public String getHTML() {
    return getElement().getInnerHTML();
  }

  public String getText() {
    return getElement().getInnerText();
  }

  public void setHTML(String html) {
    getElement().setInnerHTML(html);
  }

  public void setText(String text) {
    getElement().setInnerText(text);
  }
}
