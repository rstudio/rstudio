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
package com.google.gwt.rpc.linker;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.AbstractLinker;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.SymbolData;
import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.rpc.server.WebModeClientOracle.Builder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exports data required by server components for directly-evalable RPC.
 */
@LinkerOrder(Order.PRE)
@Shardable
public class ClientOracleLinker extends AbstractLinker {

  private static final String SUFFIX = ".gwt.rpc";

  @Override
  public String getDescription() {
    return "deRPC linker";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts, boolean onePermutation)
      throws UnableToCompleteException {
    if (onePermutation) {
      artifacts = new ArtifactSet(artifacts);

      Map<String, List<String>> allSerializableFields = new HashMap<String, List<String>>();

      for (RpcDataArtifact data : artifacts.find(RpcDataArtifact.class)) {
        allSerializableFields.putAll(data.getOperableFields());
      }

      for (CompilationResult result : artifacts.find(CompilationResult.class)) {
        Builder builder = new Builder();

        for (Map.Entry<String, List<String>> entry : allSerializableFields.entrySet()) {
          builder.setSerializableFields(entry.getKey(), entry.getValue());
        }

        for (SymbolData symbolData : result.getSymbolMap()) {
          
          String castableTypeMapString =
              (symbolData.getCastableTypeMap() == null) ? null :
                symbolData.getCastableTypeMap().toJs();
       
          builder.add(symbolData.getSymbolName(), symbolData.getJsniIdent(),
              symbolData.getClassName(), symbolData.getMemberName(),
              symbolData.getQueryId(), 
              new CastableTypeDataImpl(castableTypeMapString),
              symbolData.getSeedId());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
          builder.getOracle().store(out);
        } catch (IOException e) {
          // Should generally not happen
          logger.log(TreeLogger.ERROR, "Unable to store deRPC data", e);
          throw new UnableToCompleteException();
        }

        SyntheticArtifact a = emitBytes(logger, out.toByteArray(),
            result.getStrongName() + SUFFIX);
        artifacts.add(a);
      }
    }
    return artifacts;
  }

}
