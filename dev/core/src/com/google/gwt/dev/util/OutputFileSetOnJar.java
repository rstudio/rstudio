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
package com.google.gwt.dev.util;

import com.google.gwt.dev.util.NullOutputFileSet.NullOutputStream;
import com.google.gwt.dev.util.collect.HashSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * An {@link OutputFileSet} on a jar file.
 */
public class OutputFileSetOnJar extends OutputFileSet {
  /**
   * An output stream on a jar entry for <code>jar</code>. It is assumed that
   * the entry has already been written, so this class only has to forward the
   * writes.
   */
  private final class OutputStreamOnJarEntry extends OutputStream {
    @Override
    public void close() throws IOException {
      jar.closeEntry();
    }

    @Override
    public void write(byte b[], int off, int len) throws IOException {
      jar.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
      jar.write(b);
    }
  }

  public static final boolean normalizeTimestamps = Boolean.parseBoolean(
      System.getProperty("gwt.normalizeTimestamps", "false"));

  /**
   * Returns the parent path of forward-slash based partial path. Assumes the
   * given path does not end with a trailing slash.
   */
  private static String getParentPath(String path) {
    assert !path.endsWith("/");
    int pos = path.lastIndexOf('/');
    return (pos >= 0) ? path.substring(0, pos) : null;
  }

  private Set<String> createdDirs = new HashSet<String>();

  private final JarOutputStream jar;

  private final String pathPrefix;
  
  private final Set<String> seenEntries = new HashSet<String>();

  public OutputFileSetOnJar(File jarFile, String pathPrefix) throws IOException {
    super(jarFile.getAbsolutePath());
    jarFile.delete();
    jar = new JarOutputStream(new FileOutputStream(jarFile));
    this.pathPrefix = pathPrefix;
  }

  @Override
  public void close() throws IOException {
    jar.close();
  }

  @Override
  public OutputStream createNewOutputStream(String path, long lastModifiedTime)
      throws IOException {
    String fullPath = pathPrefix + path;
    if (seenEntries.contains(fullPath)) {
      return new NullOutputStream();
    }
    seenEntries.add(fullPath);
    mkzipDirs(getParentPath(fullPath));

    ZipEntry zipEntry = new ZipEntry(fullPath);
    if (normalizeTimestamps) {
      zipEntry.setTime(0);
    } else if (lastModifiedTime >= 0) {
      zipEntry.setTime(lastModifiedTime);
    }
    jar.putNextEntry(zipEntry);

    return new OutputStreamOnJarEntry();
  }

  /**
   * Creates directory entries within a zip archive. Uses
   * <code>createdDirs</code> to avoid creating entries for the same path twice.
   * 
   * @param path the path of a directory within the archive to create
   */
  private void mkzipDirs(String path) throws IOException {
    if (path == null) {
      return;
    }
    if (createdDirs.contains(path)) {
      return;
    }
    mkzipDirs(getParentPath(path));
    ZipEntry entry = new ZipEntry(path + '/');
    entry.setSize(0);
    entry.setCompressedSize(0);
    entry.setCrc(0);
    entry.setMethod(ZipOutputStream.STORED);
    if (normalizeTimestamps) {
      entry.setTime(0);
    }
    jar.putNextEntry(entry);
    createdDirs.add(path);
  }
}
