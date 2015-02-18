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
package com.google.gwt.dev.util;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.thirdparty.guava.common.base.Preconditions;

import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.Serializable;

/**
 * Represents a File that contains the serialized form of a Serializable object.
 *
 * @param <T> the type of object serialized into the file
 */
public class FileBackedObject<T extends Serializable> implements PersistenceBackedObject<T> {

  private boolean alreadyWritten = false;
  private final File backingFile;
  private final Class<T> clazz;

  /**
   * Constructs an empty FileBackedObject. A temporary File will be used and this file will be
   * deleted when the JVM exits.
   *
   * @param clazz the type of object to be serialized
   * @throws IOException if the temporary file could not be created
   */
  public FileBackedObject(Class<T> clazz) throws IOException {
    this(clazz, File.createTempFile("fileBackedObject", ".ser"));
    backingFile.deleteOnExit();
  }

  /**
   * Constructs a FileBackedObject using an existing File object.
   *
   * @param clazz the type of object to be serialized
   * @param backingFile the file to read from or write to
   */
  public FileBackedObject(Class<T> clazz, File backingFile) {
    this.clazz = clazz;
    this.backingFile = backingFile;
  }

  @Override
  public boolean exists() {
    return backingFile.exists();
  }

  @Override
  public String getPath() {
    return backingFile.getAbsolutePath();
  }

  @Override
  public T newInstance(TreeLogger logger) throws UnableToCompleteException {
    try {
      return Util.readFileAsObject(backingFile, clazz);
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.ERROR, "Missing class definition", e);
      throw new UnableToCompleteException();
    } catch (InvalidClassException e) {
      logger.log(
          TreeLogger.ERROR, "Incompatible version of class " + clazz.getSimpleName() + ".", e);
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
    Preconditions.checkState(!alreadyWritten);
    alreadyWritten = true;
    Util.writeObjectAsFile(logger, backingFile, object);
  }

  @Override
  public String toString() {
    return backingFile.toString() + "<" + clazz.getName() + ">";
  }
}
