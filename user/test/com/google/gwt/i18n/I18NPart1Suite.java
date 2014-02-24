/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.i18n;

import com.google.gwt.i18n.client.AnnotationsTest;
import com.google.gwt.i18n.client.ArabicPluralsTest;
import com.google.gwt.i18n.client.CurrencyTest;
import com.google.gwt.i18n.client.CustomPluralsTest;
import com.google.gwt.i18n.client.DateTimeFormat_de_Test;
import com.google.gwt.i18n.client.DateTimeFormat_en_Test;
import com.google.gwt.i18n.client.DateTimeFormat_fil_Test;
import com.google.gwt.i18n.client.DateTimeFormat_pl_Test;
import com.google.gwt.i18n.client.DateTimeParse_en_Test;
import com.google.gwt.i18n.client.DateTimeParse_zh_CN_Test;
import com.google.gwt.i18n.shared.GwtBidiUtilsTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * I18N tests for client code running as a GWT test.
 */
public class I18NPart1Suite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("I18N client tests");

    // $JUnit-BEGIN$
    suite.addTestSuite(ArabicPluralsTest.class);
    suite.addTestSuite(AnnotationsTest.class);
    suite.addTestSuite(CurrencyTest.class);
    suite.addTestSuite(CustomPluralsTest.class);
    suite.addTestSuite(DateTimeFormat_de_Test.class);
    suite.addTestSuite(DateTimeFormat_en_Test.class);
    suite.addTestSuite(DateTimeFormat_fil_Test.class);
    suite.addTestSuite(DateTimeFormat_pl_Test.class);
    suite.addTestSuite(DateTimeParse_en_Test.class);
    suite.addTestSuite(DateTimeParse_zh_CN_Test.class);
    suite.addTestSuite(GwtBidiUtilsTest.class);
    // $JUnit-END$

    return suite;
  }
}
