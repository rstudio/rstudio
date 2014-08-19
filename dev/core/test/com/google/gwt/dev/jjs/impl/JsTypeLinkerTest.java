/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.linker.impl.JsSourceMapBuilder;
import com.google.gwt.core.ext.linker.impl.NamedRange;
import com.google.gwt.core.ext.linker.impl.StatementRangesBuilder;
import com.google.gwt.core.ext.soyc.Range;
import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.MinimalRebuildCache.PermutationRebuildCache;
import com.google.gwt.dev.jjs.JsSourceMap;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import junit.framework.TestCase;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Tests for {@link JsTypeLinker}.
 */
public class JsTypeLinkerTest extends TestCase {

  private int lines;

  public void testLink() {
    NamedRange programRange = new NamedRange("Program");
    NamedRange someModelARange = new NamedRange("com.some.app.SomeAModel");
    NamedRange someModelBRange = new NamedRange("com.some.app.SomeBModel");
    NamedRange someControllerRange = new NamedRange("com.some.app.SomeController");
    NamedRange entryPointRange = new NamedRange("com.some.app.EntryPoint");
    List<NamedRange> classRanges =
        Lists.newArrayList(someModelARange, someModelBRange, someControllerRange, entryPointRange);
    StatementRangesBuilder srb = new StatementRangesBuilder();
    JsSourceMapBuilder smb = new JsSourceMapBuilder();

    // Build the original JS and log boundaries.
    StringBuilder sb = new StringBuilder();
    appendStatement(sb, srb, smb, "<preamble>\n");
    appendStatement(sb, srb, smb, "<java.lang.Object />\n");
    appendStatement(sb, srb, smb, "<java.lang.Class />\n");
    appendStatement(sb, srb, smb, "</preamble>\n");
    {
      programRange.setStartPosition(sb.length());
      programRange.setStartLineNumber(lines);
      appendTypeStatement(sb, srb, smb, someModelARange, "<com.some.app.SomeModelA>\n");
      appendTypeStatement(sb, srb, smb, someModelBRange, "<com.some.app.SomeModelB>\n");
      appendTypeStatement(sb, srb, smb, someControllerRange, "<com.some.app.SomeController>\n");
      appendTypeStatement(sb, srb, smb, entryPointRange, "<com.some.app.EntryPoint>\n");
      programRange.setEndPosition(sb.length());
      programRange.setEndLineNumber(lines);
    }
    appendStatement(sb, srb, smb, "<epilogue>\n");
    appendStatement(sb, srb, smb, "<Some Bootstrap Code>\n");
    appendStatement(sb, srb, smb, "</epilogue>\n");
    String originalJs = sb.toString();

    MinimalRebuildCache minimalRebuildCache = new MinimalRebuildCache();
    PermutationRebuildCache permutationRebuildCache =
        minimalRebuildCache.getPermutationRebuildCache(1);

    // Create type inheritance.
    Map<String, String> superClassesByClass =
        minimalRebuildCache.getImmediateTypeRelations().getImmediateSuperclassesByClass();
    superClassesByClass.put("java.lang.Class", "java.lang.Object");
    superClassesByClass.put("com.some.app.SomeAModel", "java.lang.Object");
    superClassesByClass.put("com.some.app.SomeBModel", "java.lang.Object");
    superClassesByClass.put("com.some.app.SomeController", "java.lang.Object");
    superClassesByClass.put("com.some.app.EntryPoint", "java.lang.Object");

    // Record root types.
    minimalRebuildCache.setRootTypeNames(Lists.newArrayList("com.some.app.EntryPoint"));

    // Record type references.
    permutationRebuildCache.addTypeReference("com.some.app.EntryPoint",
        "com.some.app.SomeController");
    permutationRebuildCache.addTypeReference("com.some.app.SomeController",
        "com.some.app.SomeBModel");
    permutationRebuildCache.addTypeReference("com.some.app.SomeController",
        "com.some.app.SomeAModel");

    JsTypeLinker jsTypeLinker = new JsTypeLinker(TreeLogger.NULL,
        new JsNoopTransformer(originalJs, srb.build(), smb.build()), classRanges, programRange,
        permutationRebuildCache, new JTypeOracle(null, minimalRebuildCache, true));

    // Run the JS Type Linker.
    jsTypeLinker.exec();

    // Verify that the linker output all the expected classes and sorted them alphabetically.
    assertEquals("<preamble>\n<java.lang.Object />\n<java.lang.Class />\n</preamble>\n"
        + "<com.some.app.EntryPoint>\n" + "<com.some.app.SomeModelA>\n"
        + "<com.some.app.SomeModelB>\n" + "<com.some.app.SomeController>\n"
        + "<epilogue>\n<Some Bootstrap Code>\n</epilogue>\n", jsTypeLinker.getJs());
    assertEquals(Lists.newArrayList("preamble", "java.lang.Object", "java.lang.Class", "/preamble",
        "com.some.app.EntryPoint", "com.some.app.SomeModelA", "com.some.app.SomeModelB",
        "com.some.app.SomeController", "epilogue", "Some Bootstrap Code", "/epilogue"),
        getTypeNames(jsTypeLinker.getSourceInfoMap()));
    assertEquals(11, jsTypeLinker.getSourceInfoMap().getLines());

    // Make SomeModelB the super class of SomeModelA and then verify that B comes out before A.
    superClassesByClass.put("com.some.app.SomeAModel", "com.some.app.SomeBModel");
    jsTypeLinker = new JsTypeLinker(TreeLogger.NULL,
        new JsNoopTransformer(originalJs, srb.build(), smb.build()), classRanges, programRange,
        permutationRebuildCache, new JTypeOracle(null, minimalRebuildCache, true));
    jsTypeLinker.exec();
    assertEquals("<preamble>\n<java.lang.Object />\n<java.lang.Class />\n</preamble>\n"
        + "<com.some.app.EntryPoint>\n" + "<com.some.app.SomeModelB>\n"
        + "<com.some.app.SomeModelA>\n" + "<com.some.app.SomeController>\n"
        + "<epilogue>\n<Some Bootstrap Code>\n</epilogue>\n", jsTypeLinker.getJs());
    assertEquals(Lists.newArrayList("preamble", "java.lang.Object", "java.lang.Class", "/preamble",
        "com.some.app.EntryPoint", "com.some.app.SomeModelB", "com.some.app.SomeModelA",
        "com.some.app.SomeController", "epilogue", "Some Bootstrap Code", "/epilogue"),
        getTypeNames(jsTypeLinker.getSourceInfoMap()));
    assertEquals(11, jsTypeLinker.getSourceInfoMap().getLines());

    // Stop referring to SomeModelA from the Controller and verify that SomeModelA is not in the
    // output.
    permutationRebuildCache.removeReferencesFrom("com.some.app.SomeController");
    permutationRebuildCache.addTypeReference("com.some.app.SomeController",
        "com.some.app.SomeBModel");
    jsTypeLinker = new JsTypeLinker(TreeLogger.NULL,
        new JsNoopTransformer(originalJs, srb.build(), smb.build()), classRanges, programRange,
        permutationRebuildCache, new JTypeOracle(null, minimalRebuildCache, true));
    jsTypeLinker.exec();
    assertEquals("<preamble>\n<java.lang.Object />\n<java.lang.Class />\n</preamble>\n"
        + "<com.some.app.EntryPoint>\n" + "<com.some.app.SomeModelB>\n"
        + "<com.some.app.SomeController>\n" + "<epilogue>\n<Some Bootstrap Code>\n</epilogue>\n",
        jsTypeLinker.getJs());
    assertEquals(Lists.newArrayList("preamble", "java.lang.Object", "java.lang.Class", "/preamble",
        "com.some.app.EntryPoint", "com.some.app.SomeModelB", "com.some.app.SomeController",
        "epilogue", "Some Bootstrap Code", "/epilogue"),
        getTypeNames(jsTypeLinker.getSourceInfoMap()));
    assertEquals(10, jsTypeLinker.getSourceInfoMap().getLines());
  }

  private void appendStatement(StringBuilder sb, StatementRangesBuilder statementRangesBuilder,
      JsSourceMapBuilder jsSourceMapBuilder, String statement) {
    String typeName =
        statement.replace(" />", "").replace("<", "").replace(">", "").replace("\n", "");

    statementRangesBuilder.addStartPosition(sb.length());
    LinkedHashMap<Range, SourceInfo> sourceInfosByRange =
        Maps.<Range, SourceInfo> newLinkedHashMap();
    sourceInfosByRange.put(
        new Range(0, statement.length(), lines, 0, lines + 1, statement.length()),
        SourceOrigin.create(0, statement.length(), 0, typeName));
    jsSourceMapBuilder.append(new JsSourceMap(sourceInfosByRange, statement.length(), 1));
    sb.append(statement);
    statementRangesBuilder.addEndPosition(sb.length());

    lines++;
  }

  private void appendTypeStatement(StringBuilder sb, StatementRangesBuilder statementRangesBuilder,
      JsSourceMapBuilder jsSourceMapBuilder, NamedRange someModelARange, String statement) {
    someModelARange.setStartPosition(sb.length());
    someModelARange.setStartLineNumber(lines);
    appendStatement(sb, statementRangesBuilder, jsSourceMapBuilder, statement);
    someModelARange.setEndPosition(sb.length());
    someModelARange.setEndLineNumber(lines);
  }

  private List<String> getTypeNames(JsSourceMap sourceInfoMap) {
    List<String> typeNames = Lists.newArrayList();
    for (Entry<Range, SourceInfo> entry : sourceInfoMap.getEntries()) {
      typeNames.add(entry.getValue().getFileName());
    }
    return typeNames;
  }
}
