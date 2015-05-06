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
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import junit.framework.TestCase;

import java.util.Map;

/**
 * Tests for {@link MinimalRebuildCache}.
 */
public class MinimalRebuildCacheTest extends TestCase {

  private MinimalRebuildCache minimalRebuildCache;

  public void testComputeAndClearStale() {
    // These three compilation units exist.
    Map<String, Long> currentModifiedBySourcePath = new ImmutableMap.Builder<String, Long>().put(
        "Foo.java", 0L).put("Bar.java", 0L).put("Baz.java", 0L).build();
    minimalRebuildCache.recordDiskSourceResources(currentModifiedBySourcePath);

    // They each contain a type and nested type.
    StringAnalyzableTypeEnvironment typeEnvironment = minimalRebuildCache.getTypeEnvironment();
    minimalRebuildCache.recordNestedTypeName("Foo", "Foo");
    typeEnvironment.recordTypeEnclosesMethod("Foo", "Foo::$clinit()V");
    minimalRebuildCache.recordNestedTypeName("Foo", "Foo$Inner");
    typeEnvironment.recordTypeEnclosesMethod("Foo$Inner", "Foo$Inner::$clinit()V");
    minimalRebuildCache.recordNestedTypeName("Bar", "Bar");
    typeEnvironment.recordTypeEnclosesMethod("Bar", "Bar::$clinit()V");
    minimalRebuildCache.recordNestedTypeName("Bar", "Bar$Inner");
    typeEnvironment.recordTypeEnclosesMethod("Bar$Inner", "Bar$Inner::$clinit()V");
    minimalRebuildCache.recordNestedTypeName("Baz", "Baz");
    typeEnvironment.recordTypeEnclosesMethod("Baz", "Baz::$clinit()V");
    minimalRebuildCache.recordNestedTypeName("Baz", "Baz$Inner");
    typeEnvironment.recordTypeEnclosesMethod("Baz$Inner", "Baz$Inner::$clinit()V");

    // There's some JS for each type.
    minimalRebuildCache.setJsForType(TreeLogger.NULL, "Foo", "Some Js for Foo");
    minimalRebuildCache.setJsForType(TreeLogger.NULL, "Foo$Inner", "Some Js for Foo");
    minimalRebuildCache.setJsForType(TreeLogger.NULL, "Bar", "Some Js for Bar");
    minimalRebuildCache.setJsForType(TreeLogger.NULL, "Bar$Inner", "Some Js for Bar");
    minimalRebuildCache.setJsForType(TreeLogger.NULL, "Baz", "Some Js for Baz");
    minimalRebuildCache.setJsForType(TreeLogger.NULL, "Baz$Inner", "Some Js for Baz");

    // Record that Bar references Foo and Baz subclasses Foo.
    minimalRebuildCache.addTypeReference("Bar", "Foo");
    typeEnvironment.recordMethodCallsMethod("Bar::start()", "Foo::run()");
    typeEnvironment.recordStaticReferenceInMethod("Foo", "Bar::start()");
    typeEnvironment.recordTypeEnclosesMethod("Bar", "Bar::start()");
    typeEnvironment.recordTypeEnclosesMethod("Foo", "Foo::run()");
    minimalRebuildCache.getImmediateTypeRelations().getImmediateSuperclassesByClass().put("Baz",
        "Foo");
    typeEnvironment.recordMethodCallsMethod("Foo::run()", "Baz::run()");
    typeEnvironment.recordStaticReferenceInMethod("Baz", "Foo::run()");
    typeEnvironment.recordTypeEnclosesMethod("Baz", "Baz::run()");

    // Record that these types reference their inner classes.
    minimalRebuildCache.addTypeReference("Foo", "Foo$Inner");
    minimalRebuildCache.addTypeReference("Bar", "Bar$Inner");
    minimalRebuildCache.addTypeReference("Baz", "Baz$Inner");
    typeEnvironment.recordStaticReferenceInMethod("Bar$Inner", "Bar::start()");
    typeEnvironment.recordStaticReferenceInMethod("Foo$Inner", "Foo::run()");
    typeEnvironment.recordStaticReferenceInMethod("Baz$Inner", "Baz::run()");

    // In the next compile only Foo is modified.
    Map<String, Long> laterModifiedBySourcePath = new ImmutableMap.Builder<String, Long>().put(
        "Foo.java", 9999L).put("Bar.java", 0L).put("Baz.java", 0L).build();
    minimalRebuildCache.recordDiskSourceResources(laterModifiedBySourcePath);

    // Ensure the types are known to be reachable.
    minimalRebuildCache.setRootTypeNames(Sets.newHashSet("Foo", "Bar", "Baz"));
    minimalRebuildCache.setEntryMethodNames(Lists.newArrayList("Bar::start()"));
    minimalRebuildCache.computeReachableTypeNames();

    // Request clearing of cache related to stale types.
    minimalRebuildCache.computeAndClearStaleTypesCache(TreeLogger.NULL,
        new JTypeOracle(null, minimalRebuildCache));

    // Has the expected JS been cleared?
    assertNull(minimalRebuildCache.getJs("Foo"));
    assertNull(minimalRebuildCache.getJs("Foo$Inner"));
    assertNull(minimalRebuildCache.getJs("Bar"));
    assertNull(minimalRebuildCache.getJs("Baz"));
    assertNotNull(minimalRebuildCache.getJs("Bar$Inner"));
    assertNotNull(minimalRebuildCache.getJs("Baz$Inner"));
  }

  public void testComputeDeletedTypes() {
    // These three compilation units exist.
    Map<String, Long> currentModifiedBySourcePath = new ImmutableMap.Builder<String, Long>().put(
        "Foo.java", 0L).put("Bar.java", 0L).put("Baz.java", 0L).build();
    minimalRebuildCache.recordDiskSourceResources(currentModifiedBySourcePath);

    // They each contain a type and nested type.
    minimalRebuildCache.recordNestedTypeName("Foo", "Foo");
    minimalRebuildCache.recordNestedTypeName("Foo", "Foo$Inner");
    minimalRebuildCache.recordNestedTypeName("Bar", "Bar");
    minimalRebuildCache.recordNestedTypeName("Bar", "Bar$Inner");
    minimalRebuildCache.recordNestedTypeName("Baz", "Baz");
    minimalRebuildCache.recordNestedTypeName("Baz", "Baz$Inner");

    // In the next compile it turns out there are fewer compilation units, Baz is gone.
    Map<String, Long> laterModifiedBySourcePath =
        new ImmutableMap.Builder<String, Long>().put("Foo.java", 0L).put("Bar.java", 0L).build();
    minimalRebuildCache.recordDiskSourceResources(laterModifiedBySourcePath);

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
