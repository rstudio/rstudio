/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.user.client.ui;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.cellview.client.CellList;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link ValuePicker}.
 */
public class ValuePickerTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  // See https://code.google.com/p/google-web-toolkit/issues/detail?id=8613
  public void testSetAcceptableValuesWithShorterList() {
    CellList<String> cellList = new CellList<String>(new TextCell());
    ValuePicker<String> picker = new ValuePicker<String>(cellList);

    List<String> acceptableValues = Arrays.asList("Zeroth", "First", "Second", "Third");
    picker.setAcceptableValues(acceptableValues);
    assertEquals(acceptableValues, cellList.getVisibleItems());

    acceptableValues = Arrays.asList("First", "Second");
    picker.setAcceptableValues(acceptableValues);
    assertEquals(acceptableValues, cellList.getVisibleItems());
  }
}
