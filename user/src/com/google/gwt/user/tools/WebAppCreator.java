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
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerOutDir;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
      registerHandler(new ArgHandlerNoEclipse());
      registerHandler(new ArgHandlerOnlyEclipse());
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
      return "The name of the module to create (e.g. com.example.myapp.MyApp)";
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

  private final class ArgHandlerNoEclipse extends ArgHandlerFlag {
    @Override
    public String getPurpose() {
      return "Do not generate eclipse files";
    }

    @Override
    public String getTag() {
      return "-XnoEclipse";
    }

    @Override
    public boolean isUndocumented() {
      return true;
    }

    @Override
    public boolean setFlag() {
      noEclipse = true;
      return true;
    }
  }

  private final class ArgHandlerOnlyEclipse extends ArgHandlerFlag {
    @Override
    public String getPurpose() {
      return "Generate only eclipse files";
    }

    @Override
    public String getTag() {
      return "-XonlyEclipse";
    }

    @Override
    public boolean isUndocumented() {
      return true;
    }

    @Override
    public boolean setFlag() {
      onlyEclipse = true;
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
    private final File destDir;
    private final String destName;
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

  private boolean ignore = false;
  private String moduleName;
  private boolean noEclipse;
  private boolean onlyEclipse;
  private File outDir;
  private boolean overwrite = false;

  /**
   * Create the sample app.
   * 
   * @throws IOException if any disk write fails
   */
  protected void doRun() throws IOException {

    // Figure out the installation directory
    String installPath = Utility.getInstallPath();
    String gwtUserPath = installPath + '/' + "gwt-user.jar";
    String gwtDevPath = installPath + '/' + "gwt-dev.jar";
    String gwtServletPath = installPath + '/' + "gwt-servlet.jar";
    String gwtOophmPath = installPath + '/' + "gwt-dev-oophm.jar";

    // Public builds generate a DTD reference.
    String gwtModuleDtd = "";
    int gwtVersion[] = About.getGwtVersionArray();
    if (gwtVersion[2] == 999
        && !(gwtVersion[0] == 0 && gwtVersion[1] == 0)) {
      gwtModuleDtd = "\n<!DOCTYPE module PUBLIC \"-//Google Inc.//DTD Google Web Toolkit "
          + About.getGwtVersionNum()
          + "//EN\" \"http://google-web-toolkit.googlecode.com/svn/tags/"
          + About.getGwtVersionNum() + "/distro-source/core/src/gwt-module.dtd\">";
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
    File libDir = Utility.getDirectory(webInfDir, "lib", true);
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
    replacements.put("@gwtSdk", installPath);
    replacements.put("@gwtUserPath", gwtUserPath);
    replacements.put("@gwtDevPath", gwtDevPath);
    replacements.put("@gwtOophmPath", gwtOophmPath);
    replacements.put("@gwtVersion", About.getGwtVersionNum());
    replacements.put("@gwtModuleDtd", gwtModuleDtd);
    replacements.put("@shellClass", HostedMode.class.getName());
    replacements.put("@compileClass", Compiler.class.getName());
    replacements.put("@startupUrl", moduleShortName + ".html");
    replacements.put("@antVmargs", isMacOsX
        ? "\n<jvmarg value=\"-XstartOnFirstThread\"/>" : "");
    replacements.put("@vmargs", isMacOsX ? "&#10;-XstartOnFirstThread" : "");
    replacements.put("@renameTo", moduleShortName.toLowerCase());

    String antEclipseRule = "";
    if (noEclipse) {
      /*
       * Generate a rule into the build file that allows for the generation of
       * an eclipse project later on. This is primarily for distro samples. This
       * is a quick and dirty way to inject a build rule, but it works.
       */
      antEclipseRule = "\n\n"
          + "  <target name=\"eclipse.generate\" depends=\"libs\" description=\"Generate eclipse project\">\n"
          + "    <java failonerror=\"true\" fork=\"true\" classname=\""
          + this.getClass().getName() + "\">\n" + "      <classpath>\n"
          + "        <path refid=\"project.class.path\"/>\n"
          + "      </classpath>\n" + "      <arg value=\"-XonlyEclipse\"/>\n"
          + "      <arg value=\"-ignore\"/>\n" + "      <arg value=\""
          + moduleName + "\"/>\n" + "    </java>\n" + "  </target>";
    } else {
      antEclipseRule = "";
    }
    replacements.put("@antEclipseRule", antEclipseRule);

    List<FileCreator> files = new ArrayList<FileCreator>();
    List<FileCreator> libs = new ArrayList<FileCreator>();
    if (!onlyEclipse) {
      files.add(new FileCreator(moduleDir, moduleShortName + ".gwt.xml",
          "Module.gwt.xml"));
      files.add(new FileCreator(warDir, moduleShortName + ".html",
          "AppHtml.html"));
      files.add(new FileCreator(warDir, moduleShortName + ".css", "AppCss.css"));
      files.add(new FileCreator(webInfDir, "web.xml", "web.xml"));
      files.add(new FileCreator(clientDir, moduleShortName + ".java",
          "AppClassTemplate.java"));
      files.add(new FileCreator(clientDir, "GreetingService" + ".java",
          "RpcClientTemplate.java"));
      files.add(new FileCreator(clientDir, "GreetingServiceAsync" + ".java",
          "RpcAsyncClientTemplate.java"));
      files.add(new FileCreator(serverDir, "GreetingServiceImpl" + ".java",
          "RpcServerTemplate.java"));
      files.add(new FileCreator(outDir, "build.xml", "project.ant.xml"));
      files.add(new FileCreator(outDir, "README.txt", "README.txt"));
    }
    if (!noEclipse) {
      assert new File(gwtDevPath).isAbsolute();
      libs.add(new FileCreator(libDir, "gwt-servlet.jar", gwtServletPath));
      files.add(new FileCreator(outDir, ".project", ".project"));
      files.add(new FileCreator(outDir, ".classpath", ".classpath"));
      files.add(new FileCreator(outDir, moduleShortName + ".launch",
          "App.launch"));
    }

    // copy source files, replacing the content as needed
    for (FileCreator fileCreator : files) {
      URL url = WebAppCreator.class.getResource(fileCreator.sourceName + "src");
      if (url == null) {
        throw new FileNotFoundException(fileCreator.sourceName + "src");
      }
      File file = Utility.createNormalFile(fileCreator.destDir,
          fileCreator.destName, overwrite, ignore);
      if (file != null) {
        String data = Util.readURLAsString(url);
        Utility.writeTemplateFile(file, data, replacements);
      }
    }

    // copy libs directly
    for (FileCreator fileCreator : libs) {
      FileInputStream is = new FileInputStream(fileCreator.sourceName);
      File file = Utility.createNormalFile(fileCreator.destDir,
          fileCreator.destName, overwrite, ignore);
      if (file != null) {
        FileOutputStream os = new FileOutputStream(file);
        Util.copy(is, os);
      }
    }
  }

  protected boolean run() {
    try {
      doRun();
      return true;
    } catch (IOException e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      return false;
    }
  }
}
