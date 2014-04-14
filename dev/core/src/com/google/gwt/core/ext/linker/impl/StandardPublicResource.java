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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.PublicResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * The standard implementation of {@link PublicResource}.
 */
public class StandardPublicResource extends PublicResource {

  /**
   * Serializes a public resource via a snapshot of the content.
   */
  private static final class SerializedPublicResource extends PublicResource {
    private final byte[] data;
    private final long lastModified;

    protected SerializedPublicResource(String partialPath, byte[] data,
        long lastModified) {
      super(StandardLinkerContext.class, partialPath);
      this.data = data;
      this.lastModified = lastModified;
    }

    @SuppressWarnings("unused")
    @Override
    public InputStream getContents(TreeLogger logger)
        throws UnableToCompleteException {
      return new ByteArrayInputStream(data);
    }

    @Override
    public long getLastModified() {
      return lastModified;
    }

    @Override
    public void writeTo(TreeLogger logger, OutputStream out)
        throws UnableToCompleteException {
      try {
        out.write(data);
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Unable to write to stream", e);
        throw new UnableToCompleteException();
      }
    }
  }

  private final Resource resource;

  public StandardPublicResource(String partialPath, Resource resource) {
    super(StandardLinkerContext.class, partialPath);
    this.resource = resource;
  }

  @Override
  public InputStream getContents(TreeLogger logger)
      throws UnableToCompleteException {
    try {
      return resource.openContents();
    } catch (IOException ex) {
      logger.log(TreeLogger.ERROR, "Problem reading resource: " + resource.getLocation(), ex);
      throw new UnableToCompleteException();
    }
  }

  @Override
  public long getLastModified() {
    return resource.getLastModified();
  }

  private Object writeReplace() {
    if (resource instanceof Serializable) {
      return this;
    }
    // Resource is not serializable, must replace myself.
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Util.copy(resource.openContents(), baos);
      return new SerializedPublicResource(getPartialPath(), baos.toByteArray(),
          getLastModified());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
