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
import com.google.gwt.dev.jjs.ast.JBinaryOperation;
import com.google.gwt.dev.jjs.ast.JCastOperation;
import com.google.gwt.dev.jjs.ast.JConditional;
import com.google.gwt.dev.jjs.ast.JConstructor;
import com.google.gwt.dev.jjs.ast.JGwtCreate;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JModVisitor;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNonNullType;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.dev.jjs.ast.JVariable;

/**
 * This class is a visitor which can find all sites where a JType can be updated
 * from one type to another, and calls an overridable remap method for each
 * instance.  An extending class can override the remap and return a new type
 * where it deems it necessary, such as to replace the type of all references
 * of a class.
 */
public class TypeRemapper extends JModVisitor {
  
  @Override
  public void endVisit(JBinaryOperation x, Context ctx) {
    x.setType(remap(x.getType()));
  }
  
  @Override
  public void endVisit(JCastOperation x, Context ctx) {
    // JCastOperation doesn't have a settable castType method, so need to
    // create a new one and do a replacement.
    JType remapCastType = remap(x.getCastType());
    if (remapCastType != x.getCastType()) {
      JCastOperation newX = new JCastOperation(x.getSourceInfo(), 
                                               remapCastType, x.getExpr());
      ctx.replaceMe(newX);
    }
  }
  
  @Override
  public void endVisit(JConditional x, Context ctx) {
    x.setType(remap(x.getType()));
  }
  
  @Override
  public void endVisit(JConstructor x, Context ctx) {
    x.setType(remap(x.getType()));
  }
  
  @Override
  public void endVisit(JGwtCreate x, Context ctx) {
    x.setType(remap(x.getType()));
  }
  
  @Override
  public void endVisit(JMethod x, Context ctx) {
    x.setType(remap(x.getType()));
  }
  
  @Override
  public void endVisit(JNewArray x, Context ctx) {
    x.setType((JNonNullType) remap(x.getType()));
  }
  
  @Override
  public void endVisit(JVariable x, Context ctx) {
    x.setType(remap(x.getType()));
  }
  
  /**
   * An overriding method will be called for each detected JType element.
   * @param type
   */
  protected JType remap(JType type) {
    // override to possibly return an different type
    return type;
  }
}
