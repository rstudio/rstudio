/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.precompress.linker;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.AbstractLinker;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.ConfigurationProperty;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.util.regexfilter.RegexFilter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * <p>
 * A linker that precompresses the public artifacts that it sees. That way, a
 * web server that uses gzip transfer encoding can use the precompressed files
 * instead of having to compress them on the fly.
 * 
 * <p>
 * To use this linker, add the following to your module definition:
 * 
 * <pre>
 *   &lt;inherits name="com.google.gwt.precompress.Precompress"/>
 * </pre>
 * 
 * <p>
 * The files to precompress are specified by the configuration property
 * <code>precompress.path.regexes</code>. By default, the uncompressed artifacts
 * are left in the artifact set. If the configuration property
 * <code>precompress.leave.originals</code> is set to <code>false</code>,
 * however, then the uncompressed version is removed.
 */
@Shardable
@LinkerOrder(Order.POST)
public class PrecompressLinker extends AbstractLinker {
  private static class PrecompressFilter extends RegexFilter {
    public PrecompressFilter(TreeLogger logger, List<String> regexes)
        throws UnableToCompleteException {
      super(logger, regexes);
    }

    @Override
    protected boolean acceptByDefault() {
      return false;
    }

    @Override
    protected boolean entriesArePositiveByDefault() {
      return true;
    }
  }

  /**
   * Buffer size to use when streaming data from artifacts and through
   * {@link GZIPOutputStream}.
   */
  private static final int BUF_SIZE = 10000;

  private static final String PROP_LEAVE_ORIGINALS = "precompress.leave.originals";

  private static final String PROP_PATH_REGEXES = "precompress.path.regexes";

  private static ConfigurationProperty findProperty(
      TreeLogger logger,
      Iterable<com.google.gwt.core.ext.linker.ConfigurationProperty> properties,
      String propName) throws UnableToCompleteException {
    for (ConfigurationProperty prop : properties) {
      if (prop.getName().equals(propName)) {
        return prop;
      }
    }

    logger.log(TreeLogger.ERROR, "Could not find configuration property "
        + propName);
    throw new UnableToCompleteException();
  }

  @Override
  public String getDescription() {
    return "PrecompressLinker";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts, boolean onePermutation)
      throws UnableToCompleteException {
    ConfigurationProperty leaveOriginalsProp = findProperty(logger,
        context.getConfigurationProperties(), PROP_LEAVE_ORIGINALS);
    boolean leaveOriginals = Boolean.valueOf(leaveOriginalsProp.getValues().get(
        0));

    PrecompressFilter filter = new PrecompressFilter(logger.branch(
        TreeLogger.TRACE, "Analyzing the path patterns"), findProperty(logger,
        context.getConfigurationProperties(), PROP_PATH_REGEXES).getValues());

    // Record the list of all paths for later lookup
    Set<String> allPaths = new HashSet<String>();
    for (EmittedArtifact art : artifacts.find(EmittedArtifact.class)) {
      allPaths.add(art.getPartialPath());
    }

    try {
      // Buffer for streaming data to be compressed
      byte[] buf = new byte[BUF_SIZE];

      ArtifactSet updated = new ArtifactSet(artifacts);
      for (EmittedArtifact art : artifacts.find(EmittedArtifact.class)) {
        if (art.getVisibility() != Visibility.Public) {
          // only compress things that will be served to the client
          continue;
        }
        if (art.getPartialPath().endsWith(".gz")) {
          // Already a compressed artifact
          continue;
        }
        if (allPaths.contains(art.getPartialPath() + ".gz")) {
          // It's already been compressed
          continue;
        }
        if (!filter.isIncluded(logger.branch(TreeLogger.TRACE,
            "Checking the path patterns"), art.getPartialPath())) {
          continue;
        }

        TreeLogger compressBranch = logger.branch(TreeLogger.TRACE,
            "Compressing " + art.getPartialPath());

        InputStream originalBytes = art.getContents(compressBranch);
        ByteArrayOutputStream compressedBytes = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(compressedBytes);

        int originalLength = 0;
        int n;
        while ((n = originalBytes.read(buf)) > 0) {
          originalLength += n;
          gzip.write(buf, 0, n);
        }
        gzip.close();

        byte[] compressed = compressedBytes.toByteArray();
        if (compressed.length < originalLength) {
          updated.add(emitBytes(compressBranch, compressed,
              art.getPartialPath() + ".gz"));
          if (!leaveOriginals) {
            updated.remove(art);
          }
        }
      }
      return updated;
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unexpected exception", e);
      throw new UnableToCompleteException();
    }
  }
}
