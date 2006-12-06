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
package com.google.gwt.dev.util;

import com.google.gwt.core.ext.TreeLogger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Creates a FileOracle based on a set of logical packages combined with either
 * a URLClassLoader. For each specified package, the ClassLoader is searched for
 * instances of that package as a directory. The results of this operation are
 * merged together into a single list of URLs whose order is determined by the
 * order of URLs in the ClassLoader. The relative order of different logical
 * packages originating from the same URL in the ClassLoader is undefined.
 * 
 * Once the sorted list of URLs is resolved, each URL is recursively searched to
 * index all of its files (optionally, that pass the given FileOracleFilter).
 * The results of this indexing are used to create the output FileOracle. Once
 * the FileOracle is created, its index is fixed and no longer depends on the
 * underlying URLClassLoader or file system. However, URLs returned from the
 * FileOracle may become invalid if the contents of the file system change.
 * 
 * Presently, only URLs beginning with* "file:" and "jar:file:" can be inspected
 * to index children. Any other types of URLs will generate a warning. The set
 * of children indexed by "jar:file:" type URLs is fixed at creation time, but
 * the set of children from "file:" type URLs will dynamically query the
 * underlying file system.
 */
public class FileOracleFactory {

  /**
   * Used to decide whether or not a resource name should be included in an
   * enumeration.
   */
  public interface FileFilter {
    boolean accept(String name);
  }

  /**
   * Implementation of a FileOracle as an ordered (based on class path) list of
   * abstract names (relative to some root), each mapped to a concrete URL.
   */
  private static final class FileOracleImpl extends FileOracle {

    /**
     * Creates a new FileOracle.
     * 
     * @param logicalNames An ordered list of abstract path name strings.
     * @param logicalToPhysical A map of every item in logicalNames onto a URL.
     */
    public FileOracleImpl(List logicalNames, Map logicalToPhysical) {
      this.logicalNames = (String[]) logicalNames.toArray(new String[logicalNames.size()]);
      this.logicalToPhysical = new HashMap(logicalToPhysical);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.google.gwt.dev.util.FileOracle#find(java.lang.String)
     */
    public URL find(String partialPath) {
      return (URL) logicalToPhysical.get(partialPath);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.google.gwt.dev.util.FileOracle#getAllFiles()
     */
    public String[] getAllFiles() {
      return logicalNames;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.google.gwt.dev.util.FileOracle#isEmpty()
     */
    public boolean isEmpty() {
      return logicalNames.length == 0;
    }

    private final String[] logicalNames;
    private final Map logicalToPhysical;
  }

  /**
   * Given a set of logical packages, finds every occurrence of each of those
   * packages within cl, and then sorts them relative to each other based on
   * classPathUrlList.
   * 
   * @param logger Logs the process.
   * @param cl Provides the underlying class path.
   * @param packageSet The input set of logical packages to search for and sort.
   * @param classPathUrlList The order in which to sort the results.
   * @param sortedUrls An output list to which urls are appended.
   * @param sortedPackages An output list to which logical packages are appended
   *          exactly corresponding to appends made to sortedUrls.
   * @param recordPackages If false, only empty strings are appended to
   *          sortedPackages.
   */
  private static void addPackagesInSortedOrder(TreeLogger logger,
      URLClassLoader cl, Map packageMap, List classPathUrlList,
      List sortedUrls, List sortedPackages, List sortedFilters,
      boolean recordPackages) {

    // Exhaustively find every package on the classpath in an unsorted fashion
    //
    List unsortedUrls = new ArrayList();
    List unsortedPackages = new ArrayList();
    List unsortedFilters = new ArrayList();
    for (Iterator itPkg = packageMap.keySet().iterator(); itPkg.hasNext();) {
      String curPkg = (String) itPkg.next();
      FileFilter curFilter = (FileFilter) packageMap.get(curPkg);
      try {
        Enumeration found = cl.findResources(curPkg);
        if (!recordPackages) {
          curPkg = "";
        }
        while (found.hasMoreElements()) {
          URL match = (URL) found.nextElement();
          unsortedUrls.add(match);
          unsortedPackages.add(curPkg);
          unsortedFilters.add(curFilter);
        }
      } catch (IOException e) {
        logger.log(TreeLogger.WARN, "Unexpected error searching classpath for "
          + curPkg, e);
      }
    }

    /*
     * Now sort the collected list by the proper class path order. This is an
     * O(N*M) operation, but it should be okay for what we're doing
     */

    // pre-convert the List of URL to String[] to speed up the inner loop below
    int c = unsortedUrls.size();
    String[] unsortedUrlStrings = new String[c];
    for (int i = 0; i < c; ++i) {
      unsortedUrlStrings[i] = unsortedUrls.get(i).toString();
      // strip the jar prefix for text matching purposes
      if (unsortedUrlStrings[i].startsWith("jar:")) {
        unsortedUrlStrings[i] = unsortedUrlStrings[i].substring(4);
      }
    }

    // now sort the URLs based on classPathUrlList
    for (Iterator itCp = classPathUrlList.iterator(); itCp.hasNext();) {
      URL curCpUrl = (URL) itCp.next();
      String curUrlString = curCpUrl.toExternalForm();
      // find all URLs that match this particular entry
      for (int i = 0; i < c; ++i) {
        if (unsortedUrlStrings[i].startsWith(curUrlString)) {
          sortedUrls.add(unsortedUrls.get(i));
          sortedPackages.add(unsortedPackages.get(i));
          sortedFilters.add(unsortedFilters.get(i));
        }
      }
    }
  }

  /**
   * Index all the children of a particular folder (recursively).
   * 
   * @param logger Logs the process.
   * @param filter If non-null, filters out which files get indexed.
   * @param stripBaseLen The number of characters to strip from the beginning of
   *          every child's file path when computing the logical name.
   * @param curDir The directory to index.
   * @param logicalNames An output List of Children found under this URL.
   * @param logicalToPhysical An output Map of Children found under this URL
   *          mapped to their concrete URLs.
   */
  private static void indexFolder(TreeLogger logger, FileFilter filter,
      int stripBaseLen, File curDir, List logicalNames, Map logicalToPhysical) {
    File[] files = curDir.listFiles();
    for (int i = 0; i < files.length; i++) {
      File f = files[i];
      if (f.exists()) {
        if (f.isDirectory()) {
          indexFolder(logger, filter, stripBaseLen, f, logicalNames,
            logicalToPhysical);
        } else if (f.isFile()) {
          try {
            String logicalName = f.getAbsolutePath().substring(stripBaseLen);
            logicalName = logicalName.replace(File.separatorChar, '/');
            if (logicalToPhysical.containsKey(logicalName)) {
              // this logical name is shadowed
              logger.log(TreeLogger.DEBUG, "Ignoring already-resolved "
                + logicalName, null);
              continue;
            }
            if (filter != null && !filter.accept(logicalName)) {
              // filtered out
              logger.log(TreeLogger.SPAM, "Filtered out " + logicalName, null);
              continue;
            }
            URL physicalUrl = f.toURL();
            logicalToPhysical.put(logicalName, physicalUrl);
            logicalNames.add(logicalName);
            logger.log(TreeLogger.TRACE, "Found " + logicalName, null);
          } catch (IOException e) {
            logger.log(TreeLogger.WARN, "Unexpected error resolving " + f, e);
          }
        }
      }
    }
  }

  /**
   * Index all the children in a particular folder of a jar.
   * 
   * @param logger Logs the process.
   * @param filter If non-null, filters out which files get indexed.
   * @param jarUrl The URL of the containing jar file.
   * @param jarFile The jarFile to index.
   * @param basePath The sub tree within the jarFile to index.
   * @param pkgBase If non-empty, causes the logical names of children to be
   *          shorter (rooting them higher in the tree).
   * @param logicalNames An output List of Children found under this URL.
   * @param logicalToPhysical An output Map of Children found under this URL
   *          mapped to their concrete URLs.
   */
  private static void indexJar(TreeLogger logger, FileFilter filter,
      String jarUrl, JarFile jarFile, String basePath, String pkgBase,
      List logicalNames, Map logicalToPhysical) {
    int prefixCharsToStrip = basePath.length() - pkgBase.length();
    for (Enumeration enumJar = jarFile.entries(); enumJar.hasMoreElements();) {
      JarEntry jarEntry = (JarEntry) enumJar.nextElement();
      String jarEntryName = jarEntry.getName();
      if (jarEntryName.startsWith(basePath) && !jarEntry.isDirectory()) {
        String logicalName = jarEntryName.substring(prefixCharsToStrip);
        String physicalUrlString = jarUrl + "!/" + jarEntryName;
        if (logicalToPhysical.containsKey(logicalName)) {
          // this logical name is shadowed
          logger.log(TreeLogger.DEBUG, "Ignoring already-resolved "
            + logicalName, null);
          continue;
        }
        if (filter != null && !filter.accept(logicalName)) {
          // filtered out
          logger.log(TreeLogger.SPAM, "Filtered out " + logicalName, null);
          continue;
        }
        try {
          URL physicalUrl = new URL(physicalUrlString);
          logicalToPhysical.put(logicalName, physicalUrl);
          logicalNames.add(logicalName);
          logger.log(TreeLogger.TRACE, "Found " + logicalName, null);
        } catch (MalformedURLException e) {
          logger.log(TreeLogger.WARN, "Unexpected error resolving "
            + physicalUrlString, e);
        }
      }
    }
  }

  /**
   * Finds all children of the specified URL and indexes them.
   * 
   * @param logger Logs the process.
   * @param filter If non-null, filters out which files get indexed.
   * @param url The URL to index, must be "file:" or "jar:file:"
   * @param pkgBase A prefix to exclude when indexing children.
   * @param logicalNames An output List of Children found under this URL.
   * @param logicalToPhysical An output Map of Children found under this URL
   *          mapped to their concrete URLs.
   * @throws URISyntaxException if an unexpected error occurs.
   * @throws IOException if an unexpected error occurs.
   */
  private static void indexURL(TreeLogger logger, FileFilter filter, URL url,
      String pkgBase, List logicalNames, Map logicalToPhysical)
      throws URISyntaxException, IOException {

    String urlString = url.toString();
    if (url.getProtocol().equals("file")) {
      URI uri = new URI(urlString);
      File f = new File(uri);
      if (f.isDirectory()) {
        int prefixCharsToStrip = f.getAbsolutePath().length() + 1
          - pkgBase.length();
        indexFolder(logger, filter, prefixCharsToStrip, f, logicalNames,
          logicalToPhysical);
      } else {
        // We can't handle files here, only directories. If this is a jar
        // reference, the url must come in as a "jar:file:<stuff>!/[stuff/]".
        // Fall through.
        logger.log(TreeLogger.WARN, "Unexpected error, " + f
          + " is neither a file nor a jar", null);
      }
    } else if (url.getProtocol().equals("jar")) {
      String path = url.getPath();
      int pos = path.indexOf('!');
      if (pos >= 0) {
        String jarPath = path.substring(0, pos);
        String dirPath = path.substring(pos + 2);
        URL jarURL = new URL(jarPath);
        if (jarURL.getProtocol().equals("file")) {
          URI jarURI = new URI(jarURL.toString());
          File f = new File(jarURI);
          JarFile jarFile = new JarFile(f);
          // From each child, strip off the leading classpath portion when
          // determining the logical name (sans the pkgBase name we want!)
          //
          indexJar(logger, filter, "jar:" + jarPath, jarFile, dirPath, pkgBase,
            logicalNames, logicalToPhysical);
        } else {
          logger.log(TreeLogger.WARN, "Unexpected error, jar at " + jarURL
            + " must be a file: type URL", null);
        }
      } else {
        throw new URISyntaxException(path, "Cannot locate '!' separator");
      }
    } else {
      logger.log(TreeLogger.WARN, "Unknown URL type for " + urlString, null);
    }
  }

  /**
   * Creates a FileOracleFactory with the default URLClassLoader.
   */
  public FileOracleFactory() {
    this((URLClassLoader) FileOracleFactory.class.getClassLoader());
  }

  /**
   * Creates a FileOracleFactory.
   * 
   * @param classLoader The underlying class path to use.
   */
  public FileOracleFactory(URLClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  /**
   * Adds a logical package to the product FileOracle. All instances of this
   * package that can be found in the underlying URLClassLoader will have their
   * their children indexed, relative to the class path entry on which they are
   * found.
   * 
   * @param packageAsPath For example, "com/google/gwt/core/client".
   */
  public void addPackage(String packageAsPath, FileFilter filter) {
    packageAsPath = ensureTrailingBackslash(packageAsPath);
    packages.put(packageAsPath, filter);
  }

  /**
   * Adds a logical root package to the product FileOracle. All instances of
   * this package that can be found in the underlying URLClassLoader will have
   * their their children indexed, relative to their location within
   * packageAsPath. All root packages trump all non-root packages when
   * determining the final precedence order.
   * 
   * @param packageAsPath For example, "com/google/gwt/core/client".
   */
  public void addRootPackage(String packageAsPath, FileFilter filter) {
    packageAsPath = ensureTrailingBackslash(packageAsPath);
    rootPackages.put(packageAsPath, filter);
  }

  /**
   * Creates the product FileOracle based on the logical packages previously
   * added.
   * 
   * @param logger Logs the process.
   * @return a new FileOracle.
   */
  public FileOracle create(TreeLogger logger) {

    // get the full expanded URL class path for sorting purposes
    //
    List classPathUrls = new ArrayList();
    for (ClassLoader curCL = classLoader; curCL != null; curCL = curCL.getParent()) {
      if (curCL instanceof URLClassLoader) {
        URLClassLoader curURLCL = (URLClassLoader) curCL;
        URL[] curURLs = curURLCL.getURLs();
        classPathUrls.addAll(Arrays.asList(curURLs));
      }
    }

    /*
     * Collect a sorted list of URLs corresponding to all of the logical
     * packages mapped onto the
     */

    // The list of
    List urls = new ArrayList();
    List pkgNames = new ArrayList();
    List filters = new ArrayList();

    // don't record package names for root packages, they are rebased
    addPackagesInSortedOrder(logger, classLoader, rootPackages, classPathUrls,
      urls, pkgNames, filters, false);
    // record package names for non-root packages
    addPackagesInSortedOrder(logger, classLoader, packages, classPathUrls,
      urls, pkgNames, filters, true);

    // We have a complete sorted list of mapped URLs with package prefixes

    // Setup data collectors
    List logicalNames = new ArrayList();
    Map logicalToPhysical = new HashMap();

    for (int i = 0, c = urls.size(); i < c; ++i) {
      try {
        URL url = (URL) urls.get(i);
        String pkgName = (String) pkgNames.get(i);
        FileFilter filter = (FileFilter) filters.get(i);
        TreeLogger branch = logger.branch(TreeLogger.TRACE, url.toString(),
          null);
        indexURL(branch, filter, url, pkgName, logicalNames, logicalToPhysical);
      } catch (URISyntaxException e) {
        logger.log(TreeLogger.WARN,
          "Unexpected error searching " + urls.get(i), e);
      } catch (IOException e) {
        logger.log(TreeLogger.WARN,
          "Unexpected error searching " + urls.get(i), e);
      }
    }

    return new FileOracleImpl(logicalNames, logicalToPhysical);
  }

  /**
   * Helper method to regularize packages.
   * 
   * @param packageAsPath For exmaple, "com/google/gwt/core/client"
   * @return For example, "com/google/gwt/core/client/"
   */
  private String ensureTrailingBackslash(String packageAsPath) {
    if (packageAsPath.endsWith("/")) {
      return packageAsPath;
    } else {
      return packageAsPath + "/";
    }
  }

  /**
   * The underlying classloader.
   */
  private final URLClassLoader classLoader;

  /**
   * A map of packages indexed from the root of the class path onto their
   * corresponding FileFilters.
   */
  private final Map packages = new HashMap();

  /**
   * A map of packages that become their own roots (that is their children are
   * indexed relative to them) onto their corresponding FileFilters.
   */
  private final Map rootPackages = new HashMap();
}
