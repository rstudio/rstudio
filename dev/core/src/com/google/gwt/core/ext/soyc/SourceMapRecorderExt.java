/*
 * Copyright 2013 Google Inc.
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
import com.google.gwt.dev.jjs.Correlation;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceInfoCorrelation;
import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapGeneratorV3;
import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapParseException;

import java.util.List;
import java.util.Map;

/**
 * Creates Closure Compatible SourceMaps with named ranges.
 */
public class SourceMapRecorderExt extends SourceMapRecorder {

  public static final String PERMUTATION_EXT = "x_gwt_permutation";

  public static List<SyntheticArtifact> makeSourceMapArtifacts(int permutationId,
      List<Map<Range, SourceInfo>> sourceInfoMaps) {
    try {
      return (new SourceMapRecorderExt(permutationId)).recordSourceMap(sourceInfoMaps);
    } catch (Exception e) {
      throw new InternalCompilerException(e.toString(), e);
    }
  }

  protected SourceMapRecorderExt(int permutationId) {
    super(permutationId);
  }

  @Override
  protected void updateSourceMap(SourceMapGeneratorV3 generator, int fragment)
      throws SourceMapParseException {
    generator.addExtension(PERMUTATION_EXT, new Integer(permutationId));
    generator.addExtension("x_gwt_fragment", new Integer(fragment));
  }

  @Override
  protected String getJavaName(SourceInfo sourceInfo) {
    // We can discard Unknown or not-so-valid (eg. com.google.gwt.dev.js.ast.JsProgram)
    // sourceInfo
    String rangeName = null;
    if (sourceInfo instanceof SourceInfoCorrelation) {
      Correlation correlation = ((SourceInfoCorrelation) sourceInfo).getPrimaryCorrelation();
      if (correlation != null) {
        // We can reduce name sizes by removing the left part corresponding to the
        // package name, eg. com.google.gwt.client. Because this is already in the file name.
        // This name includes static/synth method names
        rangeName = correlation.getIdent();
      }
    }
    return rangeName;
  }
}
