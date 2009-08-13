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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * Re-orders function declarations according to a given metric and clustering
 * algorithm in order to boost gzip/deflation compression efficiency. This
 * version uses the edit-distance algorithm as a metric, and a semi-greedy
 * strategy for grouping functions together.
 */
public class JsFunctionClusterer extends JsAbstractTextTransformer {

  /**
   * Limit edit-distance search to MAX_DIST.
   */
  private static final int MAX_DIST = 10;

  private static final int MAX_DISTANCE_LIMIT = 100;

  private List<Integer> functionIndices;

  private int[] clusteredIndices;

  public JsFunctionClusterer(String js, StatementRanges statementRanges) {
    super(js, statementRanges);
  }

  public JsFunctionClusterer(JsAbstractTextTransformer xformer) {
    super(xformer);
  }

  public void exec() {
    functionIndices = new LinkedList<Integer>();

    // gather up all of the indices of function decl statements
    for (int i = 0; i < statementRanges.numStatements(); i++) {
      String code = getJsForRange(i);
      if (code.startsWith("function")) {
        functionIndices.add(i);
      }
    }

    // sort the indices according to size of statement range
    Collections.sort(functionIndices, new Comparator<Integer>() {
      public int compare(Integer index1, Integer index2) {
        return stmtSize(index1) - (stmtSize(index2));
      }
    });

    // used to hold the new output order
    clusteredIndices = new int[functionIndices.size()];
    int currentFunction = 0;

    // remove the first function and stick it in the output array
    clusteredIndices[currentFunction] = functionIndices.get(0);
    functionIndices.remove(0);

    while (!functionIndices.isEmpty()) {

      // get the last outputted function to match against
      String currentCode = getJsForRange(clusteredIndices[currentFunction]);
      int bestDistance = 99999;
      int bestIndex = 0;
      int bestFunction = 0;

      Iterator<Integer> it = functionIndices.iterator();
      int count = 0;
      // search up to MAX_DIST functions for the best match
      while (it.hasNext() &&
             count < Math.min(MAX_DIST, functionIndices.size())) {
        int functionIndex = it.next();
        String testCode = getJsForRange(functionIndex);
        int distanceLimit = Math.min(bestDistance, MAX_DISTANCE_LIMIT);
        int dist = levdist(currentCode, testCode, distanceLimit);
        if (dist < bestDistance) {
          bestDistance = dist;
          bestIndex = count;
          bestFunction = functionIndex;
        }
        count++;
      }
      // output the best match and remove it from worklist of functions
      currentFunction++;
      clusteredIndices[currentFunction] = bestFunction;
      functionIndices.remove(bestIndex);
    }

    recomputeJsAndStatementRanges(clusteredIndices);
  }

  @Override
  protected void endStatements(StringBuilder newJs, ArrayList<Integer> starts,
      ArrayList<Integer> ends) {
    // Then output everything else that is not a function.
    for (int i = 0; i < statementRanges.numStatements(); i++) {
      String code = getJsForRange(i);
      if (!code.startsWith("function")) {
        addStatement(code, newJs, starts, ends);
      }
    }
    super.endStatements(newJs, starts, ends);
  }

  /**
   * Compute the Levenshtein distance between two strings, up to a distance
   * limit.
   */
  private int levdist(String str1, String str2, int limit) {
    if (str1.length() > str2.length()) {
      return levdist(str2, str1, limit);
    }
    if (str1.length() == 0) {
      return str2.length();
    }
    if (Math.abs(str1.length() - str2.length()) >= limit) {
      return limit;
    }

    int str1len = str1.length();
    int str2len = str2.length();

    int lastRow[] = new int[str2len + 1];
    int nextRow[] = new int[str2len + 1];

    for (int j = 0; j <= Math.min(str2len, limit + 1); j++) {
      lastRow[j] = j;
    }

    for (int i = 1; i <= str1len; i++) {
      nextRow[0] = i;

      if (i >= limit) {
        nextRow[i - limit] = limit;
      }
      if (i >= limit + 1) {
        nextRow[i - limit - 1] = limit;
      }
      if (i + limit <= str2len) {
        nextRow[i + limit] = limit;
      }
      if (i + limit + 1 <= str2len) {
        nextRow[i + limit + 1] = limit;
      }

      char c1 = str1.charAt(i - 1);

      int j = Math.max(1, (i - limit + 1));
      int jmax = Math.min(str2len, (i + limit - 1));

      while (j <= jmax) {
        char c2 = str2.charAt(j - 1);
        int costSwap = c1 == c2 ? 0 : 1;
        nextRow[j] = Math.min(Math.min(lastRow[j] + 1, nextRow[j - 1] + 1),
            lastRow[j - 1] + costSwap);
        j = j + 1;
      }
      int tmpRow[] = nextRow;
      nextRow = lastRow;
      lastRow = tmpRow;
    }
    return lastRow[Math.min(str2len, limit)];
  }

  private int stmtSize(int index1) {
    return statementRanges.end(index1) - statementRanges.start(index1);
  }
}
