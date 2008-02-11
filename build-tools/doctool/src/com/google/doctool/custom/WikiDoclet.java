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

package com.google.doctool.custom;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

/**
 * A doclet for using producing wiki output listing the specified classes and
 * their methods and constructors.
 */
public class WikiDoclet {

  private static final String OPT_WKHEADER = "-wkhead";
  private static final String OPT_WKOUT = "-wkout";
  private static final String JAVADOC_URL = "http://java.sun.com/j2se/1.5.0/docs/api/";

  private static WikiDoclet sWikiDoclet;

  public static int optionLength(String option) {
    if (option.equals(OPT_WKOUT)) {
      return 2;
    }
    if (option.equals(OPT_WKHEADER)) {
      return 2;
    }

    return 0;
  }

  public static boolean start(RootDoc root) {
    getDoclet().process(root);
    return true;
  }

  public static boolean validOptions(String[][] options,
      DocErrorReporter reporter) {
    return getDoclet().analyzeOptions(options, reporter);
  }

  private static WikiDoclet getDoclet() {
    if (sWikiDoclet == null) {
      sWikiDoclet = new WikiDoclet();
    }
    return sWikiDoclet;
  }

  private String headerFile;

  private String outputFile;

  private boolean analyzeOptions(String[][] options, DocErrorReporter reporter) {
    for (int i = 0; i < options.length; i++) {
      if (options[i][0] == OPT_WKOUT) {
        outputFile = options[i][1];
      } else if (options[i][0] == OPT_WKHEADER) {
        headerFile = options[i][1];
      }
    }

    if (outputFile == null) {
      reporter.printError("You must specify an output path/filename with "
          + OPT_WKOUT);
      return false;
    }

    return true;
  }

  private String createMemberList(Collection<ExecutableMemberDoc> members) {
    StringBuffer buffer = new StringBuffer();
    Iterator<ExecutableMemberDoc> iter = members.iterator();
    while (iter.hasNext()) {
      ExecutableMemberDoc member = iter.next();
      buffer.append(member.name() + member.flatSignature());
      if (iter.hasNext()) {
        buffer.append(", ");
      }
    }
    return buffer.toString();
  }

  private void importHeader(OutputStreamWriter writer) throws IOException {
    FileReader fr = new FileReader(headerFile);

    char[] cbuf = new char[4096];
    int len;
    while ((len = fr.read(cbuf)) != -1) {
      writer.write(cbuf, 0, len);
    }

    fr.close();
  }

  private void process(RootDoc root) {
    try {
      File outFile = new File(outputFile);
      outFile.getParentFile().mkdirs();
      FileWriter fw = new FileWriter(outFile);
      PrintWriter pw = new PrintWriter(fw, true);

      if (headerFile != null) {
        importHeader(fw);
      }

      for (PackageDoc pack : root.specifiedPackages()) {
        pw.println("==Package " + pack.name() + "==\n");

        String packURL = JAVADOC_URL + pack.name().replace(".", "/") + "/";

        // Sort the classes alphabetically
        ClassDoc[] classes = pack.allClasses(true);
        Arrays.sort(classes, new Comparator<ClassDoc>() {
          public int compare(ClassDoc arg0, ClassDoc arg1) {
            return arg0.name().compareTo(arg1.name());
          }
        });

        for (ClassDoc cls : classes) {
          // Each class links to Sun's main JavaDoc (brackets indicate wiki
          // link)
          pw.format("[%s%s.html %s]\n", packURL, cls.name(), cls.name());

          // Print out all constructors and methods
          Collection<ExecutableMemberDoc> members = new ArrayList<ExecutableMemberDoc>();
          members.addAll(Arrays.asList(cls.constructors(true)));
          members.addAll(Arrays.asList(cls.methods(true)));

          pw.print("  " + createMemberList(members));

          pw.print("\n\n");
        }
      }
      pw.close();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
