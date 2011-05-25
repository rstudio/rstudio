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
import com.google.gwt.core.ext.soyc.Range;
import com.google.gwt.dev.jjs.SourceInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Limits top-level blocks to MAX_BLOCK_SIZE statements.
 */
public class JsIEBlockTextTransformer extends JsAbstractTextTransformer {

  // uncomment to test

  // private static final int MAX_BLOCK_SIZE = 10;
  private static final int MAX_BLOCK_SIZE = 1 << 15 - 1;

  private int currentStatementCount;

  private boolean doSplits;
  
  private Set<Integer> statementsAddedBlockClose = new HashSet<Integer>();
  
  private Set<Integer> statementsAddedBlockOpen = new HashSet<Integer>();

  public JsIEBlockTextTransformer(JsAbstractTextTransformer xformer) {
    super(xformer);
  }

  public JsIEBlockTextTransformer(String js, StatementRanges statementRanges, 
      Map<Range, SourceInfo> sourceInfoMap) {
    super(js, statementRanges, sourceInfoMap);
  }

  /**
   * Do not perform clustering, only fix up IE7 block issue.
   */
  @Override
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
  
  public Set<Integer> getStatementsAddedBlockClose() {
    return statementsAddedBlockClose;
  }
  
  public Set<Integer> getStatementsAddedBlockOpen() {
    return statementsAddedBlockOpen;
  }

  /**
   * Record start of statement, and optionally inject new open block.
   */
  @Override
  protected void beginStatement(int index, StringBuilder newJs, ArrayList<Integer> starts) {
    if (doSplits && currentStatementCount == 0) {
      super.beginStatement(index, newJs, starts);
      newJs.append('{');
      statementsAddedBlockOpen.add(Integer.valueOf(index));
    } else if (!doSplits) {
      super.beginStatement(index, newJs, starts);
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
  @Override
  protected void endStatement(int index, StringBuilder newJs, ArrayList<Integer> ends) {
    currentStatementCount++;
    if (doSplits && currentStatementCount == MAX_BLOCK_SIZE) {
      newJs.append('}');
      super.endStatement(index, newJs, ends);
      currentStatementCount = 0;
      statementsAddedBlockClose.add(Integer.valueOf(index));
    } else if (!doSplits) {
      super.endStatement(index, newJs, ends);
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
   * Fixes the index ranges of individual expressions in the generated
   * JS after chunking statements into blocks that satisfy the IE block
   * size problem. Loops over each expression, determines whether the
   * statement in which it falls has a brace inserted before/after, and
   * shifts forward according to where it falls in the block.
   */
  @Override
  protected void updateSourceInfoMap() {
    if (sourceInfoMap != null) {
      Range[] oldExpressionRanges = sourceInfoMap.keySet().toArray(new Range[0]);
      Arrays.sort(oldExpressionRanges, Range.SOURCE_ORDER_COMPARATOR);
      
      // iterate over expression ranges and shift
      Map<Range, SourceInfo> updatedInfoMap = new HashMap<Range, SourceInfo>();
      Range entireProgram = 
        new Range(0, originalStatementRanges.end(originalStatementRanges.numStatements() - 1));
      int shift = 0;
      
      // set to keep track of which statements have already shifted.
      // need to account for when a shift has already been added for the extra
      // open brace in a statement--sometimes there are multiple expressions
      // that all start at the same place a the beginning of a statement in
      // the expression list
      // ex: _.gC=function x()... yields the expressions _, _.gC, _.gC = ...
      Set<Integer> shiftAdded = new HashSet<Integer>();
      
      for (int i = 0, j = 0; j < oldExpressionRanges.length; j++) {
        Range oldExpression = oldExpressionRanges[j];
        if (oldExpression.equals(entireProgram)) {
          continue;
        }
        
        if (originalStatementRanges.start(i) > oldExpression.getStart() 
            || oldExpression.getEnd() > originalStatementRanges.end(i)) {
          
          // expression should fall in the next statement
          i++;
          assert originalStatementRanges.start(i) <= oldExpression.getStart() 
            && oldExpression.getEnd() <= originalStatementRanges.end(i);
          
          if (statementsAddedBlockClose.contains(Integer.valueOf(i - 1))) {
            // there's an extra statement index in the addedBlockClose list,
            // which corresponds to the extra closing brace at the end of the
            // program. but this index doesn't match up to the indices in the
            // old statement ranges--it's equal to the # of statements in the
            // original code divided by the IE block size
            if (i != statementRanges.numStatements()) {
              shift++;
            }
          }
        }
        
        if (statementsAddedBlockOpen.contains(Integer.valueOf(i)) 
            && oldExpression.getStart() == originalStatementRanges.start(i) 
            && !shiftAdded.contains(i)) {
          
          shift++;
          shiftAdded.add(Integer.valueOf(i));
        }
  
        int newStart = oldExpression.getStart() + shift;
        int newEnd = oldExpression.getEnd() + shift;
        
        Range newExpression = new Range(newStart, newEnd);
        updatedInfoMap.put(newExpression, sourceInfoMap.get(oldExpression));
      }
  
      updatedInfoMap.put(new Range(0, entireProgram.getEnd() + shift), 
          sourceInfoMap.get(entireProgram));
      
      sourceInfoMap = updatedInfoMap;
    }
  }

  /**
   * Close last block if it never filled.
   */
  private void optionallyCloseLastBlock(StringBuilder newJs, ArrayList<Integer> ends) {
    if (doSplits && currentStatementCount > 0 && currentStatementCount < MAX_BLOCK_SIZE) {
      newJs.append("}");
      ends.add(newJs.length());
      statementsAddedBlockClose.add(Integer.valueOf(ends.size() - 1));
    }
  }
  
}