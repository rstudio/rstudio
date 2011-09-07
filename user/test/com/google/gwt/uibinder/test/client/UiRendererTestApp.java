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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.core.client.EntryPoint;

/**
 * Demonstration of templated UI. Used by UiRendererTest
 */
public class UiRendererTestApp implements EntryPoint {
  private static UiRendererTestApp instance;

  /**
   * Ensure the singleton instance has installed its UI, and return it.
   */
  public static UiRendererTestApp getInstance() {
    if (instance == null) {
      setAndInitInstance(new UiRendererTestApp());
    }

    return instance;
  }

  private static void setAndInitInstance(UiRendererTestApp newInstance) {
    instance = newInstance;
    instance.uiRendererUi = new UiRendererUi();
  }

  private UiRendererUi uiRendererUi;

  private UiRendererTestApp() {
  }

  public UiRendererUi getUiRendererUi() {
    return uiRendererUi;
  }

  /**
   * Entry point method, called only when this is run as an application.
   */
  public void onModuleLoad() {
    setAndInitInstance(this);
  }
}
