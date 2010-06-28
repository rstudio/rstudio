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
package com.google.gwt.view;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.view.client.AbstractListViewAdapterTest;
import com.google.gwt.view.client.AbstractSelectionModelTest;
import com.google.gwt.view.client.AsyncListViewAdapterTest;
import com.google.gwt.view.client.DefaultNodeInfoTest;
import com.google.gwt.view.client.DefaultSelectionModelTest;
import com.google.gwt.view.client.MultiSelectionModelTest;
import com.google.gwt.view.client.RangeTest;
import com.google.gwt.view.client.SingleSelectionModelTest;

import junit.framework.Test;

/**
 * Tests of the view package.
 */
public class ViewSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test suite for all view classes");

    suite.addTestSuite(AbstractListViewAdapterTest.class);
    suite.addTestSuite(AbstractSelectionModelTest.class);
    suite.addTestSuite(AsyncListViewAdapterTest.class);
    suite.addTestSuite(DefaultNodeInfoTest.class);
    suite.addTestSuite(DefaultSelectionModelTest.class);
    suite.addTestSuite(MultiSelectionModelTest.class);
    suite.addTestSuite(RangeTest.class);
    suite.addTestSuite(SingleSelectionModelTest.class);
    return suite;
  }
}
