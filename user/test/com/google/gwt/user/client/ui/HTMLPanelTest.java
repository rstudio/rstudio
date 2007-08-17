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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the HTMLPanel widget.
 */
public class HTMLPanelTest extends GWTTestCase {

  static class Adder implements HasWidgetsTester.WidgetAdder {
    public void addChild(HasWidgets container, Widget child) {
      ((HTMLPanel) container).add(child, "w00t");
    }
  }

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testAttachDetachOrder() {
    HTMLPanel p = new HTMLPanel("<div id='w00t'></div>");
    HasWidgetsTester.testAll(p, new Adder());
  }
}
