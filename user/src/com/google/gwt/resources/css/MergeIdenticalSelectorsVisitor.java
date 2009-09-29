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
import com.google.gwt.resources.css.ast.CssRule;
import com.google.gwt.resources.css.ast.CssSelector;
import com.google.gwt.resources.rg.CssResourceGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Merges rules that have matching selectors.
 */
public class MergeIdenticalSelectorsVisitor extends CssModVisitor {
  private final Map<String, CssRule> canonicalRules = new HashMap<String, CssRule>();
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
    // Assumed to run immediately after SplitRulesVisitor
    assert x.getSelectors().size() == 1;
    CssSelector sel = x.getSelectors().get(0);

    if (canonicalRules.containsKey(sel.getSelector())) {
      CssRule canonical = canonicalRules.get(sel.getSelector());

      // Check everything between the canonical rule and this rule for common
      // properties. If there are common properties, it would be unsafe to
      // promote the rule.
      boolean hasCommon = false;
      int index = rulesInOrder.indexOf(canonical) + 1;
      assert index != 0;

      for (Iterator<CssRule> i = rulesInOrder.listIterator(index); i.hasNext()
          && !hasCommon;) {
        hasCommon = CssResourceGenerator.haveCommonProperties(i.next(), x);
      }

      if (!hasCommon) {
        // It's safe to promote the rule
        canonical.getProperties().addAll(x.getProperties());
        ctx.removeMe();
        return false;
      }
    }

    canonicalRules.put(sel.getSelector(), x);
    rulesInOrder.add(x);
    return false;
  }

  private void visitInNewContext(List<CssNode> nodes) {
    MergeIdenticalSelectorsVisitor v = new MergeIdenticalSelectorsVisitor();
    v.acceptWithInsertRemove(nodes);
    rulesInOrder.addAll(v.rulesInOrder);
  }
}