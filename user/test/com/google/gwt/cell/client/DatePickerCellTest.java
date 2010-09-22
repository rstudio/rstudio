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

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;

import java.util.Date;

/**
 * Tests for {@link DatePickerCell}.
 */
public class DatePickerCellTest extends EditableCellTestBase<Date, Date> {

  @Override
  protected DatePickerCell createCell() {
    return new DatePickerCell(
        DateTimeFormat.getFormat(PredefinedFormat.DATE_MEDIUM));
  }

  @Override
  @SuppressWarnings("deprecation")
  protected Date createCellValue() {
    return new Date(2010 - 1900, 0, 1);
  }

  @Override
  @SuppressWarnings("deprecation")
  protected Date createCellViewData() {
    return new Date(2010 - 1900, 0, 3);
  }

  @Override
  protected boolean dependsOnSelection() {
    return false;
  }

  @Override
  protected String[] getConsumedEvents() {
    return new String[]{"click", "keydown"};
  }

  @Override
  protected String getExpectedInnerHtml() {
    return "Jan 1, 2010";
  }

  @Override
  protected String getExpectedInnerHtmlNull() {
    return "";
  }

  @Override
  protected String getExpectedInnerHtmlViewData() {
    return "Jan 3, 2010";
  }
}
