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

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Demonstrates that IsWidget and related interfaces can be used in JRE tests.
 */
public class IsWidgetTest extends TestCase {
  interface SomeView extends IsWidget {
    void blah(); // Empty interface warning be damned
  }

  static class SomeViewMock implements SomeView {
    public Widget asWidget() {
      throw new UnsupportedOperationException();
    }

    public void blah() {
      throw new UnsupportedOperationException();
    }
  }

  static class MockPanel implements HasWidgets.ForIsWidget {
    List<IsWidget> children = new ArrayList<IsWidget>();
    
    public void add(Widget w) {
      throw new UnsupportedOperationException();
    }

    public void clear() {
      children.clear();
    }

    public Iterator<Widget> iterator() {
      throw new UnsupportedOperationException();
    }

    public boolean remove(Widget w) {
      throw new UnsupportedOperationException();
    }

    public void add(IsWidget w) {
      children.add(w);
    }

    public boolean remove(IsWidget w) {
      return children.remove(w);
    }
  }

  static class MockDisplay implements AcceptsOneWidget {
    IsWidget w;

    public void setWidget(IsWidget w) {
      this.w = w;
    }
  }

  public void testIt() {
    SomeViewMock view = new SomeViewMock();
    MockDisplay simple = new MockDisplay();
    simple.setWidget(view);
    assertSame(view, simple.w);
    
    SomeViewMock view2 = new SomeViewMock();
    MockPanel panel = new MockPanel();
    
    panel.add(view);
    panel.add(view2);
    
    panel.clear();
    assertTrue(panel.children.isEmpty());
  }
}
