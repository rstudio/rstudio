/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests the basic widget infrastructure.
 * 
 */
public class WidgetTest extends GWTTestCase {

  ClickHandler handlerA = new ClickHandler() {

    public void onClick(ClickEvent event) {
    }
  };

  ClickHandler handlerB = new ClickHandler() {

    public void onClick(ClickEvent event) {
    }
  };

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testCreateHandlerManager() {
    final HandlerManager manager = new HandlerManager(null);
    Widget w = new Widget() {
      @Override
      protected HandlerManager createHandlerManager() {
        return manager;
      }
    };
    w.ensureHandlers();
    assertEquals(manager, w.getHandlerManager());
  }
  
  public void testAsWidget() {
    Widget a = new Widget();
    assertSame(a, a.asWidget());
  }

  public void testHandlerCount() {
    Widget a = new Widget();
    assertEquals(0, a.getHandlerCount(ClickEvent.getType()));
    HandlerRegistration r1 = a.addDomHandler(handlerA, ClickEvent.getType());
    assertEquals(1, a.getHandlerCount(ClickEvent.getType()));
    HandlerRegistration r2 = a.addHandler(handlerB, ClickEvent.getType());
    assertEquals(2, a.getHandlerCount(ClickEvent.getType()));

    assertEquals(0, a.getHandlerCount(ChangeEvent.getType()));
    r1.removeHandler();
    r2.removeHandler();
    assertEquals(0, a.getHandlerCount(ClickEvent.getType()));
  }

  public void testOnAttachWithErrorDoAttachChildren() {
    // Create a panel that will throw an exception doAttachChildren
    BadWidget w = new BadWidget();
    w.setFailAttachChildren(true);
    assertFalse(w.isAttached());
    assertNull(w.getParent());

    // Attach the widget.
    try {
      RootPanel.get().add(w);
      fail("Expected AttachDetachException");
    } catch (AttachDetachException e) {
      // Expected.
    }
    assertTrue(w.isAttached());
    assertEquals(RootPanel.get(), w.getParent());

    // Detach the widget.
    RootPanel.get().remove(w);
    assertFalse(w.isAttached());
    assertNull(w.getParent());
  }

  public void testOnAttachWithErrorOnLoad() {
    // Create a widget that will throw an exception onLoad.
    BadWidget w = new BadWidget();
    w.setFailOnLoad(true);
    assertFalse(w.isAttached());
    assertNull(w.getParent());

    // Attach the widget.
    try {
      RootPanel.get().add(w);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
    assertTrue(w.isAttached());
    assertEquals(RootPanel.get(), w.getParent());

    // Detach the widget.
    RootPanel.get().remove(w);
    assertFalse(w.isAttached());
    assertNull(w.getParent());
  }

  public void testOnDetachWithErrorDoDetachChildren() {
    // Create a widget that will throw an exception onUnload.
    BadWidget w = new BadWidget();
    w.setFailDetachChildren(true);
    assertFalse(w.isAttached());
    assertNull(w.getParent());

    // Attach the widget.
    RootPanel.get().add(w);
    assertTrue(w.isAttached());
    assertEquals(RootPanel.get(), w.getParent());

    // Detach the widget.
    try {
      RootPanel.get().remove(w);
      fail("Expected AttachDetachException");
    } catch (AttachDetachException e) {
      // Expected.
    }
    assertFalse(w.isAttached());
    assertNull(w.getParent());
  }

  public void testOnDetachWithErrorOnUnload() {
    // Create a widget that will throw an exception onUnload.
    BadWidget w = new BadWidget();
    w.setFailOnUnload(true);
    assertFalse(w.isAttached());
    assertNull(w.getParent());

    // Attach the widget.
    RootPanel.get().add(w);
    assertTrue(w.isAttached());
    assertEquals(RootPanel.get(), w.getParent());

    // Detach the widget.
    try {
      RootPanel.get().remove(w);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
    assertFalse(w.isAttached());
    assertNull(w.getParent());
  }

  public void testSetParentWithErrorOnUnload() {
    // Create a widget that will throw an exception onUnload.
    BadWidget w = new BadWidget();
    w.setFailOnUnload(true);
    assertFalse(w.isAttached());
    assertNull(w.getParent());

    // Attach the widget.
    RootPanel.get().add(w);
    assertTrue(w.isAttached());
    assertEquals(RootPanel.get(), w.getParent());

    // Detach the widget.
    try {
      w.removeFromParent();
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
    assertFalse(w.isAttached());
    assertNull(w.getParent());
  }
}
