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
package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;

/**
 * Only a single GWT application can preview native events.
 */
public class Issue3892EntryPoint2 implements EntryPoint {
  public void onModuleLoad() {
    Window.alert("Module 2 loaded");
    Event.addNativePreviewHandler(new NativePreviewHandler() {
      public void onPreviewNativeEvent(NativePreviewEvent event) {
        if (event.getTypeInt() == Event.ONCLICK) {
          Element target = event.getNativeEvent().getEventTarget().cast();
          if (Issue3892EntryPoint1.BUTTON_3_ID.equals(target.getId())) {
            event.cancel();
            Window.alert("Click handled by module 2 and cancelled");
          } else {
            Window.alert("Click handled by module 2");
          }
        }
      }
    });
  }
}
