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

import com.google.gwt.user.client.DOM;

import java.util.Iterator;

/**
 * A test for {@link VerticalPanel}.
 */
public class VerticalPanelTest extends AbstractCellPanelTest<VerticalPanel> {

  public void testDebugId() {
    VerticalPanel p = new VerticalPanel();
    Label a = new Label("a");
    Label b = new Label("b");
    Label c = new Label("c");
    p.add(a);
    p.add(b);
    p.add(c);

    p.ensureDebugId("myPanel");
    UIObjectTest.assertDebugId("myPanel", p.getElement());
    UIObjectTest.assertDebugId("myPanel-0", DOM.getParent(a.getElement()));
    UIObjectTest.assertDebugId("myPanel-1", DOM.getParent(b.getElement()));
    UIObjectTest.assertDebugId("myPanel-2", DOM.getParent(c.getElement()));
  }

  public void testInsertMultipleTimes() {
    VerticalPanel p = new VerticalPanel();

    TextBox tb = new TextBox();
    p.add(tb);
    p.add(tb);
    p.add(tb);

    assertEquals(1, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(tb));
    Iterator<Widget> i = p.iterator();
    assertTrue(i.hasNext());
    assertTrue(tb.equals(i.next()));
    assertFalse(i.hasNext());

    Label l = new Label();
    p.add(l);
    p.add(l);
    p.add(l);
    assertEquals(2, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(tb));
    assertEquals(1, p.getWidgetIndex(l));

    p.insert(l, 0);
    assertEquals(2, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(l));
    assertEquals(1, p.getWidgetIndex(tb));

    p.insert(l, 1);
    assertEquals(2, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(l));
    assertEquals(1, p.getWidgetIndex(tb));

    p.insert(l, 2);
    assertEquals(2, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(tb));
    assertEquals(1, p.getWidgetIndex(l));
  }

  @Override
  protected CellPanel createCellPanel() {
    VerticalPanel p = new VerticalPanel();
    p.add(new Label("a"));
    p.add(new Label("b"));
    p.add(new Label("c"));
    return p;
  }

  @Override
  protected VerticalPanel createPanel() {
    return new VerticalPanel();
  }
}
