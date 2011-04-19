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

import java.util.List;

/**
 * Java try statement.
 */
public class JTryStatement extends JStatement {

  private final List<JLocalRef> catchArgs;
  private final List<JBlock> catchBlocks;
  private final JBlock finallyBlock;
  private final JBlock tryBlock;

  public JTryStatement(SourceInfo info, JBlock tryBlock, List<JLocalRef> catchArgs,
      List<JBlock> catchBlocks, JBlock finallyBlock) {
    super(info);
    assert (catchArgs.size() == catchBlocks.size());
    this.tryBlock = tryBlock;
    this.catchArgs = catchArgs;
    this.catchBlocks = catchBlocks;
    this.finallyBlock = finallyBlock;
  }

  public List<JLocalRef> getCatchArgs() {
    return catchArgs;
  }

  public List<JBlock> getCatchBlocks() {
    return catchBlocks;
  }

  public JBlock getFinallyBlock() {
    return finallyBlock;
  }

  public JBlock getTryBlock() {
    return tryBlock;
  }

  public void traverse(JVisitor visitor, Context ctx) {
    if (visitor.visit(this, ctx)) {
      visitor.accept(tryBlock);
      visitor.accept(catchArgs);
      visitor.accept(catchBlocks);
      // TODO: normalize this so it's never null?
      if (finallyBlock != null) {
        visitor.accept(finallyBlock);
      }
    }
    visitor.endVisit(this, ctx);
  }
}
