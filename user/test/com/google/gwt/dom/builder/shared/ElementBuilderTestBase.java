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
package com.google.gwt.dom.builder.shared;

import com.google.gwt.dom.builder.client.DomBuilderFactory;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;

/**
 * Base tests for subclasses of {@link ElementBuilderBase}.
 * 
 * @param <T> the type of builder being tested
 */
public abstract class ElementBuilderTestBase<T extends ElementBuilderBase<?>> extends GWTTestCase {

  /**
   * A command that operates on a builder.
   */
  protected interface BuilderCommand<T extends ElementBuilderBase<?>> {
    /**
     * Execute an action.
     * 
     * @param builder the builder being tested
     */
    void execute(T builder);
  }

  /**
   * The GWT module name when running tests on the client.
   */
  protected static final String GWT_MODULE_NAME = "com.google.gwt.dom.builder.DomBuilder";

  /**
   * Indicates whether or not the element under test supports child elements.
   */
  private boolean isChildElementSupported;

  /**
   * Indicates whether or not the element under test supports an end tag.
   */
  private boolean isEndTagForbidden;

  /**
   * Indicates whether or not the element under test supports inner html.
   */
  private boolean isInnerHtmlSupported;

  /**
   * Indicates whether or not the element under test supports inner text.
   */
  private boolean isInnerTextSupported;

  @Override
  public String getModuleName() {
    // Default to JVM implementation.
    return null;
  }

  /**
   * Test that you cannot append text, html, or elements after setting the text.
   */
  public void testAppendAfterHtml() {
    // Skip this test if inner html is not supported.
    if (!isInnerHtmlSupported) {
      return;
    }

    for (ElementBuilderFactory factory : getFactories()) {
      T builder = createElementBuilder(factory);
      builder.html(SafeHtmlUtils.fromString("Hello World"));

      try {
        builder.text("moretext");
        fail("Expected IllegalStateException: setting text after setting html");
      } catch (IllegalStateException e) {
        // Expected.
      }

      try {
        builder.html(SafeHtmlUtils.fromString("morehtml"));
        fail("Expected IllegalStateException: setting html twice");
      } catch (IllegalStateException e) {
        // Expected.
      }

      try {
        builder.startDiv();
        fail("Expected IllegalStateException: appending a div after setting html");
      } catch (IllegalStateException e) {
        // Expected.
      }
    }
  }

  /**
   * Test that you cannot append text, html, or elements after setting the text.
   */
  public void testAppendAfterText() {
    // Skip this test if inner text is not supported.
    if (!isInnerTextSupported) {
      return;
    }

    for (ElementBuilderFactory factory : getFactories()) {
      T builder = createElementBuilder(factory);
      builder.text("Hello World");

      try {
        builder.text("moretext");
        fail("Expected IllegalStateException: setting text twice");
      } catch (IllegalStateException e) {
        // Expected.
      }

      if (isInnerHtmlSupported) {
        try {
          builder.html(SafeHtmlUtils.fromString("morehtml"));
          fail("Expected IllegalStateException: setting html after setting text");
        } catch (IllegalStateException e) {
          // Expected.
        }
      }

      if (isChildElementSupported) {
        try {
          builder.startDiv();
          fail("Expected IllegalStateException: appending a div after setting text");
        } catch (IllegalStateException e) {
          // Expected.
        }
      }
    }
  }

  public void testAttributeAfterAppendHtml() {
    // String value.
    assertActionFailsAfterAppendHtml(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.attribute("name", "value");
      }
    }, "Cannot add attribute after appending html");

    // Int value.
    assertActionFailsAfterAppendHtml(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.attribute("name", 1);
      }
    }, "Cannot add attribute after appending html");
  }

  public void testAttributeAfterEnd() {
    assertActionFailsAfterEnd(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.attribute("name", "value");
      }
    }, "Cannot add attribute after adding a child element");
  }

  public void testClassNameAfterAppendHtml() {
    assertActionFailsAfterAppendHtml(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.className("value");
      }
    }, "Cannot add attribute after appending html");
  }

  public void testDirNameAfterAppendHtml() {
    assertActionFailsAfterAppendHtml(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.dir("value");
      }
    }, "Cannot add attribute after appending html");
  }

  public void testDraggableNameAfterAppendHtml() {
    assertActionFailsAfterAppendHtml(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.draggable("value");
      }
    }, "Cannot add attribute after appending html");
  }

  public void testEnd() {
    for (ElementBuilderFactory factory : getFactories()) {
      // Test that a builder can be ended if it comes directly from the factory.
      {
        T builder = createElementBuilder(factory);
        builder.id("myid");
        endElement(builder);
      }

      /*
       * Test that a builder can be ended if it was started from another
       * builder.
       * 
       * Skip this test if child elements are not supported.
       */
      if (isChildElementSupported) {
        T builder = createElementBuilder(factory);
        T elem = startElement(builder);
        endElement(elem);
        builder.end();
      }
    }
  }

  public void testEndUnmatchedTagName() {
    // Skip this test if child elements are not supported.
    if (!isChildElementSupported) {
      return;
    }

    for (ElementBuilderFactory factory : getFactories()) {
      T builder = createElementBuilder(factory);
      T child = startElement(builder);

      try {
        child.end("notamatch");
        fail("Expected IllegalStateException: end tag does not match start tag");
      } catch (IllegalStateException e) {
        // Expected.
      }
    }
  }

  public void testHtmlAfterAppend() {
    assertActionFailsAfterAppendHtml(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.html(SafeHtmlUtils.fromString("hello world"));
      }
    }, "Cannot set html after appending a child element");
  }

  public void testHtmlAfterEnd() {
    if (!isInnerHtmlSupported) {
      return;
    }

    assertActionFailsAfterEnd(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.html(SafeHtmlUtils.fromString("hello world"));
      }
    }, "Cannot set html after adding a child element");
  }

  /**
   * Test that HTML can be set after ending one element and starting another.
   */
  public void testHtmlAfterRestart() {
    if (!isInnerHtmlSupported || !isChildElementSupported) {
      return;
    }

    for (ElementBuilderFactory factory : getFactories()) {
      T builder = createElementBuilder(factory);
      builder.startDiv().id("test").html(SafeHtmlUtils.fromString("test")).end();

      // Should not cause any errors.
      builder.startDiv().html(SafeHtmlUtils.fromString("hello"));
    }
  }

  /**
   * Test that all implementations of a builder are consistent in their support
   * of setting inner html.
   */
  public void testHtmlConsistent() {
    boolean expected = false;
    ElementBuilderFactory[] factories = getFactories();
    for (int i = 0; i < factories.length; i++) {
      T builder = createElementBuilder(factories[0]);
      boolean isSupported = true;
      try {
        builder.html(SafeHtmlUtils.EMPTY_SAFE_HTML);
      } catch (UnsupportedOperationException e) {
        // Child elements are not supported.
        isSupported = false;
      }

      assertEquals(isInnerHtmlSupported, isSupported);
    }
  }

  public void testIdNameAfterAppendHtml() {
    assertActionFailsAfterAppendHtml(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.id("value");
      }
    }, "Cannot add attribute after appending html");
  }

  public void testIsEndTagForbidden() {
    // Skip this test if the end tag is allowed.
    if (!isEndTagForbidden) {
      return;
    }

    for (ElementBuilderFactory factory : getFactories()) {
      try {
        T builder = createElementBuilder(factory);
        builder.html(SafeHtmlUtils.fromString("html is not allowed"));
        fail("Expected UnsupportedOperationException: cannot set html if end tag is forbidden");
      } catch (UnsupportedOperationException e) {
        // Expected.
      }

      try {
        T builder = createElementBuilder(factory);
        builder.text("text is not allowed");
        fail("Expected UnsupportedOperationException: cannot set text if end tag is forbidden");
      } catch (UnsupportedOperationException e) {
        // Expected.
      }

      try {
        T builder = createElementBuilder(factory);
        builder.startDiv();
        fail("Expected UnsupportedOperationException: "
            + "cannot append a child if end tag is forbidden");
      } catch (UnsupportedOperationException e) {
        // Expected.
      }
    }
  }

  /**
   * Test that all implementations of a builder return the same value from
   * {@link ElementBuilderBase#isEndTagForbidden()}.
   */
  public void testIsEndTagForbiddenConsistent() {
    boolean expected = false;
    ElementBuilderFactory[] factories = getFactories();
    for (int i = 0; i < factories.length; i++) {
      T builder = createElementBuilder(factories[0]);
      assertEquals(isEndTagForbidden, builder.isEndTagForbidden());
    }
  }

  public void testLangAfterAppendHtml() {
    assertActionFailsAfterAppendHtml(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.lang("value");
      }
    }, "Cannot add attribute after appending html");
  }

  /**
   * Test that all implementations of a builder are consistent in their support
   * of appending child elements.
   */
  public void testStartConsistent() {
    boolean expected = false;
    ElementBuilderFactory[] factories = getFactories();
    for (int i = 0; i < factories.length; i++) {
      T builder = createElementBuilder(factories[0]);
      assertEquals(isChildElementSupported, builder.isChildElementSupported());
    }
  }

  public void testStartSecondTopLevelElement() {
    for (ElementBuilderFactory factory : getFactories()) {
      T builder = createElementBuilder(factory);
      builder.end(); // Close the top level attribute.

      try {
        startElement(builder);
        fail("Expected IllegalStateException: Cannot start multiple top level attributes");
      } catch (IllegalStateException e) {
        // Expected.
      }
    }
  }

  public void testStylePropertyAfterAppendHtml() {
    assertActionFailsAfterAppendHtml(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        // Accessing the style builder is allowed.
        StylesBuilder style;
        try {
          style = builder.style();
        } catch (RuntimeException e) {
          fail("Accessing StyleBuilder should not trigger an error: " + e.getMessage());
          throw e;
        }

        // Using it is not.
        style.trustedProperty("name", "value");
      }
    }, "Cannot access StyleBuilder appending html");
  }

  public void testStylePropertyAfterEnd() {
    assertActionFailsAfterEnd(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        // Accessing the style builder is allowed.
        StylesBuilder style;
        try {
          style = builder.style();
        } catch (RuntimeException e) {
          fail("Accessing StyleBuilder should not trigger an error: " + e.getMessage());
          throw e;
        }

        // Using it is not.
        style.trustedProperty("name", "value");
      }
    }, "Cannot access StyleBuilder appending html");
  }

  /**
   * Test that you cannot add style properties after interrupting them with an
   * attribute.
   */
  public void testStyleTwice() {
    for (ElementBuilderFactory factory : getFactories()) {
      T builder = createElementBuilder(factory);

      // Access style first time.
      StylesBuilder style = builder.style().borderWidth(1.0, Unit.PX).fontSize(10.0, Unit.PX);

      // Access style again, without interruption.
      builder.style().trustedColor("red");

      // Add an attribute.
      builder.id("id");

      // Accessing style after interruption is allowed.
      StylesBuilder style0 = builder.style();

      // Using it is not.
      try {
        style0.left(1.0, Unit.PX);
        fail("Expected IllegalStateException: Cannot access StyleBuilder after interruption");
      } catch (IllegalStateException e) {
        // Expected.
      }

      // Reuse existing style after interruption.
      try {
        style.left(1.0, Unit.PX);
        fail("Expected IllegalStateException: Cannot access StyleBuilder after interruption");
      } catch (IllegalStateException e) {
        // Expected.
      }
    }
  }

  public void testTabIndexAfterAppendHtml() {
    assertActionFailsAfterAppendHtml(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.tabIndex(1);
      }
    }, "Cannot add attribute after appending html");
  }

  public void testTextAfterAppend() {
    assertActionFailsAfterAppendHtml(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.text("hello world");
      }
    }, "Cannot set html after appending a child element");
  }

  public void testTextAfterEnd() {
    if (!isInnerTextSupported) {
      return;
    }

    assertActionFailsAfterEnd(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.text("hello world");
      }
    }, "Cannot set text after adding a child element");
  }

  /**
   * Test that text can be set after ending one element and starting another.
   */
  public void testTextAfterRestart() {
    if (!isInnerTextSupported || !isChildElementSupported) {
      return;
    }

    for (ElementBuilderFactory factory : getFactories()) {
      T builder = createElementBuilder(factory);
      builder.startDiv().id("test").text("test").end();

      // Should not cause any errors.
      builder.startDiv().text("hello");
    }
  }

  /**
   * Test that all implementations of a builder are consistent in their support
   * of setting inner text.
   */
  public void testTextConsistent() {
    boolean expected = false;
    ElementBuilderFactory[] factories = getFactories();
    for (int i = 0; i < factories.length; i++) {
      T builder = createElementBuilder(factories[0]);
      boolean isSupported = true;
      try {
        builder.text("");
      } catch (UnsupportedOperationException e) {
        // Child elements are not supported.
        isSupported = false;
      }

      assertEquals(isInnerTextSupported, isSupported);
    }
  }

  public void testTitleAfterAppendHtml() {
    assertActionFailsAfterAppendHtml(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.title("value");
      }
    }, "Cannot add attribute after appending html");
  }

  /**
   * Test that the specified command triggers an {@link IllegalStateException}
   * after appending html to the element.
   * 
   * @param action the command to execute
   * @param message the failure message if the test fails
   */
  protected void assertActionFailsAfterAppendHtml(BuilderCommand<T> action, String message) {
    // Skip this test if inner html is not supported.
    if (!isInnerHtmlSupported) {
      return;
    }

    for (ElementBuilderFactory factory : getFactories()) {
      T builder = createElementBuilder(factory);
      builder.html(SafeHtmlUtils.EMPTY_SAFE_HTML);

      try {
        action.execute(builder);
        fail("Expected IllegalStateException: " + message);
      } catch (IllegalStateException e) {
        // Expected.
      }
    }
  }

  /**
   * Test that the specified command triggers an {@link IllegalStateException}
   * after ending a child element.
   * 
   * @param action the command to execute
   * @param message the failure message if the test fails
   */
  protected void assertActionFailsAfterEnd(BuilderCommand<T> action, String message) {
    // Skip this test if child elements are not supported.
    if (!isChildElementSupported) {
      return;
    }

    for (ElementBuilderFactory factory : getFactories()) {
      T builder = createElementBuilder(factory);

      // Add a child.
      builder.startDiv().end();

      try {
        action.execute(builder);
        fail("Expected IllegalStateException: " + message);
      } catch (IllegalStateException e) {
        // Expected.
      }
    }
  }

  /**
   * Create an {@link ElementBuilderBase} to test.
   * 
   * @param factory the {@link ElementBuilderFactory} used to create the element
   */
  protected abstract T createElementBuilder(ElementBuilderFactory factory);

  /**
   * End the element within an existing builder.
   * 
   * @param builder the existing builder
   */
  protected abstract void endElement(ElementBuilderBase<?> builder);

  /**
   * Get the array of factories to test.
   * 
   * @return an array of factories to test.
   */
  protected ElementBuilderFactory[] getFactories() {
    if (getModuleName() == null) {
      // JRE tests only work with HtmlBuilderFactory.
      return new ElementBuilderFactory[]{HtmlBuilderFactory.get()};
    } else {
      // GWT tests work with both implementations.
      return new ElementBuilderFactory[]{HtmlBuilderFactory.get(), DomBuilderFactory.get()};
    }
  }

  @Override
  protected void gwtSetUp() throws Exception {
    isChildElementSupported = createElementBuilder(getFactories()[0]).isChildElementSupported();
    isEndTagForbidden = createElementBuilder(getFactories()[0]).isEndTagForbidden();
    isInnerHtmlSupported = true;
    isInnerTextSupported = true;

    try {
      createElementBuilder(getFactories()[0]).html(SafeHtmlUtils.EMPTY_SAFE_HTML);
    } catch (UnsupportedOperationException e) {
      isInnerHtmlSupported = false;
    }

    try {
      createElementBuilder(getFactories()[0]).text("");
    } catch (UnsupportedOperationException e) {
      isInnerTextSupported = false;
    }
  }

  protected boolean isInnerHtmlSupported() {
    return true;
  }

  /**
   * Start a new element within an existing builder.
   * 
   * @param builder the existing builder
   * @return the builder for the new element
   */
  protected abstract T startElement(ElementBuilderBase<?> builder);
}
