/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import java.io.PrintWriter;

/**
 * Generator used to generate a class unique to the current module to hold the boot strap entry
 * method that the compiler generates. The contents of the function will be filled in with calls to
 * registered entry points.
 */
public class EntryMethodHolderGenerator extends Generator {

  private static final String ENTRY_METHOD_HOLDER_SUFFIX = "EntryMethodHolder";
  private static final String PACKAGE_PATH = "com.google.gwt.lang";

  @Override
  public String generate(TreeLogger logger, GeneratorContext context, String moduleName)
      throws UnableToCompleteException {
    // Module names aren't subject to Java class name restrictions, so must be escaped.
    String typeName = Generator.escapeClassName(moduleName + "_" + ENTRY_METHOD_HOLDER_SUFFIX);
    PrintWriter out = context.tryCreate(logger, PACKAGE_PATH, typeName);

    if (out != null) {
      out.println("package " + PACKAGE_PATH + ";");
      out.println("public class " + typeName + " {");
      out.println("  public static final void init() {");
      out.println("    // Filled in by the compiler to call entry methods.");
      out.println("  }");
      out.println("}");

      context.commit(logger, out);
    } else {
      // Must have been a cache hit.
    }

    return PACKAGE_PATH + "." + typeName;
  }
}
