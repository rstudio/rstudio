/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.dev;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.Generator.RunsLocal;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import java.io.PrintWriter;

/**
 * A Generator whose output attempts to rebind String to trigger another Generator.
 */
@RunsLocal
public class CauseStringRebindGenerator extends Generator {

  public static int runCount = 0;

  @Override
  public String generate(TreeLogger logger, GeneratorContext context, String typeName)
      throws UnableToCompleteException {
    runCount++;

    PrintWriter pw = context.tryCreate(logger, "com.foo", "Bar");
    if (pw != null) {
      pw.println("package com.foo;");
      pw.println("import com.google.gwt.core.client.GWT;");
      pw.println("public class Bar {");
      pw.println("  private String string = GWT.create(String.class);");
      pw.println("}");
      context.commit(logger, pw);
    }
    return "com.foo.Bar";
  }
}
