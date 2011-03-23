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
package com.google.gwt.cell;

import com.google.gwt.cell.client.AbstractCellTest;
import com.google.gwt.cell.client.ActionCellTest;
import com.google.gwt.cell.client.ButtonCellTest;
import com.google.gwt.cell.client.CheckboxCellTest;
import com.google.gwt.cell.client.ClickableTextCellTest;
import com.google.gwt.cell.client.CompositeCellTest;
import com.google.gwt.cell.client.DateCellTest;
import com.google.gwt.cell.client.DatePickerCellTest;
import com.google.gwt.cell.client.EditTextCellTest;
import com.google.gwt.cell.client.IconCellDecoratorTest;
import com.google.gwt.cell.client.ImageCellTest;
import com.google.gwt.cell.client.ImageLoadingCellTest;
import com.google.gwt.cell.client.ImageResourceCellTest;
import com.google.gwt.cell.client.NumberCellTest;
import com.google.gwt.cell.client.SelectionCellTest;
import com.google.gwt.cell.client.TextButtonCellTest;
import com.google.gwt.cell.client.TextCellTest;
import com.google.gwt.cell.client.TextInputCellTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Tests of the cell package.
 */
public class CellSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test suite for all cell classes");

    suite.addTestSuite(AbstractCellTest.class);
    suite.addTestSuite(ActionCellTest.class);
    suite.addTestSuite(ButtonCellTest.class);
    suite.addTestSuite(CheckboxCellTest.class);
    suite.addTestSuite(ClickableTextCellTest.class);
    suite.addTestSuite(CompositeCellTest.class);
    suite.addTestSuite(DateCellTest.class);
    suite.addTestSuite(DatePickerCellTest.class);
    suite.addTestSuite(EditTextCellTest.class);
    suite.addTestSuite(IconCellDecoratorTest.class);
    suite.addTestSuite(ImageCellTest.class);
    suite.addTestSuite(ImageLoadingCellTest.class);
    suite.addTestSuite(ImageResourceCellTest.class);
    suite.addTestSuite(NumberCellTest.class);
    suite.addTestSuite(SelectionCellTest.class);
    suite.addTestSuite(TextButtonCellTest.class);
    suite.addTestSuite(TextCellTest.class);
    suite.addTestSuite(TextInputCellTest.class);
    return suite;
  }
}
