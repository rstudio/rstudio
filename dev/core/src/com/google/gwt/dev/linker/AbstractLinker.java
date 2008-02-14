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
import com.google.gwt.dev.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides basic functions common to all Linker implementations.
 */
public abstract class AbstractLinker extends Linker {

  /**
   * Delegates to {@link #doEmitArtifacts(TreeLogger, LinkerContext)}.
   */
  public final void link(TreeLogger logger, LinkerContext context)
      throws UnableToCompleteException {
    doEmitArtifacts(logger, context);
  }

  /**
   * Emit a byte array into the output. Linkers that require knowledge of all
   * resources emitted may override this function to spy on the output.
   * 
   * @param logger a logger
   * @param context the LinkerContext
   * @param what the bytes to emit
   * @param where the partial path within the output directory
   * @throws UnableToCompleteException
   */
  protected void doEmit(TreeLogger logger, LinkerContext context, byte[] what,
      String where) throws UnableToCompleteException {
    OutputStream out = context.tryCreateArtifact(logger, where);
    if (out != null) {
      try {
        out.write(what);
        context.commit(logger, out);
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Unable to emit artifact", e);
        throw new UnableToCompleteException();
      }
    }
  }

  /**
   * The default implementation will emit all compilations, public resources,
   * and generated resources by calling the relevant functions defined in the
   * linker interface.
   */
  protected void doEmitArtifacts(TreeLogger logger, LinkerContext context)
      throws UnableToCompleteException {
    for (CompilationResult result : context.getCompilations()) {
      doEmitCompilation(logger, context, result);
    }

    // Copy the public resources
    for (PublicResource resource : context.getPublicResources()) {
      doEmitPublicResource(logger, context, resource);
    }

    // Copy the generated resources
    for (GeneratedResource resource : context.getGeneratedResources()) {
      doEmitGeneratedResource(logger, context, resource);
    }
  }

  /**
   * Linkers must implement this function to emit compilations.
   * 
   * @param logger
   * @param context
   * @param result
   * @throws UnableToCompleteException
   */
  protected abstract void doEmitCompilation(TreeLogger logger,
      LinkerContext context, CompilationResult result)
      throws UnableToCompleteException;

  protected void doEmitGeneratedResource(TreeLogger logger,
      LinkerContext context, GeneratedResource resource)
      throws UnableToCompleteException {
    emitInputStream(logger, context, resource.tryGetResourceAsStream(logger),
        resource.getPartialPath());
  }

  protected void doEmitPublicResource(TreeLogger logger, LinkerContext context,
      PublicResource resource) throws UnableToCompleteException {
    emitInputStream(logger, context, resource.tryGetResourceAsStream(logger),
        resource.getPartialPath());
  }

  /**
   * Helper method that emits the contents of an InputStream into the output.
   * This delegates to
   * {@link #doEmit(TreeLogger, LinkerContext, byte[], String)}.
   * 
   * @param logger a logger
   * @param context the LinkerContext
   * @param what the stream to emit
   * @param where the partial path within the output directory
   * @throws UnableToCompleteException
   */
  protected final void emitInputStream(TreeLogger logger,
      LinkerContext context, InputStream what, String where)
      throws UnableToCompleteException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Util.copy(logger, what, out);
    doEmit(logger, context, out.toByteArray(), where);
  }

  /**
   * A helper method to emit a byte array into the output, using an
   * automatically-computed strong path. This function delegates to
   * {@link #doEmit(TreeLogger, LinkerContext, byte[], String)}.
   * 
   * @return the partial path of the emitted resource.
   */
  protected final String emitWithStrongName(TreeLogger logger,
      LinkerContext context, byte[] what, String prefix, String suffix)
      throws UnableToCompleteException {
    String strongName = prefix + Util.computeStrongName(what) + suffix;
    doEmit(logger, context, what, strongName);
    return strongName;
  }
}
