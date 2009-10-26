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
import com.google.gwt.core.ext.linker.impl.StandardStatementRanges;

import java.util.ArrayList;

/**
 * Base class for transforming program text.
 */
public abstract class JsAbstractTextTransformer {

  protected String js;

  protected StatementRanges statementRanges;

  public JsAbstractTextTransformer(String js, StatementRanges statementRanges) {
    this.js = js;
    this.statementRanges = statementRanges;
  }

  public JsAbstractTextTransformer(JsAbstractTextTransformer xformer) {
    this.js = xformer.getJs();
    this.statementRanges = xformer.getStatementRanges();
  }

  public abstract void exec();

  public String getJs() {
    return js;
  }

  public StatementRanges getStatementRanges() {
    return statementRanges;
  }

  protected void addStatement(String code, StringBuilder newJs,
      ArrayList<Integer> starts, ArrayList<Integer> ends) {
    beginStatement(newJs, starts);
    newJs.append(code);
    endStatement(newJs, ends);
  }

  protected void beginStatement(StringBuilder newJs,
      ArrayList<Integer> starts) {
    starts.add(newJs.length());
  }

  // FIXME document parameters
  /**
   * Called if any operations need to be performed before all statements have
   * been processed.
   * 
   * @param newJs
   * @param starts
   * @param ends
   */
  protected void beginStatements(StringBuilder newJs, ArrayList<Integer> starts,
      ArrayList<Integer> ends) {
  }

  // FIXME document
  /**
   * @param newJs
   * @param ends
   */
  protected void endStatement(StringBuilder newJs, ArrayList<Integer> ends) {
    ends.add(newJs.length());
  }

  // FIXME document parameters
  /**
   * Called if any operations need to be performed after all statements have
   * been processed.
   *
   * @param newJs
   * @param starts
   * @param ends
   */
  protected void endStatements(StringBuilder newJs, ArrayList<Integer> starts,
      ArrayList<Integer> ends) {
  }

  protected String getJsForRange(int stmtIndex) {
    return js.substring(statementRanges.start(stmtIndex),
        statementRanges.end(stmtIndex));
  }

  /**
   * Dump functions and fragments back into a new JS string, and calculate a new
   * StatementRanges object.
   */
  protected void recomputeJsAndStatementRanges(int[] stmtIndices) {

    StringBuilder newJs = new StringBuilder();
    ArrayList<Integer> starts = new ArrayList<Integer>();
    ArrayList<Integer> ends = new ArrayList<Integer>();

    beginStatements(newJs, starts, ends);
    for (int i = 0; i < stmtIndices.length; i++) {
      String code = getJsForRange(stmtIndices[i]);
      addStatement(code, newJs, starts, ends);
    }
    endStatements(newJs, starts, ends);

    assert
        starts.size() == ends.size() :
        "Size mismatch between start and" + " end statement ranges.";
    assert starts.get(0) == 0 && ends.get(ends.size() - 1) ==
        newJs.length() :
        "statement ranges don't cover entire JS output string.";

    StandardStatementRanges newRanges = new StandardStatementRanges(starts,
        ends);
    js = newJs.toString();
    statementRanges = newRanges;
  }
}
