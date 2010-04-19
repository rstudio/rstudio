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
 * A {@link Cell} used to render text.  Clicking on the call causes its
 * @{link ValueUpdater} to be called.
 */
public class ClickableTextCell extends Cell<String, Void> {

  private static ClickableTextCell instance;

  public static ClickableTextCell getInstance() {
    if (instance == null) {
      instance = new ClickableTextCell();
    }
    return instance;
  }

  private ClickableTextCell() {
  }

  @Override
  public Void onBrowserEvent(Element parent, String value, Void viewData,
      NativeEvent event, ValueUpdater<String, Void> valueUpdater) {
    String type = event.getType();
    System.out.println(type);
    if (type.equals("click")) {
      valueUpdater.update(value, null);
    }
    return null;
  }

  @Override
  public void render(String value, Void viewData, StringBuilder sb) {
    if (value != null) {
      sb.append(value);
    }
  }
}
