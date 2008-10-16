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
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
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
    public void addAdditonalAncestors(SourceInfo... sourceInfos) {
      throw new UnsupportedOperationException(
          "May not add additional ancestors to the " + getFileName()
              + " SourceInfo");
    }

    @Override
    public void addCorrelation(Correlation c) {
      throw new UnsupportedOperationException(
          "May not add correlations to the " + getFileName() + "SourceInfo");
    }

    @Override
    public void addSupertypeAncestors(SourceInfo... sourceInfos) {
      throw new UnsupportedOperationException(
          "May not add supertype ancestors to the " + getFileName()
              + " SourceInfo");
    }
  }

  /**
   * Compares SourceInfos by their file and position information.
   */
  public static final Comparator<SourceInfo> LOCATION_COMPARATOR = new Comparator<SourceInfo>() {
    public int compare(SourceInfo q, SourceInfo b) {
      int a = q.getFileName().compareTo(b.getFileName());
      if (a != 0) {
        return a;
      }

      a = q.startPos - q.startPos;
      if (a != 0) {
        return a;
      }

      a = q.endPos - b.endPos;
      if (a != 0) {
        return a;
      }

      a = q.startLine - b.startLine;
      if (a != 0) {
        return a;
      }

      return 0;
    }
  };

  /**
   * Indicates that the source for an AST element is unknown. This indicates a
   * deficiency in the compiler.
   */
  public static final SourceInfo UNKNOWN = new Immutable(0, 0, 0,
      "Unknown source", true);

  private static final SourceInfo[] EMPTY_SOURCEINFO_ARRAY = new SourceInfo[0];
  private final Set<SourceInfo> additionalAncestors = new HashSet<SourceInfo>();
  private final String caller;
  private final EnumMap<Axis, Correlation> correlations = new EnumMap<Axis, Correlation>(
      Axis.class);
  /**
   * This flag controls the behavior of {@link #makeChild}.
   */
  private final boolean createDescendants;
  private final int endPos;
  private transient Reference<Set<SourceInfo>> lazyRoots;
  private final String mutation;
  private final SourceInfo parent;
  private final int startLine;
  private final int startPos;

  private final Set<SourceInfo> supertypeAncestors = new HashSet<SourceInfo>();

  protected SourceInfo(int startPos, int endPos, int startLine,
      String fileName, boolean createDescendants) {
    assert fileName != null;

    this.createDescendants = createDescendants;
    this.startPos = startPos;
    this.endPos = endPos;
    this.startLine = startLine;
    this.parent = null;
    this.mutation = null;
    this.caller = null;

    // Don't use addCorrelation because of the immutable subclasses
    Correlation file = Correlation.by(fileName);
    correlations.put(file.getAxis(), file);
  }

  private SourceInfo(SourceInfo parent, String mutation, String caller,
      SourceInfo... additionalAncestors) {
    assert parent != null;
    assert mutation != null;
    assert caller != null;

    this.createDescendants = parent.createDescendants;
    this.startPos = parent.startPos;
    this.endPos = parent.endPos;
    this.startLine = parent.startLine;
    this.additionalAncestors.addAll(Arrays.asList(additionalAncestors));
    this.additionalAncestors.addAll(parent.additionalAncestors);
    this.parent = parent;
    this.mutation = mutation;
    this.caller = caller;
  }

  /**
   * Add additional ancestor SourceInfos. These SourceInfo objects indicate that
   * a merge-type operation took place or that the additional ancestors have a
   * containment relationship with the SourceInfo.
   */
  public void addAdditonalAncestors(SourceInfo... sourceInfos) {
    if (!createDescendants) {
      return;
    }

    additionalAncestors.addAll(Arrays.asList(sourceInfos));
    additionalAncestors.remove(this);

    if (lazyRoots != null) {
      lazyRoots.clear();
    }
  }

  /**
   * Add a Correlation to the SourceInfo.
   * 
   * @throws IllegalArgumentException if a Correlation with the same Axis had
   *           been previously added to the SourceInfo. The reason for this is
   *           that a Correlation shouldn't be re-applied to the same SourceInfo
   *           node, if this were done, the caller should have also called
   *           makeChild() first, since something interesting is gong on.
   */
  public void addCorrelation(Correlation c) {
    if (!createDescendants) {
      return;
    }

    Axis axis = c.getAxis();

    if (correlations.containsKey(axis)) {
      throw new IllegalArgumentException("Correlation on axis " + axis
          + " has already been added. Call makeChild() first.");
    }

    correlations.put(axis, c);
  }

  /**
   * Add SourceInfos that indicate the supertype derivation.
   */
  public void addSupertypeAncestors(SourceInfo... sourceInfos) {
    if (!createDescendants) {
      return;
    }

    supertypeAncestors.addAll(Arrays.asList(sourceInfos));
    supertypeAncestors.remove(this);
  }

  /**
   * Returns all Correlations applied to this SourceInfo, its parent, additional
   * ancestor SourceInfo, and any supertype SourceInfos.
   */
  public Set<Correlation> getAllCorrelations() {
    EnumMap<Axis, Set<Correlation>> accumulator = new EnumMap<Axis, Set<Correlation>>(
        Axis.class);
    findCorrelations(accumulator, EnumSet.allOf(Axis.class), false);

    Set<Correlation> toReturn = new HashSet<Correlation>();
    for (Set<Correlation> toAdd : accumulator.values()) {
      toReturn.addAll(toAdd);
    }

    return Collections.unmodifiableSet(toReturn);
  }

  /**
   * Returns all Correlations along a given axis applied to this SourceInfo, its
   * parent, additional ancestor SourceInfo, and any supertype SourceInfos.
   */
  public Set<Correlation> getAllCorrelations(Axis axis) {
    EnumMap<Axis, Set<Correlation>> accumulator = new EnumMap<Axis, Set<Correlation>>(
        Axis.class);
    findCorrelations(accumulator, EnumSet.of(axis), false);
    assert accumulator.size() < 2;
    if (accumulator.size() == 0) {
      return Collections.unmodifiableSet(new HashSet<Correlation>());
    } else {
      assert accumulator.containsKey(axis);
      assert accumulator.get(axis).size() > 0;
      return Collections.unmodifiableSet(accumulator.get(axis));
    }
  }

  public int getEndPos() {
    return endPos;
  }

  public String getFileName() {
    return getPrimaryCorrelation(Axis.FILE).getIdent();
  }

  /**
   * Return the most-derived Correlation along a given Axis or <code>null</code>
   * if no such correlation exists. The search path uses the current SourceInfo,
   * parent chain, and additional ancestors, but not supertype SourceInfos.
   */
  public Correlation getPrimaryCorrelation(Axis axis) {
    EnumMap<Axis, Set<Correlation>> accumulator = new EnumMap<Axis, Set<Correlation>>(
        Axis.class);
    findCorrelations(accumulator, EnumSet.of(axis), true);
    assert accumulator.size() < 2;
    if (accumulator.size() == 0) {
      return null;
    } else {
      assert accumulator.containsKey(axis);
      assert accumulator.get(axis).size() == 1;
      return accumulator.get(axis).iterator().next();
    }
  }

  /**
   * Returns the most-derived Correlations along each Axis on which a
   * Correlation has been set. The search path uses the current SourceInfo,
   * parent chain, and additional ancestors, but not supertype SourceInfos.
   */
  public Set<Correlation> getPrimaryCorrelations() {
    EnumMap<Axis, Set<Correlation>> accumulator = new EnumMap<Axis, Set<Correlation>>(
        Axis.class);
    findCorrelations(accumulator, EnumSet.allOf(Axis.class), true);

    EnumMap<Axis, Correlation> toReturn = new EnumMap<Axis, Correlation>(
        Axis.class);
    for (Map.Entry<Axis, Set<Correlation>> entry : accumulator.entrySet()) {
      assert entry.getValue().size() == 1;
      toReturn.put(entry.getKey(), entry.getValue().iterator().next());
    }

    return Collections.unmodifiableSet(new HashSet<Correlation>(
        toReturn.values()));
  }

  /**
   * Returns the SourceInfos from which this SourceInfo was ultimately derived.
   * SourceInfo objects which were not derived from others, via
   * {@link #makeChild}, will list itself as its root SourceInfo.
   */
  public Set<SourceInfo> getRoots() {
    if (parent == null && additionalAncestors.isEmpty()) {
      // If parent is null, we shouldn't have additional ancestors
      return Collections.unmodifiableSet(new HashSet<SourceInfo>(
          Collections.singleton(this)));

    } else if (additionalAncestors.size() == 0) {
      // This is a fairly typical case, where a node only has a parent
      return parent.getRoots();
    }

    // See if previously-computed work is available
    Set<SourceInfo> roots;
    if (lazyRoots != null && (roots = lazyRoots.get()) != null) {
      return roots;
    }

    // Otherwise, do some actual work
    roots = new HashSet<SourceInfo>();

    if (parent == null) {
      roots.add(this);
    } else {
      roots.addAll(parent.getRoots());
    }

    for (SourceInfo ancestor : additionalAncestors) {
      roots.addAll(ancestor.getRoots());
    }

    Set<SourceInfo> toReturn = Collections.unmodifiableSet(roots);
    lazyRoots = new SoftReference<Set<SourceInfo>>(toReturn);
    return toReturn;
  }

  public int getStartLine() {
    return startLine;
  }

  public int getStartPos() {
    return startPos;
  }

  public String getStory() {
    /*
     * TODO(bobv): This is a temporary implementation. At some point it should
     * return a proper object which tools could use.
     */
    StringBuilder toReturn = new StringBuilder();

    if (caller != null) {
      SourceInfo si = this;
      while (si != null) {
        if (si.mutation != null) {
          toReturn.append(si.mutation + " by " + si.caller + "\n  ");
        }
        si = si.parent;
      }
    }

    Set<SourceInfo> roots = getRoots();
    if (!roots.isEmpty()) {
      toReturn.append("\nRoots:\n");
      for (SourceInfo root : roots) {
        toReturn.append("  " + root.getFileName() + ":" + root.getStartLine()
            + "\n");
      }
    }

    Set<Correlation> allCorrelations = getAllCorrelations();
    Set<Correlation> primaryCorrelations = getPrimaryCorrelations();
    if (!allCorrelations.isEmpty()) {
      toReturn.append("\nCorrelations:\n");
      for (Correlation c : allCorrelations) {
        toReturn.append((primaryCorrelations.contains(c) ? " *" : "  ") + c
            + "\n");
      }
    }

    Set<SourceInfo> supertypes = supertypeAncestors;
    if (!supertypes.isEmpty()) {
      toReturn.append("\nSupertypes:\n{\n");
      for (SourceInfo info : supertypes) {
        toReturn.append(info.getStory());
      }
      toReturn.append("\n}\n");
    }

    return toReturn.toString();
  }

  /**
   * Returns <code>true</code> if {@link #getAllCorrelations()} would return a
   * Correlation that has one or more of the specifies Axes.
   */
  public boolean hasCorrelation(Set<Axis> axes) {
    // Try local information
    if (!correlations.isEmpty()) {
      for (Axis a : axes) {
        if (correlations.containsKey(a)) {
          return true;
        }
      }
    }

    // Try the parent chain
    if (parent != null && parent.hasCorrelation(axes)) {
      return true;
    }

    // Try additional ancestors
    for (SourceInfo info : additionalAncestors) {
      if (info.hasCorrelation(axes)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Create a derived SourceInfo object. If SourceInfo collection is disabled,
   * this method will return the current object.
   */
  public SourceInfo makeChild(Class<?> caller, String description) {
    return makeChild(caller, description, EMPTY_SOURCEINFO_ARRAY);
  }

  /**
   * Create a derived SourceInfo object that indicates that one or more AST
   * nodes were merged to create a new node. The derived node will inherit its
   * location from the SourceInfo object on which the method is invoked.
   */
  public SourceInfo makeChild(Class<?> caller, String description,
      SourceInfo... additionalAncestors) {
    if (!createDescendants) {
      return this;
    }

    String callerName = caller == null ? "Unrecorded caller" : caller.getName();
    return new SourceInfo(this, description, callerName, additionalAncestors);
  }

  /**
   * Implementation of the various getCorrelations functions.
   */
  private void findCorrelations(EnumMap<Axis, Set<Correlation>> accumulator,
      EnumSet<Axis> filter, boolean derivedOnly) {
    // Short circuit if all possible values have been seen
    if (derivedOnly && accumulator.size() == filter.size()) {
      return;
    }

    for (Map.Entry<Axis, Correlation> entry : correlations.entrySet()) {
      Axis key = entry.getKey();
      boolean containsKey = accumulator.containsKey(key);
      Correlation value = entry.getValue();

      if (containsKey) {
        if (!derivedOnly) {
          accumulator.get(key).add(value);
        }
      } else if (filter.contains(key)) {
        Set<Correlation> set = new HashSet<Correlation>();
        set.add(value);
        accumulator.put(key, derivedOnly ? Collections.unmodifiableSet(set)
            : set);
      }
    }

    if (parent != null) {
      parent.findCorrelations(accumulator, filter, derivedOnly);
    }

    for (SourceInfo info : additionalAncestors) {
      info.findCorrelations(accumulator, filter, derivedOnly);
    }

    if (!derivedOnly) {
      for (SourceInfo info : supertypeAncestors) {
        info.findCorrelations(accumulator, filter, derivedOnly);
      }
    }
  }
}
