/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.GeneratedResource;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.resource.ResourceOracle;

import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Provides metadata to deferred binding generators.
 */
public interface GeneratorContext {

  /**
   * Checks whether a rebind rule is available for a given sourceTypeName, such
   * as can appear in a replace-with or generate-with rule.
   * 
   * @param sourceTypeName the name of a type to check for rebind rule
   *          availability.
   * @return true if a rebind rule is available
   */
  boolean checkRebindRuleAvailable(String sourceTypeName);

  /**
   * Commits source generation begun with
   * {@link #tryCreate(TreeLogger, String, String)}.
   */
  void commit(TreeLogger logger, PrintWriter pw);

  /**
   * Add an Artifact to the {@link com.google.gwt.core.ext.linker.ArtifactSet}
   * that will be presented to the {@link Linker} chain at the end of the
   * compilation cycle. Custom sub-classes of Artifact can be used to write
   * cooperating Generator and Linker combinations. This method is semantically
   * equivalent to calling
   * {@link com.google.gwt.core.ext.linker.ArtifactSet#replace(Artifact)} if an
   * equivalent Artifact had previously been committed.
   * 
   * @param logger a logger; normally the logger passed into the currently
   *          invoked generator or a branch thereof
   * @param artifact the Artifact to provide to the Linker chain.
   */
  void commitArtifact(TreeLogger logger, Artifact<?> artifact) throws UnableToCompleteException;

  /**
   * Commits resource generation begun with
   * {@link #tryCreateResource(TreeLogger, String)}.
   * 
   * @return the GeneratedResource that was created as a result of committing
   *         the OutputStream.
   * @throws UnableToCompleteException if the resource cannot be written to
   *           disk, if the specified stream is unknown, or if the stream has
   *           already been committed
   */
  GeneratedResource commitResource(TreeLogger logger, OutputStream os)
      throws UnableToCompleteException;

  /**
   * Get the cached rebind result that has been provided to the context, if
   * available. The provided result will be the most recent previously generated
   * result for the currently active rebind rule and requested type name.
   * 
   * @return A {@link CachedGeneratorResult} object, if one has been provided to
   *         the context. Null is returned if there is no previous result
   *         available.
   */
  CachedGeneratorResult getCachedGeneratorResult();

  /**
   * Gets the property oracle for the current generator context. Generators can
   * use the property oracle to query deferred binding properties.
   */
  PropertyOracle getPropertyOracle();

  /**
   * Returns a resource oracle containing all resources that are mapped into the
   * module's source (or super-source) paths. Conceptually, this resource oracle
   * exposes resources which are "siblings" to GWT-compatible Java classes. For
   * example, if the module includes <code>com.google.gwt.core.client</code> as
   * a source package, then a resource at
   * <code>com/google/gwt/core/client/Foo.properties</code> would be exposed by
   * this resource oracle.
   */
  ResourceOracle getResourcesOracle();

  /**
   * Gets the type oracle for the current generator context. Generators can use
   * the type oracle to ask questions about the entire translatable code base.
   * 
   * @return a TypeOracle over all the relevant translatable compilation units
   *         in the source path
   */
  TypeOracle getTypeOracle();

  /**
   * Check whether generator result caching is currently enabled.
   */
  boolean isGeneratorResultCachingEnabled();

  /**
   * Returns true if generators are being run to produce code for a production
   * compile. Returns false for dev mode. Generators can use this information to
   * produce code optimized for the target.
   */
  boolean isProdMode();

  /**
   * Attempts to get a <code>PrintWriter</code> so that the caller can generate
   * the source code for the named type. If the named types already exists,
   * <code>null</code> is returned to indicate that no work needs to be done.
   * The file is not committed until {@link #commit(TreeLogger, PrintWriter)} is
   * called.
   * 
   * @param logger a logger; normally the logger passed into the currently
   *          invoked generator, or a branch thereof
   * @param packageName the name of the package to which the create type belongs
   * @param simpleName the unqualified source name of the type being generated
   * @return <code>null</code> if the package and class already exists,
   *         otherwise a <code>PrintWriter</code> is returned.
   */
  PrintWriter tryCreate(TreeLogger logger, String packageName, String simpleName);

  /**
   * Attempts to get an <code>OutputStream</code> so that the caller can write
   * file contents into the named file underneath the compilation output
   * directory. The file is not committed until
   * {@link #commitResource(TreeLogger, OutputStream)} is called.
   * 
   * @param logger a logger; normally the logger passed into the currently
   *          invoked generator, or a branch thereof
   * @param partialPath the name of the file whose contents are to be written;
   *          the name can include subdirectories separated by forward slashes
   *          ('/')
   * @return an <code>OutputStream</code> into which file contents can be
   *         written, or <code>null</code> if a resource by that name is already
   *         pending or already exists
   * @throws UnableToCompleteException if the resource could not be initialized
   *           for some reason, such as if the specified partial path is invalid
   */
  OutputStream tryCreateResource(TreeLogger logger, String partialPath)
      throws UnableToCompleteException;

  /**
   * Mark a type to be reused from the generator result cache, if available.
   * Calling this method with a successful response indicates that the calling
   * generator will not re-generate this type. A cached version of this type
   * will be added to the context once the calling generator returns from
   * {@link IncrementalGenerator#generateIncrementally}, with a result
   * containing {@link RebindMode#USE_PARTIAL_CACHED}.
   * 
   * @param typeName the fully qualified source name of a type.
   * @return true if the requested type is available from the generator result
   *         cache, false otherwise.
   */
  boolean tryReuseTypeFromCache(String typeName);
}
