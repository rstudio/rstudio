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
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.Shardable;

/**
 * This linker emits {@link RpcLogArtifact}s as output files.
 */
@LinkerOrder(Order.POST)
@Shardable
public class RpcLogLinker extends AbstractLinker {

  @Override
  public String getDescription() {
    return "RPC log linker";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts, boolean onePermutation)
      throws UnableToCompleteException {
    if (onePermutation) {
      ArtifactSet toReturn = new ArtifactSet(artifacts);
      logger = logger.branch(TreeLogger.TRACE, "Emitting RPC log files");

      for (CompilationResult result : artifacts.find(CompilationResult.class)) {
        for (RpcLogArtifact logArt : artifacts.find(RpcLogArtifact.class)) {
          String policyStrongName = logArt.getSerializationPolicyStrongName();
          if (policyStrongName.equals(RpcLogArtifact.UNSPECIFIED_STRONGNAME)) {
            /*
             * If the artifact has no strong name of its own, use the
             * compilation strong name.
             */
            policyStrongName = result.getStrongName();
          }
          EmittedArtifact art = emitBytes(logger, logArt.getContents(),
              logArt.getQualifiedSourceName() + "-" + policyStrongName
                  + ".rpc.log");
          art.setVisibility(Visibility.Private);
          toReturn.add(art);
        }
      }

      return toReturn;
    } else {
      return artifacts;
    }
  }
}
