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
import com.google.gwt.core.ext.linker.EmittedArtifact;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.impl.StandardCompilationAnalysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Converts SOYC report files into emitted private artifacts.
 */
@LinkerOrder(Order.POST)
public class SoycReportLinker extends Linker {

  private static class SoycArtifact extends EmittedArtifact {
    private final File file;

    public SoycArtifact(String partialPath, File file) {
      super(SoycReportLinker.class, partialPath);
      this.file = file;
    }

    @Override
    public InputStream getContents(TreeLogger logger)
        throws UnableToCompleteException {
      try {
        return new FileInputStream(file);
      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Unable to open file", e);
        throw new UnableToCompleteException();
      }
    }

    @Override
    public long getLastModified() {
      return file.lastModified();
    }

    @Override
    public boolean isPrivate() {
      return true;
    }
  }

  @Override
  public String getDescription() {
    return "Emit SOYC artifacts";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {
    ArtifactSet results = new ArtifactSet(artifacts);
    for (StandardCompilationAnalysis soycFiles : artifacts.find(StandardCompilationAnalysis.class)) {
      File depFile = soycFiles.getDepFile();
      results.add(new SoycArtifact(depFile.getName(), depFile));

      File storiesFile = soycFiles.getStoriesFile();
      results.add(new SoycArtifact(storiesFile.getName(), storiesFile));

      File splitPointsFile = soycFiles.getSplitPointsFile();
      results.add(new SoycArtifact(splitPointsFile.getName(), splitPointsFile));
    }
    return results;
  }

}
