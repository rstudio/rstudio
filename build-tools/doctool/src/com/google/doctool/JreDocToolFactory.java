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

package com.google.doctool;

import java.io.PrintStream;

/**
 * Supports two-phase creation of {@link JreDocTool} objects.
 */
public class JreDocToolFactory {

  private String classpath;

  private String outputFile;

  private String packages;

  private String sourcepath;

  public JreDocTool create(PrintStream err) {
    if (this.classpath == null) {
      err.println("You must specify the -classpath");
      return null;
    }

    if (this.outputFile == null) {
      err.println("You must specify the output file (-out)");
      return null;
    }

    if (this.packages == null) {
      err.println("You must specify the -packages");
      return null;
    }

    if (this.sourcepath == null) {
      err.println("You must specify the -sourcepath");
      return null;
    }

    return new JreDocTool(classpath, outputFile, packages, sourcepath);
  }

  public void setClasspath(String classpath) {
    this.classpath = classpath;
  }

  public void setOutputFile(String outputFile) {
    this.outputFile = outputFile;
  }

  public void setPackages(String packages) {
    this.packages = packages;
  }

  public void setSourcepath(String sourcePath) {
    this.sourcepath = sourcePath;
  }
}
