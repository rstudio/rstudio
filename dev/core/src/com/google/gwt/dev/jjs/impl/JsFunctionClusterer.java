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
import com.google.gwt.dev.util.editdistance.GeneralEditDistance;
import com.google.gwt.dev.util.editdistance.GeneralEditDistances;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.regex.Pattern;

/**
 * Re-orders function declarations according to a given metric and clustering
 * algorithm in order to boost gzip/deflation compression efficiency. This
 * version uses the edit-distance algorithm as a metric, and a semi-greedy
 * strategy for grouping functions together.
 */
public class JsFunctionClusterer extends JsAbstractTextTransformer {

  /**
   * Used by isFunctionDeclaration to check a statement is a function
   * declaration or not. This should match standard declarations,
   * such as "function a() { ... }" and "jslink.a=function() { ... }".
   * The latter form is typically emitted by the cross-site linker.
   */
  private static final Pattern functionDeclarationPattern =
      Pattern.compile("function |[a-zA-Z][.$_a-zA-Z0-9]*=function");

  /**
   * Functions which have an edit-distance greater than this limit are
   * considered equally different.
   */
  private static final int MAX_DISTANCE_LIMIT = 100;

  /**
   * Maximum number of functions to search for minimal edit-distance
   * before giving up.
   */
  private static final int SEARCH_LIMIT = 10;

  /**
   * Tells whether a statement is a function declaration or not.
   */
  private static boolean isFunctionDeclaration(String code) {
    return functionDeclarationPattern.matcher(code).lookingAt();
  }

  public JsFunctionClusterer(JsAbstractTextTransformer xformer) {
    super(xformer);
  }

  public JsFunctionClusterer(String js, StatementRanges statementRanges) {
    super(js, statementRanges);
  }

  @Override
  public void exec() {
    LinkedList<Integer> functionIndices = new LinkedList<Integer>();

    // gather up all of the indices of function decl statements
    for (int i = 0; i < statementRanges.numStatements(); i++) {
      String code = getJsForRange(i);
      if (isFunctionDeclaration(code)) {
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
    int[] clusteredIndices = new int[functionIndices.size()];
    int currentFunction = 0;

    // remove the first function and stick it in the output array
    clusteredIndices[currentFunction] = functionIndices.get(0);
    functionIndices.remove(0);
    while (!functionIndices.isEmpty()) {
      // get the last outputted function to match against
      String currentCode = getJsForRange(clusteredIndices[currentFunction]);
      final GeneralEditDistance editDistance =
        GeneralEditDistances.getLevenshteinDistance(currentCode);

      int bestIndex = 0;
      int bestFunction = functionIndices.getFirst();
      int bestDistance = MAX_DISTANCE_LIMIT;

      int count = 0;
      for (int functionIndex : functionIndices) {
        if (count >= SEARCH_LIMIT) {
          break;
        }
        String testCode = getJsForRange(functionIndex);
        int dist = editDistance.getDistance(testCode, bestDistance);
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
      if (!isFunctionDeclaration(code)) {
        addStatement(code, newJs, starts, ends);
      }
    }
    super.endStatements(newJs, starts, ends);
  }

  private int stmtSize(int index1) {
    return statementRanges.end(index1) - statementRanges.start(index1);
  }
}
