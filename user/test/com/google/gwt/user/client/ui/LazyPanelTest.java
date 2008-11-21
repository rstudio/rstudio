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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests {@link LazyPanel}.
 */
public class LazyPanelTest extends GWTTestCase {
  private final static class MyLazyPanel extends LazyPanel {
    final Widget widgetToCreate;
    boolean createWasCalled;

    public MyLazyPanel(Widget widgetToCreate) {
      this.widgetToCreate = widgetToCreate;
    }

    @Override
    protected Widget createWidget() {
      createWasCalled = true;
      return widgetToCreate;
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testSetVisible() {
    Widget w = new Label();
    MyLazyPanel p = new MyLazyPanel(w);
    assertNull(p.getWidget());
    assertNull(w.getParent());

    RootPanel.get().add(p);
    assertNull(w.getParent());

    p.setVisible(true);
    assertWidgetIsChildOf(w, p);
    
    p.createWasCalled = false;
    p.setVisible(false);
    assertFalse(p.createWasCalled);
    
    p.setVisible(true);
    assertFalse("Should not call createWidget again", p.createWasCalled);
  }

  public void testEnsureWidget() {
    Widget w = new Label();
    MyLazyPanel p = new MyLazyPanel(w);
    
    p.ensureWidget();
    assertWidgetIsChildOf(w, p);
    assertEquals(w, p.getWidget());
  }

  private void assertWidgetIsChildOf(Widget w, Widget p) {
    Widget parentCursor = w;
    while (parentCursor != null && parentCursor != RootPanel.get()) {
      parentCursor = parentCursor.getParent();
      if (p.equals(parentCursor)) {
        break;
      }
    }
    assertEquals("Expect w to be child of p", p, parentCursor);
  }
  
  public void testInDeckPanel() {
    // There are separate paths for the first widget displayed 
    // and for succeeding, so test both (see DeckPanel#showWidget)
    
    DeckPanel deck = new DeckPanel();

    Widget w0 = new Label();
    deck.insert(new MyLazyPanel(w0), 0);
    assertNull(w0.getParent());

    Widget w1 = new Label();
    deck.insert(new MyLazyPanel(w1), 1);
    assertNull(w0.getParent());
    assertNull(w1.getParent());
    
    deck.showWidget(0);
    assertWidgetIsChildOf(w0, deck);
    assertNull(w1.getParent());

    deck.showWidget(1);
    assertWidgetIsChildOf(w1, deck);
  }
  
  public void testInStackPanel() {
    StackPanel stack = new StackPanel();
    stack.add(new Label(), "Able");

    Widget w = new Label();
    stack.add(new MyLazyPanel(w), "Baker");
    assertNull(w.getParent());
    
    stack.showStack(1);
    assertWidgetIsChildOf(w, stack);
  }
  
  public void testInDisclosurePanel() {
    Widget w = new Label();
    DisclosurePanel dp = new DisclosurePanel();

    dp.add(new MyLazyPanel(w));
    assertNull(w.getParent());
    
    dp.setOpen(true);
    assertWidgetIsChildOf(w, dp);
  }
  
  public void testInAnimatedDisclosurePanel() {
    Widget w = new Label();
    DisclosurePanel dp = new DisclosurePanel();
    dp.setAnimationEnabled(true);

    dp.add(new MyLazyPanel(w));
    assertNull(w.getParent());
    
    dp.setOpen(true);
    assertWidgetIsChildOf(w, dp);
  }
}
