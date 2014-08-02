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
import com.google.gwt.core.ext.linker.impl.NamedRange;
import com.google.gwt.core.ext.linker.impl.StatementRangesBuilder;
import com.google.gwt.dev.MinimalRebuildCache;
import com.google.gwt.dev.MinimalRebuildCache.PermutationRebuildCache;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import junit.framework.TestCase;

import java.util.List;
import java.util.Map;

/**
 * Tests for {@link JsTypeLinker}.
 */
public class JsTypeLinkerTest extends TestCase {

  public void testLink() {
    NamedRange programRange = new NamedRange("Program");
    NamedRange someModelARange = new NamedRange("com.some.app.SomeAModel");
    NamedRange someModelBRange = new NamedRange("com.some.app.SomeBModel");
    NamedRange someControllerRange = new NamedRange("com.some.app.SomeController");
    NamedRange entryPointRange = new NamedRange("com.some.app.EntryPoint");
    List<NamedRange> classRanges =
        Lists.newArrayList(someModelARange, someModelBRange, someControllerRange, entryPointRange);
    StatementRangesBuilder srb = new StatementRangesBuilder();

    // Build the original JS and log boundaries.
    StringBuilder sb = new StringBuilder();
    appendStatement(sb, srb, "<preamble>");
    appendStatement(sb, srb, "<java.lang.Object />");
    appendStatement(sb, srb, "<java.lang.Class />");
    appendStatement(sb, srb, "</preamble>");
    {
      programRange.setStartPosition(sb.length());
      appendTypeStatement(sb, srb, someModelARange, "<com.some.app.SomeModelA>");
      appendTypeStatement(sb, srb, someModelBRange, "<com.some.app.SomeModelB>");
      appendTypeStatement(sb, srb, someControllerRange, "<com.some.app.SomeController>");
      appendTypeStatement(sb, srb, entryPointRange, "<com.some.app.EntryPoint>");
      programRange.setEndPosition(sb.length());
    }
    appendStatement(sb, srb, "<epilogue>");
    appendStatement(sb, srb, "<Some Bootstrap Code>");
    appendStatement(sb, srb, "</epilogue>");
    String originalJs = sb.toString();

    MinimalRebuildCache minimalRebuildCache = new MinimalRebuildCache();
    PermutationRebuildCache permutationRebuildCache =
        minimalRebuildCache.getPermutationRebuildCache(1);

    // Create type inheritance.
    Map<String, String> superClassesByClass =
        minimalRebuildCache.getImmediateTypeRelations().getSuperClassesByClass();
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
        new JsNoopTransformer(originalJs, srb.build(), null), classRanges, programRange,
        permutationRebuildCache, new JTypeOracle(null, minimalRebuildCache, true));

    // Run the JS Type Linker.
    jsTypeLinker.exec();

    // Verify that the linker output all the expected classes and sorted them alphabetically.
    assertEquals("<preamble><java.lang.Object /><java.lang.Class /></preamble>"
        + "<com.some.app.EntryPoint>" + "<com.some.app.SomeModelA>" + "<com.some.app.SomeModelB>"
        + "<com.some.app.SomeController>" + "<epilogue><Some Bootstrap Code></epilogue>",
        jsTypeLinker.getJs());

    // Make SomeModelB the super class of SomeModelA and then verify that B comes out before A.
    superClassesByClass.put("com.some.app.SomeAModel", "com.some.app.SomeBModel");
    jsTypeLinker = new JsTypeLinker(TreeLogger.NULL,
        new JsNoopTransformer(originalJs, srb.build(), null), classRanges, programRange,
        permutationRebuildCache, new JTypeOracle(null, minimalRebuildCache, true));
    jsTypeLinker.exec();
    assertEquals("<preamble><java.lang.Object /><java.lang.Class /></preamble>"
        + "<com.some.app.EntryPoint>" + "<com.some.app.SomeModelB>" + "<com.some.app.SomeModelA>"
        + "<com.some.app.SomeController>" + "<epilogue><Some Bootstrap Code></epilogue>",
        jsTypeLinker.getJs());

    // Stop referring to SomeModelA from the Controller and verify that SomeModelA is not in the
    // output.
    permutationRebuildCache.removeReferencesFrom("com.some.app.SomeController");
    permutationRebuildCache.addTypeReference("com.some.app.SomeController",
        "com.some.app.SomeBModel");
    jsTypeLinker = new JsTypeLinker(TreeLogger.NULL,
        new JsNoopTransformer(originalJs, srb.build(), null), classRanges, programRange,
        permutationRebuildCache, new JTypeOracle(null, minimalRebuildCache, true));
    jsTypeLinker.exec();
    assertEquals("<preamble><java.lang.Object /><java.lang.Class /></preamble>"
        + "<com.some.app.EntryPoint>" + "<com.some.app.SomeModelB>"
        + "<com.some.app.SomeController>" + "<epilogue><Some Bootstrap Code></epilogue>",
        jsTypeLinker.getJs());
  }

  private void appendStatement(StringBuilder sb, StatementRangesBuilder statementRangesBuilder,
      String statement) {
    statementRangesBuilder.addStartPosition(sb.length());
    sb.append(statement);
    statementRangesBuilder.addEndPosition(sb.length());
  }

  private void appendTypeStatement(StringBuilder sb, StatementRangesBuilder statementRangesBuilder,
      NamedRange someModelARange, String statement) {
    someModelARange.setStartPosition(sb.length());
    appendStatement(sb, statementRangesBuilder, statement);
    someModelARange.setEndPosition(sb.length());
  }
}
