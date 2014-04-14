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
import com.google.gwt.dev.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Provides basic functions common to all Linker implementations.
 */
public abstract class AbstractLinker extends Linker {
  /**
   * A helper method to create an artifact from an array of bytes.
   *
   * @param logger a TreeLogger
   * @param what the data to emit
   * @param partialPath the partial path of the resource
   * @return an artifact that contains the given data
   * @throws UnableToCompleteException
   */
  protected final SyntheticArtifact emitBytes(TreeLogger logger, byte[] what,
      String partialPath) throws UnableToCompleteException {
    return new SyntheticArtifact(getClass(), partialPath, what);
  }

  /**
   * A helper method to create an artifact from an array of bytes.
   *
   * @param logger a TreeLogger
   * @param what the data to emit
   * @param partialPath the partial path of the resource
   * @return an artifact that contains the given data
   * @param lastModified the last modified time of the new artifact
   * @throws UnableToCompleteException
   */
  protected final SyntheticArtifact emitBytes(TreeLogger logger, byte[] what,
      String partialPath, long lastModified) throws UnableToCompleteException {
    return new SyntheticArtifact(getClass(), partialPath, what, lastModified);
  }

  /**
   * A helper method to create an artifact to emit the contents of an
   * InputStream.
   *
   * @param logger a TreeLogger
   * @param what the source InputStream
   * @param partialPath the partial path of the emitted resource
   * @return an artifact that contains the contents of the InputStream
   */
  protected final SyntheticArtifact emitInputStream(TreeLogger logger,
      InputStream what, String partialPath) throws UnableToCompleteException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Util.copy(logger, what, out);
    return emitBytes(logger, out.toByteArray(), partialPath);
  }

  /**
   * A helper method to create an artifact to emit the contents of an
   * InputStream.
   *
   * @param logger a TreeLogger
   * @param what the source InputStream
   * @param partialPath the partial path of the emitted resource
   * @param lastModified the last modified time of the new artifact
   * @return an artifact that contains the contents of the InputStream
   */
  protected final SyntheticArtifact emitInputStream(TreeLogger logger,
      InputStream what, String partialPath, long lastModified)
      throws UnableToCompleteException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Util.copy(logger, what, out);
    return emitBytes(logger, out.toByteArray(), partialPath, lastModified);
  }

  /**
   * A helper method to create an artifact to emit a String.
   *
   * @param logger a TreeLogger
   * @param what the contents of the Artifact to emit
   * @param partialPath the partial path of the emitted resource
   * @return an artifact that contains the contents of the given String
   */
  protected final SyntheticArtifact emitString(TreeLogger logger, String what,
      String partialPath) throws UnableToCompleteException {
    return emitBytes(logger, Util.getBytes(what), partialPath);
  }

  /**
   * A helper method to create an artifact to emit a String.
   *
   * @param logger a TreeLogger
   * @param what the contents of the Artifact to emit
   * @param partialPath the partial path of the emitted resource
   * @param lastModified the last modified time of the new artifact
   * @return an artifact that contains the contents of the given String
   */
  protected final SyntheticArtifact emitString(TreeLogger logger, String what,
      String partialPath, long lastModified) throws UnableToCompleteException {
    return emitBytes(logger, Util.getBytes(what), partialPath, lastModified);
  }

  /**
   * A helper method to create an artifact from an array of bytes with a strong
   * name.
   *
   * @param logger a TreeLogger
   * @param what the data to emit
   * @param prefix a non-null string to prepend to the hash to determine the
   *          Artifact's partial path
   * @param suffix a non-null string to append to the hash to determine the
   *          Artifact's partial path
   * @return an artifact that contains the given data
   */
  protected final SyntheticArtifact emitWithStrongName(TreeLogger logger,
      byte[] what, String prefix, String suffix)
      throws UnableToCompleteException {
    String strongName = prefix + Util.computeStrongName(what) + suffix;
    return emitBytes(logger, what, strongName);
  }

  /**
   * A helper method to create an artifact from an array of bytes with a strong
   * name.
   *
   * @param logger a TreeLogger
   * @param what the data to emit
   * @param prefix a non-null string to prepend to the hash to determine the
   *          Artifact's partial path
   * @param suffix a non-null string to append to the hash to determine the
   *          Artifact's partial path
   * @param lastModified the last modified time of the new artifact
   * @return an artifact that contains the given data
   */
  protected final SyntheticArtifact emitWithStrongName(TreeLogger logger,
      byte[] what, String prefix, String suffix, long lastModified)
      throws UnableToCompleteException {
    String strongName = prefix + Util.computeStrongName(what) + suffix;
    return emitBytes(logger, what, strongName, lastModified);
  }
}
