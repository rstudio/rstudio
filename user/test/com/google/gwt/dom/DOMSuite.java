/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dom;

import junit.framework.Test;

import com.google.gwt.dom.client.DocumentTest;
import com.google.gwt.dom.client.ElementTest;
import com.google.gwt.dom.client.FormTests;
import com.google.gwt.dom.client.MapTests;
import com.google.gwt.dom.client.NodeTest;
import com.google.gwt.dom.client.SelectTests;
import com.google.gwt.dom.client.TableTests;
import com.google.gwt.junit.tools.GWTTestSuite;

public class DOMSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite(
        "Test for suite for the com.google.gwt.dom module");

    suite.addTestSuite(DocumentTest.class);
    suite.addTestSuite(NodeTest.class);
    suite.addTestSuite(ElementTest.class);
    suite.addTestSuite(FormTests.class);
    suite.addTestSuite(MapTests.class);
    suite.addTestSuite(SelectTests.class);
    suite.addTestSuite(TableTests.class);

    return suite;
  }
}
