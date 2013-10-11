/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.util;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import java.io.IOException;
import java.io.Serializable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Represents an entry in a ZipFile that contains the serialized form of a Serializable object.
 *
 * @param <T> the type of object serialized into the zip entry
 */
public class ZipEntryBackedObject<T extends Serializable> implements PersistenceBackedObject<T> {

  private final Class<T> clazz;
  private final String libraryPath;
  private final String resourcePath;
  private final ZipFile zipFile;
  private final ZipOutputStream zipOutputStream;

  /**
   * Constructs a read-only instance that will dynamically open the provided resourcePath in the
   * provided zipFile when requested.
   */
  public ZipEntryBackedObject(
      ZipFile zipFile, String libraryPath, String resourcePath, Class<T> clazz) {
    this.zipFile = zipFile;
    this.zipOutputStream = null;
    this.libraryPath = libraryPath;
    this.resourcePath = resourcePath;
    this.clazz = clazz;
  }

  /**
   * Constructs a write-only instance that will write a ZipEntry and bytes to the provided
   * zipOutputStream when requested.<br />
   *
   * Cannot be used to overwrite an already existing entry. Brand new zip files that are being built
   * manifest as empty files which can not be probed by ZipFile but which are legal for zip output
   * stream writing and ZipOutputStreams can't be probed about existing entries. So it is left up to
   * the user to avoid duplicate entry errors.
   */
  public ZipEntryBackedObject(
      ZipOutputStream zipOutputStream, String libraryPath, String resourcePath, Class<T> clazz) {
    this.zipFile = null;
    this.zipOutputStream = zipOutputStream;
    this.libraryPath = libraryPath;
    this.resourcePath = resourcePath;
    this.clazz = clazz;
  }

  @Override
  public boolean exists() {
    return zipFile.getEntry(resourcePath) != null;
  }

  @Override
  public String getPath() {
    return "jar:file:" + libraryPath + "!/" + resourcePath;
  }

  @Override
  public T newInstance(TreeLogger logger) throws UnableToCompleteException {
    if (zipOutputStream != null) {
      logger.log(TreeLogger.ERROR, "Tried to read from a write-only ZipEntryBackedObject");
      throw new UnableToCompleteException();
    }
    try {
      return Util.readStreamAsObject(zipFile.getInputStream(zipFile.getEntry(resourcePath)), clazz);
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.ERROR, "Missing class definition", e);
      throw new UnableToCompleteException();
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to instantiate object", e);
      throw new UnableToCompleteException();
    }
  }

  @Override
  public void set(TreeLogger logger, T object)
      throws IllegalStateException, UnableToCompleteException {
    assert clazz.isInstance(object);
    if (zipOutputStream == null) {
      logger.log(TreeLogger.ERROR, "Tried to write to a read-only ZipEntryBackedObject");
      throw new UnableToCompleteException();
    }
    try {
      // Inherently disallows duplicate entries.
      zipOutputStream.putNextEntry(new ZipEntry(resourcePath));
      Util.writeObjectToStream(zipOutputStream, object);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to write file: " + getPath(), e);
      throw new UnableToCompleteException();
    }
  }
}
