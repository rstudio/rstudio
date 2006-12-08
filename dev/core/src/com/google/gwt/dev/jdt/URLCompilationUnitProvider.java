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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.dev.util.Util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Implements {@link CompilationUnitProvider} in terms of a URL.
 */
public class URLCompilationUnitProvider implements CompilationUnitProvider {

  private static File trySimplify(URL url) {
    String s = url.toExternalForm();
    File f = null;
    if (s.startsWith("file:")) {
      // Strip the file: off, and use the result. If the result
      // does not start with file, we cannot simplify. Using URI
      // to do the simplification fails for paths with spaces.
      // Any number of slashes at the beginning cause no problem for Java, so
      // if c:/windows exists so will ///////c:/windows.
      f = new File(s.substring(5));
      if (!f.exists()) {
        f = null;
      }
    } else {
      f = null;
    }
    return f;
  }

  private final File file;

  private final String location;

  private final String packageName;

  private char[] source;

  private long sourceCurrentTime = Long.MIN_VALUE;

  private final URL url;

  public URLCompilationUnitProvider(URL url, String packageName) {
    assert (url != null);
    assert (packageName != null);
    this.url = url;

    // Files are faster to work with, so use file if available.
    this.file = trySimplify(url);
    if (file == null) {
      this.location = url.toExternalForm();
    } else {
      this.location = this.file.getAbsolutePath();
    }
    this.packageName = packageName;
  }

  public long getLastModified() throws UnableToCompleteException {
    try {
      if (file != null) {
        return file.lastModified();
      } else {
        String converted = Util.findFileName(location);
        if (converted != location) {
          return new File(converted).lastModified();
        }
        URLConnection conn = url.openConnection();
        return conn.getLastModified();
      }
    } catch (IOException e) {
      throw new UnableToCompleteException();
    }
  }

  public String getLocation() {
    return location;
  }

  public String getPackageName() {
    return packageName;
  }

  public char[] getSource() throws UnableToCompleteException {
    long lastModified = getLastModified();
    if (sourceCurrentTime >= lastModified && source != null) {
      return source;
    } else {
      sourceCurrentTime = lastModified;
    }
    if (file == null) {
      // Pre-read source.
      source = Util.readURLAsChars(url);
    } else {
      source = Util.readFileAsChars(file);
    }
    if (source == null) {
      throw new UnableToCompleteException();
    }
    return source;
  }

  public boolean isTransient() {
    return false;
  }

  public String toString() {
    return location;
  }
}
