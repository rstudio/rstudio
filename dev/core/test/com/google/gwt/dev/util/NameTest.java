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
package com.google.gwt.dev.util;

import com.google.gwt.dev.util.Name.BinaryName;
import com.google.gwt.dev.util.Name.SourceOrBinaryName;
import com.google.gwt.dev.util.Name.InternalName;
import com.google.gwt.dev.util.Name.SourceName;

import junit.framework.TestCase;

/**
 * Tests for {@link Name}.
 */
public class NameTest extends TestCase {

  /**
   * Used to test getting names from a Class instance.
   */
  private static class Inner {   
  }

  public void testBinaryName() {
    assertEquals("org.test.Foo", BinaryName.toSourceName("org.test.Foo"));
    assertEquals("org.test.Foo.Bar",
        BinaryName.toSourceName("org.test.Foo$Bar"));
    assertEquals("org.test.Foo.Bar.Baz",
        BinaryName.toSourceName("org.test.Foo$Bar$Baz"));
    assertEquals("org.test.Foo.Bar.Baz$",
        BinaryName.toSourceName("org.test.Foo$Bar$Baz$"));
    assertEquals("org/test/Foo", BinaryName.toInternalName("org.test.Foo"));
    assertEquals("org/test/Foo$Bar",
        BinaryName.toInternalName("org.test.Foo$Bar"));
    assertEquals("org/test/Foo$Bar$Baz",
        BinaryName.toInternalName("org.test.Foo$Bar$Baz"));
    assertEquals("org/test/Foo$Bar$Baz$",
        BinaryName.toInternalName("org.test.Foo$Bar$Baz$"));
    assertEquals("org/test/Foo$Bar$Baz$1",
        BinaryName.toInternalName("org.test.Foo$Bar$Baz$1"));
    assertEquals("org.test.Foo$Bar",
        BinaryName.getInnerClassName("org.test.Foo", "Bar"));
    assertEquals("org.test.Foo",
        BinaryName.getOuterClassName("org.test.Foo$Bar"));
    assertEquals("org.test",
        BinaryName.getPackageName("org.test.Foo$Bar"));
    assertEquals("Foo$Bar", BinaryName.getClassName("org.test.Foo$Bar"));
    assertEquals("Bar", BinaryName.getShortClassName("org.test.Foo$Bar"));
  }

  public void testSourceOrBinaryName() {
    assertEquals("org.test.Foo.Bar",
        SourceOrBinaryName.toSourceName("org.test.Foo.Bar"));
    assertEquals("org.test.Foo.Bar",
        SourceOrBinaryName.toSourceName("org.test.Foo$Bar"));
    assertEquals("org.test.Foo.Bar$",
        SourceOrBinaryName.toSourceName("org.test.Foo.Bar$"));
    assertEquals("org.test.Foo.Bar$",
        SourceOrBinaryName.toSourceName("org.test.Foo$Bar$"));
  }

  public void testGetBinaryNameForClass() {
    assertEquals("com.google.gwt.dev.util.NameTest$Inner",
        Name.getBinaryNameForClass(Inner.class));
  }

  public void testGetInternalNameForClass() {
    assertEquals("com/google/gwt/dev/util/NameTest$Inner",
        Name.getInternalNameForClass(Inner.class));
  }

  public void testGetSourceNameForClass() {
    assertEquals("com.google.gwt.dev.util.NameTest.Inner",
        Name.getSourceNameForClass(Inner.class));
  }

  public void testInternalName() {
    assertEquals("org.test.Foo", InternalName.toSourceName("org/test/Foo"));
    assertEquals("org.test.Foo.Bar",
        InternalName.toSourceName("org/test/Foo$Bar"));
    assertEquals("org.test.Foo.Bar.Baz",
        InternalName.toSourceName("org/test/Foo$Bar$Baz"));
    assertEquals("org.test.Foo.Bar.Baz$",
        InternalName.toSourceName("org/test/Foo$Bar$Baz$"));
    assertEquals("org.test.Foo", InternalName.toBinaryName("org/test/Foo"));
    assertEquals("org.test.Foo$Bar",
        InternalName.toBinaryName("org/test/Foo$Bar"));
    assertEquals("org.test.Foo$Bar$Baz",
        InternalName.toBinaryName("org/test/Foo$Bar$Baz"));
    assertEquals("org.test.Foo$Bar$Baz$",
        InternalName.toBinaryName("org/test/Foo$Bar$Baz$"));
    assertEquals("org.test.Foo$Bar$Baz$1",
        InternalName.toBinaryName("org/test/Foo$Bar$Baz$1"));
    assertEquals("org/test/Foo$Bar",
        InternalName.getInnerClassName("org/test/Foo", "Bar"));
    assertEquals("org/test/Foo",
        InternalName.getOuterClassName("org/test/Foo$Bar"));
    assertEquals("org/test",
        InternalName.getPackageName("org/test/Foo$Bar"));
    assertEquals("Foo$Bar", InternalName.getClassName("org/test/Foo$Bar"));
    assertEquals("Bar", InternalName.getShortClassName("org/test/Foo$Bar"));
  }

  public void testIsBinaryName() {
    assertTrue(Name.isBinaryName("org.test.Foo"));
    assertTrue(Name.isBinaryName("org.test.Foo$Bar"));
    assertTrue(Name.isBinaryName("org.test.Foo$Bar$Baz"));
    assertTrue(Name.isBinaryName("org.test.Foo$Bar$Baz$"));
    assertTrue(Name.isBinaryName("org.test.Foo$Bar$Baz$1"));
    assertFalse(Name.isBinaryName("org/test/Foo"));
    assertFalse(Name.isBinaryName("org/test/Foo$Bar"));
    assertFalse(Name.isBinaryName("org/test/Foo$Bar$Baz"));
    assertFalse(Name.isBinaryName("org/test/Foo$Bar$Baz$"));
    assertFalse(Name.isBinaryName("org/test/Foo$Bar$Baz$1"));
    assertTrue(Name.isBinaryName("org.test.Foo.Bar"));
    // We can't tell these aren't binary names without being able to tell
    // what the name of the top-level class is, but don't want to encode
    // bad behavior in the test.
    // assertTrue(Name.isBinaryName("org.test.Foo.Bar.Baz"));
    // assertTrue(Name.isBinaryName("org.test.Foo.Bar.Baz$"));
  }

  public void testIsInternalName() {
    assertFalse(Name.isInternalName("org.test.Foo"));
    assertFalse(Name.isInternalName("org.test.Foo$Bar"));
    assertFalse(Name.isInternalName("org.test.Foo$Bar$Baz"));
    assertFalse(Name.isInternalName("org.test.Foo$Bar$Baz$"));
    assertFalse(Name.isInternalName("org.test.Foo$Bar$Baz$1"));
    assertTrue(Name.isInternalName("org/test/Foo"));
    assertTrue(Name.isInternalName("org/test/Foo$Bar"));
    assertTrue(Name.isInternalName("org/test/Foo$Bar$Baz"));
    assertTrue(Name.isInternalName("org/test/Foo$Bar$Baz$"));
    assertTrue(Name.isInternalName("org/test/Foo$Bar$Baz$1"));
    assertFalse(Name.isInternalName("org.test.Foo.Bar"));
    assertFalse(Name.isInternalName("org.test.Foo.Bar.Baz"));
    assertFalse(Name.isInternalName("org.test.Foo.Bar.Baz$"));
  }

  public void testIsSourceName() {
    assertTrue(Name.isSourceName("org.test.Foo"));
    assertFalse(Name.isSourceName("org.test.Foo$Bar"));
    assertFalse(Name.isSourceName("org.test.Foo$Bar$Baz"));
    assertFalse(Name.isSourceName("org.test.Foo$Bar$Baz$"));
    assertFalse(Name.isSourceName("org.test.Foo$Bar$Baz$1"));
    assertFalse(Name.isSourceName("org/test/Foo"));
    assertFalse(Name.isSourceName("org/test/Foo$Bar"));
    assertFalse(Name.isSourceName("org/test/Foo$Bar$Baz"));
    assertFalse(Name.isSourceName("org/test/Foo$Bar$Baz$"));
    assertFalse(Name.isSourceName("org/test/Foo$Bar$Baz$1"));
    assertTrue(Name.isSourceName("org.test.Foo.Bar"));
    assertTrue(Name.isSourceName("org.test.Foo.Bar.Baz"));
    assertTrue(Name.isSourceName("org.test.Foo.Bar.Baz$"));
  }

  public void testIsSourceOrBinaryName() {
    assertTrue(Name.isSourceOrBinaryName("org.test.Foo"));
    assertTrue(Name.isSourceOrBinaryName("org.test.Foo$Bar"));
    assertTrue(Name.isSourceOrBinaryName("org.test.Foo$Bar$Baz"));
    assertTrue(Name.isSourceOrBinaryName("org.test.Foo$Bar$Baz$"));
    assertTrue(Name.isSourceOrBinaryName("org.test.Foo$Bar$Baz$1"));
    assertFalse(Name.isSourceOrBinaryName("org/test/Foo"));
    assertFalse(Name.isSourceOrBinaryName("org/test/Foo$Bar"));
    assertFalse(Name.isSourceOrBinaryName("org/test/Foo$Bar$Baz"));
    assertFalse(Name.isSourceOrBinaryName("org/test/Foo$Bar$Baz$"));
    assertFalse(Name.isSourceOrBinaryName("org/test/Foo$Bar$Baz$1"));
    assertTrue(Name.isSourceOrBinaryName("org.test.Foo.Bar"));
    assertTrue(Name.isSourceOrBinaryName("org.test.Foo.Bar.Baz"));
    assertTrue(Name.isSourceOrBinaryName("org.test.Foo.Bar.Baz$"));
  }

  public void testSourceName() {
    assertEquals("org.test.Foo.Bar",
        SourceName.getInnerClassName("org.test.Foo", "Bar"));
    assertEquals("Bar", SourceName.getShortClassName("org.test.Foo.Bar"));
  }
}
