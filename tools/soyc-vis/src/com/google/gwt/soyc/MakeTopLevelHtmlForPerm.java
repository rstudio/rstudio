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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility to make the top level HTTML file for one permutation.
 */
public class MakeTopLevelHtmlForPerm {
  public class DependencyLinkerForExclusiveFragment implements DependencyLinker {
    public String dependencyLinkForClass(String className, String permutationId) {
      return null;
    }
  }

  public class DependencyLinkerForInitialCode implements DependencyLinker {
    public String dependencyLinkForClass(String className, String permutationId) {
      String packageName = globalInformation.getClassToPackage().get(className);
      assert packageName != null;
      return dependenciesFileName("initial", packageName, permutationId) + "#"
          + className;
    }
  }

  public class DependencyLinkerForLeftoversFragment implements DependencyLinker {
    public String dependencyLinkForClass(String className, String permutationId) {
      return leftoversStatusFileName(className, permutationId);
    }
  }

  public class DependencyLinkerForTotalBreakdown implements DependencyLinker {
    public String dependencyLinkForClass(String className, String permutationId) {
      return splitStatusFileName(className, permutationId);
    }
  }

  interface DependencyLinker {
    String dependencyLinkForClass(String className, String permutationId);
  }

  /**
   * Default constructor. Will be used for all permutations.
   */
  MakeTopLevelHtmlForPerm() {
    this.globalInformation = new GlobalInformation();
    this.settings = new Settings();
  }

  /**
   * Constructor for a specific permutation.
   * 
   * @param globalInformation All the information about this permutation
   */
  MakeTopLevelHtmlForPerm(final GlobalInformation globalInformation) {
    this.globalInformation = globalInformation;
  }

  /**
   * Global information for this permutation.
   */
  private GlobalInformation globalInformation = new GlobalInformation();

  /**
   * Settings for this permutation.
   */
  private Settings settings = new Settings();

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

  private static String RESOURCES_PATH = MakeTopLevelHtmlForPerm.class.getPackage().getName().replace(
      '.', '/')
      + "/resources/";

  public void copyFileOrDirectory(File srcPath, File dstPath, String classPath,
      String inputFileName, boolean isDirectory) throws IOException {
    if (srcPath.isDirectory()) {
      if (!dstPath.exists()) {
        dstPath.mkdir();
      }
      String files[] = srcPath.list();
      for (int i = 0; i < files.length; i++) {
        copyFileOrDirectory(new File(srcPath, files[i]), new File(dstPath,
            files[i]), classPath, inputFileName, isDirectory);
      }
    } else {
      if (!srcPath.exists()) {
        copyFileOrDirectoryFromJar(classPath, inputFileName, dstPath,
            isDirectory);
      } else {
        InputStream in = new FileInputStream(srcPath);
        OutputStream out = new FileOutputStream(dstPath);
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }
        in.close();
        out.close();
      }
    }
  }

  public void copyFileOrDirectoryFromJar(String jarFileName,
      String inputFileName, File dstPath, boolean isDirectory)
      throws IOException {

    JarFile jarFile = new JarFile(jarFileName);
    if (isDirectory) {
      dstPath.mkdir();

      JarInputStream jarFileIS = new JarInputStream(new FileInputStream(
          jarFileName));
      JarEntry jarEntry = jarFileIS.getNextJarEntry();
      while (jarEntry != null) {
        if (!inputFileName.endsWith("/")) {
          inputFileName += "/";
        }
        if ((jarEntry.getName().compareTo(inputFileName) != 0)
            && (jarEntry.getName().startsWith(inputFileName))) {
          File newDstPath = getOutFile(jarEntry.getName());
          copyFileOrDirectoryFromJar(jarFileName, jarEntry.getName(),
              newDstPath, false);
        }
        jarEntry = jarFileIS.getNextJarEntry();
      }
      jarFileIS.close();
    } else {
      InputStream in = jarFile.getInputStream(jarFile.getEntry(inputFileName));
      OutputStream out = new FileOutputStream(dstPath);

      int c;
      while ((c = in.read()) != -1) {
        out.write(c);
      }
      in.close();
      out.close();
      jarFile.close();
    }
  }

  public static String escapeXml(String unescaped) {
    String escaped = unescaped.replaceAll("\\&", "&amp;");
    escaped = escaped.replaceAll("\\<", "&lt;");
    escaped = escaped.replaceAll("\\>", "&gt;");
    escaped = escaped.replaceAll("\\\"", "&quot;");
    escaped = escaped.replaceAll("\\'", "&apos;");
    return escaped;
  }

  private static void addCenteredHeader(final PrintWriter outFile, String header) {
    outFile.println("<hr>");
    outFile.println("<b>" + header + "</b>");
    outFile.println("<hr>");
  }

  /**
   * Adds a header line indicating which breakdown is being analyzed.
   */
  private static void addHeaderWithBreakdownContext(SizeBreakdown breakdown,
      final PrintWriter outFile) {
    addCenteredHeader(outFile, headerLineForBreakdown(breakdown));
  }

  private static String classesInPackageFileName(SizeBreakdown breakdown,
      String packageName, String permutationId) {
    return breakdown.getId() + "_" + packageName + "-" + permutationId
        + "_Classes.html";
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

  /**
   * Return a {@link File} object for a file to be emitted into the output
   * directory.
   */
  private File getOutFile(String localFileName) {
    File outDir = new File(settings.out.get());
    return new File(outDir, localFileName);
  }

  private static String headerLineForBreakdown(SizeBreakdown breakdown) {
    return "(Analyzing code subset: " + breakdown.getDescription() + ")";
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

  private boolean isInitialSplitPoint(int splitPoint) {
    return globalInformation.getSplitPointInitialLoadSequence().contains(
        splitPoint);
  }

  private String makeCodeTypeHtml(SizeBreakdown breakdown,
      Map<String, CodeCollection> nameToCodeColl,
      Map<String, LiteralsCollection> nameToLitColl, String permutationId)
      throws IOException {
    String outFileName = breakdown.getId() + "-" + permutationId
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
    outFile.println("<link rel=\"stylesheet\" href=\"common.css\" media=\"screen\">");
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
          + permutationId + "Classes.html";

      float ratio = (size / maxSize) * 79;
      float perc = (size / sumSize) * 100;

      if (ratio < 5) {
        ratio = 5;
      }

      outFile.println("<tr>");
      outFile.println("<td class=\"barlabel\">" + size + "</td>");
      outFile.println("<td class=\"barlabel\">" + perc + "%</td>");
      outFile.println("<td class=\"barlabel\"><a href=\"" + drillDownFileName
          + "\" target=\"_top\">" + codeType + "</a></td>");
      outFile.println("<td class=\"box\">");
      outFile.println("<div style=\"width:"
          + ratio
          + "%;\" class=\"lb\"><div class=\"rb\"><div class=\"bb\"><div class=\"blc\"><div class=\"brc\"><div class=\"tb\"><div class=\"tlc\"><div class=\"trc\"><div class=\"content\"></div></div></div></div></div></div></div></div>");
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
          + permutationId + "Lits.html";

      float ratio = (size / maxSize) * 79;
      float perc = (size / sumSize) * 100;

      if (ratio < 5) {
        ratio = 5;
      }

      outFile.println("<tr>");
      outFile.println("<td class=\"barlabel\">" + size + "</td>");
      outFile.println("<td class=\"barlabel\">" + perc + "%</td>");
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

  private static String shellFileName(SizeBreakdown breakdown,
      String permutationId) {
    return breakdown.getId() + "-" + permutationId + "-overallBreakdown.html";
  }

  public void makeBreakdownShell(SizeBreakdown breakdown, String permutationId)
      throws IOException {
    // this will contain the place holder iframes where the actual information
    // is going to go.

    Map<String, CodeCollection> nameToCodeColl = breakdown.nameToCodeColl;
    Map<String, LiteralsCollection> nameToLitColl = breakdown.nameToLitColl;

    // copy from the bin directory to the current directory
    String classPath = settings.resources.get();
    if (classPath == null) {
      classPath = System.getProperty("java.class.path");
    }
    if (!classPath.endsWith("/")) {
      classPath += "/";
    }
    String inputFileName = "roundedCorners.css";
    File inputFile = new File(classPath + RESOURCES_PATH + inputFileName);
    File outputFile = getOutFile("roundedCorners.css");
    copyFileOrDirectory(inputFile, outputFile, classPath, RESOURCES_PATH + inputFileName, false);

    inputFileName = "classLevel.css";
    File inputFile2 = new File(classPath + RESOURCES_PATH + inputFileName);
    File outputFile2 = getOutFile("classLevel.css");
    copyFileOrDirectory(inputFile2, outputFile2, classPath, RESOURCES_PATH + inputFileName,
        false);

    inputFileName = "common.css";
    File inputFile3 = new File(classPath + RESOURCES_PATH + inputFileName);
    File outputFile3 = getOutFile("common.css");
    copyFileOrDirectory(inputFile3, outputFile3, classPath, RESOURCES_PATH + inputFileName,
        false);

    inputFileName = "images";
    File inputDir = new File(classPath + RESOURCES_PATH + "images");
    File outputDir = getOutFile("images");
    copyFileOrDirectory(inputDir, outputDir, classPath, inputFileName, true);

    final PrintWriter outFile = new PrintWriter(getOutFile(shellFileName(
        breakdown, permutationId)));

    outFile.println("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML//EN\">");
    outFile.println("<html>");
    outFile.println("<head>");
    outFile.println("  <title>Story of Your Compile - Top Level Dashboard for Permutation</title>");
    outFile.println("  <link rel=\"stylesheet\" href=\"common.css\">");
    outFile.println("  <style type=\"text/css\">");
    outFile.println(" body {");
    outFile.println("   background-color: #728FCE;");
    outFile.println(" }");

    outFile.println(".abs {");
    outFile.println("  position: absolute;");
    outFile.println(" overflow: hidden;");
    outFile.println(" }");

    outFile.println(" .mainHeader {");
    outFile.println("   left:0; right: 0;");
    outFile.println("   top:0; height: 6em;");
    outFile.println("  text-align: center;");
    outFile.println("}");

    outFile.println(".mainContent {");
    outFile.println("  left:0; right: 0;");
    outFile.println("  top: 6em; bottom: 0;");
    outFile.println("}");

    outFile.println(".header {");
    outFile.println("  left:0; right: 0;");
    outFile.println(" top:0; height: 5em;");
    outFile.println("  padding: 0.5em;");
    outFile.println("}");

    outFile.println(".innerContent {");
    outFile.println("   left:0; right: 0;");
    outFile.println("  top: 2em; bottom: 0;");
    outFile.println("}");

    outFile.println(".frame {");
    outFile.println(" width: 100%; height: 100%;");
    outFile.println("  border: 0px;");
    outFile.println("}");

    outFile.println(".packages {");
    outFile.println(" left:0; right: 0;");
    outFile.println(" top:0; bottom: 50%;");
    outFile.println("}");

    outFile.println(".codeType {");
    outFile.println("left:0; right: 0;");
    outFile.println("top:50%; bottom: 0;");
    outFile.println("}");
    outFile.println("</style>");
    outFile.println("</head>");

    outFile.println("<body>");
    outFile.println("<div class='abs mainHeader'>");

    outFile.println("<center>");
    outFile.println("<h3>Story of Your Compile Dashboard</h3>");

    addHeaderWithBreakdownContext(breakdown, outFile);

    outFile.println("</center>");

    outFile.println("<hr>");
    outFile.println("</div>");

    outFile.println("<div class='abs mainContent' style='overflow:auto'>");
    outFile.println("<div class='abs packages'>");

    outFile.println("<div class='abs header'>Package breakdown</div>");
    outFile.println("<div class='abs innerContent'>");
    String packageBreakdownFileName = makePackageHtml(breakdown, permutationId);
    outFile.println(" <iframe class='frame' src=\"" + packageBreakdownFileName
        + "\" scrolling=auto></iframe>");
    outFile.println("</div>");
    outFile.println("</div>");

    outFile.println("<div class='abs codeType'>");
    outFile.println("<div class='abs header'>Code type breakdown</div>");

    outFile.println("<div class='abs innerContent'>");
    String codeTypeBreakdownFileName = makeCodeTypeHtml(breakdown,
        nameToCodeColl, nameToLitColl, permutationId);
    outFile.println("<iframe class='frame' src=\"" + codeTypeBreakdownFileName
        + "\" scrolling=auto></iframe>");
    outFile.println("</div>");
    outFile.println("</div>");
    outFile.println("</div>");
    outFile.println("</body>");
    outFile.println("</html>");
    outFile.close();
  }

  public void makeCodeTypeClassesHtmls(SizeBreakdown breakdown,
      String permutationId) throws IOException {
    HashMap<String, CodeCollection> nameToCodeColl = breakdown.nameToCodeColl;

    for (String codeType : nameToCodeColl.keySet()) {

      // construct file name
      String outFileName = breakdown.getId() + "_" + codeType + "-"
          + permutationId + "Classes.html";

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

      outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
      outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
      outFile.println("<html>");
      outFile.println("<head>");
      outFile.println("<title>Classes in package \"" + codeType + "\"</title>");
      outFile.println("  <meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
      outFile.println("  <link rel=\"stylesheet\" href=\"common.css\" media=\"screen\">");

      outFile.println("  <style type=\"text/css\">");

      outFile.println(".abs {");
      outFile.println("  position: absolute;");
      outFile.println(" overflow: hidden;");
      outFile.println(" }");

      outFile.println(" .mainHeader {");
      outFile.println("   left:0; right: 0;");
      outFile.println("   top:0; height: 6em;");
      outFile.println("  text-align: center;");
      outFile.println("background-color: #728FCE;");
      outFile.println("}");

      outFile.println(".mainContent {");
      outFile.println("  left:0; right: 0;");
      outFile.println("  top: 6em; bottom: 0;");
      outFile.println("}");

      outFile.println(".header {");
      outFile.println("  left:0; right: 0;");
      outFile.println(" top:0; height: 2em;");
      outFile.println("  padding: 0.5em;");
      outFile.println("}");

      outFile.println(".innerContent {");
      outFile.println("   left:0; right: 0;");
      outFile.println("  top: 2em; bottom: 0;");
      outFile.println("}");

      outFile.println(".frame {");
      outFile.println(" width: 100%; height: 100%;");
      outFile.println("  border: 0px;");
      outFile.println("}");

      outFile.println(".packages {");
      outFile.println(" left:0; right: 0;");
      outFile.println(" top:0; bottom: 50%;");
      outFile.println("}");

      outFile.println(".codeType {");
      outFile.println("left:0; right: 0;");
      outFile.println("top:50%; bottom: 0;");
      outFile.println("}");
      outFile.println("</style>");
      outFile.println("</head>");

      outFile.println("<body>");
      outFile.println("<div class='abs mainHeader'>");
      outFile.println("<h2>Classes in package \"" + codeType + "\"</h2>");
      addHeaderWithBreakdownContext(breakdown, outFile);
      outFile.println("</center>");
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

        if (ratio < 5) {
          ratio = 5;
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

  public void makeDependenciesHtml(
      Map<String, Map<String, String>> allDependencies, String permutationId)
      throws IOException {
    for (String depGraphName : allDependencies.keySet()) {
      makeDependenciesHtml(depGraphName, allDependencies.get(depGraphName),
          permutationId);
    }
  }

  public void makeLeftoverStatusPages(String permutationId) throws IOException {
    for (String className : globalInformation.getClassToPackage().keySet()) {
      makeLeftoversStatusPage(className, permutationId);
    }
  }

  public void makeLiteralsClassesTableHtmls(SizeBreakdown breakdown,
      String permutationId) throws IOException {
    Map<String, LiteralsCollection> nameToLitColl = breakdown.nameToLitColl;

    for (String literalType : nameToLitColl.keySet()) {

      String outFileName = literalType + "-" + permutationId + "Lits.html";
      final PrintWriter outFile = new PrintWriter(getOutFile(breakdown.getId()
          + "_" + outFileName));

      outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
      outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
      outFile.println("<html>");
      outFile.println("<head>");
      outFile.println("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
      outFile.println("<title>Literals of type \"" + literalType + "\"</title>");
      outFile.println("</head>");

      outFile.println("<style type=\"text/css\">");
      outFile.println("body {background-color: #728FCE}");
      outFile.println("h2 {background-color: transparent}");
      outFile.println("p {background-color: fuchsia}");
      outFile.println("</style>");

      outFile.println("<body>");
      outFile.println("<center>");
      outFile.println("<h2>Literals of type \"" + literalType + "\"</h2>");
      addHeaderWithBreakdownContext(breakdown, outFile);
      outFile.println("</center>");

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
      DependencyLinker depLinker, String permutationId) throws IOException {
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
              permutationId)));

      outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
      outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
      outFile.println("<html>");
      outFile.println("<head>");
      outFile.println("<title>Classes in package \"" + packageName
          + "\"</title>");
      outFile.println("  <meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
      outFile.println("  <link rel=\"stylesheet\" href=\"common.css\" media=\"screen\">");

      outFile.println("  <style type=\"text/css\">");

      outFile.println(".abs {");
      outFile.println("  position: absolute;");
      outFile.println(" overflow: hidden;");
      outFile.println(" }");

      outFile.println(" .mainHeader {");
      outFile.println("   left:0; right: 0;");
      outFile.println("   top:0; height: 6em;");
      outFile.println("  text-align: center;");
      outFile.println("background-color: #728FCE;");
      outFile.println("}");

      outFile.println(".mainContent {");
      outFile.println("  left:0; right: 0;");
      outFile.println("  top: 6em; bottom: 0;");
      outFile.println("}");

      outFile.println(".header {");
      outFile.println("  left:0; right: 0;");
      outFile.println(" top:0; height: 2em;");
      outFile.println("  padding: 0.5em;");
      outFile.println("}");

      outFile.println(".innerContent {");
      outFile.println("   left:0; right: 0;");
      outFile.println("  top: 2em; bottom: 0;");
      outFile.println("}");

      outFile.println(".frame {");
      outFile.println(" width: 100%; height: 100%;");
      outFile.println("  border: 0px;");
      outFile.println("}");

      outFile.println(".packages {");
      outFile.println(" left:0; right: 0;");
      outFile.println(" top:0; bottom: 50%;");
      outFile.println("}");

      outFile.println(".codeType {");
      outFile.println("left:0; right: 0;");
      outFile.println("top:50%; bottom: 0;");
      outFile.println("}");
      outFile.println("</style>");
      outFile.println("</head>");

      outFile.println("<body>");
      outFile.println("<div class='abs mainHeader'>");
      outFile.println("<h2>Classes in package \"" + packageName + "\"</h2>");
      addHeaderWithBreakdownContext(breakdown, outFile);
      outFile.println("</center>");
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
        if (ratio < 5) {
          ratio = 5;
        }
        float perc = (size / sumSize) * 100;

        String dependencyLink = depLinker.dependencyLinkForClass(className,
            permutationId);
        outFile.println("<tr>");
        outFile.println("<td class=\"barlabel\">" + size + "</td>");
        outFile.println("<td class=\"barlabel\">" + perc + "%</td>");
        if (dependencyLink != null) {
          outFile.println("<td class=\"barlabel\"><a href=\"" + dependencyLink
              + "\" target=\"_top\">" + className + "</a></td>");
        } else {
          outFile.println("<td class=\"barlabel\">" + className + "</td>");
        }
        outFile.println("<td class=\"box\">");
        outFile.println("  <div style=\"width:"
            + ratio
            + "%;\" class=\"lb\"><div class=\"rb\"><div class=\"bb\"><div class=\"blc\"><div class=\"brc\"><div class=\"tb\"><div class=\"tlc\"><div class=\"trc\"><div class=\"content\"></div></div></div></div></div></div></div></div>");
        outFile.println("</td>");
        outFile.println("</tr>");

      }
      outFile.println("</table>");
      outFile.println("</div>");
      addStandardHtmlEnding(outFile);
      outFile.close();
    }
  }

  public void makeSplitStatusPages(String permutationId) throws IOException {
    for (String className : globalInformation.getClassToPackage().keySet()) {
      makeSplitStatusPage(className, permutationId);
    }
  }

  public void makeTopLevelShell(String permutationId) throws IOException {
    PrintWriter outFile = new PrintWriter(getOutFile("SoycDashboard" + "-"
        + permutationId + "-index.html"));

    outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
    outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
    outFile.println("<html>");
    outFile.println("<head>");
    outFile.println("<title>Story of Your Compile - Top Level Dashboard for Permutation</title>");
    outFile.println("  <meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
    outFile.println("  <link rel=\"stylesheet\" href=\"common.css\" media=\"screen\">");

    outFile.println("  <style type=\"text/css\">");

    outFile.println(".abs {");
    outFile.println("  position: absolute;");
    outFile.println(" overflow: hidden;");
    outFile.println(" }");

    outFile.println(" .mainHeader {");
    outFile.println("   left:0; right: 0;");
    outFile.println("   top:0; height: 6em;");
    outFile.println("  text-align: center;");
    outFile.println("background-color: #728FCE;");
    outFile.println("}");

    outFile.println(".mainContent {");
    outFile.println("  left:0; right: 0;");
    outFile.println("  top: 6em; bottom: 0;");
    outFile.println("}");

    outFile.println(".header {");
    outFile.println("  left:0; right: 0;");
    outFile.println(" top:0; height: 2em;");
    outFile.println("  padding: 0.5em;");
    outFile.println("}");

    outFile.println(".innerContent {");
    outFile.println("   left:0; right: 0;");
    outFile.println("  top: 2em; bottom: 0;");
    outFile.println("}");

    outFile.println(".frame {");
    outFile.println(" width: 100%; height: 100%;");
    outFile.println("  border: 0px;");
    outFile.println("}");

    outFile.println(".packages {");
    outFile.println(" left:0; right: 0;");
    outFile.println(" top:0; bottom: 50%;");
    outFile.println("}");

    outFile.println(".codeType {");
    outFile.println("left:0; right: 0;");
    outFile.println("top:50%; bottom: 0;");
    outFile.println("}");
    outFile.println("</style>");
    outFile.println("</head>");

    outFile.println("<body>");
    outFile.println("<div class='abs mainHeader'>");
    outFile.println("<h2>Story of Your Compile Dashboard</h2>");

    outFile.println("<hr>");
    outFile.println("<center>");
    if (globalInformation.getSplitPointToLocation().size() > 1) {
      outFile.println("<b>Initial download size: <span style=\"color:maroon\">"
          + globalInformation.getInitialCodeBreakdown().sizeAllCode
          + "</span></span></b>");
    }
    outFile.println("<b>Full code size: <span style=\"color:maroon\">"
        + globalInformation.getTotalCodeBreakdown().sizeAllCode
        + "</span></span></b>");

    outFile.println("<hr>");
    outFile.println("Available code subsets to analyze");
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

      String drillDownFileName = shellFileName(breakdown, permutationId);
      String splitPointDescription = breakdown.getDescription();

      int size = breakdown.sizeAllCode;
      float ratio;
      if (globalInformation.getInitialCodeBreakdown().sizeAllCode > 0) {
        ratio = (size / globalInformation.getInitialCodeBreakdown().sizeAllCode) * 79;
      } else {
        ratio = (size / maxSize) * 79;
      }
      if (ratio < 5) {
        ratio = 5;
      }
      float perc = (size / maxSize) * 100;

      outFile.println("<tr>");
      outFile.println("<td class=\"barlabel\">" + size + "</td>");
      outFile.println("<td class=\"barlabel\">" + perc + "%</td>");
      outFile.println("<td class=\"barlabel\"><a href=\"" + drillDownFileName
          + "\" target=\"_top\">" + splitPointDescription + "</a></td>");
      outFile.println("<td class=\"box\">");
      if (splitPointDescription.compareTo("Total program") != 0) {
        outFile.println("<div style=\"width:"
            + ratio
            + "%;\" class=\"lb\"><div class=\"rb\"><div class=\"bb\"><div class=\"blc\"><div class=\"brc\"><div class=\"tb\"><div class=\"tlc\"><div class=\"trc\"><div class=\"content\"></div></div></div></div></div></div></div></div>");
      }
      outFile.println("</td>");
      outFile.println("</tr>");
    }
    outFile.println("</div>");
    outFile.println("</body></html>");
    outFile.close();
  }

  private void addLefttoversStatus(String className, String packageName,
      PrintWriter out, String permutationId) {
    out.println("<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;<a href=\""
        + dependenciesFileName("total", packageName, permutationId) + "#"
        + className + "\">See why it's live</a></td></tr>");
    for (int sp = 1; sp <= globalInformation.getNumSplitPoints(); sp++) {
      out.println("<tr><td>&nbsp;&nbsp;&nbsp;&nbsp;<a href=\""
          + dependenciesFileName("sp" + sp, packageName, permutationId) + "#"
          + className + "\">See why it's not exclusive to s.p. #" + sp + " ("
          + globalInformation.getSplitPointToLocation().get(sp)
          + ")</a></td></tr>");
    }
  }

  private void addStandardHtmlEnding(final PrintWriter out) {
    out.println("</div>");
    out.println("</body>");
    out.println("</html>");
  }

  private void addStandardHtmlProlog(final PrintWriter out, String title,
      String header) {
    out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
    out.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
    out.println("<html>");
    out.println("<head>");
    out.println("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
    out.println("<title>" + title + "</title>");
    out.println("</head>");

    out.println("<style type=\"text/css\">");
    out.println("body {background-color: #728FCE}");
    out.println("h2 {background-color: transparent}");
    out.println("p {background-color: fuchsia}");
    out.println("</style>");

    out.println("<body>");
    out.println("<center>");
    out.println("<h2>" + title + "</h2>");
    if (header != null) {
      addCenteredHeader(out, header);
    }
    out.println("</center>");
  }

  private void addDependenciesHtmlProlog(final PrintWriter out, String title,
      String header) {
    out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
    out.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
    out.println("<html>");

    out.println("<head>");
    out.println("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
    out.println("<title>" + title + "</title>");
    out.println("<link rel=\"stylesheet\" href=\"common.css\">");
    out.println("<style type=\"text/css\">");
    out.println("body {background-color: #728FCE}");
    out.println("h2 {background-color: transparent}");
    out.println("p {background-color: fuchsia}");
    out.println(".calledBy {");
    out.println("  color: green;");
    out.println("}");
    out.println(".toggle {");
    out.println("cursor: pointer;");
    out.println("}");
    out.println(".main {");
    out.println("background-color: white;");
    out.println("padding: 8px;");
    out.println("}");
    out.println("</style>");
    out.println("<script>");
    out.println("function nextSiblingElement(a) {");
    out.println("var ul = a.nextSibling;");
    out.println("while (ul && ul.nodeType != 1) { // 1==element");
    out.println("  ul = ul.nextSibling;");
    out.println("}");
    out.println("return ul;");
    out.println("}");
    out.println("function toggle() {");
    out.println("var ul = nextSiblingElement(this);");
    out.println("if (ul) {");
    out.println(" ul.style.display = (ul.style.display == 'none') ? '' : 'none';");
    out.println("}");
    out.println("}");
    out.println("</script>");

    out.println("</head>");

    out.println("<body>");
    out.println("<center>");
    out.println("<h2>" + title + "</h2>");
    if (header != null) {
      addCenteredHeader(out, header);
    }
    out.println("</center>");

  }

  private String dependenciesFileName(String depGraphName, String packageName,
      String permutationId) {
    return "methodDependencies-" + depGraphName + "-" + filename(packageName)
        + "-" + permutationId + ".html";
  }

  private String leftoversStatusFileName(String className, String permutationId) {
    return "leftoverStatus-" + filename(className) + "-" + permutationId
        + ".html";
  }

  private void makeDependenciesHtml(String depGraphName,
      Map<String, String> dependencies, String permutationId)
      throws FileNotFoundException {
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

        String outFileName = dependenciesFileName(depGraphName, curPackageName,
            permutationId);
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
        outFile.println("<h3>Class: " + curClassName + "</h3>");
      }

      outFile.println("<div class='main'>");
      outFile.println("<a class='toggle' onclick='toggle.call(this)' name="
          + name + "><span class='calledBy'> Call stack: </span>" + name
          + "</a>");
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

  private void makeLeftoversStatusPage(String className, String permutationId)
      throws IOException {
    String packageName = globalInformation.getClassToPackage().get(className);
    PrintWriter out = new PrintWriter(getOutFile(leftoversStatusFileName(
        className, permutationId)));

    addStandardHtmlProlog(out, "Leftovers page for " + className, null);

    out.println("<center>");
    out.println("<table border=\"1\" width=\"80%\" style=\"font-size: 11pt;\" bgcolor=\"white\">");

    out.println("<tr><td>This class has some leftover code, neither initial nor exclusive to any split point:</td></tr>");
    addLefttoversStatus(className, packageName, out, permutationId);
    out.println("</table>");

    addStandardHtmlEnding(out);

    out.close();
  }

  private String makePackageHtml(SizeBreakdown breakdown, String permutationId)
      throws FileNotFoundException {
    String outFileName = breakdown.getId() + "-" + permutationId + "_"
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
    outFile.println("<link rel=\"stylesheet\" href=\"common.css\" media=\"screen\">");
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
          packageName, permutationId);

      float ratio = (size / maxSize) * 79;
      if (ratio < 5) {
        ratio = 5;
      }
      float perc = (size / sumSize) * 100;

      outFile.println("<tr>");
      outFile.println("<td class='barlabel'>" + size + "</td>");
      outFile.println("<td class='barlabel'>" + perc + "</td>");
      outFile.println("<td class='barlabel'><a href=\"" + drillDownFileName
          + "\" target=\"_top\">" + packageName + "</a></td>");
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

  private void makeSplitStatusPage(String className, String permutationId)
      throws IOException {
    String packageName = globalInformation.getClassToPackage().get(className);
    PrintWriter out = new PrintWriter(getOutFile(splitStatusFileName(className,
        permutationId)));

    addStandardHtmlProlog(out, "Split point status for " + className, null);

    out.println("<center>");
    out.println("<table border=\"1\" width=\"80%\" style=\"font-size: 11pt;\" bgcolor=\"white\">");

    if (globalInformation.getInitialCodeBreakdown().classToSize.containsKey(className)) {
      out.println("<tr><td>Some code is initial (<a href=\""
          + dependenciesFileName("initial", packageName, permutationId) + "#"
          + className + "\">see why</a>)</td></tr>");
    }
    for (int sp : splitPointsWithClass(className)) {
      out.println("<tr><td>Some code downloads with s.p. #" + sp + " ("
          + globalInformation.getSplitPointToLocation().get(sp) + ")</td></tr>");
    }
    if (globalInformation.getLeftoversBreakdown().classToSize.containsKey(className)) {
      out.println("<tr><td>Some code is left over:</td></tr>");
      addLefttoversStatus(className, packageName, out, permutationId);
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

  private String splitStatusFileName(String className, String permutationId) {
    return "splitStatus-" + filename(className) + "-" + permutationId + ".html";
  }

  public void makeTopLevelHtmlForAllPerms() throws FileNotFoundException {

    PrintWriter outFile = new PrintWriter(getOutFile("index.html"));

    outFile.println("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML//EN\">");
    outFile.println("<html>");
    outFile.println("<head>");
    outFile.println("  <title>Story of Your Compile - Top Level Dashboard for all Permutations</title>");
    outFile.println("<style type=\"text/css\">");
    outFile.println("body {background-color: #728FCE}");
    outFile.println("h2 {background-color: transparent}");
    outFile.println("</style>");
    outFile.println("</head>");

    outFile.println("<body>");
    outFile.println("<center>");
    outFile.println("<h1>Story of Your Compile</h1>");
    outFile.println("<hr>");
    outFile.println("<h3>Story of Your Compile - Overview of Permutations</h3>");
    outFile.println("<hr>");

    outFile.println("<div style='overflow:auto; background-color:white'>");
    outFile.println("<center>");
    for (String permutationId : settings.allPermsInfo.keySet()) {
      String permutationInfo = settings.allPermsInfo.get(permutationId);
      outFile.print("<p><a href=\"SoycDashboard" + "-" + permutationId
          + "-index.html\">Permutation " + permutationId);
      if (permutationInfo.length() > 0) {
        outFile.println(" (" + permutationInfo + ")" + "</a>");
      } else {
        outFile.println("</a>");
      }
    }
    outFile.println("</center>");
    outFile.println("</div>");
    addStandardHtmlEnding(outFile);
    outFile.close();
  }

  public GlobalInformation getGlobalInformation() {
    return globalInformation;
  }

  public void setGlobalInformation(GlobalInformation globalInformation) {
    this.globalInformation = globalInformation;
  }

  public void setSettings(Settings settings) {
    this.settings = settings;
  }
}
