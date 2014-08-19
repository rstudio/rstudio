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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.MinimalRebuildCache.PermutationRebuildCache;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import junit.framework.TestCase;

/**
 * Tests for {@link MinimalRebuildCache}.
 */
public class MinimalRebuildCacheTest extends TestCase {

  private MinimalRebuildCache minimalRebuildCache;

  public void testComputeAndClearStale() {
    // These three compilation units exist.
    minimalRebuildCache.setAllCompilationUnitNames(TreeLogger.NULL,
        Sets.newHashSet("Foo", "Bar", "Baz"));

    // They each contain a type and nested type.
    minimalRebuildCache.recordNestedTypeName("Foo", "Foo");
    minimalRebuildCache.recordNestedTypeName("Foo", "Foo$Inner");
    minimalRebuildCache.recordNestedTypeName("Bar", "Bar");
    minimalRebuildCache.recordNestedTypeName("Bar", "Bar$Inner");
    minimalRebuildCache.recordNestedTypeName("Baz", "Baz");
    minimalRebuildCache.recordNestedTypeName("Baz", "Baz$Inner");

    // There's some JS for each type.
    PermutationRebuildCache permutationRebuildCache =
        minimalRebuildCache.getPermutationRebuildCache(0);
    permutationRebuildCache.setJsForType(TreeLogger.NULL, "Foo", "Some Js for Foo");
    permutationRebuildCache.setJsForType(TreeLogger.NULL, "Foo$Inner", "Some Js for Foo");
    permutationRebuildCache.setJsForType(TreeLogger.NULL, "Bar", "Some Js for Bar");
    permutationRebuildCache.setJsForType(TreeLogger.NULL, "Bar$Inner", "Some Js for Bar");
    permutationRebuildCache.setJsForType(TreeLogger.NULL, "Baz", "Some Js for Baz");
    permutationRebuildCache.setJsForType(TreeLogger.NULL, "Baz$Inner", "Some Js for Baz");

    // Record that Bar references Foo and Baz subclasses Foo.
    permutationRebuildCache.addTypeReference("Bar", "Foo");
    minimalRebuildCache.getImmediateTypeRelations()
        .getImmediateSuperclassesByClass().put("Baz", "Foo");

    // In the next compile Foo is modified.
    minimalRebuildCache.setModifiedCompilationUnitNames(TreeLogger.NULL, Sets.newHashSet("Foo"));

    // Request clearing of cache related to stale types.
    minimalRebuildCache.clearStaleTypeJsAndStatements(TreeLogger.NULL,
        new JTypeOracle(null, minimalRebuildCache, true));

    // Has the expected JS been cleared?
    assertNull(permutationRebuildCache.getJs("Foo"));
    assertNull(permutationRebuildCache.getJs("Foo$Inner"));
    assertNull(permutationRebuildCache.getJs("Bar"));
    assertNull(permutationRebuildCache.getJs("Baz"));
    assertNotNull(permutationRebuildCache.getJs("Bar$Inner"));
    assertNotNull(permutationRebuildCache.getJs("Baz$Inner"));
  }

  public void testComputeDeletedTypes() {
    // These three compilation units exist.
    minimalRebuildCache.setAllCompilationUnitNames(TreeLogger.NULL,
        Sets.newHashSet("Foo", "Bar", "Baz"));

    // They each contain a type and nested type.
    minimalRebuildCache.recordNestedTypeName("Foo", "Foo");
    minimalRebuildCache.recordNestedTypeName("Foo", "Foo$Inner");
    minimalRebuildCache.recordNestedTypeName("Bar", "Bar");
    minimalRebuildCache.recordNestedTypeName("Bar", "Bar$Inner");
    minimalRebuildCache.recordNestedTypeName("Baz", "Baz");
    minimalRebuildCache.recordNestedTypeName("Baz", "Baz$Inner");

    // In the next compile it turns out there are fewer compilation units, Baz is gone.
    minimalRebuildCache.setAllCompilationUnitNames(TreeLogger.NULL, Sets.newHashSet("Foo", "Bar"));

    // Is the correct deleted type set calculated?
    assertEquals(Sets.newHashSet("Baz", "Baz$Inner"),
        minimalRebuildCache.computeDeletedTypeNames());
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    minimalRebuildCache = new MinimalRebuildCache();
  }
}
