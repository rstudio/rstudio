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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.CompilationAnalysis;
import java.io.File;

/**
 * An implementation of CompilationAnalysis. This class transforms SourceInfos
 * and related data into an API suitable for public consumption via the Linker
 * API.
 */
public class StandardCompilationAnalysis extends CompilationAnalysis {

  /**
   *  File containing method-level control-flow dependencies (corresponding to the current report)
   */
  private File depFile;
  
  /**
   * File containing stories
   */
  private File storiesFile;
  
  /**
   * File containing split points
   */
  private File splitPointsFile;
  
  /**
   * Constructed by PermutationCompiler.
   */
  public StandardCompilationAnalysis(TreeLogger logger, File depFile, File storiesFile, File splitPointsFile)
      throws UnableToCompleteException {
    super(StandardLinkerContext.class);
    logger = logger.branch(TreeLogger.INFO,
        "Creating CompilationAnalysis");
    
    this.depFile = depFile;
    this.storiesFile = storiesFile;
    this.splitPointsFile = splitPointsFile;

    logger.log(TreeLogger.INFO, "Done");
  }

  @Override
  public File getDepFile() {
    return depFile;
  }

  @Override
  public File getStoriesFile() {
    return storiesFile;
  }

  @Override
  public File getSplitPointsFile() {
    return splitPointsFile;
  }
}
