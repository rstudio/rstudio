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
package com.google.gwt.core.linker;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.AbstractLinker;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.SortedMap;

/**
 * This Linker exports the symbol maps associated with each compilation result
 * as a private file. The names of the symbol maps files are computed by
 * appending {@value #STRONG_NAME_SUFFIX} to the value returned by
 * {@link CompilationResult#getStrongName()}.
 */
@LinkerOrder(Order.POST)
public class SymbolMapsLinker extends AbstractLinker {

  /**
   * This value is appended to the strong name of the CompilationResult to form
   * the symbol map's filename.
   */
  public static final String STRONG_NAME_SUFFIX = "_symbolMap.properties";

  @Override
  public String getDescription() {
    return "Export CompilationResult symbol maps";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {

    artifacts = new ArtifactSet(artifacts);

    for (CompilationResult result : artifacts.find(CompilationResult.class)) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PrintWriter pw = new PrintWriter(out);

      doWriteSymbolMap(logger, result, pw);
      pw.close();

      doEmitSymbolMap(logger, artifacts, result, out);
    }

    return artifacts;
  }

  /**
   * Override to change the manner in which the symbol map is emitted.
   */
  protected void doEmitSymbolMap(TreeLogger logger, ArtifactSet artifacts,
      CompilationResult result, ByteArrayOutputStream out)
      throws UnableToCompleteException {
    EmittedArtifact symbolMapArtifact = emitBytes(logger, out.toByteArray(),
        result.getStrongName() + STRONG_NAME_SUFFIX);
    symbolMapArtifact.setPrivate(true);
    artifacts.add(symbolMapArtifact);
  }

  /**
   * Override to change the format of the symbol map.
   */
  protected void doWriteSymbolMap(TreeLogger logger, CompilationResult result,
      PrintWriter pw) throws UnableToCompleteException {
    for (SortedMap<SelectionProperty, String> map : result.getPropertyMap()) {
      pw.print("# { ");

      boolean needsComma = false;
      for (Map.Entry<SelectionProperty, String> entry : map.entrySet()) {
        if (needsComma) {
          pw.print(" , ");
        } else {
          needsComma = true;
        }

        pw.print("'");
        pw.print(entry.getKey().getName());
        pw.print("' : '");
        pw.print(entry.getValue());
        pw.print("'");
      }
      pw.println(" }");
    }

    for (Map.Entry<String, String> entry : result.getSymbolMap().entrySet()) {
      // Don't use an actual Properties object because it emits a timestamp
      pw.print(entry.getKey());
      pw.print(" = ");
      pw.print(entry.getValue());
      pw.println();
    }
  }
}
