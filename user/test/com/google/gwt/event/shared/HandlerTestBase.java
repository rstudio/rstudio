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

package com.google.gwt.event.shared;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.junit.client.GWTTestCase;

import java.util.HashSet;

/**
 * Support code for handler tests.
 */
public abstract class HandlerTestBase extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.event.Event";
  }

  class Adaptor implements ClickHandler, MouseDownHandler {

    public void onClick(ClickEvent event) {
      add(this);
    }

    public void onMouseDown(MouseDownEvent event) {
      add(this);
    }

    @Override
    public String toString() {
      return "adaptor 1";
    }
  }

  Adaptor adaptor1 = new Adaptor();

  private HashSet<EventHandler> active = new HashSet<EventHandler>();

  MouseDownHandler mouse1 = new MouseDownHandler() {
    public void onMouseDown(MouseDownEvent event) {
      add(mouse1);
    }

    @Override
    public String toString() {
      return "mouse 1";
    }
  };

  MouseDownHandler mouse2 = new MouseDownHandler() {
    public void onMouseDown(MouseDownEvent event) {
      add(mouse2);
    }

    @Override
    public String toString() {
      return "mouse 2";
    }
  };

  MouseDownHandler mouse3 = new MouseDownHandler() {
    public void onMouseDown(MouseDownEvent event) {
      add(mouse3);
    }

    @Override
    public String toString() {
      return "mouse 3";
    }
  };

  ClickHandler click1 = new ClickHandler() {

    public void onClick(ClickEvent event) {
      add(click1);
    }

    @Override
    public String toString() {
      return "click 1";
    }
  };

  ClickHandler click2 = new ClickHandler() {

    public void onClick(ClickEvent event) {
      add(click2);
    }

    @Override
    public String toString() {
      return "click 2";
    }
  };

  ClickHandler click3 = new ClickHandler() {

    public void onClick(ClickEvent event) {
      add(click3);
    }

    @Override
    public String toString() {
      return "click 3";
    }
  };

  void add(EventHandler handler) {
    active.add(handler);
  }

  void assertFired(EventHandler... handler) {
    for (int i = 0; i < handler.length; i++) {
      assertTrue(handler[i] + " should have fired", active.contains(handler[i]));
    }
  }

  void assertNotFired(EventHandler... handler) {
    for (int i = 0; i < handler.length; i++) {
      assertFalse(handler[i] + " should not have fired",
          active.contains(handler[i]));
    }
  }

  void reset() {
    active.clear();
  }

}
