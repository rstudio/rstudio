/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

/**
 * The backing file for a {@link Precompilation} for a subset of the
 * permutations for a compilation. The permutations this one is for are in a
 * consecutive range described by {@link #getFirstPerm()} and
 * {@link #getNumPerms()}.
 */
public class PrecompilationFile {
  /**
   * Suffix for precompilation files stored in the work directory.
   */
  private static final String FILENAME_PREFIX = "precompilation";

  /**
   * Prefix for precompilation files stored in the work directory.
   */
  private static final String FILENAME_SUFFIX = ".ser";

  public static String fileNameForPermutations(int firstPerm, int numPerms) {
    return FILENAME_PREFIX + firstPerm + "-" + (firstPerm + numPerms - 1)
        + FILENAME_SUFFIX;
  }

  public static Collection<PrecompilationFile> scanJarFile(File file)
      throws IOException {
    Pattern pattern = Pattern.compile("precompilation([0-9]+)-([0-9]+)\\.ser");

    List<PrecompilationFile> precomps = new ArrayList<PrecompilationFile>();

    JarFile jarFile = new JarFile(file);
    Enumeration<JarEntry> entries = jarFile.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      Matcher matcher = pattern.matcher(entry.getName());
      if (matcher.matches()) {
        int start = Integer.parseInt(matcher.group(1));
        int end = Integer.parseInt(matcher.group(2));
        int numPerms = end - start + 1;

        precomps.add(new PrecompilationFile(jarFile, entry, start, numPerms));
      }
    }

    return precomps;
  }

  private final JarFile jarFile;
  private final ZipEntry zipEntry;

  private final int firstPerm;

  private final int numPerms;

  public PrecompilationFile(JarFile jarFile, ZipEntry zipEntry, int firstPerm,
      int numPerms) {
    this.firstPerm = firstPerm;
    this.numPerms = numPerms;

    this.jarFile = jarFile;
    this.zipEntry = zipEntry;
  }

  /**
   * Return the first permutation this {@link Precompilation} is for.
   */
  public int getFirstPerm() {
    return firstPerm;
  }

  /**
   * Get the number of the highest permutation included in this
   * {@link Precompilation}.
   */
  public int getLastPerm() {
    return getFirstPerm() + getNumPerms() - 1;
  }

  /**
   * Return the number of permutations in this {@link Precompilation}.
   */
  public int getNumPerms() {
    return numPerms;
  }

  public boolean isForPermutation(int perm) {
    return perm >= getFirstPerm() && perm <= getLastPerm();
  }

  public Precompilation newInstance(TreeLogger logger)
      throws UnableToCompleteException {
    Precompilation toReturn;

    try {
      toReturn = Util.readStreamAsObject(jarFile.getInputStream(zipEntry),
          Precompilation.class);
    } catch (IOException e) {
      toReturn = null;
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.ERROR, "Missing class definition", e);
      throw new UnableToCompleteException();
    }

    if (toReturn == null) {
      logger.log(TreeLogger.ERROR, "Unable to instantiate object");
      throw new UnableToCompleteException();
    }
    return toReturn;
  }
}
