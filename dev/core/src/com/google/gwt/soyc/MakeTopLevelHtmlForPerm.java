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

import com.google.gwt.core.ext.soyc.impl.SizeMapRecorder;
import com.google.gwt.dev.util.Util;
import com.google.gwt.soyc.io.OutputDirectory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility to make the top level HTTML file for one permutation.
 */
public class MakeTopLevelHtmlForPerm {
  /**
   * A dependency linker for the initial code download. It links to the
   * dependencies for the initial download.
   */
  public class DependencyLinkerForInitialCode implements DependencyLinker {
    public String dependencyLinkForClass(String className) {
      String packageName = globalInformation.getClassToPackage().get(className);
      assert packageName != null;
      return dependenciesFileName("initial", packageName) + "#" + className;
    }
  }

  /**
   * A dependency linker for the leftovers fragment. It links to leftovers
   * status pages.
   */
  public class DependencyLinkerForLeftoversFragment implements DependencyLinker {
    public String dependencyLinkForClass(String className) {
      return leftoversStatusFileName(className);
    }
  }

  /**
   * A dependency linker for the total program breakdown. It links to a split
   * status page.
   * 
   */
  public class DependencyLinkerForTotalBreakdown implements DependencyLinker {
    public String dependencyLinkForClass(String className) {
      return splitStatusFileName(className);
    }
  }

  /**
   * A dependency linker that never links to anything.
   */
  public static class NullDependencyLinker implements DependencyLinker {
    public String dependencyLinkForClass(String className) {
      return null;
    }
  }

  interface DependencyLinker {
    String dependencyLinkForClass(String className);
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

  private static String escapeXml(String unescaped) {
      return SizeMapRecorder.escapeXml(unescaped);
  }

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
      outFile.print("<li><a href=\"SoycDashboard" + "-" + permutationId
        + "-index.html\">Permutation " + permutationId);
      if (permutationInfoList.size() > 0) {
        for (String desc : permutationInfoList) {
          outFile.println("  (" + desc + ")");
        }
        outFile.println("</a></li>");
      } else {
        outFile.println("</a>");
      }
    }
    outFile.println("</ul>");
    addStandardHtmlEnding(outFile);
    outFile.close();
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
    outFile.println("<a href=\"index.html\" id=\"gwt-logo\" class=\"soyc-ir\"><span>Google Web Toolkit</span></a>");
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
    outFile.println("<h2>" + header2 + "</h2>");
  }

  private static String classesInPackageFileName(SizeBreakdown breakdown,
      String packageName, String permutationId) {
    return breakdown.getId() + "_" + filename(packageName) + "-"
        + permutationId + "_Classes.html";
  }

  /**
   * Convert a potentially long string into a short file name. The current
   * implementation simply hashes the long name.
   */
  private static String filename(String longFileName) {
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

    outFile.println("<div id=\"bd\">  ");
    outFile.println("<h2>Package breakdown</h2>");
    outFile.println("<iframe class='soyc-iframe-package' src=\""
        + packageBreakdownFileName + "\" scrolling=auto></iframe>");
    outFile.println("<h2>Code type breakdown</h2>");
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
      TreeMap<Float, String> sortedClasses = new TreeMap<Float, String>(
          Collections.reverseOrder());

      for (String className : nameToCodeColl.get(codeType).classes) {
        if (breakdown.classToSize.containsKey(className)) {
          float curSize = 0f;
          if (breakdown.classToSize.containsKey(className)) {
            curSize = breakdown.classToSize.get(className);
          }
          if (curSize != 0f) {
            sortedClasses.put(curSize, className);
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
      outFile.println("<th>Size <span class=\"soyc-th-units\">Bytes (% All Code)</span></th>");
      outFile.println("</thead>");

      for (Float size : sortedClasses.keySet()) {
        String className = sortedClasses.get(size);
        float perc = (size / sumSize) * 100;
        outFile.println("<tr>");
        outFile.println("<td>" + className + "</a></td>");
        outFile.println("<td>");
        outFile.println("<div class=\"soyc-bar-graph goog-inline-block\">");
        // CHECKSTYLE_OFF
        outFile.println("<div style=\"width:" + perc
            + "%;\" class=\"soyc-bar-graph-fill goog-inline-block\"></div>");
        // CHECKSTYLE_ON
        outFile.println("</div>");
        outFile.println(size + " (" + formatNumber(perc) + "%)");
        outFile.println("</td>");
        outFile.println("</tr>");
      }
      addStandardHtmlEnding(outFile);
      outFile.close();
    }
  }

  public void makeDependenciesHtml() throws IOException {
    for (String depGraphName : globalInformation.dependencies.keySet()) {
      makeDependenciesHtml(depGraphName,
          globalInformation.dependencies.get(depGraphName));
    }
  }

  public void makeLeftoverStatusPages() throws IOException {
    for (String className : globalInformation.getClassToPackage().keySet()) {
      makeLeftoversStatusPage(className);
    }
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
    for (String packageName : globalInformation.getPackageToClasses().keySet()) {
      TreeMap<Float, String> sortedClasses = new TreeMap<Float, String>(
          Collections.reverseOrder());
      float sumSize = 0f;
      for (String className : globalInformation.getPackageToClasses().get(
          packageName)) {
        float curSize = 0f;
        if (!breakdown.classToSize.containsKey(className)) {
          // This class not present in this code collection
        } else {
          curSize = breakdown.classToSize.get(className);
        }
        if (curSize != 0f) {
          sumSize += curSize;
          sortedClasses.put(curSize, className);
        }
      }

      PrintWriter outFile = new PrintWriter(
          getOutFile(classesInPackageFileName(breakdown, packageName,
              getPermutationId())));

      addStandardHtmlProlog(outFile, "Classes in package " + packageName,
          "Classes in package " + packageName,
          headerLineForBreakdown(breakdown));
      outFile.println("<table class=\"soyc-table\">");
      outFile.println("<colgroup>");
      outFile.println("<col id=\"soyc-splitpoint-type-col\">");
      outFile.println("<col id=\"soyc-splitpoint-size-col\">");
      outFile.println("</colgroup>");
      outFile.println("<thead>");
      outFile.println("<th>Package</th>");
      outFile.println("<th>Size <span class=\"soyc-th-units\">Bytes (% All Code)</span></th>");
      outFile.println("</thead>");

      for (Float size : sortedClasses.keySet()) {
        String className = sortedClasses.get(size);
        String drillDownFileName = depLinker.dependencyLinkForClass(className);
        float perc = (size / sumSize) * 100;
        outFile.println("<tr>");
        if (drillDownFileName == null) {
          outFile.println("<td>" + className + "</td>");
        } else {
          outFile.println("<td><a href=\"" + drillDownFileName
              + "\" target=\"_top\">" + className + "</a></td>");
        }
        outFile.println("<td>");
        outFile.println("<div class=\"soyc-bar-graph goog-inline-block\">");
        // CHECKSTYLE_OFF
        outFile.println("<div style=\"width:" + perc
            + "%;\" class=\"soyc-bar-graph-fill goog-inline-block\"></div>");
        // CHECKSTYLE_ON
        outFile.println("</div>");
        outFile.println(size + " (" + formatNumber(perc) + "%)");
        outFile.println("</td>");
        outFile.println("</tr>");
      }

      outFile.println("</table>");
      addStandardHtmlEnding(outFile);
      outFile.close();
    }
  }

  public void makeSplitStatusPages() throws IOException {
    for (String className : globalInformation.getClassToPackage().keySet()) {
      makeSplitStatusPage(className);
    }
  }

  public void makeTopLevelShell() throws IOException {

    String permutationId = getPermutationId();
    PrintWriter outFile = new PrintWriter(getOutFile("SoycDashboard" + "-"
        + getPermutationId() + "-index.html"));

    addStandardHtmlProlog(outFile, "Compile report: Permutation "
        + permutationId, "Compile report: Permutation " + permutationId, "");

    outFile.println("<div id=\"bd\">");
    outFile.println("<div id=\"soyc-summary\" class=\"g-section\">");
    outFile.println("<dl>");
    outFile.println("<dt>Full code size</dt>");
    outFile.println("<dd class=\"value\">"
        + globalInformation.getTotalCodeBreakdown().sizeAllCode
        + " Bytes</dd>");
    outFile.println("<dd class=\"report\"><a href=\"total-" + permutationId
        + "-overallBreakdown.html\">Report</a></dd>");

    outFile.println("</dl>");
    outFile.println("<dl>");
    outFile.println("<dt>Initial download size</dt>");
    // TODO(kprobst) -- add percentage here: (48%)</dd>");
    outFile.println("<dd class=\"value\">"
        + globalInformation.getInitialCodeBreakdown().sizeAllCode + " Bytes</dd>");
    outFile.println("<dd class=\"report\"><a href=\"initial-" + permutationId
        + "-overallBreakdown.html\">Report</a></dd>");
    outFile.println("</dl>");
    outFile.println("<dl>");

    outFile.println("<dt>Left over code</dt>");
    outFile.println("<dd class=\"value\">"
        + globalInformation.getLeftoversBreakdown().sizeAllCode + " Bytes</dd>");
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
    outFile.println("<th>Size</th>");
    outFile.println("</thead>");
    outFile.println("<tbody>");

    if (globalInformation.getSplitPointToLocation().size() >= 1) {

      int numSplitPoints = globalInformation.getSplitPointToLocation().size();
      float maxSize = globalInformation.getTotalCodeBreakdown().sizeAllCode;

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

        float size = breakdown.sizeAllCode;
        float ratio;
        ratio = (size / maxSize) * 100;

        outFile.println("<tr>");
        outFile.println("<td>" + i + "</td>");
        outFile.println("<td><a href=\"" + drillDownFileName + "\">"
            + splitPointDescription + "</a></td>");
        outFile.println("<td>");
        outFile.println("<div class=\"soyc-bar-graph goog-inline-block\">");
        // CHECKSTYLE_OFF
        outFile.println("<div style=\"width:" + ratio
            + "%;\" class=\"soyc-bar-graph-fill goog-inline-block\"></div>");
        // CHECKSTYLE_ON
        outFile.println("</div>");
        outFile.println((int) size + " Bytes (" + formatNumber(ratio) + "%)");
        outFile.println("</td>");
        outFile.println("</tr>");
      }
    }
    outFile.println("</tbody>");
    outFile.println("</table>");
    outFile.println("</div>");
    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  private void addLefttoversStatus(String className, String packageName,
      PrintWriter outFile) {
    outFile.println("<ul class=\"soyc-exclusive\">");
    if (globalInformation.dependencies != null) {
      outFile.println("<li><a href=\""
          + dependenciesFileName("total", packageName) + "#" + className
          + "\">See why it's live</a></li>");
      for (int sp = 1; sp <= globalInformation.getNumSplitPoints(); sp++) {
        outFile.println("<li><a href=\""
            + dependenciesFileName("sp" + sp, packageName) + "#" + className
            + "\">See why it's not exclusive to s.p. #" + sp + " ("
            + globalInformation.getSplitPointToLocation().get(sp)
            + ")</a></li>");
      }
    }
    outFile.println("</ul>");
  }

  /**
   * Returns a file name for the dependencies list.
   */
  private String dependenciesFileName(String depGraphName, String packageName) {
    return "methodDependencies-" + depGraphName + "-" + filename(packageName)
        + "-" + getPermutationId() + ".html";
  }

  /**
   * Format floating point number to two decimal points.
   * 
   * @param number
   * @return formatted number
   */
  private String formatNumber(float number) {
    DecimalFormat formatBy = new DecimalFormat("#.##");
    return formatBy.format(number);
  }

  /**
   * Return a {@link File} object for a file to be emitted into the output
   * directory.
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
   * @param className
   * @return the file name of the leftovers status file
   */
  private String leftoversStatusFileName(String className) {
    return "leftoverStatus-" + filename(className) + "-" + getPermutationId()
        + ".html";
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
    float sumSize = 0f;
    TreeMap<Float, String> sortedCodeTypes = new TreeMap<Float, String>(
        Collections.reverseOrder());

    for (String codeType : nameToCodeColl.keySet()) {
      float curSize = nameToCodeColl.get(codeType).getCumPartialSize(breakdown);
      sumSize += curSize;
      if (curSize != 0f) {
        sortedCodeTypes.put(curSize, codeType);
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
    outFile.println("<th>Size</th>");
    outFile.println("</thead>");

    for (Float size : sortedCodeTypes.keySet()) {
      String codeType = sortedCodeTypes.get(size);
      String drillDownFileName = breakdown.getId() + "_" + codeType + "-"
          + getPermutationId() + "Classes.html";
      float perc = (size / sumSize) * 100;
      outFile.println("<tr>");
      outFile.println("<td><a href=\"" + drillDownFileName
          + "\" target=\"_top\">" + codeType + "</a></td>");
      outFile.println("<td>");
      outFile.println("<div class=\"soyc-bar-graph goog-inline-block\">");
      // CHECKSTYLE_OFF
      outFile.println("<div style=\"width:" + perc
          + "%;\" class=\"soyc-bar-graph-fill goog-inline-block\"></div>");
      // CHECKSTYLE_ON
      outFile.println("</div>");
      outFile.println(size + " (" + formatNumber(perc) + "%)");
      outFile.println("</td>");
      outFile.println("</tr>");
    }
    outFile.println("</table>");

    TreeMap<Float, String> sortedLitTypes = new TreeMap<Float, String>(
        Collections.reverseOrder());
    float curSize = nameToLitColl.get("string").size;
    sumSize += curSize;
    if (curSize != 0f) {
      sortedLitTypes.put(curSize, "string");
    }
    for (Float size : sortedLitTypes.keySet()) {
      String literal = sortedLitTypes.get(size);
      String drillDownFileName = breakdown.getId() + "_" + literal + "-"
          + getPermutationId() + "Lits.html";
      outFile.println("<p class=\"soyc-breakdown-strings\"><a href=\""
          + drillDownFileName + "\" target=\"_top\">Strings</a> occupy " + size
          + " bytes</p>");
    }
    addStandardHtmlEnding(outFile);
    outFile.close();
    return outFileName;
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
    String depGraphDescription = inferDepGraphDescription(depGraphName);
    PrintWriter outFile = null;
    String curPackageName = "";
    String curClassName = "";

    for (String method : dependencies.keySet()) {
      // this key set is already in alphabetical order
      // get the package of this method, i.e., everything up to .[A-Z]

      String packageName = method;
      packageName = packageName.replaceAll("\\.\\p{Upper}.*", "");

      String className = method;
      className = className.replaceAll("::.*", "");

      if ((curPackageName.compareTo("") == 0)
          || (curPackageName.compareTo(packageName) != 0)) {

        curPackageName = packageName;
        if (outFile != null) {
          // finish up the current file
          addStandardHtmlEnding(outFile);
          outFile.close();
        }

        String outFileName = dependenciesFileName(depGraphName, curPackageName);
        outFile = new PrintWriter(getOutFile(outFileName));

        String packageDescription = packageName.length() == 0
            ? "the default package" : packageName;
        addStandardHtmlProlog(outFile, "Method Dependencies for "
            + depGraphDescription, "Method Dependencies for "
            + depGraphDescription, "Showing Package: " + packageDescription);
      }
      String name = method;
      if (curClassName.compareTo(className) != 0) {
        name = className;
        curClassName = className;
        outFile.println("<a name=\"" + curClassName
            + "\"><h3 class=\"soyc-class-header\">Class: " + curClassName
            + "</a></h3>");
      }

      outFile.println("<div class='main'>");
      outFile.println("<a class='toggle soyc-call-stack-link' onclick='toggle.call(this)'><span class='calledBy'> Call stack: </span>"
          + name + "</a>");
      outFile.println("<ul class=\"soyc-call-stack-list\">");

      String depMethod = dependencies.get(method);
      while (depMethod != null) {
        String nextDep = dependencies.get(depMethod);
        if (nextDep != null) {
          outFile.println("<li>" + depMethod + "</li>");
        }
        depMethod = nextDep;
      }
      outFile.println("</ul>");
      outFile.println("</div>");
    }
    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  /**
   * Produces an HTML file for leftovers status.
   * 
   * @param className
   * @throws IOException
   */
  private void makeLeftoversStatusPage(String className) throws IOException {
    String packageName = globalInformation.getClassToPackage().get(className);
    PrintWriter outFile = new PrintWriter(
        getOutFile(leftoversStatusFileName(className)));
    addStandardHtmlProlog(outFile, "Leftovers page for " + className,
        "Leftovers page for " + className, "");
    outFile.println("<div id=\"bd\">");
    outFile.println("<p>This class has some leftover code, neither initial nor exclusive to any split point:</p>");
    addLefttoversStatus(className, packageName, outFile);
    addStandardHtmlEnding(outFile);

    outFile.close();
  }

  /**
   * Produces an HTML file that shows information about a package.
   * 
   * @param breakdown
   * @return the name of the HTML file
   * @throws IOException
   */
  private String makePackageHtml(SizeBreakdown breakdown) throws IOException {
    String outFileName = breakdown.getId() + "-" + getPermutationId() + "-"
        + "packageBreakdown.html";
    Map<String, Integer> packageToPartialSize = breakdown.packageToSize;
    TreeMap<Integer, String> sortedPackages = new TreeMap<Integer, String>(
        Collections.reverseOrder());
    float sumSize = 0f;
    for (String packageName : packageToPartialSize.keySet()) {
      sortedPackages.put(packageToPartialSize.get(packageName), packageName);
      sumSize += packageToPartialSize.get(packageName);
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
    outFile.println("<th>Package</th>");
    outFile.println("<th>Size <span class=\"soyc-th-units\">Bytes (% All Code)</span></th>");
    outFile.println("</thead>");

    for (int size : sortedPackages.keySet()) {
      String packageName = sortedPackages.get(size);
      String drillDownFileName = classesInPackageFileName(breakdown,
          packageName, getPermutationId());
      float perc = (size / sumSize) * 100;
      outFile.println("<tr>");
      outFile.println("<td><a href=\"" + drillDownFileName
          + "\" target=\"_top\">" + packageName + "</a></td>");
      outFile.println("<td>");
      outFile.println("<div class=\"soyc-bar-graph goog-inline-block\">");
      // CHECKSTYLE_OFF
      outFile.println("<div style=\"width:" + perc
          + "%;\" class=\"soyc-bar-graph-fill goog-inline-block\"></div>");
      // CHECKSTYLE_ON
      outFile.println("</div>");
      outFile.println(size + " (" + formatNumber(perc) + "%)");
      outFile.println("</td>");
      outFile.println("</tr>");
    }

    outFile.println("</table>");
    addStandardHtmlEnding(outFile);
    outFile.close();
    return outFileName;
  }

  private void makeSplitStatusPage(String className) throws IOException {
    String packageName = globalInformation.getClassToPackage().get(className);
    PrintWriter outFile = new PrintWriter(
        getOutFile(splitStatusFileName(className)));

    addStandardHtmlProlog(outFile, "Split point status for " + className,
        "Split point status for " + className, "");
    outFile.println("<div id=\"bd\">");

    if (globalInformation.getInitialCodeBreakdown().classToSize.containsKey(className)) {
      if (globalInformation.dependencies != null) {
        outFile.println("<p>Some code is included in the initial fragment (<a href=\""
            + dependenciesFileName("initial", packageName)
            + "#"
            + className
            + "\">see why</a>)</p>");
      } else {
        outFile.println("<p>Some code is included in the initial fragment</p>");
      }
    }
    for (int sp : splitPointsWithClass(className)) {
      outFile.println("<p>Some code downloads with split point " + sp + ": "
          + globalInformation.getSplitPointToLocation().get(sp) + "</p>");
    }
    if (globalInformation.getLeftoversBreakdown().classToSize.containsKey(className)) {
      outFile.println("<p>Some code is left over:</p>");
      addLefttoversStatus(className, packageName, outFile);
    }

    addStandardHtmlEnding(outFile);
    outFile.close();
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

  private String splitStatusFileName(String className) {
    return "splitStatus-" + filename(className) + "-" + getPermutationId()
        + ".html";
  }
}
