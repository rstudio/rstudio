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
import com.google.gwt.dev.jjs.JsSourceMap;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapGeneratorV3;
import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapParseException;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Creates Closure Compatible SourceMaps.
 */
public class SourceMapRecorder {

  public static List<SyntheticArtifact> makeSourceMapArtifacts(int permutationId,
      List<JsSourceMap> sourceInfoMaps) {
    try {
      return (new SourceMapRecorder(permutationId)).recordSourceMap(sourceInfoMaps);
    } catch (Exception e) {
      throw new InternalCompilerException(e.toString(), e);
    }
  }

  protected final int permutationId;

  protected SourceMapRecorder(int permutationId) {
    this.permutationId = permutationId;
  }

  protected List<SyntheticArtifact> recordSourceMap(List<JsSourceMap> sourceInfoMaps)
      throws IOException, JSONException, SourceMapParseException {
    List<SyntheticArtifact> toReturn = Lists.newArrayList();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    SourceMapGeneratorV3 generator = new SourceMapGeneratorV3();
    int fragment = 0;
    if (!sourceInfoMaps.isEmpty()) {
      for (JsSourceMap sourceMap : sourceInfoMaps) {
        generator.reset();
        addMappings(new SourceMappingWriter(generator), sourceMap);
        updateSourceMap(generator, fragment);

        baos.reset();
        OutputStreamWriter out = new OutputStreamWriter(baos);
        generator.appendTo(out, "sourceMap" + fragment);
        out.flush();
        toReturn.add(new SymbolMapsLinker.SourceMapArtifact(permutationId, fragment,
            baos.toByteArray()));
        fragment++;
      }
    }
    return toReturn;
  }

  /**
   * A hook allowing a subclass to add more info to the sourcemap for a given fragment.
   */
  protected void updateSourceMap(SourceMapGeneratorV3 generator, int fragment)
      throws SourceMapParseException { }

  /**
   * A hook allowing a subclass to populate the "names" field in the sourcemap.
   *
   * <p>The name is currently always a Java identifier, but in theory may be any Java expression.
   * For example, a compiler-introduced temporary variable could be annotated with the expression
   * that produced it.
   *
   * <p>The name should only be set if the JavaScript range covers one JavaScript identifier.
   * (Otherwise return null.)
   */
  protected String getJavaName(SourceInfo sourceInfo) {
    return null;
  }

  /**
   * Adds the source mappings for one JavaScript file to its sourcemap.
   * Consolidates adjacent or overlapping ranges to reduce the amount of data that the JavaScript
   * debugger has to load.
   */
  private void addMappings(SourceMappingWriter output, JsSourceMap mappings) {
    Set<Range> rangeSet = mappings.keySet();

    Range[] ranges = rangeSet.toArray(new Range[rangeSet.size()]);
    Arrays.sort(ranges, Range.DEPENDENCY_ORDER_COMPARATOR);

    for (Range r : ranges) {
      SourceInfo info = mappings.get(r);

      if (info == SourceOrigin.UNKNOWN || info.getFileName() == null || info.getStartLine() < 0) {
        // skip a synthetic with no Java source
        continue;
      }
      if (r.getStartLine() == 0 || r.getEndLine() == 0) {
        // skip a bogus entry
        // JavaClassHierarchySetupUtil:prototypesByTypeId is pruned here. Maybe others too?
        continue;
      }

      output.addMapping(r, info, getJavaName(info));
    }
    output.flush();
  }
}
