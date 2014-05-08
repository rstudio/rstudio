/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.cfg.DepsInfoProvider;
import com.google.gwt.dev.util.UnitTestTreeLogger;
import com.google.gwt.thirdparty.guava.common.base.CharMatcher;
import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.SetMultimap;
import com.google.gwt.thirdparty.guava.common.io.Files;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * Verifies log and file output of TypeRefDepsChecker.
 */
public class TypeRefDepsCheckerTest extends TestCase {

  private static class MockDepsInfoProvider implements DepsInfoProvider {

    private Map<String, String> gwtXmlFilePathByModuleName = Maps.newHashMap();
    private SetMultimap<String, String> sourceModuleNamesByTypeSourceName =
        LinkedHashMultimap.create();
    private SetMultimap<String, String> transitiveDepModuleNamesByModuleName =
        LinkedHashMultimap.create();

    public void addGwtXmlFile(String moduleName, String gwtXmlFilePath) {
      gwtXmlFilePathByModuleName.put(moduleName, gwtXmlFilePath);
    }

    public void addModuleDep(String fromModuleName, String toModuleName) {
      transitiveDepModuleNamesByModuleName.put(fromModuleName, toModuleName);
    }

    @Override
    public String getGwtXmlFilePath(String moduleName) {
      return gwtXmlFilePathByModuleName.get(moduleName);
    }

    @Override
    public Set<String> getSourceModuleNames(String typeSourceName) {
      return sourceModuleNamesByTypeSourceName.get(typeSourceName);
    }

    @Override
    public Set<String> getTransitiveDepModuleNames(String targetModuleName) {
      return transitiveDepModuleNamesByModuleName.get(targetModuleName);
    }

    public void registerType(String typeSourceName, String sourceModuleName) {
      sourceModuleNamesByTypeSourceName.put(typeSourceName, sourceModuleName);
    }
  }

  public void testMultipleFromMultipleTo() throws IOException {
    // Setup environment.
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.expectWarn("Type 'FromType' wants to reference type 'ToType' but can't because module "
        + "'FromModule1' has no dependency (neither direct nor transitive) on "
        + "'ToModule1|ToModule2|ToModule3'.", null);
    builder.expectWarn("Type 'FromType' wants to reference type 'ToType' but can't because module "
        + "'FromModule3' has no dependency (neither direct nor transitive) on "
        + "'ToModule1|ToModule2|ToModule3'.", null);
    UnitTestTreeLogger logger = builder.createLogger();
    File missingDepsFile = java.nio.file.Files.createTempFile("missingDepsFile", "txt").toFile();
    MockDepsInfoProvider depsInfo = new MockDepsInfoProvider();
    depsInfo.addModuleDep("FooModule", "FromModule1");
    depsInfo.addModuleDep("FooModule", "FromModule2");
    depsInfo.addModuleDep("FooModule", "FromModule3");
    depsInfo.addModuleDep("FooModule", "ToModule1");
    depsInfo.addModuleDep("FooModule", "ToModule2");
    depsInfo.addModuleDep("FooModule", "ToModule3");
    // Notice that FromModule2 can see just ToModule2 while the rest are completely blind.
    depsInfo.addModuleDep("FromModule2", "ToModule2");
    depsInfo.addGwtXmlFile("FromModule1", "com/google/gwt/FromModule1.gwt.xml");
    depsInfo.addGwtXmlFile("FromModule2", "com/google/gwt/FromModule2.gwt.xml");
    depsInfo.addGwtXmlFile("FromModule3", "com/google/gwt/FromModule3.gwt.xml");
    depsInfo.addGwtXmlFile("ToModule1", "com/google/gwt/ToModule1.gwt.xml");
    depsInfo.addGwtXmlFile("ToModule2", "com/google/gwt/ToModule2.gwt.xml");
    depsInfo.addGwtXmlFile("ToModule3", "com/google/gwt/ToModule3.gwt.xml");
    depsInfo.registerType("FromType", "FromModule1");
    depsInfo.registerType("FromType", "FromModule2");
    depsInfo.registerType("FromType", "FromModule3");
    depsInfo.registerType("ToType", "ToModule1");
    depsInfo.registerType("ToType", "ToModule2");
    depsInfo.registerType("ToType", "ToModule3");
    TypeRefDepsChecker typeRefDepsChecker =
        new TypeRefDepsChecker(logger, null, depsInfo, true, missingDepsFile);
    typeRefDepsChecker.maybeRecordTypeRef("FromType", "ToType");

    // Run typeRefDepsChecker.
    typeRefDepsChecker.verifyTypeRefsInModules();

    // Verify results.
    logger.assertCorrectLogEntries();
    assertEquals(
         // Line 1.
        "FromModule1\tcom/google/gwt/FromModule1.gwt.xml\t"
        + "ToModule1|ToModule2|ToModule3\tcom/google/gwt/ToModule1.gwt.xml|"
        + "com/google/gwt/ToModule2.gwt.xml|com/google/gwt/ToModule3.gwt.xml\t"
        + "Type 'FromType' wants to reference type 'ToType'.\n"
        // Line 2.
        + "FromModule3\tcom/google/gwt/FromModule3.gwt.xml\t"
        + "ToModule1|ToModule2|ToModule3\tcom/google/gwt/ToModule1.gwt.xml|"
        + "com/google/gwt/ToModule2.gwt.xml|com/google/gwt/ToModule3.gwt.xml\t"
        + "Type 'FromType' wants to reference type 'ToType'.",
        CharMatcher.WHITESPACE.trimFrom(Files.toString(missingDepsFile, StandardCharsets.UTF_8)));
  }

  public void testOptionsDisabled() {
    // Setup environment.
    UnitTestTreeLogger logger = new UnitTestTreeLogger.Builder().createLogger();
    MockDepsInfoProvider depsInfo = new MockDepsInfoProvider();
    // Notice that BarModule can not see BazModule.
    depsInfo.addModuleDep("FooModule", "BarModule");
    depsInfo.addModuleDep("FooModule", "BazModule");
    depsInfo.addGwtXmlFile("FooModule", "com/google/gwt/FooModule.gwt.xml");
    depsInfo.addGwtXmlFile("BarModule", "com/google/gwt/BarModule.gwt.xml");
    depsInfo.addGwtXmlFile("BazModule", "com/google/gwt/BazModule.gwt.xml");
    depsInfo.registerType("BangType", "BarModule");
    depsInfo.registerType("ZingType", "BazModule");
    TypeRefDepsChecker typeRefDepsChecker =
        new TypeRefDepsChecker(logger, null, depsInfo, false, null);
    // Make a type from BarModule depend on a type from BazModule.
    typeRefDepsChecker.maybeRecordTypeRef("BangType", "ZingType");

    // Run typeRefDepsChecker.
    typeRefDepsChecker.verifyTypeRefsInModules();

    // Verify results.
    logger.assertCorrectLogEntries();
  }

  public void testSimple() throws IOException {
    // Setup environment.
    UnitTestTreeLogger.Builder builder = new UnitTestTreeLogger.Builder();
    builder.expectWarn(
        "Type 'BangType' wants to reference type 'ZingType' but can't because module "
        + "'BarModule' has no dependency (neither direct nor transitive) on 'BazModule'.", null);
    UnitTestTreeLogger logger = builder.createLogger();
    File missingDepsFile = java.nio.file.Files.createTempFile("missingDepsFile", "txt").toFile();
    MockDepsInfoProvider depsInfo = new MockDepsInfoProvider();
    // Notice that BarModule can not see BazModule.
    depsInfo.addModuleDep("FooModule", "BarModule");
    depsInfo.addModuleDep("FooModule", "BazModule");
    depsInfo.addGwtXmlFile("FooModule", "com/google/gwt/FooModule.gwt.xml");
    depsInfo.addGwtXmlFile("BarModule", "com/google/gwt/BarModule.gwt.xml");
    depsInfo.addGwtXmlFile("BazModule", "com/google/gwt/BazModule.gwt.xml");
    depsInfo.registerType("BangType", "BarModule");
    depsInfo.registerType("ZingType", "BazModule");
    TypeRefDepsChecker typeRefDepsChecker =
        new TypeRefDepsChecker(logger, null, depsInfo, true, missingDepsFile);
    // Make a type from BarModule depend on a type from BazModule.
    typeRefDepsChecker.maybeRecordTypeRef("BangType", "ZingType");

    // Run typeRefDepsChecker.
    typeRefDepsChecker.verifyTypeRefsInModules();

    // Verify results.
    logger.assertCorrectLogEntries();
    assertEquals(
        "BarModule\tcom/google/gwt/BarModule.gwt.xml\tBazModule\tcom/google/gwt/BazModule.gwt.xml"
        + "\tType 'BangType' wants to reference type 'ZingType'.",
        CharMatcher.WHITESPACE.trimFrom(Files.toString(missingDepsFile, StandardCharsets.UTF_8)));
  }
}
