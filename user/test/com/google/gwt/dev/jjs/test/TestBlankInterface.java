// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;

public class TestBlankInterface extends GWTTestCase implements
    NoMethodsOrFields {

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testBlank() {
    // supposed to be blank
  }

}
