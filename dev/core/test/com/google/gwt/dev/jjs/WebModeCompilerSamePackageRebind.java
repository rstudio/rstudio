// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.UnableToCompleteException;

import java.util.HashMap;
import java.util.Map;

public class WebModeCompilerSamePackageRebind extends ReflectiveWebModeCompilerTestCase {

  public void testCompile() throws UnableToCompleteException {
    Map rebinds = new HashMap();
    rebinds.put("test.rebind.SamePackageRebindBefore", "test.rebind.SamePackageRebindAfter");
    compile("test.rebind.TestRebind", rebinds);
  }

  public static class Package_test_rebind {
    
    public static String src_SamePackageRebindBefore() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test.rebind;\n");
      sb.append("public class SamePackageRebindBefore {\n");
      sb.append("    protected void foo() {\n");
      sb.append("    }\n");
      sb.append("}\n");
      return sb.toString();
    }

    public static String src_SamePackageRebindAfter() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test.rebind;\n");
      sb.append("public class SamePackageRebindAfter {\n");
      sb.append("    protected void foo() {\n");
      sb.append("    }\n");
      sb.append("}\n");
      return sb.toString();
    }

    public static String src_TestRebind() {
      StringBuffer sb = new StringBuffer();
      sb.append("package test.rebind;\n");
      sb.append("import com.google.gwt.core.client.Rebind;");
      sb.append("public class TestRebind {\n");
      sb.append("    public void onModuleLoad() {\n");
      sb.append("       SamePackageRebindBefore b;\n");
      sb.append("       b = (SamePackageRebindBefore)GWT.create(SamePackageRebindBefore.class);\n");
      sb.append("    }\n");
      sb.append("}\n");
      return sb.toString();
    }
  }

}
