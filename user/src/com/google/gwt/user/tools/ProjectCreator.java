/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.user.tools.util.ArgHandlerAddToClassPath;
import com.google.gwt.user.tools.util.ArgHandlerEclipse;
import com.google.gwt.user.tools.util.ArgHandlerIgnore;
import com.google.gwt.user.tools.util.ArgHandlerOverwrite;
import com.google.gwt.user.tools.util.CreatorUtilities;
import com.google.gwt.util.tools.ArgHandlerOutDir;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.ToolBase;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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
   * Create a set of project files.
   * 
   * @param eclipse The name of project to create.
   * @param ant The name of an ant file to create.
   * @param outDir The directory to write into.
   * @param overwrite Overwrite an existing files if they exist.
   * @param ignore Ignore existing files if they exist.
   * @throws IOException
   */
  static void createProject(String eclipse, String ant, File outDir,
      boolean overwrite, boolean ignore) throws IOException {
    createProject(eclipse, ant, outDir, overwrite, ignore, null);
  }

  /**
   * Create a set of project files.
   * 
   * @param eclipse The name of project to create.
   * @param ant The name of an ant file to create.
   * @param outDir The directory to write into.
   * @param overwrite Overwrite an existing files if they exist.
   * @param ignore Ignore existing files if they exist.
   * @param extraClassPaths class path entries passed on the command line
   * @throws IOException
   */
  static void createProject(String eclipse, String ant, File outDir,
      boolean overwrite, boolean ignore, List<String> extraClassPaths)
      throws IOException {

    // Figure out the installation directory
    String installPath = Utility.getInstallPath();

    // Create a map of replacements.
    Map<String, String> replacements = new HashMap<String, String>();
    String userJarPath = installPath + '/' + "gwt-user.jar";
    replacements.put("@gwtUserPath", userJarPath);

    // Check to see that the passed extra path/module arguments are valid.
    if (!CreatorUtilities.validatePathsAndModules(userJarPath, extraClassPaths,
        null)) {
      return;
    }

    Utility.getDirectory(outDir, "src", true);
    Utility.getDirectory(outDir, "test", true);

    if (ant != null) {
      // Create an ant build file
      replacements.put("@projectName", ant);

      // Build the list of extra paths
      StringBuilder buf = new StringBuilder();
      if (extraClassPaths != null) {
        for (String path : extraClassPaths) {
          buf.append("    <pathelement path=\"" + path + "\"/>");
        }
      }
      replacements.put("@extraAntPathElements", buf.toString());

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

      StringBuilder classpathEntries = new StringBuilder();
      if (extraClassPaths != null) {
        for (String path : extraClassPaths) {
          File f = new File(path);

          if (!f.exists()) {
            throw new FileNotFoundException("extraClassPath: " + path
                + " must be present before .launch file can be created.");
          }
          // Handle both .jar files and paths
        String kindString;
          if (f.isDirectory()) {
            kindString = "output";
          } else if (path.endsWith(".jar")) {
            kindString = "lib";
          } else {
            throw new RuntimeException("Don't know how to handle path: " + path
                + ". It doesn't appear to be a directory or a .jar file");
          }
          classpathEntries.append("   <classpathentry kind=\"");
          classpathEntries.append(kindString);
          classpathEntries.append("\" path=\"");
          classpathEntries.append(path);
          classpathEntries.append("\"/>\n");
        }
      }

      replacements.put("@eclipseClassPathEntries", classpathEntries.toString());

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
  private ArgHandlerAddToClassPath classPathHandler = new ArgHandlerAddToClassPath();

  protected ProjectCreator() {

    registerHandler(new ArgHandlerString() {

      @Override
      public String getPurpose() {
        return "Generate an Ant buildfile to compile source (.ant.xml will be appended)";
      }

      @Override
      public String getTag() {
        return "-ant";
      }

      @Override
      public String[] getTagArgs() {
        return new String[] {"projectName"};
      }

      @Override
      public boolean setString(String str) {
        ant = str;
        return true;
      }

    });

    registerHandler(new ArgHandlerEclipse() {
      @Override
      public String getPurpose() {
        return "Generate an eclipse project";
      }

      @Override
      public boolean setString(String str) {
        eclipse = str;
        return true;
      }
    });

    registerHandler(new ArgHandlerOutDir() {
      @Override
      public void setDir(File dir) {
        outDir = dir;
      }
    });

    registerHandler(new ArgHandlerOverwrite() {
      @Override
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
      @Override
      public boolean setFlag() {
        if (overwrite) {
          System.err.println("-ignore cannot be used with -overwrite.");
          return false;
        }
        ignore = true;
        return true;
      }
    });

    registerHandler(classPathHandler);
  }

  protected boolean run() {
    try {
      if (ant == null && eclipse == null) {
        System.err.println("Please specify either -ant or -eclipse.");
        printHelp();
        return false;
      }
      createProject(eclipse, ant, outDir, overwrite, ignore,
          classPathHandler.getExtraClassPathList());
      return true;
    } catch (IOException e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      return false;
    }
  }
}
