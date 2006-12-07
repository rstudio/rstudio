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

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.ConstantsWithLookup;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.rebind.LocalizableGenerator;
import com.google.gwt.i18n.rebind.util.AbstractLocalizableInterfaceCreator;
import com.google.gwt.i18n.rebind.util.ConstantsInterfaceCreator;
import com.google.gwt.i18n.rebind.util.MessagesInterfaceCreator;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.ToolBase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

/**
 * Common public access point for localization support methods.
 */
public class I18NSync extends ToolBase {

  private class classNameArgHandler extends ArgHandlerExtra {

    public boolean addExtraArg(String str) {
      if (classNameArg != null) {
        System.err.println("Too many arguments.");
        return false;
      }
      // We wish to use the same sets of checks for validity whether the user
      // calls the static method to create localizable fields or uses the
      // command line, as the java call must throw IOException, here we must
      // catch it and convert it to a System.err message.
      try {
        File resourceFile = urlToResourceFile(str);
        checkValidResourceInputFile(resourceFile);
        classNameArg = str;
      } catch (IOException e) {
        System.err.println("Error: " + e.getMessage());
        return false;
      }
      return true;
    }

    public String[] getDefaultArgs() {
      return null;
    }

    public String getPurpose() {
      return "Identifies the Constants/Messages class to be created.  For example com.google.sample.i18n.client.Colors";
    }

    public String[] getTagArgs() {
      String[] interfaceArg = {"name of the Constants/Messages interface to create"};
      return interfaceArg;
    }

    public boolean isRequired() {
      return true;
    }
  }

  private class outDirHandler extends ArgHandlerString {

    public String[] getDefaultArgs() {
      return null;
    }

    public String getPurpose() {
      return "Java source directory, defaults to the resource's class path.";
    }

    public String getTag() {
      return "-out";
    }

    public String[] getTagArgs() {
      String[] resourceArgs = {"fileName"};
      return resourceArgs;
    }

    public boolean isRequired() {
      return false;
    }

    public boolean setString(String str) {

      // We wish to use the same sets of checks for validity whether the user
      // calls the static method to create localizable classes or uses the
      // command line, as the java call must throw IOException, here we must
      // catch it and convert it to a System.err message.
      outDirArg = new File(str);
      try {
        checkValidSourceDir(outDirArg);
      } catch (IOException e) {
        System.err.println("Error: " + e.getMessage());
        return false;
      }
      return true;
    }
  }

  /**
   * Created Key.
   */
  public static final String ID = "@" + LocalizableGenerator.GWT_KEY + " ";

  /**
   * Creates a <code>Constants</code> interface from a class name. The
   * resource file needed to create the class must be on your class path.
   * 
   * @param className the name of the Constants class to be created
   * @param outDir source dir root
   * @throws IOException
   */
  public static void createConstantsInterfaceFromClassName(String className,
      File outDir) throws IOException {
    createLocalizableInterfaceFromClassName(className, outDir, Constants.class);
  }

  /**
   * Creates a <code>ConstantsWithLookup</code> interface from a class name.
   * The resource file needed to create the class must be on your class path.
   * 
   * @param className the name of the Constants class to be created
   * @throws IOException
   */
  public static void createConstantsWithLookupInterfaceFromClassName(
      String className) throws IOException {
    createLocalizableInterfaceFromClassName(className, null,
        ConstantsWithLookup.class);
  }

  /**
   * Creates a <code>ConstantsWithLookup</code> interface from a class name.
   * The resource file needed to create the class must be on your class path.
   * 
   * @param className the name of the Constants class to be created
   * @param outDir source dir root
   * @throws IOException
   */
  public static void createConstantsWithLookupInterfaceFromClassName(
      String className, File outDir) throws IOException {
    createLocalizableInterfaceFromClassName(className, outDir,
        ConstantsWithLookup.class);
  }

  /**
   * Creates a <code>Messages</code> interface from a class name. The resource
   * file needed to create the class must be on your class path.
   * 
   * @param className the name of the Constants class to be created
   * @throws IOException
   */
  public static void createMessagesInterfaceFromClassName(String className)
      throws IOException {
    createLocalizableInterfaceFromClassName(className, null, Messages.class);
  }

  /**
   * Creates a <code>Messages</code> interface from a class name. The resource
   * file needed to create the class must be on your class path.
   * 
   * @param className the name of the Constants class to be created
   * @param outDir source dir root
   * @throws IOException
   */
  public static void createMessagesInterfaceFromClassName(String className,
      File outDir) throws IOException {
    createLocalizableInterfaceFromClassName(className, outDir, Messages.class);
  }

  /**
   * Creates Messages and Constants java source files.
   * 
   * @param args arguements for generation
   */
  public static void main(String[] args) {
    I18NSync creator = new I18NSync();
    if (creator.processArgs(args)) {
      if (creator.run()) {
        return;
      }
    }
    System.exit(1);
  }

  static void checkValidJavaSourceOutputFile(File targetSource)
      throws IOException {

    if (targetSource.isDirectory()) {
      throw new IOException("Output file'" + targetSource
          + "' exists and is a directory; cannot overwrite");
    }
    if (targetSource.getParentFile().isDirectory() == false) {
      throw new IOException("The target source's directory '"
          + targetSource.getParent() + "' must be an existing directory");
    }
  }

  static void checkValidResourceInputFile(File resource) throws IOException {
    if (!resource.getPath().endsWith(".properties")) {
      throw new IOException("Properties files " + resource
          + " should end with '.properties'");
    }
    if (!resource.exists() || !resource.isFile()) {
      throw new IOException("Properties file not found: " + resource);
    }
  }

  private static void checkValidSourceDir(File outDir) throws IOException {
    if (outDir.isDirectory() == false) {
      throw new IOException(outDir
          + " must be an existing directory. Current path is "
          + new File(".").getCanonicalPath());
    }
  }

  private static void createLocalizableInterfaceFromClassName(String className,
      File sourceDir, Class interfaceClass) throws FileNotFoundException,
      IOException {
    File resource = urlToResourceFile(className);
    File source;
    if (sourceDir == null) {
      source = synthesizeSourceFile(resource);
    } else {
      checkValidSourceDir(sourceDir);
      String sourcePath = className.replace('.', File.separatorChar);
      sourcePath = sourceDir.getCanonicalFile() + File.separator + sourcePath
          + ".java";
      source = new File(sourcePath);
    }
    // Need both source path and class name for this check
    checkValidJavaSourceOutputFile(source);
    checkValidResourceInputFile(resource);

    int classDiv = className.lastIndexOf(".");
    String packageName = className.substring(0, classDiv);
    String name = className.substring(classDiv + 1);
    AbstractLocalizableInterfaceCreator creator;
    if (interfaceClass.equals(Messages.class)) {
      creator = new MessagesInterfaceCreator(name, packageName, resource,
          source);
    } else {
      creator = new ConstantsInterfaceCreator(name, packageName, resource,
          source, interfaceClass);
    }
    creator.generate();
  }

  private static File synthesizeSourceFile(File resource) {
    String javaPath = resource.getName();
    javaPath = javaPath.substring(0, javaPath.lastIndexOf("."));
    javaPath = resource.getParentFile().getPath() + File.separator + javaPath
        + ".java";
    File targetClassFile = new File(javaPath);
    return targetClassFile;
  }

  private static File urlToResourceFile(String className)
      throws FileNotFoundException, IOException {
    if (className.endsWith(".java") || className.endsWith(".properties")
        || className.endsWith(".class")
        || className.indexOf(File.separator) > 0) {
      throw new IllegalArgumentException(
          "class '"
              + className
              + "'should not contain an extension. \"com.google.gwt.SomeClass\" is an example of a correctly formed class string");
    }
    String resourcePath = className.replace('.', '/') + ".properties";
    URL r = ClassLoader.getSystemResource(resourcePath);
    if (r == null) {
      throw new FileNotFoundException("Could not find the resource '"
          + resourcePath + " matching '" + className
          + "' did you remember to add it to your classpath?");
    }
    File resourceFile = new File(r.getFile());
    return resourceFile;
  }

  private String classNameArg;

  private boolean createMessagesArg = false;

  private File outDirArg;

  private I18NSync() {
    registerHandler(new classNameArgHandler());
    registerHandler(new outDirHandler());
    registerHandler(new ArgHandlerFlag() {

      public String getPurpose() {
        return "create Messages interface rather than Constants interface.";
      }

      public String getTag() {
        return "-createMessages";
      }

      public boolean setFlag() {
        createMessagesArg = true;
        return true;
      }
    });
  }

  /**
   * Creates the interface.
   * 
   * @return whether the interface was created
   */
  protected boolean run() {
    try {
      Class createMe;
      if (createMessagesArg) {
        createMe = Messages.class;
      } else {
        createMe = Constants.class;
      }
      createLocalizableInterfaceFromClassName(classNameArg, outDirArg, createMe);
      return true;
    } catch (Throwable e) {
      System.err.println(e.getMessage());
      return false;
    }
  }
}
