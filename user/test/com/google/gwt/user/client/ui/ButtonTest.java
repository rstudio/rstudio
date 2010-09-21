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
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitHandler;

/**
 * Tests for {@link Button}.
 */
public class ButtonTest extends GWTTestCase {

  private static final String html = "<b>hello</b><i>world</i>";

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

  private static class H2 implements SubmitHandler {
    boolean submitted;

    public void onSubmit(SubmitEvent event) {
      submitted = true;
      event.cancel();
    }
  }

  public void testButton() {
    Button pushButton = new Button();
    assertEquals("button", pushButton.getButtonElement().getType());

    ResetButton resetButton = new ResetButton();
    assertEquals("reset", resetButton.getButtonElement().getType());

    SubmitButton submitButton = new SubmitButton();
    assertEquals("submit", submitButton.getButtonElement().getType());
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

  /**
   * Tests issues 1585 and 3962: a button shouldn't submit a form.
   */
  public void testPushButton() {
    FormPanel f = new FormPanel();
    f.setAction("javascript:''");
    RootPanel.get().add(f);

    Button b = new Button();
    f.setWidget(b);

    final H2 h = new H2();
    f.addSubmitHandler(h);

    delayTestFinish(5000);
    new Timer() {
      @Override
      public void run() {
        assertFalse(h.submitted);
        finishTest();
      }
    }.schedule(2500);

    b.click();
  }

  public void testSetSafeHtml() {
    Button button = new Button("hello");
    button.setHTML(SafeHtmlUtils.fromSafeConstant(html));
    
    assertEquals(html, button.getHTML().toLowerCase());
  }

  public void testSafeHtmlConstructor() {
    Button button = new Button(SafeHtmlUtils.fromSafeConstant(html));
    
    assertEquals(html, button.getHTML().toLowerCase());
  }

  public void testSafeHtmlWithHandler() {
    H handler = new H();
    Button button = new Button(SafeHtmlUtils.fromSafeConstant(html), handler);
    
    assertEquals(html, button.getHTML().toLowerCase());
  }
}
