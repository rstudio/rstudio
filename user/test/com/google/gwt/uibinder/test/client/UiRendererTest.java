/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.test.client.UiRendererUi.HtmlRenderer;

/**
 * Functional test of UiBinder.
 */
public class UiRendererTest extends GWTTestCase {

  private static final String RENDERED_VALUE = "bar";
  private static final String RENDERED_VALUE_TWICE = "quux";

  private DivElement docDiv;
  private SafeHtml renderedHtml;
  private HtmlRenderer renderer;
  private UiRendererUi uiRendererUi;

  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.test.LazyWidgetBuilderSuite";
  }

  @Override
  public void gwtSetUp() throws Exception {
    super.gwtSetUp();
    UiRendererTestApp app = UiRendererTestApp.getInstance();
    uiRendererUi = app.getUiRendererUi();
    renderedHtml = uiRendererUi.render(RENDERED_VALUE, RENDERED_VALUE_TWICE);
    renderer = UiRendererUi.getRenderer();

    docDiv = Document.get().createDivElement();
    docDiv.setInnerHTML(renderedHtml.asString());
    Document.get().getBody().appendChild(docDiv);
  }

  public void testFieldGetters() {
    assertTrue(renderer.isParentOrRenderer(docDiv));
    // Get root from parent
    DivElement root = renderer.getRoot(docDiv);
    assertTrue(renderer.isParentOrRenderer(root));
    assertNotNull(root);

    // For example, the rendered value should be inside
    assertSpanContainsRenderedValue(root);

    // Get nameSpan
    SpanElement nameSpan = renderer.getNameSpan(docDiv);
    assertSpanContainsRenderedValueText(RENDERED_VALUE, nameSpan.getFirstChild());

    // Getters also work from the root element
    DivElement root2 = renderer.getRoot(root);
    assertTrue(renderer.isParentOrRenderer(root2));
    assertNotNull(root2);
    assertSpanContainsRenderedValue(root2);
    nameSpan = renderer.getNameSpan(root);
    assertSpanContainsRenderedValueText(RENDERED_VALUE, nameSpan.getFirstChild());
  }

  public void testFieldGettersDetachedRoot() {
    // Detach root
    DivElement root = renderer.getRoot(docDiv);
    root.removeFromParent();

    // Getting the root element is still fine
    DivElement rootAgain = renderer.getRoot(root);
    assertEquals(root, rootAgain);

    if (GWT.isProdMode()) {
      // In prod mode we avoid checking whether the parent is attached
      try {
        renderer.getNameSpan(root);
        fail("Expected a IllegalArgumentException because root is not attached to the DOM");
      } catch (IllegalArgumentException e) {
        // Expected
      }
    } else {
      // In dev Mode we explicitly check to see if parent is attached
      try {
        renderer.getNameSpan(root);
        fail("Expected a RuntimeException because root is not attached to the DOM");
      } catch (RuntimeException e) {
        // Expected
      }
    }
  }

  public void testFieldGettersNoPreviouslyRenderedElement() {
    assertFalse(renderer.isParentOrRenderer(null));

    try {
      renderer.getRoot(null);
      fail("Expected NPE");
    } catch (NullPointerException e) {
      // Expected
    }

    try {
      renderer.getNameSpan(null);
      fail("Expected NPE");
    } catch (NullPointerException e) {
      // Expected
    }

    DivElement root = renderer.getRoot(docDiv);
    SpanElement nameSpan = renderer.getNameSpan(docDiv);

    // remove nameSpan
    nameSpan.removeFromParent();
    try {
      renderer.getNameSpan(docDiv);
      fail("Expected IllegalStateException because nameSpan was removed");
    } catch (IllegalStateException e) {
      // In dev mode this is different from not being attached
      assertFalse(GWT.isProdMode());
    } catch (IllegalArgumentException e) {
      // Otherwise the same error as being not attached
      assertTrue(GWT.isProdMode());
    }

    // Add a a sibling to the root element and remove the root from the parent altogether
    SpanElement spanElement = Document.get().createSpanElement();
    docDiv.appendChild(spanElement);
    root.removeFromParent();

    assertFalse(renderer.isParentOrRenderer(docDiv));
    if (GWT.isProdMode()) {
      // In prod mode no attempt is made to check whether root is still attached
      assertTrue(renderer.isParentOrRenderer(root));
    } else {
      assertFalse(renderer.isParentOrRenderer(root));
    }
    try {
      renderer.getRoot(docDiv);
      fail("Expected an IllegalArgumentException to fail because parent does not contain the root");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      renderer.getNameSpan(docDiv);
      fail("Expected an IllegalArgumentException to fail because parent does not contain the root");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    // Finally remove the spanElement too
    spanElement.removeFromParent();

    assertFalse(renderer.isParentOrRenderer(docDiv));
    try {
      renderer.getRoot(docDiv);
      fail("Expected an IllegalArgumentException to fail because parent does not contain the root");
    } catch (IllegalArgumentException e) {
      // Expected
    }

    try {
      renderer.getNameSpan(docDiv);
      fail("Expected an IllegalArgumentException to fail because parent does not contain the root");
    } catch (IllegalArgumentException e) {
      // Expected
    }
  }

  public void testFieldGettersNotOnlyChild() {
    DivElement root = renderer.getRoot(docDiv);

    // Add a a sibling to the root element
    docDiv.appendChild(Document.get().createSpanElement());

    // Getting the root element is still fine
    DivElement rootAgain = renderer.getRoot(docDiv);
    assertEquals(root, rootAgain);

    if (GWT.isProdMode()) {
      // In prod mode we avoid checking for being the only child
      assertTrue(renderer.isParentOrRenderer(docDiv));
      SpanElement nameSpan = renderer.getNameSpan(docDiv);
      assertSpanContainsRenderedValueText(RENDERED_VALUE, nameSpan.getFirstChild());
    } else {
      // in dev mode an explicit check is made
      assertFalse(renderer.isParentOrRenderer(docDiv));
      try {
        renderer.getNameSpan(docDiv);
        fail("Expected an IllegalArgumentException to fail because root is not the only child");
      } catch (IllegalArgumentException e) {
        // Expected
      }
    }
  }

  public void testSafeHtmlRendererText() {
    Node innerDiv = docDiv.getFirstChild();

    // Was the first span rendered as a "HTML-safe" text string?
    Node spanWithConstantTextNode = innerDiv.getChild(0);
    assertEquals("span", spanWithConstantTextNode.getNodeName().toLowerCase());
    assertEquals(Node.TEXT_NODE, spanWithConstantTextNode.getFirstChild().getNodeType());
    assertEquals("<b>This text won't be bold!</b>",
        spanWithConstantTextNode.getFirstChild().getNodeValue());

    Node firstRawTextNode = innerDiv.getChild(1);
    assertEquals(Node.TEXT_NODE, firstRawTextNode.getNodeType());
    assertEquals(" Hello, ", firstRawTextNode.getNodeValue());

    // The value passed to render() was rendered correctly
    assertSpanContainsRenderedValue(innerDiv);

    // ui:msg tags get rendered but the "<ui:msg>" tag is not
    Node secondRawTextNode = innerDiv.getChild(3);
    assertEquals(Node.TEXT_NODE, secondRawTextNode.getNodeType());
    assertEquals(". How goes it? ", secondRawTextNode.getNodeValue());

    // Fields not present in owning class produce no content
    Node spanNode = innerDiv.getChild(4);
    assertEquals(Node.ELEMENT_NODE, spanNode.getNodeType());
    assertEquals("span", spanNode.getNodeName().toLowerCase());
    assertFalse(spanNode.hasChildNodes());

    // Field passed to render() and used twice was rendered correctly too
    Node spanNode2 = innerDiv.getChild(5);
    assertEquals(Node.ELEMENT_NODE, spanNode2.getNodeType());
    assertEquals("span", spanNode2.getNodeName().toLowerCase());
    assertTrue(spanNode2.hasChildNodes());
    assertSpanContainsRenderedValueText(RENDERED_VALUE_TWICE + RENDERED_VALUE_TWICE,
        spanNode2.getFirstChild());
  }

  public void testStyleManipulation() {
    SpanElement nameSpan = renderer.getNameSpan(docDiv);
    assertEquals(renderer.getUiStyle().enabled(), nameSpan.getClassName());
    nameSpan.replaceClassName(renderer.getUiStyle().enabled(),
        renderer.getUiStyle().disabled());
    assertEquals(renderer.getUiStyle().disabled(), nameSpan.getClassName());
  }

  @Override
  protected void gwtTearDown() {
    docDiv.removeFromParent();
    docDiv = null;
  }

  private void assertSpanContainsRenderedValue(Node root) {
    Node firstFieldNode = root.getChild(2);
    assertEquals(Node.ELEMENT_NODE, firstFieldNode.getNodeType());
    assertEquals("span", firstFieldNode.getNodeName().toLowerCase());
    assertTrue(firstFieldNode.hasChildNodes());
    Node renderedValue = firstFieldNode.getFirstChild();
    assertSpanContainsRenderedValueText(RENDERED_VALUE, renderedValue);
  }

  private void assertSpanContainsRenderedValueText(String expectedValue, Node renderedValue) {
    assertEquals(Node.TEXT_NODE, renderedValue.getNodeType());
    assertEquals(expectedValue, renderedValue.getNodeValue());
  }
}
