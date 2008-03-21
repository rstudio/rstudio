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
}
