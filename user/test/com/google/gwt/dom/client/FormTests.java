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
package com.google.gwt.dom.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the {@link FormElement} and {@link InputElement} classes.
 */
public class FormTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dom.DOMTest";
  }

  /**
   * getElements.
   */
  public void testGetElements() {
    Document doc = Document.get();
    FormElement form = doc.createFormElement();
    doc.getBody().appendChild(form);
    form.setInnerHTML("<div>" + "<input name='text' id='text' type='text'>"
        + "<input name='hidden' id='hidden' type='hidden'>"
        + "<textarea name='textarea' id='textarea'>" + "</div>");

    NodeCollection<Element> formElems = form.getElements();
    assertEquals(3, formElems.getLength());

    assertEquals("text", formElems.getItem(0).getId());
    assertEquals("hidden", formElems.getItem(1).getId());
    assertEquals("textarea", formElems.getItem(2).getId());

    assertEquals("text", formElems.getNamedItem("text").getId());
    assertEquals("hidden", formElems.getNamedItem("hidden").getId());
    assertEquals("textarea", formElems.getNamedItem("textarea").getId());
  }
}
