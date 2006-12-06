// Copyright 2006 Google Inc. All Rights Reserved.
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
