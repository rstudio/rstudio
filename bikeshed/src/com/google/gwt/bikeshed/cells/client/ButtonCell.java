/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.bikeshed.cells.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

/**
 * A {@link Cell} used to render a button.
 */
public class ButtonCell extends Cell<String, Void> {

  private static ButtonCell instance;

  public static ButtonCell getInstance() {
    if (instance == null) {
      instance = new ButtonCell();
    }
    return instance;
  }

  private ButtonCell() {
  }

  @Override
  public boolean consumesEvents() {
    return true;
  }

  @Override
  public Void onBrowserEvent(Element parent, String value, Void viewData,
      NativeEvent event, ValueUpdater<String, Void> valueUpdater) {
    if (valueUpdater != null && "mouseup".equals(event.getType())) {
      valueUpdater.update(value, viewData);
    }

    return viewData;
  }

  @Override
  public void render(String data, Void viewData, StringBuilder sb) {
    sb.append("<button>");
    if (data != null) {
      sb.append(data);
    }
    sb.append("</button>");
  }
}
