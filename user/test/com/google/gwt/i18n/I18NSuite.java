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
import com.google.gwt.i18n.client.I18N2Test;
import com.google.gwt.i18n.client.I18NTest;
import com.google.gwt.i18n.client.I18N_es_MX_RuntimeTest;
import com.google.gwt.i18n.client.I18N_es_MX_Test;
import com.google.gwt.i18n.client.I18N_iw_Test;
import com.google.gwt.i18n.client.I18N_nb_Test;
import com.google.gwt.i18n.client.LocaleInfoTest;
import com.google.gwt.i18n.client.LocaleInfo_ar_Test;
import com.google.gwt.i18n.client.NumberFormat_ar_Test;
import com.google.gwt.i18n.client.NumberFormat_en_Test;
import com.google.gwt.i18n.client.NumberFormat_fr_Test;
import com.google.gwt.i18n.client.NumberParse_en_Test;
import com.google.gwt.i18n.client.NumberParse_fr_Test;
import com.google.gwt.i18n.client.RuntimeLocalesTest;
import com.google.gwt.i18n.client.TimeZoneInfoTest;
import com.google.gwt.i18n.client.TimeZoneTest;
import com.google.gwt.i18n.rebind.MessageFormatParserTest;
import com.google.gwt.i18n.server.GwtLocaleTest;
import com.google.gwt.i18n.server.RegionInheritanceTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * All I18N tests.
 */
public class I18NSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("All I18N tests");

    // $JUnit-BEGIN$
    suite.addTestSuite(ArabicPluralsTest.class);
    suite.addTestSuite(AnnotationsTest.class);
    suite.addTestSuite(ConstantMapTest.class);
    suite.addTestSuite(CurrencyTest.class);
    suite.addTestSuite(CustomPluralsTest.class);
    suite.addTestSuite(DateTimeFormat_de_Test.class);
    suite.addTestSuite(DateTimeFormat_en_Test.class);
    suite.addTestSuite(DateTimeFormat_fil_Test.class);
    suite.addTestSuite(DateTimeFormat_pl_Test.class);
    suite.addTestSuite(DateTimeParse_en_Test.class);
    suite.addTestSuite(DateTimeParse_zh_CN_Test.class);
    suite.addTestSuite(GwtLocaleTest.class);
    suite.addTestSuite(I18NTest.class);
    suite.addTestSuite(I18N2Test.class);
    suite.addTestSuite(I18N_iw_Test.class);
    suite.addTestSuite(I18N_es_MX_Test.class);
    suite.addTestSuite(I18N_es_MX_RuntimeTest.class);
    suite.addTestSuite(I18N_nb_Test.class);
    suite.addTestSuite(LocaleInfo_ar_Test.class);    
    suite.addTestSuite(LocaleInfoTest.class);
    suite.addTestSuite(MessageFormatParserTest.class);
    suite.addTestSuite(NumberFormat_ar_Test.class);
    suite.addTestSuite(NumberFormat_en_Test.class);
    suite.addTestSuite(NumberFormat_fr_Test.class);
    suite.addTestSuite(NumberParse_en_Test.class);
    suite.addTestSuite(NumberParse_fr_Test.class);
    suite.addTestSuite(RegionInheritanceTest.class);
    suite.addTestSuite(RuntimeLocalesTest.class);
    suite.addTestSuite(TimeZoneInfoTest.class);
    suite.addTestSuite(TimeZoneTest.class);
    // $JUnit-END$

    return suite;
  }
}
