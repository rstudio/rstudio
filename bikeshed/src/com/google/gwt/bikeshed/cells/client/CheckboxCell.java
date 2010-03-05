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
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;

/**
 * A {@link Cell} used to render a checkbox.
 */
public class CheckboxCell extends Cell<Boolean> {

  @Override
  public void onBrowserEvent(Element parent, Boolean value, NativeEvent event,
      ValueUpdater<Boolean> valueUpdater) {
    if (valueUpdater == null) {
      return;
    }

    if ("change".equals(event.getType())) {
      InputElement input = parent.getFirstChild().cast();
      valueUpdater.update(input.isChecked());
    }
  }

  @Override
  public void render(Boolean data, StringBuilder sb) {
    sb.append("<input type=\"checkbox\"");
    if (data == true) {
      sb.append(" checked");
    }
    sb.append("/>");
  }
}
