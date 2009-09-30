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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.CompilationResult;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.SelectionProperty;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.impl.StandardCompilationAnalysis;
import com.google.gwt.soyc.SoycDashboard;
import com.google.gwt.soyc.io.ArtifactsOutputDirectory;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Converts SOYC report files into emitted private artifacts.
 */
@LinkerOrder(Order.POST)
public class SoycReportLinker extends Linker {

  @Override
  public String getDescription() {
    return "Emit compile report artifacts";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {
    ArtifactSet results = new ArtifactSet(artifacts);
    boolean foundReports = false;

    for (StandardCompilationAnalysis soycFiles : artifacts.find(StandardCompilationAnalysis.class)) {
      if (soycFiles.getDepFile() != null) {
        results.add(soycFiles.getDepFile());
      }
      if (soycFiles.getSizeMapsFile() != null) {
        results.add(soycFiles.getSizeMapsFile());
      }
      if (soycFiles.getDetailedStoriesFile() != null) {
        results.add(soycFiles.getDetailedStoriesFile());
      }
      results.add(soycFiles.getSplitPointsFile());
      if (!soycFiles.getReportFiles().isEmpty()) {
        results.addAll(soycFiles.getReportFiles());
        foundReports = true;
      }
    }

    if (foundReports) {
      // run the final step of the dashboard to generate top-level files
      ArtifactsOutputDirectory out = new ArtifactsOutputDirectory();
      try {
        new SoycDashboard(out).generateCrossPermutationFiles(extractPermutationDescriptions(artifacts));
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR,
            "Error while generating a Story of Your Compile", e);
        e.printStackTrace();
      }
      results.addAll(out.getArtifacts());
    }
    return results;
  }

  private Map<String, String> extractPermutationDescriptions(
      ArtifactSet artifacts) {
    Map<String, String> permDescriptions = new TreeMap<String, String>();

    for (CompilationResult res : artifacts.find(CompilationResult.class)) {
      String permId = Integer.toString(res.getPermutationId());
      /*
       * TODO(kprobst,spoon) support permutations that collapsed to the same
       * compilation result
       */
      Map<SelectionProperty, String> propertyMap = res.getPropertyMap().iterator().next();
      String permDesc = SymbolMapsLinker.propertyMapToString(propertyMap);
      permDescriptions.put(permId, permDesc);
    }

    return permDescriptions;
  }
}
