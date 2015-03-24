/*
 * Copyright 2015 Google Inc.
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

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.AbstractLinker;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.DelegatingCompilationResult;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.core.ext.linker.impl.StatementRangesBuilder;
import com.google.gwt.core.linker.SymbolMapsLinker;
import com.google.gwt.util.tools.Utility;

import java.io.IOException;

/**
 * A linker which prepends the code to bootstrap closurehelpers.js into the primary fragment.
 */
@Shardable
@LinkerOrder(LinkerOrder.Order.PRE)
public class ClosureHelpersLinker extends AbstractLinker {

  @Override
  public String getDescription() {
    return "ClosureHelpersLinker";
  }

  @Override
  public ArtifactSet link(final TreeLogger logger, LinkerContext context, ArtifactSet artifacts,
    boolean onePermutation) throws UnableToCompleteException {
    if (!onePermutation) {
      // only do work on final link
      return artifacts;
    }

    ArtifactSet updatedArtifacts = new ArtifactSet(artifacts);
    for (final CompilationResult compilationResult : artifacts.find(CompilationResult.class)) {
      final String closureHelpers = getClosureHelpers(logger);
      DelegatingCompilationResult updatedResult = new DelegatingCompilationResult(
          ClosureHelpersLinker.class, compilationResult) {
        String rewrittenJs[] = null;
        StatementRanges ranges[] = null;

        @Override
        public String[] getJavaScript() {
          if (rewrittenJs == null) {
            rewrittenJs = compilationResult.getJavaScript().clone();
            rewrittenJs[0] = closureHelpers + rewrittenJs[0];
          }
          return rewrittenJs;
        }

        @Override
        public StatementRanges[] getStatementRanges() {
          if (ranges == null) {
            ranges = compilationResult.getStatementRanges().clone();
            StatementRanges oldStmtRange = ranges[0];
            StatementRangesBuilder builder = new StatementRangesBuilder();
            builder.addStartPosition(0);
            builder.addEndPosition(closureHelpers.length());
            builder.append(oldStmtRange);
            ranges[0] = builder.build();
          }
          return ranges;
        }
      };
      updatedArtifacts.remove(compilationResult);
      updatedArtifacts.add(updatedResult);
      SymbolMapsLinker.ScriptFragmentEditsArtifact editArtifact =
          new SymbolMapsLinker.ScriptFragmentEditsArtifact(null, 0);
      editArtifact.prefixLines(closureHelpers);
      updatedArtifacts.add(editArtifact);
      break;
    }
    return updatedArtifacts;
  }

  private String getClosureHelpers(TreeLogger logger) throws UnableToCompleteException {
    try {
      return Utility.getFileFromClassPath("com/google/gwt/junit/linker/closurehelpers.js");
    } catch (IOException e) {
      logger.log(Type.ERROR, "Can't load closurehelpers.js", e);
      throw new UnableToCompleteException();
    }
  }
}
