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

/**
 * Defines a linker for the GWT compiler. One or more Linkers will be invoked
 * after the Java to JavaScript compilation process and are responsible for
 * assembly of the final output from the compiler.
 */
public abstract class Linker {
  /**
   * Returns a human-readable String describing the Linker.
   */
  public abstract String getDescription();

  /**
   * Invoke the Linker. The implementation of this method should rely only on
   * the provided LinkerContext in order to manipulate the environment.
   * 
   * @param logger the TreeLogger to record to
   * @param context provides access to the Linker's environment
   * @throws UnableToCompleteException if compilation violates assumptions made
   *           by the Linker or for errors encountered by the Linker
   */
  public abstract void link(TreeLogger logger, LinkerContext context)
      throws UnableToCompleteException;
}
