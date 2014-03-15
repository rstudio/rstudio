/*
 * Copyright 2010 Google Inc.
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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.EmittedArtifact;

import junit.framework.TestCase;

import java.io.InputStream;

/**
 * Tests {@link SoycReportLinker}.
 */
public class SoycReportLinkerTest extends TestCase {
  private static class NullInputStream extends InputStream {
    @Override
    public int read() {
      return -1;
    }
  }

  /**
   * Create a mock emitted artifact with the given path.
   */
  private static EmittedArtifact emitted(String path) {
    return new EmittedArtifact(SoycReportLinker.class, path) {
      @Override
      public InputStream getContents(TreeLogger logger) {
        return new NullInputStream();
      }
    };
  }

  public void testAnyReportFilesPresent() {
    SoycReportLinker soyc = new SoycReportLinker();

    // empty artifact set
    {
      ArtifactSet artifacts = new ArtifactSet();
      assertFalse(soyc.anyReportFilesPresent(artifacts));
    }

    // report files are in /soycReport/compile-report
    {
      ArtifactSet artifacts = new ArtifactSet();
      artifacts.add(emitted("soycReport/compile-report/SoycDashboard-0-index.html"));
      assertTrue(soyc.anyReportFilesPresent(artifacts));
    }

    // report files are in /compile-report
    {
      ArtifactSet artifacts = new ArtifactSet();
      artifacts.add(emitted("compile-report/SoycDashboard-0-index.html"));
      assertTrue(soyc.anyReportFilesPresent(artifacts));
    }

    // no report files, but other files are present
    {
      ArtifactSet artifacts = new ArtifactSet();
      artifacts.add(emitted("blahblah"));
      artifacts.add(emitted("blue/blue/blue"));
      assertFalse(soyc.anyReportFilesPresent(artifacts));
    }
  }
}
