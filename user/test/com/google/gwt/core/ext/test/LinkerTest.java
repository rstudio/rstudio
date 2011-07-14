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
package com.google.gwt.core.ext.test;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Tests that all the linkers work well enough to run the
 * JUnit testing infrastructure.
 */
public abstract class LinkerTest extends GWTTestCase {
  public void testSomethingTrivial() {
    assertTrue(true);
  }
  
  public void testSomethingBigEnoughToTriggerChunking() {
    RootPanel.get().add(new Label("Hello there"));
    assertEquals(1, RootPanel.get().getWidgetCount());
  }
}
