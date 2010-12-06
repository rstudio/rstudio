/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.javac.asm;

import com.google.gwt.dev.javac.typemodel.test.TestAnnotation;
import com.google.gwt.dev.asm.ClassReader;
import com.google.gwt.dev.util.Name.BinaryName;

import java.util.Map;
import java.util.Set;

/**
 * Tests for {@link CollectClassData}.
 */
public class CollectReferencesVisitorTest extends AsmTestCase {

  /**
   * This class is empty, but it still has references to itself, its superclass
   * (Object), and its enclosing class.
   */
  public static class Empty {
  }

  /**
   * In addition to the visible types, this class has references to itself, its
   * superclass (Object), and its enclosing class.
   */
  public static class Full {

    protected Integer i;
    protected String s;

    @TestAnnotation(value = "foo", classLiteral = Double.class)
    public Map<Boolean, String> getMap() {
      return null;
    }
  }

  public void testEmpty() {
    CollectReferencesVisitor rv = collect(Empty.class);
    Set<String> referencedTypes = rv.getReferencedTypes();
    assertDoesNotContainNull(referencedTypes);
    assertEquals(3, referencedTypes.size());
    assertContainsInternalName(Object.class, referencedTypes);
    assertContainsInternalName(CollectReferencesVisitorTest.class,
        referencedTypes);
    assertContainsInternalName(Empty.class, referencedTypes);
  }

  public void testFull() {
    CollectReferencesVisitor rv = collect(Full.class);
    Set<String> referencedTypes = rv.getReferencedTypes();
    assertDoesNotContainNull(referencedTypes);
    assertEquals(7, referencedTypes.size());
    assertContainsInternalName(Object.class, referencedTypes);
    assertContainsInternalName(CollectReferencesVisitorTest.class,
        referencedTypes);
    assertContainsInternalName(Full.class, referencedTypes);
    assertContainsInternalName(Map.class, referencedTypes);
    assertContainsInternalName(Integer.class, referencedTypes);
    assertContainsInternalName(String.class, referencedTypes);
    assertContainsInternalName(Boolean.class, referencedTypes);
    // We no longer collect references from annotations to allow for
    // binary-only annotations and nontranslatable things like File mentioned
    // in the annotation
    // assertContainsInternalName(Double.class, referencedTypes);
    // assertContainsInternalName(TestAnnotation.class, referencedTypes);
  }

  // ASM passes null for Object's superclass, so we make sure we don't
  // insert null into the set of referenced types.
  public void testObject() {
    CollectReferencesVisitor rv = collect(Object.class);
    Set<String> referencedTypes = rv.getReferencedTypes();
    assertDoesNotContainNull(referencedTypes);
  }

  private void assertContainsInternalName(Class<?> clazz, Set<String> set) {
    String className = BinaryName.toInternalName(clazz.getName());
    assertTrue("Should contain " + className, set.contains(className));
  }

  private void assertDoesNotContainNull(Set<String> referencedTypes) {
    assertFalse(referencedTypes.contains(null));
  }

  private CollectReferencesVisitor collect(Class<?> clazz) {
    return collect(clazz.getName());
  }

  private CollectReferencesVisitor collect(String className) {
    byte[] bytes = getClassBytes(className);
    assertNotNull("Couldn't load bytes for " + className, bytes);
    CollectReferencesVisitor cv = new CollectReferencesVisitor();
    ClassReader reader = new ClassReader(bytes);
    reader.accept(cv, 0);
    return cv;
  }
}
