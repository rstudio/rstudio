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
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

/**
 * A utility to make the top level HTTML file for one permutation.
 */
public class MakeTopLevelHtmlForPerm {
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

  public static void copyFileOrDirectory(File srcPath, File dstPath,
      String classPath, String inputFileName, boolean isDirectory)
      throws IOException {
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

  public static void copyFileOrDirectoryFromJar(String jarFileName,
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
          File newDstPath = new File(jarEntry.getName());
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

  public static void makeBreakdownShell(SizeBreakdown breakdown)
      throws IOException {
    // this will contain the place holder iframes where the actual information
    // is going to go.

    Map<String, CodeCollection> nameToCodeColl = breakdown.nameToCodeColl;
    Map<String, LiteralsCollection> nameToLitColl = breakdown.nameToLitColl;

    // copy from the bin directory to the current directory
    String classPath = GlobalInformation.settings.resources.get();
    if (classPath == null) {
      classPath = System.getProperty("java.class.path");
    }
    if (!classPath.endsWith("/")) {
      classPath += "/";
    }
    String inputFileName = "roundedCorners.css";
    File inputFile = new File(classPath + inputFileName);
    File outputFile = new File("roundedCorners.css");
    copyFileOrDirectory(inputFile, outputFile, classPath, inputFileName, false);

    inputFileName = "classLevel.css";
    File inputFile2 = new File(classPath + inputFileName);
    File outputFile2 = new File("classLevel.css");
    copyFileOrDirectory(inputFile2, outputFile2, classPath, inputFileName,
        false);

    inputFileName = "images";
    File inputDir = new File(classPath + "images");
    File outputDir = new File("images");
    copyFileOrDirectory(inputDir, outputDir, classPath, inputFileName, true);

    final PrintWriter outFile = new PrintWriter(shellFileName(breakdown));
    outFile.println("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML//EN\">");
    outFile.println("<html>");
    outFile.println("<head>");
    outFile.println("<title>Story of Your Compile - Top Level Dashboard for Permutation</title>");

    outFile.println("<link rel=\"stylesheet\" href=\"roundedCorners.css\">");
    outFile.println("<style type=\"text/css\">");
    outFile.println("body {background-color: #728FCE}");
    outFile.println("h2 {background-color: transparent}");
    outFile.println("p {background-color: fuchsia}");
    outFile.println("</style>");
    outFile.println("</head>");

    outFile.println("<body>");
    outFile.println("<center>");
    outFile.println("<h3>Story of Your Compile Dashboard</h3>");
    addHeaderWithBreakdownContext(breakdown, outFile);

    outFile.println("</center>");
    outFile.println("  <div style=\"width:50%;  float:left; padding-top: 10px;\">");
    outFile.println("<b>Package breakdown</b>");
    outFile.println("    </div>");
    outFile.println("  <div style=\"width:48%;  float:right; padding-top: 10px; \">");
    outFile.println("<b>Code type breakdown</b>");
    outFile.println("    </div>");

    outFile.println("  <div style=\"width:50%;  float:left; padding-top: 10px;\">");
    outFile.println("<div style=\"width: 110px; float: left; font-size:16px;\">Size</div>");
    outFile.println("<div style=\"width: 200px; float: left; text-align:left; font-size:16px; \">Package Name</div>");
    outFile.println("    </div>");

    outFile.println("  <div style=\"width:48%;  float:right; padding-top: 10px;\">");
    outFile.println("<div style=\"width: 110px; float: left; font-size:16px;\">Size</div>");
    outFile.println("<div style=\"width: 200px; float: left; text-align:left; font-size:16px; \">Code Type</div>");
    outFile.println("    </div>");

    String packageBreakdownFileName = makePackageHtml(breakdown);
    outFile.println("<div style=\"height:35%; width:48%; margin:0 auto; background-color:white; float:left;\">");
    outFile.println("<iframe src=\"" + packageBreakdownFileName
        + "\" width=100% height=100% scrolling=auto></iframe>");
    outFile.println("  </div>");

    String codeTypeBreakdownFileName = makeCodeTypeHtml(breakdown,
        nameToCodeColl);
    outFile.println("<div style=\"height:35%; width:48%; margin:0 auto; background-color:white; float:right;\">");
    outFile.println("<iframe src=\"" + codeTypeBreakdownFileName
        + "\" width=100% height=100% scrolling=auto></iframe>");
    outFile.println("  </div>");

    outFile.println("  <div style=\"width:50%;  float:left; padding-top: 10px;\">");
    outFile.println("<b>Literals breakdown</b>");
    outFile.println("    </div>");
    outFile.println("  <div style=\"width:48%;  float:right; padding-top: 10px;  \">");
    outFile.println("<b>String literals breakdown</b>");
    outFile.println("    </div>");

    outFile.println("  <div style=\"width:50%;  float:left; padding-top: 10px;\">");
    outFile.println("<div style=\"width: 110px; float: left; font-size:16px;\">Size</div>");
    outFile.println("<div style=\"width: 200px; float: left; text-align:left; font-size:16px; \">Literal Type</div>");
    outFile.println("    </div>");

    outFile.println("  <div style=\"width:48%;  float:right; padding-top: 10px; \">");
    outFile.println("<div style=\"width: 110px; float: left; font-size:16px;\">Size</div>");
    outFile.println("<div style=\"width: 200px; float: left; text-align:left; font-size:16px; \">String Literal Type</div>");
    outFile.println("    </div>");

    String literalsBreakdownFileName = makeLiteralsHtml(breakdown,
        nameToLitColl);
    outFile.println("<div style=\"height:35%; width:48%; margin:0 auto; background-color:white; float:left;\">");
    outFile.println("<iframe src=\"" + literalsBreakdownFileName
        + "\" width=100% height=100% scrolling=auto></iframe>");
    outFile.println("</div>");

    String stringLiteralsBreakdownFileName = makeStringLiteralsHtml(breakdown,
        nameToLitColl);
    outFile.println("<div style=\"height:35%; width:48%; margin:0 auto; background-color:white; float:right;\">");
    outFile.println("<iframe src=\"" + stringLiteralsBreakdownFileName
        + "\" width=100% height=100% scrolling=auto></iframe>");
    outFile.println("  </div>");

    outFile.println("  </body>");
    outFile.println("</html>");
    outFile.close();
  }

  public static void makeCodeTypeClassesHtmls(SizeBreakdown breakdown)
      throws IOException {
    HashMap<String, CodeCollection> nameToCodeColl = breakdown.nameToCodeColl;

    for (String codeType : nameToCodeColl.keySet()) {

      // construct file name
      String outFileName = breakdown.getId() + "_" + codeType + "Classes.html";

      float maxSize = 0f;
      TreeMap<Float, String> sortedClasses = new TreeMap<Float, String>(
          Collections.reverseOrder());

      for (String className : nameToCodeColl.get(codeType).classes) {
        if (breakdown.classToPartialSize.containsKey(className)) {

          float curSize = 0f;
          if (breakdown.classToPartialSize.containsKey(className)) {
            curSize = breakdown.classToPartialSize.get(className);
          }

          if (curSize != 0f) {
            sortedClasses.put(curSize, className);
            if (curSize > maxSize) {
              maxSize = curSize;
            }
          }
        }
      }

      final PrintWriter outFile = new PrintWriter(outFileName);

      outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
      outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
      outFile.println("<html>");
      outFile.println("<head>");

      outFile.println("<style type=\"text/css\">");
      outFile.println("body {background-color: #728FCE}");
      outFile.println("h2 {background-color: transparent}");
      outFile.println("p {background-color: fuchsia}");
      outFile.println("</style>");

      outFile.println("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
      outFile.println("<link rel=\"stylesheet\" href=\"classLevel.css\" media=\"screen\">");
      outFile.println("<title>Classes of type \"" + codeType + "\"</title>");
      outFile.println("</head>");
      outFile.println("<body>");

      outFile.println("<center>");
      outFile.println("<h2>Classes of type \"" + codeType + "\"</h2>");
      addHeaderWithBreakdownContext(breakdown, outFile);
      outFile.println("</center>");

      outFile.println("<div style=\"width:90%; height:80%; overflow-y:auto; overflow-x:auto; top: 120px; left:70px; position:absolute; background-color:white\"");

      int yOffset = 0;
      for (Float size : sortedClasses.keySet()) {

        String className = sortedClasses.get(size);

        float ratio = (size / maxSize) * 85;

        if (ratio < 3) {
          ratio = 3;
        }

        outFile.println("<div class=\"box\" style=\"width:" + ratio
            + "%; top: " + yOffset + "px; left: 60px;\">");
        outFile.println("<div id=\"lb\">");
        outFile.println("<div id=\"rb\">");
        outFile.println("<div id=\"bb\"><div id=\"blc\"><div id=\"brc\">");
        outFile.println("<div id=\"tb\"><div id=\"tlc\"><div id=\"trc\">");
        outFile.println("<div id=\"content\">");
        outFile.println("</div>");
        outFile.println("</div></div></div></div>");
        outFile.println("</div></div></div></div>");
        outFile.println("</div>");

        int yOffsetText = yOffset + 8;
        outFile.printf("<div class=\"barlabel\" style=\"top:" + yOffsetText
            + "px; left:5px;\">%.1f</div>\n", size);
        outFile.println("<div class=\"barlabel\" style=\"top:" + yOffsetText
            + "px; left:70px;\">" + className + "</div>");

        yOffset = yOffset + 25;
      }

      outFile.println("</div>");
      outFile.println("</body>");
      outFile.println("</html>");
      outFile.close();
    }
  }

  public static void makeLiteralsClassesTableHtmls(SizeBreakdown breakdown)
      throws IOException {
    Map<String, LiteralsCollection> nameToLitColl = breakdown.nameToLitColl;

    for (String literalType : nameToLitColl.keySet()) {

      String outFileName = literalType + "Lits.html";
      final PrintWriter outFile = new PrintWriter(breakdown.getId() + "_"
          + outFileName);

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

      for (String literal : nameToLitColl.get(literalType).literalToLocations.keySet()) {

        if (literal.trim().compareTo("") == 0) {
          literal = "[whitespace only string]";
        }

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

        String escliteral = escapeXml(newLiteral);

        outFile.println("<tr>");
        outFile.println("<td width=\"40%\">" + escliteral + "</td>");

        int ct = 0;
        if ((nameToLitColl.containsKey(literalType))
            && (nameToLitColl.get(literalType).literalToLocations.containsKey(literal))) {
          for (String location : nameToLitColl.get(literalType).literalToLocations.get(literal)) {

            if (ct > 0) {
              outFile.println("<tr>");
              outFile.println("<td width=\"40%\"> </td>");
            }

            String newLocation = "";
            if (location.length() > 80) {
              int i;
              for (i = 80; i < location.length(); i = i + 80) {
                String part1 = location.substring(i - 80, i);
                newLocation = newLocation + part1 + " ";
              }
              if (i - 80 > 0) {
                newLocation = newLocation + location.substring(i - 80);
              }
            } else {
              newLocation = location;
            }

            outFile.println("<td width=\"40%\">" + newLocation + "</td>");

            if (ct > 0) {
              outFile.println("</tr>");
            }
            ct++;
          }
        } else {
          System.err.println("either literalType " + literalType
              + " not known, or no location for literal " + literal);
        }

        outFile.println("</tr>");
      }

      outFile.println("</table>");
      outFile.println("<center>");

      outFile.println("</div>");
      outFile.println("</body>");
      outFile.println("</html>");
      outFile.close();
    }
  }

  public static void makeStringLiteralsClassesTableHtmls(SizeBreakdown breakdown)
      throws IOException {
    Map<String, LiteralsCollection> nameToLitColl = breakdown.nameToLitColl;

    for (String literalType : nameToLitColl.get("string").stringTypeToSize.keySet()) {
      String outFileName = literalType + "Strings.html";
      final PrintWriter outFile = new PrintWriter(breakdown.getId() + "_"
          + outFileName);

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

      for (String literal : nameToLitColl.get("string").stringLiteralToType.keySet()) {

        if (nameToLitColl.get("string").stringLiteralToType.get(literal).compareTo(
            literalType) == 0) {

          if (literal.trim().compareTo("") == 0) {
            literal = "[whitespace only string]";
          }

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

          String escliteral = escapeXml(newLiteral);

          outFile.println("<tr>");
          outFile.println("<td width=\"40%\">" + escliteral + "</td>");

          int ct = 0;

          if (nameToLitColl.get("string").literalToLocations.containsKey(literal)) {

            for (String location : nameToLitColl.get("string").literalToLocations.get(literal)) {

              if (ct > 0) {
                outFile.println("<tr>");
                outFile.println("<td width=\"40%\"> </td>");
              }

              String newLocation = "";
              if (location.length() > 80) {
                int i;
                for (i = 80; i < location.length(); i = i + 80) {
                  String part1 = location.substring(i - 80, i);
                  newLocation = newLocation + part1 + " ";
                }
                if (i - 80 > 0) {
                  newLocation = newLocation + location.substring(i - 80);
                }
              } else {
                newLocation = location;
              }

              outFile.println("<td width=\"40%\">" + newLocation + "</td>");

              if (ct > 0) {
                outFile.println("</tr>");
              }
              ct++;
            }
          } else {
            System.err.println("no location given for string literal: "
                + literal);
          }
          outFile.println("</tr>");
        }
      }

      outFile.println("</table>");
      outFile.println("<center>");

      outFile.println("</div>");
      outFile.println("</body>");
      outFile.println("</html>");
      outFile.close();
    }
  }

  public static void makeTopLevelShell() throws IOException {
    PrintWriter outFile = new PrintWriter("SoycDashboard-index.html");

    outFile.println("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML//EN\">");
    outFile.println("<html>");
    outFile.println("<head>");
    outFile.println("<title>Story of Your Compile - Top Level Dashboard for Permutation</title>");

    outFile.println("<link rel=\"stylesheet\" href=\"roundedCorners.css\">");
    outFile.println("<style type=\"text/css\">");
    outFile.println("body {background-color: #728FCE}");
    outFile.println("h2 {background-color: transparent}");
    outFile.println("p {background-color: fuchsia}");
    outFile.println("</style>");
    outFile.println("</head>");

    outFile.println("<body>");
    outFile.println("<center>");
    outFile.println("<h3>Story of Your Compile Dashboard</h3>");
    outFile.println("<hr>");
    if (GlobalInformation.fragmentToStories.size() > 1) {
      outFile.println("<b>Initial download size: <span style=\"color:maroon\">"
          + GlobalInformation.initialCodeBreakdown.sizeAllCode
          + "</span></span></b>");
    } else {
      outFile.println("<b>Full code size: <span style=\"color:maroon\">"
          + GlobalInformation.totalCodeBreakdown.sizeAllCode
          + "</span></span></b>");
    }

    outFile.println("<hr>");

    outFile.println("Available code subsets to analyze");

    int numSplitPoints = GlobalInformation.splitPointToLocation.size();

    int outerHeight = 25 * (numSplitPoints + 2);
    outFile.println("<div style=\"width:100%; margin:20px 0 20px 0; background-color:white;position:relative;height:"
        + outerHeight + "\">");
    float maxSize = GlobalInformation.totalCodeBreakdown.sizeAllCode;

    int yOffset = 0;
    for (int i = FRAGMENT_NUMBER_TOTAL_PROGRAM; i <= numSplitPoints; i++) {
      String drillDownFileName, splitPointDescription;
      if (i == FRAGMENT_NUMBER_TOTAL_PROGRAM) {
        drillDownFileName = shellFileName(GlobalInformation.totalCodeBreakdown);
        splitPointDescription = "Total program";
      } else {
        String splitPointName = i == FRAGMENT_NUMBER_INITIAL_DOWNLOAD
            ? "initialDownload" : GlobalInformation.splitPointToLocation.get(i);
        if (i == FRAGMENT_NUMBER_INITIAL_DOWNLOAD) {
          drillDownFileName = shellFileName(GlobalInformation.initialCodeBreakdown);
        } else {
          drillDownFileName = "splitPoint-" + filename(splitPointName)
              + "-Classes.html";
        }
        splitPointDescription = i == FRAGMENT_NUMBER_INITIAL_DOWNLOAD
            ? "Initial download" : ("Code exclusive to " + splitPointName);
      }

      float size;
      if (i >= 0) {
        size = GlobalInformation.fragmentToPartialSize.get(i);
      } else {
        size = GlobalInformation.totalCodeBreakdown.sizeAllCode;
      }
      float ratio = (size / maxSize) * 79;
      if (ratio < 3) {
        ratio = 3;
      }

      outFile.println("<div id=\"box\" style=\"width: " + ratio + "%; top: "
          + yOffset + "px; left: 110px;\">");
      outFile.println("<div id=\"lb\">");
      outFile.println("<div id=\"rb\">");
      outFile.println("<div id=\"bb\"><div id=\"blc\"><div id=\"brc\">");
      outFile.println("<div id=\"tb\"><div id=\"tlc\"><div id=\"trc\">");
      outFile.println("<div id=\"content\">");
      outFile.println("</div>");
      outFile.println("</div></div></div></div>");
      outFile.println("</div></div></div></div>");
      outFile.println("</div>");

      int yOffsetText = yOffset + 8;
      outFile.printf("<div class=\"barlabel\" style=\"top: " + yOffsetText
          + "px; left:5px;\">%.1f</div>\n", size);
      outFile.println("<div class=\"barlabel\" style=\"top: " + yOffsetText
          + "px; left:120px;\"><a href=\"" + drillDownFileName
          + "\" target=\"_top\">" + splitPointDescription + "</a></div>");

      yOffset = yOffset + 25;
    }
    outFile.println("</div>");
    outFile.println("</body></html>");
    outFile.close();
  }

  /**
   * Adds a header line indicating which breakdown is being analyzed.
   */
  private static void addHeaderWithBreakdownContext(SizeBreakdown breakdown,
      final PrintWriter outFile) {
    outFile.println("<hr>");
    outFile.println("<b>(Analyzing code subset: " + breakdown.getDescription()
        + ")</b>");
    outFile.println("<hr>");
  }

  private static String classesInPackageFileName(SizeBreakdown breakdown,
      String packageName) {
    return breakdown.getId() + "_" + packageName + "_Classes.html";
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

  private static String makeCodeTypeHtml(SizeBreakdown breakdown,
      Map<String, CodeCollection> nameToCodeColl) throws IOException {
    String outFileName = breakdown.getId() + "_codeTypeBreakdown.html";
    float maxSize = 0f;
    float sumSize = 0f;
    TreeMap<Float, String> sortedCodeTypes = new TreeMap<Float, String>(
        Collections.reverseOrder());

    // TODO(kprobst): turn this into a multimap?
    // com.google.common.collect.TreeMultimap
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

    final PrintWriter outFile = new PrintWriter(outFileName);

    outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
    outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
    outFile.println("<html>");
    outFile.println("<head>");
    outFile.println("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
    outFile.println("<link rel=\"stylesheet\" href=\"roundedCorners.css\" media=\"screen\">");
    outFile.println("</head>");
    outFile.println("<body>");

    int yOffset = 0;
    for (Float size : sortedCodeTypes.keySet()) {

      String codeType = sortedCodeTypes.get(size);
      String drillDownFileName = breakdown.getId() + "_" + codeType
          + "Classes.html";

      float ratio = (size / maxSize) * 79;
      float perc = (size / sumSize) * 100;

      if (ratio < 3) {
        ratio = 3;
      }

      outFile.println("<div id=\"box\" style=\"width:" + ratio + "%; top: "
          + yOffset + "px; left: 110px;\">");
      outFile.println("<div id=\"lb\">");
      outFile.println("<div id=\"rb\">");
      outFile.println("<div id=\"bb\"><div id=\"blc\"><div id=\"brc\">");
      outFile.println("<div id=\"tb\"><div id=\"tlc\"><div id=\"trc\">");
      outFile.println("<div id=\"content\">");
      outFile.println("</div>");
      outFile.println("</div></div></div></div>");
      outFile.println("</div></div></div></div>");
      outFile.println("</div>");

      int yOffsetText = yOffset + 8;
      outFile.printf("<div class=\"barlabel\" style=\"top:" + yOffsetText
          + "px; left:5px;\">%.1f</div>\n", size);
      outFile.printf("<div class=\"barlabel\" style=\"top:" + yOffsetText
          + "px; left:70px;\">%.1f", perc);
      outFile.println("%</div>\n");
      outFile.println("<div class=\"barlabel\" style=\"top:" + yOffsetText
          + "px; left:110px;\"><a href=\"" + drillDownFileName
          + "\" target=\"_top\">" + codeType + "</a></div>");

      yOffset = yOffset + 25;
    }
    outFile.println("</body>");
    outFile.println("</html>");
    outFile.close();

    return outFileName;
  }

  private static String makeLiteralsHtml(SizeBreakdown breakdown,
      Map<String, LiteralsCollection> nameToLitColl) throws IOException {
    String outFileName = breakdown.getId() + "_literalsBreakdown.html";
    float maxSize = 0f;
    float sumSize = 0f;
    TreeMap<Float, String> sortedLitTypes = new TreeMap<Float, String>(
        Collections.reverseOrder());

    for (String literal : nameToLitColl.keySet()) {
      float curSize = nameToLitColl.get(literal).cumSize;
      sumSize += curSize;

      if (curSize != 0f) {
        sortedLitTypes.put(curSize, literal);

        if (curSize > maxSize) {
          maxSize = curSize;
        }
      }
    }

    final PrintWriter outFile = new PrintWriter(outFileName);

    outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
    outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
    outFile.println("<html>");
    outFile.println("<head>");
    outFile.println("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
    outFile.println("<link rel=\"stylesheet\" href=\"roundedCorners.css\" media=\"screen\">");
    outFile.println("</head>");
    outFile.println("<body>");

    int yOffset = 0;
    for (Float size : sortedLitTypes.keySet()) {

      String literal = sortedLitTypes.get(size);
      String drillDownFileName = breakdown.getId() + "_" + literal
          + "Lits.html";

      float ratio = (size / maxSize) * 79;
      float perc = (size / sumSize) * 100;

      if (ratio < 3) {
        ratio = 3;
      }

      outFile.println("<div id=\"box\" style=\"width:" + ratio + "%; top: "
          + yOffset + "px; left: 110px;\">");
      outFile.println("<div id=\"lb\">");
      outFile.println("<div id=\"rb\">");
      outFile.println("<div id=\"bb\"><div id=\"blc\"><div id=\"brc\">");
      outFile.println("<div id=\"tb\"><div id=\"tlc\"><div id=\"trc\">");
      outFile.println("<div id=\"content\">");
      outFile.println("</div>");
      outFile.println("</div></div></div></div>");
      outFile.println("</div></div></div></div>");
      outFile.println("</div>");

      int yOffsetText = yOffset + 8;
      outFile.printf("<div class=\"barlabel\" style=\"top:" + yOffsetText
          + "px; left:5px;\">%.1f</div>\n", size);
      outFile.printf("<div class=\"barlabel\" style=\"top:" + yOffsetText
          + "px; left:70px;\">%.1f", perc);
      outFile.println("%</div>\n");
      outFile.println("<div class=\"barlabel\" style=\"top:" + yOffsetText
          + "px; left:110px;\"><a href=\"" + drillDownFileName
          + "\" target=\"_top\">" + literal + "</a></div>");

      yOffset = yOffset + 25;
    }
    outFile.println("</body>");
    outFile.println("</html>");
    outFile.close();

    return outFileName;
  }

  private static String makePackageHtml(SizeBreakdown breakdown)
      throws FileNotFoundException {
    String outFileName = breakdown.getId() + "_" + "packageBreakdown.html";
    Map<String, Float> packageToPartialSize = breakdown.packageToPartialSize;
    TreeMap<Float, String> sortedPackages = new TreeMap<Float, String>(
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

    final PrintWriter outFile = new PrintWriter(outFileName);

    outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
    outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
    outFile.println("<html>");
    outFile.println("<head>");
    outFile.println("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
    outFile.println("<link rel=\"stylesheet\" href=\"roundedCorners.css\" media=\"screen\">");

    outFile.println("</head>");
    outFile.println("<body>");

    int yOffset = 0;
    for (Float size : sortedPackages.keySet()) {
      String packageName = sortedPackages.get(size);
      String drillDownFileName = classesInPackageFileName(breakdown,
          packageName);

      float ratio = (size / maxSize) * 79;

      if (ratio < 3) {
        ratio = 3;
      }

      float perc = (size / sumSize) * 100;

      outFile.println("<div id=\"box\" style=\"width:" + ratio + "%; top: "
          + yOffset + "px; left: 110px;\">");
      outFile.println("<div id=\"lb\">");
      outFile.println("<div id=\"rb\">");
      outFile.println("<div id=\"bb\"><div id=\"blc\"><div id=\"brc\">");
      outFile.println("<div id=\"tb\"><div id=\"tlc\"><div id=\"trc\">");
      outFile.println("<div id=\"content\">");
      outFile.println("</div>");
      outFile.println("</div></div></div></div>");
      outFile.println("</div></div></div></div>");
      outFile.println("</div>");

      int yOffsetText = yOffset + 8;
      outFile.printf("<div class=\"barlabel\" style=\"top:" + yOffsetText
          + "px; left:5px;\">%.1f</div>\n", size);
      outFile.printf("<div class=\"barlabel\" style=\"top:" + yOffsetText
          + "px; left:70px;\">%.1f", perc);
      outFile.println("%</div>\n");
      outFile.println("<div class=\"barlabel\" style=\"top:" + yOffsetText
          + "px; left:110px;\"><a href=\"" + drillDownFileName
          + "\" target=\"_top\">" + packageName + "</a></div>");

      yOffset = yOffset + 25;
    }
    outFile.println("</body>");
    outFile.println("</html>");
    outFile.close();

    return outFileName;
  }

  private static String makeStringLiteralsHtml(SizeBreakdown breakdown,
      Map<String, LiteralsCollection> nameToLitColl) throws IOException {
    String outFileName = breakdown.getId() + "_stringLiteralsBreakdown.html";
    final PrintWriter outFile = new PrintWriter(outFileName);

    outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
    outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
    outFile.println("<html>");
    outFile.println("<head>");
    outFile.println("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
    outFile.println("<link rel=\"stylesheet\" href=\"roundedCorners.css\" media=\"screen\">");
    outFile.println("</head>");
    outFile.println("<body>");

    if (nameToLitColl.get("string").stringTypeToSize.size() > 0) {

      float maxSize = 0f;
      float sumSize = 0f;
      TreeMap<Float, String> sortedStLitTypes = new TreeMap<Float, String>(
          Collections.reverseOrder());

      for (String stringLiteral : nameToLitColl.get("string").stringTypeToSize.keySet()) {
        float curSize = nameToLitColl.get("string").stringTypeToSize.get(stringLiteral);
        sumSize += curSize;

        if (curSize != 0f) {
          sortedStLitTypes.put(curSize, stringLiteral);

          if (curSize > maxSize) {
            maxSize = curSize;
          }
        }
      }

      int yOffset = 0;
      for (Float size : sortedStLitTypes.keySet()) {

        String stringLiteral = sortedStLitTypes.get(size);
        String drillDownFileName = breakdown.getId() + "_" + stringLiteral
            + "Strings.html";

        float ratio = (size / maxSize) * 79;
        float perc = (size / sumSize) * 100;

        if (ratio < 3) {
          ratio = 3;
        }

        outFile.println("<div id=\"box\" style=\"width:" + ratio + "%; top: "
            + yOffset + "px; left: 110px;\">");
        outFile.println("<div id=\"lb\">");
        outFile.println("<div id=\"rb\">");
        outFile.println("<div id=\"bb\"><div id=\"blc\"><div id=\"brc\">");
        outFile.println("<div id=\"tb\"><div id=\"tlc\"><div id=\"trc\">");
        outFile.println("<div id=\"content\">");
        outFile.println("</div>");
        outFile.println("</div></div></div></div>");
        outFile.println("</div></div></div></div>");
        outFile.println("</div>");

        int yOffsetText = yOffset + 8;
        outFile.printf("<div class=\"barlabel\" style=\"top:" + yOffsetText
            + "px; left:5px;\">%.1f</div>\n", size);
        outFile.printf("<div class=\"barlabel\" style=\"top:" + yOffsetText
            + "px; left:70px;\">%.1f", perc);
        outFile.println("%</div>\n");
        outFile.println("<div class=\"barlabel\" style=\"top:" + yOffsetText
            + "px; left:110px;\"><a href=\"" + drillDownFileName
            + "\" target=\"_top\">" + stringLiteral + "</a></div>");

        yOffset = yOffset + 25;
      }
    } else {
      outFile.println("No string literals found for this application.");
    }

    outFile.println("</body>");
    outFile.println("</html>");
    outFile.close();

    return outFileName;
  }

  private static String shellFileName(SizeBreakdown breakdown) {
    return breakdown.getId() + "-overallBreakdown.html";
  }

  public void makeDependenciesHtml(Map<String, ArrayList<String>> dependencies)
      throws IOException {

    String origOutFileName = "methodDependencies-";
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
          outFile.println("</table>");
          outFile.println("<center>");

          outFile.println("</div>");
          outFile.println("</body>");
          outFile.println("</html>");
          outFile.close();
        }

        String outFileName = origOutFileName + filename(curPackageName)
            + ".html";
        outFile = new PrintWriter(outFileName);

        outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
        outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
        outFile.println("<html>");
        outFile.println("<head>");
        outFile.println("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
        outFile.println("<title>Method Dependencies</title>");
        outFile.println("</head>");

        outFile.println("<style type=\"text/css\">");
        outFile.println("body {background-color: #728FCE}");
        outFile.println("h2 {background-color: transparent}");
        outFile.println("p {background-color: fuchsia}");
        outFile.println("</style>");

        outFile.println("<body>");
        outFile.println("<center>");
        outFile.println("<h2>Method Dependencies for package " + curPackageName
            + "</h2>");
        outFile.println("</center>");
        outFile.println("<hr>");

        outFile.println("<center>");
        outFile.println("<table border=\"1\" width=\"80%\" style=\"font-size: 11pt;\" bgcolor=\"white\">");
      }
      outFile.println("<tr>");
      if (curClassName.compareTo(className) != 0) {
        outFile.println("<td width=\"80%\"><a name=\"" + className + "\">"
            + "<a name=\"" + method + "\">" + method
            + "</a></a><font color=\"green\"> called by</font></td>");
        curClassName = className;
      } else {
        outFile.println("<td width=\"80%\"><a name=\"" + method + "\">"
            + method + "</a><font color=\"green\"> called by</font></td>");
      }
      outFile.println("</tr>");

      for (int i = 0; i < dependencies.get(method).size(); i++) {
        String depMethod = dependencies.get(method).get(i);

        outFile.println("<tr>");
        outFile.println("<td width=\"20%\"></td>");
        if (i != dependencies.get(method).size() - 1) {
          outFile.println("<td width=\"60%\">" + depMethod
              + "<font color=\"green\"> called by</font></td>");
        } else {
          outFile.println("<td width=\"60%\">" + depMethod + "</td>");
        }
        outFile.println("</tr>");
      }
    }
  }

  /**
   * Make size breakdowns for each package for one code collection.
   */
  public void makePackageClassesHtmls(SizeBreakdown breakdown)
      throws IOException {
    for (String packageName : GlobalInformation.packageToClasses.keySet()) {

      TreeMap<Float, String> sortedClasses = new TreeMap<Float, String>(
          Collections.reverseOrder());
      float maxSize = 0f;

      int maxDepCount = 1;

      for (String className : GlobalInformation.packageToClasses.get(packageName)) {

        float curSize = 0f;
        if (!breakdown.classToPartialSize.containsKey(className)) {
          // This class not present in this code collection
        } else {
          curSize = breakdown.classToPartialSize.get(className);
        }

        int depCount = 0;
        if (GlobalInformation.classToWhatItDependsOn.containsKey(className)) {
          depCount = GlobalInformation.classToWhatItDependsOn.get(className).size();
        }

        if (curSize != 0f) {

          sortedClasses.put(curSize, className);
          if (curSize > maxSize) {
            maxSize = curSize;
          }
          if (depCount > maxDepCount) {
            maxDepCount = depCount;
          }
        }
      }

      PrintWriter outFile = new PrintWriter(classesInPackageFileName(breakdown,
          packageName));

      outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
      outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
      outFile.println("<html>");
      outFile.println("<head>");
      outFile.println("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
      outFile.println("<link rel=\"stylesheet\" href=\"classLevel.css\" media=\"screen\">");
      outFile.println("<title>Classes in package \"" + packageName
          + "\"</title>");
      outFile.println("</head>");
      outFile.println("<body>");

      outFile.println("<center>");
      outFile.println("<h2>Classes in package \"" + packageName + "\"</h2>");
      addHeaderWithBreakdownContext(breakdown, outFile);
      outFile.println("</center>");

      outFile.println("<div style=\"width:90%; height:80%; overflow-y:auto; overflow-x:auto; top: 120px; left:70px; position:absolute; background-color:white\"");

      int yOffset = 0;
      for (Float size : sortedClasses.keySet()) {

        String className = sortedClasses.get(size);

        // TODO(kprobst): switch out the commented/uncommented lines below when
        // showing dependencies
        // float ratio = (size / maxSize) * 45;
        float ratio = (size / maxSize) * 85;

        if (ratio < 3) {
          ratio = 3;
        }

        // TODO(kprobst): not currently used, but will be for dependencies
        // get the dependency count
        int depCount = 0;
        if (GlobalInformation.classToWhatItDependsOn.containsKey(className)) {
          depCount = GlobalInformation.classToWhatItDependsOn.get(className).size();
        }
        float depRatio = ((float) depCount / (float) maxDepCount) * 45f;
        if (depRatio < 3.0) {
          depRatio = 3;
        }

        outFile.println("<div class=\"box\" style=\"width:" + ratio
            + "%; top: " + yOffset + "px; left: 60px;\">");
        outFile.println("<div id=\"lb\">");
        outFile.println("<div id=\"rb\">");
        outFile.println("<div id=\"bb\"><div id=\"blc\"><div id=\"brc\">");
        outFile.println("<div id=\"tb\"><div id=\"tlc\"><div id=\"trc\">");
        outFile.println("<div id=\"content\">");
        outFile.println("</div>");
        outFile.println("</div></div></div></div>");
        outFile.println("</div></div></div></div>");
        outFile.println("</div>");

        // TODO(kprobst): not currently used, but will be for dependencies
        /*
         * outFile.println("<div class=\"box-right\" style=\"width:" + depRatio
         * + "%; top: " + yOffset + "px; left: 50%\">");
         * outFile.println("<div id=\"lb\">");
         * outFile.println("<div id=\"rb\">");
         * outFile.println("<div id=\"bb\"><div id=\"blc\"><div id=\"brc\">");
         * outFile.println("<div id=\"tb\"><div id=\"tlc\"><div id=\"trc\">");
         * outFile.println("<div id=\"content\">"); outFile.println("</div>");
         * outFile.println("</div></div></div></div>");
         * outFile.println("</div></div></div></div>");
         * outFile.println("</div>");
         */

        int yOffsetText = yOffset + 8;
        outFile.printf("<div class=\"barlabel\" style=\"top:" + yOffsetText
            + "px; left:5px;\">%.1f</div>\n", size);
        if (GlobalInformation.displayDependencies == true) {
          outFile.println("<div class=\"barlabel\" style=\"top:" + yOffsetText
              + "px; left:70px;\"><a href=\"methodDependencies-"
              + filename(packageName) + ".html#" + className + "\">"
              + className + "</a></div>");
        } else {
          outFile.println("<div class=\"barlabel\" style=\"top:" + yOffsetText
              + "px; left:70px;\">" + className + "</div>");
        }
        /*
         * //TODO(kprobst) make this a link String drillDownFileName = className
         * + "Deps.html"; outFile.println("<div class=\"barlabel\" style=\"top:"
         * + yOffsetText + "px; left:50%;\"><a href=\"" + drillDownFileName +
         * "\" target=\"_top\">Dependencies: " + depCount + "</a></div>");
         */
        yOffset = yOffset + 25;
      }

      outFile.println("</div>");
      outFile.println("</body>");
      outFile.println("</html>");
      outFile.close();
    }
  }

  /**
   * Makes html file for fragment classes. TODO(kprobst): update this once we
   * have SOYC updated to supply enough information
   * 
   * TODO(spoon) instead of this listing, make a size breakdown for each
   * exclusive fragment
   */
  public void makeSplitPointClassesHtmls() throws IOException {

    // for the initial fragment and the fragments in the load order, we can
    // print this immediately
    // For those fragments *not* in the initial load order, we just collect and
    // then print at the end
    TreeSet<String> sortedClassesAndMethodsAllOtherFragments = new TreeSet<String>();

    for (Integer fragmentName : GlobalInformation.fragmentToStories.keySet()) {
      if (fragmentName == FRAGMENT_NUMBER_INITIAL_DOWNLOAD) {
        /*
         * For the initial download, a size breakdown is availale to replace the
         * split point class listing
         */
        continue;
      }

      if (!GlobalInformation.splitPointToLocation.containsKey(fragmentName)) {
        // get the stories from ALL the fragments
        for (String storyName : GlobalInformation.fragmentToStories.get(fragmentName)) {
          if ((!GlobalInformation.totalCodeBreakdown.nameToLitColl.get("string").storyToLocations.containsKey(storyName))
              && (GlobalInformation.storiesToCorrClassesAndMethods.containsKey(storyName))) {
            for (String className : GlobalInformation.storiesToCorrClassesAndMethods.get(storyName)) {
              sortedClassesAndMethodsAllOtherFragments.add(className);
            }
          }
        }
      } else {
        String curSplitPointLocation;

        if (fragmentName == FRAGMENT_NUMBER_INITIAL_DOWNLOAD) {
          curSplitPointLocation = "initialDownload";
        } else {
          curSplitPointLocation = GlobalInformation.splitPointToLocation.get(fragmentName);
        }

        String outFileName = "splitPoint-" + filename(curSplitPointLocation)
            + "-Classes.html";

        final PrintWriter outFile = new PrintWriter(outFileName);

        outFile.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"");
        outFile.println("\"http://www.w3.org/TR/html4/strict.dtd\">");
        outFile.println("<html>");
        outFile.println("<head>");
        outFile.println("<meta http-equiv=\"content-type\" content=\"text/html;charset=ISO-8859-1\">");
        outFile.println("<title>Classes and methods in exclusives fragment for runAsync split point \""
            + curSplitPointLocation + "\" </title>");
        outFile.println("</head>");

        outFile.println("<style type=\"text/css\">");
        outFile.println("body {background-color: #728FCE}");
        outFile.println("h2 {background-color: transparent}");
        outFile.println("p {background-color: fuchsia}");
        outFile.println(".tablediv {");
        outFile.println("display:  table;");
        outFile.println("width:100%;");
        outFile.println("background-color:#eee;");
        outFile.println("border:1px solid  #666666;");
        outFile.println("border-spacing:5px;");
        outFile.println("border-collapse:separate;");
        outFile.println("}");
        outFile.println(".celldiv {");
        outFile.println("float:left;");
        outFile.println("display:  table-cell;");
        outFile.println("width:49.5%;");
        outFile.println("font-size: 14px;");
        outFile.println("background-color:white;");
        outFile.println("}");
        outFile.println(".rowdiv  {");
        outFile.println("display:  table-row;");
        outFile.println("width:100%;");
        outFile.println("}");
        outFile.println("</style>");

        outFile.println("<body>");
        outFile.println("<center>");
        outFile.println("<h2>Classes and methods in exclusives fragment for runAsync split point \""
            + curSplitPointLocation + "\"</h2>");
        outFile.println("</center>");
        outFile.println("<hr>");

        outFile.println("<div style=\"width:90%; height:80%; overflow-y:auto; overflow-x:auto; top: 30px; left:60px; position:relative; background-color:white\"");
        outFile.println("<div  class=\"tablediv\">");

        TreeSet<String> sortedClassesAndMethods = new TreeSet<String>();
        for (String storyName : GlobalInformation.fragmentToStories.get(fragmentName)) {
          if ((!GlobalInformation.totalCodeBreakdown.nameToLitColl.get("string").storyToLocations.containsKey(storyName))
              && (GlobalInformation.storiesToCorrClassesAndMethods.containsKey(storyName))) {
            for (String className : GlobalInformation.storiesToCorrClassesAndMethods.get(storyName)) {
              sortedClassesAndMethods.add(className);
            }
          }
        }
        for (String classOrMethod : sortedClassesAndMethods) {

          // if it's a method
          if ((GlobalInformation.displayDependencies == true)
              && (classOrMethod.contains("(")) && (classOrMethod.contains(")"))) {
            // get the package
            String packageName = classOrMethod;
            packageName = packageName.replaceAll("\\.\\p{Upper}.*", "");

            String noParamMethod = classOrMethod;
            noParamMethod = noParamMethod.replaceAll("\\(.*", "");

            outFile.println("<div  class=\"rowdiv\"><a href=\"methodDependencies-"
                + filename(packageName)
                + ".html#"
                + noParamMethod
                + "\">"
                + classOrMethod + "</a></div>");
          } else {
            outFile.println("<div  class=\"rowdiv\">" + classOrMethod
                + "</div>");
          }
        }

        outFile.println("</div>");
        outFile.println("</body>");
        outFile.println("</html>");
        outFile.close();
      }
    }
  }
}
