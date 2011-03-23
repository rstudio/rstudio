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

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the {@link SelectElement} and {@link OptionElement} classes.
 */
public class SelectTests extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.dom.DOMTest";
  }

  /**
   * add, remove.
   */
  public void testAddRemove() {
    Document doc = Document.get();
    SelectElement select = doc.createSelectElement();
    doc.getBody().appendChild(select);

    OptionElement opt0 = doc.createOptionElement();
    OptionElement opt1 = doc.createOptionElement();
    OptionElement opt2 = doc.createOptionElement();
    opt0.setText("foo");
    opt1.setText("bar");
    opt2.setText("baz");
    opt0.setValue("0");
    opt1.setValue("1");
    opt2.setValue("2");

    select.appendChild(opt0);
    select.appendChild(opt1);
    select.appendChild(opt2);

    assertEquals("3 options expected", 3, select.getOptions().getLength());
    assertEquals("[0] == opt0", opt0, select.getOptions().getItem(0));
    assertEquals("[1] == opt1", opt1, select.getOptions().getItem(1));
    assertEquals("[2] == opt2", opt2, select.getOptions().getItem(2));

    select.remove(1);
    assertNull("null parent expected when removed", opt1.getParentElement());

    select.add(opt1, opt0);
    assertEquals("[0] == opt1", opt1, select.getOptions().getItem(0));
    assertEquals("[1] == opt0", opt0, select.getOptions().getItem(1));
  }

  /**
   * selectedIndex, option.selected.
   */
  public void testSelection() {
    Document doc = Document.get();
    SelectElement select = doc.createSelectElement();
    doc.getBody().appendChild(select);

    OptionElement opt0 = doc.createOptionElement();
    OptionElement opt1 = doc.createOptionElement();
    OptionElement opt2 = doc.createOptionElement();
    opt0.setText("foo");
    opt1.setText("bar");
    opt2.setText("baz");
    opt0.setValue("0");
    opt1.setValue("1");
    opt2.setValue("2");

    select.appendChild(opt0);
    select.appendChild(opt1);
    select.appendChild(opt2);

    // Single selection.
    opt0.setSelected(true);
    assertTrue(opt0.isSelected());
    assertEquals(0, select.getSelectedIndex());

    opt1.setSelected(true);
    assertFalse(opt0.isSelected());
    assertTrue(opt1.isSelected());
    assertEquals(1, select.getSelectedIndex());
  }

  /**
   * multiple.
   */
  public void testMultipleSelection() {
    Document doc = Document.get();
    SelectElement select = doc.createSelectElement(true);
    doc.getBody().appendChild(select);

    OptionElement opt0 = doc.createOptionElement();
    OptionElement opt1 = doc.createOptionElement();
    OptionElement opt2 = doc.createOptionElement();
    opt0.setText("foo");
    opt1.setText("bar");
    opt2.setText("baz");
    opt0.setValue("0");
    opt1.setValue("1");
    opt2.setValue("2");

    select.appendChild(opt0);
    select.appendChild(opt1);
    select.appendChild(opt2);

    // Multiple selection.
    opt0.setSelected(true);
    opt1.setSelected(true);
    opt2.setSelected(true);

    assertTrue(select.isMultiple());
    assertTrue(opt0.isSelected());
    assertTrue(opt1.isSelected());
    assertTrue(opt2.isSelected());
  }
  
  /**
   * Test optgroups.
   * 
   * @see <a href="http://code.google.com/p/google-web-toolkit/issues/detail?id=4916">Issue 4916</a>
   */
  @DoNotRunWith(Platform.HtmlUnitBug)
  public void testOptGroups() {
    Document doc = Document.get();
    SelectElement select = doc.createSelectElement();
    doc.getBody().appendChild(select);

    OptionElement opt0 = doc.createOptionElement();
    OptionElement opt1 = doc.createOptionElement();
    OptionElement opt2 = doc.createOptionElement();
    OptGroupElement group1 = doc.createOptGroupElement();
    opt0.setText("foo");
    opt1.setText("bar");
    opt2.setText("baz");
    opt0.setValue("0");
    opt1.setValue("1");
    opt2.setValue("2");
    group1.setLabel("group1");

    select.appendChild(opt0);   // select child 0
    select.appendChild(group1); // select child 1
    group1.appendChild(opt1);
    select.appendChild(opt2);   // select child 2

    assertEquals("3 options expected", 3, select.getOptions().getLength());
    assertEquals("[0] == opt0", opt0, select.getOptions().getItem(0));
    assertEquals("[1] == opt1", opt1, select.getOptions().getItem(1));
    assertEquals("[2] == opt2", opt2, select.getOptions().getItem(2));

    select.remove(1);
    // IE9 seems to have a stricter behavior. removing 1 actually removes the group
    // but opt1 is still a child of group1. 
    assertTrue("null parent expected when removed", 
      opt1.getParentElement() == null || group1.getParentElement() == null);

    select.add(opt1, opt0);
    assertEquals("[0] == opt1", opt1, select.getOptions().getItem(0));
    assertEquals("[1] == opt0", opt0, select.getOptions().getItem(1));
    }
}
