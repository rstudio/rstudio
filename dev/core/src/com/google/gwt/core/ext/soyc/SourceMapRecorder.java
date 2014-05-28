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
import com.google.gwt.dev.jjs.Correlation;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.JsSourceMap;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceInfoCorrelation;
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

  /**
   * Generates a sourcemap for each fragment in the list.
   *
   * @param sourceFilePrefix the prefix that a debugger should add to the beginning of each
   * filename in a sourcemap to determine the file's full URL.
   * If null, filenames are relative to the sourcemap's URL.
   */
  public static List<SyntheticArtifact> exec(int permutationId,
      List<JsSourceMap> fragmentMaps, String sourceFilePrefix) {
    try {
      return new SourceMapRecorder(permutationId, fragmentMaps, sourceFilePrefix).createArtifacts();
    } catch (Exception e) {
      throw new InternalCompilerException(e.toString(), e);
    }
  }

  /**
   * Generates a sourcemap for each fragment in the list, with JavaScript-to-Java
   * name mappings included.
   */
  public static List<SyntheticArtifact> execWithJavaNames(int permutationId,
      List<JsSourceMap> fragmentMaps, String sourceFilePrefix) {
    try {
      SourceMapRecorder recorder = new SourceMapRecorder(permutationId, fragmentMaps,
          sourceFilePrefix);
      recorder.wantJavaNames = true;
      return recorder.createArtifacts();
    } catch (Exception e) {
      throw new InternalCompilerException(e.toString(), e);
    }
  }

  private final int permutationId;
  private final List<JsSourceMap> fragmentMaps;
  private final String sourceRoot;
  private boolean wantJavaNames;

  private SourceMapRecorder(int permutationId, List<JsSourceMap> fragmentMaps, String sourceRoot) {
    this.permutationId = permutationId;
    this.fragmentMaps = fragmentMaps;
    this.sourceRoot = sourceRoot;
  }

  private List<SyntheticArtifact> createArtifacts()
      throws IOException, JSONException, SourceMapParseException {
    List<SyntheticArtifact> toReturn = Lists.newArrayList();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    SourceMapGeneratorV3 generator = new SourceMapGeneratorV3();
    int fragment = 0;
    for (JsSourceMap sourceMap : fragmentMaps) {
      generator.reset();

      if (sourceRoot != null) {
        generator.setSourceRoot(sourceRoot);
      }
      addExtensions(generator, fragment);
      addMappings(new SourceMappingWriter(generator), sourceMap);

      baos.reset();
      OutputStreamWriter out = new OutputStreamWriter(baos);
      generator.appendTo(out, "sourceMap" + fragment);
      out.flush();
      toReturn.add(new SymbolMapsLinker.SourceMapArtifact(permutationId, fragment,
          baos.toByteArray(), sourceRoot));
      fragment++;
    }
    return toReturn;
  }

  private void addExtensions(SourceMapGeneratorV3 generator, int fragment)
      throws SourceMapParseException {
    // We don't convert to a string here so that the values will be added
    // to the JSON as a number instead of a string.
    generator.addExtension("x_gwt_permutation", permutationId);
    generator.addExtension("x_gwt_fragment", fragment);
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

  /**
   * Returns the name to be added to the "names" field in the sourcemap.
   *
   * <p>The name is currently always a Java identifier, but in theory may be any Java expression.
   * For example, a compiler-introduced temporary variable could be annotated with the expression
   * that produced it.
   *
   * <p>The name should only be set if the JavaScript range covers one JavaScript identifier.
   * (Otherwise return null.)
   */
  private String getJavaName(SourceInfo sourceInfo) {

    if (!wantJavaNames) {
      return null;
    }

    if (!(sourceInfo instanceof SourceInfoCorrelation)) {
      return null;
    }

    Correlation correlation = ((SourceInfoCorrelation) sourceInfo).getPrimaryCorrelation();
    if (correlation == null) {
      return null;
    }

    // Conserve space by not recording the package name. The sourcemap already contains the full
    // path of the Java file (in the "sources" field), which is usually enough to identify
    // the package. (The name may be a synthetic method name.)
    return correlation.getIdent();
  }
}
