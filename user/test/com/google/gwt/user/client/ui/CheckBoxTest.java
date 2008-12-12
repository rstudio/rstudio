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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Tests the CheckBox Widget.
 */
public class CheckBoxTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    RootPanel.get().clear();
  }

  @Override
  protected void gwtTearDown() throws Exception {
    RootPanel.get().clear();
    super.gwtTearDown();
  }

  /**
   * Test accessors.
   */
  public void testAccessors() {
    final CheckBox cb = new CheckBox();
    RootPanel.get().add(cb);

    // Label accessors
    cb.setHTML("test HTML");
    assertEquals(cb.getHTML(), "test HTML");
    cb.setText("test Text");
    assertEquals(cb.getText(), "test Text");

    // Input accessors
    cb.setChecked(true);
    assertTrue(cb.isChecked());
    cb.setChecked(false);
    assertFalse(cb.isChecked());
    cb.setEnabled(false);
    assertFalse(cb.isEnabled());
    cb.setEnabled(true);
    assertTrue(cb.isEnabled());
    cb.setTabIndex(2);
    assertEquals(cb.getTabIndex(), 2);
    cb.setName("my name");
    assertEquals(cb.getName(), "my name");
  }

  public void testDebugId() {
    CheckBox check = new CheckBox("myLabel");

    // We need to replace the input element so we can keep a handle to it
    Element newInput = DOM.createInputCheck();
    check.replaceInputElement(newInput);

    check.ensureDebugId("myCheck");
    RootPanel.get().add(check);

    UIObjectTest.assertDebugId("myCheck", check.getElement());
    UIObjectTest.assertDebugId("myCheck-input", newInput);
    UIObjectTest.assertDebugIdContents("myCheck-label", "myLabel");
  }

  public void testValueChangeEvent() {
    CheckBox cb = new CheckBox();
    Handler h = new Handler();
    cb.addValueChangeHandler(h);
    cb.setChecked(false);
    assertNull(h.received);
    cb.setChecked(true);
    assertNull(h.received);

    cb.setValue(false);
    assertNull(h.received);
    cb.setValue(true);
    assertNull(h.received);

    cb.setValue(true, true);
    assertNull(h.received);

    cb.setValue(false, true);
    assertFalse(h.received);

    cb.setValue(true, true);
    assertTrue(h.received);

  }

  static class ListenerTester implements ClickListener {
    static int fired = 0;
    static HandlerManager manager;

    public static void fire() {
      fired = 0;
      manager.fireEvent(new ClickEvent() {
      });
    }
 
    public void onClick(Widget sender) {
      ++fired;
    }
  }

  @SuppressWarnings("deprecation")
  public void testListenerRemoval() {
    CheckBox b = new CheckBox();
 
    ClickListener r1 = new ListenerTester();
    ClickListener r2 = new ListenerTester();
    ListenerTester.manager = b.ensureHandlers();
    b.addClickListener(r1);
    b.addClickListener(r2);

    ListenerTester.fire();
    assertEquals(ListenerTester.fired, 2);

    b.removeClickListener(r1);
    ListenerTester.fire();
    assertEquals(ListenerTester.fired, 1);

    b.removeClickListener(r2);
    ListenerTester.fire();
    assertEquals(ListenerTester.fired, 0);
  }

  private static class Handler implements ValueChangeHandler<Boolean> {
    Boolean received = null;

    public void onValueChange(ValueChangeEvent<Boolean> event) {
      received = event.getValue();
    }
  }
}
