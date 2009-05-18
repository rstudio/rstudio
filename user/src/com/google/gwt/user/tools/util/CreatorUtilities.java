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
package com.google.gwt.user.tools.util;

import com.google.gwt.dev.cfg.ModuleDefLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods used by ApplicationCreator.
 * 
 */
public class CreatorUtilities {

  /**
   * Create a PATH style string separated by the specified delimiter (';' for
   * windows, ':' for UNIX) Note that this method prepends the delimiter to the
   * front of the string. There is an existing path we want to append to.
   * 
   * @param delimiter The delimiter string to place between variables.
   * @param paths The list of paths to concatenate together.
   * @return the concatenated list of paths as a single string.
   */
  public static String appendPaths(String delimiter, List<String> paths) {
    if (paths == null) {
      return "";
    }
    StringBuilder buf = new StringBuilder();
    for (String value : paths) {
      buf.append(delimiter);
      buf.append(value);
    }
    return buf.toString();
  }

  /**
   * Create the extra path entries for an Eclipse '.launch' file in XML format.
   * 
   * @param extraClassPaths a list of paths/.jar files to add to the class path
   * @return A string formatted to include in the .launch file
   */
  public static String createEclipseExtraLaunchPaths(
      List<String> extraClassPaths) throws FileNotFoundException {

    if (extraClassPaths == null) {
      return "";
    }

    // Create an entry for an Eclipse launch file additional classpath entry.
    StringBuilder buf = new StringBuilder();
    for (String path : extraClassPaths) {
      File f = new File(path);

      if (!f.exists()) {
        throw new FileNotFoundException("extraClassPath: " + path
            + "Must be present before .launch file can be created");
      }

      String lcPath = path.toLowerCase();

      if (f.isDirectory()) {
        // For a directory, we assume it contains compiled class files
        buf.append("<listEntry value=\"&lt;?xml version=&quot;1.0&quot; ");
        buf.append("encoding=&quot;UTF-8&quot; standalone=&quot;no&quot;");
        buf.append("?&gt;&#13;&#10;&lt;runtimeClasspathEntry ");
        buf.append("internalArchive=&quot;");
        buf.append(path);
        buf.append("&quot; path=&quot;3&quot; type=&quot;2&quot;/&gt;&#13;&#10;\"/>");
        buf.append("\n");
      } else if (lcPath.endsWith(".jar") || lcPath.endsWith(".zip")) {
        // Any plain file we assume is an external library (e.g. a .jar file)
        buf.append("<listEntry value=\"&lt;?xml version=&quot;1.0&quot; ");
        buf.append("encoding=&quot;UTF-8&quot;?&gt;&#13;&#10;&lt;runtimeClasspathEntry ");
        buf.append("externalArchive=&quot;");
        buf.append(path);
        buf.append("&quot; path=&quot;3&quot; type=&quot;2&quot;/&gt;&#13;&#10;\"/>");
        buf.append("\n");
      } else {
        throw new RuntimeException("Don't know how to handle path: " + path
            + ". It doesn't appear to be a directory or a .jar/.zip file");
      }
    }
    return buf.toString();
  }

  /**
   * Returns <code>true</code> if <code>moduleName</code> is a valid module
   * name.
   */
  public static boolean isValidModuleName(String moduleName) {
    return moduleName.matches("[\\w]+(\\.[\\w]+)+");
  }

  /**
   * Check to see that the userJar and pathList files all exist, and that the
   * moduleList entries can be found within the jars.
   * 
   * @param userJar The full path to gwt-user.jar
   * @param pathList A list of jar files to add to the class path.
   * @param moduleList A list of GWT module names to add as 'inherits' tags
   * @return <code>true</code> if all validations pass.
   */
  public static boolean validatePathsAndModules(String userJar,
      List<String> pathList, List<String> moduleList) {
    List<URL> urlList = new ArrayList<URL>();

    if (!addURL(urlList, userJar)) {
      return false;
    }
    if (pathList != null) {
      for (String path : pathList) {
        if (!addURL(urlList, path)) {
          return false;
        }
      }
    }

    /*
     * Create a class loader from the extra class paths and the current class
     * loader. The assumption is that if the userJar isn't available right now,
     * that the current class loader will contain the same gwt.xml module def
     * files.
     */
    final URL urlArray[] = urlList.toArray(new URL[urlList.size()]);
    URLClassLoader classLoader = AccessController.doPrivileged(
        new PrivilegedAction<URLClassLoader>() {
      public URLClassLoader run() {
        return new URLClassLoader(urlArray,
            CreatorUtilities.class.getClassLoader());
      }
    });
    if (moduleList != null) {
      for (String module : moduleList) {
        String modulePath = module.replace(".", "/")
            + ModuleDefLoader.GWT_MODULE_XML_SUFFIX;
        URL found = classLoader.getResource(modulePath);
        if (found == null) {
          System.err.println("Couldn't find module definition file "
              + modulePath + " in class path.");
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Append a path to a list of URLs.
   * 
   * @param urls list to append to
   * @param pathToAdd string to append as the last entry in the URL list.
   * @return <code>true</code> on success. <code>false</code> if an error
   *         occurs (malformed URL or missing file.)
   */
  private static boolean addURL(List<URL> urls, String pathToAdd) {
    File f = new File(pathToAdd);

    // Ignore gwt-user.jar in the validation. This helps the build process
    // get by when overriding the location of the .jar with -Dgwt.devjar
    if (!pathToAdd.matches(".*gwt-user.jar") && !f.exists()) {
      System.err.println("Couldn't find library file or path " + pathToAdd);
      return false;
    }
    try {
      urls.add(f.toURI().toURL());
    } catch (MalformedURLException urlEx) {
      urlEx.printStackTrace();
      return false;
    }
    return true;
  }
}
