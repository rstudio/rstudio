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
package com.google.gwt.dev.jjs;

import com.google.gwt.dev.jjs.Correlation.Axis;
import com.google.gwt.dev.jjs.CorrelationFactory.RealCorrelationFactory;

/**
 * Tracks file and line information for AST nodes.
 *
 * TODO: make this package-protected?
 */
public class SourceInfoCorrelation implements SourceInfo {

  private static final int NUM_AXES = Axis.values().length;

  /**
   * Holds the origin data for the SourceInfo.
   */
  private final SourceOrigin origin;

  /**
   * My parent node, or <code>null</code> if I have no parent.
   */
  private final SourceInfoCorrelation parent;

  /**
   * Records the first Correlation on any given Axis applied to the SourceInfo.
   * Each index of this array corresponds to the Correlation.Axis with the same
   * ordinal().
   */
  private Correlation[] primaryCorrelations = null;

  public SourceInfoCorrelation(SourceOrigin origin) {
    this.origin = origin;
    this.parent = null;
  }

  public SourceInfoCorrelation(SourceInfoCorrelation parent, SourceOrigin origin) {
    this.origin = origin;
    this.parent = parent;
  }

  /**
   * Add a Correlation to the SourceInfo.
   */
  @Override
  public void addCorrelation(Correlation c) {
    if (primaryCorrelations == null) {
      primaryCorrelations = new Correlation[NUM_AXES];
    }
    int index = c.getAxis().ordinal();
    primaryCorrelations[index] = c;
  }

  @Override
  public Correlation getCorrelation(Axis axis) {
    if (primaryCorrelations != null) {
      Correlation c = primaryCorrelations[axis.ordinal()];
      if (c != null) {
        return c;
      }
    }
    if (parent != null) {
      return parent.getCorrelation(axis);
    }
    return null;
  }

  @Override
  public Correlation[] getCorrelations() {
    if (parent == null) {
      if (primaryCorrelations == null) {
        return new Correlation[NUM_AXES];
      } else {
        return primaryCorrelations.clone();
      }
    } else {
      Correlation[] result = parent.getCorrelations();
      if (primaryCorrelations != null) {
        for (int i = 0; i < NUM_AXES; ++i) {
          Correlation c = primaryCorrelations[i];
          if (c != null) {
            result[i] = c;
          }
        }
      }
      return result;
    }
  }

  @Override
  public CorrelationFactory getCorrelator() {
    return RealCorrelationFactory.INSTANCE;
  }

  @Override
  public int getEndPos() {
    return getOrigin().getEndPos();
  }

  @Override
  public String getFileName() {
    return getOrigin().getFileName();
  }

  @Override
  public SourceOrigin getOrigin() {
    return origin;
  }

  /**
   * Usually (always?) there is only one primary correlation. It is the link to the original kind
   * of node. Getting this value will provide a way to name each source info (node) in the source
   * map.
   *
   * @return the original correlation added when created
   */
  public Correlation getPrimaryCorrelation() {
    if (primaryCorrelations == null) {
      if (parent != null) {
        return this.parent.getPrimaryCorrelation();
      } else {
        return null;
      }
    }
    if (primaryCorrelations[Axis.LITERAL.ordinal()] != null) {
      if (primaryCorrelations[Axis.LITERAL.ordinal()].getIdent().equals("class") &&
          parent != null) {
        return this.parent.getPrimaryCorrelation();
      } else {
        return null;
      }
    }
    int[] order = new int[]{
        Axis.FIELD.ordinal(),
        Axis.METHOD.ordinal(),
        Axis.CLASS.ordinal()};
    for (int idx : order) {
      if (primaryCorrelations[idx] != null) {
        return primaryCorrelations[idx];
      }
    }
    return null;
  }

  @Override
  public int getStartLine() {
    return getOrigin().getStartLine();
  }

  @Override
  public int getStartPos() {
    return getOrigin().getStartPos();
  }

  @Override
  public SourceInfo makeChild() {
    return new SourceInfoCorrelation(this, this.origin);
  }

  @Override
  public SourceInfo makeChild(SourceOrigin origin) {
    return new SourceInfoCorrelation(this, origin);
  }

  @Override
  public String toString() {
    return origin.toString();
  }
}
