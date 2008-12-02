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
package com.google.gwt.core.ext.linker;

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Artifacts created by {@link AbstractLinker}.
 */
public class SyntheticArtifact extends EmittedArtifact {
  private final File backing;

  SyntheticArtifact(TreeLogger logger, Class<? extends Linker> linkerType,
      String partialPath, byte[] data) throws UnableToCompleteException {
    super(linkerType, partialPath);
    assert data != null;

    try {
      backing = File.createTempFile("synthetic", ".artifact");
      backing.deleteOnExit();
      Util.writeBytesToFile(TreeLogger.NULL, backing, data);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to write backing file for artifact "
          + partialPath, e);
      throw new UnableToCompleteException();
    }
  }

  @Override
  public InputStream getContents(TreeLogger logger)
      throws UnableToCompleteException {
    try {
      return new FileInputStream(backing);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to read backing file for artifact "
          + getPartialPath(), e);
      throw new UnableToCompleteException();
    }
  }
}