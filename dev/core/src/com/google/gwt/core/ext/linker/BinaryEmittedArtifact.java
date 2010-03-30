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
package com.google.gwt.core.ext.linker;

import com.google.gwt.core.ext.linker.impl.StandardLinkerContext;

/**
 * <p>
 * A thinned down version of some {@link EmittedArtifact}. Only its essentials,
 * including name and contents, are available.
 * </p>
 * 
 * <p>
 * This class should only be extended within the GWT implementation.
 * </p>
 */
public abstract class BinaryEmittedArtifact extends EmittedArtifact {
  protected BinaryEmittedArtifact(String partialPath) {
    super(StandardLinkerContext.class, partialPath);
  }
  
  /**
   * Force subclasses to define.
   */
  @Override
  public abstract long getLastModified();
}
