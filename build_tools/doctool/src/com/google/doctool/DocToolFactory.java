/*
 * Copyright 2006 Google Inc.
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

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Supports two-phase creation of {@link DocTool} objects.
 */
public class DocToolFactory {

  private final List classPathEntries = new ArrayList();

  private String fileBase;

  private String fileType;

  private boolean generateHtml;

  private final List htmlFileBases = new ArrayList();

  private final List imagePathEntries = new ArrayList();

  private File outDir;

  private File overviewFile;

  private final List packageNameEntries = new ArrayList();

  private final List srcPathEntries = new ArrayList();

  private String title;

  public DocToolFactory() {
  }

  public void addHtmlFileBase(String filebase) {
    htmlFileBases.add(filebase);
  }

  public void addToClassPath(String path) {
    classPathEntries.add(new File(path));
  }

  public void addToImagePath(String path) {
    imagePathEntries.add(new File(path));
  }

  public void addToPackages(String packageName) {
    this.packageNameEntries.add(packageName);
  }

  public void addToSourcePath(String path) {
    srcPathEntries.add(new File(path));
  }

  public DocTool create(PrintStream out, PrintStream err) {
    File localOutDir = outDir;
    if (localOutDir == null) {
      localOutDir = new File(System.getProperty("user.dir"), "out");
      out.println("Using default output directory: "
          + localOutDir.getAbsolutePath());
    }

    File[] classPath = null;
    File[] sourcePath = null;
    String[] packageNames = null;
    if (fileType != null) {
      // Generating a doc set implies other settings.
      //
      if (fileBase == null) {
        err.println("A file base must be specified when generating doc");
        return null;
      }
      // if (overviewFile == null) {
      // err
      // .println("An overview file must be specified when generating doc; if
      // you don't have one, use this:");
      // err.println("<html><body>");
      // err.println(" " + fileBase + "documentation");
      // err.println(" @id " + fileBase + "-doc");
      // err.println(" @title Documentation for " + fileBase);
      // err.println("</body></html>");
      // return null;
      // }
      classPath = (File[]) classPathEntries.toArray(new File[0]);
      sourcePath = (File[]) srcPathEntries.toArray(new File[0]);
      packageNames = (String[]) packageNameEntries.toArray(new String[0]);
    }

    if (generateHtml) {
      if (title == null) {
        out.println("A title must be specified when generating html");
        return null;
      }

      if (htmlFileBases.isEmpty()) {
        out.println("No html filebases were specified");
        return null;
      }
    }

    String[] htmlFileBaseArray = (String[]) htmlFileBases.toArray(new String[0]);

    // Handle -imagepath
    //
    List localImagePathEntries = new ArrayList(imagePathEntries);
    if (localImagePathEntries.isEmpty()) {
      out.println("No image path specified; using only the output dir");
    }

    localImagePathEntries.add(localOutDir);
    File[] imagePath = (File[]) imagePathEntries.toArray(new File[0]);

    return new DocTool(out, err, localOutDir, generateHtml, title,
        htmlFileBaseArray, fileType, fileBase, overviewFile, sourcePath,
        classPath, packageNames, imagePath);
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileBase(String fileBase) {
    this.fileBase = fileBase;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public void setGenerateHtml(boolean generateHtml) {
    this.generateHtml = generateHtml;
  }

  public void setOutDir(String outDirPath) {
    this.outDir = new File(outDirPath);
  }

  public void setOverviewFile(String overviewFile) {
    this.overviewFile = new File(overviewFile);
  }

  public void setTitle(String title) {
    this.title = title;
  }
}
