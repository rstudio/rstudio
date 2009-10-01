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
import com.google.gwt.core.ext.linker.impl.StandardCompilationAnalysis.SoycArtifact;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents analysis data for a CompilationResult.
 */
public abstract class CompilationAnalysis extends Artifact<CompilationAnalysis> {

  protected CompilationAnalysis(Class<? extends Linker> linkerType) {
    super(linkerType);
  }

  /**
   * @return a file of dependencies
   */
  public abstract EmittedArtifact getDepFile();

  /**
   * @return a file with detailed story information
   */
  public abstract EmittedArtifact getDetailedStoriesFile();

  /**
   * Files containing the HTML dashboard.
   */

  public abstract List<SoycArtifact> getReportFiles();

  /**
   * @return a file of size maps
   */
  public abstract EmittedArtifact getSizeMapsFile();

  /**
   * @return a file of split points
   */
  public abstract EmittedArtifact getSplitPointsFile();

  @Override
  public final int hashCode() {
    int code = 37;
    for (EmittedArtifact file : allFiles()) {
      if (file == null) {
        code = code * 17 + 37;
      } else {
        code = code * 17 + file.getPartialPath().hashCode();
      }
    }
    return code;
  }

  @Override
  protected final int compareToComparableArtifact(CompilationAnalysis o) {
    LinkedList<EmittedArtifact> myFiles = new LinkedList<EmittedArtifact>(
        allFiles());
    LinkedList<EmittedArtifact> otherFiles = new LinkedList<EmittedArtifact>(
        o.allFiles());

    while (!myFiles.isEmpty()) {
      if (otherFiles.isEmpty()) {
        return 1;
      }

      EmittedArtifact myFile = myFiles.removeFirst();
      EmittedArtifact otherFile = otherFiles.removeFirst();
      if (myFile == null && otherFile == null) {
        continue;
      }
      if (myFile == null && otherFile != null) {
        return -1;
      }
      if (myFile != null && otherFile == null) {
        return 1;
      }
      assert myFile != null;
      assert otherFile != null;

      int fileCompare = myFile.getPartialPath().compareTo(
          otherFile.getPartialPath());
      if (fileCompare != 0) {
        return fileCompare;
      }
    }

    if (!otherFiles.isEmpty()) {
      return -1;
    }

    return 0;
  }

  @Override
  protected final Class<CompilationAnalysis> getComparableArtifactType() {
    return CompilationAnalysis.class;
  }

  private List<EmittedArtifact> allFiles() {
    List<EmittedArtifact> files = new ArrayList<EmittedArtifact>();
    files.add(getSplitPointsFile());
    files.add(getDepFile());
    files.add(getSizeMapsFile());
    files.add(getDetailedStoriesFile());
    files.addAll(getReportFiles());
    return files;
  }
}
