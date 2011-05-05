/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;

/**
 * Test the use of SafeHtml objects as UiBinder components. E.g.
 * 
 * <pre>
 * &lt;div>&lt;my:SafeHtmlThingy foo="bar"/>&lt;/div></pre>
 */
public class SafeHtmlAsComponentsTest extends GWTTestCase {
  static class Ui {
    interface Binder extends UiBinder<Element, Ui> {
    }

    static final Binder binder = GWT.create(Binder.class);

    @UiField
    DivElement div;
    @UiField
    SafeHtmlObject safeObject;

    Ui() {
      binder.createAndBindUi(this);
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.test.LazyWidgetBuilderSuite";
  }

  public void testSafeHtml() {
    Ui domUi = new Ui();
    assertNotNull(domUi.safeObject);
    assertEquals(domUi.safeObject.asString().toLowerCase(),
                 domUi.div.getInnerHTML().toLowerCase());
    assertEquals("Hello <b>Bob</b>".toLowerCase(), domUi.div.getInnerHTML().toLowerCase());
  }
}
