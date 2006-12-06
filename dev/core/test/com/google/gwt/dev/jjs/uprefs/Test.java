// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.uprefs;

import com.google.gwt.dev.jjs.SimpleWebModeCompilerTestCase;

public class Test extends SimpleWebModeCompilerTestCase {

  public void testCompile() throws Exception {
    compile(Main.class.getName(), null);
  }

}
