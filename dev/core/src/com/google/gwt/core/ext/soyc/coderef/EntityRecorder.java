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
package com.google.gwt.core.ext.soyc.coderef;

import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.core.ext.soyc.SourceMapRecorderExt;
import com.google.gwt.core.ext.soyc.coderef.EntityDescriptor.Fragment;
import com.google.gwt.core.linker.SoycReportLinker;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.JsSourceMap;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.jjs.impl.codesplitter.FragmentPartitioningResult;
import com.google.gwt.dev.js.SizeBreakdown;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.util.tools.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Creates the entities artifacts for the new soyc.
 */
public class EntityRecorder {

  public static final String ENTITIES         = "entities";
  public static final String FRAGMENT_ID      = "id";
  public static final String FRAGMENT_SIZE    = "size";
  public static final String FRAGMENT_STR_VAR = "strAndVarSize";
  public static final String FRAGMENT_POINTS  = "runAsyncs";
  public static final String FRAGMENTS        = "fragments";
  public static final String INITIAL_SEQUENCE = "initialSequence";

  public static List<SyntheticArtifact> makeSoycArtifacts(int permutationId,
      List<JsSourceMap> sourceInfoMaps, JavaToJavaScriptMap jjsmap,
      SizeBreakdown[] sizeBreakdowns, DependencyGraphRecorder codeGraph, JProgram jprogram) {

    EntityRecorder recorder = new EntityRecorder(sizeBreakdowns, permutationId);
    try {
      recorder.recordCodeReferences(codeGraph, sizeBreakdowns, jjsmap);
      recorder.recordFragments(jprogram);
      // record source map with named ranges
      recorder.toReturn.addAll(SourceMapRecorderExt.makeSourceMapArtifacts(
          permutationId, sourceInfoMaps));
    } catch (Exception e) {
      throw new InternalCompilerException(e.toString(), e);
    }

    return recorder.toReturn;
  }

  private final List<SyntheticArtifact> toReturn = Lists.newArrayList();
  private final int permutationId;
  private final int[] fragmentSizes;
  // String and variable sizes
  private final int[] otherSizes;
  private JSONObject[] sizeMetrics;

  private EntityRecorder(SizeBreakdown[] sizeBreakdowns, int permutationId) {
    fragmentSizes = new int[sizeBreakdowns.length];
    otherSizes = new int[sizeBreakdowns.length];
    for (int i = 0; i < sizeBreakdowns.length; i++) {
      fragmentSizes[i] = sizeBreakdowns[i].getSize();
      otherSizes[i] = 0;
    }
    this.permutationId = permutationId;
  }

  private JSONObject getSizeMetrics(int fragment) throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put(FRAGMENT_ID, fragment);
    obj.put(FRAGMENT_SIZE, this.fragmentSizes[fragment]);
    obj.put(FRAGMENT_STR_VAR, this.otherSizes[fragment]);
    return obj;
  }

  private void recordCodeReferences(DependencyGraphRecorder codeGraph,
      SizeBreakdown[] sizeBreakdowns, JavaToJavaScriptMap jjsmap)
      throws IOException, JSONException {
    this.sizeMetrics = new JSONObject[sizeBreakdowns.length];
    // add sizes and other info
    for (int i = 0; i < sizeBreakdowns.length; i++) {
      for (Entry<JsName, Integer> kv : sizeBreakdowns[i].getSizeMap().entrySet()) {
        JsName name = kv.getKey();
        int size = kv.getValue();
        // find method
        JMethod method = jjsmap.nameToMethod(name);
        if (method != null) {
          codeGraph.methodDescriptorFrom(method).addFragment(new Fragment(i, size));
          continue;
        }
        // find field
        JField field = jjsmap.nameToField(name);
        if ((field != null) && (field.getEnclosingType() != null)) {
          codeGraph.classDescriptorFrom(field.getEnclosingType()).fieldFrom(field)
              .addFragment(new Fragment(i, size));
          continue;
        }
        // find class
        JClassType type = jjsmap.nameToType(name);
        if (type != null) {
          codeGraph.classDescriptorFrom(type).addFragment(new Fragment(i, size));
          continue;
        }
        // otherwise is a string or variable
        this.otherSizes[i] += size;
      }
      sizeMetrics[i] = this.getSizeMetrics(i);
    }
    // adding symbol names considering that jjsmap has all obfuscated name for all entities
    for (ClassDescriptor cls : codeGraph.getCodeModel().values()) {
      JDeclaredType type = cls.getTypeReference();
      if (type instanceof JClassType) {
        JsName jsName = jjsmap.nameForType((JClassType) type);
        if (jsName != null) {
          cls.addObfuscatedName(jsName.getShortIdent());
        }
      }
      for (MethodDescriptor mth : cls.getMethods()) {
        for (JMethod jMethod : mth.getMethodReferences()) {
          JsName jsName = jjsmap.nameForMethod(jMethod);
          if (jsName != null) {
            mth.addObfuscatedName(jsName.getShortIdent());
          }
        }
      }
      for (JField field : type.getFields()) {
        JsName jsName = jjsmap.nameForField(field);
        if (jsName != null) {
          cls.fieldFrom(field).addObfuscatedName(jsName.getShortIdent());
        }
      }
    }
    // build json
    addArtifactFromJson(
        EntityDescriptorJsonTranslator.writeJson(
            PackageDescriptor.from(codeGraph.getCodeModel())),
            ENTITIES + permutationId + ".json");
  }

  private String addArtifactFromJson(Object value, String named) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(baos);
    writer.write(value.toString());
    Utility.close(writer);

    // TODO(ocallau) Must be updated with the correct/final linker
    SyntheticArtifact artifact = new SyntheticArtifact(
        SoycReportLinker.class, named, baos.toByteArray());
    artifact.setVisibility(Visibility.LegacyDeploy);

    toReturn.add(artifact);
    return named;
  }

  private void recordFragments(JProgram jprogram) throws IOException, JSONException {
    JSONObject jsonPoints = new JSONObject();
    // adding runAsyncs, if any
    FragmentPartitioningResult partitionResult = jprogram.getFragmentPartitioningResult();
    // a FragmentPartitioningResult' instance exist if and only if there are runAyncs nodes
    if (partitionResult != null) {
      Map<Integer, Set<String>> runAsyncPerFragment = Maps.newHashMap();
      for (JRunAsync runAsync : jprogram.getRunAsyncs()) {
        int fragmentId = partitionResult.getFragmentForRunAsync(runAsync.getRunAsyncId());
        Set<String> runAsyncNames = runAsyncPerFragment.get(fragmentId);
        if (runAsyncNames == null) {
          runAsyncNames = Sets.newHashSet();
        }
        runAsyncNames.add(runAsync.getName());
        runAsyncPerFragment.put(fragmentId, runAsyncNames);
      }
      for (Integer idx : runAsyncPerFragment.keySet()) {
        sizeMetrics[idx].put(FRAGMENT_POINTS, runAsyncPerFragment.get(idx));
      }
      // initial fragment sequence points
      JSONArray initialSequence = new JSONArray();
      for (int fragId : jprogram.getInitialFragmentIdSequence()) {
        initialSequence.put(partitionResult.getFragmentForRunAsync(fragId));
      }
      jsonPoints.put(INITIAL_SEQUENCE, initialSequence);
    }

    jsonPoints.put(FRAGMENTS, this.sizeMetrics);
    addArtifactFromJson(jsonPoints, FRAGMENTS + permutationId + ".json");
  }
}
