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
package com.google.gwt.user.cellview;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.user.cellview.client.AbstractPagerTest;
import com.google.gwt.user.cellview.client.AnimatedCellTreeTest;
import com.google.gwt.user.cellview.client.CellBrowserTest;
import com.google.gwt.user.cellview.client.CellListTest;
import com.google.gwt.user.cellview.client.CellTableTest;
import com.google.gwt.user.cellview.client.CellTreeTest;
import com.google.gwt.user.cellview.client.CellWidgetTest;
import com.google.gwt.user.cellview.client.ColumnSortEventTest;
import com.google.gwt.user.cellview.client.ColumnSortInfoTest;
import com.google.gwt.user.cellview.client.ColumnSortListTest;
import com.google.gwt.user.cellview.client.ColumnTest;
import com.google.gwt.user.cellview.client.DataGridTest;
import com.google.gwt.user.cellview.client.HasDataPresenterTest;
import com.google.gwt.user.cellview.client.PageSizePagerTest;
import com.google.gwt.user.cellview.client.SimplePagerTest;

import junit.framework.Test;

/**
 * Tests of the cellview package.
 */
public class CellViewSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test suite for all cellview classes");

    suite.addTestSuite(AbstractPagerTest.class);
    suite.addTestSuite(AnimatedCellTreeTest.class);
    suite.addTestSuite(CellBrowserTest.class);
    suite.addTestSuite(CellListTest.class);
    suite.addTestSuite(CellTableTest.class);
    suite.addTestSuite(CellTreeTest.class);
    suite.addTestSuite(CellWidgetTest.class);
    suite.addTestSuite(ColumnSortEventTest.class);
    suite.addTestSuite(ColumnSortInfoTest.class);
    suite.addTestSuite(ColumnSortListTest.class);
    suite.addTestSuite(ColumnTest.class);
    suite.addTestSuite(DataGridTest.class);
    suite.addTestSuite(HasDataPresenterTest.class);
    suite.addTestSuite(PageSizePagerTest.class);
    suite.addTestSuite(SimplePagerTest.class);
    return suite;
  }
}
