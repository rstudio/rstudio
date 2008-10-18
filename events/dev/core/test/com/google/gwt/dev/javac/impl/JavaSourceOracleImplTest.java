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
package com.google.gwt.dev.javac.impl;

import com.google.gwt.dev.javac.JavaSourceFile;
import com.google.gwt.dev.resource.Resource;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Tests {@link JavaSourceOracleImpl} using a mock {@link ResourceOracle}.
 */
public class JavaSourceOracleImplTest extends TestCase {

  private MockResourceOracle resourceOracle = new MockResourceOracle(
      JavaResourceBase.getStandardResources());

  private JavaSourceOracleImpl sourceOracle = new JavaSourceOracleImpl(
      resourceOracle);

  public void testAdd() {
    validateSourceOracle();

    Map<String, JavaSourceFile> originalSourceMap = sourceOracle.getSourceMap();
    resourceOracle.add(JavaResourceBase.FOO);
    Map<String, JavaSourceFile> newSourceMap = sourceOracle.getSourceMap();
    assertNotSame(originalSourceMap, newSourceMap);
    assertEquals(originalSourceMap.size() + 1, newSourceMap.size());
    validateSourceOracle();
  }

  public void testBasic() {
    validateSourceOracle();
  }

  public void testEmpty() {
    resourceOracle = new MockResourceOracle();
    sourceOracle = new JavaSourceOracleImpl(resourceOracle);
    validateSourceOracle();
  }

  public void testRemove() {
    validateSourceOracle();

    Map<String, JavaSourceFile> originalSourceMap = sourceOracle.getSourceMap();
    resourceOracle.remove(JavaResourceBase.OBJECT.getPath());
    Map<String, JavaSourceFile> newSourceMap = sourceOracle.getSourceMap();
    assertNotSame(originalSourceMap, newSourceMap);
    assertEquals(originalSourceMap.size() - 1, newSourceMap.size());
    validateSourceOracle();
  }

  public void testReplace() {
    validateSourceOracle();

    Map<String, JavaSourceFile> originalSourceMap = sourceOracle.getSourceMap();
    resourceOracle.replace(new MockResource(JavaResourceBase.OBJECT.getPath()) {
      @Override
      protected CharSequence getContent() {
        return JavaResourceBase.OBJECT.getContent();
      }
    });
    Map<String, JavaSourceFile> newSourceMap = sourceOracle.getSourceMap();
    assertNotSame(originalSourceMap, newSourceMap);
    assertEquals(originalSourceMap.size(), newSourceMap.size());
    assertFalse(originalSourceMap.equals(newSourceMap));
    validateSourceOracle();
  }

  public void testReplaceWithSame() {
    validateSourceOracle();

    Map<String, JavaSourceFile> originalSourceMap = sourceOracle.getSourceMap();
    resourceOracle.replace(JavaResourceBase.OBJECT);
    Map<String, JavaSourceFile> newSourceMap = sourceOracle.getSourceMap();
    assertNotSame(originalSourceMap, newSourceMap);
    assertEquals(originalSourceMap.size(), newSourceMap.size());
    assertEquals(originalSourceMap, newSourceMap);
    validateSourceOracle();
  }

  /**
   * Validate that the source oracle accurately reflects the resource oracle.
   */
  private void validateSourceOracle() {
    // Save off the reflected collections.
    Map<String, JavaSourceFile> sourceMap = sourceOracle.getSourceMap();
    Set<String> classNames = sourceOracle.getClassNames();
    Set<JavaSourceFile> sourceFiles = sourceOracle.getSourceFiles();

    // Validate that the collections are consistent with each other.
    assertEquals(sourceMap.keySet(), classNames);
    assertEquals(new HashSet<JavaSourceFile>(sourceMap.values()), sourceFiles);

    // Save off a mutable copy of the resource map to compare with.
    Map<String, Resource> resourceMap = new HashMap<String, Resource>(
        resourceOracle.getResourceMap());
    assertEquals(resourceMap.size(), sourceMap.size());
    for (Entry<String, JavaSourceFile> entry : sourceMap.entrySet()) {
      // Validate source file internally consistent.
      String className = entry.getKey();
      JavaSourceFile sourceFile = entry.getValue();
      assertEquals(className, sourceFile.getTypeName());
      assertEquals(Shared.getPackageName(className),
          sourceFile.getPackageName());
      assertEquals(Shared.getShortName(className), sourceFile.getShortName());

      // Find the matching resource (and remove it from the resource map!)
      String expectedPath = Shared.toPath(className);
      assertTrue(resourceMap.containsKey(expectedPath));

      // Validate the source file matches the resource.
      Resource resource = resourceMap.remove(expectedPath);
      assertNotNull(resource);
      assertEquals(Shared.readContent(resource.openContents()),
          sourceFile.readSource());
    }
    // The resource map should be empty now.
    assertEquals(0, resourceMap.size());

    // Validate collection identity hasn't changed.
    assertSame(sourceMap, sourceOracle.getSourceMap());
    assertSame(sourceFiles, sourceOracle.getSourceFiles());
    assertSame(classNames, sourceOracle.getClassNames());
  }
}
