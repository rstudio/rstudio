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
package com.google.gwt.user.tools;

import com.google.gwt.user.tools.util.ArgHandlerEclipse;
import com.google.gwt.user.tools.util.ArgHandlerIgnore;
import com.google.gwt.user.tools.util.ArgHandlerOverwrite;
import com.google.gwt.util.tools.ArgHandlerOutDir;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.ToolBase;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a new GWT project.
 */
public final class ProjectCreator extends ToolBase {

  private static final String PACKAGE_PATH;

  static {
    String path = ProjectCreator.class.getName();
    path = path.substring(0, path.lastIndexOf('.') + 1);
    PACKAGE_PATH = path.replace('.', '/');
  }

  public static void main(String[] args) {
    ProjectCreator creator = new ProjectCreator();
    if (creator.processArgs(args)) {
      if (creator.run()) {
        return;
      }
    }

    System.exit(1);
  }

  /**
   * @param eclipse The name of project to create.
   * @param ant The name of an ant file to create.
   * @param outDir The directory to write into.
   * @param overwrite Overwrite an existing files if they exist.
   * @param ignore Ignore existing files if they exist.
   * @throws IOException
   */
  static void createProject(String eclipse, String ant, File outDir,
      boolean overwrite, boolean ignore) throws IOException {

    // Figure out the installation directory
    String installPath = Utility.getInstallPath();

    // Create a map of replacements.
    //
    Map replacements = new HashMap();
    replacements.put("@gwtUserPath", installPath + '/' + "gwt-user.jar");

    Utility.getDirectory(outDir, "src", true);
    Utility.getDirectory(outDir, "test", true);

    if (ant != null) {
      // Create an ant build file
      replacements.put("@projectName", ant);
      File antXML = Utility.createNormalFile(outDir, ant + ".ant.xml",
          overwrite, ignore);
      if (antXML != null) {
        String out = Utility.getFileFromClassPath(PACKAGE_PATH
            + "project.ant.xmlsrc");
        Utility.writeTemplateFile(antXML, out, replacements);
      }
    }

    if (eclipse != null) {
      // Create an eclipse project file
      replacements.put("@projectName", eclipse);
      File dotProject = Utility.createNormalFile(outDir, ".project", overwrite,
          ignore);
      if (dotProject != null) {
        String out = Utility.getFileFromClassPath(PACKAGE_PATH + ".projectsrc");
        Utility.writeTemplateFile(dotProject, out, replacements);
      }

      // Create an eclipse classpath file
      File dotClasspath = Utility.createNormalFile(outDir, ".classpath",
          overwrite, ignore);
      if (dotClasspath != null) {
        String out = Utility.getFileFromClassPath(PACKAGE_PATH
            + ".classpathsrc");
        Utility.writeTemplateFile(dotClasspath, out, replacements);
      }
    }
  }

  private String ant = null;

  private String eclipse = null;

  private boolean ignore = false;
  private File outDir = null;
  private boolean overwrite = false;

  protected ProjectCreator() {

    registerHandler(new ArgHandlerString() {

      public String getPurpose() {
        return "Generate an Ant buildfile to compile source (.ant.xml will be appended)";
      }

      public String getTag() {
        return "-ant";
      }

      public String[] getTagArgs() {
        return new String[] {"projectName"};
      }

      public boolean setString(String str) {
        ant = str;
        return true;
      }

    });

    registerHandler(new ArgHandlerEclipse() {
      public String getPurpose() {
        return "Generate an eclipse project";
      }

      public boolean setString(String str) {
        eclipse = str;
        return true;
      }
    });

    registerHandler(new ArgHandlerOutDir() {
      public void setDir(File dir) {
        outDir = dir;
      }
    });

    registerHandler(new ArgHandlerOverwrite() {
      public boolean setFlag() {
        if (ignore) {
          System.err.println("-overwrite cannot be used with -ignore.");
          return false;
        }
        overwrite = true;
        return true;
      }
    });

    registerHandler(new ArgHandlerIgnore() {
      public boolean setFlag() {
        if (overwrite) {
          System.err.println("-ignore cannot be used with -overwrite.");
          return false;
        }
        ignore = true;
        return true;
      }
    });
  }

  protected boolean run() {
    try {
      if (ant == null && eclipse == null) {
        System.err.println("Please specify either -ant or -eclipse.");
        printHelp();
        return false;
      }
      createProject(eclipse, ant, outDir, overwrite, ignore);
      return true;
    } catch (IOException e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      return false;
    }
  }
}
