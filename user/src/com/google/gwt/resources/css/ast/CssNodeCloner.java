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
package com.google.gwt.resources.css.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Clones CssNodes.
 */
public class CssNodeCloner extends CssVisitor {

  /**
   * Clone a list of nodes.
   */
  public static <T extends CssNode> List<T> clone(Class<T> clazz, List<T> nodes) {

    // Push a fake context that will contain the cloned node
    List<CssNode> topContext = new ArrayList<CssNode>();
    final List<CssProperty> topProperties = new ArrayList<CssProperty>();
    final List<CssSelector> topSelectors = new ArrayList<CssSelector>();

    CssNodeCloner cloner = new CssNodeCloner();
    cloner.curentNodes.push(topContext);
    cloner.currentHasProperties = new HasProperties() {
      public List<CssProperty> getProperties() {
        return topProperties;
      }
    };
    cloner.currentHasSelectors = new HasSelectors() {
      public List<CssSelector> getSelectors() {
        return topSelectors;
      }
    };

    // Process the nodes
    cloner.accept(nodes);

    /*
     * Return the requested data. Different AST nodes will have been collected
     * in different parts of the initial context.
     */
    List<?> toIterate;
    if (CssProperty.class.isAssignableFrom(clazz)) {
      toIterate = topProperties;
    } else if (CssSelector.class.isAssignableFrom(clazz)) {
      toIterate = topSelectors;
    } else {
      toIterate = topContext;
    }

    assert toIterate.size() == nodes.size() : "Wrong number of nodes in top "
        + "context. Expecting: " + nodes.size() + " Found: " + toIterate.size();

    // Create the final return value
    List<T> toReturn = new ArrayList<T>(toIterate.size());
    for (Object node : toIterate) {
      assert clazz.isInstance(node) : "Return type mismatch. Expecting: "
          + clazz.getName() + " Found: " + node.getClass().getName();

      // Cast to the correct type to avoid an unchecked generic cast
      toReturn.add(clazz.cast(node));
    }

    return toReturn;
  }

  /**
   * Clone a single node.
   */
  public static <T extends CssNode> T clone(Class<T> clazz, T node) {
    return clone(clazz, Collections.singletonList(node)).get(0);
  }

  private HasProperties currentHasProperties;

  private HasSelectors currentHasSelectors;

  private final Stack<List<CssNode>> curentNodes = new Stack<List<CssNode>>();

  private CssNodeCloner() {
  }

  @Override
  public void endVisit(CssMediaRule x, Context ctx) {
    popNodes(x);
  }

  @Override
  public void endVisit(CssNoFlip x, Context ctx) {
    popNodes(x);
  }

  @Override
  public void endVisit(CssStylesheet x, Context ctx) {
    popNodes(x);
  }

  @Override
  public boolean visit(CssDef x, Context ctx) {
    CssDef newDef = new CssDef(x.getKey());
    newDef.getValues().addAll(x.getValues());

    addToNodes(newDef);
    return true;
  }

  @Override
  public boolean visit(CssEval x, Context ctx) {
    assert x.getValues().size() == 1;
    assert x.getValues().get(0).isExpressionValue() != null;

    String value = x.getValues().get(0).isExpressionValue().getExpression();
    CssEval newEval = new CssEval(x.getKey(), value);
    addToNodes(newEval);
    return true;
  }

  @Override
  public boolean visit(CssExternalSelectors x, Context ctx) {
    CssExternalSelectors newExternals = new CssExternalSelectors();
    newExternals.getClasses().addAll(x.getClasses());
    addToNodes(newExternals);
    return true;
  }

  /**
   * A CssIf has two lists of nodes, so we want to handle traversal in this
   * visitor.
   */
  @Override
  public boolean visit(CssIf x, Context ctx) {
    CssIf newIf = new CssIf();

    if (x.getExpression() != null) {
      newIf.setExpression(x.getExpression());
    } else {
      newIf.setProperty(x.getPropertyName());

      String[] newValues = new String[x.getPropertyValues().length];
      System.arraycopy(x.getPropertyValues(), 0, newValues, 0, newValues.length);
      newIf.setPropertyValues(newValues);

      newIf.setNegated(x.isNegated());
    }

    // Handle the "then" part
    pushNodes(newIf);
    accept(x.getNodes());
    popNodes(x, newIf);

    /*
     * Push the "else" part as though it were its own node, but don't add it as
     * its own top-level node.
     */
    CollapsedNode oldElseNodes = new CollapsedNode(x.getElseNodes());
    CollapsedNode newElseNodes = new CollapsedNode(newIf.getElseNodes());
    pushNodes(newElseNodes, false);
    accept(oldElseNodes);
    popNodes(oldElseNodes, newElseNodes);

    return false;
  }

  @Override
  public boolean visit(CssMediaRule x, Context ctx) {
    CssMediaRule newRule = new CssMediaRule();
    newRule.getMedias().addAll(newRule.getMedias());

    pushNodes(newRule);
    return true;
  }

  @Override
  public boolean visit(CssNoFlip x, Context ctx) {
    pushNodes(new CssNoFlip());
    return true;
  }

  @Override
  public boolean visit(CssPageRule x, Context ctx) {
    CssPageRule newRule = new CssPageRule();
    newRule.setPseudoPage(x.getPseudoPage());
    addToNodes(newRule);
    return true;
  }

  @Override
  public boolean visit(CssProperty x, Context ctx) {
    CssProperty newProperty = new CssProperty(x.getName(), x.getValues(),
        x.isImportant());
    currentHasProperties.getProperties().add(newProperty);
    return true;
  }

  @Override
  public boolean visit(CssFontFace x, Context ctx) {
    CssFontFace newRule = new CssFontFace();
    addToNodes(newRule);
    return true;
  }

  @Override
  public boolean visit(CssRule x, Context ctx) {
    CssRule newRule = new CssRule();
    addToNodes(newRule);
    return true;
  }

  @Override
  public boolean visit(CssSelector x, Context ctx) {
    CssSelector newSelector = new CssSelector(x.getSelector());
    currentHasSelectors.getSelectors().add(newSelector);
    return true;
  }

  @Override
  public boolean visit(CssSprite x, Context ctx) {
    CssSprite newSprite = new CssSprite();
    newSprite.setResourceFunction(x.getResourceFunction());
    addToNodes(newSprite);
    return true;
  }

  @Override
  public boolean visit(CssStylesheet x, Context ctx) {
    CssStylesheet newSheet = new CssStylesheet();
    pushNodes(newSheet);
    return true;
  }

  @Override
  public boolean visit(CssUrl x, Context ctx) {
    assert x.getValues().size() == 1;
    assert x.getValues().get(0).isDotPathValue() != null;
    CssUrl newUrl = new CssUrl(x.getKey(),
        x.getValues().get(0).isDotPathValue());
    addToNodes(newUrl);
    return true;
  }
  
  @Override
  public boolean visit(CssUnknownAtRule x, Context ctx) {
    CssUnknownAtRule newRule = new CssUnknownAtRule(x.getRule());
    addToNodes(newRule);
    return true;
  }

  /**
   * Add a cloned node instance to the output.
   */
  private void addToNodes(CssNode node) {
    curentNodes.peek().add(node);

    currentHasProperties = node instanceof HasProperties ? (HasProperties) node
        : null;
    currentHasSelectors = node instanceof HasSelectors ? (HasSelectors) node
        : null;
  }

  /**
   * Remove a frame.
   * 
   * @param original the node that was being cloned so that validity checks may
   *          be performed
   */
  private List<CssNode> popNodes(HasNodes original) {
    List<CssNode> toReturn = curentNodes.pop();

    if (toReturn.size() != original.getNodes().size()) {
      throw new RuntimeException("Insufficient number of nodes for a "
          + original.getClass().getName() + " Expected: "
          + original.getNodes().size() + " Found: " + toReturn.size());
    }

    return toReturn;
  }

  /**
   * Remove a frame.
   * 
   * @param original the node that was being cloned so that validity checks may
   *          be performed
   * @param expected the HasNodes whose nodes were being populated by the frame
   *          being removed.
   */
  private List<CssNode> popNodes(HasNodes original, HasNodes expected) {
    List<CssNode> toReturn = popNodes(original);

    if (toReturn != expected.getNodes()) {
      throw new RuntimeException("Incorrect parent node list popped");
    }

    return toReturn;
  }

  /**
   * Push a new frame, adding the new parent as a child of the current parent.
   */
  private <T extends CssNode & HasNodes> void pushNodes(T parent) {
    pushNodes(parent, true);
  }

  /**
   * Push a new frame.
   * 
   * @param addToNodes if <code>true</code> add the new parent node as a child
   *          of the current parent.
   */
  private <T extends CssNode & HasNodes> void pushNodes(T parent,
      boolean addToNodes) {
    if (addToNodes) {
      addToNodes(parent);
    }
    this.curentNodes.push(parent.getNodes());
  }
}
