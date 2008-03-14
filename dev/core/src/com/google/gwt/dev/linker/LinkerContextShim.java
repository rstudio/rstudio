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
 * This base class allows behaviors to be injected into the
 * {@link LinkerContext} that is observed by the {@link Linker} types operating
 * on the output from the compiler. Instances of LinkerContextShim are mapped
 * into the compilation process by including {@code <extend-linker-context>}
 * tags in the GWT module definition. Subclasses of LinkerContextShim must
 * define a two-argument constructor that accepts an instance of TreeLogger and
 * LinkerContext.
 * <p>
 * The default behavior of all methods in this class is to delegate to the
 * LinkerContext returned from {@link #getParent()}. Separate shim instances
 * are guaranteed to be used for each Linker instance.
 * <p>
 * No guarantees are made on the order in which or number of times any method on
 * the LinkerContextShim will be invoked. Implementations are encouraged to
 * precompute all return values for each method in their constructors and return
 * an unmodifiable wrapper around the collection using
 * {@link java.util.Collections#unmodifiableSortedSet(SortedSet)}.
 */
public abstract class LinkerContextShim implements LinkerContext {
  private final LinkerContext parent;

  protected LinkerContextShim(TreeLogger logger, LinkerContext parent)
      throws UnableToCompleteException {
    this.parent = parent;
  }

  /**
   * Finalize all actions performed by the LinkerContextShim. This method will
   * be called in reverse order; it will not be called on a parent until all of
   * its children have been committed.
   */
  public void commit(TreeLogger logger) throws UnableToCompleteException {
  }

  public void commit(TreeLogger logger, OutputStream out)
      throws UnableToCompleteException {
    getParent().commit(logger, out);
  }

  public SortedSet<CompilationResult> getCompilations() {
    return getParent().getCompilations();
  }

  public SortedSet<GeneratedResource> getGeneratedResources() {
    return getParent().getGeneratedResources();
  }

  public String getModuleFunctionName() {
    return getParent().getModuleFunctionName();
  }

  public String getModuleName() {
    return getParent().getModuleName();
  }

  public SortedSet<ModuleScriptResource> getModuleScripts() {
    return getParent().getModuleScripts();
  }

  public SortedSet<ModuleStylesheetResource> getModuleStylesheets() {
    return getParent().getModuleStylesheets();
  }

  /**
   * Obtain a reference to the parent LinkerContext. This method is guaranteed
   * to return a useful value before any of the other LinkerContext-derived
   * methods are invoked.
   */
  // NB This is final because StandardLinkerContext depends on it to unwind
  public final LinkerContext getParent() {
    return parent;
  }

  public SortedSet<SelectionProperty> getProperties() {
    return getParent().getProperties();
  }

  public SortedSet<PublicResource> getPublicResources() {
    return getParent().getPublicResources();
  }

  public String optimizeJavaScript(TreeLogger logger, String jsProgram)
      throws UnableToCompleteException {
    return getParent().optimizeJavaScript(logger, jsProgram);
  }

  public OutputStream tryCreateArtifact(TreeLogger logger, String partialPath) {
    return getParent().tryCreateArtifact(logger, partialPath);
  }

  public GeneratedResource tryGetGeneratedResource(TreeLogger logger,
      String partialPath) {
    return getParent().tryGetGeneratedResource(logger, partialPath);
  }

  public PublicResource tryGetPublicResource(TreeLogger logger,
      String partialPath) {
    return getParent().tryGetPublicResource(logger, partialPath);
  }
}
