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
import com.google.gwt.dev.util.StringInterner;
import com.google.gwt.soyc.MakeTopLevelHtmlForPerm.DependencyLinker;
import com.google.gwt.soyc.MakeTopLevelHtmlForPerm.NullDependencyLinker;
import com.google.gwt.soyc.io.FileSystemOutputDirectory;
import com.google.gwt.soyc.io.OutputDirectory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * The command-line entry point for creating a compile report.
 */
public class SoycDashboard {
  private static class FormatException extends RuntimeException {
    public FormatException() {
      super();
    }

    public FormatException(String message) {
      super(message);
    }

    public FormatException(Throwable cause) {
      super(cause);
    }
  }

  public static void main(final String[] args) throws InterruptedException {

    System.out.println("WARNING: The direct use of the SoycDashboard is deprecated and will be removed. " +
      "The preferred usage is to invoke the compiler with the -compileReport option, which " +
      "writes the compile report directly to the extra directory.");
    Thread.currentThread();
    Thread.sleep(5000);

    Settings settings;
    try {
      settings = Settings.fromArgumentList(args);
    } catch (Settings.ArgumentListException e) {
      System.err.println(e.getMessage());
      System.err.println("Usage: "
          + "java com.google.gwt.soyc.SoycDashboard -resources dir -soycDir dir -symbolMaps dir [-out dir]");
      System.err.println("(Legacy usage: "
          + "java com.google.gwt.soyc.SoycDashboard options stories0.xml[.gz] [dependencies0.xml[.gz]] [splitpoints0.xml[.gz]])");
      System.err.println("Options:");
      System.err.println(Settings.settingsHelp());
      System.exit(1);
      return; // not reached
    }

    System.out.println("Generating the Story of Your Compile...");

    OutputDirectory outDir = new FileSystemOutputDirectory(new File(
        settings.out.get()));

    try {
      Map<String, List<String>> permInfo = readPermutationInfo(settings);
      SoycDashboard dashboard = new SoycDashboard(outDir);
      for (String permutationId : permInfo.keySet()) {
        dashboard.startNewPermutation(permutationId);
        if (settings.symbolMapsDir.get() == null) {
          dashboard.readFromFilesNamed(settings.storiesFileName,
              settings.depFileName, settings.splitPointsFileName);
        } else {
          String soycDir = settings.soycDir.get();
          dashboard.readFromFilesNamed(soycInputFile(soycDir, "stories",
              permutationId), soycInputFile(soycDir, "dependencies",
              permutationId), soycInputFile(soycDir, "splitPoints",
              permutationId));
        }
        dashboard.generateForOnePermutation();
        System.out.println("Finished creating reports for permutation.");
      }

      dashboard.generateCrossPermutationFiles(permInfo);
      System.out.println("Finished creating reports. To see the dashboard, open index.html in your browser.");
    } catch (ParserConfigurationException e) {
      System.err.println("Could not parse document. " + e.getMessage());
      System.exit(1);
    } catch (SAXException e) {
      System.err.println("Could not create SAX parser. " + e.getMessage());
      System.exit(1);
    } catch (FileNotFoundException e) {
      System.err.println("Cannot open file " + e.getMessage());
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Error creating html file. " + e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Open a file for reading. If the filename ends in .gz, then wrap the stream
   * with a {@link GZIPInputStream}.
   */
  public static InputStream openPossiblyGzippedFile(String filename)
      throws IOException {
    InputStream in = new FileInputStream(filename);
    if (filename.endsWith(".gz")) {
      in = new GZIPInputStream(in);
    }
    in = new BufferedInputStream(in);
    return in;
  }

  /*
   * cleans up the RPC code categories
   */
  private static void foldInRPCHeuristic(
      final HashMap<String, CodeCollection> nameToCodeColl) {
    /**
     * Heuristic: this moves all classes that override serializable from RPC to
     * "Other Code" *if* there is no RPC generated code, i.e., if the
     * application really is not using RPC
     */

    if (nameToCodeColl.get("rpcGen").classes.size() == 0) {

      for (String className : nameToCodeColl.get("rpcUser").classes) {

        if ((!nameToCodeColl.get("widget").classes.contains(className))
            && (!nameToCodeColl.get("jre").classes.contains(className))
            && (!nameToCodeColl.get("gwtLang").classes.contains(className))) {
          nameToCodeColl.get("allOther").classes.add(className);
        }
      }
      nameToCodeColl.get("rpcUser").classes.clear();

      for (String className : nameToCodeColl.get("rpcGwt").classes) {
        if ((!nameToCodeColl.get("widget").classes.contains(className))
            && (!nameToCodeColl.get("jre").classes.contains(className))
            && (!nameToCodeColl.get("gwtLang").classes.contains(className))) {
          nameToCodeColl.get("allOther").classes.add(className);
        }
      }
      nameToCodeColl.get("rpcGwt").classes.clear();
    }
  }

  private static DefaultHandler parseXMLDocumentDependencies(
      final Map<String, Map<String, String>> allDependencies) {
    DefaultHandler handler = new DefaultHandler() {

      // may want to create a class for this later
      String curMethod;
      Map<String, String> dependencies = new TreeMap<String, String>();
      String graphExtends = null;
      StringBuilder valueBuilder = new StringBuilder();

      @Override
      public void endElement(String uri, String localName, String qName) {
        if (localName.compareTo("table") == 0) {
          if (graphExtends != null) {
            // Add in elements from the extended graph
            for (Entry<String, String> entry : allDependencies.get(graphExtends).entrySet()) {
              dependencies.put(entry.getKey(), entry.getValue());
            }
          }
        }
      }

      @Override
      public void startElement(String nsUri, String strippedName,
          String tagName, final Attributes attributes) {

        valueBuilder.delete(0, valueBuilder.length());

        if (strippedName.compareTo("table") == 0
            && (attributes.getValue("name") != null)) {
          String name = attributes.getValue("name");
          dependencies = new TreeMap<String, String>();
          allDependencies.put(StringInterner.get().intern(name), dependencies);
          if (attributes.getValue("extends") != null) {
            graphExtends = StringInterner.get().intern(attributes.getValue("extends"));
            if (!allDependencies.containsKey(graphExtends)) {
              throw new FormatException("Graph " + name
                  + " extends an unknown graph " + graphExtends);
            }
          } else {
            graphExtends = null;
          }
        } else if ((strippedName.compareTo("method") == 0)
            && (attributes.getValue("name") != null)) {
          curMethod = StringInterner.get().intern(attributes.getValue("name"));
        } else if ((strippedName.compareTo("called") == 0)
            && (attributes.getValue("by") != null)) {
          String curDepMethod = attributes.getValue("by");
          if (!dependencies.containsKey(curMethod)) {
            dependencies.put(StringInterner.get().intern(curMethod),
                StringInterner.get().intern(curDepMethod));
          }
        }
      }
    };
    return handler;
  }

  private static Map<String, List<String>> readPermutationInfo(Settings settings)
      throws FileNotFoundException {
    Map<String, List<String>> allPermsInfo = new TreeMap<String, List<String>>();
    if (settings.symbolMapsDir.get() == null) {
      String permutationId = settings.storiesFileName;
      permutationId = permutationId.replaceAll(".*/stories", "");
      permutationId = permutationId.replaceAll("\\.xml(\\.gz)?", "");
    } else {
      File dir = new File(settings.symbolMapsDir.get());
      String files[] = dir.list();
      for (Integer i = 0; i < files.length; i++) {
        String permFileName = settings.symbolMapsDir.get() + "/" + files[i];
        FileReader fir = new FileReader(permFileName);

        Scanner sc = new Scanner(fir);

        String permutationId = "";
        String permutationInfo = "";
        List<String> permutationInfoList = new ArrayList<String>();
        boolean firstLine = true;
        String curLine = sc.nextLine();
        curLine = curLine.trim();
        while (curLine.startsWith("# {")) {

            curLine = curLine.replace("# {", "");
            curLine = curLine.replace("}", "");
            curLine = curLine.trim();
            if (firstLine) {
              permutationId = curLine;
              firstLine = false;
            } else {
              if (permutationInfo.length() > 0) {
                permutationInfo += "<br>";
              }
              permutationInfo += curLine;
              permutationInfoList.add(curLine);
            }
            if (sc.hasNextLine()) {
              curLine = sc.nextLine();
              curLine = curLine.trim();
            }
        }
        allPermsInfo.put(permutationId, permutationInfoList);
      }
    }
    return allPermsInfo;
  }

  private static String soycInputFile(String soycDir, String baseName,
      String permutationId) {
    String name = soycDir + "/" + baseName + permutationId + ".xml.gz";
    if (new File(name).exists()) {
      return name;
    }
    return soycDir + "/" + baseName + permutationId + ".xml";
  }

  /**
   * Global information for the current permutation being emitted.
   */
  private GlobalInformation globalInformation;

  /**
   * HTML emitter for the current permutation being emitted.
   */
  private MakeTopLevelHtmlForPerm makeTopLevelHtmlForPerm;

  private final OutputDirectory outDir;

  public SoycDashboard(OutputDirectory outDir) {
    this.outDir = outDir;
  }

  public void generateCompilerMetricsForOnePermutation(
      ModuleMetricsArtifact moduleMetrics,
      PrecompilationMetricsArtifact precompilationMetrics,
      CompilationMetricsArtifact compilationMetrics)  throws IOException {
    makeTopLevelHtmlForPerm.makeCompilerMetricsPermFiles(moduleMetrics,
        precompilationMetrics, compilationMetrics);
  }

  public void generateCrossPermutationFiles(Map<String, List<String>> permInfo)
      throws IOException {
    StaticResources.emit(outDir);
    MakeTopLevelHtmlForPerm.makeTopLevelHtmlForAllPerms(permInfo, outDir);
  }

  public void generateForOnePermutation() throws IOException {
    if (globalInformation.dependencies != null) {
      makeTopLevelHtmlForPerm.makeDependenciesHtml();
    }

    if (globalInformation.getNumFragments() > 0) {
      makeTopLevelHtmlForPerm.makeSplitStatusPages();
      makeTopLevelHtmlForPerm.makeLeftoverStatusPages();
    }
    for (SizeBreakdown breakdown : globalInformation.allSizeBreakdowns()) {
      DependencyLinker linker = chooseDependencyLinker(breakdown);
      makeHTMLFiles(makeTopLevelHtmlForPerm, breakdown, linker);
    }
  }

  public void readDependencies(InputStream stream)
      throws ParserConfigurationException, SAXException, IOException {
    globalInformation.dependencies = new TreeMap<String, Map<String, String>>();
    DefaultHandler depHandler = parseXMLDocumentDependencies(globalInformation.dependencies);
    SAXParserFactory depFactoryMain = SAXParserFactory.newInstance();
    depFactoryMain.setNamespaceAware(true);
    SAXParser saxParser = depFactoryMain.newSAXParser();
    saxParser.parse(stream, depHandler);
  }

  public void readSizeMaps(InputStream stream)
      throws ParserConfigurationException, SAXException, IOException {
    DefaultHandler handler = parseXMLDocumentSizeMap(globalInformation);
    SAXParserFactory factoryMain = SAXParserFactory.newInstance();
    factoryMain.setNamespaceAware(true);
    SAXParser saxParser = factoryMain.newSAXParser();
    saxParser.parse(stream, handler);

    // Now clean up the information that has been read in various ways
    globalInformation.computePackageSizes();

    // add to "All Other Code" if none of the special categories apply
    for (SizeBreakdown breakdown : globalInformation.allSizeBreakdowns()) {
      updateAllOtherCodeType(breakdown.nameToCodeColl);
    }

    // clean up the RPC categories
    for (SizeBreakdown breakdown : globalInformation.allSizeBreakdowns()) {
      foldInRPCHeuristic(breakdown.nameToCodeColl);
    }
  }

  public void readSplitPoints(InputStream stream)
      throws ParserConfigurationException, SAXException, IOException {
    DefaultHandler splitPointHandler = parseXMLDocumentSplitPoints();
    SAXParserFactory splitPointsFactoryMain = SAXParserFactory.newInstance();
    splitPointsFactoryMain.setNamespaceAware(true);

    SAXParser saxParser = splitPointsFactoryMain.newSAXParser();
    saxParser.parse(stream, splitPointHandler);
  }

  public void startNewPermutation(String permutationId) {
    globalInformation = new GlobalInformation(permutationId);
    makeTopLevelHtmlForPerm = new MakeTopLevelHtmlForPerm(globalInformation,
        outDir);
  }

  private Collection<SizeBreakdown> breakdownsForFragment(Integer fragment) {
    List<SizeBreakdown> breakdowns = new ArrayList<SizeBreakdown>();
    breakdowns.add(globalInformation.getTotalCodeBreakdown());
    if (fragment == 0) {
      breakdowns.add(globalInformation.getInitialCodeBreakdown());
    }
    if (fragment == (globalInformation.getNumFragments() + 1)) {
      breakdowns.add(globalInformation.getLeftoversBreakdown());
    }
    if (fragment >= 1 && fragment <= globalInformation.getNumFragments()) {
      breakdowns.add(globalInformation.fragmentCodeBreakdown(fragment));
    }
    return breakdowns;
  }

  private DependencyLinker chooseDependencyLinker(SizeBreakdown breakdown) {
    if (globalInformation.dependencies == null) {
      // no dependencies are available
      return new NullDependencyLinker();
    }

    if (breakdown == globalInformation.getTotalCodeBreakdown()) {
      if (globalInformation.getNumFragments() > 0) {
        return makeTopLevelHtmlForPerm.new DependencyLinkerForTotalBreakdown();
      } else {
        return makeTopLevelHtmlForPerm.new DependencyLinkerForInitialCode();
      }
    } else if (breakdown == globalInformation.getInitialCodeBreakdown()) {
      return makeTopLevelHtmlForPerm.new DependencyLinkerForInitialCode();
    } else if (breakdown == globalInformation.getLeftoversBreakdown()) {
      assert globalInformation.getNumFragments() > 0;
      return makeTopLevelHtmlForPerm.new DependencyLinkerForLeftoversFragment();
    } else {
      return new NullDependencyLinker();
    }
  }

  /**
   * Generates all the HTML files for one size breakdown.
   */
  private void makeHTMLFiles(MakeTopLevelHtmlForPerm makeTopLevelHtmlForPerm,
      SizeBreakdown breakdown, DependencyLinker depLinker) throws IOException {
    makeTopLevelHtmlForPerm.makePackageClassesHtmls(breakdown, depLinker);
    makeTopLevelHtmlForPerm.makeCodeTypeClassesHtmls(breakdown);
    makeTopLevelHtmlForPerm.makeLiteralsClassesTableHtmls(breakdown);
    makeTopLevelHtmlForPerm.makeBreakdownShell(breakdown);
    makeTopLevelHtmlForPerm.makeTopLevelShell();
  }

  private DefaultHandler parseXMLDocumentSizeMap(
      final GlobalInformation globalInformation) {
    return new DefaultHandler() {
      int fragment = -1;

      @Override
      public void endElement(String uri, String localName, String qName) {
        if (localName.compareTo("sizemap") == 0) {
          fragment = -1;
        }
      }

      @Override
      public void startElement(String uri, String localName, String qName,
          final Attributes attributes) {
        if (localName.compareTo("sizemap") == 0) {
          // starting a new size map
          String fragString = attributes.getValue("fragment");
          if (fragString == null) {
            throw new FormatException();
          }
          try {
            fragment = Integer.valueOf(fragString);
          } catch (NumberFormatException e) {
            throw new FormatException(e);
          }
          String sizeString = attributes.getValue("size");
          if (sizeString == null) {
            throw new FormatException();
          }
          int size;
          try {
            size = Integer.valueOf(sizeString);
          } catch (NumberFormatException e) {
            throw new FormatException(e);
          }
          for (SizeBreakdown breakdown : breakdownsForFragment(fragment)) {
            breakdown.sizeAllCode += size;
          }
        } else if (localName.compareTo("size") == 0) {
          String type = attributes.getValue("type");
          if (type == null) {
            throw new FormatException();
          }
          String ref = attributes.getValue("ref");
          if (ref == null) {
            throw new FormatException();
          }
          String sizeString = attributes.getValue("size");
          if (sizeString == null) {
            throw new FormatException();
          }
          int size;
          try {
            size = Integer.valueOf(sizeString);
          } catch (NumberFormatException e) {
            throw new FormatException(e);
          }
          recordSize(type, ref, size, globalInformation);
        }
      }

      private void accountForSize(SizeBreakdown breakdown, String refType,
          String ref, int size, GlobalInformation globalInformation) {
        if (refType.equals("string")) {
          LiteralsCollection stringLiterals = breakdown.nameToLitColl.get("string");
          stringLiterals.size += size;
          stringLiterals.literals.add(ref);
        } else if (refType.equals("var")) {
          // Nothing to record, because no breakdown is provided for random
          // variables
        } else {
          if (!refType.equals("type") && !refType.equals("method") && !refType.equals("field")) {
            throw new FormatException();
          }

          if (refType.equals("method")) {
            breakdown.methodToSize.put(ref, Integer.valueOf(size));
          }

          String className = ref;
          if (className.contains("::")) {
            /*
             * It's a method or field reference. Discard the method/field part.
             */
            int idx = className.indexOf(':');
            className = className.substring(0, idx);
          }
          className = StringInterner.get().intern(className);
          // derive the package name from the class
          String packageName;
          if (!globalInformation.getClassToPackage().containsKey(className)) {
            packageName = className;
            packageName = packageName.replaceAll("\\.[A-Z].*", "");
            globalInformation.getClassToPackage().put(className, packageName);
          } else {
            packageName = globalInformation.getClassToPackage().get(className);
          }

          if (!globalInformation.getPackageToClasses().containsKey(packageName)) {
            TreeSet<String> insertSet = new TreeSet<String>();
            insertSet.add(className);
            globalInformation.getPackageToClasses().put(packageName, insertSet);
          } else {
            globalInformation.getPackageToClasses().get(packageName).add(
                className);
          }

          recordClassCategories(breakdown.nameToCodeColl, className,
              packageName);

          if (breakdown.classToSize.containsKey(className)) {
            int newSize = breakdown.classToSize.get(className) + size;
            breakdown.classToSize.put(className, newSize);
          } else {
            breakdown.classToSize.put(className, size);
          }
        }
      }

      private void recordClassCategories(
          final HashMap<String, CodeCollection> nameToCodeColl,
          String className, String packageName) {
        if (packageName.startsWith("java")) {
          nameToCodeColl.get("jre").classes.add(className);
        } else if (packageName.startsWith("com.google.gwt.lang")) {
          nameToCodeColl.get("gwtLang").classes.add(className);
        }
        if (className.contains("_CustomFieldSerializer")) {
          nameToCodeColl.get("rpcUser").classes.add(className);
        } else if (className.endsWith("_FieldSerializer")
            || className.endsWith("_Proxy")
            || className.endsWith("_TypeSerializer")) {
          nameToCodeColl.get("rpcGen").classes.add(className);
        }
      }

      private void recordSize(String refType, String ref, int size,
          GlobalInformation globalInformation) {
        refType = StringInterner.get().intern(refType);
        ref = StringInterner.get().intern(ref);
        for (SizeBreakdown breakdown : breakdownsForFragment(fragment)) {
          accountForSize(breakdown, refType, ref, size, globalInformation);
        }
      }
    };
  }

  private DefaultHandler parseXMLDocumentSplitPoints() {
    DefaultHandler handler = new DefaultHandler() {

      private boolean inInitialLoadSequence = false;

      @Override
      public void endElement(String uri, String localName, String qName) {
        if (localName.compareTo("initialesq") == 0) {
          inInitialLoadSequence = false;
        }
      }

      @Override
      public void startElement(String nsUri, String strippedName,
          String tagName, final Attributes attributes) {
        if (strippedName.compareTo("splitpoint") == 0) {
          parseSplitPoint(attributes);
        } else if (strippedName.compareTo("initialseq") == 0) {
          inInitialLoadSequence = true;
        } else if (inInitialLoadSequence
            && strippedName.compareTo("splitpointref") == 0) {
          globalInformation.getInitialFragmentLoadSequence().add(
              parseSplitPointReference(attributes));
        }
      }

      /**
       * Parses a split point entry from a splitpoints XML soyc file.
       * A split point node as in \<splitpoint id=N location=DESC/\>
       *
       * @param attributes the attributes of the splitpoint node (provided by the SAX parsing
       *                   infrastructure)
       */
      private void parseSplitPoint(final Attributes attributes) {
        if (attributes.getValue("id") != null) {
          String curSplitPoint = attributes.getValue("id");
          if (attributes.getValue("location") != null) {
            String curSplitPointLocation = attributes.getValue("location");

            curSplitPointLocation = curSplitPointLocation.replaceAll("\\(L.*",
                "");

            globalInformation.addFragmentDescriptor(
                Integer.parseInt(curSplitPoint), curSplitPointLocation);
          }
        }
      }

      private Integer parseSplitPointReference(final Attributes attributes) {
        String spString = attributes.getValue("id");
        if (spString == null) {
          throw new FormatException("Could not parse split point reference");
        }
        return Integer.valueOf(spString);
      }

    };
    return handler;
  }

  private void readFromFilesNamed(String storiesFileName,
      String dependenciesFileName, String splitPointsFileName)
      throws ParserConfigurationException, SAXException, IOException {
    if (dependenciesFileName != null && new File(dependenciesFileName).exists()) {
      readDependencies(openPossiblyGzippedFile(dependenciesFileName));
    }

    if (splitPointsFileName != null && new File(splitPointsFileName).exists()) {
      readSplitPoints(openPossiblyGzippedFile(splitPointsFileName));
    }

    readSizeMaps(openPossiblyGzippedFile(storiesFileName));
  }

  /*
   * assigns code to "all other code" if none of the special categories apply
   */
  private void updateAllOtherCodeType(
      final HashMap<String, CodeCollection> nameToCodeColl) {
    // all classes not in any of the other categories
    for (String className : globalInformation.getClassToPackage().keySet()) {
      if ((!nameToCodeColl.get("widget").classes.contains(className))
          && (!nameToCodeColl.get("rpcUser").classes.contains(className))
          && (!nameToCodeColl.get("rpcGwt").classes.contains(className))
          && (!nameToCodeColl.get("rpcGen").classes.contains(className))
          && (!nameToCodeColl.get("jre").classes.contains(className))
          && (!nameToCodeColl.get("gwtLang").classes.contains(className))) {
        nameToCodeColl.get("allOther").classes.add(className);
      }
    }
  }
}
