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
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import java.io.PrintWriter;
import java.util.LinkedList;

/**
 * A Generator whose output is unstable and that contains nested types that reference some external
 * type.
 * <p>
 * Makes it possible to test what how much Generator output is recreated when a referenced external
 * type is modified.
 */
@RunsLocal
public class UnstableNestedAnonymousGenerator extends Generator {

  /**
   * Indicates which version of Generator output should be created.
   */
  public enum OutputVersion {
    A, B
  }

  public static LinkedList<OutputVersion> outputVersionOrder = Lists.newLinkedList();

  @Override
  public String generate(TreeLogger logger, GeneratorContext context, String typeName)
      throws UnableToCompleteException {
    PrintWriter pw = context.tryCreate(logger, "com.foo", "NestedAnonymousClasses");
    if (pw != null) {
      OutputVersion outputVersion = outputVersionOrder.removeFirst();

      pw.println("package com.foo;");
      pw.println("import java.lang.Runnable;");
      pw.println("public class NestedAnonymousClasses {");
      if (outputVersion == OutputVersion.A) {
        insertClassDefinitionOne(pw);
        insertClassDefinitionTwo(pw);
      } else {
        insertClassDefinitionTwo(pw);
        insertClassDefinitionOne(pw);
      }
      pw.println("  public NestedAnonymousClasses() {run();}");
      pw.println("  void run() {");
      pw.println("    new Runnable() {");
      pw.println("      public void run() {");
      pw.println("        new Runnable() {");
      pw.println("          public void run() {");
      if (outputVersion == OutputVersion.A) {
        pw.println("ClassOne classOne = new ClassOne();");
        pw.println("ClassTwo classTwo = new ClassTwo();");
      } else {
        pw.println("ClassTwo classTwo = new ClassTwo();");
        pw.println("ClassOne classOne = new ClassOne();");
      }
      pw.println("          }");
      pw.println("        }.run();");
      pw.println("      }");
      pw.println("    }.run();");
      pw.println("  }");
      pw.println("}");
      pw.flush();
      context.commit(logger, pw);
    }
    return "com.foo.NestedAnonymousClasses";
  }

  private void insertClassDefinitionOne(PrintWriter pw) {
    pw.println("class ClassOne {");
    pw.println("  Foo foo = new Foo();");
    pw.println("};");
  }

  private void insertClassDefinitionTwo(PrintWriter pw) {
    pw.println("class ClassTwo {");
    pw.println("  Foo foo = new Foo();");
    pw.println("};");
  }
}
