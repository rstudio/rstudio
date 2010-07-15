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

import com.google.gwt.cell.client.AbstractEditableCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.sample.bikeshed.cookbook.client.ValidatableField.DefaultValidatableField;

/**
 * A String {@link AbstractEditableCell} that supports validation using a
 * {@link ValidatableField}.
 */
public class ValidatableInputCell extends AbstractEditableCell<
    String, ValidatableField<String>> {

  @Override
  public boolean consumesEvents() {
    return true;
  }

  @Override
  public void onBrowserEvent(Element parent, String value, Object key,
      NativeEvent event, ValueUpdater<String> valueUpdater) {
    ValidatableField<String> vf = getViewData(key);

    if (event.getType().equals("change")) {
      InputElement input = parent.getFirstChild().cast();

      // Mark cell as containing a pending change
      input.getStyle().setColor("blue");

      // Create a new ValidatableField if needed
      if (vf == null) {
        vf = new DefaultValidatableField<String>(input.getValue());
        setViewData(key, vf);
      }
      vf.setValue(input.getValue());
      valueUpdater.update(vf.getValue());
    }
  }

  @Override
  public void render(String value, Object key, StringBuilder sb) {
    // Get the view data.
    ValidatableField<String> vf = getViewData(key);
    if (vf != null && vf.getValue().equals(value)) {
      clearViewData(key);
      vf = null;
    }

    /*
     * If viewData is null, just paint the contents black. If it is non-null,
     * show the pending value and paint the contents red if they are known to be
     * invalid.
     */
    String pendingValue = vf == null ? null : vf.getValue();
    boolean invalid = vf == null ? false : vf.isInvalid();

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
