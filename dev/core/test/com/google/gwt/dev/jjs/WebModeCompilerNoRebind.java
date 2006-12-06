// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs;

/**
 * 
 */
public class WebModeCompilerNoRebind extends ReflectiveWebModeCompilerTestCase {

  public void testCompile() throws Exception {
    compile("test.TestNoRebind", null);
  }

  public static class Package_test {
    public static String src_TestNoRebind() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test;\n");
      sb.append("public class TestNoRebind {\n");
      sb.append("    public void onModuleLoad() {\n");
      sb.append("    }\n");
      sb.append("}\n");
      return sb.toString();
    }
  }
}
