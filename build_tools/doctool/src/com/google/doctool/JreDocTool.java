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

package com.google.doctool;

import com.google.doctool.custom.EztDoclet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Uses the Javadoc tool to produce wiki output documenting the JRE emulation
 * classes in GWT.
 */
public class JreDocTool {

  public static void main(String[] args) {
    JreDocToolFactory factory = new JreDocToolFactory();
    String arg;

    for (int i = 0; i < args.length; i++) {
      if (tryParseFlag(args, i, "-help")) {
        printHelp();
        return;
      } else if (null != (arg = tryParseArg(args, i, "-classpath"))) {
        i++;
        factory.setClasspath(arg);
      } else if (null != (arg = tryParseArg(args, i, "-out"))) {
        i++;
        factory.setOutputFile(arg);
      } else if (null != (arg = tryParseArg(args, i, "-packages"))) {
        i++;
        factory.setPackages(arg);
      } else if (null != (arg = tryParseArg(args, i, "-sourcepath"))) {
        i++;
        factory.setSourcepath(arg);
      }
    }

    JreDocTool docTool = factory.create(System.err);
    if (docTool != null) {
      docTool.process();
    }
  }

  /**
   * Prints help for using JreDocTool.
   */
  private static void printHelp() {
    String s = "";
    s += "JreDocTool\n";
    s += "    Creates EZT format member listing from Java source";
    s += "    for emulated JRE classes.\n";
    s += "\n";
    s += "Required arguments:\n";
    s += "  -classpath\n";
    s += "    The path to find imported classes for this doc set.\n";
    s += "  -sourcepath\n";
    s += "    The path to find Java source for this doc set.\n";
    s += "    E.g. /gwt/src/trunk/user/super/com/google/gwt/emul\n";
    s += "  -out\n";
    s += "    The path and filename of the output file\n";
    s += "  -packages\n";
    s += "    A semicolon-separated list of fully-qualified package names.\n";
    s += "    E.g. java.lang;java.lang.annotation;java.util;java.io;java.sql\n";
    s += "\n";
    System.out.println(s);
  }

  /**
   * Parse a flag with a argument.
   */
  private static String tryParseArg(String[] args, int i, String name) {
    if (i < args.length) {
      if (args[i].equals(name)) {
        if (i + 1 < args.length) {
          String arg = args[i + 1];
          if (arg.startsWith("-")) {
            System.out.println("Warning: arg to " + name
                + " looks more like a flag: " + arg);
          }
          return arg;
        } else {
          throw new IllegalArgumentException("Expecting an argument after "
              + name);
        }
      }
    }
    return null;
  }

  /**
   * Parse just a flag with no subsequent argument.
   */
  private static boolean tryParseFlag(String[] args, int i, String name) {
    if (i < args.length) {
      if (args[i].equals(name)) {
        return true;
      }
    }
    return false;
  }

  private String classpath;

  private String outputFile;

  private String packages;

  private String sourcepath;

  JreDocTool(String classpath, String outputFile, String packages,
      String sourcepath) {
    this.classpath = classpath;
    this.outputFile = outputFile;
    this.packages = packages;
    this.sourcepath = sourcepath;
  }

  private void process() {
    List<String> args = new ArrayList<String>();

    args.add("-public");

    args.add("-quiet");

    args.add("-source");
    args.add("1.5");

    args.add("-doclet");
    args.add(EztDoclet.class.getName());

    args.add("-classpath");
    args.add(this.classpath);

    args.add("-sourcepath");
    args.add(this.sourcepath);

    args.add(EztDoclet.OPT_EZTFILE);
    args.add(this.outputFile);

    args.addAll(Arrays.asList(this.packages.split(";")));

    com.sun.tools.javadoc.Main.execute(args.toArray(new String[0]));
  }
}
