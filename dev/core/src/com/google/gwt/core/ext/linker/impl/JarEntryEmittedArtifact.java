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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.BinaryEmittedArtifact;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 *A <code>BinaryEmittedArtifact</code> that reads a jar entry.
 */
public class JarEntryEmittedArtifact extends BinaryEmittedArtifact {
  /**
   * An input stream that has its own open {@link JarFile}. When this stream is
   * closed, the {@link JarFile} is as well.
   */
  private class JarEntryInputStream extends InputStream {
    private JarFile jarFile;
    private InputStream stream;

    public JarEntryInputStream() throws IOException {
      this.jarFile = new JarFile(file);
      this.stream = jarFile.getInputStream(entry);
    }

    @Override
    public void close() throws IOException {
      stream.close();
      jarFile.close();
    }

    @Override
    public int read() throws IOException {
      return stream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
      return stream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      return stream.read(b, off, len);
    }
  }

  private final JarEntry entry;
  private final File file;

  public JarEntryEmittedArtifact(String path, File jarFile, JarEntry entry) {
    super(path);
    this.file = jarFile;
    this.entry = entry;
  }

  @Override
  public InputStream getContents(TreeLogger logger)
      throws UnableToCompleteException {
    try {
      return new JarEntryInputStream();
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "unexpected IOException", e);
      throw new UnableToCompleteException();
    }
  }

  @Override
  public long getLastModified() {
    return entry.getTime();
  }
}
