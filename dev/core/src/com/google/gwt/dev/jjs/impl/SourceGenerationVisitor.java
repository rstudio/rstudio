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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.CanBeFinal;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodBody;
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.js.JsniMethodBody;
import com.google.gwt.dev.util.TextOutput;

/**
 * Generates Java source from our AST. ToStringGenerationVisitor is for
 * relatively short toString() results, for easy viewing in a debugger. This
 * subclass delves into the bodies of classes, interfaces, and methods to
 * produce the whole source tree.
 * 
 * The goal is not to generate the input source tree. Rather, the goal is to
 * produce a set of classes that can be pasted into an enclosing class and
 * compiled with a standard Java compiler. In practice, there are cases that
 * require hand-editting to actually get a full compilation, due to Java's
 * built-in reliance on particular built-in types.
 * 
 * Known to be broken: Our generated String, Class, and Throwable are not
 * compatable with the real ones, which breaks string literals, class literals,
 * try/catch/throw, and overrides of Object methods.
 */
public class SourceGenerationVisitor extends ToStringGenerationVisitor {

  private static boolean isEmptyInitializer(JMethod x) {
    return isInitializer(x)
        && (((JMethodBody) x.getBody()).getStatements().size() == 0);
  }

  public SourceGenerationVisitor(TextOutput textOutput) {
    super(textOutput);
  }

  @Override
  public boolean visit(JClassType x, Context ctx) {
    // All classes are deemed "static" so the monolithic compile results can be
    // copy/pasted into a single enclosing class.
    print(CHARS_STATIC);

    super.visit(x, ctx);

    openBlock();

    for (int i = 0; i < x.fields.size(); ++i) {
      JField it = x.fields.get(i);
      accept(it);
      newline();
      newline();
    }
    for (int i = 0; i < x.methods.size(); ++i) {
      JMethod it = x.methods.get(i);
      if (!isEmptyInitializer(it)) {
        accept(it);
        newline();
        newline();
      }
    }

    closeBlock();
    return false;
  }

  @Override
  public boolean visit(JField x, Context ctx) {
    super.visit(x, ctx);

    if (x.getConstInitializer() != null) {
      print(" = ");
      accept(x.getConstInitializer());
    }
    semi();
    return false;
  }

  @Override
  public boolean visit(JInterfaceType x, Context ctx) {
    super.visit(x, ctx);

    openBlock();

    for (int i = 0; i < x.fields.size(); ++i) {
      JField field = x.fields.get(i);
      accept(field);
      newline();
      newline();
    }
    for (int i = 0; i < x.methods.size(); ++i) {
      JMethod method = x.methods.get(i);
      accept(method);
      newline();
      newline();
    }

    closeBlock();
    return false;
  }

  @Override
  public boolean visit(JProgram x, Context ctx) {
    for (int i = 0; i < x.entryMethods.size(); ++i) {
      JMethod method = x.entryMethods.get(i);
      accept(method);
      newline();
      newline();
    }
    for (int i = 0; i < x.getDeclaredTypes().size(); ++i) {
      JReferenceType type = x.getDeclaredTypes().get(i);
      accept(type);
      newline();
      newline();
    }
    return false;
  }

  @Override
  public boolean visit(JsniMethodBody x, Context ctx) {
    print("/*-");
    String jsniCode = x.getFunc().getBody().toString();
    String[] splits = jsniCode.split("\r|\n");
    for (int i = 0, c = splits.length; i < c; ++i) {
      if (i > 0) {
        newline();
      }
      print(splits[i]);
    }
    print("-*/");
    semi();
    return false;
  }

  @Override
  protected void printMemberFinalFlag(CanBeFinal x) {
    // suppress final flags
  }

  @Override
  protected void printTypeName(JType type) {
    if (type instanceof JNullType) {
      print("Object");
    } else {
      super.printTypeName(type);
    }
  }

  @Override
  protected boolean shouldPrintMethodBody() {
    return true;
  }

}
