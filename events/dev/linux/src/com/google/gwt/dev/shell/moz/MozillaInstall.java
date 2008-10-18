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
package com.google.gwt.dev.shell.moz;

import com.google.gwt.util.tools.Utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Utility class to represent a Mozilla installation, including its installation
 * directory and a load order file. Instances are immutable, and static helper
 * functions are provided to create instances.
 */
public class MozillaInstall {

  // Default hosted browser config file.
  private static final String CONFIG_FILENAME = "mozilla-hosted-browser.conf";

  // Default load order file (optional).
  private static final String LOAD_ORDER_CONFIG_FILE = "gwt-dl-loadorder.conf";

  // Mozilla installation which was actually loaded.
  private static MozillaInstall loadedInstallation = null;

  /**
   * Find a suitable mozilla install for GWT hosted mode.
   * 
   * This is done by reading the mozilla-hosted-browser.conf in the install
   * directory, which contains one or more entries, each describing a Mozilla
   * install to try and load. The first suitable (present and compiled against
   * GTK2 instead of GTK1) one found is used.
   * 
   * Blank lines and lines beginning with # are ignored. The install directory
   * is prepended to non-absolute paths.
   * 
   * @return a MozillaInstall instance or <code>null</code> if none could be
   *         found
   */
  public static MozillaInstall find() {
    String installPath = Utility.getInstallPath();
    try {
      // try to make absolute
      installPath = new File(installPath).getCanonicalPath();
    } catch (IOException e) {
      /*
       * We might have an exception here if a parent directory is not readable,
       * so leave it non-absolute and hope the relative path works. If this
       * fails, the libraries will fail to load when SWT initializes.
       */
    }

    /*
     * Read from the mozilla-hosted-browser.conf file and try to find a suitable
     * mozilla installation. Return immediately if a suitable one is found.
     */
    try {
      BufferedReader reader = new BufferedReader(new FileReader(installPath
          + "/" + CONFIG_FILENAME));
      try { // make sure we close the reader
        String mozillaDir;
        while ((mozillaDir = reader.readLine()) != null) {
          if (mozillaDir.startsWith("#") || mozillaDir.matches("^\\s*$")) {
            // lines beginning with # are comments, ignore blank lines
            continue;
          }
          if (!mozillaDir.startsWith("/")) {
            mozillaDir = installPath + "/" + mozillaDir;
          }
          MozillaInstall mozInstall = new MozillaInstall(mozillaDir);
          if (mozInstall.isAcceptable()) {
            return mozInstall;
          }
        }
      } finally {
        reader.close();
      }
    } catch (FileNotFoundException e) {
      // fall through to common error-path code
    } catch (IOException e) {
      // fall through to common error-path code
    }

    // if we get here, nothing worked. Return failure.
    return null;
  }

  /**
   * @return the installation which was loaded, or null if none
   */
  public static MozillaInstall getLoaded() {
    return loadedInstallation;
  }

  /**
   * The absolute path to a directory containing a Mozilla install.
   */
  private String path;

  /**
   * Create a new instance. Private since this should only be created via the
   * find factory method.
   * 
   * @param path absolute path to the directory containing the Mozilla install
   */
  private MozillaInstall(String path) {
    this.path = path;
  }

  /**
   * @return the path of this Mozilla install
   */
  public String getPath() {
    return path;
  }

  /**
   * Load any libraries required for this Mozilla install, reading from the
   * loadOrderFile.
   * 
   * The format of the load order configuration file is simply one library path
   * per line (if non-absolute, the install directory is prepended) to be
   * loaded. This is necessary because we have to forcibly load some libraries
   * to make sure they are used instead of system-supplied libraries.
   * 
   * Blank lines and lines beginning with # are ignored.
   * 
   * @throws UnsatisfiedLinkError if libxpcom.so failed to load
   */
  public void load() {
    try {
      /*
       * Read the library load order configuration file to get the list of
       * libraries which must be preloaded. This is required because Eclipse
       * seems to always add the system mozilla directory to the
       * java.library.path, which results in loading the system-supplied
       * libraries to fulfill dependencies rather than the ones we supply. So,
       * to get around this we preload individual libraries in a particular
       * order to make sure they get pulled in properly.
       * 
       * The file has one line per library, each giving either an absolute path
       * or a path relative to the browser directory, in the order the libraries
       * should be loaded.
       */
      String loadOrderFile = path + "/" + LOAD_ORDER_CONFIG_FILE;
      BufferedReader reader = new BufferedReader(new FileReader(loadOrderFile));
      try {
        // read each line and load the library specified
        String library;
        while ((library = reader.readLine()) != null) {
          if (library.startsWith("#") || library.matches("^\\s*$")) {
            // lines beginning with # are comments, ignore blank lines
            continue;
          }
          if (!library.startsWith("/")) {
            library = path + "/" + library;
          }
          System.load(library);
        }
      } finally {
        reader.close();
      }
    } catch (FileNotFoundException e) {
      // ignore error, assume system will load libraries properly
    } catch (IOException e) {
      // ignore error, assume system will load libraries properly
    }
    /*
     * Forcibly load libxpcom.so. Since it has to be loaded before SWT loads its
     * swt-mozilla library, load it here. This will also cause a failure early
     * in the process if we have a problem rather than waiting until SWT starts
     * up.
     */
    System.load(path + "/libxpcom.so");
    loadedInstallation = this;
  }

  /**
   * Checks to see if the specified path refers to an acceptable Mozilla
   * install.
   * 
   * In this case, acceptable means it is present and was not compiled against
   * GTK1.
   * 
   * Note: Embedding a Mozilla GTK1.2 causes a crash. The workaround is to check
   * the version of GTK used by Mozilla by looking for the libwidget_gtk.so
   * library used by Mozilla GTK1.2. Mozilla GTK2 uses the libwidget_gtk2.so
   * library.
   * 
   * @return <code>true</code> if this Mozilla installation is acceptable for
   *         use with GWT
   */
  private boolean isAcceptable() {
    // make sure the main library exists
    if (!new File(path, "libxpcom.so").exists()) {
      return false;
    }
    // check to see if it was built against GTK1 instead of GTK2
    if (new File(path, "components/libwidget_gtk.so").exists()) {
      return false;
    }
    return true;
  }
}
