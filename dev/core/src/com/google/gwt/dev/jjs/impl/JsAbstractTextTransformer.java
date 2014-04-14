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
import com.google.gwt.core.ext.soyc.Range;
import com.google.gwt.dev.jjs.SourceInfo;

import java.util.ArrayList;
import java.util.Map;

/**
 * Base class for transforming program text.
 */
public abstract class JsAbstractTextTransformer {

  protected String js;

  protected StatementRanges originalStatementRanges;

  protected StatementRanges statementRanges;

  protected Map<Range, SourceInfo> sourceInfoMap;

  public JsAbstractTextTransformer(JsAbstractTextTransformer xformer) {
    this(xformer.getJs(), xformer.getStatementRanges(), xformer.getSourceInfoMap());
  }

  public JsAbstractTextTransformer(String js, StatementRanges statementRanges,
      Map<Range, SourceInfo> sourceInfoMap) {
    this.js = js;
    this.statementRanges = statementRanges;
    this.originalStatementRanges = statementRanges;
    this.sourceInfoMap = sourceInfoMap;
  }

  public abstract void exec();

  public String getJs() {
    return js;
  }

  public Map<Range, SourceInfo> getSourceInfoMap() {
    return sourceInfoMap;
  }

  public StatementRanges getStatementRanges() {
    return statementRanges;
  }

  protected void addStatement(int index, String code, StringBuilder newJs,
      ArrayList<Integer> starts, ArrayList<Integer> ends) {
    beginStatement(index, newJs, starts);
    newJs.append(code);
    endStatement(index, newJs, ends);
  }

  protected void beginStatement(int index, StringBuilder newJs, ArrayList<Integer> starts) {
    starts.add(newJs.length());
  }

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

  protected void endStatement(int index, StringBuilder newJs, ArrayList<Integer> ends) {
    ends.add(newJs.length());
  }

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
    return js.substring(statementRanges.start(stmtIndex), statementRanges.end(stmtIndex));
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
    for (int stmtIndex : stmtIndices) {
      String code = getJsForRange(stmtIndex);
      addStatement(stmtIndex, code, newJs, starts, ends);
    }
    endStatements(newJs, starts, ends);

    assert starts.size() == ends.size() : "Size mismatch between start and"
        + " end statement ranges.";
    assert starts.get(0) == 0 && ends.get(ends.size() - 1) == newJs.length() :
        "statement ranges don't cover entire JS output string.";

    js = newJs.toString();
    statementRanges = new StandardStatementRanges(starts, ends);
    updateSourceInfoMap();
  }

  /**
   * Update the expression ranges in the SourceInfo map after the
   * transformer has manipulated the statements.
   */
  protected abstract void updateSourceInfoMap();

}
