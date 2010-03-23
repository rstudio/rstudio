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
package com.google.gwt.sample.bikeshed.stocks.client;

import com.google.gwt.bikeshed.cells.client.Cell;

/**
 * A cell that represents a {@link StockQuote}.
 */
public class ChangeCell extends Cell<String, Void> {

  @Override
  public void render(String value, Void viewData, StringBuilder sb) {
    if (value == null || value.length() == 0) {
      return;
    }
    sb.append("<span style=\"color:");
    if (value.charAt(0) == '-') {
      sb.append("red\">");
    } else {
      sb.append("green\">");
    }
    sb.append(value);
    sb.append("</span>");
  }
}
