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
import com.google.gwt.core.ext.linker.StatementRanges;
import com.google.gwt.core.ext.linker.impl.NamedRange;
import com.google.gwt.core.ext.linker.impl.StatementRangesBuilder;
import com.google.gwt.core.ext.linker.impl.StatementRangesExtractor;
import com.google.gwt.dev.MinimalRebuildCache.PermutationRebuildCache;
import com.google.gwt.dev.jjs.ast.JTypeOracle;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Transforms program JS source by performing a per-type link.
 * <p>
 * Provided JS and Ranges are used to grab new JS type chunks. A RebuildCache is used to cache per
 * type JS and statement ranges (possibly across compiles) and calculate the set of reachable types.
 * JTypeOracle is used to order linked output.
 */
public class JsTypeLinker extends JsAbstractTextTransformer {

  private static final String FOOTER_NAME = "-footer-";
  private static final String HEADER_NAME = "-header-";
  private final NamedRange footerRange;
  private final NamedRange headerRange;
  private final StringBuilder jsBuilder = new StringBuilder();
  private final Set<String> linkedTypeNames = Sets.newHashSet();
  private TreeLogger logger;
  private final PermutationRebuildCache permutationRebuildCache;
  private final StatementRangesBuilder statementRangesBuilder = new StatementRangesBuilder();
  private final StatementRangesExtractor statementRangesExtractor;
  private final JTypeOracle typeOracle;
  private final List<NamedRange> typeRanges;

  public JsTypeLinker(TreeLogger logger, JsAbstractTextTransformer textTransformer,
      List<NamedRange> typeRanges, NamedRange programTypeRange,
      PermutationRebuildCache permutationRebuildCache, JTypeOracle typeOracle) {
    super(textTransformer);
    this.logger = logger;
    this.statementRangesExtractor = new StatementRangesExtractor(statementRanges);
    this.typeRanges = typeRanges;
    this.headerRange = new NamedRange(HEADER_NAME, 0, programTypeRange.getStartPosition());
    this.footerRange = new NamedRange(FOOTER_NAME, programTypeRange.getEndPosition(), js.length());
    this.permutationRebuildCache = permutationRebuildCache;
    this.typeOracle = typeOracle;
  }

  @Override
  public void exec() {
    logger = logger.branch(TreeLogger.DEBUG,
        "Linking per-type JS with " + typeRanges.size() + " new types.");
    linkAll(computeReachableTypes());
  }

  @Override
  protected void updateSourceInfoMap() {
    // TODO(stalcup): update sourcemaps to match relinking.
  }

  private List<String> computeReachableTypes() {
    List<String> reachableTypeNames =
        Lists.newArrayList(permutationRebuildCache.computeReachableTypeNames());
    Collections.sort(reachableTypeNames);
    return reachableTypeNames;
  }

  private void extractOne(NamedRange typeRange) {
    String typeName = typeRange.getName();
    permutationRebuildCache.setJsForType(logger, typeName,
        js.substring(typeRange.getStartPosition(), typeRange.getEndPosition()));
    permutationRebuildCache.setStatementRangesForType(typeName,
        statementRangesExtractor.extract(typeRange.getStartPosition(), typeRange.getEndPosition()));
  }

  private void linkAll(List<String> reachableTypeNames) {
    // Extract new JS.
    if (permutationRebuildCache.getJs(HEADER_NAME) == null) {
      extractOne(headerRange);
    }
    for (NamedRange typeRange : typeRanges) {
      extractOne(typeRange);
    }
    if (permutationRebuildCache.getJs(FOOTER_NAME) == null) {
      extractOne(footerRange);
    }

    // Link new and old JS.
    linkOne(HEADER_NAME);
    for (String reachableTypeName : reachableTypeNames) {
      linkOne(reachableTypeName);
    }
    linkOne(FOOTER_NAME);

    logger.log(TreeLogger.INFO, "prelink JS size = " + js.length());
    js = jsBuilder.toString();
    statementRanges = statementRangesBuilder.build();
    logger.log(TreeLogger.INFO, "postlink JS size = " + js.length());
  }

  private void linkOne(String typeName) {
    if (linkedTypeNames.contains(typeName)) {
      return;
    }
    linkedTypeNames.add(typeName);

    String typeJs = permutationRebuildCache.getJs(typeName);
    if (typeJs == null) {
      return;
    }

    // Link super types before sub types.
    String superTypeName = typeOracle.getSuperTypeName(typeName);
    if (superTypeName != null) {
      linkOne(superTypeName);
    }

    logger.log(TreeLogger.SPAM, "linking type " + typeName + " (" + typeJs.length() + " bytes)");
    StatementRanges typeStatementRanges = permutationRebuildCache.getStatementRanges(typeName);

    jsBuilder.append(typeJs);
    statementRangesBuilder.append(typeStatementRanges);
    // TODO(stalcup): build a sourcemap one type at a time.
  }
}
