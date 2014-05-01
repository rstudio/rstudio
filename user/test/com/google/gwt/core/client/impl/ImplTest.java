/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.core.client.impl;

import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests for {@link Impl}.
 */
public class ImplTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.core.Core";
  }

  // A function that is not referenced and can be pruned very early in the compilation pipeline.
  public static String prunableFunction() {
    return "Prunnable";
  }

  // A very simple function that will certainly be inlined away in optimized compiles.
  public static String inlinableFunction() {
    return "Inlinable";
  }

  private static final class Foo {
    // A very simple function that will certainly be made static but not inlined.
    // because it refers to its parameter twice.
    public final String statifiableFunction(String o) {
      return "Statifiable " + o + o;
    }
  }

  private static abstract class ClassWithAbstractMethod {
    public abstract void abstractMethod();
  }

  @DoNotRunWith(Platform.Devel)
  public void testPinnedByImpl_getNameOf() {
    Foo foo = new Foo();
    assertNotNull(foo.statifiableFunction(null));
    assertNotNull(inlinableFunction());
    assertNotNullNorEmpty(
        Impl.getNameOf("@com.google.gwt.core.client.impl.ImplTest::prunableFunction()"));
    assertNotNullNorEmpty(
        Impl.getNameOf("@com.google.gwt.core.client.impl.ImplTest::inlinableFunction()"));
    assertNotNullNorEmpty(
        Impl.getNameOf("@com.google.gwt.core.client.impl.ImplTest.Foo::statifiableFunction(*)"));
    assertNull(Impl.getNameOf("@com.google.gwt.core.client.impl.ImplTest.ClassWithAbstractMethod::"
        + "abstractMethod(*)"));

    if (areNamesObfuscatedOrNotPresent()) {
      // In obfuscated mode the names of the functions in the output code have no relation
      // to their source names.
      return;
    }

    String prunnableFnName =
        Impl.getNameOf("@com.google.gwt.core.client.impl.ImplTest::prunableFunction()");
    assertTrue("Expecting 'prunableFunction' got '" + prunnableFnName + "'",
        prunnableFnName.contains("prunableFunction"));

    String inlineableFnName =
        Impl.getNameOf("@com.google.gwt.core.client.impl.ImplTest::inlinableFunction()");
    assertTrue("Expecting 'inlinableFunction' got '" + inlineableFnName + "'",
        inlineableFnName.contains("inlinableFunction"));

    String statisfiableFnName =
        Impl.getNameOf("@com.google.gwt.core.client.impl.ImplTest.Foo::statifiableFunction(*)");
    assertTrue("Expecting 'statifiableFunction' got '" + statisfiableFnName + "'",
        statisfiableFnName.contains("statifiableFunction"));
  }

  // This function is never inlined because there exists a jsni reference to it.
  // TODO(rluble): This function should have a do not inline annotation.
  private static native boolean areNamesObfuscatedOrNotPresent() /*-{

    var fn = @ImplTest::areNamesObfuscatedOrNotPresent();
    return !(fn.name && fn.name.indexOf("areNamesObfuscated") != -1);
  }-*/;

  private void assertNotNullNorEmpty(String s) {
    assertNotNull(s);
    assertFalse(s.isEmpty());
  }
}
