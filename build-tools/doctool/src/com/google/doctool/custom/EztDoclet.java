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
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

/**
 * A doclet for using producing EZT output listing the specified classes and
 * their methods and constructors.
 */
public class EztDoclet {

  private static final String JAVADOC_URL = "http://java.sun.com/javase/6/docs/api/";

  private static EztDoclet EZT_DOCLET;

  public static boolean start(RootDoc root) {
    getDoclet().process(root);
    return true;
  }

  private static EztDoclet getDoclet() {
    if (EZT_DOCLET == null) {
      EZT_DOCLET = new EztDoclet();
    }
    return EZT_DOCLET;
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

  private void process(RootDoc root) {
    PrintWriter pw = new PrintWriter(System.out, true);

    pw.println("<ol class=\"toc\" id=\"pageToc\">");
    for (PackageDoc pack : root.specifiedPackages()) {
      pw.format("  <li><a href=\"#Package_%s\">%s</a></li>\n",
          pack.name().replace('.', '_'), pack.name());
    }
    pw.println("</ol>\n");

    for (PackageDoc pack : root.specifiedPackages()) {
      pw.format("<h1 id=\"Package_%s\">Package %s</h1>\n", pack.name().replace(
          '.', '_'), pack.name());
      pw.println("<dl>");

      String packURL = JAVADOC_URL + pack.name().replace(".", "/") + "/";

      // Sort the classes alphabetically
      ClassDoc[] classes = pack.allClasses(true);
      Arrays.sort(classes, new Comparator<ClassDoc>() {
        public int compare(ClassDoc arg0, ClassDoc arg1) {
          return arg0.name().compareTo(arg1.name());
        }
      });

      Iterator<ClassDoc> iter = Arrays.asList(classes).iterator();
      while (iter.hasNext()) {
        ClassDoc cls = iter.next();

        // Each class links to Sun's main JavaDoc
        pw.format("  <dt><a href=\"%s%s.html\">%s</a></dt>\n", packURL,
            cls.name(), cls.name());

        // Print out all constructors and methods
        Collection<ExecutableMemberDoc> members = new ArrayList<ExecutableMemberDoc>();
        members.addAll(Arrays.asList(cls.constructors(true)));
        members.addAll(Arrays.asList(cls.methods(true)));

        if (!members.isEmpty()) {
          pw.format("  <dd>%s</dd>\n", createMemberList(members));
        }

        if (iter.hasNext()) {
          pw.print("\n");
        }
      }

      pw.println("</dl>\n");
    }
    pw.close();
  }
}
