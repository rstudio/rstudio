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
package com.google.gwt.user.tools;

import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.user.tools.util.ArgHandlerAddToClassPath;
import com.google.gwt.user.tools.util.ArgHandlerEclipse;
import com.google.gwt.user.tools.util.ArgHandlerIgnore;
import com.google.gwt.user.tools.util.ArgHandlerOverwrite;
import com.google.gwt.user.tools.util.CreatorUtilities;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerOutDir;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.ToolBase;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Creates a GWT application skeleton.
 */
public final class ApplicationCreator extends ToolBase {

  /*
   * Arguments for the application creator.
   */

  /**
   * Add an extra module injection into the top level module file.
   */
  protected class ArgHandlerAddModule extends ArgHandlerString {
    private List<String> extraModuleList = new ArrayList<String>();

    public List<String> getExtraModuleList() {
      return extraModuleList;
    }

    @Override
    public String getPurpose() {
      return "Adds extra GWT modules to be inherited.";
    }

    @Override
    public String getTag() {
      return "-addModule";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"module"};
    }

    @Override
    public boolean setString(String str) {
      // Parse out a comma separated list
      StringTokenizer st = new StringTokenizer(str, ",");
      while (st.hasMoreTokens()) {
        String module = st.nextToken();

        // Check className to see that it is a period separated string of words.
        if (!module.matches("[\\w\\$]+(\\.[\\w\\$]+)+")) {
          System.err.println("'" + module
              + "' does not appear to be a valid fully-qualified module name");
          return false;
        }
        extraModuleList.add(module);
      }

      return true;
    }
  }

  /**
   * Specify the top level class name of the application to create.
   */
  protected class ArgHandlerAppClass extends ArgHandlerExtra {

    @Override
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

    @Override
    public String getPurpose() {
      return "The fully-qualified name of the application class to create";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"className"};
    }

    @Override
    public boolean isRequired() {
      return true;
    }
  }

  static class FileCreator {
    private final File dir;
    private final String sourceName;
    private final String className;

    FileCreator(File dir, String sourceName, String className) {
      this.dir = dir;
      this.sourceName = sourceName;
      this.className = className;
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
    createApplication(fullClassName, outDir, eclipse, overwrite, ignore, null,
        null);
  }

  /**
   * @param fullClassName Name of the fully-qualified Java class to create as an
   *          Application.
   * @param outDir Where to put the output files
   * @param eclipse The name of a project to attach a .launch config to
   * @param overwrite Overwrite an existing files if they exist.
   * @param ignore Ignore existing files if they exist.
   * @param extraClassPaths A list of paths to append to the class path for
   *          launch configs.
   * @param extraModules A list of GWT modules to add 'inherits' tags for.
   * @throws IOException
   */
  static void createApplication(String fullClassName, File outDir,
      String eclipse, boolean overwrite, boolean ignore,
      List<String> extraClassPaths, List<String> extraModules)
      throws IOException {
    createApplication(fullClassName, outDir, eclipse, overwrite, ignore,
        extraClassPaths, extraModules, null);
  }

  /**
   * @param fullClassName Name of the fully-qualified Java class to create as an
   *          Application.
   * @param outDir Where to put the output files
   * @param eclipse The name of a project to attach a .launch config to
   * @param overwrite Overwrite an existing files if they exist.
   * @param ignore Ignore existing files if they exist.
   * @param extraClassPaths A list of paths to append to the class path for
   *          launch configs.
   * @param extraModules A list of GWT modules to add 'inherits' tags for.
   * @param newModuleName The new module name
   * @throws IOException
   */
  static void createApplication(String fullClassName, File outDir,
      String eclipse, boolean overwrite, boolean ignore,
      List<String> extraClassPaths, List<String> extraModules,
      String newModuleName) throws IOException {

    // Figure out the installation directory

    String installPath = Utility.getInstallPath();
    String gwtUserPath = installPath + '/' + "gwt-user.jar";
    String gwtDevPath = installPath + '/' + Utility.getDevJarName();
    String gwtServletPath = installPath + '/' + "gwt-servlet.jar";

    // Validate the arguments for extra class path entries and modules.
    if (!CreatorUtilities.validatePathsAndModules(gwtUserPath, extraClassPaths,
        extraModules)) {
      return;
    }
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
    String serverPackageName = null;
    File javaDir = Utility.getDirectory(outDir, "src", true);
    Utility.getDirectory(outDir, "test", true);
    File warDir = Utility.getDirectory(outDir, "war", true);
    File webInfDir = Utility.getDirectory(warDir, "WEB-INF", true);
    if (pos >= 0) {
      String basePackage = clientPackageName.substring(0, pos);
      moduleName = basePackage + "." + className;
      serverPackageName = basePackage + ".server";
      basePackage = basePackage.replace('.', '/');
      basePackageDir = Utility.getDirectory(javaDir, basePackage, true);
    } else {
      moduleName = className;
      basePackageDir = javaDir;
      serverPackageName = "server";
    }
    File clientDir = Utility.getDirectory(basePackageDir, "client", true);
    File serverDir = Utility.getDirectory(basePackageDir, "server", true);
    String startupUrl = className + ".html";

    // Create a map of replacements
    //
    Map<String, String> replacements = new HashMap<String, String>();
    replacements.put("@className", className);
    replacements.put("@moduleName", moduleName);
    replacements.put("@clientPackage", clientPackageName);
    replacements.put("@serverPackage", serverPackageName);
    replacements.put("@gwtUserPath", basePathEnv + gwtUserPath);
    replacements.put("@gwtDevPath", basePathEnv + gwtDevPath);
    replacements.put("@shellClass", "com.google.gwt.dev.GWTHosted");
    replacements.put("@compileClass", "com.google.gwt.dev.GWTCompiler");
    replacements.put("@startupUrl", startupUrl);
    replacements.put("@antVmargs", isMacOsX
        ? "<jvmarg value=\"-XstartOnFirstThread\"/>" : "");
    replacements.put("@vmargs", isMacOsX ? "-XstartOnFirstThread" : "");
    replacements.put("@eclipseExtraLaunchPaths",
        CreatorUtilities.createEclipseExtraLaunchPaths(extraClassPaths));
    replacements.put("@extraModuleInherits",
        createExtraModuleInherits(extraModules));
    replacements.put("@extraClassPathsColon", CreatorUtilities.appendPaths(":",
        extraClassPaths));
    replacements.put("@extraClassPathsSemicolon", CreatorUtilities.appendPaths(
        ";", extraClassPaths));
    replacements.put("@newModuleName", (newModuleName != null) ? newModuleName
        : moduleName);

    {
      // create the module xml file, skeleton html file, skeleton css file,
      // web.xml file
      FileCreator fileCreators[] = new FileCreator[] {
          new FileCreator(basePackageDir, "Module.gwt.xml", className
              + ModuleDefLoader.GWT_MODULE_XML_SUFFIX),
          new FileCreator(warDir, "AppHtml.html", className + ".html"),
          new FileCreator(warDir, "AppCss.css", className + ".css"),
          new FileCreator(webInfDir, "web.xml", "web.xml"),};
      for (FileCreator fileCreator : fileCreators) {
        File file = Utility.createNormalFile(fileCreator.dir,
            fileCreator.className, overwrite, ignore);
        if (file != null) {
          String out = Utility.getFileFromClassPath(PACKAGE_PATH
              + fileCreator.sourceName + "src");
          Utility.writeTemplateFile(file, out, replacements);
        }
      }
    }

    {
      /*
       * Create a skeleton Application: main client class, rpc stub for the
       * client, async counterpart of the rpc stub, rpc implementation on the
       * server.
       */
      FileCreator fileCreators[] = new FileCreator[] {
          new FileCreator(clientDir, "AppClass", className),
          new FileCreator(clientDir, "RpcClient", "EchoService"),
          new FileCreator(clientDir, "RpcAsyncClient", "EchoServiceAsync"),
          new FileCreator(serverDir, "RpcServer", "EchoServiceImpl"),};
      for (FileCreator fileCreator : fileCreators) {
        File javaClass = Utility.createNormalFile(fileCreator.dir,
            fileCreator.className + ".java", overwrite, ignore);
        if (javaClass != null) {
          String out = Utility.getFileFromClassPath(PACKAGE_PATH
              + fileCreator.sourceName + "Template.javasrc");
          Utility.writeTemplateFile(javaClass, out, replacements);
        }
      }
    }

    if (eclipse != null) {
      replacements.put("@projectName", eclipse);
      // Build the list of extra paths
      replacements.put("@gwtServletPath", basePathEnv + gwtServletPath);
      StringBuilder buf = new StringBuilder();
      if (extraClassPaths != null) {
        for (String path : extraClassPaths) {
          buf.append("    <pathelement path=\"" + path + "\"/>");
        }
      }
      replacements.put("@extraAntPathElements", buf.toString());

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

      /*
       * create an ant file, an eclipse .project, an eclipse .classpath, and an
       * eclipse launch-config
       */
      FileCreator fileCreators[] = new FileCreator[] {
          new FileCreator(outDir, "project.ant.xml", "build.xml"),
          new FileCreator(outDir, ".project", ".project"),
          new FileCreator(outDir, ".classpath", ".classpath"),
          new FileCreator(outDir, "App.launch", className + ".launch"),};
      for (FileCreator fileCreator : fileCreators) {
        File file = Utility.createNormalFile(fileCreator.dir,
            fileCreator.className, overwrite, ignore);
        if (file != null) {
          String out = Utility.getFileFromClassPath(PACKAGE_PATH
              + fileCreator.sourceName + "src");
          Utility.writeTemplateFile(file, out, replacements);
        }
      }
    }
  }

  private static String createExtraModuleInherits(List<String> modules) {
    if (modules == null) {
      return "";
    }
    // Create an <inherits> tag in the gwt.xml file for each extra module
    StringBuilder buf = new StringBuilder();
    for (String module : modules) {
      buf.append("      <inherits name=\"");
      buf.append(module);
      buf.append("\" />\n");
    }
    return buf.toString();
  }

  private ArgHandlerAddToClassPath classPathHandler = new ArgHandlerAddToClassPath();
  private String eclipse = null;
  private String fullClassName = null;
  private boolean ignore = false;
  private ArgHandlerAddModule moduleHandler = new ArgHandlerAddModule();
  private File outDir;
  private boolean overwrite = false;
  private String newModuleName = null;

  protected ApplicationCreator() {

    registerHandler(new ArgHandlerEclipse() {
      @Override
      public String getPurpose() {
        return "Creates an ant file, an eclipse project, and a launch config";
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
          System.err.println("-overwrite cannot be used with -ignore");
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
          System.err.println("-ignore cannot be used with -overwrite");
          return false;
        }
        ignore = true;
        return true;
      }
    });

    // handler to process newModuleName argument
    registerHandler(new ArgHandlerString() {
      @Override
      public String[] getDefaultArgs() {
        return null; // later reset to moduleName
      }

      @Override
      public String getPurpose() {
        return "Specifies the new name of the module";
      }

      @Override
      public String getTag() {
        return "-moduleName";
      }

      @Override
      public String[] getTagArgs() {
        return new String[] {"moduleName"};
      }

      @Override
      public boolean setString(String str) {
        newModuleName = str;
        return true;
      }
    });

    registerHandler(new ArgHandlerAppClass());
    registerHandler(classPathHandler);
    registerHandler(moduleHandler);
  }

  protected boolean run() {
    try {
      createApplication(fullClassName, outDir, eclipse, overwrite, ignore,
          classPathHandler.getExtraClassPathList(),
          moduleHandler.getExtraModuleList(), newModuleName);
      return true;
    } catch (IOException e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      return false;
    }
  }

}
