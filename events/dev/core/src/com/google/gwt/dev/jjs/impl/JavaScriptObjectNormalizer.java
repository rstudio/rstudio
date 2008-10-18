/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JInstanceOf;
import com.google.gwt.dev.jjs.ast.JLocal;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JParameter;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JType;

/**
 * Replace references to JSO subtypes with JSO itself.
 */
public class JavaScriptObjectNormalizer {

  /**
   * Map types from JSO subtypes to JSO itself.
   */
  private class NormalizeVisitor extends JModVisitor {

    @Override
    public void endVisit(JCastOperation x, Context ctx) {
      JType newType = translate(x.getCastType());
      if (newType != x.getCastType()) {
        ctx.replaceMe(new JCastOperation(program, x.getSourceInfo(), newType,
            x.getExpr()));
      }
    }

    @Override
    public void endVisit(JClassLiteral x, Context ctx) {
      JType newType = translate(x.getRefType());
      if (newType != x.getRefType()) {
        ctx.replaceMe(program.getLiteralClass(newType));
      }
    }

    @Override
    public void endVisit(JField x, Context ctx) {
      x.setType(translate(x.getType()));
    }

    @Override
    public void endVisit(JInstanceOf x, Context ctx) {
      JReferenceType newType = (JReferenceType) translate(x.getTestType());
      if (newType != x.getTestType()) {
        ctx.replaceMe(new JInstanceOf(program, x.getSourceInfo(), newType,
            x.getExpr()));
      }
    }

    @Override
    public void endVisit(JLocal x, Context ctx) {
      x.setType(translate(x.getType()));
    }

    @Override
    public void endVisit(JMethod x, Context ctx) {
      x.setType(translate(x.getType()));
    }

    @Override
    public void endVisit(JNewArray x, Context ctx) {
      x.setType(translate(x.getType()));
    }

    @Override
    public void endVisit(JParameter x, Context ctx) {
      x.setType(translate(x.getType()));
    }

    private JType translate(JType type) {
      if (program.isJavaScriptObject(type)) {
        return program.getJavaScriptObject();
      } else if (type instanceof JArrayType) {
        JArrayType arrayType = (JArrayType) type;
        if (program.isJavaScriptObject(arrayType.getLeafType())) {
          return program.getTypeArray(program.getJavaScriptObject(),
              arrayType.getDims());
        }
      }
      return type;
    }
  }

  public static void exec(JProgram program) {
    new JavaScriptObjectNormalizer(program).execImpl();
  }

  private final JProgram program;

  private JavaScriptObjectNormalizer(JProgram program) {
    this.program = program;
  }

  private void execImpl() {
    NormalizeVisitor visitor = new NormalizeVisitor();
    visitor.accept(program);
  }

}
