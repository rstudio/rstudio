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

import java.io.OutputStream;
import java.util.SortedSet;

/**
 * Provides access to all information and runtime services required by a
 * {@link Linker}. Methods that return a {@link SortedSet} are guaranteed to
 * have stable iteration order between runs of the compiler over identical
 * input. Unless otherwise specified, the exact iteration order is left as an
 * implementation detail.
 */
public interface LinkerContext {
  /**
   * Finalizes the OutputStream for a given artifact. This method must be called
   * in order to actually place the artifact into the output directory. If the
   * OutptStream has not already been closed, this method will close the
   * OutputStream.
   */
  void commit(TreeLogger logger, OutputStream out)
      throws UnableToCompleteException;

  /**
   * Returns all unique compilations of the module.
   */
  SortedSet<CompilationResult> getCompilations();

  /**
   * Returns all resources emitted through a GeneratorContext.
   */
  SortedSet<GeneratedResource> getGeneratedResources();

  /**
   * Returns the name of the module's bootstrap function.
   */
  String getModuleFunctionName();

  /**
   * Returns the name of the module being compiled.
   */
  String getModuleName();

  /**
   * Provides access to all <code>script</code> tags referenced by the module
   * definition.
   */
  SortedSet<ModuleScriptResource> getModuleScripts();

  /**
   * Provides access to all <code>stylesheet</code> tags referenced by the
   * module definition.
   */
  SortedSet<ModuleStylesheetResource> getModuleStylesheets();

  /**
   * Returns all deferred binding properties defined in the module. The
   * SelectionProperties will be sorted by the standard string comparison
   * function on the name of the property.
   */
  SortedSet<SelectionProperty> getProperties();

  /**
   * Returns all files in the module's public path.
   */
  SortedSet<PublicResource> getPublicResources();

  /**
   * Applies optimizations to a JavaScript program. This method is intended to
   * be applied to bootstrap scripts in order to apply context-specific
   * transformations to the program, based on the compiler's configuration. The
   * return value will be functionally-equivalent JavaScript, although the exact
   * transformations and structure of the output should be considered opaque.
   * 
   * While this function can be safely applied multiple times, the best results
   * will be obtained by performing all JavaScript assembly and calling the
   * function just before writing the selection script to disk.
   */
  String optimizeJavaScript(TreeLogger logger, String jsProgram)
      throws UnableToCompleteException;

  /**
   * Attempt to create an artifact within the linker's output directory. If a
   * similarly-named resource has already been created, this method will return
   * <code>null</code>. The data written to the OutputStream will not be
   * written into the output directory unless
   * {@link #commit(TreeLogger, OutputStream)} is called.
   * 
   * @param logger
   * @param partialPath the partial path of the artifact
   * @return An OutputStream through which the artifact may be written, or
   *         <code>null</code> if the artifact had previously been created.
   */
  OutputStream tryCreateArtifact(TreeLogger logger, String partialPath);

  /**
   * Provides named access to generated resources.
   * 
   * @param logger a logging destination
   * @param partialPath a partial path, generally obtained from
   *          {@link GeneratedResource#getPartialPath()}
   * @return The requested resource, or <code>null</code> if no such resource
   *         exists
   */
  GeneratedResource tryGetGeneratedResource(TreeLogger logger,
      String partialPath);

  /**
   * Provides name access to resources in the module's public path.
   * 
   * @param logger a logging destination
   * @param partialPath a partial path, generally obtained from
   *          {@link PublicResource#getPartialPath()}
   * @return The requested resource or <code>null</code> if no such resource
   *         exists
   */
  PublicResource tryGetPublicResource(TreeLogger logger, String partialPath);
}