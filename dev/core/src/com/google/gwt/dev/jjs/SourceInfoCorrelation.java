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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Tracks file and line information for AST nodes.
 * 
 * TODO: make this package-protected?
 */
public class SourceInfoCorrelation implements SourceInfo, Serializable {

  /**
   * Micro-opt for {@link #makeChild(Class, String)}.
   */
  private static final SourceInfo[] EMPTY_SOURCEINFO_ARRAY = new SourceInfo[0];
  
  private static final int numCorrelationAxes = Axis.values().length;

  private static int numCorrelationAxes() {
    return numCorrelationAxes;
  }

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
   * Each index of this array corresponds to the Correlation.Axis with the same
   * ordinal().
   */
  private final Correlation[] primaryCorrelations;

  public SourceInfoCorrelation(SourceOrigin origin) {
    this.origin = origin;
    allCorrelations = new ArrayList<Correlation>();
    primaryCorrelations = new Correlation[numCorrelationAxes()];
  }

  private SourceInfoCorrelation(SourceInfoCorrelation parent, String caller,
      SourceInfo... additionalAncestors) {
    assert parent != null;
    assert caller != null;
    this.origin = parent.origin;

    this.allCorrelations = new ArrayList<Correlation>(parent.allCorrelations);
    primaryCorrelations = new Correlation[numCorrelationAxes()];
    for (int i = 0; i < numCorrelationAxes(); i++) {
      primaryCorrelations[i] = parent.primaryCorrelations[i];
    }

    merge(additionalAncestors);
  }

  /**
   * Add a Correlation to the SourceInfo.
   */
  public void addCorrelation(Correlation c) {
    if (!isAlreadyInAllCorrelations(c)) {
      allCorrelations.add(c);
    }

    int index = c.getAxis().ordinal();
    if (primaryCorrelations[index] == null) {
      primaryCorrelations[index] = c;
    }
  }

  /**
   * Copy any Correlations from another SourceInfo node if there are no
   * Correlations on this SourceInfo with the same Axis.
   */
  public void copyMissingCorrelationsFrom(SourceInfo other) {
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
    return allCorrelations;
  }

  /**
   * Returns all Correlations along a given axis applied to this SourceInfo, its
   * parent, additional ancestor SourceInfo, and any supertype SourceInfos.
   */
  public List<Correlation> getAllCorrelations(Axis axis) {
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
    return primaryCorrelations[axis.ordinal()];
  }

  /**
   * Returns the first Correlations added along each Axis on which a Correlation
   * has been set.
   */
  public Set<Correlation> getPrimaryCorrelations() {
    HashSet<Correlation> toReturn = new HashSet<Correlation>();
    for (Correlation c : primaryCorrelations) {
      if (c != null) {
        toReturn.add(c);
      }
    }
    return toReturn;
  }
  
  public Correlation[] getPrimaryCorrelationsArray() {
    return primaryCorrelations;
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
    return makeChild(caller, description, EMPTY_SOURCEINFO_ARRAY);
  }

  /**
   * If data accumulation is enabled, create a derived SourceInfo object that
   * indicates that one or more AST nodes were merged to create a new node. The
   * derived node will inherit its Origin and Correlations from the SourceInfo
   * object on which the method is invoked.
   */
  public SourceInfoCorrelation makeChild(Class<?> caller, String description,
      SourceInfo... merge) {
    String callerName = caller == null ? "Unrecorded caller" : caller.getName();
    return new SourceInfoCorrelation(this, callerName, merge);
  }

  /**
   * Add additional ancestor SourceInfos. These SourceInfo objects indicate that
   * a merge-type operation took place or that the additional ancestors have a
   * containment relationship with the SourceInfo.
   */
  public void merge(SourceInfo... sourceInfos) {
    for (SourceInfo info : sourceInfos) {
      if (this == info) {
        continue;
      }

      for (Correlation c : info.getAllCorrelations()) {
        if (!isAlreadyInAllCorrelations(c)) {
          allCorrelations.add(c);
        }
      }

      for (Correlation c : info.getPrimaryCorrelations()) {
        int i = c.getAxis().ordinal();
        if (primaryCorrelations[i] == null) {
          primaryCorrelations[i] = c;
        }
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
