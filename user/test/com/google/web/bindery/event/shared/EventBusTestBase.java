/*
 * Copyright 2011 Google Inc.
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

package com.google.web.bindery.event.shared;

import junit.framework.TestCase;

import java.util.HashSet;

/**
 * Support code for handler tests.
 */
public abstract class EventBusTestBase extends TestCase {

  /**
   * Handler implementation to allow for easy testing of whether the handler is being called.
   */
  protected class Adaptor implements FooEvent.Handler, BarEvent.Handler {

    public void onFoo(FooEvent event) {
      add(this);
    }

    public void onBar(BarEvent event) {
      add(this);
    }

    @Override
    public String toString() {
      return "adaptor 1";
    }
  }

  protected Adaptor adaptor1 = new Adaptor();

  private HashSet<Object> active = new HashSet<Object>();

  protected FooEvent.Handler fooHandler1 = new FooEvent.Handler() {
    public void onFoo(FooEvent event) {
      add(fooHandler1);
    }

    @Override
    public String toString() {
      return "fooHandler 1";
    }
  };

  protected FooEvent.Handler fooHandler2 = new FooEvent.Handler() {
    public void onFoo(FooEvent event) {
      add(fooHandler2);
    }

    @Override
    public String toString() {
      return "fooHandler 2";
    }
  };

  protected FooEvent.Handler fooHandler3 = new FooEvent.Handler() {
    public void onFoo(FooEvent event) {
      add(fooHandler3);
    }

    @Override
    public String toString() {
      return "fooHandler 3";
    }
  };

  protected BarEvent.Handler barHandler1 = new BarEvent.Handler() {

    public void onBar(BarEvent event) {
      add(barHandler1);
    }

    @Override
    public String toString() {
      return "barHandler 1";
    }
  };

  protected BarEvent.Handler barHandler2 = new BarEvent.Handler() {

    public void onBar(BarEvent event) {
      add(barHandler2);
    }

    @Override
    public String toString() {
      return "barHandler 2";
    }
  };

  protected BarEvent.Handler barHandler3 = new BarEvent.Handler() {

    public void onBar(BarEvent event) {
      add(barHandler3);
    }

    @Override
    public String toString() {
      return "barHandler 3";
    }
  };

  protected void add(Object handler) {
    active.add(handler);
  }

  protected void assertFired(Object... handler) {
    for (int i = 0; i < handler.length; i++) {
      assertTrue(handler[i] + " should have fired", active.contains(handler[i]));
    }
  }

  protected void assertNotFired(Object... handler) {
    for (int i = 0; i < handler.length; i++) {
      assertFalse(handler[i] + " should not have fired",
          active.contains(handler[i]));
    }
  }

  protected void reset() {
    active.clear();
  }

}
