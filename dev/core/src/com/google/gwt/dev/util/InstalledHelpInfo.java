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
package com.google.gwt.dev.util;

import com.google.gwt.core.ext.TreeLogger.HelpInfo;
import com.google.gwt.util.tools.Utility;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Help info based on the GWT installation directory.
 */
public class InstalledHelpInfo extends HelpInfo {
  private URL url;

  public InstalledHelpInfo(String htmlDocName) {
    try {
      String installPath = Utility.getInstallPath();
      File file = new File(installPath, "doc");
      file = new File(file, "helpInfo");
      file = new File(file, htmlDocName);
      if (file.isFile() && file.canRead()) {
        url = file.toURI().toURL();
      }
    } catch (RuntimeException e) {
      // Installation problem; just don't provide help info
    } catch (MalformedURLException e) {
      // Unexpected; just don't provide help info
    }
  }

  @Override
  public URL getURL() {
    return url;
  }
}
