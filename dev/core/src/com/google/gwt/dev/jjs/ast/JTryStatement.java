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
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.SourceInfo;

import java.io.Serializable;
import java.util.List;

/**
 * Java try statement.
 */
public class JTryStatement extends JStatement {

  /**
   * Represents the catch clause parts of the try statement.
   */
  public static class CatchClause implements Serializable {
    private final List<JType> catchTypes;
    private final JLocalRef arg;
    private final JBlock block;

    public CatchClause(List<JType> catchTypes, JLocalRef arg, JBlock block) {
      this.catchTypes = catchTypes;
      this.arg = arg;
      this.block = block;
    }

    public List<JType> getTypes() {
      return catchTypes;
    }

    public JLocalRef getArg() {
      return arg;
    }

    public JBlock getBlock() {
      return block;
    }
  }

  private final List<CatchClause> catchClauses;
  private final JBlock finallyBlock;
  private final JBlock tryBlock;

  /**
   * Construct a Java try statement.
   *
   * Parameters catchTypes, catchArgs and catchBlocks must agree on size. Each element of each
   * of these lists corresponds to a catch statement.
   *
   * @param info the source information.
   * @param tryBlock the statement block inside the try construct.
   * @param catchClauses  each element of this list contains a catch clause.
   * @param finallyBlock the statement block corresponding to the finally construct.
   */
  public JTryStatement(SourceInfo info, JBlock tryBlock, List<CatchClause> catchClauses,
      JBlock finallyBlock) {
    super(info);
    this.tryBlock = tryBlock;
    this.catchClauses = catchClauses;
    this.finallyBlock = finallyBlock;
  }

  public List<CatchClause> getCatchClauses() {
    return catchClauses;
  }

  public JBlock getFinallyBlock() {
    return finallyBlock;
  }

  public JBlock getTryBlock() {
    return tryBlock;
  }

  @Override
  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(tryBlock);

      for (CatchClause clause : catchClauses) {
        visitor.accept(clause.getArg());
        visitor.accept(clause.getBlock());
      }
      // TODO: normalize this so it's never null?
      if (finallyBlock != null) {
        visitor.accept(finallyBlock);
      }
    }
    visitor.endVisit(this, ctx);
  }
}
