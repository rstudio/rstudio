/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.resources.css;

import com.google.gwt.resources.css.ast.Context;
import com.google.gwt.resources.css.ast.CssIf;
import com.google.gwt.resources.css.ast.CssMediaRule;
import com.google.gwt.resources.css.ast.CssModVisitor;
import com.google.gwt.resources.css.ast.CssNode;
import com.google.gwt.resources.css.ast.CssProperty;
import com.google.gwt.resources.css.ast.CssRule;
import com.google.gwt.resources.rg.CssResourceGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Merges rules that have identical content.
 */
public class MergeRulesByContentVisitor extends CssModVisitor {
  private Map<String, CssRule> rulesByContents = new HashMap<String, CssRule>();
  private final List<CssRule> rulesInOrder = new ArrayList<CssRule>();

  @Override
  public boolean visit(CssIf x, Context ctx) {
    visitInNewContext(x.getNodes());
    visitInNewContext(x.getElseNodes());
    return false;
  }

  @Override
  public boolean visit(CssMediaRule x, Context ctx) {
    visitInNewContext(x.getNodes());
    return false;
  }

  @Override
  public boolean visit(CssRule x, Context ctx) {
    StringBuilder b = new StringBuilder();
    for (CssProperty p : x.getProperties()) {
      b.append(p.getName()).append(":").append(p.getValues().getExpression());
      if (p.isImportant()) {
        b.append("!important");
      }
    }

    String content = b.toString();
    CssRule canonical = rulesByContents.get(content);

    // Check everything between the canonical rule and this rule for common
    // properties. If there are common properties, it would be unsafe to
    // promote the rule.
    if (canonical != null) {
      boolean hasCommon = false;
      int index = rulesInOrder.indexOf(canonical) + 1;
      assert index != 0;

      for (Iterator<CssRule> i = rulesInOrder.listIterator(index); i.hasNext()
          && !hasCommon;) {
        hasCommon = CssResourceGenerator.haveCommonProperties(i.next(), x);
      }

      if (!hasCommon) {
        canonical.getSelectors().addAll(x.getSelectors());
        ctx.removeMe();
        return false;
      }
    }

    rulesByContents.put(content, x);
    rulesInOrder.add(x);
    return false;
  }

  private void visitInNewContext(List<CssNode> nodes) {
    MergeRulesByContentVisitor v = new MergeRulesByContentVisitor();
    v.acceptWithInsertRemove(nodes);
    rulesInOrder.addAll(v.rulesInOrder);
  }
}