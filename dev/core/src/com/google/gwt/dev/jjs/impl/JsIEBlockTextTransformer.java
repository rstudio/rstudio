/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.core.ext.linker.StatementRanges;

import java.util.ArrayList;

/**
 * Limits top-level blocks to MAX_BLOCK_SIZE statements. 
 */
public class JsIEBlockTextTransformer extends JsAbstractTextTransformer {

  // uncomment to test

  //  private static final int MAX_BLOCK_SIZE = 10;
  private static final int MAX_BLOCK_SIZE = 1 << 15 - 1;

  private boolean doSplits;

  private int currentStatementCount;

  public JsIEBlockTextTransformer(String js, StatementRanges statementRanges) {
    super(js, statementRanges);
  }

  public JsIEBlockTextTransformer(JsAbstractTextTransformer xformer) {
    super(xformer);
  }

  /**
   * Do not perform clustering, only fix up IE7 block issue.
   */
  public void exec() {
    doSplits = statementRanges.numStatements() > MAX_BLOCK_SIZE;
    if (doSplits) {
      int statementIndices[] = new int[statementRanges.numStatements()];
      for (int i = 0; i < statementRanges.numStatements(); i++) {
        statementIndices[i] = i;
      }
      recomputeJsAndStatementRanges(statementIndices);
    }
  }

  /**
   * Record start of statement, and optionally inject new open block.
   */
  protected void beginStatement(StringBuilder newJs,
      ArrayList<Integer> starts) {
    if (doSplits && currentStatementCount == 0) {
      super.beginStatement(newJs, starts);
      newJs.append('{');
    } else if (!doSplits) {
      super.beginStatement(newJs, starts);
    }
  }

  @Override
  protected void beginStatements(StringBuilder newJs, ArrayList<Integer> starts,
      ArrayList<Integer> ends) {
    super.beginStatements(newJs, starts, ends);
    currentStatementCount = 0;
  }

  /**
   * Record end of statement, and optionally inject close block, if block is
   * full.
   */
  protected void endStatement(StringBuilder newJs, ArrayList<Integer> ends) {
    currentStatementCount++;
    if (doSplits && currentStatementCount == MAX_BLOCK_SIZE) {
      newJs.append('}');
      super.endStatement(newJs, ends);
      currentStatementCount = 0;
    } else if (!doSplits) {
      super.endStatement(newJs, ends);
    }
  }

  /**
   * Used to close a trailing block which never filled.
   */
  @Override
  protected void endStatements(StringBuilder newJs, ArrayList<Integer> starts,
      ArrayList<Integer> ends) {
    optionallyCloseLastBlock(newJs, ends);
    super.endStatements(newJs, starts, ends);
  }

  /**
   * Close last block if it never filled.
   */
  private void optionallyCloseLastBlock(StringBuilder newJs,
      ArrayList<Integer> ends) {
    if (doSplits && currentStatementCount > 1
        && currentStatementCount < MAX_BLOCK_SIZE) {
      newJs.append("}");
      ends.add(newJs.length());
    }
  }
}