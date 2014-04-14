/*
 * Copyright 2010 Google Inc.
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
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JGwtCreate;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVariable;

/**
 * A visitor that changes all JType references in the AST. Subclasses override
 * {@link #remap(JType)} to replace all occurrences of one or more types with
 * different types.
 */
public abstract class TypeRemapper extends JModVisitor {

  @Override
  public void endVisit(JBinaryOperation x, Context ctx) {
    x.setType(remap(x.getType()));
  }

  @Override
  public void endVisit(JCastOperation x, Context ctx) {
    /*
     * JCastOperation doesn't have a settable castType method, so need to create
     * a new one and do a replacement. Use remap() instead of modRemap() since
     * the ctx.replaceMe() will record a change.
     */
    JType remapCastType = remap(x.getCastType());
    if (remapCastType != x.getCastType()) {
      JCastOperation newX = new JCastOperation(x.getSourceInfo(), remapCastType, x.getExpr());
      ctx.replaceMe(newX);
    }
  }

  @Override
  public void endVisit(JConditional x, Context ctx) {
    x.setType(modRemap(x.getType()));
  }

  @Override
  public void endVisit(JConstructor x, Context ctx) {
    x.setType(modRemap(x.getType()));
  }

  @Override
  public void endVisit(JGwtCreate x, Context ctx) {
    x.setType(modRemap(x.getType()));
  }

  @Override
  public void endVisit(JMethod x, Context ctx) {
    x.setType(modRemap(x.getType()));
  }

  @Override
  public void endVisit(JNewArray x, Context ctx) {
    x.setType((JArrayType) modRemap(x.getArrayType()));
  }

  @Override
  public void endVisit(JVariable x, Context ctx) {
    x.setType(modRemap(x.getType()));
  }

  /**
   * Override to return a possibly-different type.
   *
   * @param type the original type
   * @return a replacement type, which may be the original type
   */
  protected abstract JType remap(JType type);

  private JType modRemap(JType type) {
    JType result = remap(type);
    if (result != type) {
      madeChanges();
    }
    return result;
  }
}
