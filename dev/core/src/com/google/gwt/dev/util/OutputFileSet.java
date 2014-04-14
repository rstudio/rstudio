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
package com.google.gwt.dev.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * An abstract set of files that a linker links into.
 */
public abstract class OutputFileSet {

  /**
   * Indicates that the file should be overwritten.
   */
  public static final int TIMESTAMP_UNAVAILABLE = -1;

  private final String pathDescription;
  private final Set<String> pathsSeen = new HashSet<String>();

  protected OutputFileSet(String pathDescription) {
    this.pathDescription = pathDescription;
  }

  public boolean alreadyContains(String path) {
    return pathsSeen.contains(path);
  }

  /**
   * No more output will be sent to this OutputFileSet.  It is safe to call
   * {@link #close()} on an already-closed instance.
   *
   * @throws IOException
   */
  public abstract void close() throws IOException;

  /**
   * Return a description of this output file set's path. The precise meaning is
   * unspecified, except that it should be informative when used in log
   * messages.
   */
  public String getPathDescription() {
    return pathDescription;
  }

  /**
   * Opens a file for write. If writing to a zip file and the file already exists,
   * this has no effect. Otherwise, overwrites any existing file.
   * @return the output stream to write to, possibly a NullOutputStream
   */
  public final OutputStream openForWrite(String path) throws IOException {
    return openForWrite(path, TIMESTAMP_UNAVAILABLE);
  }

  /**
   * Opens a file for write. If writing to a zip file and the file already
   * exists, there will be no effect. If writing to a directory, the file
   * exists, and the given timestamp is older or equal to the file's
   * current timestamp, there will be no effect.
   * @param timeStampMillis last modified time in milliseconds, or
   * TIMESTAMP_UNAVAILABLE to force an overwrite.
   * @return the output stream to write to, possibly a NullOutputStream
   */
  public OutputStream openForWrite(String path, long timeStampMillis)
      throws IOException {
    pathsSeen.add(path);
    return createNewOutputStream(path, timeStampMillis);
  }

  protected abstract OutputStream createNewOutputStream(String path,
      long lastModifiedTime) throws IOException;
}
