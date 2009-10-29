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
import java.util.TreeMap;
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
    String escaped = unescaped.replaceAll("\\&", "&amp;");
    escaped = escaped.replaceAll("\\<", "&lt;");
    escaped = escaped.replaceAll("\\>", "&gt;");
    escaped = escaped.replaceAll("\\\"", "&quot;");
    escaped = escaped.replaceAll("\\'", "&apos;");
    return escaped;
  }

  public static void makeTopLevelHtmlForAllPerms(
      Map<String, String> allPermsInfo, OutputDirectory outDir)
      throws IOException {
    PrintWriter outFile = new PrintWriter(outDir.getOutputStream("index.html"));
    addStandardHtmlProlog(outFile, "Compile report", "Overview of permutations");
    outFile.println("<div style='overflow:auto; background-color:white'>");
    outFile.println("<center>");
    for (String permutationId : allPermsInfo.keySet()) {
      String permutationInfo = allPermsInfo.get(permutationId);
      outFile.print("<p><a href=\"SoycDashboard" + "-" + permutationId
          + "-index.html\">Permutation " + permutationId);
      if (permutationInfo.length() > 0) {
        outFile.println(" (" + permutationInfo + ")" + "</a>");
      } else {
        outFile.println("</a>");
      }
    }
    outFile.println("</center>");
    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  private static void addStandardHtmlEnding(final PrintWriter out) {
    out.println("</div>");
    out.println("</body>");
    out.println("</html>");
  }

  private static void addStandardHtmlProlog(final PrintWriter outFile, String header,
      String barText) {
    outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
    outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
    outFile.println("<html>");
    outFile.println("<head>");
    if ((header != null) && (header.compareTo("") != 0)) {
      outFile.println("<title>" + header + "</title>");
    } else {
      outFile.println("<title/>");
    }
    outFile.println("<meta http-equiv=\"content-type\" content=\"text/html;chars  et=ISO-8859-1\">");
    outFile.println("<link rel=\"stylesheet\" href=\"soycStyling.css\" media=\"screen\">");
    outFile.println("</head>");
    outFile.println("<body> ");
    outFile.println("<div>");
    outFile.println("<h2 id='header'>");
    outFile.println("<img id='logo' src=\"http://code.google.com/webtoolkit/images/gwt-logo.png\"/>");
    outFile.println(header);
    outFile.println("</h2>");
    outFile.println("</div>");
    if ((barText != null) && (barText.compareTo("") != 0)) {
      outFile.println("<div id=\"topBar\">");
      outFile.println(barText);
      outFile.println("</div>");
    } else {
      outFile.println("<div id=\"topBar\"><br></div>");
    }
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

  /**
   * Adds a header line indicating which breakdown is being analyzed.
   */
  private void addHeaderWithBreakdownContext(SizeBreakdown breakdown,
      final PrintWriter outFile, String header) {
    addStandardHtmlProlog(outFile, header, headerLineForBreakdown(breakdown));
  }

  public void makeBreakdownShell(SizeBreakdown breakdown) throws IOException {
    Map<String, CodeCollection> nameToCodeColl = breakdown.nameToCodeColl;
    Map<String, LiteralsCollection> nameToLitColl = breakdown.nameToLitColl;

    PrintWriter outFile = new PrintWriter(getOutFile(shellFileName(breakdown,
        getPermutationId())));

    outFile.println("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML//EN\">");
    outFile.println("<html>");
    outFile.println("<head>");
    outFile.println("  <title>Story of Your Compile - Top Level Dashboard for Permutation</title>");
    outFile.println("  <link rel=\"stylesheet\" href=\"soycStyling.css\">");
    outFile.println("</head>");

    outFile.println("<body>");
    outFile.println("<div class='abs mainHeader'>");

    outFile.println("<center>");
    outFile.println("<h3>Story of Your Compile Dashboard</h3>");

    addHeaderWithBreakdownContext(breakdown, outFile, "Compile report");

    outFile.println("</center>");

    outFile.println("<hr>");
    outFile.println("</div>");

    outFile.println("<div class='abs mainContent' style='overflow:auto'>");
    outFile.println("<div class='abs packages'>");

    outFile.println("<div class='abs header'>Package breakdown</div>");
    outFile.println("<div class='abs innerContent'>");
    String packageBreakdownFileName = makePackageHtml(breakdown);
    outFile.println(" <iframe class='frame' src=\"" + packageBreakdownFileName
        + "\" scrolling=auto></iframe>");
    outFile.println("</div>");
    outFile.println("</div>");

    outFile.println("<div class='abs codeType'>");
    outFile.println("<div class='abs header'>Code type breakdown</div>");

    outFile.println("<div class='abs innerContent'>");
    String codeTypeBreakdownFileName = makeCodeTypeHtml(breakdown,
        nameToCodeColl, nameToLitColl);
    outFile.println("<iframe class='frame' src=\"" + codeTypeBreakdownFileName
        + "\" scrolling=auto></iframe>");
    outFile.println("</div>");
    outFile.println("</div>");
    outFile.println("</div>");
    outFile.println("</body>");
    outFile.println("</html>");
    outFile.close();
  }

  public void makeCodeTypeClassesHtmls(SizeBreakdown breakdown)
      throws IOException {
    HashMap<String, CodeCollection> nameToCodeColl = breakdown.nameToCodeColl;

    for (String codeType : nameToCodeColl.keySet()) {

      // construct file name
      String outFileName = breakdown.getId() + "_" + codeType + "-"
          + getPermutationId() + "Classes.html";

      float maxSize = 0f;
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
            if (curSize > maxSize) {
              maxSize = curSize;
            }
          }
        }
      }

      final PrintWriter outFile = new PrintWriter(getOutFile(outFileName));
      addHeaderWithBreakdownContext(breakdown, outFile, "Classes in package "
          + codeType);
      outFile.println("<hr>");
      outFile.println("</div>");
      outFile.println("<div class='abs mainContent' style='overflow:auto'>");
      outFile.println("<table style='width:100%'>");
      outFile.println("<thead>");
      outFile.println("<th class='barlabel'>Size</th>");
      outFile.println("<th class='barlabel'></th>");
      outFile.println("<th style='width:100%' class='barlabel'></th>");
      outFile.println("</thead>");

      for (Float size : sortedClasses.keySet()) {

        String className = sortedClasses.get(size);
        float ratio = (size / maxSize) * 85;
        if (ratio < 1) {
          ratio = 1;
        }
        outFile.println("<tr>");
        outFile.println("<td class=\"barlabel\">" + size + "</td>");
        outFile.println("<td class=\"barlabel\">" + className + "</td>");
        outFile.println("<td class=\"box\">");
        outFile.println("<div style=\"width:"
            + ratio
            + "%;\" class=\"lb\"><div class=\"rb\"><div class=\"bb\"><div class=\"blc\"><div class=\"brc\"><div class=\"tb\"><div class=\"tlc\"><div class=\"trc\"><div class=\"content\"></div></div></div></div></div></div></div></div>");
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
      addHeaderWithBreakdownContext(breakdown, outFile, "Literals of type "
          + literalType);
      outFile.println("<center>");
      outFile.println("<table border=\"1\" width=\"80%\" style=\"font-size: 11pt;\" bgcolor=\"white\">");

      for (String literal : nameToLitColl.get(literalType).literals) {
        String newLiteral = "";
        if (literal.length() > 80) {
          int i;
          for (i = 80; i < literal.length(); i = i + 80) {
            String part1 = literal.substring(i - 80, i);
            newLiteral = newLiteral + part1 + " ";
          }
          if (i - 80 > 0) {
            newLiteral = newLiteral + literal.substring(i - 80);
          }
        } else {
          newLiteral = literal;
        }

        if (newLiteral.trim().length() == 0) {
          newLiteral = "[whitespace only string]";
        }
        String escliteral = escapeXml(newLiteral);

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
      float maxSize = 0f;
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
          if (curSize > maxSize) {
            maxSize = curSize;
          }
        }
      }

      PrintWriter outFile = new PrintWriter(
          getOutFile(classesInPackageFileName(breakdown, packageName,
              getPermutationId())));

      addHeaderWithBreakdownContext(breakdown, outFile, "Classes in package "
          + packageName);
      outFile.println("<hr>");
      outFile.println("</div>");
      outFile.println("<div class='abs mainContent' style='overflow:auto'>");
      outFile.println("<table style='width:100%'>");
      outFile.println("<thead>");
      outFile.println("<th class='barlabel'>Size</th>");
      outFile.println("<th class='barlabel'>Percentage</th>");
      outFile.println("<th class='barlabel'></th>");
      outFile.println("<th style='width:100%' class='barlabel'></th>");
      outFile.println("</thead>");

      for (Float size : sortedClasses.keySet()) {
        String className = sortedClasses.get(size);
        float ratio = (size / maxSize) * 85;
        if (ratio < 1) {
          ratio = 1;
        }
        float perc = (size / sumSize) * 100;

        String dependencyLink = depLinker.dependencyLinkForClass(className);
        outFile.println("<tr>");
        outFile.println("<td class=\"barlabel\">" + size + "</td>");
        outFile.println("<td class=\"barlabel\">" + formatNumber(perc)
            + "%</td>");
        if (dependencyLink != null) {
          outFile.println("<td class=\"barlabel\"><a href=\"" + dependencyLink
              + "\" target=\"_top\">" + className + "</a></td>");
        } else {
          outFile.println("<td class=\"barlabel\">" + className + "</td>");
        }
        outFile.println("<td class=\"box\">");
        outFile.println("  <div style=\"width:" + ratio
            + "%;\" class=\"sizebar\"/>");
        outFile.println("</td>");
        outFile.println("</tr>");
      }
      outFile.println("</table>");
      outFile.println("</div>");
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

    
    addStandardHtmlProlog(outFile, "Compile report", "Permutation "
        + permutationId);

    if (globalInformation.getSplitPointToLocation().size() > 1) {
      outFile.println("<b>Initial download size: <span style=\"color:maroon\">"
          + globalInformation.getInitialCodeBreakdown().sizeAllCode
          + "</span></span></b>");
    }
    outFile.println("<b>Full code size: <span style=\"color:maroon\">"
        + globalInformation.getTotalCodeBreakdown().sizeAllCode
        + "</span></span></b>");

    outFile.println("<hr>");
    outFile.println("</div>");

    outFile.println("<div class='abs mainContent' style=\"overflow: auto\" >");
    outFile.println("<table style='width:100%'>");
    outFile.println("<thead>");
    outFile.println("<th class='barlabel'>Size</th>");
    outFile.println("<th class='barlabel'>Percentage</th>");
    outFile.println("<th class='barlabel'></th>");
    outFile.println("<th style='width:100%' class='barlabel'></th>");
    outFile.println("</thead>");

    int numSplitPoints = globalInformation.getSplitPointToLocation().size();

    int numRows = 2;
    if (numSplitPoints > 0) {
      // add one for the leftovers fragment
      numRows += numSplitPoints + 1;
    }
    int outerHeight = 25 * numRows;
    outFile.println("<div style=\"width:100%; margin:20px 0 20px 0; background-color:white;position:relative;height:"
        + outerHeight + "\">");
    float maxSize = globalInformation.getTotalCodeBreakdown().sizeAllCode;

    for (int i = FRAGMENT_NUMBER_TOTAL_PROGRAM; i <= numSplitPoints + 1; i++) {
      if (i == 1 && numSplitPoints == 0) {
        // don't show the leftovers fragment if the split points aren't known
        continue;
      }

      SizeBreakdown breakdown;
      if (i == FRAGMENT_NUMBER_TOTAL_PROGRAM) {
        breakdown = globalInformation.getTotalCodeBreakdown();
      } else if (i == numSplitPoints + 1) {
        breakdown = globalInformation.getLeftoversBreakdown();
      } else if (i == FRAGMENT_NUMBER_INITIAL_DOWNLOAD) {
        breakdown = globalInformation.getInitialCodeBreakdown();
      } else {
        breakdown = globalInformation.splitPointCodeBreakdown(i);
      }

      String drillDownFileName = shellFileName(breakdown, getPermutationId());
      String splitPointDescription = breakdown.getDescription();

      float size = breakdown.sizeAllCode;
      float ratio;
      if (globalInformation.getInitialCodeBreakdown().sizeAllCode > 0) {
        ratio = (size / globalInformation.getInitialCodeBreakdown().sizeAllCode) * 79;
      } else {
        ratio = (size / maxSize) * 79;
      }
      if (ratio < 1) {
        ratio = 1;
      }
      float perc = (size / maxSize) * 100;

      outFile.println("<tr>");
      outFile.println("<td class=\"barlabel\">" + size + "</td>");
      outFile.println("<td class=\"barlabel\">" + formatNumber(perc) + "%</td>");
      outFile.println("<td class=\"barlabel\"><a href=\"" + drillDownFileName
          + "\" target=\"_top\">" + splitPointDescription + "</a></td>");
      outFile.println("<td class=\"box\">");
      if (splitPointDescription.compareTo("Total program") != 0) {
        outFile.println("<div style=\"width:" + ratio
            + "%;\" class=\"sizebar\"/>");
      }
      outFile.println("</td>");
      outFile.println("</tr>");
    }
    outFile.println("</div>");
    outFile.println("</body></html>");
    outFile.close();
  }

  private void addDependenciesHtmlProlog(final PrintWriter out, String title,
      String header) {
    addStandardHtmlProlog(out, title, header);
  }

  private void addLefttoversStatus(String className, String packageName,
      PrintWriter out) {
    if (globalInformation.dependencies != null) {
      out.println("<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;<a href=\""
          + dependenciesFileName("total", packageName) + "#" + className
          + "\">See why it's live</a></td></tr>");
      for (int sp = 1; sp <= globalInformation.getNumSplitPoints(); sp++) {
        out.println("<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;<a href=\""
            + dependenciesFileName("sp" + sp, packageName) + "#" + className
            + "\">See why it's not exclusive to s.p. #" + sp + " ("
            + globalInformation.getSplitPointToLocation().get(sp)
            + ")</a></td></tr>");
      }
    }
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
   * @return
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
        + "_codeTypeBreakdown.html";
    float maxSize = 0f;
    float sumSize = 0f;
    TreeMap<Float, String> sortedCodeTypes = new TreeMap<Float, String>(
        Collections.reverseOrder());

    for (String codeType : nameToCodeColl.keySet()) {
      float curSize = nameToCodeColl.get(codeType).getCumPartialSize(breakdown);
      sumSize += curSize;

      if (curSize != 0f) {
        sortedCodeTypes.put(curSize, codeType);
        if (curSize > maxSize) {
          maxSize = curSize;
        }
      }
    }

    final PrintWriter outFile = new PrintWriter(getOutFile(outFileName));

    outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
    outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
    outFile.println("<html>");
    outFile.println("<head>");
    outFile.println("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
    outFile.println("<link rel=\"stylesheet\" href=\"soycStyling.css\" media=\"screen\">");
    outFile.println("</head>");

    outFile.println("<body>");
    outFile.println("<table style='width:100%'>");
    outFile.println("<thead>");
    outFile.println("<th class='barlabel'>Size</th>");

    outFile.println("<th class='barlabel'>Percentage</th>");
    outFile.println("<th class='barlabel'></th>");
    outFile.println("<th style='width:100%' class='barlabel'></th>");
    outFile.println("</thead>");

    for (Float size : sortedCodeTypes.keySet()) {

      String codeType = sortedCodeTypes.get(size);
      String drillDownFileName = breakdown.getId() + "_" + codeType + "-"
          + getPermutationId() + "Classes.html";

      float ratio = (size / maxSize) * 79;
      float perc = (size / sumSize) * 100;

      if (ratio < 5) {
        ratio = 5;
      }

      outFile.println("<tr>");
      outFile.println("<td class=\"barlabel\">" + size + "</td>");
      outFile.println("<td class=\"barlabel\">" + formatNumber(perc) + "%</td>");
      outFile.println("<td class=\"barlabel\"><a href=\"" + drillDownFileName
          + "\" target=\"_top\">" + codeType + "</a></td>");
      outFile.println("<td class=\"box\">");
      outFile.println("<div style=\"width:" + ratio
          + "%;\" class=\"sizebar\"/>");
      outFile.println("</td>");
      outFile.println("</tr>");
    }

    maxSize = 0f;
    sumSize = 0f;
    TreeMap<Float, String> sortedLitTypes = new TreeMap<Float, String>(
        Collections.reverseOrder());

    for (String literal : nameToLitColl.keySet()) {
      float curSize = nameToLitColl.get(literal).size;
      sumSize += curSize;

      if (curSize != 0f) {
        sortedLitTypes.put(curSize, literal);

        if (curSize > maxSize) {
          maxSize = curSize;
        }
      }
    }

    for (Float size : sortedLitTypes.keySet()) {
      String literal = sortedLitTypes.get(size);
      String drillDownFileName = breakdown.getId() + "_" + literal + "-"
          + getPermutationId() + "Lits.html";

      float ratio = (size / maxSize) * 79;
      float perc = (size / sumSize) * 100;

      if (ratio < 1) {
        ratio = 1;
      }

      outFile.println("<tr>");
      outFile.println("<td class=\"barlabel\">" + size + "</td>");
      outFile.println("<td class=\"barlabel\">" + formatNumber(perc) + "%</td>");
      outFile.println("<td class=\"barlabel\"><a href=\"" + drillDownFileName
          + "\" target=\"_top\">" + literal + "</a></td>");
      outFile.println("<td class=\"box\">");
      outFile.println("<div style=\"width:"
          + ratio
          + "%;\" class=\"lb\"><div class=\"rb\"><div class=\"bb\"><div class=\"blc\"><div class=\"brc\"><div class=\"tb\"><div class=\"tlc\"><div class=\"trc\"><div class=\"content\"></div></div></div></div></div></div></div></div>");
      outFile.println("</td>");
      outFile.println("</tr>");
    }

    outFile.println("</table>");
    outFile.println("</body>");
    outFile.println("</html>");
    outFile.close();

    return outFileName;
  }

  /**
   * Produces an HTML file that displays dependencies.
   * 
   * @param name of dependency graph
   * @param map of dependencies
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
        addDependenciesHtmlProlog(outFile, "Method Dependencies for "
            + depGraphDescription, "Showing Package: " + packageDescription);
      }
      String name = method;
      if (curClassName.compareTo(className) != 0) {
        name = className;
        curClassName = className;
        outFile.println("<h3><a name=\"" + name + "\"/>Class: " + curClassName + "</h3>");
      }

      outFile.println("<div class='main'>");
      outFile.println("<a class='toggle' onclick='toggle.call(this)'> " +
          "<span class='calledBy'> Call stack: </span>"
          + name + "</a>");
      outFile.println("<ul>");

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
    PrintWriter out = new PrintWriter(
        getOutFile(leftoversStatusFileName(className)));

    addStandardHtmlProlog(out, "Leftovers page for " + className, null);

    out.println("<center>");
    out.println("<table border=\"1\" width=\"80%\" style=\"font-size: 11pt;\" bgcolor=\"white\">");

    out.println("<tr><td>This class has some leftover code, neither initial nor exclusive to any split point:</td></tr>");
    addLefttoversStatus(className, packageName, out);
    out.println("</table>");

    addStandardHtmlEnding(out);

    out.close();
  }

  /**
   * Produces an HTML file that shows information about a package.
   * 
   * @param breakdown
   * @return the name of the HTML file
   * @throws IOException
   */
  private String makePackageHtml(SizeBreakdown breakdown) throws IOException {
    String outFileName = breakdown.getId() + "-" + getPermutationId() + "_"
        + "packageBreakdown.html";
    Map<String, Integer> packageToPartialSize = breakdown.packageToSize;
    TreeMap<Integer, String> sortedPackages = new TreeMap<Integer, String>(
        Collections.reverseOrder());
    float maxSize = 0f;
    float sumSize = 0f;
    for (String packageName : packageToPartialSize.keySet()) {
      sortedPackages.put(packageToPartialSize.get(packageName), packageName);
      sumSize += packageToPartialSize.get(packageName);
      if (packageToPartialSize.get(packageName) > maxSize) {
        maxSize = packageToPartialSize.get(packageName);
      }
    }

    final PrintWriter outFile = new PrintWriter(getOutFile(outFileName));

    outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
    outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
    outFile.println("<html>");
    outFile.println("<head>");
    outFile.println("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
    outFile.println("<link rel=\"stylesheet\" href=\"soycStyling.css\" media=\"screen\">");
    outFile.println("</head>");
    outFile.println("<body>");
    outFile.println("<table style='width:100%'>");
    outFile.println("<thead>");
    outFile.println("<th class='barlabel'>Size</th>");

    outFile.println("<th class='barlabel'>Percentage</th>");
    outFile.println("<th class='barlabel'>Package</th>");
    outFile.println("<th style='width:100%' class='barlabel'></th>");
    outFile.println("</thead>");

    for (int size : sortedPackages.keySet()) {
      String packageName = sortedPackages.get(size);
      String drillDownFileName = classesInPackageFileName(breakdown,
          packageName, getPermutationId());

      float ratio = (size / maxSize) * 79;
      if (ratio < 1) {
        ratio = 1;
      }
      float perc = (size / sumSize) * 100;

      outFile.println("<tr>");
      outFile.println("<td class='barlabel'>" + size + "</td>");
      outFile.println("<td class='barlabel'>" + formatNumber(perc) + "%</td>");
      outFile.println("<td class='barlabel'><a href=\"" + drillDownFileName
          + "\" target=\"_top\">" + packageName + "</a></td>");
      outFile.println("<td class=\"box\">");
      outFile.println("<div style=\"width:"
          + ratio
          + "%;\" class=\"sizebar\"/>");
      outFile.println("</td>");
      outFile.println("</tr>");
    }

    outFile.println("</table>");
    outFile.println("</body>");
    outFile.println("</html>");
    outFile.close();

    return outFileName;
  }

  private void makeSplitStatusPage(String className) throws IOException {
    String packageName = globalInformation.getClassToPackage().get(className);
    PrintWriter out = new PrintWriter(
        getOutFile(splitStatusFileName(className)));

    addStandardHtmlProlog(out, "Split point status for " + className, "");

    out.println("<center>");
    out.println("<table border=\"1\" width=\"80%\" style=\"font-size: 11pt;\" bgcolor=\"white\">");

    if (globalInformation.getInitialCodeBreakdown().classToSize.containsKey(className)) {
      if (globalInformation.dependencies != null) {
        out.println("<tr><td>Some code is initial (<a href=\""
            + dependenciesFileName("initial", packageName) + "#" + className
            + "\">see why</a>)</td></tr>");
      } else {
        out.println("<tr><td>Some code is initial</td></tr>");
      }
    }
    for (int sp : splitPointsWithClass(className)) {
      out.println("<tr><td>Some code downloads with s.p. #" + sp + " ("
          + globalInformation.getSplitPointToLocation().get(sp) + ")</td></tr>");
    }
    if (globalInformation.getLeftoversBreakdown().classToSize.containsKey(className)) {
      out.println("<tr><td>Some code is left over:</td></tr>");
      addLefttoversStatus(className, packageName, out);
    }
    out.println("</table>");

    addStandardHtmlEnding(out);

    out.close();
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
