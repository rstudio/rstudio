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

package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;

/**
 * Holds the expected version number for the hosted.html file and a check
 * for it.
 */
public class HostedHtmlVersion {
  /**
   * The version number that should be passed into gwtOnLoad. Must match the
   * version in hosted.html.
   */
  public static final String EXPECTED_GWT_ONLOAD_VERSION = "2.1";

  /**
   * Validate that the supplied hosted.html version matches.
   * 
   * This is to detect cases where users upgrade to a new version but forget to
   * update the generated hosted.html file.
   * 
   * @param logger to report errors on
   * @param version version supplied by hosted.html file
   * @return true if the version is valid, false otherwise
   */
  public static boolean validHostedHtmlVersion(TreeLogger logger,
      String version) {
    if (!EXPECTED_GWT_ONLOAD_VERSION.equals(version)) {
      logger.log(TreeLogger.ERROR,
          "Invalid version number \"" + version
              + "\" passed to external.gwtOnLoad(), expected \""
              + EXPECTED_GWT_ONLOAD_VERSION
              + "\"; your development mode bootstrap file may be out of date; "
              + "if you are using -noserver try recompiling and redeploying "
              + "your app; if you just switched to a different version of "
              + "GWT, try clearing your browser cache");
      return false;
    }
    return true;
  }
  
  // prevent instantiation
  private HostedHtmlVersion() {
  }
}
