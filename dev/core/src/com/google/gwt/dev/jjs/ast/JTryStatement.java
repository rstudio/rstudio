/*
 * Copyright 2006 Google Inc.
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

import java.util.List;

/**
 * Java try statement.
 */
public class JTryStatement extends JStatement {

  public JBlock tryBlock;
  public final HolderList catchArgs = new HolderList();
  public List/*<JBlock>*/ catchBlocks;
  public JBlock finallyBlock;

  public JTryStatement(JProgram program, JBlock tryBlock, List/*<JLocalRef>*/ catchArgs,
      List/*<JBlock>*/ catchBlocks, JBlock finallyBlock) {
    super(program);
    this.tryBlock = tryBlock;
    this.catchArgs.addAll(catchArgs);
    this.catchBlocks = catchBlocks;
    this.finallyBlock = finallyBlock;
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      tryBlock.traverse(visitor);
      catchArgs.traverse(visitor);
      for (int i = 0; i < catchBlocks.size(); ++i) {
        JBlock block = (JBlock) catchBlocks.get(i);
        block.traverse(visitor);
      }
      if (finallyBlock != null) {
        finallyBlock.traverse(visitor);
      }
    }
    visitor.endVisit(this);
  }
}
