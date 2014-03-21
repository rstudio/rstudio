/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.cfg;

import com.google.gwt.dev.cfg.Libraries.IncompatibleLibraryVersionException;
import com.google.gwt.dev.javac.CompilationStateTestBase;
import com.google.gwt.dev.javac.CompilationUnit;
import com.google.gwt.dev.javac.JdtCompilerTest;
import com.google.gwt.dev.javac.testing.impl.MockJavaResource;
import com.google.gwt.dev.javac.testing.impl.MockResource;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.util.Util;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Tests for ZipLibrary and ZipLibraryWriter.
 */
public class ZipLibrariesTest extends CompilationStateTestBase {

  public static final MockJavaResource BAR = new MockJavaResource("test.Bar") {
      @Override
    public CharSequence getContent() {
      StringBuilder sb = new StringBuilder();
      sb.append("package test;");
      sb.append("public class Bar extends Foo {");
      sb.append("  public String value() { return \"Bar\"; }");
      sb.append("}");
      return sb;
    }
  };

  public static final MockJavaResource SUPER_FOO = new MockJavaResource("test.Foo") {
      @Override
    public CharSequence getContent() {
      StringBuilder sb = new StringBuilder();
      sb.append("package test;");
      sb.append("public class Foo {");
      sb.append("}");
      return sb;
    }

      @Override
    public String getLocation() {
      return "/super/" + path;
    }

      @Override
    public boolean wasRerooted() {
      return true;
    }
  };

  private static class SimpleMockResource extends MockResource {

    public SimpleMockResource(String path) {
      super(path);
    }

    @Override
    public CharSequence getContent() {
      return "";
    }
  }

  public void testRoundTrip() throws IOException, IncompatibleLibraryVersionException {
    File zipFile = File.createTempFile("Test", ".gwtlib");
    zipFile.deleteOnExit();

    // Data
    String expectedLibraryName = "BazLib";
    final String expectedResourceContents =
        "<html><head><title>Index</title></head><body>Hi</body></html>";
    Set<String> expectedRanGeneratorNames =
        Sets.newHashSet("UiBinderGenerator", "PlatinumGenerator");
    Set<String> expectedUserAgentConfigurationValues = Sets.newHashSet("webkit");
    Set<String> expectedLocaleConfigurationValues = Sets.newHashSet("en,default,en_US", "fr");
    Set<String> expectedDependencyLibraryNames = Sets.newHashSet("FooLib", "BarLib");
    oracle.add(BAR, SUPER_FOO, JdtCompilerTest.OUTER_INNER);
    rebuildCompilationState();
    List<CompilationUnit> compilationUnits =
        Lists.newArrayList(state.getCompilationUnitMap().values());

    // Put data in the library and save it.
    ZipLibraryWriter zipLibraryWriter = new ZipLibraryWriter(zipFile.getPath());
    zipLibraryWriter.setLibraryName(expectedLibraryName);
    // Include unusual path characters.
    zipLibraryWriter.addPublicResource(new SimpleMockResource("ui:binder:com.foo.baz.TableView"));
    // Include specific expected contents.
    zipLibraryWriter.addPublicResource(new MockResource("index.html") {
        @Override
      public CharSequence getContent() {
        return expectedResourceContents;
      }
    });
    zipLibraryWriter.addNewConfigurationPropertyValuesByName("user.agent",
        expectedUserAgentConfigurationValues);
    zipLibraryWriter.addNewConfigurationPropertyValuesByName("locale",
        expectedLocaleConfigurationValues);
    for (String generatorName : expectedRanGeneratorNames) {
      zipLibraryWriter.addRanGeneratorName(generatorName);
    }
    zipLibraryWriter.addDependencyLibraryNames(expectedDependencyLibraryNames);
    for (CompilationUnit compilationUnit : compilationUnits) {
      zipLibraryWriter.addCompilationUnit(compilationUnit);
    }
    zipLibraryWriter.write();

    // Read data back from disk.
    ZipLibrary zipLibrary = new ZipLibrary(zipFile.getPath());
    CompilationUnit barCompilationUnit =
        zipLibrary.getCompilationUnitByTypeSourceName(BAR.getTypeName());
    CompilationUnit superFooCompilationUnit =
        zipLibrary.getCompilationUnitByTypeSourceName(SUPER_FOO.getTypeName());

    // Compare it.
    assertEquals(expectedLibraryName, zipLibrary.getLibraryName());
    assertEquals(expectedResourceContents,
        Util.readStreamAsString(zipLibrary.getPublicResourceByPath("index.html").openContents()));
    assertEquals(expectedRanGeneratorNames, zipLibrary.getRanGeneratorNames());
    assertEquals(expectedUserAgentConfigurationValues,
        zipLibrary.getNewConfigurationPropertyValuesByName().get("user.agent"));
    assertEquals(expectedLocaleConfigurationValues,
        zipLibrary.getNewConfigurationPropertyValuesByName().get("locale"));
    assertEquals(expectedDependencyLibraryNames, zipLibrary.getDependencyLibraryNames());

    // CompilationUnit
    List<JDeclaredType> barTypes = barCompilationUnit.getTypes();
    assertEquals(1, barTypes.size());
    assertEquals(BAR.getTypeName(), barTypes.get(0).getName());
    assertEquals(BAR.getLocation(), barCompilationUnit.getResourceLocation());
    assertEquals(BAR.getTypeName(), barCompilationUnit.getTypeName());

    // SuperSourceCompilationUnit
    List<JDeclaredType> superFoo = superFooCompilationUnit.getTypes();
    assertEquals(1, superFoo.size());
    assertEquals(SUPER_FOO.getTypeName(), superFoo.get(0).getName());
    assertEquals(SUPER_FOO.getLocation(), superFooCompilationUnit.getResourceLocation());
    assertEquals(SUPER_FOO.getTypeName(), superFooCompilationUnit.getTypeName());

    // Can find inner classes by source name.
    assertTrue(zipLibrary.getNestedSourceNamesByCompilationUnitName().get(
        JdtCompilerTest.OUTER_INNER.getTypeName()).contains(
        JdtCompilerTest.OUTER_INNER.getTypeName() + ".Inner"));

    // Can find inner classes by binary name.
    assertTrue(zipLibrary.getNestedBinaryNamesByCompilationUnitName().get(
        JdtCompilerTest.OUTER_INNER.getTypeName()).contains(
        JdtCompilerTest.OUTER_INNER.getTypeName() + "$Inner"));
  }

  public void testVersionNumberException() throws IOException {
    File zipFile = File.createTempFile("Test", ".gwtlib");
    zipFile.deleteOnExit();

    // Put data in the library and save it.
    ZipLibraryWriter zipLibraryWriter = new ZipLibraryWriter(zipFile.getPath());
    zipLibraryWriter.setLibraryName("BazLib");
    zipLibraryWriter.write();

    // Change the expected version number so that this next read should fail.
    ZipLibraries.versionNumber++;

    // Read data back from disk.
    try {
      new ZipLibrary(zipFile.getPath());
      fail("Expected zip library initialization to fail with a version "
          + "mismatch, but it didn't fail.");
    } catch (IncompatibleLibraryVersionException e) {
      // Expected behavior.
    }
  }
}
