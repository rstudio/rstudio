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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.TypeOracleTestingUtils;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.shell.FailErrorLogger;
import com.google.gwt.i18n.server.GwtLocaleFactoryImpl;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import junit.framework.TestCase;

import java.util.Map;

/**
 * Check that locale aliases are handled properly for Localizable and related
 * processing.
 */
public class LocalizableLinkageCreatorTest extends TestCase {

  private static final MockJavaResource LOCALIZABLE = new MockJavaResource(
      "com.google.gwt.i18n.client.Localizable") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package com.google.gwt.i18n.client;\n");
      code.append("public interface Localizable { }\n");
      return code;
    }
  };

  private static final MockJavaResource TEST = new MockJavaResource(
      "foo.Test") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("import com.google.gwt.i18n.client.Localizable;\n");
      code.append("public class Test implements Localizable { }\n");
      return code;
    }
  };

  private static final MockJavaResource TEST_HE = new MockJavaResource(
      "foo.Test_he") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("public class Test_he extends Test { }\n");
      return code;
    }
  };

  private static final MockJavaResource TEST_IW = new MockJavaResource(
      "foo.Test_iw") {
    @Override
    public CharSequence getContent() {
      StringBuffer code = new StringBuffer();
      code.append("package foo;\n");
      code.append("public class Test_iw extends Test { }\n");
      return code;
    }
  };

  public void testFindDerivedClasses() throws UnableToCompleteException {
    TreeLogger logger = new FailErrorLogger();
    TypeOracle oracle = TypeOracleTestingUtils.buildStandardTypeOracleWith(
        logger, LOCALIZABLE, TEST, TEST_IW, TEST_HE);
    JClassType clazz = oracle.findType("foo.Test");
    Map<String, JClassType> derivedClasses = LocalizableLinkageCreator.findDerivedClasses(logger, clazz);
    assertEquals(3, derivedClasses.size());
    assertEquals("foo.Test",
        derivedClasses.get("default").getQualifiedSourceName());
    assertEquals("foo.Test_iw",
        derivedClasses.get("iw").getQualifiedSourceName());
    assertEquals("foo.Test_he",
        derivedClasses.get("he").getQualifiedSourceName());
  }

  public void testLinkWithImplClass() throws UnableToCompleteException {
    LocalizableLinkageCreator llc = new LocalizableLinkageCreator();
    TreeLogger logger = new FailErrorLogger();
    TypeOracle oracle = TypeOracleTestingUtils.buildStandardTypeOracleWith(
        logger, LOCALIZABLE, TEST, TEST_IW);
    JClassType test = oracle.findType("foo.Test");
    GwtLocaleFactory factory = new GwtLocaleFactoryImpl();
    GwtLocale locale = factory.fromString("he");
    String implClass = llc.linkWithImplClass(logger, test, locale);
    assertEquals("foo.Test_iw", implClass);
    oracle = TypeOracleTestingUtils.buildStandardTypeOracleWith(
        logger, LOCALIZABLE, TEST, TEST_HE);
    test = oracle.findType("foo.Test");
    locale = factory.fromString("iw");
    implClass = llc.linkWithImplClass(logger, test, locale);
    assertEquals("foo.Test_he", implClass);
  }
}
