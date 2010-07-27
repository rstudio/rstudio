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
 * Tests for {@link ButtonCell}.
 */
public class NumberCellTest extends CellTestBase<Number> {

  @Override
  protected Cell<Number> createCell() {
    return new NumberCell(NumberFormat.getFormat("#.##"));
  }

  @Override
  protected Number createCellValue() {
    return new Double(100.12);
  }

  @Override
  protected boolean dependsOnSelection() {
    return false;
  }

  @Override
  protected String[] getConsumedEvents() {
    return null;
  }

  @Override
  protected String getExpectedInnerHtml() {
    return "100.12";
  }

  @Override
  protected String getExpectedInnerHtmlNull() {
    return "";
  }
}
