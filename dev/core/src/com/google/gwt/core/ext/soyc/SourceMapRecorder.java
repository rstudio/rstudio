/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.core.ext.soyc;

import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.core.linker.SymbolMapsLinker;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.thirdparty.debugging.sourcemap.FilePosition;
import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapFormat;
import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapGenerator;
import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapGeneratorFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates Closure Compatible SourceMaps.
 */
public class SourceMapRecorder {

  public static List<SyntheticArtifact> makeSourceMapArtifacts(
      List<Map<Range, SourceInfo>> sourceInfoMaps,
      int permutationId) {
    List<SyntheticArtifact> toReturn = new ArrayList<SyntheticArtifact>();
    recordSourceMap(sourceInfoMaps, toReturn, permutationId);
    return toReturn;
  }

  public static void recordSourceMap(List<Map<Range, SourceInfo>> sourceInfoMaps,
       List<SyntheticArtifact> artifacts, int permutationId) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    SourceMapGenerator generator = SourceMapGeneratorFactory.getInstance(SourceMapFormat.V3);
    OutputStreamWriter out = new OutputStreamWriter(baos);
    int fragment = 0;
    if (!sourceInfoMaps.isEmpty()) {
      for (Map<Range, SourceInfo> sourceMap : sourceInfoMaps) {
        generator.reset();
        Set<Range> rangeSet = sourceMap.keySet();
        Range[] ranges = rangeSet.toArray(new Range[rangeSet.size()]);
        Arrays.sort(ranges, Range.DEPENDENCY_ORDER_COMPARATOR);
        for (Range r : ranges) {
          SourceInfo si = sourceMap.get(r);
          if (si.getFileName() == null || si.getStartLine() < 0) {
            // skip over synthetics with no Java source
            continue;
          }
          if (r.getStartLine() == 0 || r.getEndLine() == 0) {
            // or other bogus entries that appear
            continue;
          }

          // Starting with V3, SourceMap line numbers are zero-based.
          // GWT's line numbers for Java files originally came from the JDT, which is 1-based,
          // so adjust them here to avoid an off-by-one error in debuggers.
          generator.addMapping(si.getFileName(), null,
              new FilePosition(si.getStartLine() - 1, 0),
              new FilePosition(r.getStartLine(), r.getStartColumn()),
              new FilePosition(r.getEndLine(), r.getEndColumn()));
        }
        try {
          baos.reset();
          generator.appendTo(out, "sourceMap" + fragment);
          out.flush();
          artifacts.add(new SymbolMapsLinker.SourceMapArtifact(permutationId, fragment,
              baos.toByteArray()));
          fragment++;
        } catch (IOException e) {
          throw new InternalCompilerException(e.toString(), e);
        }
      }
    }
  }
}
