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

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;

/**
 * Test cases for {@link StackPanel}.
 */
public class StackPanelTest extends PanelTestBase<StackPanel> {

  static class Adder implements HasWidgetsTester.WidgetAdder {
    public void addChild(HasWidgets container, Widget child) {
      ((StackPanel) container).add(child, "foo");
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.DebugTest";
  }

  public String curContents(StackPanel p) {
    String accum = "";
    int size = p.getWidgetCount();
    for (int i = 0; i < size; i++) {
      Label l = (Label) p.getWidget(i);
      if (i != 0) {
        accum += " ";
      }
      accum += l.getText();
    }
    return accum;
  }

  @Override
  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(createStackPanel(), new Adder(), true);
  }

  public void testDebugId() {
    final StackPanel p = createStackPanel();
    Label a = new Label("a");
    Label b = new Label("b");
    Label c = new Label("c");
    p.add(a, "header a");
    p.add(b, "header b");
    p.add(c, "header c");
    RootPanel.get().add(p);

    p.ensureDebugId("myStack");

    // Check the body ids
    UIObjectTest.assertDebugId("myStack", p.getElement());
    UIObjectTest.assertDebugId("myStack-content0",
        DOM.getParent(a.getElement()));
    UIObjectTest.assertDebugId("myStack-content1",
        DOM.getParent(b.getElement()));
    UIObjectTest.assertDebugId("myStack-content2",
        DOM.getParent(c.getElement()));

    delayTestFinish(5000);

    // Check the header IDs
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        UIObjectTest.assertDebugIdContents("myStack-text0", "header a");
        UIObjectTest.assertDebugIdContents("myStack-text1", "header b");
        UIObjectTest.assertDebugIdContents("myStack-text2", "header c");

        Element td0 = DOM.getElementById("gwt-debug-myStack-text-wrapper0");
        Element td1 = DOM.getElementById("gwt-debug-myStack-text-wrapper1");
        Element td2 = DOM.getElementById("gwt-debug-myStack-text-wrapper2");

        assertEquals(p.getElement(),
            DOM.getParent(DOM.getParent(DOM.getParent(td0))));
        assertEquals(p.getElement(),
            DOM.getParent(DOM.getParent(DOM.getParent(td1))));
        assertEquals(p.getElement(),
            DOM.getParent(DOM.getParent(DOM.getParent(td2))));

        RootPanel.get().remove(p);
        finishTest();
      }
    });
  }

  /**
   * Tests getSelectedStack.
   */
  public void testGetSelectedStack() {
    StackPanel p = createStackPanel();
    assertEquals(-1, p.getSelectedIndex());
    Label a = new Label("a");
    Label b = new Label("b");
    Label c = new Label("c");
    Label d = new Label("d");
    p.add(a);
    p.add(b, "b");
    p.add(c);
    p.add(d, "d");
    assertEquals(0, p.getSelectedIndex());
    p.showStack(2);
    assertEquals(2, p.getSelectedIndex());
    p.showStack(-1);
    assertEquals(2, p.getSelectedIndex());
  }

  /**
   * Tests new remove implementation for StackPanel.
   */
  public void testRemove() {
    StackPanel p = createStackPanel();
    Label a = new Label("a");
    Label b = new Label("b");
    Label c = new Label("c");
    Label d = new Label("d");
    p.add(a);
    p.add(b, "b");
    p.add(c);
    p.add(d, "d");
    assertEquals("a b c d", curContents(p));

    // Remove c
    p.remove(c);
    assertEquals("a b d", curContents(p));

    // Remove b.
    p.remove(1);
    assertEquals("a d", curContents(p));

    // Remove non-existant element
    assertFalse(p.remove(b));

    // Remove a.
    p.remove(a);
    assertEquals("d", curContents(p));

    // Remove d.
    p.remove(a);
    assertEquals("d", curContents(p));
  }

  /**
   * Create a new stack panel.
   */
  protected StackPanel createStackPanel() {
    return new StackPanel();
  }

  @Override
  protected StackPanel createPanel() {
    return createStackPanel();
  }
}
