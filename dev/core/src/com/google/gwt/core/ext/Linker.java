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
package com.google.gwt.core.ext;

import com.google.gwt.core.ext.linker.ArtifactSet;

/**
 * Defines a linker for the GWT compiler. Each Linker must be annotated with a
 * {@link com.google.gwt.core.ext.linker.LinkerOrder} annotation to determine
 * the relative ordering of the Linkers. Exact order of Linker execution will be
 * determined by the order of <code>add-linker</code> tags in the module
 * configuration.
 * 
 * <p>
 * A new instance of a linker is created each time a module is compiled or
 * during hosted mode when a module first loads (or is refreshed). During a
 * compile, {@link #link(TreeLogger, LinkerContext, ArtifactSet)} will be called
 * exactly once, and the artifact set will contain any and all generated
 * artifacts. . In hosted mode,
 * {@link #link(TreeLogger, LinkerContext, ArtifactSet)} is called initially,
 * but with no generated artifacts. If any artifacts are subsequently generated
 * during the course of running hosted mode,
 * {@link #relink(TreeLogger, LinkerContext, ArtifactSet)} will be called with
 * the new artifacts.
 * </p>
 */
public abstract class Linker {
  /**
   * Returns a human-readable String describing the Linker.
   */
  public abstract String getDescription();

  /**
   * Invoke the Linker.
   * 
   * @param logger the TreeLogger to record to
   * @param context provides access to the Linker's environment
   * @param artifacts an unmodifiable view of the artifacts to link
   * @return the artifacts that should be propagated through the linker chain
   * @throws UnableToCompleteException if compilation violates assumptions made
   *           by the Linker or for errors encountered by the Linker
   */
  public abstract ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException;

  /**
   * Re-invoke the Linker with newly generated artifacts. Linkers that need to
   * reference the original artifact set passed into
   * {@link #link(TreeLogger, LinkerContext, ArtifactSet)} should retain a copy
   * of the original artifact set in an instance variable.
   * 
   * @param logger the TreeLogger to record to
   * @param context provides access to the Linker's environment
   * @param newArtifacts an unmodifiable view of the newly generated artifacts
   * @return the new artifacts that should be propagated through the linker
   *         chain; it is not necessary to return any artifacts from the
   *         original link (or previous calls to relink) that have not been
   *         modified
   * @throws UnableToCompleteException if compilation violates assumptions made
   *           by the Linker or for errors encountered by the Linker
   */
  @SuppressWarnings("unused")
  public ArtifactSet relink(TreeLogger logger, LinkerContext context,
      ArtifactSet newArtifacts) throws UnableToCompleteException {
    return newArtifacts;
  }
}
