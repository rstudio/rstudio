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
package com.google.gwt.i18n.tools;

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.ConstantsWithLookup;
import com.google.gwt.i18n.client.Localizable;
import com.google.gwt.i18n.client.Messages;
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
 * Command line assistant for i18n.
 */

public final class I18NCreator extends ToolBase {

  /**
   * Utility class to handle class name argument.
   * 
   */
  protected class ArgHandlerClassName extends ArgHandlerExtra {

    @Override
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

    @Override
    public String getPurpose() {
      return "The fully qualified name of the interface to create";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {
        "interfaceName"
      };
    }

    @Override
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
   * @param interfaceToCreate the class instance to create - Constants,
   *          ConstantsWithLookup, or Messages
   * @throws IOException
   */
  static void createLocalizable(String fullInterfaceName, File outDir,
      String eclipse, boolean overwrite, boolean ignore,
      Class<? extends Localizable> interfaceToCreate) throws IOException {

    // Figure out the installation directory
    String installPath = Utility.getInstallPath();
    String gwtUserPath = installPath + '/' + "gwt-user.jar";
    String gwtDevPath = installPath + '/' + "gwt-dev.jar";

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
    Map<String, String> replacements = new HashMap<String, String>();
    replacements.put("@className", fullInterfaceName);
    replacements.put("@shortClassName", interfaceName);
    replacements.put("@gwtUserPath", basePathEnv + gwtUserPath);
    replacements.put("@gwtDevPath", basePathEnv + gwtDevPath);
    replacements.put("@compileClass", "com.google.gwt.dev.GWTCompiler");
    replacements.put("@i18nClass", "com.google.gwt.i18n.tools.I18NSync");

    // Add command line arguments to create
    // Messages/Constants/ConstantsWithLookup code.
    String templateData = null;

    if (Messages.class == interfaceToCreate) {
      replacements.put("@createMessages", "-createMessages");
      templateData = Utility.getFileFromClassPath(PACKAGE_PATH
          + "i18nMessages.propertiessrc");
    } else {
      if (ConstantsWithLookup.class == interfaceToCreate) {
        replacements.put("@createMessages", "-createConstantsWithLookup");
      } else if (Constants.class == interfaceToCreate) {
        replacements.put("@createMessages", "");
      } else {
        throw new RuntimeException(
            "Internal Error: Unable to create i18n class derived from "
                + interfaceToCreate.getName());
      }
      // This same template works for both Constants and ConstantsWithLookup
      // classes
      templateData = Utility.getFileFromClassPath(PACKAGE_PATH
          + "i18nConstants.propertiessrc");
    }

    // Populate the file from the template
    File i18nPropertiesFile = Utility.createNormalFile(clientDir, interfaceName
        + ".properties", overwrite, ignore);
    if (i18nPropertiesFile != null && templateData != null) {
      Utility.writeTemplateFile(i18nPropertiesFile, templateData, replacements);
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

  private ArgHandlerValueChooser chooser;
  private String eclipse = null;
  private String fullInterfaceName = null;
  private boolean ignore = false;
  private File outDir;
  private boolean overwrite = false;

  protected I18NCreator() {

    registerHandler(new ArgHandlerEclipse() {
      @Override
      public String getPurpose() {
        return "Creates a i18n update launch config for the named eclipse project";
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
    chooser = new ArgHandlerValueChooser();
    registerHandler(chooser.getConstantsWithLookupArgHandler());
    registerHandler(chooser.getMessagesArgHandler());

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

    registerHandler(new ArgHandlerClassName());
  }

  protected boolean run() {
    try {
      createLocalizable(fullInterfaceName, outDir, eclipse, overwrite, ignore,
          chooser.getArgValue());
      return true;
    } catch (IOException e) {
      System.err.println(e.getClass().getName() + ": " + e.getMessage());
      return false;
    }
  }
}
