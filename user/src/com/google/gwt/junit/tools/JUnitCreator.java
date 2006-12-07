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
package com.google.gwt.junit.tools;

import com.google.gwt.user.tools.util.ArgHandlerEclipse;
import com.google.gwt.user.tools.util.ArgHandlerIgnore;
import com.google.gwt.user.tools.util.ArgHandlerOverwrite;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerOutDir;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.ToolBase;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Tool to create JUnit test case.
 * 
 */
public final class JUnitCreator extends ToolBase {

  /**
   * Arg Handler for <code>JUnitCreator</code>.
   */
  protected class ArgHandlerTestClass extends ArgHandlerExtra {

    public boolean addExtraArg(String arg) {
      if (fullClassName != null) {
        System.err.println("Too many arguments.");
        return false;
      }

      // Check className for certain properties
      if (!arg.matches("[\\w\\$]+(\\.[\\w\\$]+)+")) {
        System.err.println("'"
            + arg
            + "' does not appear to be a valid fully-qualified Java class name.");
        return false;
      }

      // Check out the class name.
      //
      if (arg.indexOf('$') != -1) {
        System.err.println("'" + arg
            + "': This version of the tool does not support nested classes");
        return false;
      }

      String[] parts = arg.split("\\.");
      if (parts.length < 2) {
        System.err.println("'" + arg
            + "': Cannot live in the root package. Please specify a package.");
        return false;
      }

      fullClassName = arg;
      return true;
    }

    public String getPurpose() {
      return "The fully-qualified name of the test class to create";
    }

    public String[] getTagArgs() {
      return new String[] {"className"};
    }

    public boolean isRequired() {
      return true;
    }
  }

  private static final String PACKAGE_PATH;

  static {
    String path = JUnitCreator.class.getName();
    path = path.substring(0, path.lastIndexOf('.') + 1);
    PACKAGE_PATH = path.replace('.', '/');
  }

  public static void main(String[] args) {
    JUnitCreator creator = new JUnitCreator();
    if (creator.processArgs(args)) {
      if (creator.run()) {
        return;
      }
    }

    System.exit(1);
  }

  /**
   * @param junitPath the path to the user's junit jar
   * @param moduleName the name of the module to contain this test
   * @param fullClassName Name of the fully-qualified Java class to create as an
   *          Application.
   * @param outDir Where to put the output files
   * @param eclipse The name of a project to attach a .launch config to
   * @param overwrite Overwrite an existing files if they exist.
   * @param ignore Ignore existing files if they exist.
   * @throws IOException
   */
  static void createTest(String junitPath, String moduleName,
      String fullClassName, File outDir, String eclipse, boolean overwrite,
      boolean ignore) throws IOException {

    // Figure out the installation directory
    String installPath = Utility.getInstallPath();
    String gwtUserPath = installPath + '/' + "gwt-user.jar";
    String gwtDevPath = installPath + '/' + Utility.getDevJarName();

    // Figure out what platform we're on
    // 
    boolean isWindows = gwtDevPath.substring(gwtDevPath.lastIndexOf('/') + 1).indexOf(
        "windows") >= 0;
    boolean isMacOsX = gwtDevPath.substring(gwtDevPath.lastIndexOf('/') + 1).indexOf(
        "mac") >= 0;

    // If the path from here to the install directory is relative, we need to
    // set specific "base" directory tags; this is for sample generation during
    // the build.
    String basePathEnv;
    if (!new File(installPath).isAbsolute()) {
      if (isWindows) {
        basePathEnv = "%~dp0\\";
      } else {
        basePathEnv = "$APPDIR/";
      }
    } else {
      basePathEnv = "";
    }

    // Check if junit path is absolute, add base if needed
    if (!new File(junitPath).isAbsolute()
        && junitPath.charAt(0) != File.separatorChar) {
      if (isWindows) {
        junitPath = "%~dp0\\" + junitPath;
      } else {
        junitPath = "$APPDIR/" + junitPath;
      }
    }

    // Check out the class and package names.
    //
    int pos = fullClassName.lastIndexOf('.');
    String clientPackageName = fullClassName.substring(0, pos);
    String className = fullClassName.substring(pos + 1);

    // Is the requested moduleName in a parent package of the clientPackage?
    //
    pos = moduleName.lastIndexOf('.');
    if (pos >= 0) {
      String modulePackageName = moduleName.substring(0, pos);
      if (modulePackageName.length() == clientPackageName.length()
          || !clientPackageName.startsWith(modulePackageName + '.')) {
        System.err.println("Warning: '" + modulePackageName
            + "' is not a parent package of '" + clientPackageName
            + "'.  The source for '" + className + "' may be unavailable.");
      }
    }

    // Compute module name and directories
    //
    pos = clientPackageName.lastIndexOf('.');
    File clientDir = Utility.getDirectory(outDir, "test", true);
    if (pos >= 0) {
      String clientPackage = clientPackageName.replace('.', '/');
      clientDir = Utility.getDirectory(clientDir, clientPackage, true);
    }

    // Create a map of replacements
    //
    Map replacements = new HashMap();
    replacements.put("@className", className);
    replacements.put("@moduleName", moduleName);
    replacements.put("@clientPackage", clientPackageName);
    replacements.put("@junitPath", junitPath);
    replacements.put("@gwtUserPath", basePathEnv + gwtUserPath);
    replacements.put("@gwtDevPath", basePathEnv + gwtDevPath);
    replacements.put("@vmargs", isMacOsX ? "-XstartOnFirstThread" : "");

    {
      // Create a skeleton Test class
      File javaClass = Utility.createNormalFile(clientDir, className + ".java",
          overwrite, ignore);
      if (javaClass != null) {
        String out = Utility.getFileFromClassPath(PACKAGE_PATH
            + "JUnitClassTemplate.javasrc");
        Utility.writeTemplateFile(javaClass, out, replacements);
      }
    }

    if (eclipse != null) {
      // Create an eclipse launch config
      replacements.put("@projectName", eclipse);
      File hostedConfig = Utility.createNormalFile(outDir, className
          + "-hosted.launch", overwrite, ignore);
      if (hostedConfig != null) {
        String out = Utility.getFileFromClassPath(PACKAGE_PATH
            + "JUnit-hosted.launchsrc");
        Utility.writeTemplateFile(hostedConfig, out, replacements);
      }

      File webConfig = Utility.createNormalFile(outDir, className
          + "-web.launch", overwrite, ignore);
      if (webConfig != null) {
        String out = Utility.getFileFromClassPath(PACKAGE_PATH
            + "JUnit-web.launchsrc");
        Utility.writeTemplateFile(webConfig, out, replacements);
      }
    }

    // create startup files
    String extension;
    if (isWindows) {
      extension = ".cmd";
    } else {
      extension = "";
    }

    File junitHosted = Utility.createNormalFile(outDir, className + "-hosted"
        + extension, overwrite, ignore);
    if (junitHosted != null) {
      String out = Utility.getFileFromClassPath(PACKAGE_PATH + "junit-hosted"
          + extension + "src");
      Utility.writeTemplateFile(junitHosted, out, replacements);
      if (extension.length() == 0) {
        Runtime.getRuntime().exec("chmod u+x " + junitHosted.getAbsolutePath());
      }
    }

    File junitWeb = Utility.createNormalFile(outDir, className + "-web"
        + extension, overwrite, ignore);
    if (junitWeb != null) {
      String out = Utility.getFileFromClassPath(PACKAGE_PATH + "junit-web"
          + extension + "src");
      Utility.writeTemplateFile(junitWeb, out, replacements);
      if (extension.length() == 0) {
        Runtime.getRuntime().exec("chmod u+x " + junitWeb.getAbsolutePath());
      }
    }
  }

  private String eclipse = null;

  private String fullClassName = null;

  private boolean ignore = false;
  private String junitPath = null;
  private String moduleName = null;
  private File outDir;
  private boolean overwrite = false;

  protected JUnitCreator() {

    registerHandler(new ArgHandlerString() {

      public String[] getDefaultArgs() {
        return null;
      }

      public String getPurpose() {
        return "Specify the path to your junit.jar (required)";
      }

      public String getTag() {
        return "-junit";
      }

      public String[] getTagArgs() {
        return new String[] {"pathToJUnitJar"};
      }

      public boolean isRequired() {
        return true;
      }

      public boolean setString(String str) {
        File f = new File(str);
        if (!f.exists() || !f.isFile()) {
          System.err.println("File not found: " + str);
          return false;
        }
        junitPath = str;
        return true;
      }
    });

    registerHandler(new ArgHandlerString() {

      public String[] getDefaultArgs() {
        return null;
      }

      public String getPurpose() {
        return "Specify the name of the GWT module to use (required)";
      }

      public String getTag() {
        return "-module";
      }

      public String[] getTagArgs() {
        return new String[] {"moduleName"};
      }

      public boolean isRequired() {
        return true;
      }

      public boolean setString(String str) {
        moduleName = str;
        return true;
      }
    });

    registerHandler(new ArgHandlerEclipse() {
      public String getPurpose() {
        return "Creates a debug launch config for the named eclipse project";
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

    registerHandler(new ArgHandlerTestClass());
  }

  protected boolean run() {
    try {
      createTest(junitPath, moduleName, fullClassName, outDir, eclipse,
          overwrite, ignore);
      return true;
    } catch (IOException e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      return false;
    }
  }
}
