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
package com.google.gwt.bikeshed.sample.validation.client;

import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.cells.client.ValueUpdater;
import com.google.gwt.bikeshed.sample.validation.client.ValidatableField.DefaultValidatableField;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;

/**
 * A String Cell that supports validation.
 */
public class ValidatableInputCell extends Cell<ValidatableField<String>> {

  @Override
  public void onBrowserEvent(Element parent, ValidatableField<String> value, NativeEvent event,
      ValueUpdater<ValidatableField<String>> valueUpdater) {
    if (event.getType().equals("change")) {
      InputElement input = parent.getFirstChild().cast();

      // Mark as pending
      input.getStyle().setColor("blue");

      ValidatableField<String> field = new DefaultValidatableField<String>(value);
      field.setPendingValue(input.getValue());
      valueUpdater.update(field);
    }
  }

  @Override
  public void render(ValidatableField<String> value, StringBuilder sb) {
    String pendingValue = value.getPendingValue();
    sb.append("<input type=\"text\" value=\"");
    boolean invalid = value.isInvalid();
    if (pendingValue != null) {
      sb.append(pendingValue);        
    } else {
      sb.append(value.getValue());
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
