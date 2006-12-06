// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.CanBeFinal;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JInterfaceType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JNullType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.js.JsniMethod;
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

  public SourceGenerationVisitor(TextOutput textOutput) {
    super(textOutput);
  }

  // @Override
  public boolean visit(JClassType x) {
    // All classes are deemed "static" so the monolithic compile results can be
    // copy/pasted into a single enclosing class.
    print(CHARS_STATIC);

    super.visit(x);

    openBlock();

    for (int i = 0; i < x.fields.size(); ++i) {
      JField it = (JField) x.fields.get(i);
      it.traverse(this);
      newline();
      newline();
    }
    for (int i = 0; i < x.methods.size(); ++i) {
      JMethod it = (JMethod) x.methods.get(i);
      if (!isEmptyInitializer(it)) {
        it.traverse(this);
        newline();
        newline();
      }
    }

    closeBlock();
    return false;
  }

  public boolean visit(JField x) {
    super.visit(x);

    if (x.constInitializer != null) {
      print(" = ");
      x.constInitializer.traverse(this);
    }
    semi();
    return false;
  }

  // @Override
  public boolean visit(JInterfaceType x) {
    super.visit(x);

    openBlock();

    for (int i = 0; i < x.fields.size(); ++i) {
      JField field = (JField) x.fields.get(i);
      field.traverse(this);
      newline();
      newline();
    }
    for (int i = 0; i < x.methods.size(); ++i) {
      JMethod method = (JMethod) x.methods.get(i);
      method.traverse(this);
      newline();
      newline();
    }

    closeBlock();
    return false;
  }

  // @Override
  public boolean visit(JMethod x) {
    // special: transcribe clinit and init as if they were initializer blocks
    if (isInitializer(x)) {
      if (x.isStatic()) {
        print(CHARS_STATIC);
      }
      x.body.traverse(this);
    } else {
      super.visit(x);

      if (x.isAbstract()) {
        semi();
      } else {
        space();
        x.body.traverse(this);
      }
    }

    return false;
  }

  // @Override
  public boolean visit(JsniMethod x) {
    super.visit(x);
    space();
    print(CHARS_SLASHSTAR);
    String jsniCode = x.getFunc().getBody().toString();
    String[] splits = jsniCode.split("\r|\n");
    for (int i = 0, c = splits.length; i < c; ++i) {
      if (i > 0) {
        newline();
      }
      print(splits[i]);
    }
    print(CHARS_STARSLASH);
    semi();

    return false;
  }

  // @Override
  public boolean visit(JProgram x) {
    for (int i = 0; i < x.entryMethods.size(); ++i) {
      JMethod method = (JMethod) x.entryMethods.get(i);
      method.traverse(this);
      newline();
      newline();
    }
    for (int i = 0; i < x.getDeclaredTypes().size(); ++i) {
      JReferenceType type = (JReferenceType) x.getDeclaredTypes().get(i);
      type.traverse(this);
      newline();
      newline();
    }
    return false;
  }

  // @Override
  protected void printTypeName(JType type) {
    if (type instanceof JNullType) {
      print("Object");
    } else {
      super.printTypeName(type);
    }
  }

  // @Override
  protected void printMemberFinalFlag(CanBeFinal x) {
    // suppress final flags
  }

  private boolean isInitializer(JMethod x) {
    return x.getName().equals("$clinit") || x.getName().equals("$init");
  }

  private boolean isEmptyInitializer(JMethod x) {
    return isInitializer(x) && (x.body.statements.size() == 0);
  }

}
