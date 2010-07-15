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
package com.google.gwt.cell.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;

/**
 * A {@link AbstractCell} used to render a text input.
 *
 * <p>
 * Note: This class is new and its interface subject to change.
 * </p>
 */
public class TextInputCell extends AbstractEditableCell<String, String> {

  @Override
  public boolean consumesEvents() {
    return true;
  }

  @Override
  public void onBrowserEvent(Element parent, String value, Object key,
      NativeEvent event, ValueUpdater<String> valueUpdater) {
    String eventType = event.getType();
    if ("change".equals(eventType)) {
      InputElement input = parent.getFirstChild().cast();
      String newValue = input.getValue();
      setViewData(key, newValue);
      if (valueUpdater != null) {
        valueUpdater.update(newValue);
      }
    } else if ("keyup".equals(eventType)) {
      // Record keys as they are typed.
      InputElement input = parent.getFirstChild().cast();
      setViewData(key, input.getValue());
    }
  }

  @Override
  public void render(String value, Object key, StringBuilder sb) {
    // Get the view data.
    String viewData = getViewData(key);
    if (viewData != null && viewData.equals(value)) {
      clearViewData(key);
      viewData = null;
    }

    sb.append("<input type='text'");
    if (viewData != null) {
      sb.append(" value='" + viewData + "'");
    } else if (value != null) {
      sb.append(" value='" + value + "'");
    }
    sb.append("></input>");
  }
}
