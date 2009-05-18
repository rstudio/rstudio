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

import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for {@link Button}.
 */
public class ButtonTest extends GWTTestCase {

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

  public void testClick() {
    Button b = new Button();
    RootPanel.get().add(b);

    H h = new H();
    b.addClickHandler(h);

    b.click();
    assertTrue(h.clicked);

    // Old Mozilla browsers don't set up the event target properly for
    // synthesized clicks. This tests the workaround in DOMImplMozillaOld.
    assertEquals(b.getElement(), h.target);
  }
}

