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
package com.google.gwt.core.ext.linker;

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.soyc.ClassMember;
import com.google.gwt.core.ext.soyc.FunctionMember;
import com.google.gwt.core.ext.soyc.Range;
import com.google.gwt.core.ext.soyc.Story;

import java.util.Map;
import java.util.SortedSet;

/**
 * Represents analysis data for a CompilationResult.
 */
public abstract class CompilationAnalysis extends Artifact<CompilationAnalysis> {

  /**
   * Associates a Story and a Range of the output. Instances of this interface
   * are obtained from {@link CompilationAnalysis#getSnippets()}.
   */
  public interface Snippet {
    Range getRange();

    Story getStory();
  }

  protected CompilationAnalysis(Class<? extends Linker> linkerType) {
    super(linkerType);
  }

  /**
   * Returns all ClassMembers present in the CompilationResult. This method
   * would typically be used by consumers that are interested in the type
   * hierarchy of the compilation.
   */
  public abstract SortedSet<ClassMember> getClasses();

  /**
   * Returns the CompilationResult upon which the analysis was performed.
   */
  public abstract CompilationResult getCompilationResult();

  /**
   * Returns all JavaScript FunctionMembers in the output.
   */
  public abstract SortedSet<FunctionMember> getFunctions();

  /**
   * Provides access to the assignments of Stories to Ranges for a fragment of
   * the output. The Ranges are guaranteed not to overlap, and may be used for
   * exact accounting of bytes. Due to the potential for very large data-sets to
   * be accessible through this method, it is recommended that Snippets should
   * be processed in an incremental fashion that does not require all instances
   * to be retained at once.
   */
  /*
   * NB: The reason that this returns an Iterable, and not a Map, is that we
   * want to delay the construction of Range objects for as long as possible. If
   * we were to return a Map for an analysis of N stories, we would also need N
   * Ranges, plus the overhead of constructing an ordered Map.
   */
  public abstract Iterable<Snippet> getSnippets(int fragmentNumber);

  /**
   * Returns splitPointMap.
   */
  public abstract Map<Integer, String> getSplitPointMap();

  /**
   * Returns all Stories.
   */
  public abstract SortedSet<Story> getStories();

  @Override
  public final int hashCode() {
    // NB: Identity is keyed to the CompilationResult
    return getCompilationResult().hashCode();
  }

  @Override
  public String toString() {
    return "Compilation analysis for " + getCompilationResult().toString();
  }

  @Override
  protected final int compareToComparableArtifact(CompilationAnalysis o) {
    /*
     * The identity of a CompilationAnalysis is based on the identity of its
     * associated CompilationResult.
     */
    return getCompilationResult().compareToComparableArtifact(
        o.getCompilationResult());
  }

  @Override
  protected final Class<CompilationAnalysis> getComparableArtifactType() {
    return CompilationAnalysis.class;
  }
}
