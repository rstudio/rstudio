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
package com.google.gwt.dev.js;

import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsCase;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsDefault;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVisitor;

import java.util.List;
import java.util.ListIterator;

/**
 * Some versions of IE7 have a limit on the number of statements that can appear
 * within a JsBlock. This visitor will restructure blocks and other block-like
 * structures with too many statements in order to reduce the total number of
 * statements that appear within any given block to fewer than
 * {@value #MAX_BLOCK_SIZE} statements by creating nested blocks:
 * 
 * <pre>
 * {
 *   { statements }
 *   { statements }
 *   remainder of statements
 * }
 * </pre>
 * 
 * This change is purely structural, it will not affect code flow.
 */
public class JsIEBlockSizeVisitor {

  /**
   * Visits every block-like structure.
   */
  private static class BlockVisitor extends JsVisitor {

    private final JsProgram program;

    public BlockVisitor(JsProgram program) {
      this.program = program;
    }

    @Override
    public void endVisit(JsBlock x, JsContext ctx) {
      // JsFunctionClusterer handles restructuring top-level statement blocks
      if (!x.isGlobalBlock()) {
        restructure(x.getStatements());
      }
    }

    @Override
    public void endVisit(JsCase x, JsContext ctx) {
      restructure(x.getStmts());
    }

    @Override
    public void endVisit(JsDefault x, JsContext ctx) {
      restructure(x.getStmts());
    }

    /**
     * Perform the restructuring on a list of statements. Blocks are created as
     * necessary to prevent any given block from exceeding the maximum size.
     */
    private void restructure(List<JsStatement> statements) {
      SourceInfo sourceInfo = program.createSourceInfoSynthetic(JsIEBlockSizeVisitor.class);
      // This outer loop will collapse the newly-created block into super-blocks
      while (statements.size() > MAX_BLOCK_SIZE) {
        ListIterator<JsStatement> i = statements.listIterator();
        List<JsStatement> statementsInNewBlock = null;

        // This loop represents a single fold over the list of statements
        while (statements.size() > MAX_BLOCK_SIZE && i.hasNext()) {
          JsStatement current = i.next();

          if (statementsInNewBlock == null) {
            // Replace the current statement with a new block
            JsBlock newBlock = new JsBlock(sourceInfo);
            statementsInNewBlock = newBlock.getStatements();
            i.set(newBlock);
          } else {
            /*
             * There's an open replacement block, remove the statement from its
             * current block.
             */
            i.remove();
          }

          // Move the statement into the new block
          statementsInNewBlock.add(current);

          /*
           * If we hit the cap on a new block, discard the reference to create a
           * new block for the next statement that we see.
           */
          if (statementsInNewBlock.size() == MAX_BLOCK_SIZE) {
            statementsInNewBlock = null;
          }
        }
      }
      assert statements.size() <= MAX_BLOCK_SIZE;
    }
  }

  // Use this value instead to test the effects of the visitor
  // private static final int MAX_BLOCK_SIZE = 1 << 5;

  private static final int MAX_BLOCK_SIZE = 1 << 15 - 1;

  /**
   * Entry point.
   */
  public static void exec(JsProgram program) {
    (new BlockVisitor(program)).accept(program);
  }
}
