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
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

/**
 * Tests the CheckBox Widget.
 */
public class CheckBoxTest extends GWTTestCase {
  @SuppressWarnings("deprecation")  
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
  
  private static class Handler implements ValueChangeHandler<Boolean> {
    Boolean received = null;

    public void onValueChange(ValueChangeEvent<Boolean> event) {
      received = event.getValue();
    }
  }

  private static final String html = "<b>hello</b><i>world</i>";

  private CheckBox cb;

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  /**
   * Test accessors.
   */
  @SuppressWarnings("deprecation")
  public void testAccessors() {
    cb.setHTML("test HTML");
    assertEquals(cb.getHTML(), "test HTML");
    cb.setText("test Text");
    assertEquals(cb.getText(), "test Text");

    cb.setChecked(true);
    assertTrue(cb.isChecked());
    cb.setChecked(false);
    assertFalse(cb.isChecked());

    cb.setValue(true);
    assertTrue(cb.getValue());
    cb.setValue(false);
    assertFalse(cb.getValue());

    // null implies false
    cb.setValue(null);
    assertFalse(cb.getValue());

    cb.setEnabled(false);
    assertFalse(cb.isEnabled());
    cb.setEnabled(true);
    assertTrue(cb.isEnabled());

    cb.setTabIndex(2);
    assertEquals(cb.getTabIndex(), 2);

    cb.setName("my name");
    assertEquals(cb.getName(), "my name");

    cb.setFormValue("valuable");
    assertEquals("valuable", cb.getFormValue());
  }

  public void testConstructorInputElement() {
    InputElement elm = DOM.createInputCheck().cast();
    CheckBox box = new CheckBox(elm.<Element> cast());
    assertFalse(box.getValue());
    elm.setDefaultChecked(true);
    assertTrue(box.getValue());
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

  /**
   * Tests that detaching and attaching a CheckBox widget retains the checked
   * state of the element. This is known to be tricky on IE.
   */
  public void testDetachment() {
    InputElement elm = DOM.createInputCheck().cast();
    CheckBox box = new CheckBox(elm.<Element> cast());
    RootPanel.get().add(box);
    elm.setChecked(true);
    RootPanel.get().remove(box);
    RootPanel.get().add(box);
    assertTrue(elm.isChecked());
  }

  public void testFormValue() {
    InputElement elm = Document.get().createCheckInputElement();
    Element asOldElement = elm.cast();
    cb.replaceInputElement(asOldElement);

    // assertEquals("", elm.getValue());
    cb.setFormValue("valuable");
    assertEquals("valuable", elm.getValue());

    elm.setValue("invaluable");
    assertEquals("invaluable", cb.getFormValue());
  }

  @SuppressWarnings("deprecation")
  public void testListenerRemoval() {
    ClickListener r1 = new ListenerTester();
    ClickListener r2 = new ListenerTester();
    ListenerTester.manager = cb.ensureHandlers();
    cb.addClickListener(r1);
    cb.addClickListener(r2);

    ListenerTester.fire();
    assertEquals(ListenerTester.fired, 2);

    cb.removeClickListener(r1);
    ListenerTester.fire();
    assertEquals(ListenerTester.fired, 1);

    cb.removeClickListener(r2);
    ListenerTester.fire();
    assertEquals(ListenerTester.fired, 0);
  }

  public void testCheckboxClick() {
    final int[] clickCount = {0};

    CheckBox check = new CheckBox();
    Element newInput = DOM.createInputCheck();
    check.replaceInputElement(newInput);

    check.setText("Burger");
    check.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent arg0) {
        clickCount[0]++;
      }
    });
    RootPanel.get().add(check);

    NativeEvent e = Document.get().createClickEvent(0, 25, 25, 25, 25, false, 
        false, false, false);

    newInput.dispatchEvent(e);
    assertEquals(1, clickCount[0]);
  }
  
  public void testReplaceInputElement() {
    cb.setValue(true);
    cb.setTabIndex(1234);
    cb.setEnabled(false);
    cb.setAccessKey('k');
    cb.setFormValue("valuable");

    InputElement elm = Document.get().createCheckInputElement();
    assertFalse(elm.isChecked());

    Element asOldElement = elm.cast();
    cb.replaceInputElement(asOldElement);

    // The values should be preserved
    assertTrue(cb.getValue());
    assertEquals(1234, cb.getTabIndex());
    assertFalse(cb.isEnabled());
    assertEquals("k", elm.getAccessKey());
    assertEquals("valuable", cb.getFormValue());

    assertTrue(elm.isChecked());
    cb.setValue(false);
    assertFalse(elm.isChecked());

    elm.setChecked(true);
    assertTrue(cb.getValue());
  }

  public void testSafeHtmlConstructor() {
    CheckBox box = new CheckBox(SafeHtmlUtils.fromSafeConstant(html));
    
    assertEquals(html, box.getHTML().toLowerCase());
  }

  public void testSetSafeHtml() {
    CheckBox box = new CheckBox("hello");
    box.setHTML(SafeHtmlUtils.fromSafeConstant(html));
    
    assertEquals(html, box.getHTML().toLowerCase());
  }

  @SuppressWarnings("deprecation")
  public void testValueChangeEvent() {
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
    
    // Note that we cannot test this with a simulated click, the way
    // we do for the click handlers. IE does not change the value of
    // the native checkbox on simulated click event, and there's 
    // naught to be done about it.
  }

  public void testWordWrap() {
    assertTrue(cb.getWordWrap());

    cb.setWordWrap(false);
    assertFalse(cb.getWordWrap());

    cb.setWordWrap(true);
    assertTrue(cb.getWordWrap());
  }

  @Override
  protected void gwtSetUp() throws Exception {
    super.gwtSetUp();
    RootPanel.get().clear();
    cb = new CheckBox();
    RootPanel.get().add(cb);
  }

  @Override
  protected void gwtTearDown() throws Exception {
    RootPanel.get().clear();
    super.gwtTearDown();
  }
}
