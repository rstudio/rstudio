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

import java.io.InputStream;

/**
 * An artifact that will be emitted into the output. All EmittedArtifacts
 * contained in the {@link ArtifactSet} at the end of the Linking process will
 * be emitted by the compiler into the module's output directory. This type may
 * be extended by Linker providers to provide alternative implementations of
 * {@link #getContents(TreeLogger)}.
 * 
 * TODO(bobv): provide a timestamp so we can make the time on output files match
 * that of input files?
 */
public abstract class EmittedArtifact extends Artifact<EmittedArtifact> {

  private final String partialPath;

  /**
   * This is mutable because it has no effect on identity.
   */
  private boolean isPrivate;

  protected EmittedArtifact(Class<? extends Linker> linker, String partialPath) {
    super(linker);
    assert partialPath != null;
    this.partialPath = partialPath;
  }

  /**
   * Provides access to the contents of the EmittedResource.
   */
  public abstract InputStream getContents(TreeLogger logger)
      throws UnableToCompleteException;

  /**
   * Returns the partial path within the output directory of the
   * EmittedArtifact.
   */
  public final String getPartialPath() {
    return partialPath;
  }

  @Override
  public final int hashCode() {
    return getPartialPath().hashCode();
  }

  /**
   * Returns whether or not the data contained in the EmittedArtifact should be
   * written into the module output directory or into an auxiliary directory.
   * <p>
   * EmittedArtifacts that return <code>true</code> for this method will not
   * be emitted into the normal module output location, but will instead be
   * written into a directory that is a sibling to the module output directory.
   * The partial path of the EmittedArtifact will be prepended with the
   * short-name of the Linker type that created the EmittedArtifact.
   * <p>
   * Private EmittedArtifacts are intended for resources that generally should
   * not be deployed to the server in the same location as the module
   * compilation artifacts.
   */
  public boolean isPrivate() {
    return isPrivate;
  }

  /**
   * Sets the private attribute of the EmittedResource.
   */
  public void setPrivate(boolean isPrivate) {
    this.isPrivate = isPrivate;
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
