/*
 * Copyright 2010 Google Inc.
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

import java.util.ArrayList;
import java.util.List;

/**
 * Stores statistics on the results of running an optimizer pass.
 */
public class OptimizerStats {
  private final List<OptimizerStats> children = new ArrayList<OptimizerStats>();
  private final String name;
  private int numMods = 0;
  private int numVisits = 0;

  public OptimizerStats(String name) {
    this.name = name;
  }

  /**
   * Add a child stats object.
   */
  public void add(OptimizerStats childStats) {
    children.add(childStats);
  }

  /**
   * @return <code>true</code> if the AST changed during this pass.
   */
  public boolean didChange() {
    if (numMods > 0) {
      return true;
    }
    for (OptimizerStats child : children) {
      if (child.didChange()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Retrieves an immutable list of child stats objects. Don't modify this list.
   */
  public List<OptimizerStats> getChildren() {
    return children;
  }

  public String getName() {
    return name;
  }

  /**
   * @return the number of times the AST was modified by the optimizer
   */
  public int getNumMods() {
    int childMods = 0;
    for (OptimizerStats child : children) {
      childMods += child.getNumMods();
    }
    return numMods + childMods;
  }

  /**
   * @return the number of nodes visited by the optimizer
   */
  public int getNumVisits() {
    int childVisits = 0;
    for (OptimizerStats child : children) {
      childVisits += child.getNumVisits();
    }
    return numVisits + childVisits;
  }

  /**
   * Return a human-readable string representing the values of all statistics.
   */
  public String prettyPrint() {
    StringBuilder builder = new StringBuilder();
    prettyPrint(builder, 0);
    return builder.toString();
  }

  /**
   * Increment the number of times the tree was modified.
   */
  public OptimizerStats recordModified() {
    this.numMods++;
    return this;
  }

  /**
   * Increment the number of times the tree was modified.
   *
   * @param numMods the number of changes made to the AST.
   */
  public OptimizerStats recordModified(int numMods) {
    this.numMods += numMods;
    return this;
  }

  /**
   * Increment the number of times tree nodes were visited.
   */
  public OptimizerStats recordVisit() {
    this.numVisits++;
    return this;
  }

  /**
   * Increment the number of times tree nodes were visited.
   */
  public OptimizerStats recordVisits(int numVisits) {
    this.numVisits += numVisits;
    return this;
  }

  private void prettyPrint(StringBuilder builder, int level) {
    int visits = getNumVisits();
    int mods = getNumMods();
    String ratioString = " ----";
    if (visits > 0) {
      ratioString = String.format("%5.2f", ((double) mods / (double) visits) * 100.0);
    }
    String entry = String.format("%-6s%% (%6d/%6d)", ratioString, mods, visits);
    builder.append(String.format("%12s: %-22s  ", name, entry));

    if (children.size() > 0) {
      builder.append("\n      ");
      for (int i = 0; i <= level; i++) {
        builder.append("  ");
      }
      for (OptimizerStats child : children) {
        child.prettyPrint(builder, ++level);
      }
    }
  }
}
