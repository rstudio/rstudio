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

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks file and line information for AST nodes.
 */
public class SourceInfo implements Serializable {

  /**
   * Indicates that the source for an AST element is unknown. This indicates a
   * deficiency in the compiler.
   */
  public static final SourceInfo UNKNOWN = new SourceInfo(0, 0, 0,
      "Unknown source", true);

  /**
   * Examines the call stack to automatically determine a useful value to
   * provide as the caller argument to SourceInfo factory methods.
   */
  public static String findCaller() {
    /*
     * TODO(bobv): This function needs to be made robust for middle-man callers
     * other than JProgram and JsProgram.
     */
    String sourceInfoClassName = SourceInfo.class.getName();
    boolean wasInFindCaller = false;

    for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
      boolean inSourceInfo = e.getClassName().equals(SourceInfo.class.getName());
      if (inSourceInfo && "findCaller".equals(e.getMethodName())) {
        wasInFindCaller = true;
      } else if (wasInFindCaller
          && !"createSourceInfoSynthetic".equals(e.getMethodName())
          && !sourceInfoClassName.equals(e.getClassName())) {
        return e.getClassName() + "." + e.getMethodName();
      }
    }

    return "Unknown caller";
  }

  private final Set<SourceInfo> additionalAncestors = new HashSet<SourceInfo>();
  private final String caller;
  /**
   * This flag controls the behaviors of {@link #makeSynthetic(String)} and
   * {@link #makeChild(String)}.
   */
  private final boolean createDescendants;
  private final int endPos;
  private final String fileName;
  private transient Reference<Collection<SourceInfo>> lazyRoots;
  private final String mutation;
  private final SourceInfo parent;
  private final int startLine;
  private final int startPos;

  protected SourceInfo(int startPos, int endPos, int startLine,
      String fileName, boolean createDescendants) {
    this.createDescendants = createDescendants;
    this.startPos = startPos;
    this.endPos = endPos;
    this.startLine = startLine;
    this.fileName = fileName;
    this.parent = null;
    this.mutation = null;
    this.caller = null;
  }

  private SourceInfo(SourceInfo parent, String mutation, String caller,
      SourceInfo... additionalAncestors) {
    this.createDescendants = parent.createDescendants;
    this.startPos = parent.startPos;
    this.endPos = parent.endPos;
    this.startLine = parent.startLine;
    this.fileName = parent.fileName;
    this.additionalAncestors.addAll(Arrays.asList(additionalAncestors));
    this.additionalAncestors.addAll(parent.additionalAncestors);
    this.parent = parent;
    this.mutation = mutation;
    this.caller = caller;
  }

  public synchronized void addAdditonalAncestors(SourceInfo... sourceInfos) {
    for (SourceInfo ancestor : sourceInfos) {
      if (ancestor == this) {
        continue;
      } else if (additionalAncestors.contains(ancestor)) {
        continue;
      } else {
        additionalAncestors.add(ancestor);
      }
    }
    if (lazyRoots != null) {
      lazyRoots.clear();
    }
  }

  public int getEndPos() {
    return endPos;
  }

  public String getFileName() {
    return fileName;
  }

  /**
   * Returns the SourceInfos from which this SourceInfo was ultimately derived.
   * SourceInfo objects which were not derived from others, via
   * {@link #makeChild}, will list itself as its root SourceInfo.
   */
  public synchronized Collection<SourceInfo> getRoots() {
    if (parent == null) {
      // If parent is null, we shouldn't have additional ancestors
      assert additionalAncestors.size() == 0;
      return Collections.singleton(this);

    } else if (additionalAncestors.size() == 0) {
      // This is a fairly typical case, where a node only has a parent
      return parent.getRoots();
    }

    // See if previously-computed work is available
    Collection<SourceInfo> roots;
    if (lazyRoots != null && (roots = lazyRoots.get()) != null) {
      return roots;
    }

    // Otherwise, do some actual work
    roots = new ArrayList<SourceInfo>();

    roots.addAll(parent.getRoots());
    for (SourceInfo ancestor : additionalAncestors) {
      roots.addAll(ancestor.getRoots());
    }

    Collection<SourceInfo> toReturn = Collections.unmodifiableCollection(roots);
    lazyRoots = new SoftReference<Collection<SourceInfo>>(toReturn);
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

    for (SourceInfo root : getRoots()) {
      toReturn.append(root.fileName + ":" + root.startLine + "\n");
    }

    return toReturn.toString();
  }

  /**
   * Create a derived SourceInfo object. If SourceInfo collection is disabled,
   * this method will return the current object.
   */
  public SourceInfo makeChild(String description) {
    return makeChild(description, new SourceInfo[0]);
  }

  /**
   * Create a derived SourceInfo object that indicates that one or more AST
   * nodes were merged to create a new node. The derived node will inherit its
   * location from the SourceInfo object on which the method is invoked.
   */
  public SourceInfo makeChild(String description,
      SourceInfo... additionalAncestors) {
    if (!createDescendants) {
      return this;
    }

    return new SourceInfo(this, description, findCaller(), additionalAncestors);
  }
}
