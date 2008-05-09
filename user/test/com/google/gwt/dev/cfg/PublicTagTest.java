/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.io.PrintWriter;

/**
 * Tests various permutations of the GWT module's &amp;public&amp; tag,
 * specifically its ant-like inclusion support.
 */
public class PublicTagTest extends TestCase {

  private static TreeLogger getRootLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
        System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  private final ModuleDef moduleDef;

  public PublicTagTest() throws UnableToCompleteException {
    // Module has the same name as this class.
    String moduleName = getClass().getCanonicalName();
    moduleDef = ModuleDefLoader.loadFromClassPath(getRootLogger(), moduleName);
  }

  public void testPublicTag() {
    assertNotNull(moduleDef.findPublicFile("good0.html"));
    assertNotNull(moduleDef.findPublicFile("good1.html"));
    assertNotNull(moduleDef.findPublicFile("bar/good.html"));
    assertNotNull(moduleDef.findPublicFile("good2.html"));
    assertNotNull(moduleDef.findPublicFile("good3.html"));
    assertNotNull(moduleDef.findPublicFile("good4.html"));
    assertNotNull(moduleDef.findPublicFile("good5.html"));
    assertNotNull(moduleDef.findPublicFile("good6.html"));
    assertNotNull(moduleDef.findPublicFile("good7.html"));
    assertNotNull(moduleDef.findPublicFile("good8.html"));
    assertNotNull(moduleDef.findPublicFile("good10.html"));
    assertNotNull(moduleDef.findPublicFile("good11.html"));
    assertNotNull(moduleDef.findPublicFile("good9.html"));
    assertNotNull(moduleDef.findPublicFile("bar/CVS/good.html"));
    assertNotNull(moduleDef.findPublicFile("CVS/good.html"));
    assertNotNull(moduleDef.findPublicFile("GOOD/bar/GOOD/good.html"));
    assertNotNull(moduleDef.findPublicFile("GOOD/good.html"));

    assertNull(moduleDef.findPublicFile("bad.Html"));
    assertNull(moduleDef.findPublicFile("bar/CVS/bad.html"));
    assertNull(moduleDef.findPublicFile("CVS/bad.html"));
    assertNull(moduleDef.findPublicFile("bad1.html"));
    assertNull(moduleDef.findPublicFile("bad2.html"));
    assertNull(moduleDef.findPublicFile("bad3.html"));
    assertNull(moduleDef.findPublicFile("bad.html"));
    assertNull(moduleDef.findPublicFile("bar/bad.html"));
    assertNull(moduleDef.findPublicFile("GOOD/bar/bad.html"));
    assertNull(moduleDef.findPublicFile("GOOD/bar/GOOD/bar/bad.html"));
  }

}
