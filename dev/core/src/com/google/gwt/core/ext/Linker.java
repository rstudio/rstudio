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
import com.google.gwt.core.ext.linker.Shardable;

/**
 * Defines a linker for the GWT compiler. Each Linker must be annotated with a
 * {@link com.google.gwt.core.ext.linker.LinkerOrder} annotation to determine
 * the relative ordering of the Linkers. Exact order of Linker execution will be
 * determined by the order of <code>add-linker</code> tags in the module
 * configuration. Each Linker should also be annotated with {@link Shardable};
 * non-shardable linkers are deprecated and will eventually not be supported.
 *
 * <p>
 * A new instance of a linker is created each time a module is compiled or
 * during hosted mode when a module first loads (or is refreshed). During a
 * compile, {@link #link(TreeLogger, LinkerContext, ArtifactSet)} is called
 * exactly once on each non-shardable linker, and the artifact set will contain
 * any and all generated artifacts. For shardable linkers,
 * {@link #link(TreeLogger, LinkerContext, ArtifactSet, boolean)} is called once
 * for each compiled permutation and once after all compiles are finished. The
 * precise artifacts supplied differ with each call and are described in the
 * method's documentation.
 *
 * <p>
 * When hosted mode starts for a module, it calls
 * {@link #link(TreeLogger, LinkerContext, ArtifactSet)} for non-shardable
 * linkers and {@link #link(TreeLogger, LinkerContext, ArtifactSet, boolean)}
 * for shardable ones, passing <code>false</code> as the
 * <code>onePermutation</code> argument. If any artifacts are subsequently
 * generated during the course of running hosted mode,
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
   * Check whether this class is considered a shardable linker. A linker is
   * shardable if it either implements the {@link Shardable} annotation or it
   * has a field named <code>gwtIsShardable</code>. If such a field is present,
   * it doesn't matter what value the field holds. The latter mechanism is only
   * intended to support linkers that must compile against older versions of
   * GWT.
   */
  public final boolean isShardable() {
    if (getClass().isAnnotationPresent(Shardable.class)) {
      return true;
    }

    try {
      getClass().getDeclaredField("gwtIsShardable");
      return true;
    } catch (NoSuchFieldException e) {
      // The field does not exist; fall through
    }

    return false;
  }

  /**
   * This method is invoked for linkers not annotated with {@link Shardable}. It
   * sees all artifacts across the whole compile and can modify them
   * arbitrarily. This method is only called if the linker is not annotated with
   * {@link Shardable}.
   *
   * @param logger the TreeLogger to record to
   * @param context provides access to the Linker's environment
   * @param artifacts an unmodifiable view of the artifacts to link
   * @return the artifacts that should be propagated through the linker chain
   * @throws UnableToCompleteException if compilation violates assumptions made
   *           by the Linker or for errors encountered by the Linker
   */
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {
    assert !isShardable();
    return artifacts;
  }

  /**
   * <p>
   * This method is invoked for linkers annotated with {@link Shardable}. It is
   * called at two points during compilation: after the compile of each
   * permutation, and after all compilation has finished. The
   * <code>onePermutation</code> is <code>true</code> for a per-permutation call
   * and <code>false</code> for a global final-link call.
   *
   * <p>
   * For one-permutation calls, this method is passed all artifacts generated
   * for just the one permutation. For the global call at the end of
   * compilation, this method sees artifacts for the whole compilation, but with
   * two modifications intended to support builds on computer clusters:
   * <ol>
   * <li>All EmittedArtifacts have been converted to BinaryEmittedArtifacts
   * <li>All artifacts not marked as
   * {@link com.google.gwt.core.ext.linker.Transferable} have been discarded.
   * </ol>
   *
   * @param logger the TreeLogger to record to
   * @param context provides access to the Linker's environment
   * @param artifacts an unmodifiable view of the artifacts to link
   * @param onePermutation true for a one-permutation call
   * @return the artifacts that should be propagated through the linker chain
   * @throws UnableToCompleteException if compilation violates assumptions made
   *           by the Linker or for errors encountered by the Linker
   */
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts, boolean onePermutation)
      throws UnableToCompleteException {
    assert isShardable();
    return artifacts;
  }

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
  public ArtifactSet relink(TreeLogger logger, LinkerContext context,
      ArtifactSet newArtifacts) throws UnableToCompleteException {
    return newArtifacts;
  }

  /**
   * Returns {@code true} if this linker supports DevMode.
   *
   * @param context a LinkerContext
   */
  public boolean supportsDevModeInJunit(LinkerContext context) {
    // By default, linkers do not support Dev Mode
    return false;
  }
}
