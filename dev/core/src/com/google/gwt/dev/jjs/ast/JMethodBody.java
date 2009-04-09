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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.util.collect.Lists;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a the body of a method. Can be Java or JSNI.
 */
public class JMethodBody extends JAbstractMethodBody {

  private JBlock block;
  private List<JLocal> locals = Collections.emptyList();

  public JMethodBody(SourceInfo info) {
    super(info);
    block = new JBlock(info);
  }

  /**
   * Adds a local to this method body.
   */
  public void addLocal(JLocal local) {
    locals = Lists.add(locals, local);
  }

  public JBlock getBlock() {
    return block;
  }

  /**
   * Returns this method's local variables.
   */
  public List<JLocal> getLocals() {
    return locals;
  }

  public List<JStatement> getStatements() {
    return block.getStatements();
  }

  @Override
  public boolean isNative() {
    return false;
  }

  /**
   * Removes a local from this method body.
   */
  public void removeLocal(int index) {
    locals = Lists.remove(locals, index);
  }

  /**
   * Sorts this method's locals according to the specified sort.
   */
  public void sortLocals(Comparator<? super JLocal> sort) {
    locals = Lists.sort(locals, sort);
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      locals = visitor.acceptImmutable(locals);
      block = (JBlock) visitor.accept(block);
    }
    visitor.endVisit(this, ctx);
  }
}
