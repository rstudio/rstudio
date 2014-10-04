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
package com.google.gwt.resources;

import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.resources.converter.AlternateAnnotationCreatorVisitorTest;
import com.google.gwt.resources.converter.Css2GssTest;
import com.google.gwt.resources.converter.DefCollectorVisitorTest;
import com.google.gwt.resources.converter.ElseNodeCreatorTest;
import com.google.gwt.resources.converter.FontFamilyVisitorTest;
import com.google.gwt.resources.converter.UndefinedConstantVisitorTest;
import com.google.gwt.resources.rg.CssOutputTestCase;

import junit.framework.Test;

/**
 * JRE tests of the ClientBundle framework.
 */
public class ResourcesJreSuite {
  public static Test suite() {

    GWTTestSuite suite = new GWTTestSuite("JRE test for com.google.gwt.resources");
    suite.addTestSuite(Css2GssTest.class);
    suite.addTestSuite(CssOutputTestCase.class);
    suite.addTestSuite(DefCollectorVisitorTest.class);
    suite.addTestSuite(ElseNodeCreatorTest.class);
    suite.addTestSuite(AlternateAnnotationCreatorVisitorTest.class);
    suite.addTestSuite(FontFamilyVisitorTest.class);
    suite.addTestSuite(UndefinedConstantVisitorTest.class);

    return suite;
  }
}
