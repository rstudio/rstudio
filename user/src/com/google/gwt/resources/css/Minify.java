/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.resources.css;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.resources.css.ast.CssStylesheet;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ToolBase;

import java.io.File;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This is a command-line utility to minify a GWT CSS stylesheet.
 */
public class Minify extends ToolBase {

  /**
   * See {@link #printHelp()} for usage.
   */
  public static void main(String[] args) {
    Minify m = new Minify();
    if (m.exec(args)) {
      System.exit(0);
    } else {
      System.exit(-1);
    }
  }

  private final PrintWriterTreeLogger logger = new PrintWriterTreeLogger(
      new PrintWriter(System.err));
  private URL source;
  private boolean pretty;

  private Minify() {
    logger.setMaxDetail(TreeLogger.DEBUG);
  }

  @Override
  protected String getDescription() {
    return "Minify a CSS file";
  }

  private boolean exec(String[] args) {
    registerHandler(new ArgHandlerExtra() {

      @Override
      public boolean addExtraArg(String arg) {
        if (source != null) {
          return false;
        }

        try {
          source = (new File(arg)).toURL();
          return true;
        } catch (MalformedURLException e) {
          e.printStackTrace();
          return false;
        }
      }

      @Override
      public String getPurpose() {
        return "The css file to process";
      }

      @Override
      public String[] getTagArgs() {
        return new String[] {"stylesheet.css"};
      }
    });

    registerHandler(new ArgHandlerFlag() {
      @Override
      public String getPurpose() {
        return "Enable human-parsable output";
      }

      @Override
      public String getTag() {
        return "-pretty";
      }

      @Override
      public boolean setFlag() {
        return pretty = true;
      }
    });

    if (!processArgs(args)) {
      return false;
    }

    if (source == null) {
      printHelp();
      return false;
    }

    try {
      CssStylesheet sheet = GenerateCssAst.exec(logger, source);
      TextOutput out = new DefaultTextOutput(!pretty);
      (new CssGenerationVisitor(out)).accept(sheet);
      System.out.println(out.toString());
    } catch (UnableToCompleteException e) {
      logger.log(TreeLogger.ERROR, "Unable to compile CSS");
    }

    return true;
  }
}
