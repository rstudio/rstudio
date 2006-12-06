// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

public class MethodInterfaceTest extends GWTTestCase implements MethodsOnly {

  public void f() {
  }

  public char[] g() {
    return null;
  }

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public int i() {
    return 0;
  }

  public String s() {
    return null;
  }

  public void testMethodCalls() {
    g();
    i();
    s();
  }

  public void testSuperMethodCalls() {
    MethodsOnly mo = this;
    mo.g();
    mo.i();
    mo.s();
  }

}
