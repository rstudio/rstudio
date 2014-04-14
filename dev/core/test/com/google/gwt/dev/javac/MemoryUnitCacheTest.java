/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;

import junit.framework.TestCase;

/**
 * Unit test for {@link MemoryUnitCache}.
 */
public class MemoryUnitCacheTest extends TestCase {

  public void testMemoryCache() {
    TreeLogger logger = TreeLogger.NULL;
    MemoryUnitCache cache = new MemoryUnitCache();
    MockCompilationUnit foo1 = new MockCompilationUnit("com.example.Foo", "source1");
    cache.add(foo1);
    MockCompilationUnit bar1 = new MockCompilationUnit("com.example.Bar", "source2");
    cache.add(bar1);
    CompilationUnit result;

    // Find by content Id
    result = cache.find(foo1.getContentId());
    assertNotNull(result);
    assertEquals("com.example.Foo", result.getTypeName());
    result = cache.find(bar1.getContentId());
    assertNotNull(result);
    assertEquals("com.example.Bar", result.getTypeName());

    // Find by type name
    result = cache.find("com/example/Foo.java");
    assertNotNull(result);
    assertEquals("com.example.Foo", result.getTypeName());
    result = cache.find("com/example/Bar.java");
    assertNotNull(result);
    assertEquals("com.example.Bar", result.getTypeName());
    cache.cleanup(logger); // should be a no-op

    // Replace a type
    MockCompilationUnit foo2 = new MockCompilationUnit("com.example.Foo", "source3");
    cache.add(foo2);
    result = cache.find(foo1.getContentId());
    assertNull(result);
    result = cache.find(foo2.getContentId());
    assertNotNull(result);
    assertEquals("com.example.Foo", result.getTypeName());
    result = cache.find("com/example/Foo.java");
    assertNotNull(result);
    assertEquals("com.example.Foo", result.getTypeName());
    assertEquals(foo2.getContentId(), result.getContentId());

    // Remove a type
    cache.remove(bar1);
    result = cache.find(bar1.getContentId());
    assertNull(result);
    result = cache.find("com/example.Bar");
    assertNull(result);
  }
}
