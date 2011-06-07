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
package com.google.gwt.dev;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.Compiler.CompilerOptionsImpl;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.util.tools.Utility;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

/**
 * Test for {@link Compiler} with option -soyc.
 */
public class SoycTest extends TestCase {

  private final CompilerOptionsImpl options = new CompilerOptionsImpl();

  public void testSoyc() throws UnableToCompleteException, IOException {
    File work = Utility.makeTemporaryDirectory(null, "hellowork");
    try {
      options.setSoycEnabled(true);
      options.addModuleName("com.google.gwt.sample.hello.Hello");
      options.setWarDir(new File(work, "war"));
      options.setExtraDir(new File(work, "extra"));
      PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(TreeLogger.ERROR);
      new Compiler(options).run(logger);
  
      // make sure the files have been produced
      assertTrue(new File(options.getExtraDir() + "/hello/soycReport/compile-report/index.html").exists());
      assertTrue(new File(options.getExtraDir() + "/hello/soycReport/compile-report/SoycDashboard-1-index.html").exists());
      assertTrue(new File(options.getExtraDir() + "/hello/soycReport/compile-report/total-1-overallBreakdown.html").exists());
      assertTrue(new File(options.getExtraDir() + "/hello/soycReport/compile-report/soyc.css").exists());
  
      assertFalse(new File(options.getExtraDir() + "/hello/soycReport/compile-report/index2.html").exists());
    } finally {
      Util.recursiveDelete(work, false);
    }
  }
}
