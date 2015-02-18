/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.util;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.util.tools.shared.Md5Utils;
import com.google.gwt.util.tools.shared.StringUtils;

import java.io.File;
import java.util.List;

/**
 * General utility functions for disk caching.
 */
public class DiskCachingUtil {

  /**
   * Computes and returns a consistent preferred cache dir based on the given set of module names
   * and the current working directory.
   * <p>
   * Using a consistent cache dir has performance advantages since caches can be reused between JVM
   * process launches.
   */
  public static synchronized File computePreferredCacheDir(List<String> moduleNames,
      TreeLogger logger) {
    String tempDir = System.getProperty("java.io.tmpdir");
    String currentWorkingDirectory = System.getProperty("user.dir");
    String preferredCacheDirName = "gwt-cache-" + StringUtils.toHexString(
        Md5Utils.getMd5Digest(currentWorkingDirectory + Joiner.on(", ").join(moduleNames)));

    File preferredCacheDir = new File(tempDir, preferredCacheDirName);
    if (!preferredCacheDir.exists() && !preferredCacheDir.mkdir()) {
      logger.log(TreeLogger.WARN, "Can't create cache directory: " + preferredCacheDir);
      return null;
    }
    return preferredCacheDir;
  }
}
