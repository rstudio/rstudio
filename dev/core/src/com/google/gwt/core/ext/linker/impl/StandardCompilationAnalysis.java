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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.linker.CompilationAnalysis;
import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.core.linker.SoycReportLinker;

/**
 * An implementation of CompilationAnalysis. This class transforms SourceInfos
 * and related data into an API suitable for public consumption via the Linker
 * API.
 */
public class StandardCompilationAnalysis extends CompilationAnalysis {

  /**
   * A SOYC artifact. The existence of this class is an implementation detail.
   */
  public static class SoycArtifact extends SyntheticArtifact {
    public SoycArtifact(String partialPath, byte[] bytes) {
      super(SoycReportLinker.class, partialPath, bytes);
      setPrivate(true);
    }
  }

  /**
   * File containing method-level control-flow dependencies (corresponding to
   * the current report).
   */
  private SoycArtifact depFile;

  /**
   * File containing detailed story information.
   */
  private SoycArtifact detailedStoriesFile;

  /**
   * File containing split points.
   */
  private SoycArtifact splitPointsFile;

  /**
   * File containing size maps.
   */
  private SoycArtifact sizeMapsFile;

  /**
   * Constructed by PermutationCompiler.
   */
  public StandardCompilationAnalysis(SoycArtifact dependencies,
      SoycArtifact sizeMaps, SoycArtifact splitPoints,
      SoycArtifact detailedStories) {
    super(StandardLinkerContext.class);
    this.depFile = dependencies;
    this.sizeMapsFile = sizeMaps;
    this.splitPointsFile = splitPoints;
    this.detailedStoriesFile = detailedStories;
  }

  @Override
  public SoycArtifact getDepFile() {
    return depFile;
  }

  @Override
  public SoycArtifact getDetailedStoriesFile() {
    return detailedStoriesFile;
  }

  @Override
  public SoycArtifact getSizeMapsFile() {
    return sizeMapsFile;
  }

  @Override
  public SoycArtifact getSplitPointsFile() {
    return splitPointsFile;
  }
}
