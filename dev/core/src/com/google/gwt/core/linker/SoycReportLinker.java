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

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.linker.Artifact;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.ModuleMetricsArtifact;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.SyntheticArtifact;
import com.google.gwt.core.ext.linker.Transferable;
import com.google.gwt.soyc.CompilerMetricsXmlFormatter;
import com.google.gwt.soyc.SoycDashboard;
import com.google.gwt.soyc.io.ArtifactsOutputDirectory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Converts SOYC report files into emitted private artifacts.
 */
@LinkerOrder(Order.POST)
@Shardable
public class SoycReportLinker extends Linker {
  /**
   * An artifact giving a one-line description of a permutation ID in terms of
   * its deferred bindings.
   */
  @Transferable
  private static class PermDescriptionArtifact extends
      Artifact<PermDescriptionArtifact> {

    private List<String> permDesc;
    private int permId;

    public PermDescriptionArtifact(int permId, List<String> permDesc) {
      super(SoycReportLinker.class);
      this.permId = permId;
      this.permDesc = permDesc;
    }

    public List<String> getPermDesc() {
      return permDesc;
    }

    public int getPermId() {
      return permId;
    }

    @Override
    public int hashCode() {
      return permId;
    }

    @Override
    protected int compareToComparableArtifact(PermDescriptionArtifact o) {
      int cmp;
      cmp = permId - o.getPermId();
      if (cmp != 0) {
        return cmp;
      }

      cmp = permDesc.size() - o.getPermDesc().size();
      if (cmp != 0) {
        return cmp;
      }

      for (int i = 0; i < permDesc.size(); i++) {
        cmp = permDesc.get(i).compareTo(o.getPermDesc().get(i));
        if (cmp != 0) {
          return cmp;
        }
      }

      return 0;
    }

    @Override
    protected Class<PermDescriptionArtifact> getComparableArtifactType() {
      return PermDescriptionArtifact.class;
    }
  }

  @Override
  public String getDescription() {
    return "Emit compile report artifacts";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts, boolean onePermutation) {

    boolean reportFilesPresent = anyReportFilesPresent(artifacts);
    boolean metricsPresent = anyCompilerMetricsPresent(artifacts);
    
    if (!reportFilesPresent && !metricsPresent) {
      return artifacts;
    }
    
    artifacts = new ArtifactSet(artifacts);
    
    if (!onePermutation) {
      buildCompilerMetricsXml(artifacts);
    }
    
    if (reportFilesPresent) {
      if (onePermutation) {
        emitPermutationDescriptions(artifacts);
      } else {
        buildTopLevelFiles(logger, artifacts);
      }
    }
    
    return artifacts;
  }

  /**
   * Check whether an artifact set contains any compilerMetrics.
   */  
  boolean anyCompilerMetricsPresent(ArtifactSet artifacts) {
    return  !artifacts.find(ModuleMetricsArtifact.class).isEmpty(); 
  }
  
  /**
   * Check whether an artifact set contains any SOYC report documents.
   */
  boolean anyReportFilesPresent(ArtifactSet artifacts) {
    String prefix1 = ArtifactsOutputDirectory.COMPILE_REPORT_DIRECTORY + "/";
    String prefix2 = "soycReport/" + prefix1;

    for (EmittedArtifact art : artifacts.find(EmittedArtifact.class)) {
      if (art.getPartialPath().startsWith(prefix1)) {
        return true;
      }
      if (art.getPartialPath().startsWith(prefix2)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Compiler Metrics are captured in the module load, precompilation,
   * and compile permutations step, then all merged together into a single
   * XML file as output.  That file can then be consumed by external 
   * reporting tools.
   */
  private void buildCompilerMetricsXml(ArtifactSet artifacts) {
    ModuleMetricsArtifact moduleMetrics = null;
    Set<ModuleMetricsArtifact> moduleMetricsSet = artifacts.find(
        ModuleMetricsArtifact.class);
    if (!moduleMetricsSet.isEmpty()) {
      for (ModuleMetricsArtifact metrics : moduleMetricsSet) {
        moduleMetrics = metrics;
        // We only need one module metrics definition.
        break;
      }
    }

    // No module metrics? Then we'll skip creating the compilerMetrics output
    if (moduleMetrics == null) {
      return;
    }

    byte[] xmlResult = CompilerMetricsXmlFormatter.writeMetricsAsXml(
        artifacts, moduleMetrics);
    EmittedArtifact metricsArtifact = new SyntheticArtifact(
        SoycReportLinker.class, "compilerMetrics.xml", xmlResult);
    metricsArtifact.setVisibility(Visibility.Private);
    artifacts.add(metricsArtifact);
  }

  private void buildTopLevelFiles(TreeLogger logger,
      ArtifactSet artifacts) {
    ArtifactsOutputDirectory out = new ArtifactsOutputDirectory();
    try {
      new SoycDashboard(out).generateCrossPermutationFiles(
          extractPermutationDescriptions(artifacts));
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR,
          "Error while generating a Story of Your Compile", e);
      e.printStackTrace();
    }

    artifacts.addAll(out.getArtifacts());
  }

  private void emitPermutationDescriptions(ArtifactSet artifacts) {
    for (CompilationResult res : artifacts.find(CompilationResult.class)) {
      int permId = res.getPermutationId();
      List<String> permDesc = new ArrayList<String>();
      for (Map<SelectionProperty, String> propertyMap : res.getPropertyMap()) {
        permDesc.add(SymbolMapsLinker.propertyMapToString(propertyMap));
      }

      artifacts.add(new PermDescriptionArtifact(permId, permDesc));
    }
  }

  private Map<String, List<String>> extractPermutationDescriptions(
      ArtifactSet artifacts) {
    Map<String, List<String>> permDescriptions = new TreeMap<String,
        List<String>>();
    for (PermDescriptionArtifact art : artifacts.find(
        PermDescriptionArtifact.class)) {
      permDescriptions.put(Integer.toString(art.getPermId()),
          art.getPermDesc());
    }
    return permDescriptions;
  }
}
