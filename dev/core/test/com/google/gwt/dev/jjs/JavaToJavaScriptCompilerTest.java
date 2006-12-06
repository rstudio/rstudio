// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.jdt.RebindOracle;
import com.google.gwt.dev.jdt.RebindPermutationOracle;
import com.google.gwt.dev.jdt.WebModeCompilerFrontEnd;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.util.HashMap;
import java.util.Map;

public class JavaToJavaScriptCompilerTest {

  public static void main(String[] args) throws UnableToCompleteException {
    if (args.length < 1) {
      System.err.println("Usage: root-class [[rebind] ...]\n"
        + "       mainClass must implement public void appMain()\n"
        + "       rebind is of the form com.before.class:com.after.class\n");
      System.exit(1);
    }

    Map rebinds = new HashMap();

    for (int i = 1; i < args.length; ++i) {
      String[] split = args[i].split(":");
      if (split.length != 2) {
        System.err
          .println("Rebinds must be of the form com.before.class:com.after.class\n");
        System.exit(1);
      }
      rebinds.put(split[0], split[1]);
    }

    AbstractTreeLogger logger = new PrintWriterTreeLogger();
    logger.setMaxDetail(TreeLogger.INFO);
    SourceOracleImpl soi = new SourceOracleImpl();
    WebModeCompilerFrontEnd compiler = new WebModeCompilerFrontEnd(soi, new RebindPermutationOracle() {
      public String[] getAllPossibleRebindAnswers(TreeLogger logger, String sourceTypeName) throws UnableToCompleteException {
        return new String[] { sourceTypeName };
      }
    });

    JavaToJavaScriptCompiler jjs = new JavaToJavaScriptCompiler(logger,
      compiler, new String[]{args[0]});
    
    
    jjs.compile(logger, new RebindOracle() {
      public String rebind(TreeLogger logger, String sourceTypeName) throws UnableToCompleteException {
        return sourceTypeName;
      }
    });
    System.exit(0);
  }
}
