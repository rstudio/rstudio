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
package com.google.gwt.dev.linker;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import java.io.InputStream;

/**
 * An artifact that intended to be emitted into the output.
 */
public abstract class EmittedArtifact extends Artifact<EmittedArtifact> {

  private final String partialPath;

  protected EmittedArtifact(Class<? extends Linker> linker, String partialPath) {
    super(linker);
    this.partialPath = partialPath;
  }

  public abstract InputStream getContents(TreeLogger logger)
      throws UnableToCompleteException;

  public final String getPartialPath() {
    return partialPath;
  }

  @Override
  public final int hashCode() {
    return getPartialPath().hashCode();
  }

  @Override
  public String toString() {
    return getPartialPath();
  }

  @Override
  protected final int compareToComparableArtifact(EmittedArtifact o) {
    return getPartialPath().compareTo(o.getPartialPath());
  }

  @Override
  protected final Class<EmittedArtifact> getComparableArtifactType() {
    return EmittedArtifact.class;
  }
}
