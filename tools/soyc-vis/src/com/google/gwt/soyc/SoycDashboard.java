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

import com.google.gwt.soyc.MakeTopLevelHtmlForPerm.DependencyLinker;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * The command-line entry point for creating a SOYC report.
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

  public static void main(String[] args) {
    try {
      System.out.println("Generating the Story of Your Compile...");
      GlobalInformation.settings = Settings.fromArgumentList(args);

      Settings settings = GlobalInformation.settings;
      GlobalInformation.displayDependencies = (settings.depFileName != null);
      GlobalInformation.displaySplitPoints = (settings.splitPointsFileName != null);

      MakeTopLevelHtmlForPerm makeTopLevelHtmlForPerm = new MakeTopLevelHtmlForPerm();

      new File(settings.out.get()).mkdir();

      if (GlobalInformation.displayDependencies == true) {
        /**
         * handle dependencies
         */
        Map<String, Map<String, String>> dependencies = new TreeMap<String, Map<String, String>>();
        DefaultHandler depHandler = parseXMLDocumentDependencies(dependencies);

        // start parsing
        SAXParserFactory depFactoryMain = SAXParserFactory.newInstance();
        depFactoryMain.setNamespaceAware(true);

        SAXParser saxParser = depFactoryMain.newSAXParser();
        InputStream in = new FileInputStream(settings.depFileName);
        if (settings.depFileName.endsWith(".gz")) {
          in = new GZIPInputStream(in);
        }
        in = new BufferedInputStream(in);
        saxParser.parse(in, depHandler);

        makeTopLevelHtmlForPerm.makeDependenciesHtml(dependencies);
      }

      if (GlobalInformation.displaySplitPoints == true) {
        /**
         * handle runAsync split points
         */

        DefaultHandler splitPointHandler = parseXMLDocumentSplitPoints();

        // start parsing
        SAXParserFactory splitPointsFactoryMain = SAXParserFactory.newInstance();
        splitPointsFactoryMain.setNamespaceAware(true);

        SAXParser saxParser = splitPointsFactoryMain.newSAXParser();
        InputStream in = new FileInputStream(settings.splitPointsFileName);
        if (settings.depFileName.endsWith(".gz")) {
          in = new GZIPInputStream(in);
        }
        in = new BufferedInputStream(in);
        saxParser.parse(in, splitPointHandler);
      }

      /**
       * handle everything else
       */

      // make the parser handler
      DefaultHandler handler = parseXMLDocumentSizeMap();

      // start parsing
      SAXParserFactory factoryMain = SAXParserFactory.newInstance();
      factoryMain.setNamespaceAware(true);
      SAXParser saxParser = factoryMain.newSAXParser();
      InputStream in = new FileInputStream(settings.storiesFileName);
      if (settings.storiesFileName.endsWith(".gz")) {
        in = new GZIPInputStream(in);
      }
      in = new BufferedInputStream(in);
      saxParser.parse(in, handler);

      // add to "All Other Code" if none of the special categories apply
      for (SizeBreakdown breakdown : GlobalInformation.allSizeBreakdowns()) {
        updateAllOtherCodeType(breakdown.nameToCodeColl);
      }

      // now we need to aggregate numbers
      GlobalInformation.computePackageSizes();

      // clean up the RPC categories
      for (SizeBreakdown breakdown : GlobalInformation.allSizeBreakdowns()) {
        foldInRPCHeuristic(breakdown.nameToCodeColl);
      }

      // generate all the html files
      makeTopLevelHtmlForPerm.makeSplitStatusPages();
      makeTopLevelHtmlForPerm.makeLeftoverStatusPages();
      for (SizeBreakdown breakdown : GlobalInformation.allSizeBreakdowns()) {
        DependencyLinker linker = chooseDependencyLinker(
            makeTopLevelHtmlForPerm, breakdown);
        makeHTMLFiles(makeTopLevelHtmlForPerm, breakdown, linker);
      }

      System.out.println("Finished creating reports. To see the dashboard, open SoycDashboard-index.html in your browser.");

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
    } catch (Settings.ArgumentListException e) {
      System.err.println(e.getMessage());
      System.err.println("Usage: java com.google.gwt.soyc.SoycDashboard options stories0.xml[.gz] [dependencies0.xml[.gz]] [splitpoints0.xml[.gz]]");
      System.err.println("Options:");
      System.err.println(Settings.settingsHelp());
      System.exit(1);
    }
  }

  private static Collection<SizeBreakdown> breakdownsForFragment(
      Integer fragment) {
    List<SizeBreakdown> breakdowns = new ArrayList<SizeBreakdown>();
    breakdowns.add(GlobalInformation.totalCodeBreakdown);
    if (fragment == 0) {
      breakdowns.add(GlobalInformation.initialCodeBreakdown);
    }
    if (fragment == (GlobalInformation.numSplitPoints + 1)) {
      breakdowns.add(GlobalInformation.leftoversBreakdown);
    }
    if (fragment >= 1 && fragment <= GlobalInformation.numSplitPoints) {
      breakdowns.add(GlobalInformation.splitPointCodeBreakdown(fragment));
    }
    return breakdowns;
  }

  private static DependencyLinker chooseDependencyLinker(
      MakeTopLevelHtmlForPerm makeTopLevelHtmlForPerm, SizeBreakdown breakdown) {
    if (breakdown == GlobalInformation.totalCodeBreakdown) {
      return makeTopLevelHtmlForPerm.new DependencyLinkerForTotalBreakdown();
    } else if (breakdown == GlobalInformation.initialCodeBreakdown) {
      return makeTopLevelHtmlForPerm.new DependencyLinkerForInitialCode();
    } else if (breakdown == GlobalInformation.leftoversBreakdown) {
      return makeTopLevelHtmlForPerm.new DependencyLinkerForLeftoversFragment();
    } else {
      return makeTopLevelHtmlForPerm.new DependencyLinkerForExclusiveFragment();
    }
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

  /**
   * generates all the HTML files for one size breakdown
   */
  private static void makeHTMLFiles(
      MakeTopLevelHtmlForPerm makeTopLevelHtmlForPerm, SizeBreakdown breakdown,
      DependencyLinker depLinker) throws IOException {
    makeTopLevelHtmlForPerm.makePackageClassesHtmls(breakdown, depLinker);
    makeTopLevelHtmlForPerm.makeCodeTypeClassesHtmls(breakdown);
    makeTopLevelHtmlForPerm.makeLiteralsClassesTableHtmls(breakdown);
    makeTopLevelHtmlForPerm.makeBreakdownShell(breakdown);
    makeTopLevelHtmlForPerm.makeTopLevelShell();
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
          String tagName, Attributes attributes) {

        valueBuilder.delete(0, valueBuilder.length());

        if (strippedName.compareTo("table") == 0
            && (attributes.getValue("name") != null)) {
          String name = attributes.getValue("name");
          dependencies = new TreeMap<String, String>();
          allDependencies.put(name, dependencies);
          if (attributes.getValue("extends") != null) {
            graphExtends = attributes.getValue("extends");
            if (!allDependencies.containsKey(graphExtends)) {
              throw new FormatException("Graph " + name
                  + " extends an unknown graph " + graphExtends);
            }
          } else {
            graphExtends = null;
          }
        } else if ((strippedName.compareTo("method") == 0)
            && (attributes.getValue("name") != null)) {
          curMethod = attributes.getValue("name");
        } else if ((strippedName.compareTo("called") == 0)
            && (attributes.getValue("by") != null)) {
          String curDepMethod = attributes.getValue("by");
          if (!dependencies.containsKey(curMethod)) {
            dependencies.put(curMethod, curDepMethod);
          }
        }
      }
    };
    return handler;
  }

  private static DefaultHandler parseXMLDocumentSizeMap() {
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
          Attributes attributes) {
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
          recordSize(type, ref, size);
        }
      }

      private void accountForSize(SizeBreakdown breakdown, String refType,
          String ref, int size) {
        if (refType.equals("string")) {
          LiteralsCollection stringLiterals = breakdown.nameToLitColl.get("string");
          stringLiterals.size += size;
          stringLiterals.literals.add(ref);
        } else if (refType.equals("var")) {
          // Nothing to record, because no breakdown is provided for random
          // variables
        } else {
          if (!refType.equals("type") && !refType.equals("method")) {
            throw new FormatException();
          }
          String className = ref;
          if (className.contains("::")) {
            /*
             * It's a method reference. Discard the method part.
             */
            int idx = className.indexOf(':');
            className = className.substring(0, idx);
          }

          // derive the package name from the class
          String packageName;
          if (!GlobalInformation.classToPackage.containsKey(className)) {
            packageName = className;
            packageName = packageName.replaceAll("\\.[A-Z].*", "");
            GlobalInformation.classToPackage.put(className, packageName);
          } else {
            packageName = GlobalInformation.classToPackage.get(className);
          }
          if (!GlobalInformation.packageToClasses.containsKey(packageName)) {
            TreeSet<String> insertSet = new TreeSet<String>();
            insertSet.add(className);
            GlobalInformation.packageToClasses.put(packageName, insertSet);
          } else {
            GlobalInformation.packageToClasses.get(packageName).add(className);
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

      private void recordSize(String refType, String ref, int size) {
        for (SizeBreakdown breakdown : breakdownsForFragment(fragment)) {
          accountForSize(breakdown, refType, ref, size);
        }
      }
    };
  }

  private static DefaultHandler parseXMLDocumentSplitPoints() {

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
          String tagName, Attributes attributes) {
        if (strippedName.compareTo("splitpoint") == 0) {
          parseSplitPoint(attributes);
        } else if (strippedName.compareTo("initialseq") == 0) {
          inInitialLoadSequence = true;
        } else if (inInitialLoadSequence
            && strippedName.compareTo("splitpointref") == 0) {
          GlobalInformation.splitPointInitialLoadSequence.add(parseSplitPointReference(attributes));
        }
      }

      /*
       * parses the split points
       */
      private void parseSplitPoint(Attributes attributes) {
        if (attributes.getValue("id") != null) {
          String curSplitPoint = attributes.getValue("id");
          if (attributes.getValue("location") != null) {
            String curSplitPointLocation = attributes.getValue("location");

            curSplitPointLocation = curSplitPointLocation.replaceAll("\\(L.*",
                "");

            GlobalInformation.splitPointToLocation.put(
                Integer.parseInt(curSplitPoint), curSplitPointLocation);
            GlobalInformation.numSplitPoints++;
          }
        }
      }

      private Integer parseSplitPointReference(Attributes attributes) {
        String spString = attributes.getValue("id");
        if (spString == null) {
          throw new FormatException("Could not parse split point reference");
        }
        return Integer.valueOf(spString);
      }

    };
    return handler;
  }

  /*
   * assigns code to "all other code" if none of the special categories apply
   */
  private static void updateAllOtherCodeType(
      final HashMap<String, CodeCollection> nameToCodeColl) {
    // all classes not in any of the other categories
    for (String className : GlobalInformation.classToPackage.keySet()) {
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
