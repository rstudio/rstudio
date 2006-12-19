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
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerOutDir;
import com.google.gwt.util.tools.ToolBase;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a GWT application.
 * 
 */
public final class ApplicationCreator extends ToolBase {

  /**
   * Arguments for the application creator.
   * 
   */
  protected class ArgHandlerAppClass extends ArgHandlerExtra {

    public boolean addExtraArg(String arg) {
      if (fullClassName != null) {
        System.err.println("Too many arguments");
        return false;
      }

      // Check className for certain properties
      if (!arg.matches("[\\w\\$]+(\\.[\\w\\$]+)+")) {
        System.err.println("'" + arg
            + "' does not appear to be a valid fully-qualified Java class name");
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
      if (parts.length < 2 || !parts[parts.length - 2].equals("client")) {
        System.err.println("'"
            + arg
            + "': Please use 'client' as the final package, as in 'com.example.foo.client.MyApp'.\n"
            + "It isn't technically necessary, but this tool enforces the best practice.");
        return false;
      }

      fullClassName = arg;
      return true;
    }

    public String getPurpose() {
      return "The fully-qualified name of the application class to create";
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
    String path = ApplicationCreator.class.getName();
    path = path.substring(0, path.lastIndexOf('.') + 1);
    PACKAGE_PATH = path.replace('.', '/');
  }

  public static void main(String[] args) {
    ApplicationCreator creator = new ApplicationCreator();
    if (creator.processArgs(args)) {
      if (creator.run()) {
        return;
      }
    }
    System.exit(1);
  }

  /**
   * @param fullClassName Name of the fully-qualified Java class to create as an
   *          Application.
   * @param outDir Where to put the output files
   * @param eclipse The name of a project to attach a .launch config to
   * @param overwrite Overwrite an existing files if they exist.
   * @param ignore Ignore existing files if they exist.
   * @throws IOException
   */
  static void createApplication(String fullClassName, File outDir,
      String eclipse, boolean overwrite, boolean ignore) throws IOException {

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

    // Check out the class and package names.
    //
    int pos = fullClassName.lastIndexOf('.');
    String clientPackageName = fullClassName.substring(0, pos);
    String className = fullClassName.substring(pos + 1);

    // Compute module name and directories
    //
    pos = clientPackageName.lastIndexOf('.');
    File basePackageDir;
    String moduleName;
    File javaDir = Utility.getDirectory(outDir, "src", true);
    if (pos >= 0) {
      String basePackage = clientPackageName.substring(0, pos);
      moduleName = basePackage + "." + className;
      basePackage = basePackage.replace('.', '/');
      basePackageDir = Utility.getDirectory(javaDir, basePackage, true);
    } else {
      moduleName = className;
      basePackageDir = javaDir;
    }
    File clientDir = Utility.getDirectory(basePackageDir, "client", true);
    File publicDir = Utility.getDirectory(basePackageDir, "public", true);
    String startupUrl = moduleName + "/" + className + ".html";

    // Create a map of replacements
    //
    Map replacements = new HashMap();
    replacements.put("@className", className);
    replacements.put("@moduleName", moduleName);
    replacements.put("@clientPackage", clientPackageName);
    replacements.put("@gwtUserPath", basePathEnv + gwtUserPath);
    replacements.put("@gwtDevPath", basePathEnv + gwtDevPath);
    replacements.put("@shellClass", "com.google.gwt.dev.GWTShell");
    replacements.put("@compileClass", "com.google.gwt.dev.GWTCompiler");
    replacements.put("@startupUrl", startupUrl);
    replacements.put("@vmargs", isMacOsX ? "-XstartOnFirstThread" : "");

    {
      // Create the module
      File moduleXML = Utility.createNormalFile(basePackageDir, className
          + ".gwt.xml", overwrite, ignore);
      if (moduleXML != null) {
        String out = Utility.getFileFromClassPath(PACKAGE_PATH
            + "Module.gwt.xmlsrc");
        Utility.writeTemplateFile(moduleXML, out, replacements);
      }
    }

    {
      // Create a skeleton html file
      File publicHTML = Utility.createNormalFile(publicDir,
          className + ".html", overwrite, ignore);
      if (publicHTML != null) {
        String out = Utility.getFileFromClassPath(PACKAGE_PATH
            + "AppHtml.htmlsrc");
        Utility.writeTemplateFile(publicHTML, out, replacements);
      }
    }

    {
      // Create a skeleton Application class
      File javaClass = Utility.createNormalFile(clientDir, className + ".java",
          overwrite, ignore);
      if (javaClass != null) {
        String out = Utility.getFileFromClassPath(PACKAGE_PATH
            + "AppClassTemplate.javasrc");
        Utility.writeTemplateFile(javaClass, out, replacements);
      }
    }

    if (eclipse != null) {
      // Create an eclipse launch config
      replacements.put("@projectName", eclipse);
      File launchConfig = Utility.createNormalFile(outDir, className
          + ".launch", overwrite, ignore);
      if (launchConfig != null) {
        String out = Utility.getFileFromClassPath(PACKAGE_PATH
            + "App.launchsrc");
        Utility.writeTemplateFile(launchConfig, out, replacements);
      }
    }

    // create startup files
    String extension;
    if (isWindows) {
      extension = ".cmd";
    } else {
      extension = "";
    }

    File gwtshell = Utility.createNormalFile(outDir, className + "-shell"
        + extension, overwrite, ignore);
    if (gwtshell != null) {
      String out = Utility.getFileFromClassPath(PACKAGE_PATH + "gwtshell"
          + extension + "src");
      Utility.writeTemplateFile(gwtshell, out, replacements);
      if (extension.length() == 0) {
        chmodExecutable(gwtshell);
      }
    }

    File gwtcompile = Utility.createNormalFile(outDir, className + "-compile"
        + extension, overwrite, ignore);
    if (gwtcompile != null) {
      String out = Utility.getFileFromClassPath(PACKAGE_PATH + "gwtcompile"
          + extension + "src");
      Utility.writeTemplateFile(gwtcompile, out, replacements);
      if (extension.length() == 0) {
        chmodExecutable(gwtcompile);
      }
    }
  }

  /**
   * Try to make the given file executable. Implementation tries to exec chmod,
   * which may fail if the platform doesn't support it. Prints a warning to
   * stderr if the call fails.
   * 
   * @param file the file to make executable
   */
  private static void chmodExecutable(File file) {
    try {
      Runtime.getRuntime().exec("chmod u+x " + file.getAbsolutePath());
    } catch (Throwable e) {
      System.err.println(("Warning: cannot exec chmod to set permission on generated file."));
    }
  }

  private String eclipse = null;
  private String fullClassName = null;
  private boolean ignore = false;
  private File outDir;
  private boolean overwrite = false;

  protected ApplicationCreator() {

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
          System.err.println("-overwrite cannot be used with -ignore");
          return false;
        }
        overwrite = true;
        return true;
      }
    });

    registerHandler(new ArgHandlerIgnore() {
      public boolean setFlag() {
        if (overwrite) {
          System.err.println("-ignore cannot be used with -overwrite");
          return false;
        }
        ignore = true;
        return true;
      }
    });

    registerHandler(new ArgHandlerAppClass());
  }

  protected boolean run() {
    try {
      createApplication(fullClassName, outDir, eclipse, overwrite, ignore);
      return true;
    } catch (IOException e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      return false;
    }
  }

}
