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
package com.google.gwt.sample.bikeshed.cookbook.client;

import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.cells.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.sample.bikeshed.cookbook.client.ValidatableField.DefaultValidatableField;

/**
 * A String {@link Cell} that supports validation using a
 * {@link ValidatableField}.
 */
public class ValidatableInputCell extends Cell<String, ValidatableField<String>> {

  @Override
  public boolean consumesEvents() {
    return true;
  }

  @Override
  public ValidatableField<String> onBrowserEvent(Element parent, String value,
      ValidatableField<String> viewData, NativeEvent event,
      ValueUpdater<String, ValidatableField<String>> valueUpdater) {
    if (event.getType().equals("change")) {
      InputElement input = parent.getFirstChild().cast();

      // Mark cell as containing a pending change
      input.getStyle().setColor("blue");

      // Create a new ValidatableField if needed
      if (viewData == null) {
        viewData = new DefaultValidatableField<String>(input.getValue());
      }
      viewData.setValue(input.getValue());
      valueUpdater.update(value, viewData);
    }

    return viewData;
  }

  @Override
  public void render(String value, ValidatableField<String> viewData, StringBuilder sb) {
    /*
     * If viewData is null, just paint the contents black. If it is non-null,
     * show the pending value and paint the contents red if they are known to be
     * invalid.
     */
    String pendingValue = viewData == null ? null : viewData.getValue();
    boolean invalid = viewData == null ? false : viewData.isInvalid();

    sb.append("<input type=\"text\" value=\"");
    if (pendingValue != null) {
      sb.append(pendingValue);
    } else {
      sb.append(value);
    }
    sb.append("\" style=\"color:");
    if (pendingValue != null) {
      sb.append(invalid ? "red" : "blue");
    } else {
      sb.append("black");
    }
    sb.append("\"></input>");
  }
}
