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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.linker.StatementRanges;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * The standard implementation of {@link StatementRanges}.
 */
public class StandardStatementRanges implements StatementRanges, Serializable {
  private static int[] toArray(ArrayList<Integer> list) {
    int[] ary = new int[list.size()];
    for (int i = 0; i < list.size(); i++) {
      ary[i] = list.get(i);
    }
    return ary;
  }

  private final int[] ends;
  private final int[] starts;

  public StandardStatementRanges(ArrayList<Integer> starts, ArrayList<Integer> ends) {
    assert starts.size() == ends.size();
    this.starts = toArray(starts);
    this.ends = toArray(ends);
  }

  public int end(int i) {
    return ends[i];
  }

  public int numStatements() {
    return starts.length;
  }

  public int start(int i) {
    return starts[i];
  }
}
