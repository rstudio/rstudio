/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.uibinder.test.client;

import static com.google.gwt.uibinder.test.client.ValueChangeWidget.HandlerType.HANDLER;
import static com.google.gwt.uibinder.test.client.ValueChangeWidget.HandlerType.HANDLER_LIST_T;
import static com.google.gwt.uibinder.test.client.ValueChangeWidget.HandlerType.HANDLER_STRING;
import static com.google.gwt.uibinder.test.client.ValueChangeWidget.HandlerType.HANDLER_T;
import static com.google.gwt.uibinder.test.client.ValueChangeWidget.HandlerType.HANDLER_WILDCARD;
import static com.google.gwt.uibinder.test.client.ValueChangeWidget.HandlerType.SELECT_HANDLER_T;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Functional test for UiHandler
 */
public class UiHandlerTest extends GWTTestCase {
  private WidgetBasedUi widgetUi;

  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.test.UiBinderSuite";
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    UiBinderTestApp app = UiBinderTestApp.getInstance();
    widgetUi = app.getWidgetUi();
  }

  public void testValueChangeEvent() {
    widgetUi.doubleValueChangeEvent = null;
    widgetUi.myDoubleBox.setValue(0.0);
    widgetUi.myDoubleBox.setValue(10.0, true);
    assertNotNull(widgetUi.doubleValueChangeEvent);
    assertEquals(10.0, widgetUi.doubleValueChangeEvent.getValue());
  }

  public void testValueChangeHandlers() {
    ValueChangeWidget<?> w = widgetUi.myValueChangeWidget;
    assertEquals(1, w.getHandlerCount(HANDLER_WILDCARD));
    assertEquals(1, w.getHandlerCount(HANDLER_STRING));
    assertEquals(1, w.getHandlerCount(HANDLER)); /* Matched by List<?> */
    assertEquals(1, w.getHandlerCount(HANDLER_T));
    assertEquals(1, w.getHandlerCount(HANDLER_LIST_T));
    assertEquals(0, w.getHandlerCount(SELECT_HANDLER_T));
  }

  public void testValueChangeHandlers_extends() {
    ValueChangeWidget<?> w_extends = widgetUi.myValueChangeWidget_extends;
    assertEquals(1, w_extends.getHandlerCount(HANDLER_WILDCARD));
    assertEquals(1, w_extends.getHandlerCount(HANDLER_STRING));
    assertEquals(1, w_extends.getHandlerCount(HANDLER)); /* Matched by List<?> */
    assertEquals(1, w_extends.getHandlerCount(HANDLER_T));
    assertEquals(1, w_extends.getHandlerCount(HANDLER_LIST_T));
    assertEquals(0, w_extends.getHandlerCount(SELECT_HANDLER_T));
  }

  public void testValueChangeHandlers_raw() {
    ValueChangeWidget<?> w_raw = widgetUi.myValueChangeWidget_raw;
    assertEquals(1, w_raw.getHandlerCount(HANDLER_WILDCARD));
    assertEquals(1, w_raw.getHandlerCount(HANDLER_STRING));
    assertEquals(1, w_raw.getHandlerCount(HANDLER));
    assertEquals(0, w_raw.getHandlerCount(HANDLER_T));
    assertEquals(0, w_raw.getHandlerCount(HANDLER_LIST_T));
    assertEquals(1, w_raw.getHandlerCount(SELECT_HANDLER_T));
  }
}
