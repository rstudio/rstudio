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
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;

/**
 * Emit a file contating a map of RPC proxy classes to the partial path of the
 * RPC policy file.
 */
@LinkerOrder(Order.PRE)
public class RpcPolicyManifestLinker extends AbstractLinker {

  @Override
  public String getDescription() {
    return "RPC policy file manifest";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {
    artifacts = new ArtifactSet(artifacts);

    StringBuilder contents = new StringBuilder();
    contents.append("# Module ").append(context.getModuleName()).append("\n");
    contents.append("# RPC service class, partial path of RPC policy file\n");

    // Loop over all policy file artifacts
    for (RpcPolicyFileArtifact artifact : artifacts.find(RpcPolicyFileArtifact.class)) {
      // com.foo.Service, 12345.rpc.txt
      contents.append(artifact.getProxyClass()).append(", ").append(
          artifact.getEmittedArtifact().getPartialPath()).append("\n");
    }

    // The name of the linker is prepended as a directory prefix
    EmittedArtifact manifest = emitString(logger, contents.toString(),
        "manifest.txt");
    manifest.setPrivate(true);
    artifacts.add(manifest);

    return artifacts;
  }
}
