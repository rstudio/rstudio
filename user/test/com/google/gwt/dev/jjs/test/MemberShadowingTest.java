package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Tests that field and method shadowing works correctly in hosted and web
 * modes.
 * 
 * NOTE: Also tests that the {@link CompilingClassLoader} can resolve binary and
 * source level class names inside of JSNI Java member references.  Where
 * should this really go?
 */
public class MemberShadowingTest extends GWTTestCase {
  public class Subclass extends Superclass {
    public int getSubclassA() {
      return a;
    }

    public void setSubclassA(int a) {
      this.a = a;
    }

    private void foo() {
      fail("MemberShadowingTest.Subclass.foo() called instead of MemberShadowingTest.Superclass.foo()");
    }

    public native void callShadowedMethod() /*-{
     this.@com.google.gwt.dev.jjs.test.MemberShadowingTest$Superclass::foo()();
     }-*/;

    public int a;
  }

  public class Superclass {
    public native int getSuperclassA() /*-{
     return this.@com.google.gwt.dev.jjs.test.MemberShadowingTest.Superclass::a;
     }-*/;

    public void setSuperclassA(int a) {
      this.a = a;
    }

    private void foo() {
      /*
       * if the method that this one shadows is called the test will fail 
       * otherwise if we get here we are good
       */
    }

    public int a;
  }

  public static interface Foo {
    void f();
  }

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testFieldShadowing() {
    Subclass s1 = new Subclass();

    s1.setSuperclassA(1);
    s1.setSubclassA(2);

    assertEquals(s1.getSuperclassA(), 1);
  }

  public void testMethodShadowing() {
    Subclass s1 = new Subclass();

    s1.callShadowedMethod();
  }
}
