/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.jdt.StandardSourceOracle;
import com.google.gwt.dev.jdt.StaticCompilationUnitProvider;

/**
 * Does a little extra magic to handle hosted mode JSNI and
 * <code>GWT.create()</code>.
 */
public class HostedModeSourceOracle extends StandardSourceOracle {

  private final CompilationUnitProvider cuMeta = new StaticCompilationUnitProvider(
      "com.google.gwt.core.client", "GWT", null) {
    public char[] getSource() {
      StringBuffer sb = new StringBuffer();
      sb.append("package com.google.gwt.core.client;\n");
      sb.append("public final class GWT {\n");

      // UncaughtExceptionHandler
      //
      sb.append("  public interface UncaughtExceptionHandler {\n");
      sb.append("    void onUncaughtException(Throwable e);\n");
      sb.append("  }\n");

      sb.append("  private static String sModuleBaseURL = null;\n");

      // Hosted mode default to logging
      //
      sb.append("  private static UncaughtExceptionHandler sUncaughtExceptionHandler = \n");
      sb.append("    new UncaughtExceptionHandler() {\n");
      sb.append("      public void onUncaughtException(Throwable e) {\n");
      sb.append("        log(\"Uncaught exception escaped\", e);\n");
      sb.append("      }\n");
      sb.append("    };\n");

      // Implement getUncaughtExceptionHandler()
      //
      sb.append("  public static UncaughtExceptionHandler getUncaughtExceptionHandler() {\n");
      sb.append("    return sUncaughtExceptionHandler;\n");
      sb.append("  }\n");

      // Implement setUncaughtExceptionHandler()
      //
      sb.append("  public static void setUncaughtExceptionHandler(\n");
      sb.append("      UncaughtExceptionHandler handler) {\n");
      sb.append("    sUncaughtExceptionHandler = handler;\n");
      sb.append("  }\n");

      // Proxy create().
      //
      sb.append("  public static Object create(Class classLiteral) {\n");
      sb.append("    return ");
      sb.append(ShellGWT.class.getName());
      sb.append(".create(classLiteral);\n");
      sb.append("  }\n");

      // Proxy getTypeName().
      //
      sb.append("  public static String getTypeName(Object o) {\n");
      sb.append("    return ");
      sb.append(ShellGWT.class.getName());
      sb.append(".getTypeName(o);");
      sb.append("  }\n");

      // Hard-code isScript() to false.
      //
      sb.append("  public static boolean isScript() {\n");
      sb.append("    return false;");
      sb.append("  }\n");

      // Actually, we don't need to proxy getModuleName().
      // It's hard-coded.
      //
      sb.append("  public static String getModuleName() {\n");
      sb.append("    return \"");
      sb.append(moduleName);
      sb.append("\";\n");
      sb.append("  }\n");

      // Proxy getModuleBaseURL() to the Impl class.
      //
      sb.append("  public static String getModuleBaseURL() {\n");
      sb.append("    if (sModuleBaseURL == null) {\n");
      sb.append("      sModuleBaseURL = Impl.getModuleBaseURL();\n");
      sb.append("    }\n");
      sb.append("    return sModuleBaseURL;\n");
      sb.append("  }\n");

      // Proxy log().
      //
      sb.append("  public static void log(String message, Throwable e) {\n  ");
      sb.append(ShellGWT.class.getName());
      sb.append(".log(message, e);\n");
      sb.append("  }\n");

      sb.append("}\n");
      return sb.toString().toCharArray();
    }
  };

  private final JsniInjector injector;

  private final String moduleName;

  public HostedModeSourceOracle(TypeOracle typeOracle, String moduleName) {
    super(typeOracle);
    this.moduleName = moduleName;
    this.injector = new JsniInjector(typeOracle);
  }

  protected CompilationUnitProvider doFilterCompilationUnit(TreeLogger logger,
      String typeName, CompilationUnitProvider existing)
      throws UnableToCompleteException {

    // MAGIC: The implementation of GWT.create() is handled intrinsically by
    // the compiler in web mode, so its on-disk definition is totally empty so
    // as to be trivially compilable. In hosted mode, GWT.create() is
    // actually a real call, so we patch different source for that class in
    // hosted mode only.
    //
    // MAGIC: The implementation of GWT.getTypeSignature() is handled
    // differently in web mode versus hosted mode. The on-disk version is
    // the web mode version, so here we substitute the hosted mode version.
    //
    if (typeName.equals("com.google.gwt.core.client.GWT")) {
      return cuMeta;
    }

    // Otherwise, it's a regular translatable type, but we want to make sure
    // its JSNI stuff, if any, gets handled.
    //
    CompilationUnitProvider jsnified = injector.inject(logger, existing);

    return jsnified;
  }
}
