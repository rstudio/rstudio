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
package com.google.gwt.junit.linker;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.core.linker.SymbolMapsLinker;

import java.io.ByteArrayOutputStream;

/**
 * Emits the symbol maps into the application output directory so that the
 * JUnitHostImpl servlet can read them.
 */
@Shardable
public class JUnitSymbolMapsLinker extends SymbolMapsLinker {

  public static final String SYMBOL_MAP_DIR = ".junit_symbolMaps/";

  @Override
  protected void doEmitSymbolMap(TreeLogger logger, ArtifactSet artifacts,
      CompilationResult result, ByteArrayOutputStream out)
      throws UnableToCompleteException {
    // Collaborate with JUnitHostImpl.loadSymbolMap
    String partialPath = SYMBOL_MAP_DIR + result.getStrongName()
        + STRONG_NAME_SUFFIX;

    EmittedArtifact symbolMapArtifact = emitBytes(logger, out.toByteArray(),
        partialPath);

    artifacts.add(symbolMapArtifact);
  }

  @Override
  protected SyntheticArtifact emitSourceMapString(TreeLogger logger, String contents,
      String partialPath) throws UnableToCompleteException {
    return emitString(logger, contents, SYMBOL_MAP_DIR + partialPath);
  }
}
