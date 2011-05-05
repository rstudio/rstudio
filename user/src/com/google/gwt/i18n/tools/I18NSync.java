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
package com.google.gwt.i18n.tools;

import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.ConstantsWithLookup;
import com.google.gwt.i18n.client.Localizable;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.rebind.AbstractLocalizableInterfaceCreator;
import com.google.gwt.i18n.rebind.ConstantsInterfaceCreator;
import com.google.gwt.i18n.rebind.MessagesInterfaceCreator;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.ToolBase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

/**
 * Common public access point for localization support methods.
 */
public class I18NSync extends ToolBase {

  private class classNameArgHandler extends ArgHandlerExtra {

    @Override
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

    @Override
    public String getPurpose() {
      return "Identifies the Constants/Messages class to be created.  For example com.google.sample.i18n.client.Colors";
    }

    @Override
    public String[] getTagArgs() {
      String[] interfaceArg = {"name of the Constants/Messages interface to create"};
      return interfaceArg;
    }

    @Override
    public boolean isRequired() {
      return true;
    }
  }

  private class outDirHandler extends ArgHandlerString {
    @Override
    public String getPurpose() {
      return "Java source directory, defaults to the resource's class path.";
    }

    @Override
    public String getTag() {
      return "-out";
    }

    @Override
    public String[] getTagArgs() {
      String[] resourceArgs = {"fileName"};
      return resourceArgs;
    }

    @Override
    public boolean isRequired() {
      return false;
    }

    @Override
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
   * Creates a <code>Constants</code> interface from a class name. The
   * resource file needed to create the class must be on your class path.
   *
   * @param className the name of the Constants class to be created
   * @param outDir source dir root
   * @throws IOException
   */
  public static void createConstantsInterfaceFromClassName(String className,
      File outDir) throws IOException {
    createConstantsInterfaceFromClassName(className, outDir, Constants.class);
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
    createConstantsInterfaceFromClassName(className, null,
        ConstantsWithLookup.class);
  }

  /**
   * Creates a <code>ConstantsWithLookup</code> interface from a class name.
   * The resource file needed to create the class must be on your class path.
   *
   * @param className the name of the Constants class to be created
   * @param sourceDir source dir root
   * @throws IOException
   */
  public static void createConstantsWithLookupInterfaceFromClassName(
      String className, File sourceDir) throws IOException {
    createConstantsInterfaceFromClassName(className, sourceDir,
        ConstantsWithLookup.class);
  }

  /**
   * Creates one of a Messages, ConstantsWithLookup, or Constants subclass.
   *
   * @param className Name of the subclass to be created
   * @param sourceDir source directory root
   * @param interfaceType What kind of base class to use
   * @throws IOException
   */
  public static void createInterfaceFromClassName(String className,
      File sourceDir, Class<? extends Localizable> interfaceType)
      throws IOException {
    if (interfaceType == Messages.class) {
      createMessagesInterfaceFromClassName(className, sourceDir);
    } else {
      if (!Constants.class.isAssignableFrom(interfaceType)) {
        throw new RuntimeException(
            "Internal Error: Unable to create i18n class derived from " +
            interfaceType.getName());
      }
      createConstantsInterfaceFromClassName(className, sourceDir,
          interfaceType.asSubclass(Constants.class));
    }
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
    createMessagesInterfaceFromClassName(className, null);
  }

  /**
   * Creates a <code>Messages</code> interface from a class name. The resource
   * file needed to create the class must be on your class path.
   *
   * @param className the name of the Constants class to be created
   * @param sourceDir source directory root
   * @throws IOException
   */
  public static void createMessagesInterfaceFromClassName(String className,
      File sourceDir) throws IOException {
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
    AbstractLocalizableInterfaceCreator creator = new MessagesInterfaceCreator(
        name, packageName, resource, source);
    creator.generate();
  }

  /**
   * Creates Messages and Constants java source files.
   *
   * @param args arguments for generation
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

  private static void createConstantsInterfaceFromClassName(String className,
      File sourceDir, Class<? extends Constants> interfaceClass)
      throws IOException {
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
    AbstractLocalizableInterfaceCreator creator = new ConstantsInterfaceCreator(
        name, packageName, resource, source, interfaceClass);
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
      throws IOException {
    if (className.endsWith(".java") || className.endsWith(".properties")
        || className.endsWith(".class")
        || className.indexOf(File.separator) > 0) {
      throw new IllegalArgumentException(
          "class '"
              + className
              + "'should not contain an extension. \"com.google.gwt.SomeClass\" is an example of a correctly formed class string");
    }
    String resourcePath = className.replace('.', '/') + ".properties";
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      cl = ClassLoader.getSystemClassLoader();
    }
    URL r = cl.getResource(resourcePath);
    if (r == null) {
      throw new FileNotFoundException("Could not find the resource '"
          + resourcePath + " matching '" + className
          + "' did you remember to add it to your classpath?");
    }
    File resourceFile = new File(URLDecoder.decode(r.getPath(), "utf-8"));
    return resourceFile;
  }

  private ArgHandlerValueChooser chooser;
  private String classNameArg;
  private File outDirArg;

  private I18NSync() {
    registerHandler(new classNameArgHandler());
    registerHandler(new outDirHandler());
    chooser = new ArgHandlerValueChooser();
    registerHandler(chooser.getConstantsWithLookupArgHandler());
    registerHandler(chooser.getMessagesArgHandler());
  }

  /**
   * Creates the interface.
   *
   * @return whether the interface was created
   */
  protected boolean run() {
    try {
      createInterfaceFromClassName(classNameArg, outDirArg,
          chooser.getArgValue());
      return true;
    } catch (Throwable e) {
      System.err.println(e.getMessage());
      return false;
    }
  }
}
