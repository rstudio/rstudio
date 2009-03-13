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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Tracks file and line information for AST nodes.
 */
public class SourceInfo implements Serializable {

  /**
   * A totally-immutable version of SourceInfo.
   */
  protected static class Immutable extends SourceInfo {
    public Immutable(int startPos, int endPos, int startLine, String fileName,
        boolean createDescendants) {
      super(startPos, endPos, startLine, fileName, createDescendants);
    }

    @Override
    public void addCorrelation(Correlation c) {
      throw new UnsupportedOperationException(
          "May not add correlations to the " + getFileName()
              + "SourceInfo. Call makeChild() first.");
    }

    @Override
    public void copyMissingCorrelationsFrom(SourceInfo other) {
      throw new UnsupportedOperationException(
          "May not copy correlations into the " + getFileName()
              + "SourceInfo. Call makeChild() first.");
    }

    @Override
    public void merge(SourceInfo... sourceInfos) {
      if (sourceInfos.length > 0) {
        throw new UnsupportedOperationException(
            "May not merge SourceInfos into the " + getFileName()
                + " SourceInfo. Call makeChild() first.");
      }
    }
  }

  /**
   * Indicates that the source for an AST element is unknown. This indicates a
   * deficiency in the compiler.
   */
  public static final SourceInfo UNKNOWN = new Immutable(0, 0, 0,
      "Unknown source", true);

  /**
   * Micro-opt for {@link #makeChild(Class, String)}.
   */
  private static final SourceInfo[] EMPTY_SOURCEINFO_ARRAY = new SourceInfo[0];

  /**
   * This flag controls the behavior of the mutable methods to make them no-ops.
   */
  private final boolean accumulateData;

  /**
   * Any Correlation associated with the SourceInfo.
   */
  private final List<Correlation> allCorrelations;

  /**
   * Holds the origin data for the SourceInfo.
   */
  private final SourceOrigin origin;

  /**
   * Records the first Correlation on any given Axis applied to the SourceInfo.
   */
  private final EnumMap<Axis, Correlation> primaryCorrelations;

  protected SourceInfo(int startPos, int endPos, int startLine,
      String fileName, boolean accumulateData) {
    assert fileName != null;

    this.accumulateData = accumulateData;
    origin = SourceOrigin.create(startPos, endPos, startLine, fileName);

    // Be very aggressive in not allocating collections that we don't need.
    if (accumulateData) {
      allCorrelations = new ArrayList<Correlation>();
      primaryCorrelations = new EnumMap<Axis, Correlation>(Axis.class);
    } else {
      allCorrelations = null;
      primaryCorrelations = null;
    }
  }

  private SourceInfo(SourceInfo parent, String mutation, String caller,
      SourceInfo... additionalAncestors) {
    assert parent != null;
    assert caller != null;
    this.accumulateData = parent.accumulateData;
    this.origin = parent.origin;
    if (accumulateData) {
      this.allCorrelations = new ArrayList<Correlation>(parent.allCorrelations);
      this.primaryCorrelations = new EnumMap<Axis, Correlation>(
          parent.primaryCorrelations);
    } else {
      allCorrelations = null;
      primaryCorrelations = null;
    }
    merge(additionalAncestors);
  }

  /**
   * Add a Correlation to the SourceInfo.
   */
  public void addCorrelation(Correlation c) {
    if (!accumulateData) {
      return;
    }
    if (!isAlreadyInAllCorrelations(c)) {
      allCorrelations.add(c);
    }
    if (!primaryCorrelations.containsKey(c.getAxis())) {
      primaryCorrelations.put(c.getAxis(), c);
    }
  }

  /**
   * Copy any Correlations from another SourceInfo node if there are no
   * Correlations on this SourceInfo with the same Axis.
   */
  public void copyMissingCorrelationsFrom(SourceInfo other) {
    if (!accumulateData) {
      return;
    }

    EnumSet<Axis> toAdd = EnumSet.allOf(Axis.class);
    for (Correlation c : allCorrelations) {
      toAdd.remove(c.getAxis());
    }

    for (Correlation c : other.getAllCorrelations()) {
      if (toAdd.contains(c.getAxis())) {
        addCorrelation(c);
      }
    }
  }

  /**
   * Returns all Correlations applied to this SourceInfo, its parent, additional
   * ancestor SourceInfo, and any supertype SourceInfos.
   */
  public List<Correlation> getAllCorrelations() {
    if (accumulateData) {
      return allCorrelations;
    } else {
      return Collections.<Correlation> emptyList();
    }
  }

  /**
   * Returns all Correlations along a given axis applied to this SourceInfo, its
   * parent, additional ancestor SourceInfo, and any supertype SourceInfos.
   */
  public List<Correlation> getAllCorrelations(Axis axis) {
    if (!accumulateData) {
      return Collections.emptyList();
    }
    List<Correlation> toReturn = new ArrayList<Correlation>();
    for (Correlation c : getAllCorrelations()) {
      if (c.getAxis() == axis) {
        toReturn.add(c);
      }
    }
    return toReturn;
  }

  public int getEndPos() {
    return getOrigin().getEndPos();
  }

  public String getFileName() {
    return getOrigin().getFileName();
  }

  public SourceOrigin getOrigin() {
    return origin;
  }

  /**
   * Returns the first Correlation that had been set with a given Axis, or
   * <code>null</code> if no Correlation has been set on the given axis.
   */
  public Correlation getPrimaryCorrelation(Axis axis) {
    return accumulateData ? primaryCorrelations.get(axis) : null;
  }

  /**
   * Returns the first Correlations added along each Axis on which a Correlation
   * has been set.
   */
  public Set<Correlation> getPrimaryCorrelations() {
    return accumulateData ? new HashSet<Correlation>(
        primaryCorrelations.values()) : Collections.<Correlation> emptySet();
  }

  public int getStartLine() {
    return getOrigin().getStartLine();
  }

  public int getStartPos() {
    return getOrigin().getStartPos();
  }

  /**
   * If data accumulation is enabled, create a derived SourceInfo object that
   * indicates that one or more AST nodes were merged to create a new node. The
   * derived node will inherit its Origin and Correlations from the SourceInfo
   * object on which the method is invoked.
   */
  public SourceInfo makeChild(Class<?> caller, String description) {
    return accumulateData ? makeChild(caller, description,
        EMPTY_SOURCEINFO_ARRAY) : this;
  }

  /**
   * If data accumulation is enabled, create a derived SourceInfo object that
   * indicates that one or more AST nodes were merged to create a new node. The
   * derived node will inherit its Origin and Correlations from the SourceInfo
   * object on which the method is invoked.
   */
  public SourceInfo makeChild(Class<?> caller, String description,
      SourceInfo... merge) {
    if (!accumulateData) {
      return this;
    }

    String callerName = caller == null ? "Unrecorded caller" : caller.getName();
    return new SourceInfo(this, description, callerName, merge);
  }

  /**
   * Add additional ancestor SourceInfos. These SourceInfo objects indicate that
   * a merge-type operation took place or that the additional ancestors have a
   * containment relationship with the SourceInfo.
   */
  public void merge(SourceInfo... sourceInfos) {
    if (!accumulateData) {
      return;
    }

    for (SourceInfo info : sourceInfos) {
      if (this == info || !info.accumulateData) {
        continue;
      }

      for (Correlation c : info.getAllCorrelations()) {
        if (!isAlreadyInAllCorrelations(c)) {
          allCorrelations.add(c);
        }
      }

      if (primaryCorrelations.size() < Axis.values().length) {
        EnumMap<Axis, Correlation> copy = new EnumMap<Axis, Correlation>(
            info.primaryCorrelations);
        copy.keySet().removeAll(primaryCorrelations.keySet());
        primaryCorrelations.putAll(copy);
      }
    }
  }

  @Override
  public String toString() {
    return origin.toString();
  }

  private boolean isAlreadyInAllCorrelations(Correlation c) {
    // make sure this correlations is not yet in the allCorrelations list
    boolean alreadyThere = false;
    Iterator<Correlation> it = allCorrelations.iterator();
    while ((alreadyThere == false) && (it.hasNext())) {
      if (it.next().equals(c)) {
        alreadyThere = true;
      }
    }
    return alreadyThere;
  }
}
