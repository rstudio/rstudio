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
package com.google.gwt.user.linker.rpc;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.AbstractLinker;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.util.Util;
import com.google.gwt.user.rebind.rpc.ProxyCreator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Emit a file containing a map of RPC proxy classes to the partial path of the
 * RPC policy file.
 */
@LinkerOrder(Order.PRE)
@Shardable
public class RpcPolicyManifestLinker extends AbstractLinker {
  private static final String MANIFEST_TXT = "manifest.txt";

  /**
   * The main body of the manifest. It is built up as per-permutation manifests
   * are looped over.
   */
  private StringBuilder manifestBody = new StringBuilder();

  @Override
  public String getDescription() {
    return "RPC policy file manifest";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts, boolean onePermutation)
      throws UnableToCompleteException {
    if (onePermutation) {
      return artifacts;
    } else {
      for (EmittedArtifact art : artifacts.find(EmittedArtifact.class)) {
        if (art.getPartialPath().startsWith(ProxyCreator.MANIFEST_ARTIFACT_DIR)) {
          readOneManifest(logger, art.getContents(logger));
        }
      }

      ArtifactSet toReturn = new ArtifactSet(artifacts);
      SyntheticArtifact manifestArt = emitString(logger,
          generateManifest(context), MANIFEST_TXT);
      manifestArt.setVisibility(Visibility.LegacyDeploy);
      toReturn.add(manifestArt);
      return toReturn;
    }
  }

  /**
   * Compute a manifest for all RPC policy files seen so far.
   */
  private String generateManifest(LinkerContext context) {
    StringBuilder sb = new StringBuilder();
    sb.append("# Module " + context.getModuleName() + "\n");
    sb.append("# RPC service class, partial path of RPC policy file\n");
    sb.append(manifestBody.toString());
    return sb.toString();
  }

  /**
   * Read one manifest and close the input stream.
   */
  private void readOneManifest(TreeLogger logger, InputStream manifestStream)
      throws UnableToCompleteException {
    Map<String, String> entries = new HashMap<String, String>();
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(
          manifestStream, Util.DEFAULT_ENCODING));
      String line;
      while ((line = reader.readLine()) != null) {
        int idx = line.indexOf(':');
        if (idx < 0) {
          throw new InternalCompilerException(
              "invalid selection information line: " + line);
        }
        String propName = line.substring(0, idx).trim();
        String propValue = line.substring(idx + 1).trim();
        entries.put(propName, propValue);
      }
      reader.close();
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unexpected IOException", e);
      throw new UnableToCompleteException();
    }

    String serviceClass = entries.get("serviceClass");
    if (serviceClass == null) {
      logger.log(TreeLogger.ERROR,
          "Internal error: manifest file does not include a serviceClass");
      throw new UnableToCompleteException();
    }
    String path = entries.get("path");
    if (path == null) {
      logger.log(TreeLogger.ERROR,
          "Internal error: manifest file does not include a path");
      throw new UnableToCompleteException();
    }

    manifestBody.append(serviceClass);
    manifestBody.append(", ");
    manifestBody.append(path);
    manifestBody.append("\n");
  }
}
