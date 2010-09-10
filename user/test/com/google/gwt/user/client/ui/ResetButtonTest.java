/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

/**
 * Tests for {@link ResetButton}.
 */
public class ResetButtonTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  private static class H implements ClickHandler {
    boolean clicked;
    EventTarget target;

    public void onClick(ClickEvent event) {
      target = event.getNativeEvent().getEventTarget();
      clicked = true;
    }
  }

  private static final String html = "<b>hello</b><i>world</i>";

  public void testSetSafeHtmlConstructor() {
    ResetButton button = new ResetButton(SafeHtmlUtils.fromSafeConstant(html));
    
    assertEquals(html, button.getHTML().toLowerCase());
  }

  public void testSafeHtmlWithHandler() {
    H handler = new H();
    ResetButton button = 
      new ResetButton(SafeHtmlUtils.fromSafeConstant(html), handler);
    
    assertEquals(html, button.getHTML().toLowerCase());
  }
}
