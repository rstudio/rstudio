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

import com.google.gwt.i18n.rebind.LocaleUtilsTest;
import com.google.gwt.i18n.rebind.LocalizableGeneratorTest;
import com.google.gwt.i18n.server.GwtLocaleTest;
import com.google.gwt.i18n.server.MessageFormatParserTest;
import com.google.gwt.i18n.server.PropertyCatalogFactoryTest;
import com.google.gwt.i18n.server.RegionInheritanceTest;
import com.google.gwt.i18n.server.impl.ReflectionMessageInterfaceTest;
import com.google.gwt.i18n.shared.AnyRtlDirectionEstimatorTest;
import com.google.gwt.i18n.shared.BidiFormatterTest;
import com.google.gwt.i18n.shared.BidiUtilsTest;
import com.google.gwt.i18n.shared.FirstStrongDirectionEstimatorTest;
import com.google.gwt.i18n.shared.WordCountDirectionEstimatorTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * I18N tests running as a regular JRE test.
 */
public class I18NJreSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite("I18N shared/server/rebind tests");

    // $JUnit-BEGIN$
    suite.addTestSuite(AnyRtlDirectionEstimatorTest.class);
    suite.addTestSuite(BidiFormatterTest.class);
    suite.addTestSuite(BidiUtilsTest.class);
    suite.addTestSuite(ConstantMapTest.class);
    suite.addTestSuite(FirstStrongDirectionEstimatorTest.class);
    suite.addTestSuite(GwtLocaleTest.class);
    suite.addTestSuite(LocaleUtilsTest.class);
    suite.addTestSuite(LocalizableGeneratorTest.class);
    suite.addTestSuite(MessageFormatParserTest.class);
    suite.addTestSuite(com.google.gwt.i18n.rebind.MessageFormatParserTest.class);
    suite.addTestSuite(PropertyCatalogFactoryTest.class);
    suite.addTestSuite(ReflectionMessageInterfaceTest.class);
    suite.addTestSuite(RegionInheritanceTest.class);
    /*
     * TODO(jat): rewrite TypeOracleMessageTest to use mocks for all i18n
     * classes rather than loading the real ones, which causes problems getting
     * just what we want on the classpath. The test can be run manually from the
     * IDE with {dev,user}/{src,super} (no test) on the classpath.
     */
    // suite.addTestSuite(TypeOracleMessageTest.class);
    suite.addTestSuite(WordCountDirectionEstimatorTest.class);
    // $JUnit-END$

    return suite;
  }
}
