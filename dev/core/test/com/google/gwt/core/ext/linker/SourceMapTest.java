/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.core.ext.linker;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.soyc.SourceMapRecorderExt;
import com.google.gwt.core.ext.soyc.coderef.ClassDescriptor;
import com.google.gwt.core.ext.soyc.coderef.EntityDescriptor;
import com.google.gwt.core.ext.soyc.coderef.EntityDescriptor.Fragment;
import com.google.gwt.core.ext.soyc.coderef.EntityDescriptorJsonTranslator;
import com.google.gwt.core.ext.soyc.coderef.EntityRecorder;
import com.google.gwt.core.ext.soyc.coderef.MethodDescriptor;
import com.google.gwt.dev.CompilerOptionsImpl;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.thirdparty.debugging.sourcemap.FilePosition;
import com.google.gwt.thirdparty.debugging.sourcemap.SourceMapConsumerV3;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.primitives.Ints;
import com.google.gwt.util.tools.Utility;

import junit.framework.TestCase;

import org.eclipse.jdt.internal.compiler.problem.ShouldNotImplement;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * Basic tests for Source maps and (new) soyc reports.
 *
 */
public class SourceMapTest extends TestCase {

  private static String stringContent(File filePath) throws IOException {
    FileReader reader = new FileReader(filePath);
    char[] content = new char[(int) filePath.length()];
    reader.read(content);
    reader.close();
    return new String(content);
  }

  private static File[] filterByName(File dir, final String namePattern) {
    return dir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.getName().matches(namePattern);
      }
    });
  }

  /**
   * This class represents each row in a generated SymbolMap file.  Because not all fields are
   * serialized, such as CastableTypeMap, some methods are not implemented.
   *
   */
  static final class SimpleSymbolData implements SymbolData {

    static Map<String, SimpleSymbolData> readSymbolMap(File filePath) throws IOException {
      Map<String, SimpleSymbolData> sdata = Maps.newLinkedHashMap();

      BufferedReader reader = new BufferedReader(new FileReader(filePath));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("#")) {
          // reading a comment
          continue;
        }

        SimpleSymbolData symbolData = new SimpleSymbolData(line);
        String key = symbolData.getJsniIdent();
        assertFalse("Duplicate signature <" + key + "> in symbol maps", sdata.containsKey(key));
        sdata.put(key,symbolData);
      }

      return sdata;
    }

    private static final String NOT_IMPLEMENTED_MESSAGE =
        "Data not available in current serialized SymbolMap";

    private String jsName;
    private String jsniIdent;
    private String className;
    private String memberName;
    private String sourceUri;
    private int sourceLine;
    private int fragmentNumber;
    // counting how many times it was found in the symbol map table
    private int counter = 0;

    public SimpleSymbolData(String line) {
      this.parseFromLine(line);
    }

    public int getCounter() {
      return counter;
    }

    @Override
    public CastableTypeMap getCastableTypeMap() {
      return null;
    }

    @Override
    public String getClassName() {
      return this.className;
    }

    @Override
    public int getFragmentNumber() {
      return this.fragmentNumber;
    }

    @Override
    public String getJsniIdent() {
      return this.jsniIdent;
    }

    @Override
    public String getMemberName() {
      return this.memberName;
    }

    @Override
    public String getRuntimeTypeId() {
      throw new ShouldNotImplement(NOT_IMPLEMENTED_MESSAGE);
    }

    @Override
    public int getSourceLine() {
      return this.sourceLine;
    }

    @Override
    public String getSourceUri() {
      return this.sourceUri;
    }

    @Override
    public String getSymbolName() {
      return this.jsName;
    }

    @Override
    public boolean isClass() {
      return this.memberName == null || this.memberName.isEmpty();
    }

    @Override
    public boolean isField() {
      return !this.isClass() && jsniIdent.indexOf("(") < 0;
    }

    @Override
    public boolean isMethod() {
      return !this.isClass() && jsniIdent.indexOf("(") >= 0;
    }

    public int incCounter() {
      return ++counter;
    }

    @Override
    public String toString() {
      return jsniIdent + " -> " + jsName;
    }

    private void parseFromLine(String line) {
      String[] fields = line.split(",");

      this.jsName = fields[0];
      this.jsniIdent = fields[1].isEmpty() ? fields[2] : fields[1];
      this.className = fields[2];
      this.memberName = fields[3]; // may be empty
      this.sourceUri = fields[4];
      this.sourceLine = Integer.parseInt(fields[5]);
      this.fragmentNumber = Integer.parseInt(fields[6]);
    }
  }

  private final CompilerOptionsImpl options = new CompilerOptionsImpl();
  // maps permutationId to symbolMap content
  private Map<Integer, Map<String, SimpleSymbolData>> mapping =
      Maps.newHashMap();

  /**
   * Test the correspondence between old symbol maps and the information (such as range name and
   * source position) that is now provided by sourcemap.
   *
   * The matching is far from perfect. SymbolMaps record 1-1 correspondences between symbols in
   * JavaScript and the optimized version of the Java source. The symbol mapping information
   * provided in the sourcemap extensions maps the original Java source symbols to their JavaScript
   * counterpart but the mapping is no longer 1-1. E.g. A source java method might have two versions
   * after optimization due to MakeStaticCalls; and each of those versions might have different
   * JavaScript names.
   *
   * Also correspondence on field accesses, class literals can not be tested.
   *
   */
  private void checkSourceMap(File symbolMap, List<File> sourceMapFiles)
      throws Exception {
    final Map<String, SimpleSymbolData> symbolTable = SimpleSymbolData.readSymbolMap(symbolMap);
    boolean firstIteration = true;
    for (File sourceMapFile : sourceMapFiles) {
      SourceMapConsumerV3 sourceMap = new SourceMapConsumerV3();
      sourceMap.parse(stringContent(sourceMapFile));
      if (firstIteration) {
        mapping.put((Integer) sourceMap.getExtensions().get(SourceMapRecorderExt.PERMUTATION_EXT),
            symbolTable);
        firstIteration = false;
      }
      sourceMap.visitMappings(new  SourceMapConsumerV3.EntryVisitor() {
        @Override
        public void visit(String sourceName, String symbolName,
            FilePosition srcStartPos, FilePosition startPosition,FilePosition endPosition) {
          if (symbolName == null || symbolName.isEmpty()) {
            return;
          }
          SimpleSymbolData symbolData = symbolTable.get(symbolName);
          if (symbolData == null) {
            return;
          }
          symbolData.incCounter();
          // field declarations will work, but field accesses wont
          if (!symbolData.isField()) {
            assertEquals(symbolData.getSourceUri(), sourceName);
            if (symbolData.isClass()) {
              if (symbolData.getFragmentNumber() >= 0) {
                assertEquals(symbolData.getSourceLine() - 1, srcStartPos.getLine());
              } // Some classes on fragment -1 (interfaces) wont work.
            } else {
              if (symbolData.getCounter() == 0) {
                assertTrue(Math.abs(symbolData.getSourceLine() - srcStartPos.getLine()) <= 1);
                // Some methods wont work on source line. They were generated from the
                // parent SourceInfo
              }
            }
          }
        }
      });
    }
  }

  private void testSymbolMapsCorrespondence(File root) throws Exception {
    // Testing SourceMaps as SymbolMap replacement
    // make sure the files have been produced
    assertTrue(root.exists());

    File[] symbolMapFiles = filterByName(root, "(.*)\\.symbolMap")
        ,  sourceMapFiles = filterByName(root, "(.*)_sourceMap(\\d+)\\.json");
    // At least there is a source map file for each symbol map file
    assertTrue(symbolMapFiles.length <= sourceMapFiles.length);

    List<List<File>> sourceMapSets = Lists.newArrayList();
    for (int i = 0; i < symbolMapFiles.length; i++) {
      String name = symbolMapFiles[i].getName().split("\\.")[0];
      List<File> set =  Lists.newArrayList();
      for (File sourceMap : sourceMapFiles) {
        if (sourceMap.getName().startsWith(name)) {
          set.add(sourceMap);
        }
      }
      assertTrue(set.size() >= 1);
      sourceMapSets.add(set);
    }
    for (int i = 0; i < symbolMapFiles.length; i++) {
      checkSourceMap(symbolMapFiles[i], sourceMapSets.get(i));
    }
  }

  private void testSoycCorrespondence(File root) throws Exception {
    // Testing SourceMap as Soyc reports replacements
    assertTrue(root.exists());

    for (Integer permutation : mapping.keySet()) {

      checkSplitPloints(
          new File(root.getPath() + "/splitPoints" + permutation + ".xml.gz"),
          new File(root.getPath() + "/fragments"   + permutation + ".json"));
      checkEntities(
          new File(root.getPath() + "/stories"      + permutation + ".xml.gz"),
          new File(root.getPath() + "/dependencies" + permutation + ".xml.gz"),
          mapping.get(permutation),
          new File(root.getPath() + "/" + EntityRecorder.ENTITIES + permutation + ".json"));
    }
  }

  private void checkEntities(File sizeMap, File dependency,
      Map<String, SimpleSymbolData> symbolTable, File entitiesFile)
      throws Exception {
    Map<String, ClassDescriptor> clsMap =
          EntityDescriptorJsonTranslator.readJson(new JSONObject(stringContent(entitiesFile)))
              .getAllClassesByName();
    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

    parser.reset();
    parser.parse(new GZIPInputStream(new FileInputStream(sizeMap)), checkStories(clsMap));

    parser.reset();
    parser.parse(new GZIPInputStream(new FileInputStream(dependency)), checkDependencies(clsMap));

    checkSymbols(symbolTable, clsMap);
  }

  private void checkSymbols(Map<String, SimpleSymbolData> symbolTable,
      Map<String, ClassDescriptor> clsMap) {
    for (SimpleSymbolData symbol : symbolTable.values()) {
      if (symbol.getClassName().endsWith("[]")) {
        // Arrays aren't stored, because they are not entities, ie definable piece of code
        continue;
      }
      ClassDescriptor classDescriptor = clsMap.get(symbol.getClassName());
      if (classDescriptor == null) {
        // Few classes in symbol maps are not presented in the new report. This is because, they
        // don't contribute to fragment size nor appear in the dependency graph.
        continue;
      }
      if (symbol.isClass()) {
        assertTrue(classDescriptor.getObfuscatedNames().contains(symbol.getSymbolName()));
      } else if (symbol.isField()) {
        assertTrue(classDescriptor.getField(symbol.getMemberName()).getObfuscatedNames()
            .contains(symbol.getSymbolName()));
      } else {
        // method
        MethodDescriptor mth = classDescriptor.getMethod(
            unSynthMethodSignature(symbol.getJsniIdent().split("::")[1]));
        assertTrue(mth.getObfuscatedNames().contains(symbol.getSymbolName()));
      }
    }
  }

  private static String unSynthMethodSignature(String mthSignature) {
    if (mthSignature.startsWith("$") &&
        !mthSignature.startsWith("$init()") &&
        !mthSignature.startsWith("$clinit()")) {
      return mthSignature.replaceFirst("L[^;\\(]*;","").substring(1);
    }
    return mthSignature;
  }

  private DefaultHandler checkDependencies(
      final Map<String,ClassDescriptor> classDescriptorByName) {
    return new DefaultHandler() {
      Set<Integer> currentDependencies = Sets.newHashSet();
      // mName is just the method name, not a complete signature
      String methodName(String mName) {
        if (mName.startsWith("$") && !mName.equals("$init") && !mName.equals("$clinit")) {
          return mName.substring(1);
        }
        return mName;
      }

      boolean compareMethodNames(String strictName, String relaxName) {
        if (strictName.equals(relaxName)) {
          return true;
        }
        // only $init and $clinit will match the below if
        if (relaxName.startsWith("$")) {
          return strictName.equals(relaxName.substring(1));
        }
        return false;
      }

      @Override
      public void startElement(String uri, String localName, String qName, Attributes attributes)
          throws SAXException {
        super.startElement(uri, localName, qName, attributes);
        // "name"/"by" attributes wont include the signature, just the method name
        if (qName.equals("method")) {
          currentDependencies.clear();
          String[] fullName = attributes.getValue("name").split("::");
          for (MethodDescriptor method : classDescriptorByName.get(fullName[0]).getMethods()) {
            if (compareMethodNames(method.getName(), methodName(fullName[1]))) {
              currentDependencies.addAll(Ints.asList(method.getDependentPointers()));
            }
          }
          assertTrue(currentDependencies.size() > 0);
        } else if (qName.equals("called")) {
          assertTrue(currentDependencies.size() > 0);

          String[] fullName = attributes.getValue("by").split("::");
          boolean present = false;
          for (MethodDescriptor method : classDescriptorByName.get(fullName[0]).getMethods()) {
            if (compareMethodNames(method.getName(), methodName(fullName[1]))) {
              if (currentDependencies.contains(method.getUniqueId())) {
                present = true;
                break;
              }
            }
          }
          // We cannot do much, because of the orig dependencies.xml format impressions
          assertTrue(present);
        }
      }
    };
  }

  private DefaultHandler checkStories(final Map<String, ClassDescriptor> clsMap) {
    return new DefaultHandler() {
      int fragment = -1;
      @Override
      public void startElement(String uri, String localName, String qName, Attributes attributes)
          throws SAXException {
        super.startElement(uri, localName, qName, attributes);
        if (qName.equals("sizemap")) {
          fragment = Integer.parseInt(attributes.getValue("fragment"));
        } else if (qName.equals("size")) {
          assertTrue(fragment > -1);
          // <size type="type" ref="com.google.gwt.core.client.JavaScriptException" size="25"/>
          // eg. com.google.gwt.core.client.JavaScriptException::$clinit()V
          // type := type | method | field | string | var
          String kind = attributes.getValue("type");
          int size = Integer.parseInt(attributes.getValue("size"));
          String ref = attributes.getValue("ref");
          if (kind.equals("type")) {
            checkInFragments(size, clsMap.get(ref).getFragments());
          } else if (kind.equals("method")) {
            String[] fullName = ref.split("::");
            checkInFragments(size,
                clsMap.get(fullName[0])
                    .getMethod(unSynthMethodSignature(fullName[1])).getFragments());
          } else if (kind.equals("field")) {
            String[] fullName = ref.split("::");
            checkInFragments(size,
                clsMap.get(fullName[0]).getField(fullName[1]).getFragments());
          }
          // var and string are not recorded in entities
        }
      }

      // Checks that current fragment and size are in the list
      private void checkInFragments(int size, Collection<Fragment> fragments) {
        for (EntityDescriptor.Fragment frag : fragments) {
          if (frag.getId() == fragment &&
              frag.getSize() == size) {
            return;
          }
        }
        fail("Fragment <" + fragment + "> and size <" + size  + "> don't match");
      }
    };
  }

  private void checkSplitPloints(File origSplitPoints, File fragmentsFile)
      throws Exception {
    JSONObject jsPoints = new JSONObject(stringContent(fragmentsFile));
    final JSONArray initSeq = (JSONArray) jsPoints.opt(EntityRecorder.INITIAL_SEQUENCE);
    if (initSeq != null) {
      // Considering stable order on "initial sequence". May be this is too strict, in that case,
      // we need to store the elements in a list and provide a search method
      JSONArray fragments = (JSONArray) jsPoints.get(EntityRecorder.FRAGMENTS);
      final Map<Integer, JSONObject> fragmentById = Maps.newHashMap();
      for (int i = 0; i < fragments.length(); i++) {
        JSONObject spoint = fragments.getJSONObject(i);
        fragmentById.put(spoint.getInt(EntityRecorder.FRAGMENT_ID), spoint);
      }
      SAXParserFactory.newInstance().newSAXParser().parse(
          new GZIPInputStream(new FileInputStream(origSplitPoints)),
          new DefaultHandler() {
            int isIdx = 0;
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
              super.startElement(uri, localName, qName, attributes);
              try {
                if (localName.equals("splipoint")) {
                  JSONArray runAsyncs = fragmentById
                      .get(Integer.parseInt(attributes.getValue("id")))
                      .getJSONArray(EntityRecorder.FRAGMENT_POINTS);
                  boolean present = false;
                  String runAsync = attributes.getValue("location");
                  for (int i = 0; i < runAsyncs.length(); i++) {
                    if (runAsyncs.getString(i).equals(runAsync)) {
                      present = true;
                      break;
                    }
                  }
                  assertTrue(present);
                } else if (localName.equals("splitpointref")) {
                  assertEquals(Integer.parseInt(attributes.getValue("id")), initSeq.getInt(isIdx++));
                }
              } catch (JSONException ex) {
                fail(ex.getMessage());
              }
            }
          });
    }
  }

  public void testSourceMapT() throws Exception {
    String benchmark = "hello";
    String module = "com.google.gwt.sample.hello.Hello";

    File work = Utility.makeTemporaryDirectory(null, benchmark + "work");
    try {
      options.setSoycEnabled(true);
      options.setJsonSoycEnabled(true);
      options.addModuleName(module);
      options.setWarDir(new File(work, "war"));
      options.setExtraDir(new File(work, "extra"));
      PrintWriterTreeLogger logger = new PrintWriterTreeLogger();
      logger.setMaxDetail(TreeLogger.ERROR);
      new com.google.gwt.dev.Compiler(options).run(logger);
      // Change parentDir for cached/pre-built reports
      String parentDir = options.getExtraDir() + "/" + benchmark;
      testSymbolMapsCorrespondence(new File(parentDir + "/symbolMaps/"));
      testSoycCorrespondence(new File(parentDir + "/soycReport/"));

    } finally {
      Util.recursiveDelete(work, false);
    }
  }
}