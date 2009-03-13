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

import java.io.File;

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
  public abstract File getDepFile();

  /**
   * @return a file of split points
   */
  public abstract File getSplitPointsFile();

  /**
   * @return a file of stories
   */
  public abstract File getStoriesFile();

  @Override
  public final int hashCode() {

    assert (getDepFile() != null);
    assert (getStoriesFile() != null);
    assert (getSplitPointsFile() != null);

    return 17 * (37 + getDepFile().getName().hashCode())
        + (37 + getStoriesFile().getName().hashCode())
        + (37 + getSplitPointsFile().getName().hashCode());
  }

  @Override
  protected final int compareToComparableArtifact(CompilationAnalysis o) {

    if ((getDepFile() == null) && (o.getDepFile() == null)) {
      return 0;
    } else if ((getDepFile() == null) && (o.getDepFile() != null)) {
      return 1;
    } else if ((getDepFile() != null) && (o.getDepFile() == null)) {
      return -1;
    } else if (getDepFile().getName().compareTo(o.getDepFile().getName()) == 0) {
      if ((getStoriesFile() == null) && (o.getStoriesFile() == null)) {
        return 0;
      } else if ((getStoriesFile() == null) && (o.getStoriesFile() != null)) {
        return 1;
      } else if ((getStoriesFile() != null) && (o.getStoriesFile() == null)) {
        return -1;
      } else if (getStoriesFile().getName().compareTo(
          o.getStoriesFile().getName()) == 0) {
        if ((getSplitPointsFile() == null) && (o.getSplitPointsFile() == null)) {
          return 0;
        }
        if ((getSplitPointsFile() == null) && (o.getSplitPointsFile() != null)) {
          return 1;
        } else if ((getSplitPointsFile() != null)
            && (o.getSplitPointsFile() == null)) {
          return -1;
        } else {
          return getSplitPointsFile().getName().compareTo(
              o.getSplitPointsFile().getName());
        }
      } else {
        return getStoriesFile().getName().compareTo(
            o.getStoriesFile().getName());
      }
    } else {
      return getDepFile().getName().compareTo(o.getDepFile().getName());
    }
  }

  @Override
  protected final Class<CompilationAnalysis> getComparableArtifactType() {
    return CompilationAnalysis.class;
  }
}
