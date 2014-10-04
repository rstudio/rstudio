/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.resources.converter;

import com.google.gwt.resources.css.ast.Context;
import com.google.gwt.resources.css.ast.CssIf;
import com.google.gwt.resources.css.ast.CssNode;

import java.util.ArrayList;
import java.util.List;

/**
 * The original Css parser doesn't create specific nodes for {@code @elif} and {@code @else}
 * at-rules. That makes their conversion more difficult, especially for the {@code @else}
 * at-rule.
 * <p/>
 * The main goal of this visitor is to create specific nodes for these two at-rules in
 * order to ease their conversion.
 */
public class ElseNodeCreator extends ExtendedCssVisitor {

  @Override
  public boolean visit(CssIf cssIf, Context ctx) {
    List<CssNode> newNodes = new ArrayList<CssNode>();

    visitElseNodes(cssIf, newNodes);

    cssIf.getElseNodes().clear();
    cssIf.getElseNodes().addAll(newNodes);

    return true;
  }

  private void visitElseNodes(CssIf cssIf, List<CssNode> newNodes) {
    List<CssNode> elseNodes = cssIf.getElseNodes();
    CssElse cssElse = null;
    for (CssNode child : elseNodes) {
      if (elseNodes.size() == 1 && child instanceof CssIf) {
        // @elsif at-rule case
        CssIf cssIfChild = (CssIf) child;
        CssElIf cssElIF = new CssElIf(cssIfChild);
        newNodes.add(cssElIF);
        visitElseNodes(cssIfChild, newNodes);
      } else {
        // @else at-rule case
        if (cssElse == null) {
          cssElse = new CssElse();
        }

        cssElse.getNodes().add(child);
      }
    }

    if (cssElse != null) {
      newNodes.add(cssElse);
    }
  }
}
