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
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.DOM;

/**
 * Tests the RadioButton class.
 */
public class RadioButtonTest extends GWTTestCase {

  private static class Changeable implements ValueChangeHandler<Boolean> {
    Boolean received;

    public void onValueChange(ValueChangeEvent<Boolean> event) {
      received = event.getValue();
    }
  }

  private static final String html1 = "<b>hello</b><i>world</i>:)";
  private static final String html2 = "<b>goodbye</b><i>world</i>:(";

  /**
   * TODO: Re-enable when we figure out how to make them work properly on IE
   * (which has the unfortunate property of not passing synthesized events on to
   * native controls, keeping the clicks created by these tests from actually
   * affecting the radio buttons' states).
   */
  public void disabledTestValueChangeViaClick() {
    RadioButton r1 = new RadioButton("group1", "Radio 1");
    RadioButton r2 = new RadioButton("group1", "Radio 2");
    RootPanel.get().add(r1);
    RootPanel.get().add(r2);
    r1.setValue(true);

    Changeable c1 = new Changeable();
    r1.addValueChangeHandler(c1);

    Changeable c2 = new Changeable();
    r2.addValueChangeHandler(c2);

    // Brittle, but there's no public access
    InputElement r1Radio = getRadioElement(r1);
    InputElement r2Radio = getRadioElement(r2);

    doClick(r1Radio);
    assertEquals(null, c1.received);
    assertEquals(null, c2.received);

    doClick(r2Radio);
    assertEquals(null, c1.received);
    assertEquals(Boolean.TRUE, c2.received);
    c2.received = null;

    doClick(r1Radio);
    assertEquals(Boolean.TRUE, c1.received);
    assertEquals(null, c2.received);
  }

  /**
   * TODO: Re-enable when we figure out how to make them work properly on IE
   * (which has the unfortunate property of not passing synthesized events on to
   * native controls, keeping the clicks created by these tests from actually
   * affecting the radio buttons' states).
   */
  public void disabledTestValueChangeViaLabelClick() {
    RadioButton r1 = new RadioButton("group1", "Radio 1");
    RadioButton r2 = new RadioButton("group1", "Radio 2");
    RootPanel.get().add(r1);
    RootPanel.get().add(r2);
    r1.setValue(true);

    Changeable c1 = new Changeable();
    r1.addValueChangeHandler(c1);

    Changeable c2 = new Changeable();
    r2.addValueChangeHandler(c2);

    LabelElement r1Label = getLabelElement(r1);
    LabelElement r2Label = getLabelElement(r2);

    doClick(r1Label);
    assertEquals(null, c1.received);
    assertEquals(null, c2.received);

    doClick(r2Label);
    assertEquals(null, c1.received);
    assertEquals(Boolean.TRUE, c2.received);
    c2.received = null;

    doClick(r1Label);
    assertEquals(Boolean.TRUE, c1.received);
    assertEquals(null, c2.received);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  public void testDebugId() {
    RadioButton radio = new RadioButton("myName", "myLabel");

    // We need to replace the input element so we can keep a handle to it
    com.google.gwt.user.client.Element newInput = DOM.createInputRadio("MyName");
    radio.replaceInputElement(newInput);

    radio.ensureDebugId("myRadio");
    RootPanel.get().add(radio);

    UIObjectTest.assertDebugId("myRadio", radio.getElement());
    UIObjectTest.assertDebugId("myRadio-input", newInput);
    UIObjectTest.assertDebugIdContents("myRadio-label", "myLabel");
  }

  /**
   * Test the name and grouping methods.
   */
  public void testGrouping() {
    // Create some radio buttons
    RadioButton r1 = new RadioButton("group1", "Radio 1");
    RadioButton r2 = new RadioButton("group1", "Radio 2");
    RadioButton r3 = new RadioButton("group2", "Radio 3");
    RootPanel.get().add(r1);
    RootPanel.get().add(r2);
    RootPanel.get().add(r3);

    // Check one button in each group
    r2.setValue(true);
    r3.setValue(true);

    // Move a button over
    r2.setName("group2");

    // Check that the correct buttons are checked
    assertTrue(r2.getValue());
    assertFalse(r3.getValue());

    r1.setValue(true);
    assertTrue(r1.getValue());
    assertTrue(r2.getValue());

    r3.setValue(true);
    assertTrue(r1.getValue());
    assertFalse(r2.getValue());
    assertTrue(r3.getValue());
  }

  /**
   * Test the name and grouping methods via deprecated calls.
   */
  @SuppressWarnings("deprecation")
  public void testGroupingDeprecated() {
    // Create some radio buttons
    RadioButton r1 = new RadioButton("group1", "Radio 1");
    RadioButton r2 = new RadioButton("group1", "Radio 2");
    RadioButton r3 = new RadioButton("group2", "Radio 3");
    RootPanel.get().add(r1);
    RootPanel.get().add(r2);
    RootPanel.get().add(r3);

    // Check one button in each group
    r2.setChecked(true);
    r3.setChecked(true);

    // Move a button over
    r2.setName("group2");

    // Check that the correct buttons are checked
    assertTrue(r2.isChecked());
    assertFalse(r3.isChecked());

    r1.setChecked(true);
    assertTrue(r1.isChecked());
    assertTrue(r2.isChecked());

    r3.setChecked(true);
    assertTrue(r1.isChecked());
    assertFalse(r2.isChecked());
    assertTrue(r3.isChecked());
  }

  /**
   * Ensures that the element order doesn't get reversed when the radio's name
   * is changed.
   */
  public void testOrderAfterSetName() {
    RadioButton radio = new RadioButton("oldName");
    assertEquals("oldName", radio.getName());

    radio.setName("newName");
    assertEquals("newName", radio.getName());

    Element parent = radio.getElement();
    Element firstChild = parent.getFirstChildElement().cast();
    Element secondChild = firstChild.getNextSiblingElement().cast();
    assertEquals("input", firstChild.getTagName().toLowerCase());
    assertEquals("label", secondChild.getTagName().toLowerCase());
  }

  public void testSafeHtml() {
    RadioButton radio = 
      new RadioButton("radio", SafeHtmlUtils.fromSafeConstant(html1));
    
    assertEquals("radio", radio.getName());
    assertEquals(html1, radio.getHTML().toLowerCase());
    
    radio.setHTML(SafeHtmlUtils.fromSafeConstant(html2));
    
    assertEquals(html2, radio.getHTML().toLowerCase());
  }

  private void doClick(Element elm) {
    NativeEvent e = Document.get().createMouseDownEvent(0, 25, 25, 25, 25,
        false, false, false, false, NativeEvent.BUTTON_LEFT);
    elm.dispatchEvent(e);

    e = Document.get().createMouseUpEvent(0, 25, 25, 25, 25, false, false,
        false, false, NativeEvent.BUTTON_LEFT);
    elm.dispatchEvent(e);

    e = Document.get().createClickEvent(0, 25, 25, 25, 25, false, false, false,
        false);
    elm.dispatchEvent(e);
  }

  private LabelElement getLabelElement(RadioButton radioButton) {
    LabelElement r1Label = LabelElement.as(Element.as(getRadioElement(
        radioButton).getNextSiblingElement()));
    return r1Label;
  }

  private InputElement getRadioElement(RadioButton radioButton) {
    InputElement r1Radio = InputElement.as(Element.as(radioButton.getElement().getFirstChild()));
    return r1Radio;
  }
}
