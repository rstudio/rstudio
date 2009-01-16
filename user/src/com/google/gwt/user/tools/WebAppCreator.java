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

import com.google.gwt.dev.About;
import com.google.gwt.dev.ArgProcessorBase;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.HostedMode;
import com.google.gwt.dev.util.Util;
import com.google.gwt.user.tools.util.ArgHandlerIgnore;
import com.google.gwt.user.tools.util.ArgHandlerOverwrite;
import com.google.gwt.user.tools.util.CreatorUtilities;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerOutDir;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates a GWT application skeleton.
 */
public final class WebAppCreator {

  class ArgProcessor extends ArgProcessorBase {

    private final class ArgHandlerOutDirExtension extends ArgHandlerOutDir {
      @Override
      public void setDir(File dir) {
        outDir = dir;
      }
    }

    public ArgProcessor() {
      registerHandler(new ArgHandlerOverwriteExtension());
      registerHandler(new ArgHandlerIgnoreExtension());
      registerHandler(new ArgHandlerModuleName());
      registerHandler(new ArgHandlerOutDirExtension());
    }

    @Override
    protected String getName() {
      return WebAppCreator.class.getName();
    }
  }

  private final class ArgHandlerIgnoreExtension extends ArgHandlerIgnore {
    @Override
    public boolean setFlag() {
      if (overwrite) {
        System.err.println("-ignore cannot be used with -overwrite");
        return false;
      }
      ignore = true;
      return true;
    }
  }

  /*
   * Arguments for the application creator.
   */

  private final class ArgHandlerModuleName extends ArgHandlerExtra {
    @Override
    public boolean addExtraArg(String arg) {
      if (moduleName != null) {
        System.err.println("Too many arguments.");
        return false;
      }

      if (!CreatorUtilities.isValidModuleName(arg)) {
        System.err.println("'"
            + arg
            + "' does not appear to be a valid fully-qualified Java class name.");
        return false;
      }

      moduleName = arg;
      return true;
    }

    @Override
    public String getPurpose() {
      return "The name of the module to create (fully-qualified Java class name)";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"moduleName"};
    }

    @Override
    public boolean isRequired() {
      return true;
    }
  }

  private final class ArgHandlerOverwriteExtension extends ArgHandlerOverwrite {
    @Override
    public boolean setFlag() {
      if (ignore) {
        System.err.println("-overwrite cannot be used with -ignore");
        return false;
      }
      overwrite = true;
      return true;
    }
  }

  private static final class FileCreator {
    private final String destName;
    private final File destDir;
    private final String sourceName;

    public FileCreator(File destDir, String destName, String sourceName) {
      this.destDir = destDir;
      this.sourceName = sourceName;
      this.destName = destName;
    }
  }

  public static void main(String[] args) {
    WebAppCreator creator = new WebAppCreator();
    ArgProcessor argProcessor = creator.new ArgProcessor();
    if (argProcessor.processArgs(args)) {
      if (creator.run()) {
        return;
      }
    }
    System.exit(1);
  }

  /**
   * @param moduleName name of the fully-qualified GWT module to create as an
   *          Application.
   * @param outDir Where to put the output files
   * @param overwrite Overwrite an existing files if they exist.
   * @param ignore Ignore existing files if they exist.
   * @throws IOException
   */
  static void createApplication(String moduleName, File outDir,
      boolean overwrite, boolean ignore) throws IOException {

    // Figure out the installation directory
    String installPath = Utility.getInstallPath();
    String gwtUserPath = installPath + '/' + "gwt-user.jar";
    String gwtDevPath = installPath + '/' + Utility.getDevJarName();
    String gwtServletPath = installPath + '/' + "gwt-servlet.jar";

    // Public builds generate a DTD reference.
    String gwtModuleDtd = "";
    if (!About.GWT_VERSION_NUM.endsWith(".999")
        && !About.GWT_VERSION_NUM.startsWith("0.0")) {
      gwtModuleDtd = "\n<!DOCTYPE module PUBLIC \"-//Google Inc.//DTD Google Web Toolkit "
          + About.GWT_VERSION_NUM
          + "//EN\" \"http://google-web-toolkit.googlecode.com/svn/tags/"
          + About.GWT_VERSION_NUM + "/distro-source/core/src/gwt-module.dtd\">";
    }

    // Figure out what platform we're on
    boolean isMacOsX = gwtDevPath.substring(gwtDevPath.lastIndexOf('/') + 1).indexOf(
        "mac") >= 0;

    // Compute module package and name.
    int pos = moduleName.lastIndexOf('.');
    String modulePackageName = moduleName.substring(0, pos);
    String moduleShortName = moduleName.substring(pos + 1);

    // Compute module name and directories
    File srcDir = Utility.getDirectory(outDir, "src", true);
    File warDir = Utility.getDirectory(outDir, "war", true);
    File webInfDir = Utility.getDirectory(warDir, "WEB-INF", true);
    File moduleDir = Utility.getDirectory(srcDir, modulePackageName.replace(
        '.', '/'), true);
    File clientDir = Utility.getDirectory(moduleDir, "client", true);
    File serverDir = Utility.getDirectory(moduleDir, "server", true);

    // Create a map of replacements
    Map<String, String> replacements = new HashMap<String, String>();
    replacements.put("@moduleShortName", moduleShortName);
    replacements.put("@moduleName", moduleName);
    replacements.put("@clientPackage", modulePackageName + ".client");
    replacements.put("@serverPackage", modulePackageName + ".server");
    replacements.put("@gwtUserPath", gwtUserPath);
    replacements.put("@gwtDevPath", gwtDevPath);
    replacements.put("@gwtServletPath", gwtServletPath);
    replacements.put("@gwtVersion", About.GWT_VERSION_NUM);
    replacements.put("@gwtModuleDtd", gwtModuleDtd);
    replacements.put("@shellClass", HostedMode.class.getName());
    replacements.put("@compileClass", Compiler.class.getName());
    replacements.put("@startupUrl", moduleShortName + ".html");
    replacements.put("@antVmargs", isMacOsX
        ? "\n<jvmarg value=\"-XstartOnFirstThread\"/>" : "");
    replacements.put("@vmargs", isMacOsX ? "&#10;-XstartOnFirstThread" : "");
    replacements.put("@renameTo", moduleShortName.toLowerCase());

    List<FileCreator> files = new ArrayList<FileCreator>();
    files.add(new FileCreator(moduleDir, moduleShortName + ".gwt.xml",
        "Module.gwt.xml"));
    files.add(new FileCreator(warDir, moduleShortName + ".html", "AppHtml.html"));
    files.add(new FileCreator(warDir, moduleShortName + ".css", "AppCss.css"));
    files.add(new FileCreator(webInfDir, "web.xml", "web.xml"));
    files.add(new FileCreator(clientDir, moduleShortName + ".java",
        "AppClassTemplate.java"));
    files.add(new FileCreator(clientDir, "EchoService" + ".java",
        "RpcClientTemplate.java"));
    files.add(new FileCreator(clientDir, "EchoServiceAsync" + ".java",
        "RpcAsyncClientTemplate.java"));
    files.add(new FileCreator(serverDir, "EchoServiceImpl" + ".java",
        "RpcServerTemplate.java"));
    files.add(new FileCreator(outDir, "build.xml", "project.ant.xml"));
    files.add(new FileCreator(outDir, ".project", ".project"));
    files.add(new FileCreator(outDir, ".classpath", ".classpath"));
    files.add(new FileCreator(outDir, moduleShortName + ".launch", "App.launch"));

    for (FileCreator fileCreator : files) {
      File file = Utility.createNormalFile(fileCreator.destDir,
          fileCreator.destName, overwrite, ignore);
      if (file != null) {
        URL url = WebAppCreator.class.getResource(fileCreator.sourceName
            + "src");
        if (url == null) {
          throw new FileNotFoundException(fileCreator.sourceName + "src");
        }
        String data = Util.readURLAsString(url);
        Utility.writeTemplateFile(file, data, replacements);
      }
    }
  }

  private boolean ignore = false;
  private String moduleName;
  private File outDir;
  private boolean overwrite = false;

  protected boolean run() {
    try {
      createApplication(moduleName, outDir, overwrite, ignore);
      return true;
    } catch (IOException e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      return false;
    }
  }
}
