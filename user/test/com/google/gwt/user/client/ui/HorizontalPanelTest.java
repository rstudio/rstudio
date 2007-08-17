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

import java.util.Iterator;

/**
 * A test for {@link HorizontalPanel}.
 */
public class HorizontalPanelTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testAttachDetachOrder() {
    HasWidgetsTester.testAll(new HorizontalPanel());
  }

  public void testInsertMultipleTimes() {
    HorizontalPanel p = new HorizontalPanel();

    TextBox tb = new TextBox();
    p.add(tb);
    p.add(tb);
    p.add(tb);

    assertEquals(1, p.getWidgetCount());
    assertEquals(0, p.getWidgetIndex(tb));
    Iterator i = p.iterator();
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
}
