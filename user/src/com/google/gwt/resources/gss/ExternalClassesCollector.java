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
package com.google.gwt.resources.gss;

import com.google.gwt.thirdparty.common.css.SourceCodeLocation;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssCompilerPass;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssCompositeValueNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssLiteralNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssStringNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssUnknownAtRuleNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssValueNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.DefaultTreeVisitor;
import com.google.gwt.thirdparty.common.css.compiler.ast.ErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssError;
import com.google.gwt.thirdparty.common.css.compiler.ast.MutatingVisitController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Visitor that collect style classes flagged as external.
 */
public class ExternalClassesCollector extends DefaultTreeVisitor implements CssCompilerPass {
  public static final String EXTERNAL_AT_RULE = "external";
  private static final String STAR_SUFFIX = "*";

  private final MutatingVisitController visitController;
  private final ErrorManager errorManager;

  private Set<String> externalClassNames;
  private List<String> externalClassPrefixes;
  private boolean matchAll;

  public ExternalClassesCollector(MutatingVisitController visitController,
      ErrorManager errorManager) {
    this.visitController = visitController;
    this.errorManager = errorManager;
  }

  @Override
  public void runPass() {
    externalClassNames = new HashSet<String>();
    externalClassPrefixes = new ArrayList<String>();

    visitController.startVisit(this);
  }

  @Override
  public void leaveUnknownAtRule(CssUnknownAtRuleNode node) {
    if (EXTERNAL_AT_RULE.equals(node.getName().getValue())) {
      if (!matchAll) {
        processParameters(node.getParameters(), node.getSourceCodeLocation());
      }
      visitController.removeCurrentNode();
    }
  }

  public Set<String> getExternalClassNames(Set<String> styleClassesSet) {
    SortedSet<String> classNames = new TreeSet<String>(styleClassesSet);
    if (matchAll) {
      return classNames;
    }

    for (String prefix : externalClassPrefixes) {
      for (String styleClass : classNames.tailSet(prefix)) {
        if (styleClass.startsWith(prefix)) {
          externalClassNames.add(styleClass);
        } else {
          break;
        }
      }
    }
    return externalClassNames;
  }

  private void processParameters(List<CssValueNode> values, SourceCodeLocation sourceCodeLocation) {
    for (CssValueNode value : values) {
      if (value instanceof CssCompositeValueNode) {
        processParameters(((CssCompositeValueNode) value).getValues(), sourceCodeLocation);
      } else if (value instanceof CssStringNode) {
        String selector = ((CssStringNode) value).getConcreteValue();
        if (STAR_SUFFIX.equals(selector)) {
          matchAll = true;
          return;
        } else if (selector.endsWith(STAR_SUFFIX)) {
          externalClassPrefixes.add(selector.substring(0, selector.length() - 1));
        } else {
          externalClassNames.add(selector);
        }
      } else if (value instanceof CssLiteralNode) {
        externalClassNames.add(value.getValue());
      } else {
        errorManager.report(new GssError("External at-rule invalid. The following terms is not " +
            "accepted in an external at-rule [" + value.getValue() + "]", sourceCodeLocation));
      }
    }
  }
}
