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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Demonstration of templated UI. Used by UiBinderTest
 */
public class UiBinderTestApp implements EntryPoint {
  private static UiBinderTestApp instance;

  /**
   * Ensure the singleton instance has installed its UI, and return it.
   */
  public static UiBinderTestApp getInstance() {
    if (instance == null) {
      setAndInitInstance(new UiBinderTestApp());
    }

    return instance;
  }

  private static void setAndInitInstance(UiBinderTestApp newInstance) {
    instance = newInstance;
    instance.domUi = new DomBasedUi("Mr. User Man");
    Document.get().getBody().appendChild(instance.domUi.root);

    instance.widgetUi = new WidgetBasedUi();
    RootPanel.get().add(instance.widgetUi);
  }

  private DomBasedUi domUi;

  private WidgetBasedUi widgetUi;

  private UiBinderTestApp() {
  }

  public DomBasedUi getDomUi() {
    return domUi;
  }

  public WidgetBasedUi getWidgetUi() {
    return widgetUi;
  }

  /**
   * Entry point method, called only when this is run as an application.
   */
  public void onModuleLoad() {
    setAndInitInstance(this);
  }
}
