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

/**
 * A {@link Cell} used to render profit and loss.  Positive values are shown in
 * green with a "+" sign and negative values are shown in red with a "-" sign.
 */
public class ProfitLossCell extends Cell<Integer, Void> {

  @Override
  public void render(Integer priceDelta, Void viewData, StringBuilder sb) {
    boolean negative = priceDelta < 0;
    if (negative) {
      priceDelta = -priceDelta;
    }
    int dollars = priceDelta / 100;
    int cents = priceDelta % 100;

    sb.append("<span style=\"color:");
    if (priceDelta == 0) {
      sb.append("green\">  ");
    } else if (negative) {
      sb.append("red\">-");
    } else {
      sb.append("green\">+");
    }
    sb.append("$");
    sb.append(dollars);
    sb.append('.');
    if (cents < 10) {
      sb.append('0');
    }
    sb.append(cents);
    sb.append("</span>");
  }
}
