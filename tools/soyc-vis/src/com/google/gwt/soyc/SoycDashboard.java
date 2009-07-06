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
import java.util.HashSet;
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
    public FormatException(String message) {
      super(message);
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
      DefaultHandler handler = parseXMLDocument();

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
      GlobalInformation.computePartialPackageSizes();

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
    makeTopLevelHtmlForPerm.makeStringLiteralsClassesTableHtmls(breakdown);
    makeTopLevelHtmlForPerm.makeBreakdownShell(breakdown);
    makeTopLevelHtmlForPerm.makeTopLevelShell();
  }

  private static DefaultHandler parseXMLDocument() {
    DefaultHandler handler = new DefaultHandler() {
      String curClassId;
      Integer curFragment;
      String curLineNumber;
      String curLocation;
      HashSet<String> curRelevantCodeTypes = new HashSet<String>();
      HashSet<String> curRelevantLitTypes = new HashSet<String>();
      String curStoryId;
      String curStoryLiteralType;
      String curStoryRef;
      boolean specialCodeType = false;
      StringBuilder valueBuilder = new StringBuilder();

      /**
       * This method collects a block of the value of the current XML node that
       * the SAX parser parses. It simply adds to the the previous blocks, so
       * that we can collect the entire value block.
       */
      @Override
      public void characters(char ch[], int start, int length) {
        valueBuilder.append(ch, start, length);
      }

      /**
       * This method marks the end of an XML element that the SAX parser parses.
       * It has access to the full value of the node and uses it to add
       * information to the relevant literal or code collections.
       * 
       * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
       *      java.lang.String, java.lang.String)
       */
      @Override
      public void endElement(String nsUri, String strippedName, String qName) {

        if (strippedName.compareTo("storyref") == 0) {
          String value = valueBuilder.toString();

          int numBytes = currentStorySize();
          if (curStoryRef != null) {
            if (!GlobalInformation.fragmentToPartialSize.containsKey(curFragment)) {
              GlobalInformation.fragmentToPartialSize.put(curFragment,
                  (float) numBytes);
            } else {
              float newSize = GlobalInformation.fragmentToPartialSize.get(curFragment)
                  + numBytes;
              GlobalInformation.fragmentToPartialSize.put(curFragment, newSize);
            }

            for (SizeBreakdown breakdown : breakdownsForCurFragment()) {
              breakdown.sizeAllCode += numBytes;
            }

            // add this size to the classes associated with it
            if (GlobalInformation.storiesToCorrClasses.containsKey(curStoryRef)) {

              if ((GlobalInformation.storiesToLitType.containsKey(curStoryRef))
                  && (GlobalInformation.storiesToCorrClasses.get(curStoryRef).size() > 0)) {
                GlobalInformation.numBytesDoubleCounted += numBytes;
              }

              for (SizeBreakdown breakdown : breakdownsForCurFragment()) {
                accountForCurrentStory(breakdown.nameToCodeColl, breakdown);
              }
            }

            for (SizeBreakdown breakdown : breakdownsForCurFragment()) {
              updateLitTypes(breakdown.nameToLitColl, value, numBytes);
            }
          }
        }
      }

      /**
       * This method deals with the beginning of the XML element. It analyzes
       * the XML node and adds its information to the relevant literal or code
       * collection for later analysis.
       * 
       * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
       *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
       */
      @Override
      public void startElement(String nsUri, String strippedName,
          String tagName, Attributes attributes) {
        valueBuilder.delete(0, valueBuilder.length());

        if (strippedName.compareTo("story") == 0) {
          parseStory(attributes);
        } else if (strippedName.compareTo("of") == 0) {
          for (SizeBreakdown breakdown : breakdownsForCurFragment()) {
            parseOverrides(breakdown.nameToCodeColl, attributes);
          }
        } else if (strippedName.compareTo("by") == 0) {
          for (SizeBreakdown breakdown : breakdownsForCurFragment()) {
            parseCorrelations(breakdown.nameToCodeColl, attributes);
          }
        } else if (strippedName.compareTo("origin") == 0) {
          for (SizeBreakdown breakdown : breakdownsForCurFragment()) {
            parseOrigins(breakdown.nameToLitColl, attributes);
          }
        } else if (strippedName.compareTo("js") == 0) {
          if (attributes.getValue("fragment") != null) {
            curFragment = Integer.parseInt(attributes.getValue("fragment"));
          } else {
            curFragment = -2;
          }
        } else if (strippedName.compareTo("storyref") == 0) {
          for (SizeBreakdown breakdown : breakdownsForCurFragment()) {
            parseJs(breakdown.nameToLitColl, breakdown.nameToCodeColl,
                attributes, curFragment);
          }
        }
      }

      private void accountForCurrentStory(
          final HashMap<String, CodeCollection> nameToCodeColl,
          SizeBreakdown breakdown) {
        int storySize = currentStorySize();
        if ((!GlobalInformation.storiesToLitType.containsKey(curStoryRef))
            && (!GlobalInformation.storiesToCorrClasses.containsKey(curStoryRef))) {
          breakdown.nonAttributedStories.add(curStoryRef);
          breakdown.nonAttributedBytes += storySize;
        }

        // go through all the classes for this story
        for (String className : GlobalInformation.storiesToCorrClasses.get(curStoryRef)) {
          // get the corresponding package

          String packageName = "";

          if (!GlobalInformation.classToPackage.containsKey(className)) {
            // derive the package name from the class
            packageName = className;
            packageName = packageName.replaceAll("\\.[A-Z].*", "");
            GlobalInformation.classToPackage.put(className, packageName);
          } else {
            packageName = GlobalInformation.classToPackage.get(className);
          }
          parseClass(nameToCodeColl, className, packageName);

          if (!GlobalInformation.packageToClasses.containsKey(packageName)) {
            TreeSet<String> insertSet = new TreeSet<String>();
            insertSet.add(className);
            GlobalInformation.packageToClasses.put(packageName, insertSet);
          } else {
            GlobalInformation.packageToClasses.get(packageName).add(className);
          }

          if (breakdown.classToSize.containsKey(className)) {
            int newSize = breakdown.classToSize.get(className) + storySize;
            breakdown.classToSize.put(className, newSize);
          } else {
            breakdown.classToSize.put(className, storySize);
          }

          if (breakdown.classToPartialSize.containsKey(className)) {
            float newSize = breakdown.classToPartialSize.get(className)
                + currentStoryPartialSize();
            breakdown.classToPartialSize.put(className, newSize);
          } else {
            breakdown.classToPartialSize.put(className,
                currentStoryPartialSize());
          }
        }
      }

      private Collection<SizeBreakdown> breakdownsForCurFragment() {
        List<SizeBreakdown> breakdowns = new ArrayList<SizeBreakdown>();
        breakdowns.add(GlobalInformation.totalCodeBreakdown);
        if (curFragment == 0) {
          breakdowns.add(GlobalInformation.initialCodeBreakdown);
        }
        if (curFragment == (GlobalInformation.numSplitPoints + 1)) {
          breakdowns.add(GlobalInformation.leftoversBreakdown);
        }
        if (curFragment >= 1 && curFragment <= GlobalInformation.numSplitPoints) {
          breakdowns.add(GlobalInformation.splitPointCodeBreakdown(curFragment));
        }
        return breakdowns;
      }

      private float currentStoryPartialSize() {
        return (float) currentStorySize()
            / (float) GlobalInformation.storiesToCorrClasses.get(curStoryRef).size();
      }

      private int currentStorySize() {
        return valueBuilder.toString().getBytes().length;
      }

      /*
       * parses the "class" portion of the XML file
       */
      private void parseClass(
          final HashMap<String, CodeCollection> nameToCodeColl,
          String curClassId, String curPackage) {
        // if (attributes.getValue("id") != null) {
        // curClassId = attributes.getValue("id");

        // GlobalInformation.classToPackage.put(curClassId, curPackage);

        if (curPackage.startsWith("java")) {
          nameToCodeColl.get("jre").classes.add(curClassId);
        } else if (curPackage.startsWith("com.google.gwt.lang")) {
          nameToCodeColl.get("gwtLang").classes.add(curClassId);
        }
        if (curClassId.contains("_CustomFieldSerializer")) {
          nameToCodeColl.get("rpcUser").classes.add(curClassId);
        } else if (curClassId.endsWith("_FieldSerializer")
            || curClassId.endsWith("_Proxy")
            || curClassId.endsWith("_TypeSerializer")) {
          nameToCodeColl.get("rpcGen").classes.add(curClassId);
        }
        // }
      }

      /*
       * parses the "correlations" portion of the XML file
       */
      private void parseCorrelations(
          final HashMap<String, CodeCollection> nameToCodeColl,
          Attributes attributes) {

        if (attributes.getValue("idref") != null) {

          String corrClassOrMethod = attributes.getValue("idref");
          String corrClass = attributes.getValue("idref");

          if (corrClass.contains(":")) {
            corrClass = corrClass.replaceAll(":.*", "");
          }

          if (!GlobalInformation.storiesToCorrClassesAndMethods.containsKey(curStoryId)) {
            HashSet<String> insertSet = new HashSet<String>();
            insertSet.add(corrClassOrMethod);
            GlobalInformation.storiesToCorrClassesAndMethods.put(curStoryId,
                insertSet);
          } else {
            GlobalInformation.storiesToCorrClassesAndMethods.get(curStoryId).add(
                corrClassOrMethod);
          }

          if (!GlobalInformation.storiesToCorrClasses.containsKey(curStoryId)) {
            HashSet<String> insertSet = new HashSet<String>();
            insertSet.add(corrClass);
            GlobalInformation.storiesToCorrClasses.put(curStoryId, insertSet);
          } else {
            GlobalInformation.storiesToCorrClasses.get(curStoryId).add(
                corrClass);
          }

          for (String codeType : nameToCodeColl.keySet()) {
            if (nameToCodeColl.get(codeType).classes.contains(corrClass)) {
              nameToCodeColl.get(codeType).stories.add(curStoryId);
            }
          }
        }
      }

      /*
       * parses the "JS" portion of the XML file
       */
      private void parseJs(final Map<String, LiteralsCollection> nameToLitColl,
          final HashMap<String, CodeCollection> nameToCodeColl,
          Attributes attributes, Integer curFragment) {
        curRelevantLitTypes.clear();
        curRelevantCodeTypes.clear();

        if (attributes.getValue("idref") != null) {

          curStoryRef = attributes.getValue("idref");

          if (curFragment != -1) {
            // add this to the stories for this fragment
            if (!GlobalInformation.fragmentToStories.containsKey(curFragment)) {
              HashSet<String> insertSet = new HashSet<String>();
              insertSet.add(curStoryRef);
              GlobalInformation.fragmentToStories.put(curFragment, insertSet);
            } else {
              GlobalInformation.fragmentToStories.get(curFragment).add(
                  curStoryRef);
            }
          }

          for (String litType : nameToLitColl.keySet()) {
            if (nameToLitColl.get(litType).storyToLocations.containsKey(curStoryRef)) {
              curRelevantLitTypes.add(litType);
            }
          }

          specialCodeType = false;
          for (String codeType : nameToCodeColl.keySet()) {
            if (nameToCodeColl.get(codeType).stories.contains(curStoryRef)) {
              curRelevantCodeTypes.add(codeType);
              specialCodeType = true;
            }
          }
          if (specialCodeType == false) {

            nameToCodeColl.get("allOther").stories.add(curStoryRef);
            curRelevantCodeTypes.add("allOther");
          }
        }
      }

      /*
       * parses the "origins" portion of the XML file
       */
      private void parseOrigins(
          final Map<String, LiteralsCollection> nameToLitColl,
          Attributes attributes) {
        if ((curStoryLiteralType.compareTo("") != 0)
            && (attributes.getValue("lineNumber") != null)
            && (attributes.getValue("location") != null)) {
          curLineNumber = attributes.getValue("lineNumber");
          curLocation = attributes.getValue("location");
          String curOrigin = curLocation + ": Line " + curLineNumber;

          if (!nameToLitColl.get(curStoryLiteralType).storyToLocations.containsKey(curStoryId)) {
            HashSet<String> insertSet = new HashSet<String>();
            insertSet.add(curOrigin);
            nameToLitColl.get(curStoryLiteralType).storyToLocations.put(
                curStoryId, insertSet);
          } else {
            nameToLitColl.get(curStoryLiteralType).storyToLocations.get(
                curStoryId).add(curOrigin);
          }
        }
      }

      /*
       * parses the "overrides" portion of the XML file
       */
      private void parseOverrides(
          final HashMap<String, CodeCollection> nameToCodeColl,
          Attributes attributes) {
        if (attributes.getValue("idref") != null) {
          String overriddenClass = attributes.getValue("idref");

          // we either generalize to classes, or the
          // numbers are messed up...
          if (overriddenClass.contains(":")) {
            overriddenClass = overriddenClass.replaceAll(":.*", "");
          }

          if (overriddenClass.compareTo("com.google.gwt.user.client.ui.UIObject") == 0) {
            nameToCodeColl.get("widget").classes.add(curClassId);
          } else if (overriddenClass.contains("java.io.Serializable")
              || overriddenClass.contains("IsSerializable")) {
            nameToCodeColl.get("rpcUser").classes.add(curClassId);
          } else if (overriddenClass.contains("com.google.gwt.user.client.rpc.core.java")) {
            nameToCodeColl.get("rpcGwt").classes.add(curClassId);
          }
        }
      }

      /*
       * parses the "story" portion of the XML file
       */
      private void parseStory(Attributes attributes) {
        if (attributes.getValue("id") != null) {
          curStoryId = attributes.getValue("id");
          if (attributes.getValue("literal") != null) {
            curStoryLiteralType = attributes.getValue("literal");
            GlobalInformation.storiesToLitType.put(curStoryId,
                curStoryLiteralType);
            for (SizeBreakdown breakdown : breakdownsForCurFragment()) {
              if (!breakdown.nameToLitColl.get(curStoryLiteralType).storyToLocations.containsKey(curStoryId)) {
                HashSet<String> insertSet = new HashSet<String>();
                breakdown.nameToLitColl.get(curStoryLiteralType).storyToLocations.put(
                    curStoryId, insertSet);
              }
            }
          } else {
            curStoryLiteralType = "";
          }
        }
      }

      /*
       * This method assigns strings to the appropriate category
       */
      private void updateLitTypes(
          final Map<String, LiteralsCollection> nameToLitColl, String value,
          int numBytes) {

        int iNumCounted = 0;

        for (String relLitType : curRelevantLitTypes) {
          iNumCounted++;

          // then give string literals special treatment
          if (relLitType.compareTo("string") == 0) {

            // note that this will double-count (i.e., it will count a string
            // twice if it's in the output twice), as it should.
            nameToLitColl.get("string").cumStringSize += numBytes;
            nameToLitColl.get(relLitType).cumSize += numBytes;

            // get the origins
            HashSet<String> originSet = nameToLitColl.get("string").storyToLocations.get(curStoryRef);

            // find the most appropriate string literal category
            String mostAppropriateCategory = "";
            String mostAppropriateLocation = "";
            String backupLocation = "";
            for (String origin : originSet) {

              if ((origin.contains("ClassLiteralHolder"))
                  && (mostAppropriateCategory.compareTo("") == 0)) {
                mostAppropriateCategory = "compiler";
                mostAppropriateLocation = origin;
              } else if ((origin.startsWith("transient source for"))
                  && (origin.contains("_TypeSerializer"))
                  && (mostAppropriateCategory.compareTo("") == 0)) {
                mostAppropriateCategory = "transient";
                mostAppropriateLocation = origin;
              } else if ((origin.contains("InlineResourceBundleGenerator"))
                  && (mostAppropriateCategory.compareTo("") == 0)) {
                mostAppropriateCategory = "inlinedTextRes";
                mostAppropriateLocation = origin;
              }
              if (origin.compareTo("com.google.gwt.dev.js.ast.JsProgram: Line 0") != 0) {
                backupLocation = origin;
              }
            }

            if (backupLocation.compareTo("") == 0) {
              backupLocation = GlobalInformation.backupLocation;
            }
            if ((((value.startsWith("'")) && (value.endsWith("'"))) || ((value.startsWith("\"")) && (value.endsWith("\""))))
                && (mostAppropriateCategory.compareTo("") == 0)) {
              mostAppropriateCategory = "user";
              mostAppropriateLocation = backupLocation;
            } else if (mostAppropriateCategory.compareTo("") == 0) {
              mostAppropriateCategory = "otherStrings";
              mostAppropriateLocation = backupLocation;
            }

            if (!nameToLitColl.get("string").stringLiteralToType.containsKey(value)) {
              nameToLitColl.get("string").stringLiteralToType.put(value,
                  mostAppropriateCategory);
              if (!nameToLitColl.get("string").stringTypeToCount.containsKey(mostAppropriateCategory)) {
                nameToLitColl.get("string").stringTypeToCount.put(
                    mostAppropriateCategory, 1);
              } else {
                int iNewCount = nameToLitColl.get("string").stringTypeToCount.get(mostAppropriateCategory) + 1;
                nameToLitColl.get("string").stringTypeToCount.put(
                    mostAppropriateCategory, iNewCount);
              }

              int iNewSize = numBytes;
              if (nameToLitColl.get("string").stringTypeToSize.containsKey(mostAppropriateCategory)) {
                iNewSize += nameToLitColl.get("string").stringTypeToSize.get(mostAppropriateCategory);
              }
              nameToLitColl.get("string").stringTypeToSize.put(
                  mostAppropriateCategory, iNewSize);

              if (nameToLitColl.get("string").storyToLocations.containsKey(curStoryRef)) {
                HashSet<String> insertSet = new HashSet<String>();
                insertSet.add(mostAppropriateLocation);
                nameToLitColl.get(relLitType).literalToLocations.put(value,
                    insertSet);
              }
            }
          } else {
            // note that this will double-count (i.e., it will count a literal
            // twice if it's in the output twice), as it should.
            nameToLitColl.get(relLitType).cumSize += numBytes;

            if (nameToLitColl.get(relLitType).storyToLocations.containsKey(curStoryRef)) {
              if (nameToLitColl.get(relLitType).literalToLocations.containsKey(value)) {
                nameToLitColl.get(relLitType).literalToLocations.get(value).addAll(
                    nameToLitColl.get(relLitType).storyToLocations.get(curStoryRef));
              } else {
                HashSet<String> insertSet = nameToLitColl.get(relLitType).storyToLocations.get(curStoryRef);
                nameToLitColl.get(relLitType).literalToLocations.put(value,
                    insertSet);
              }
            }
          }
        }
      }

      /*
       * parses the "depends on" portion of the XML file
       */
      /*
       * private void parseDependsOn( final HashMap<String, CodeCollection>
       * nameToCodeColl, Attributes attributes) { if
       * (curFunctionId.compareTo("") == 0) { if (attributes.getValue("idref")
       * != null) { String curDepClassId = attributes.getValue("idref");
       * 
       * if (curDepClassId.contains(":")) { // strip everything after the :: (to
       * get to class, even if it's a // method) curDepClassId =
       * curDepClassId.replaceAll(":.*", ""); }
       * 
       * if (curDepClassId.contains(".")) { if
       * (!GlobalInformation.classToWhatItDependsOn.containsKey(curClassId)) {
       * HashSet<String> insertSet = new HashSet<String>();
       * insertSet.add(curDepClassId);
       * GlobalInformation.classToWhatItDependsOn.put(curClassId, insertSet); }
       * else { GlobalInformation.classToWhatItDependsOn.get(curClassId).add(
       * curDepClassId); } } } } }
       */
    };
    return handler;
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
