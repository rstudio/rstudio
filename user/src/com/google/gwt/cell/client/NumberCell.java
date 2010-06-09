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

import com.google.gwt.i18n.client.NumberFormat;

/**
 * A {@link Cell} used to render formatted numbers.
 */
public class NumberCell extends AbstractCell<Number> {

  /**
   * The {@link NumberFormat} used to render the number.
   */
  private final NumberFormat format;

  /**
   * Construct a new {@link NumberCell} using decimal format.
   */
  public NumberCell() {
    this(NumberFormat.getDecimalFormat());
  }

  /**
   * Construct a new {@link NumberCell}.
   * 
   * @param format the {@link NumberFormat} used to render the number
   */
  public NumberCell(NumberFormat format) {
    this.format = format;
  }

  @Override
  public void render(Number value, Object viewData, StringBuilder sb) {
    if (value != null) {
      sb.append(format.format(value));
    }
  }
}
