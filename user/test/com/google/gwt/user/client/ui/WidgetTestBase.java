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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Convenience class to use user module and a default tear down code for widget
 * tests.
 */
public class WidgetTestBase extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testAsWidget() {
    Widget a = new Widget();
    assertSame(a, a.asWidget());
  }

  /**
   * A replacement for JUnit's {@link #tearDown()} method. This method runs once
   * per test method in your subclass, just after your each test method runs and
   * can be used to perform cleanup. Override this method instead of
   * {@link #tearDown()}.
   */
  @Override
  protected void gwtTearDown() {
    RootPanel.get().clear();
  }
}
