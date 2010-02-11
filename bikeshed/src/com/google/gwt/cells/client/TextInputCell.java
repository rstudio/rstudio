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
package com.google.gwt.cells.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;

public class TextInputCell extends Cell<String> {

  @Override
  public void render(String data, StringBuilder sb) {
    sb.append("<input type='text'");
    if (data != null) {
      sb.append(" value='" + data + "'");
    }
    sb.append("></input>");
  }

  @Override
  public void onBrowserEvent(Element parent, String value, NativeEvent event,
      Mutator<String, String> mutator) {
    if (mutator == null) {
      return;
    }

    if ("change".equals(event.getType())) {
      InputElement input = parent.getFirstChild().cast();
      mutator.mutate(value, input.getValue());
    }
  }
}
