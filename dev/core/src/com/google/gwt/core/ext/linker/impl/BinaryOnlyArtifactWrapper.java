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
import com.google.gwt.core.ext.linker.EmittedArtifact;

import java.io.InputStream;

/**
 * A wrapper around an emitted artifact that only allows reading the artifact's
 * path and its binary contents.
 */
public class BinaryOnlyArtifactWrapper extends BinaryEmittedArtifact {
  private final EmittedArtifact underlyingArtifact;

  public BinaryOnlyArtifactWrapper(String path, EmittedArtifact artifact) {
    super(path);
    setVisibility(artifact.getVisibility());
    this.underlyingArtifact = artifact;
  }

  @Override
  public InputStream getContents(TreeLogger logger)
      throws UnableToCompleteException {
    return underlyingArtifact.getContents(logger);
  }

  @Override
  public long getLastModified() {
    return underlyingArtifact.getLastModified();
  }
}
