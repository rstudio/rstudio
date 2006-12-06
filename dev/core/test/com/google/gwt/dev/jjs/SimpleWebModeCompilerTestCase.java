// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jdt.WebModeCompilerFrontEnd;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public abstract class SimpleWebModeCompilerTestCase extends TestCase {

  protected void compile(String appClass, Map rebinds)
      throws UnableToCompleteException {
    rebinds = rebinds != null ? rebinds : new HashMap();
    AbstractTreeLogger logger = new PrintWriterTreeLogger();
    logger.setMaxDetail(TreeLogger.INFO);
    SourceOracleImpl soi = new SourceOracleImpl();
    RebindPermOracleImpl rpoi = new RebindPermOracleImpl(rebinds);
    WebModeCompilerFrontEnd astCompiler = new WebModeCompilerFrontEnd(soi, rpoi);
    JavaToJavaScriptCompiler jjs = new JavaToJavaScriptCompiler(logger,
      astCompiler, new String[]{appClass}, false, false);
    String result = jjs.compile(logger, rpoi);

    // print source
    if (true) {
      System.out.println("<html><head></head>");
      System.out.println("<body onload=\'init()\'>");
      System.out.println("<script>");
      System.out.println(result);
      System.out.println("</script>");
      System.out.println("</body></html>");
    }

  }

}
