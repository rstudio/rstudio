/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * TODO: document me.
 */
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
