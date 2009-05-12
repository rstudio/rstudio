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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.GeneratedResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * The standard implementation of {@link GeneratedResource}.
 */
public class StandardGeneratedResource extends GeneratedResource {
  private final File file;

  public StandardGeneratedResource(Class<? extends Generator> generatorType,
      String partialPath, File file) {
    super(StandardLinkerContext.class, generatorType, partialPath);
    this.file = file;
  }

  @Override
  public byte[] getBytes(TreeLogger logger) throws UnableToCompleteException {
    try {
      RandomAccessFile raf = new RandomAccessFile(file, "r");
      byte[] buf = new byte[(int) raf.length()];
      raf.readFully(buf);
      return buf;
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to read file", e);
      throw new UnableToCompleteException();
    }
  }

  @Override
  public InputStream getContents(TreeLogger logger)
      throws UnableToCompleteException {
    try {
      return new FileInputStream(file);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to open file", e);
      throw new UnableToCompleteException();
    }
  }

  @Override
  public long getLastModified() {
    return file.lastModified();
  }
}
