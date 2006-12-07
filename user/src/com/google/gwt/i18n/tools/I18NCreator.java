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
package com.google.gwt.i18n.tools;

import com.google.gwt.user.tools.util.ArgHandlerEclipse;
import com.google.gwt.user.tools.util.ArgHandlerIgnore;
import com.google.gwt.user.tools.util.ArgHandlerOverwrite;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerOutDir;
import com.google.gwt.util.tools.ToolBase;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Command line assistant for i18n.
 */

public final class I18NCreator extends ToolBase {

  /**
   * Utility class to handle class name argument.
   * 
   */
  protected class ArgHandlerClassName extends ArgHandlerExtra {

    public boolean addExtraArg(String arg) {
      if (fullInterfaceName != null) {
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

      fullInterfaceName = arg;
      return true;
    }

    public String getPurpose() {
      return "The fully qualified name of the interface to create";
    }

    public String[] getTagArgs() {
      return new String[] {"interfaceName"};
    }

    public boolean isRequired() {
      return true;
    }
  }

  private static final String PACKAGE_PATH;

  static {
    String path = I18NCreator.class.getName();
    path = path.substring(0, path.lastIndexOf('.') + 1);
    PACKAGE_PATH = path.replace('.', '/');
  }

  public static void main(String[] args) {
    I18NCreator creator = new I18NCreator();
    if (creator.processArgs(args)) {
      if (creator.run()) {
        return;
      }
    }

    System.exit(1);
  }

  /**
   * @param fullInterfaceName Name of the fully-qualified Java class to create
   *          as an Application.
   * @param outDir Where to put the output files
   * @param eclipse The name of a project to attach a .launch config to
   * @param overwrite Overwrite an existing files if they exist.
   * @param ignore Ignore existing files if they exist.
   * @throws IOException
   */
  static void createLocalizable(String fullInterfaceName, File outDir,
      String eclipse, boolean createMessagesInterface, boolean overwrite,
      boolean ignore) throws IOException {

    // Figure out the installation directory
    String installPath = Utility.getInstallPath();
    String gwtUserPath = installPath + '/' + "gwt-user.jar";
    String gwtDevPath = installPath + '/' + Utility.getDevJarName();

    // Figure out what platform we're on
    // 
    boolean isWindows = gwtDevPath.substring(gwtDevPath.lastIndexOf('/') + 1).indexOf(
        "windows") >= 0;

    // If the path from here to the install directory is relative, we need to
    // set specific "base" directory tags; this is for sample creation during
    // the
    // build.
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
    int pos = fullInterfaceName.lastIndexOf('.');
    String clientPackageName = fullInterfaceName.substring(0, pos);
    String interfaceName = fullInterfaceName.substring(pos + 1);

    // Compute module name and directories
    //
    pos = clientPackageName.lastIndexOf('.');
    File clientDir = Utility.getDirectory(outDir, "src", true);
    if (pos >= 0) {
      String clientPackage = clientPackageName.replace('.', '/');
      clientDir = Utility.getDirectory(clientDir, clientPackage, true);
    }

    // Create a map of replacements
    //
    Map replacements = new HashMap();
    replacements.put("@className", fullInterfaceName);
    replacements.put("@shortClassName", interfaceName);
    replacements.put("@gwtUserPath", basePathEnv + gwtUserPath);
    replacements.put("@gwtDevPath", basePathEnv + gwtDevPath);
    replacements.put("@compileClass", "com.google.gwt.dev.GWTCompiler");
    replacements.put("@i18nClass", "com.google.gwt.i18n.tools.I18NSync");
    if (createMessagesInterface) {
      replacements.put("@createMessages", "-createMessages");
    } else {
      replacements.put("@createMessages", "");
    }

    if (createMessagesInterface) {
      // Create a skeleton i18n messages properties class
      File i18nMessageProperties = Utility.createNormalFile(clientDir,
          interfaceName + ".properties", overwrite, ignore);
      if (i18nMessageProperties != null) {
        String out = Utility.getFileFromClassPath(PACKAGE_PATH
            + "i18nMessages.propertiessrc");
        Utility.writeTemplateFile(i18nMessageProperties, out, replacements);
      }
    } else {
      // Create a skeleton i18n constants properties class
      File i18nConstantProperties = Utility.createNormalFile(clientDir,
          interfaceName + ".properties", overwrite, ignore);
      if (i18nConstantProperties != null) {
        String out = Utility.getFileFromClassPath(PACKAGE_PATH
            + "i18nConstants.propertiessrc");
        Utility.writeTemplateFile(i18nConstantProperties, out, replacements);
      }
    }

    if (eclipse != null) {
      // Create an eclipse localizable creator launch config
      replacements.put("@projectName", eclipse);
      File updateLaunchConfig = Utility.createNormalFile(outDir, interfaceName
          + "-i18n" + ".launch", overwrite, ignore);
      if (updateLaunchConfig != null) {
        String out = Utility.getFileFromClassPath(PACKAGE_PATH
            + "I18N-update.launchsrc");
        Utility.writeTemplateFile(updateLaunchConfig, out, replacements);
      }
    }

    // create startup files
    String extension;
    if (isWindows) {
      extension = ".cmd";
    } else {
      extension = "";
    }
    File gwti18n = Utility.createNormalFile(outDir, interfaceName + "-i18n"
        + extension, overwrite, ignore);
    if (gwti18n != null) {
      String out = Utility.getFileFromClassPath(PACKAGE_PATH + "gwti18n"
          + extension + "src");
      Utility.writeTemplateFile(gwti18n, out, replacements);
      if (extension.length() == 0) {
        Runtime.getRuntime().exec("chmod u+x " + gwti18n.getAbsolutePath());
      }
    }
  }

  private boolean createMessagesInterface = false;

  private String eclipse = null;

  private String fullInterfaceName = null;
  private boolean ignore = false;
  private File outDir;
  private boolean overwrite = false;

  protected I18NCreator() {

    registerHandler(new ArgHandlerEclipse() {
      public String getPurpose() {
        return "Creates a i18n update launch config for the named eclipse project";
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

    registerHandler(new ArgHandlerFlag() {

      public String getPurpose() {
        return "Create scripts for a Messages interface "
            + "rather than a Constants one";
      }

      public String getTag() {
        return "-createMessages";
      }

      public boolean setFlag() {
        createMessagesInterface = true;
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

    registerHandler(new ArgHandlerClassName());
  }

  protected boolean run() {
    try {
      createLocalizable(fullInterfaceName, outDir, eclipse,
          createMessagesInterface, overwrite, ignore);
      return true;
    } catch (IOException e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      return false;
    }
  }
}
