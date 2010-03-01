/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;

/**
 * Testing TextBox.
 */
public class TextBoxTest extends TextBoxBaseTestBase {

  @Override
  protected TextBox createTextBoxBase() {
    return new TextBox();
  }

  public void testMaxLength() {
    TextBox b = createTextBoxBase();
    b.setMaxLength(5);
    assertEquals(5, b.getMaxLength());
    // As our setText does not honor max length, no way to text it in the wild
    // here.
  }

  public void testMinLength() {
    TextBox b = createTextBoxBase();
    b.setVisibleLength(5);

    // Make sure maxLength is independent from visible length.
    b.setMaxLength(10);
    assertEquals(10, b.getMaxLength());

    // Now check visible length.
    assertEquals(5, b.getVisibleLength());
  }

  public void testNoNukeTabIndex() {
    Document doc = Document.get();
    DivElement div = doc.createDivElement();
    div.setInnerHTML("<input type='text' id='tb' tabindex='1'></input>");
    doc.getBody().appendChild(div);

    TextBox tb = TextBox.wrap(doc.getElementById("tb"));
    assertEquals(1, tb.getTabIndex());
  }
}
