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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Artifacts created by {@link AbstractLinker}.
 */
public class SyntheticArtifact extends EmittedArtifact {
  private final byte[] data;

  SyntheticArtifact(Class<? extends Linker> linkerType, String partialPath,
      byte[] data) {
    super(linkerType, partialPath);
    assert data != null;
    this.data = data;
  }

  @Override
  public InputStream getContents(TreeLogger logger)
      throws UnableToCompleteException {
    return new ByteArrayInputStream(data);
  }
}