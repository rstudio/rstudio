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
package com.google.gwt.event.logical.shared;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;

import junit.framework.TestCase;

/**
 * Tests of logical events.
 */
public class LogicalEventsTest extends TestCase {

  static class Fire implements SelectionHandler<String>,
      BeforeSelectionHandler<String>, CloseHandler<String>,
      OpenHandler<String>, ResizeHandler, ValueChangeHandler<String> {
    public boolean flag = false;

    public void onBeforeSelection(BeforeSelectionEvent<String> event) {
      flag = true;
    }

    public void onClose(CloseEvent<String> event) {
      flag = true;
    }

    public void onOpen(OpenEvent<String> event) {
      flag = true;
    }

    public void onResize(ResizeEvent event) {
      flag = true;
    }

    public void onSelection(SelectionEvent<String> event) {
      flag = true;
    }

    public void onValueChange(ValueChangeEvent<String> event) {
      flag = true;
    }
  }

  private HandlerManager manager;

  @Override
  public void setUp() {
    manager = new HandlerManager(this);
  }

  public void testSimpleFire() {
    simpleFire(BeforeSelectionEvent.getType(),
        new BeforeSelectionEvent<String>());
    simpleFire(SelectionEvent.getType(), new SelectionEvent<String>(null));
    simpleFire(CloseEvent.getType(), new CloseEvent<String>(null, false));
    simpleFire(OpenEvent.getType(), new OpenEvent<String>(null));
    simpleFire(ResizeEvent.getType(), new ResizeEvent(0, 0));
    simpleFire(ValueChangeEvent.getType(), new ValueChangeEvent<String>(null));
  }

  @SuppressWarnings("unchecked")
  private <H extends EventHandler> void simpleFire(GwtEvent.Type<H> type,
      @SuppressWarnings("rawtypes") GwtEvent instance) {
    Fire f = new Fire();
    manager.addHandler(type, (H) f);
    manager.fireEvent(instance);
    assertTrue(f.flag);
  }
}
