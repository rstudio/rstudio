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

package com.google.gwt.soyc;

import com.google.gwt.core.ext.linker.CompilationMetricsArtifact;
import com.google.gwt.core.ext.linker.ModuleMetricsArtifact;
import com.google.gwt.core.ext.linker.PrecompilationMetricsArtifact;
import com.google.gwt.core.ext.soyc.impl.SizeMapRecorder;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.collect.Sets;
import com.google.gwt.soyc.io.OutputDirectory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility to make all the HTML files for one permutation.
 */
public class MakeTopLevelHtmlForPerm {
  /**
   * A dependency linker for the initial code download. It links to the
   * dependencies for the initial download.
   */
  public class DependencyLinkerForInitialCode implements DependencyLinker {
    @Override
    public String dependencyLinkForClass(String className) {
      String packageName = globalInformation.getClassToPackage().get(className);
      assert packageName != null;
      return dependenciesFileName("initial") + "#" + className;
    }
  }

  /**
   * A dependency linker for the leftovers fragment. It links to leftovers
   * status pages.
   */
  public class DependencyLinkerForLeftoversFragment implements DependencyLinker {
    @Override
    public String dependencyLinkForClass(String className) {
      return leftoversStatusFileName() + "#"
          + hashedFilenameFragment(className);
    }
  }

  /**
   * A dependency linker for the total program breakdown. It links to a split
   * status page.
   *
   */
  public class DependencyLinkerForTotalBreakdown implements DependencyLinker {
    @Override
    public String dependencyLinkForClass(String className) {
      return splitStatusFileName() + "#" + hashedFilenameFragment(className);
    }
  }

  /**
   * A dependency linker that never links to anything.
   */
  public static class NullDependencyLinker implements DependencyLinker {
    @Override
    public String dependencyLinkForClass(String className) {
      return null;
    }
  }

  interface DependencyLinker {
    String dependencyLinkForClass(String className);
  }

  /**
   * Use this class to intern strings to save space in the generated HTML. After
   * populating the map, call getJs to create a JS array of all possible methods
   * in this report.
   */
  @SuppressWarnings("serial")
  private class HtmlInterner {
    // Hashes the interned string to the number of times this string is referenced.
    Map<String, Integer> builder = new HashMap<String, Integer>();
    // Hashes the interned string to its position in the final array of interned strings.
    // Populated after the call to {@link #freeze()}
    Map<String, Integer> frozen = null;

    /**
     * Call this method after all calls to {@link #intern(String)} are complete.
     * This routine then re-orders the interned calls in order of the number of
     * times each string was referenced by intern() so that lower numbered index
     * values represent more frequently referenced strings.
     */
    public void freeze() {
      final int maxDigits = 9;
      assert (frozen == null);
      assert (builder.size() < Math.pow(10, maxDigits));

      // order the interned values with the most referenced first.
      String[] temp = new String[builder.size()];
      int index = 0;
      for (String key : builder.keySet()) {
        temp[index++] = key.format("%0" + maxDigits + "d%s", builder.get(key), key);
      }
      builder = null;
      Arrays.sort(temp);

      // strip off the numeric prefix on the key to build the frozen hash table
      index = 0;
      frozen = new LinkedHashMap<String, Integer>();
      for (int i = temp.length - 1; i >= 0; i--) {
        frozen.put(temp[i].substring(maxDigits), index++);
      }
    }

    /**
     * Stores a string for later interning. Keeps track of the number of times a
     * particular string is interned which will be used by {@link #freeze()} to
     * place the most frequently used strings at the beginning of the
     * dictionary. After a call to {@link #freeze()}, it is no longer valid to
     * call this method.
     *
     * @param key string to be added to the intern dictionary.
     */
    public void intern(String key) {
      if (builder == null) {
        throw new RuntimeException("freeze() already called.");
      }
      if (!builder.containsKey(key)) {
        builder.put(key, 1);
      } else {
        int value = builder.get(key) + 1;
        builder.put(key, value);
      }
    }

    /**
     * Displays a link for a split point that contains this code.
     */
    public void printHasCodeInSplitPoint(PrintWriter outFile, String className,
        int sp) {
      outFile.print("h(" + frozen.get(getPackageSubstring(className)) + ","
          + frozen.get(getClassSubstring(className)) + "," + sp + ");");
    }

    /**
     * Non specific message that there is code in an initial fragment.
     */
    public void printHasInitialFragment(PrintWriter outFile) {
      outFile.print("f();");
    }

    /**
     * Displays a link for code in an initial fragment.
     */
    public void printHasInitialFragment(PrintWriter outFile, String className) {
      String packageName = getPackageSubstring(className);
      outFile.print("g(" + frozen.get(packageName) + ","
          + frozen.get(getClassSubstring(className)) + ","
          + frozen.get(hashedFilenameFragment(packageName)) + ");");
    }

    public void printSomeCodeLeftover(PrintWriter outFile) {
      outFile.print("i();");
    }

    /**
     * Prints an h3 element with the class name and an anchor.
     */
    private void printClassHeader(PrintWriter outFile, String className) {
      outFile.print("e(" + frozen.get(getPackageSubstring(className)) + ","
          + frozen.get(getClassSubstring(className)) + ",'"
          + hashedFilenameFragment(className) + "');");
    }

    /**
     * Print out a single class dependency stack in the methodDependencies
     * report.
     */
    private void printDependency(PrintWriter outFile,
        Map<String, String> dependencies, String method, String depMethod) {
      String nameArray = "[" + frozen.get(getPackageSubstring(method)) + ","
          + frozen.get(getClassSubstring(method)) + ","
          + frozen.get(getMethodSubstring(method)) + "]";
      outFile.print("b(" + nameArray + ",");
      outFile.print("[");
      while (depMethod != null) {
        String nextDep = dependencies.get(depMethod);
        // The bottom of the stack frame is not interesting.
        if (nextDep != null) {
          String packageString = getPackageSubstring(depMethod);
          String classString = getClassSubstring(depMethod);
          String methodString = getMethodSubstring(depMethod);
          outFile.print("[" + frozen.get(packageString) + ","
              + frozen.get(classString) + "," + frozen.get(methodString) + "]");
        }
        depMethod = nextDep;
        if (nextDep != null && dependencies.get(nextDep) != null) {
          outFile.print(",");
        }
      }
      outFile.print("]);");
    }

    /**
     * Prints out a class header for the methodDependendies report.
     */
    private void printDependencyClassHeader(PrintWriter outFile,
        String className) {
      outFile.print("a(" + frozen.get(getPackageSubstring(className)) + ","
          + frozen.get(getClassSubstring(className)) + ");");
    }

    /**
     * Prints a JavaScript snippet that includes the dictionary of interned
     * strings and methods to use interned strings to create lines in the
     * report.
     *
     * Call this method after invoking {@link #freeze()}.
     *
     * @param outFile open file to write the data to.
     */
    private void printInternedDataAsJs(PrintWriter outFile) {
      if (frozen == null) {
        throw new RuntimeException("freeze() not called.");
      }
      outFile.println("  var internedStrings = [");
      for (String key : frozen.keySet()) {
        outFile.print("\"" + key + "\",");
      }
      outFile.println("];");

      // array of split point descriptions
      outFile.println("  var spl = [");
      for (int sp = 1; sp <= globalInformation.getNumSplitPoints(); sp++) {
        outFile.println("        '"
            + globalInformation.getSplitPointToLocation().get(sp) + "',");
      }
      outFile.println("  ];");
      
      // object/dictionary containing method sizes
      outFile.println("  var methodSizes = {");
      for (SizeBreakdown breakdown : globalInformation.allSizeBreakdowns()) {
        for (Entry<String, Integer> methodEntry : breakdown.methodToSize.entrySet()) {
          String methodSignature = methodEntry.getKey();
          String method = methodSignature.substring(0, methodSignature.indexOf("("));
          outFile.println("    \"" + method + "\" : " + methodEntry.getValue() + ",");
        }
      }
      outFile.println("};");
      
      // dropdown button image srcs
      outFile.println("  var images = {");
      outFile.println("    \"closed\" : \"images/play-g16.png\",");
      outFile.println("    \"open\" : \"images/play-g16-down.png\",");
      outFile.println("  };");

      // TODO(zundel): Most of this below is just inserting a fixed string into the code. It would
      // be easier to read and maintain if we could store the fixed part in a flat file. Use some
      // kind of HTML template?

      // function to print a class header in the methodDependencies report
      // see printDependencyClassHeader()
      outFile.println("  function a(packageRef, classRef) {");
      outFile.println("    var className = internedStrings[packageRef] + \".\" + "
          + "internedStrings[classRef];");
      outFile.println("    document.write(\"<div class='main'>\");");
      outFile.println("    document.write(\"<table class='soyc-table'>\");");
      outFile.println("    document.write(\"<thead>\");");
      outFile.println("    document.write(\"<th><a class='soyc-class-name' "
          + "name='\" + className + \"'>"
          + "Class: \" + className + \"</a></th>\");");
      outFile.println("    document.write(\"<th class='soyc-numerical-col-header'>Size "
          + "<span class='soyc-th-units'>(bytes)</span></th>\");");
      outFile.println("    document.write(\"</thead>\");");
      outFile.println("  }");
      
      outFile.println("  function swapShowHide(elementName) {");
      outFile.println("    hp = document.getElementById(elementName);");
      outFile.println("    arrow = document.getElementById(\"dropdown-\" + elementName);");
      outFile.println("    if (hp.style.display !== \"none\" && hp.style.display "
          + "!== \"inline\") {");
      outFile.println("      hp.style.display = \"inline\";");
      outFile.println("      arrow.src = images[\"open\"];");
      outFile.println("    } else if (hp.style.display === \"none\") {");
      outFile.println("      hp.style.display = \"inline\";");
      outFile.println("      arrow.src = images[\"open\"];");
      outFile.println("    } else {");
      outFile.println("      hp.style.display = \"none\";");
      outFile.println("      arrow.src = images[\"closed\"];");
      outFile.println("    }");
      outFile.println("  }");
      
      // function to print a single dependency in the methodDependencies report
      // see printDependency()
      outFile.println("  function b(c, deps) {");
      outFile.println("    var methodName = internedStrings[c[0]] + \".\" + internedStrings[c[1]] "
          + "+ \"::\" + internedStrings[c[2]];");
      outFile.println("    var methodSize = methodSizes[methodName];");
      outFile.println("    if (methodSize === undefined) methodSize = \"--\";");
      outFile.println("    var callstackId = \"callstack-\" + methodName;");
      outFile.println("    document.write(\"<tr>\");");
      outFile.println("    document.write(\"<td>\");");
      outFile.println("    document.write(\"<img onclick='swapShowHide(\\\"\" + "
          + "callstackId + \"\\\")'"
          + "id='dropdown-\" + callstackId + \"' "
          + "class='dropdown-img' "
          + "src=\" + images[\"closed\"] + \">\");");
      outFile.println("    document.write(\"<a class='toggle soyc-call-stack-link' "
          + "onclick='swapShowHide(\\\"\" + callstackId + \"\\\")'>\" + methodName + \"</a>\");");
      outFile.println("    document.write(\"<ul id='\" + callstackId + \"' "
          + "class='soyc-call-stack-list'>\");");
      outFile.println("    for (var i = 0; i < deps.length ; i++) {");
      outFile.println("      var s = deps[i];");
      outFile.println("      document.write(\"<li>\" + internedStrings[s[0]] + \".\" + "
          + "internedStrings[s[1]] + \"::\" + internedStrings[s[2]] + \"</li>\");");
      outFile.println("    }");
      outFile.println("    document.write(\"</ul>\");");
      outFile.println("    document.write(\"</td>\");");
      outFile.println("    document.write(\"<td class='soyc-numerical-col'>\" + "
          + "methodSize + \"</td>\");");
      outFile.println("    document.write(\"</tr>\");");
      outFile.println("  }");
      
      // follows all method dependency stacks
      outFile.println("  function j() {");
      outFile.println("    document.write(\"</table></div>\");");
      outFile.println("  }");

      // leftovers status line
      outFile.println("  function c(packageRef,classRef,packageHashRef) {");
      outFile.println("    var packageName = internedStrings[packageRef];");
      outFile.println("    var className = packageName + \".\" + internedStrings[classRef];");
      outFile.println("    var d1 = 'methodDependencies-total-" + getPermutationId() + ".html';");
      outFile.println("    document.write(\"<ul class='soyc-excl'>\");");
      outFile.println("    document.write(\"<li><a href='\" + d1 + \"#\" + className + \"'>"
          + "See why it's live</a></li>\");");
      outFile.println("    for (var sp = 1; sp <= "
          + globalInformation.getNumSplitPoints() + "; sp++) {");
      outFile.println("      var d2 = 'methodDependencies-sp' + sp + '-" + getPermutationId() + ".html';");
      outFile.println("      document.write(\"<li><a href='\" + d2 + \"#\" + className +\"'>"
          + " See why it's not exclusive to s.p. #\" + sp + \" (\" + spl[sp - 1] + \")"
          + "</a></li>\");");
      outFile.println("    }");
      outFile.println("    document.write(\"</ul>\");");
      outFile.println("  }");

      // leftovers status package header line
      outFile.println("  function d(packageRef) {");
      outFile.println("    document.write(\"<div class='soyc-pkg-break'>Package: \" + "
          + "internedStrings[packageRef] + \"</div>\");");
      outFile.println("  }");

      // leftovers status class header line
      outFile.println("  function e(packageRef,classRef,classHashRef) {");
      outFile.println("    document.write(\"<a name='\" + classHashRef + \"'></a><h3>\" + "
          + "internedStrings[packageRef] + \".\" + internedStrings[classRef] + \"</h3>\");");
      outFile.println("  }");

      // split point has a class with code in the initial fragment - no link
      outFile.println("  function f() {");
      outFile.println("    document.write(\"<p>Some code is included in the initial fragment"
          + "</p>\");");
      outFile.println("  }");

      // split point has a class with code in the initial fragment
      outFile.println("  function g(packageRef, classRef, packageHashRef) {");
      outFile.println("    document.write(\"<p>Some code is included in the initial fragment "
          + "(<a href='methodDependencies-initial-\" + internedStrings[packageHashRef] + \"-"
          + getPermutationId()
          + ".html#\" + internedStrings[packageRef] + \".\" + "
          + "internedStrings[classRef] + \"'> See why</a>)</p>\");");
      outFile.println("  }");

      // split point has code from class
      outFile.println("  function h(packageRef, classRef, sp) {");
      outFile.println("    document.write(\"<p>Some code downloads with split point \" + sp + "
          + "\": \" + spl[sp - 1] + \"</p>\");");
      outFile.println("  }");

      // some code is left over
      outFile.println("  function i() {");
      outFile.println("    document.write(\"<p>Some code is left over:</p>\");");
      outFile.println("  }");
    }

    /**
     * Prints links to each split point showing why a leftover fragment isn't
     * exclusive.
     */
    private void printLeftoversStatus(PrintWriter outFile, String packageName,
        String className) {
      outFile.println("c(" + frozen.get(packageName) + ","
          + frozen.get(getClassSubstring(className)) + ","
          + frozen.get(hashedFilenameFragment(packageName)) + ");");
    }

    /**
     * Prints a div containing the package name in a blue block.
     */
    private void printPackageHeader(PrintWriter outFile, String packageName) {
      outFile.print("d(" + frozen.get(packageName) + ");");
    }
  }

  /**
   * By a convention shared with the compiler, the initial download is fragment
   * number 0.
   */
  private static final int FRAGMENT_NUMBER_INITIAL_DOWNLOAD = 0;

  /**
   * Just within this file, the convention is that the total program is fragment
   * number -1.
   */
  private static final int FRAGMENT_NUMBER_TOTAL_PROGRAM = -1;

  /**
   * A pattern describing the name of dependency graphs for code fragments
   * corresponding to a specific split point. These can be either exclusive
   * fragments or fragments of code for split points in the initial load
   * sequence.
   */
  private static final Pattern PATTERN_SP_INT = Pattern.compile("sp([0-9]+)");

  public static void makeTopLevelHtmlForAllPerms(
      Map<String, List<String>> allPermsInfo, OutputDirectory outDir)
      throws IOException {
    PrintWriter outFile = new PrintWriter(outDir.getOutputStream("index.html"));
    addStandardHtmlProlog(outFile, "Compile report", "Compile report",
        "Overview of permutations");
    outFile.println("<ul>");

    // in order to print these in ascending order, we have to sort by
    // integers
    SortedSet<Integer> sortedPermIds = new TreeSet<Integer>();
    for (String permutationId : allPermsInfo.keySet()) {
      sortedPermIds.add(Integer.parseInt(permutationId));
    }

    for (Integer sortedPermId : sortedPermIds) {
      String permutationId = Integer.toString(sortedPermId);
      List<String> permutationInfoList = allPermsInfo.get(permutationId);
      outFile.print("<li>Permutation " + permutationId);

      for (String desc : permutationInfoList) {
        outFile.println("  (" + desc + ")");
      }
      outFile.println("<ul>");
      outFile.println("<li>");
      outFile.println("<a href=\"SoycDashboard-" + permutationId
          + "-index.html\">Split Point Report</a>");
      outFile.println("</li>");
      outFile.println("<li>");
      outFile.println("<a href=\"CompilerMetrics-" + permutationId
          + "-index.html\">Compiler Metrics</a>");
      outFile.println("</li>");

      outFile.println("</ul>");
      outFile.println("</li>");
    }
    outFile.println("</ul>");
    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  /**
   * @return given "com.foo.myClass" or "com.foo.myClass::myMethod" returns
   *         "myClass"
   */
  static String getClassSubstring(String fullMethodName) {
    if (fullMethodName.length() == 0) {
      return "";
    }
    int startIndex = getPackageSubstring(fullMethodName).length() + 1;
    int endIndex = fullMethodName.indexOf("::");
    if (endIndex == -1) {
      endIndex = fullMethodName.length();
    }
    if (startIndex > endIndex || startIndex > fullMethodName.length()) {
      return "";
    }
    return fullMethodName.substring(startIndex, endIndex);
  }

  /**
   * @return given "com.foo.myClass::myMethod" returns "myMethod"
   */
  static String getMethodSubstring(String fullMethodName) {
    int index = fullMethodName.indexOf("::");
    if (index == -1) {
      return "";
    }
    index += 2;
    if (index >= fullMethodName.length()) {
      return "";
    }
    return fullMethodName.substring(index);
  }

  /**
   * @return given "com.foo.myClass" or "com.foo.myClass::myMethod" returns
   *         "com.foo"
   */
  static String getPackageSubstring(String fullMethodName) {
    int endIndex = fullMethodName.lastIndexOf('.');
    if (endIndex == -1) {
      endIndex = fullMethodName.length();
    }
    return fullMethodName.substring(0, endIndex);
  }

  private static void addSmallHtmlProlog(final PrintWriter outFile, String title) {
    outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
    outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
    outFile.println("<html>");
    outFile.println("<head>");
    outFile.println("<title>");
    outFile.println(title);
    outFile.println("</title>");
    outFile.println("<style type=\"text/css\" media=\"screen\">");
    outFile.println("@import url('goog.css');");
    outFile.println("@import url('inlay.css');");
    outFile.println("@import url('soyc.css');");
    outFile.println("</style>");
    outFile.println("</head>");
  }

  private static void addStandardHtmlEnding(final PrintWriter out) {
    out.println("</div>");
    out.println("</body>");
    out.println("</html>");
  }

  private static void addStandardHtmlProlog(final PrintWriter outFile,
      String title, String header1, String header2) {
    outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
    outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
    outFile.println("<html>");
    outFile.println("<head>");
    outFile.println("<script type=\"text/javascript\">");
    outFile.println("function show(elementName)");
    outFile.println("{");
    outFile.println("hp = document.getElementById(elementName);");
    outFile.println("hp.style.visibility = \"Visible\";");
    outFile.println("}");
    outFile.println("function hide(elementName)");
    outFile.println("{");
    outFile.println("hp = document.getElementById(elementName);");
    outFile.println("hp.style.visibility = \"Hidden\";");
    outFile.println("}");
    outFile.println("</script>");
    outFile.println("<title>");
    outFile.println(title);
    outFile.println("</title>");
    outFile.println("<style type=\"text/css\" media=\"screen\">");
    outFile.println("@import url('goog.css');");
    outFile.println("@import url('inlay.css');");
    outFile.println("@import url('soyc.css');");
    outFile.println("</style>");
    outFile.println("</head>");
    outFile.println("<body>");
    outFile.println("<div class=\"g-doc\">");
    outFile.println("<div id=\"hd\" class=\"g-section g-tpl-50-50 g-split\">");
    outFile.println("<div class=\"g-unit g-first\">");
    outFile.println("<p>");
    outFile.println("<a href=\"index.html\" id=\"gwt-logo\" class=\"soyc-ir\">");
    outFile.println("<span>Google Web Toolkit</span>");
    outFile.println("</a>");
    outFile.println("</p>");
    outFile.println("</div>");
    outFile.println("<div class=\"g-unit\">");
    outFile.println("</div>");
    outFile.println("</div>");
    outFile.println("<div id=\"soyc-appbar-lrg\">");
    outFile.println("<div class=\"g-section g-tpl-75-25 g-split\">");
    outFile.println("<div class=\"g-unit g-first\">");
    outFile.println("<h1>" + header1 + "</h1>");
    outFile.println("</div>");
    outFile.println("<div class=\"g-unit\"></div>");
    outFile.println("</div>");
    outFile.println("</div>");
    outFile.println("<div id=\"bd\">");
    if (header2 != null && header2.length() > 0) {
      outFile.println("<h2>" + header2 + "</h2>");
    }
  }

  private static String classesInPackageFileName(SizeBreakdown breakdown,
      String permutationId) {
    return breakdown.getId() + "_" + permutationId + "_Classes.html";
  }

  private static String escapeXml(String unescaped) {
    return SizeMapRecorder.escapeXml(unescaped);
  }

  /**
   * Convert a potentially long string into a short file name. The current
   * implementation simply hashes the long name.
   */
  private static String hashedFilenameFragment(String longFileName) {
    try {
      return Util.computeStrongName(longFileName.getBytes(Util.DEFAULT_ENCODING));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private static String headerLineForBreakdown(SizeBreakdown breakdown) {
    return "(Analyzing code subset: " + breakdown.getDescription() + ")";
  }

  private static String shellFileName(SizeBreakdown breakdown,
      String permutationId) {
    return breakdown.getId() + "-" + permutationId + "-overallBreakdown.html";
  }

  /**
   * Global information for this permutation.
   */
  private final GlobalInformation globalInformation;

  private final OutputDirectory outDir;

  MakeTopLevelHtmlForPerm(GlobalInformation globalInformation,
      OutputDirectory outDir) {
    this.globalInformation = globalInformation;
    this.outDir = outDir;
  }

  public void makeBreakdownShell(SizeBreakdown breakdown) throws IOException {
    Map<String, CodeCollection> nameToCodeColl = breakdown.nameToCodeColl;
    Map<String, LiteralsCollection> nameToLitColl = breakdown.nameToLitColl;
    String packageBreakdownFileName = makePackageHtml(breakdown);
    String codeTypeBreakdownFileName = makeCodeTypeHtml(breakdown,
        nameToCodeColl, nameToLitColl);

    PrintWriter outFile = new PrintWriter(getOutFile(shellFileName(breakdown,
        getPermutationId())));

    addStandardHtmlProlog(outFile, "Application breakdown analysis",
        "Application breakdown analysis", "");

    String popupName = "packageBreakdownPopup";
    String popupTitle = "Package breakdown";
    String popupBody = "The package breakdown blames pieces of JavaScript "
        + "code on Java packages wherever possible.  Note that this is not possible for all "
        + "code, so the sizes of the packages here will not normally add up to the full code "
        + "size.  More specifically, the sum will exclude strings, whitespace, and a few pieces "
        + "of JavaScript code that are produced during compilation but cannot be attributed to "
        + "any Java package.";

    outFile.println("<h2>");
    addPopupLink(outFile, popupName, popupTitle, null);
    outFile.println("</h2></div>");
    addPopup(outFile, popupName, popupTitle, popupBody);
    outFile.println("<iframe class='soyc-iframe-package' src=\""
        + packageBreakdownFileName + "\" scrolling=auto></iframe>");

    popupName = "codeTypeBreakdownPopup";
    popupTitle = "Code Type Breakdown";
    popupBody = "The code type breakdown breaks down the JavaScript code according to its "
        + "type or function.  For example, it tells you how much of your code can be attributed to "
        + "JRE, GWT-RPC, etc.  As above, strings and some other JavaScript snippets are not "
        + "included in the breakdown.";
    outFile.println("<h2>");
    addPopupLink(outFile, popupName, popupTitle, null);
    outFile.println("</h2>");
    addPopup(outFile, popupName, popupTitle, popupBody);

    outFile.println("<iframe class='soyc-iframe-code' src=\""
        + codeTypeBreakdownFileName + "\" scrolling=auto></iframe>");
    outFile.println("</div>");

    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  public void makeCodeTypeClassesHtmls(SizeBreakdown breakdown)
      throws IOException {
    HashMap<String, CodeCollection> nameToCodeColl = breakdown.nameToCodeColl;

    for (String codeType : nameToCodeColl.keySet()) {

      // construct file name
      String outFileName = breakdown.getId() + "_" + codeType + "-"
          + getPermutationId() + "Classes.html";

      float sumSize = 0f;
      TreeMap<Integer, Set<String>> sortedClasses = new TreeMap<Integer, Set<String>>(
          Collections.reverseOrder());

      for (String className : nameToCodeColl.get(codeType).classes) {
        if (breakdown.classToSize.containsKey(className)) {
          int curSize = 0;
          if (breakdown.classToSize.containsKey(className)) {
            curSize = breakdown.classToSize.get(className);
          }
          if (curSize != 0) {
            if (sortedClasses.containsKey(curSize)) {
              Set<String> existingSet = sortedClasses.get(curSize);
              existingSet.add(className);
              sortedClasses.put(curSize, existingSet);
            } else {
              Set<String> newSet = new TreeSet<String>();
              newSet.add(className);
              sortedClasses.put(curSize, newSet);
            }
            sumSize += curSize;
          }
        }
      }

      final PrintWriter outFile = new PrintWriter(getOutFile(outFileName));
      addStandardHtmlProlog(outFile, "Classes in package " + codeType,
          "Classes in package " + codeType, headerLineForBreakdown(breakdown));
      outFile.println("<table class=\"soyc-table\">");
      outFile.println("<colgroup>");
      outFile.println("<col id=\"soyc-splitpoint-type-col\">");
      outFile.println("<col id=\"soyc-splitpoint-size-col\">");
      outFile.println("</colgroup>");
      outFile.println("<thead>");
      outFile.println("<th>Code type</th>");
      outFile.println("<th>");
      outFile.println("<th class=\"soyc-numerical-col-header\">");
      outFile.println("Size <span class=\"soyc-th-units\">(Bytes)</span>");
      outFile.println("</th>");
      outFile.println("<th class=\"soyc-numerical-col-header\">% of total</th>");
      outFile.println("</thead>");
      
      NumberFormat bytesFormatter = NumberFormat.getInstance();
      bytesFormatter.setGroupingUsed(true);
      
      NumberFormat percentFormatter = NumberFormat.getPercentInstance();
      percentFormatter.setMinimumFractionDigits(1);
      percentFormatter.setMaximumFractionDigits(1);

      for (Integer size : sortedClasses.keySet()) {
        Set<String> classNames = sortedClasses.get(size);
        for (String className : classNames) {
          float perc = (float) size / sumSize;
          outFile.println("<tr>");
          outFile.println("<td>" + className + "</a></td>");
          outFile.println("<td class=\"soyc-bargraph-col\">");
          outFile.println("<div class=\"soyc-bar-graph goog-inline-block\">");
          // CHECKSTYLE_OFF
          outFile.println("<div style=\"width:" + (perc * 100.0)
              + "%;\" class=\"soyc-bar-graph-fill goog-inline-block\"></div>");
          // CHECKSTYLE_ON
          outFile.println("</div>");
          outFile.println("</td>");
          outFile.println("<td class=\"soyc-numerical-col\">");
          outFile.println(bytesFormatter.format(size));
          outFile.println("</td>");
          outFile.println("<td class=\"soyc-percent-col\">" + 
              percentFormatter.format(perc) + "</td>");
          outFile.println("</tr>");
        }
      }
      addStandardHtmlEnding(outFile);
      outFile.close();
    }
  }

  public void makeCompilerMetricsPermFiles(ModuleMetricsArtifact moduleMetrics,
      PrecompilationMetricsArtifact precompilationMetrics,
      CompilationMetricsArtifact compilationMetrics) throws IOException {
    String outFileName = "CompilerMetrics-"
        + precompilationMetrics.getPermuationBase() + "-index.html";
    PrintWriter outFile = new PrintWriter(getOutFile(outFileName));
    String title = "Compiler Metrics for Permutation "
        + compilationMetrics.getPermuationId();
    addStandardHtmlProlog(outFile, title, title, "Build Time Metrics");
    
    NumberFormat elapsedFormatter = NumberFormat.getInstance();
    elapsedFormatter.setGroupingUsed(true);
    elapsedFormatter.setMinimumFractionDigits(3);
    elapsedFormatter.setMaximumFractionDigits(3);
    
    outFile.println("<div id=\"bd\">");
    int permutationId = compilationMetrics.getPermuationId();

    // Build Time Metrics
    outFile.println("<table class=\"soyc-table\">");
    outFile.println("<colgroup>");
    outFile.println("<col id=\"soyc-buildTimePhase-col\">");
    outFile.println("<col id=\"soyc-buildTimeElapsed-col\">");
    outFile.println("</colgroup>");
    outFile.println("<thead>");
    outFile.println("<th>Phase</th>");
    outFile.println("<th class=\"soyc-numerical-col-header\">Elapsed Time</th>");
    outFile.println("</thead>");

    outFile.println("<tr>");
    outFile.println("<td>");
    outFile.println("Module Analysis");
    outFile.println("</td>");
    outFile.println("<td class=\"soyc-numerical-col\">"  
        + elapsedFormatter.format(moduleMetrics.getElapsedMilliseconds() / 1000.0) + " s");
    outFile.println("</td>");
    outFile.println("</tr>");

    outFile.println("<tr>");
    outFile.println("<td>");
    outFile.println("Precompile (may include Module Analysis)");
    outFile.println("</td>");
    outFile.println("<td class=\"soyc-numerical-col\">" 
        + elapsedFormatter.format(precompilationMetrics.getElapsedMilliseconds() / 1000.0)
        + " s");
    outFile.println("</td>");
    outFile.println("</tr>");

    outFile.println("<tr>");
    outFile.println("<td>");
    outFile.println("Compile");
    outFile.println("</td>");
    outFile.println("<td class=\"soyc-numerical-col\">" 
        + elapsedFormatter.format(compilationMetrics.getElapsedMilliseconds() / 1000.0)
        + " s");
    outFile.println("</td>");
    outFile.println("</tr>");
    outFile.println("</table>");
    
    NumberFormat referencesFormatter = NumberFormat.getInstance();
    referencesFormatter.setGroupingUsed(true);

    outFile.println("<p></p>");
    outFile.println("<h2>Source/Type Metrics</h2>");

    outFile.println("<table class=\"soyc-table\">");
    outFile.println("<colgroup>");
    outFile.println("<col id=\"soyc-typeList-col\">");
    outFile.println("<col id=\"soyc-typeReferences-col\">");
    outFile.println("</colgroup>");
    outFile.println("<thead>");
    outFile.println("<th>Description</th>");
    outFile.println("<th class=\"soyc-numerical-col-header\">References</th>");
    outFile.println("</thead>");

    String sourcesFileName = "CompilerMetrics-sources.html";
    outFile.println("<tr>");
    outFile.println("<td>");
    String popupName = "compilerMetricsSourceFiles";
    String popupTitle = "Source files";
    String popupBody = "All source files on the module source path.";
    addPopupLink(outFile, popupName, popupTitle, sourcesFileName);
    addPopup(outFile, popupName, popupTitle, popupBody);
    outFile.println("</td>");
    outFile.println("<td class=\"soyc-numerical-col\">");
    outFile.println(referencesFormatter.format(moduleMetrics.getSourceFiles().length));
    outFile.println("</td>");
    outFile.println("</tr>");
    makeCompilerMetricsSources(sourcesFileName, moduleMetrics, popupBody);

    String initialTypesFileName = "CompilerMetrics-initialTypes-"
        + permutationId + ".html";
    outFile.println("<tr>");
    outFile.println("<td>");
    popupName = "compilerMetricsInitialTypes";
    popupTitle = "Initial Type Oracle Types";
    popupBody = "All types in the type oracle after compiling sources on the source path.";
    addPopupLink(outFile, popupName, popupTitle, initialTypesFileName);
    addPopup(outFile, popupName, popupTitle, popupBody);
    outFile.println("</td>");
    outFile.println("<td class=\"soyc-numerical-col\">");
    outFile.println(referencesFormatter.format(moduleMetrics.getInitialTypes().length));
    outFile.println("</td>");
    outFile.println("</tr>");
    makeCompilerMetricsInitialTypeOracleTypes(initialTypesFileName,
        moduleMetrics, popupBody);

    String finalTypesFileName = "CompilerMetrics-finalTypes-" + permutationId
        + ".html";
    outFile.println("<tr>");
    outFile.println("<td>");
    popupName = "compilerMetricsFinalTypes";
    popupTitle = "Final Type Oracle Types";
    popupBody = "All types in the type oracle after constructing the Java AST.";
    addPopupLink(outFile, popupName, popupTitle, finalTypesFileName);
    addPopup(outFile, popupName, popupTitle, popupBody);
    outFile.println("</td>");
    outFile.println("<td class=\"soyc-numerical-col\">");
    outFile.println(referencesFormatter.format(
        precompilationMetrics.getFinalTypeOracleTypes().length));
    outFile.println("</td>");
    outFile.println("</tr>");
    makeCompilerMetricsFinalTypeOracleTypes(finalTypesFileName,
        precompilationMetrics, popupBody);

    String[] generatedTypes = getGeneratedTypes(moduleMetrics,
        precompilationMetrics);
    String generatedTypesFileName = "CompilerMetrics-generatedTypes-"
        + permutationId + ".html";
    outFile.println("<tr>");
    outFile.println("<td>");
    popupName = "compilerMetricsGeneratedTypes";
    popupTitle = "GeneratedTypes";
    popupBody = "Types that were added to the type oracle while running generators.";
    addPopupLink(outFile, popupName, popupTitle, generatedTypesFileName);
    addPopup(outFile, popupName, popupTitle, popupBody);
    outFile.println("</td>");
    outFile.println("<td class=\"soyc-numerical-col\">");
    outFile.println(referencesFormatter.format(generatedTypes.length));
    outFile.println("</td>");
    outFile.println("</tr>");
    makeCompilerMetricsGeneratedTypes(generatedTypesFileName, generatedTypes,
        popupBody);

    String astFileName = "CompilerMetrics-ast-" + permutationId + ".html";
    outFile.println("<tr>");
    outFile.println("<td>");
    popupName = "compilerMetricsAstTypes";
    popupTitle = "AST Referenced Types";
    popupBody = "All types referenced by the Java AST after performing "
        + "reachability analysis from the module EntryPoint.";
    addPopupLink(outFile, popupName, popupTitle, astFileName);
    addPopup(outFile, popupName, popupTitle, popupBody);
    outFile.println("</td>");
    outFile.println("<td class=\"soyc-numerical-col\">");
    outFile.println(referencesFormatter.format(precompilationMetrics.getAstTypes().length));
    outFile.println("</td>");
    outFile.println("</tr>");
    makeCompilerMetricsAstTypes(astFileName, precompilationMetrics, popupBody);

    String[] unreferencedTypes = getUnreferencedTypes(precompilationMetrics);
    String unreferencedFileName = "CompilerMetrics-unreferencedTypes-"
        + permutationId + ".html";
    outFile.println("<tr>");
    outFile.println("<td>");
    popupName = "compilerMetricsUnreferenceTypes";
    popupTitle = "Unreferenced Types";
    popupBody = "Types that were on the initial source path but never referenced in "
        + "the Java AST.";
    addPopupLink(outFile, popupName, popupTitle, unreferencedFileName);
    addPopup(outFile, popupName, popupTitle, popupBody);
    outFile.println("</td>");
    outFile.println("<td class=\"soyc-numerical-col\">");
    outFile.println(referencesFormatter.format(unreferencedTypes.length));
    outFile.println("</td>");
    outFile.println("</tr>");
    makeCompilerMetricsUnreferencedTypes(unreferencedFileName,
        unreferencedTypes, popupBody);
    outFile.println("</table>");

    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  public void makeDependenciesHtml() throws IOException {
    for (String depGraphName : globalInformation.dependencies.keySet()) {
      makeDependenciesHtml(depGraphName,
          globalInformation.dependencies.get(depGraphName));
    }
  }

  public void makeLeftoverStatusPages() throws IOException {
    PrintWriter outFile = new PrintWriter(getOutFile(leftoversStatusFileName()));
    addStandardHtmlProlog(outFile, "Leftovers page", "Leftovers page", "");
    outFile.println("<div id=\"bd\">");
    outFile.println("<p>These classes have some leftover code, neither initial nor "
        + "exclusive to any split point:</p>");
    String curPackageName = "";
    HtmlInterner interner = new HtmlInterner();

    for (String className : globalInformation.getClassToPackage().keySet()) {
      String packageName = globalInformation.getClassToPackage().get(className);
      interner.intern(packageName);
      interner.intern(getClassSubstring(className));
      interner.intern(hashedFilenameFragment(packageName));
    }
    interner.freeze();

    outFile.println("<script language=\"javascript\">");
    interner.printInternedDataAsJs(outFile);

    for (String className : globalInformation.getClassToPackage().keySet()) {
      String packageName = globalInformation.getClassToPackage().get(className);
      if (packageName.compareTo("") == 0
          || packageName.compareTo(curPackageName) != 0) {
        curPackageName = packageName;
        interner.printPackageHeader(outFile, packageName);
      }
      interner.printClassHeader(outFile, className);
      if (globalInformation.dependencies != null) {
        interner.printLeftoversStatus(outFile, packageName, className);
      }
    }
    outFile.println("</script>");
    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  public void makeLiteralsClassesTableHtmls(SizeBreakdown breakdown)
      throws IOException {
    Map<String, LiteralsCollection> nameToLitColl = breakdown.nameToLitColl;
    for (String literalType : nameToLitColl.keySet()) {
      String outFileName = literalType + "-" + getPermutationId() + "Lits.html";
      final PrintWriter outFile = new PrintWriter(getOutFile(breakdown.getId()
          + "_" + outFileName));
      addStandardHtmlProlog(outFile, "Literals of type " + literalType,
          "Literals of type " + literalType, headerLineForBreakdown(breakdown));
      outFile.println("<table width=\"80%\" style=\"font-size: 11pt;\" bgcolor=\"white\">");
      for (String literal : nameToLitColl.get(literalType).literals) {
        if (literal.trim().length() == 0) {
          literal = "[whitespace only string]";
        }
        String escliteral = escapeXml(literal);
        outFile.println("<tr>");
        outFile.println("<td>" + escliteral + "</td>");
        outFile.println("</tr>");
      }
      outFile.println("</table>");
      outFile.println("<center>");
      addStandardHtmlEnding(outFile);
      outFile.close();
    }
  }

  /**
   * Make size breakdowns for each package for one code collection.
   */
  public void makePackageClassesHtmls(SizeBreakdown breakdown,
      DependencyLinker depLinker) throws IOException {
    PrintWriter outFile = new PrintWriter(getOutFile(classesInPackageFileName(
        breakdown, getPermutationId())));
    addStandardHtmlProlog(outFile, "Classes in  " + breakdown.getDescription(),
        "Classes in " + breakdown.getDescription(),
        headerLineForBreakdown(breakdown));

    String[] packageNames = globalInformation.getPackageToClasses().keySet().toArray(
        new String[0]);
    Arrays.sort(packageNames);
    for (String packageName : packageNames) {
      TreeMap<Integer, Set<String>> sortedClasses = new TreeMap<Integer, Set<String>>(
          Collections.reverseOrder());
      int sumSize = 0;
      for (String className : globalInformation.getPackageToClasses().get(
          packageName)) {
        int curSize = 0;
        if (!breakdown.classToSize.containsKey(className)) {
          // This class not present in this code collection
        } else {
          curSize = breakdown.classToSize.get(className);
        }
        if (curSize != 0f) {
          if (sortedClasses.containsKey(curSize)) {
            Set<String> existingSet = sortedClasses.get(curSize);
            existingSet.add(className);
            sortedClasses.put(curSize, existingSet);
          } else {
            Set<String> newSet = new TreeSet<String>();
            newSet.add(className);
            sortedClasses.put(curSize, newSet);
          }
          sumSize += curSize;
        }
      }

      if (sortedClasses.size() > 0) {
        outFile.println("<p>");
        outFile.println("<table class=\"soyc-table\">");
        outFile.print("<colgroup>");
        outFile.print("<col id=\"soyc-splitpoint-type-col\">");
        outFile.print("<col id=\"soyc-splitpoint-size-col\">");
        outFile.println("</colgroup>");
        outFile.print("<thead>");
        outFile.print("<a name=\"" + hashedFilenameFragment(packageName)
            + "\"></a><th>Package: " + packageName + "</th>");
        outFile.println("<th></th>");
        outFile.println("<th class=\"soyc-numerical-col-header\">");
        outFile.println("Size <span class=\"soyc-th-units\">(Bytes)</span>");
        outFile.println("</th>");
        outFile.println("<th class=\"soyc-numerical-col-header\">% of total</th>");
        outFile.print("</thead>");
        
        NumberFormat bytesFormatter = NumberFormat.getInstance();
        bytesFormatter.setGroupingUsed(true);
        
        NumberFormat percentFormatter = NumberFormat.getPercentInstance();
        percentFormatter.setMinimumFractionDigits(1);
        percentFormatter.setMaximumFractionDigits(1);

        for (Integer size : sortedClasses.keySet()) {
          Set<String> classNames = sortedClasses.get(size);
          for (String className : classNames) {
            String drillDownFileName = depLinker.dependencyLinkForClass(className);
            float perc = (float) size / (float) sumSize;
            outFile.println("<tr>");
            if (drillDownFileName == null) {
              outFile.println("<td>" + className + "</td>");
            } else {
              outFile.println("<td><a href=\"" + drillDownFileName
                  + "\" target=\"_top\">" + className + "</a></td>");
            }
            outFile.println("<td class=\"soyc-bargraph-col\">");
            outFile.println("<div class=\"soyc-bar-graph goog-inline-block\">");
            // CHECKSTYLE_OFF
            outFile.println("<div style=\"width:" + (perc * 100.0)
                + "%;\" class=\"soyc-bar-graph-fill goog-inline-block\"></div>");
            // CHECKSTYLE_ON
            outFile.println("</div>");
            outFile.println("</td>");
            outFile.println("<td class=\"soyc-numerical-col\">");
            outFile.println(bytesFormatter.format(size));
            outFile.println("</td>");
            outFile.println("<td class=\"soyc-percent-col\">" + 
                percentFormatter.format(perc) + "</td>");
            outFile.println("</tr>");
          }
        }
        outFile.println("</table>");
        outFile.println("</p>");
      }
    }
    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  public void makeSplitStatusPages() throws IOException {
    PrintWriter outFile = new PrintWriter(getOutFile(splitStatusFileName()));

    addStandardHtmlProlog(outFile, "Split point status", "Split point status",
        "");
    outFile.println("<div id=\"bd\">");

    HtmlInterner interner = new HtmlInterner();
    for (String className : globalInformation.getClassToPackage().keySet()) {
      String packageName = globalInformation.getClassToPackage().get(className);
      interner.intern(packageName);
      interner.intern(getClassSubstring(className));
      interner.intern(hashedFilenameFragment(packageName));
    }
    interner.freeze();

    outFile.println("<script language=\"javascript\">");
    interner.printInternedDataAsJs(outFile);

    String curPackageName = "";
    for (String className : globalInformation.getClassToPackage().keySet()) {

      String packageName = globalInformation.getClassToPackage().get(className);
      if (packageName.compareTo("") == 0
          || packageName.compareTo(curPackageName) != 0) {
        curPackageName = packageName;
        interner.printPackageHeader(outFile, packageName);
      }

      interner.printClassHeader(outFile, className);

      if (globalInformation.getInitialCodeBreakdown().classToSize.containsKey(className)) {
        if (globalInformation.dependencies != null) {
          interner.printHasInitialFragment(outFile, className);
        } else {
          interner.printHasInitialFragment(outFile);
        }
      }
      for (int sp : splitPointsWithClass(className)) {
        interner.printHasCodeInSplitPoint(outFile, className, sp);
      }
      if (globalInformation.getLeftoversBreakdown().classToSize.containsKey(className)) {
        interner.printSomeCodeLeftover(outFile);
        interner.printLeftoversStatus(outFile, packageName, className);
      }
    }
    outFile.println("</script>");

    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  public void makeTopLevelShell() throws IOException {

    String permutationId = getPermutationId();
    PrintWriter outFile = new PrintWriter(getOutFile("SoycDashboard" + "-"
        + getPermutationId() + "-index.html"));

    addStandardHtmlProlog(outFile, "Compile report: Permutation "
        + permutationId, "Compile report: Permutation " + permutationId, "");
    
    NumberFormat bytesFormatter = NumberFormat.getInstance();
    bytesFormatter.setGroupingUsed(true);

    outFile.println("<div id=\"bd\">");
    outFile.println("<div id=\"soyc-summary\" class=\"g-section\">");
    outFile.println("<dl>");
    outFile.println("<dt>Full code size</dt>");
    outFile.println("<dd class=\"value\">"
        + bytesFormatter.format(globalInformation.getTotalCodeBreakdown().sizeAllCode) 
        + " Bytes</dd>");
    outFile.println("<dd class=\"report\"><a href=\"total-" + permutationId
        + "-overallBreakdown.html\">Report</a></dd>");

    outFile.println("</dl>");
    outFile.println("<dl>");
    outFile.println("<dt>Initial download size</dt>");
    // TODO(kprobst) -- add percentage here: (48%)</dd>");
    outFile.println("<dd class=\"value\">"
        + bytesFormatter.format(globalInformation.getInitialCodeBreakdown().sizeAllCode)
        + " Bytes</dd>");
    outFile.println("<dd class=\"report\"><a href=\"initial-" + permutationId
        + "-overallBreakdown.html\">Report</a></dd>");
    outFile.println("</dl>");
    outFile.println("<dl>");

    outFile.println("<dt>Left over code</dt>");
    outFile.println("<dd class=\"value\">"
        + bytesFormatter.format(globalInformation.getLeftoversBreakdown().sizeAllCode) 
        + " Bytes</dd>");
    outFile.println("<dd class=\"report\"><a href=\"leftovers-" + permutationId
        + "-overallBreakdown.html\">Report</a></dd>");
    outFile.println("</dl>");
    outFile.println("</div>");
    outFile.println("<table id=\"soyc-table-splitpoints\" class=\"soyc-table\">");
    outFile.println("<caption>");

    outFile.println("<strong>Split Points</strong>");
    outFile.println("</caption>");
    outFile.println("<colgroup>");
    outFile.println("<col id=\"soyc-splitpoint-number-col\">");
    outFile.println("<col id=\"soyc-splitpoint-location-col\">");
    outFile.println("<col id=\"soyc-splitpoint-size-col\">");
    outFile.println("</colgroup>");
    outFile.println("<thead>");

    outFile.println("<th>#</th>");
    outFile.println("<th>Location</th>");
    outFile.println("<th></th>");
    outFile.println("<th class=\"soyc-numerical-col-header\">");
    outFile.println("Size <span class=\"soyc-th-units\">(Bytes)</span>");
    outFile.println("</th>");
    outFile.println("<th class=\"soyc-numerical-col-header\">% of total</th>");
    outFile.println("</thead>");
    outFile.println("<tbody>");
    
    NumberFormat percentFormatter = NumberFormat.getPercentInstance();
    percentFormatter.setMinimumFractionDigits(1);
    percentFormatter.setMaximumFractionDigits(1);

    if (globalInformation.getSplitPointToLocation().size() >= 1) {

      int numSplitPoints = globalInformation.getSplitPointToLocation().size();
      int maxSize = globalInformation.getTotalCodeBreakdown().sizeAllCode;

      for (int i = FRAGMENT_NUMBER_TOTAL_PROGRAM; i <= numSplitPoints + 1; i++) {
        SizeBreakdown breakdown;
        if (i == FRAGMENT_NUMBER_TOTAL_PROGRAM) {
          continue;
        } else if (i == numSplitPoints + 1) { // leftovers
          continue;
        } else if (i == FRAGMENT_NUMBER_INITIAL_DOWNLOAD) {
          continue;
        } else {
          breakdown = globalInformation.splitPointCodeBreakdown(i);
        }

        String drillDownFileName = shellFileName(breakdown, getPermutationId());
        String splitPointDescription = globalInformation.getSplitPointToLocation().get(
            i);

        int size = breakdown.sizeAllCode;
        float perc = (float) size / (float) maxSize;

        outFile.println("<tr>");
        outFile.println("<td>" + i + "</td>");
        outFile.println("<td><a href=\"" + drillDownFileName + "\">"
            + splitPointDescription + "</a></td>");
        outFile.println("<td class=\"soyc-bargraph-col\">");
        outFile.println("<div class=\"soyc-bar-graph goog-inline-block\">");
        // CHECKSTYLE_OFF
        outFile.println("<div style=\"width:" + (perc * 100.0)
            + "%;\" class=\"soyc-bar-graph-fill goog-inline-block\"></div>");
        // CHECKSTYLE_ON
        outFile.println("</div>");
        outFile.println("</td>");
        outFile.println("<td class=\"soyc-numerical-col\">");
        outFile.println(bytesFormatter.format(size));
        outFile.println("</td>");
        outFile.println("<td class=\"soyc-percent-col\">" + 
            percentFormatter.format(perc) + "</td>");
        outFile.println("</tr>");
      }
    }
    outFile.println("</tbody>");
    outFile.println("</table>");
    outFile.println("</div>");
    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  private void addPopup(PrintWriter outFile, String popupName,
      String popupTitle, String popupBody) {
    outFile.println("<div class=\"soyc-popup\" id=\"" + popupName + "\">");
    outFile.println("<table>");
    outFile.println("<tr><th><b>" + popupTitle + "</b></th></tr>");
    outFile.println("<tr><td>" + popupBody + "</td></tr>");
    outFile.println("</table>");
    outFile.println("</div>");
  }

  private void addPopupLink(PrintWriter outFile, String popupName,
      String popupTitle, String href) {
    outFile.println("<a ");
    if (href != null) {
      outFile.println("href=\"" + href + "\"");
    }
    outFile.println("style=\"cursor:default;\" onMouseOver=\"show('"
        + popupName + "');\" " + "onMouseOut=\"hide('" + popupName + "');\">"
        + popupTitle + "</a>");
  }

  /**
   * Returns a file name for the dependencies list.
   */
  private String dependenciesFileName(String depGraphName) {
    return "methodDependencies-" + depGraphName + "-" + getPermutationId() + ".html";
  }

  private String[] getGeneratedTypes(ModuleMetricsArtifact moduleMetrics,
      PrecompilationMetricsArtifact precompilationMetrics) {
    List<String> initialTypes = Lists.create(moduleMetrics.getInitialTypes());
    Set<String> generatedTypes = Sets.create(precompilationMetrics.getFinalTypeOracleTypes());
    generatedTypes.removeAll(initialTypes);
    String[] results = generatedTypes.toArray(new String[generatedTypes.size()]);
    Arrays.sort(results);
    return results;
  }

  /**
   * Return a {@link java.io.File} object for a file to be emitted into the
   * output directory.
   */
  private OutputStream getOutFile(String localFileName) throws IOException {
    return outDir.getOutputStream(localFileName);
  }

  /**
   * @return the ID for the current permutation
   */
  private String getPermutationId() {
    return globalInformation.getPermutationId();
  }

  private String[] getUnreferencedTypes(
      PrecompilationMetricsArtifact precompilationMetrics) {
    List<String> astTypes = Lists.create(precompilationMetrics.getAstTypes());
    Set<String> unreferencedTypes = Sets.create(precompilationMetrics.getFinalTypeOracleTypes());
    unreferencedTypes.removeAll(astTypes);
    String[] results = unreferencedTypes.toArray(new String[unreferencedTypes.size()]);
    Arrays.sort(results);
    return results;
  }

  /**
   * Describe the code covered by the dependency graph with the supplied name.
   */
  private String inferDepGraphDescription(String depGraphName) {
    if (depGraphName.equals("initial")) {
      return "Initially Live Code";
    }

    if (depGraphName.equals("total")) {
      return "All Code";
    }

    Matcher matcher = PATTERN_SP_INT.matcher(depGraphName);
    if (matcher.matches()) {
      int splitPoint = Integer.valueOf(matcher.group(1));
      if (isInitialSplitPoint(splitPoint)) {
        return "Code Becoming Live at Split Point " + splitPoint;
      } else {
        return "Code not Exclusive to Split Point " + splitPoint;
      }
    }

    throw new RuntimeException("Unexpected dependency graph name: "
        + depGraphName);
  }

  /**
   * Returns whether a split point is initial or not.
   *
   * @param splitPoint
   * @returns true of the split point is initial, false otherwise
   */
  private boolean isInitialSplitPoint(int splitPoint) {
    return globalInformation.getSplitPointInitialLoadSequence().contains(
        splitPoint);
  }

  /**
   * Makes a file name for a leftovers status file.
   *
   * @return the file name of the leftovers status file
   */
  private String leftoversStatusFileName() {
    return "leftoverStatus-" + getPermutationId() + ".html";
  }

  /**
   * Produces an HTML file that breaks down by code type.
   *
   * @param breakdown
   * @param nameToCodeColl
   * @param nameToLitColl
   * @return the name of the produced file
   * @throws IOException
   */
  private String makeCodeTypeHtml(SizeBreakdown breakdown,
      Map<String, CodeCollection> nameToCodeColl,
      Map<String, LiteralsCollection> nameToLitColl) throws IOException {
    String outFileName = breakdown.getId() + "-" + getPermutationId()
        + "-codeTypeBreakdown.html";
    int sumSize = 0;
    TreeMap<Integer, Set<String>> sortedCodeTypes = new TreeMap<Integer, Set<String>>(
        Collections.reverseOrder());

    for (String codeType : nameToCodeColl.keySet()) {
      int curSize = nameToCodeColl.get(codeType).getCumSize(breakdown);
      sumSize += curSize;
      if (curSize != 0) {
        if (sortedCodeTypes.containsKey(curSize)) {
          Set<String> existingSet = sortedCodeTypes.get(curSize);
          existingSet.add(codeType);
          sortedCodeTypes.put(curSize, existingSet);
        } else {
          Set<String> newSet = new TreeSet<String>();
          newSet.add(codeType);
          sortedCodeTypes.put(curSize, newSet);
        }
      }
    }

    final PrintWriter outFile = new PrintWriter(getOutFile(outFileName));
    addSmallHtmlProlog(outFile, "Code breakdown");
    outFile.println("<body class=\"soyc-breakdown\">");
    outFile.println("<div class=\"g-doc\">");

    outFile.println("<table class=\"soyc-table\">");
    outFile.println("<colgroup>");
    outFile.println("<col id=\"soyc-splitpoint-type-col\">");
    outFile.println("<col id=\"soyc-splitpoint-size-col\">");
    outFile.println("</colgroup>");
    outFile.println("<thead>");
    outFile.println("<th>Type</th>");
    outFile.println("<th></th>");
    outFile.println("<th class=\"soyc-numerical-col-header\">");
    outFile.println("Size <span class=\"soyc-th-units\">(Bytes)</span>");
    outFile.println("</th>");
    outFile.println("<th class=\"soyc-numerical-col-header\">% of total</th>");
    outFile.println("</thead>");
    
    NumberFormat bytesFormatter = NumberFormat.getInstance();
    bytesFormatter.setGroupingUsed(true);
    
    NumberFormat percentFormatter = NumberFormat.getPercentInstance();
    percentFormatter.setMinimumFractionDigits(1);
    percentFormatter.setMaximumFractionDigits(1);

    for (Integer size : sortedCodeTypes.keySet()) {
      Set<String> codeTypes = sortedCodeTypes.get(size);
      for (String codeType : codeTypes) {
        String drillDownFileName = breakdown.getId() + "_" + codeType + "-"
            + getPermutationId() + "Classes.html";
        float perc = (float) size / (float) sumSize;
        outFile.println("<tr>");
        outFile.println("<td><a href=\"" + drillDownFileName
            + "\" target=\"_top\">" + codeType + "</a></td>");
        outFile.println("<td class=\"soyc-bargraph-col\">");
        outFile.println("<div class=\"soyc-bar-graph goog-inline-block\">");
        // CHECKSTYLE_OFF
        outFile.println("<div style=\"width:" + (perc * 100.0)
            + "%;\" class=\"soyc-bar-graph-fill goog-inline-block\"></div>");
        // CHECKSTYLE_ON
        outFile.println("</div>");
        outFile.println("</td>");
        outFile.println("<td class=\"soyc-numerical-col\">");
        outFile.println(bytesFormatter.format(size));
        outFile.println("</td>");
        outFile.println("<td class=\"soyc-percent-col\">" + 
            percentFormatter.format(perc) + "</td>");
        outFile.println("</tr>");
      }
    }
    outFile.println("</table>");

    int stringSize = nameToLitColl.get("string").size;
    String drillDownFileName = breakdown.getId() + "_string-"
        + getPermutationId() + "Lits.html";
    outFile.println("<p class=\"soyc-breakdown-strings\">" + stringSize
        + " bytes occupied by <a href=\"" + drillDownFileName
        + "\" target=\"_top\">Strings</a></p>");
    int unaccountedForSize = breakdown.sizeAllCode - sumSize - stringSize;
    outFile.println("<p class=\"soyc-breakdown-strings\">"
        + unaccountedForSize
        + " bytes of the JavaScript output cannot be attributed to any package or code type.</p>");
    addStandardHtmlEnding(outFile);
    outFile.close();
    return outFileName;
  }

  private void makeCompilerMetricsAstTypes(String outFileName,
      PrecompilationMetricsArtifact precompilationMetrics, String helpText)
      throws IOException {
    PrintWriter outFile = new PrintWriter(getOutFile(outFileName));
    String title = "AST Types";
    addStandardHtmlProlog(outFile, title, title, "");
    outFile.println("<p>");
    outFile.println(helpText);
    outFile.println("</p>");
    outFile.println("<pre>");
    String[] types = precompilationMetrics.getAstTypes();
    Arrays.sort(types);
    for (String type : types) {
      outFile.println(type);
    }
    outFile.println("</pre>");
    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  private void makeCompilerMetricsFinalTypeOracleTypes(String outFileName,
      PrecompilationMetricsArtifact precompilationMetrics, String helpText)
      throws IOException {
    PrintWriter outFile = new PrintWriter(getOutFile(outFileName));
    String title = "Final Type Oracle Types";
    addStandardHtmlProlog(outFile, title, title, "");
    outFile.println("<p>");
    outFile.println(helpText);
    outFile.println("</p>");
    outFile.println("<pre>");
    String[] types = precompilationMetrics.getFinalTypeOracleTypes();
    Arrays.sort(types);
    for (String type : types) {
      outFile.println(type);
    }
    outFile.println("</pre>");
    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  private void makeCompilerMetricsGeneratedTypes(String outFileName,
      String[] generatedTypes, String helpText) throws IOException {
    PrintWriter outFile = new PrintWriter(getOutFile(outFileName));
    String title = "Generated Types";
    addStandardHtmlProlog(outFile, title, title, "");
    outFile.println("<p>");
    outFile.println(helpText);
    outFile.println("</p>");
    outFile.println("<pre>");
    for (String type : generatedTypes) {
      outFile.println(type);
    }
    outFile.println("</pre>");
    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  private void makeCompilerMetricsInitialTypeOracleTypes(String outFileName,
      ModuleMetricsArtifact moduleMetrics, String helpText) throws IOException {
    PrintWriter outFile = new PrintWriter(getOutFile(outFileName));
    String title = "Initial Type Oracle Types (built from source path)";
    addStandardHtmlProlog(outFile, title, title, "");
    outFile.println("<p>");
    outFile.println(helpText);
    outFile.println("</p>");
    outFile.println("<pre>");
    String[] types = moduleMetrics.getInitialTypes();
    Arrays.sort(types);
    for (String type : types) {
      outFile.println(type);
    }
    outFile.println("</pre>");
    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  private void makeCompilerMetricsSources(String outFileName,
      ModuleMetricsArtifact moduleMetrics, String helpText) throws IOException {
    PrintWriter outFile = new PrintWriter(getOutFile(outFileName));
    String title = "Sources on Source Path";
    addStandardHtmlProlog(outFile, title, title, "");
    outFile.println("<p>");
    outFile.println(helpText);
    outFile.println("</p>");
    outFile.println("<pre>");
    String[] sources = moduleMetrics.getSourceFiles();
    Arrays.sort(sources);
    for (String source : sources) {
      outFile.println(source);
    }
    outFile.println("</pre>");
    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  private void makeCompilerMetricsUnreferencedTypes(String outFileName,
      String[] unreferencedTypes, String helpText) throws IOException {
    PrintWriter outFile = new PrintWriter(getOutFile(outFileName));
    String title = "Unreferenced Types";
    addStandardHtmlProlog(outFile, title, title, "");
    outFile.println("<p>");
    outFile.println(helpText);
    outFile.println("</p>");
    outFile.println("<pre>");
    for (String type : unreferencedTypes) {
      outFile.println(type);
    }
    outFile.println("</pre>");
    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  /**
   * Produces an HTML file that displays dependencies.
   *
   * @param depGraphName name of dependency graph
   * @param dependencies map of dependencies
   * @throws IOException
   */
  private void makeDependenciesHtml(String depGraphName,
      Map<String, String> dependencies) throws IOException {
    String curPackageName = "";

    HtmlInterner interner = new HtmlInterner();

    for (String reportMethod : dependencies.keySet()) {
      interner.intern(getPackageSubstring(reportMethod));
      interner.intern(getClassSubstring(reportMethod));
      interner.intern(getMethodSubstring(reportMethod));

      String depMethod = dependencies.get(reportMethod);
      while (depMethod != null) {
        interner.intern(getPackageSubstring(depMethod));
        interner.intern(getClassSubstring(depMethod));
        interner.intern(getMethodSubstring(depMethod));
        depMethod = dependencies.get(depMethod);
      }
    }
    interner.freeze();

    // Write out the interned data values as a script element
    String jsFileName = "methodDependencies-" + depGraphName + "-"
        + getPermutationId() + ".js";
    PrintWriter outFile = new PrintWriter(getOutFile(jsFileName));
    interner.printInternedDataAsJs(outFile);
    outFile.close();

    List<String> classesInPackage = new ArrayList<String>();
    for (String method : dependencies.keySet()) {
      classesInPackage.add(method);
    }
    makeDependenciesInternedHtml(depGraphName, 
        classesInPackage, dependencies, interner, jsFileName);
  }

  /**
   * Produces an HTML file that displays dependencies.
   *
   * @param depGraphName name of dependency graph
   * @param dependencies map of dependencies
   * @throws IOException
   */
  private void makeDependenciesInternedHtml(String depGraphName,
      List<String> classesInSplitPoint,
      Map<String, String> dependencies, HtmlInterner interner, String jsFileName)
      throws IOException {
    String depGraphDescription = inferDepGraphDescription(depGraphName);
    PrintWriter outFile = null;
    String curClassName = "";

    String outFileName = dependenciesFileName(depGraphName);
    outFile = new PrintWriter(getOutFile(outFileName));
    
    addStandardHtmlProlog(outFile, "Method Dependencies for "
        + depGraphDescription,
        "Method Dependencies for " + depGraphDescription, null);

    outFile.print("<script src=\"" + jsFileName
        + "\" language=\"javascript\" ></script>");

    // Write out the HTML
    outFile.print("<script>");
    for (String method : classesInSplitPoint) {
      // this key set is already in alphabetical order
      // get the package of this method, i.e., everything up to .[A-Z]

      String className = method.replaceAll("::.*", "");
      String depMethod = dependencies.get(method);
      if (curClassName.compareTo(className) != 0) {
        curClassName = className;
        if (method != classesInSplitPoint.get(0)) {
          outFile.print("j();"); // close the previous table if not the first
        }
        interner.printDependencyClassHeader(outFile, className);
      }
      interner.printDependency(outFile, dependencies, method, depMethod);
    }
    outFile.println("</script>");

    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  /**
   * Produces an HTML file that shows information about a package.
   *
   * @param breakdown
   * @return the name of the HTML file
   */
  private String makePackageHtml(SizeBreakdown breakdown) throws IOException {
    String outFileName = breakdown.getId() + "-" + getPermutationId() + "-"
        + "packageBreakdown.html";
    Map<String, Integer> packageToPartialSize = breakdown.packageToSize;
    TreeMap<Integer, Set<String>> sortedPackages = new TreeMap<Integer, Set<String>>(
        Collections.reverseOrder());
    float sumSize = 0f;
    for (String packageName : packageToPartialSize.keySet()) {
      Integer curSize = packageToPartialSize.get(packageName);
      if (sortedPackages.containsKey(curSize)) {
        Set<String> existingSet = sortedPackages.get(curSize);
        existingSet.add(packageName);
        sortedPackages.put(curSize, existingSet);
      } else {
        Set<String> newSet = new TreeSet<String>();
        newSet.add(packageName);
        sortedPackages.put(curSize, newSet);
      }
      sumSize += curSize;
    }

    final PrintWriter outFile = new PrintWriter(getOutFile(outFileName));
    addSmallHtmlProlog(outFile, "Package breakdown");
    outFile.println("<body class=\"soyc-breakdown\">");
    outFile.println("<div class=\"g-doc\">");

    outFile.println("<table class=\"soyc-table\">");
    outFile.println("<colgroup>");
    outFile.println("<col id=\"soyc-splitpoint-type-col\">");
    outFile.println("<col id=\"soyc-splitpoint-size-col\">");
    outFile.println("</colgroup>");
    outFile.println("<thead>");
    outFile.println("<th>Packages (Sorted by size)</th>");
    outFile.println("<th></th>");
    outFile.println("<th class=\"soyc-numerical-col-header\">");
    outFile.println("Size <span class=\"soyc-th-units\">(Bytes)</span>");
    outFile.println("</th>");
    outFile.println("<th class=\"soyc-numerical-col-header\">% of total</th>");
    outFile.println("</thead>");
    
    NumberFormat bytesFormatter = NumberFormat.getInstance();
    bytesFormatter.setGroupingUsed(true);
    
    NumberFormat percentFormatter = NumberFormat.getPercentInstance();
    percentFormatter.setMinimumFractionDigits(1);
    percentFormatter.setMaximumFractionDigits(1);

    for (int size : sortedPackages.keySet()) {
      if (size == 0) {
        continue;
      }
      Set<String> packageNames = sortedPackages.get(size);
      for (String packageName : packageNames) {
        String drillDownFileName = classesInPackageFileName(breakdown,
            getPermutationId())
            + "#" + hashedFilenameFragment(packageName);
        float perc = size / sumSize;
        outFile.println("<tr>");
        outFile.println("<td><a href=\"" + drillDownFileName
            + "\" target=\"_top\">" + packageName + "</a></td>");
        outFile.println("<td class=\"soyc-bargraph-col\">");
        outFile.println("<div class=\"soyc-bar-graph goog-inline-block\">");
        // CHECKSTYLE_OFF
        outFile.println("<div style=\"width:" + (perc * 100.0)
            + "%;\" class=\"soyc-bar-graph-fill goog-inline-block\"></div>");
        // CHECKSTYLE_ON
        outFile.println("</div>");
        outFile.println("</td>");
        outFile.println("<td class=\"soyc-numerical-col\">");
        outFile.println(bytesFormatter.format(size));
        outFile.println("</td>");
        outFile.println("<td class=\"soyc-percent-col\">" + 
            percentFormatter.format(perc) + "</td>");
        outFile.println("</tr>");
      }
    }

    outFile.println("</table>");
    addStandardHtmlEnding(outFile);
    outFile.close();
    return outFileName;
  }

  /**
   * Find which split points include code belonging to <code>className</code>.
   */
  private Iterable<Integer> splitPointsWithClass(String className) {
    List<Integer> sps = new ArrayList<Integer>();
    for (int sp = 1; sp <= globalInformation.getNumSplitPoints(); sp++) {
      Map<String, Integer> classToSize = globalInformation.splitPointCodeBreakdown(sp).classToSize;
      if (classToSize.containsKey(className)) {
        sps.add(sp);
      }
    }
    return sps;
  }

  private String splitStatusFileName() {
    return "splitStatus-" + getPermutationId() + ".html";
  }
}
