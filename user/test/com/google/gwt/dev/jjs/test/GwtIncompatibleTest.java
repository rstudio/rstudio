/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.jjs.test;

import static com.google.gwt.dev.jjs.test.gwtincompatible.ClassWithGwtIncompatibleMethod.gwtIncompatibleMethod;

import com.google.gwt.dev.jjs.test.gwtincompatible.GwtIncompatible;
import com.google.gwt.dev.jjs.test.gwtincompatible.GwtIncompatibleClass;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests GwtIncompatible annotations.
 */
public class GwtIncompatibleTest extends GWTTestCase {

  /**
   * Class that uses reflection.
   */
  @GwtIncompatible("incompatible")
  private static class Foo  {

    public int getNbrConstructors() {
      // getConstructors is not available in the emulated java libraries used in GWT
      // and it would throw an error
      return  this.getClass().getConstructors().length;
    }
  }

  @GwtIncompatible("incompatible")
  public int incompatibleField = GwtIncompatibleTest.class.getConstructors().length;

  private static class DummyBar {
    int getClassFooNbrConstructors() {
      return -1;
    }
  }

  private static class Bar extends DummyBar {

    @GwtIncompatible("incompatible")
    @Override
    int getClassFooNbrConstructors() {
      gwtIncompatibleMethod();
      return new Foo().getNbrConstructors();
    }
  }

  private static class DifferentPackageAnnotations extends Bar {

    @com.google.gwt.core.shared.GwtIncompatible
    @Override
    int getClassFooNbrConstructors() {
      return new Foo().getNbrConstructors();
    }

    @com.google.gwt.dev.jjs.test.gwtincompatible.GwtIncompatible("also incompatible")
    int field = this.getClass().getConstructors().length;
  }

  private DummyBar getAnonymousDummyBar() {
    return new DummyBar() {
      @GwtIncompatible("incompatible")
      @Override
      int getClassFooNbrConstructors() {
        return new Foo().getNbrConstructors() + 1;
      }
     };
  }

  private DummyBar getAnonymousDummyBarWithAnonymousIncompatibleClass() {
    return new DummyBar() {
      @GwtIncompatible("incompatible")
      @Override
      int getClassFooNbrConstructors() {
        return new InDummyBar().createFoo().getNbrConstructors() + 1;
      }

      @GwtIncompatible("incompatible inner class of an anonymous inner class")
      class InDummyBar {
        public Foo createFoo() {
          return new Foo();
        }
      }
    };
  }

  public void testGwtIncompatibleReference() {
    // Have a reference to a GwtIncompatibleClass
    GwtIncompatibleClass instance = (GwtIncompatibleClass) null;

    assertNull(instance);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testIncompatibleClass() {
    Bar b = new Bar();
    assertEquals(-1, b.getClassFooNbrConstructors());
    assertEquals(-1, getAnonymousDummyBar().getClassFooNbrConstructors());
    assertEquals(-1,
        getAnonymousDummyBarWithAnonymousIncompatibleClass().getClassFooNbrConstructors());
    assertEquals(-1, new DifferentPackageAnnotations().getClassFooNbrConstructors());
  }
}
