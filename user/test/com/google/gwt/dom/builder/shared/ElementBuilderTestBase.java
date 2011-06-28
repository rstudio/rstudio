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
   * The GWT module name when running tests on the client.
   */
  protected static final String GWT_MODULE_NAME = "com.google.gwt.dom.builder.DomBuilder";

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

  @Override
  public String getModuleName() {
    // Default to JVM implementation.
    return null;
  }

  /**
   * Test that you cannot append text, html, or elements after setting the text.
   */
  public void testAppendAfterHtml() {
    T builder = createElementBuilder();
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

  /**
   * Test that you cannot append text, html, or elements after setting the text.
   */
  public void testAppendAfterText() {
    T builder = createElementBuilder();
    builder.text("Hello World");

    try {
      builder.text("moretext");
      fail("Expected IllegalStateException: setting text twice");
    } catch (IllegalStateException e) {
      // Expected.
    }

    try {
      builder.html(SafeHtmlUtils.fromString("morehtml"));
      fail("Expected IllegalStateException: setting html after setting text");
    } catch (IllegalStateException e) {
      // Expected.
    }

    try {
      builder.startDiv();
      fail("Expected IllegalStateException: appending a div after setting text");
    } catch (IllegalStateException e) {
      // Expected.
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

  public void testEndReturnType() {
    T builder = createElementBuilder();
    DivBuilder divBuilder0 = builder.startDiv();
    DivBuilder divBuilder1 = divBuilder0.startDiv();
    assertEquals(divBuilder0, divBuilder1.end());
    assertEquals(builder, divBuilder0.end());
    assertNull(builder.end());
  }

  public void testEndReturnTypeSpecified() {
    T builder = createElementBuilder();
    DivBuilder divBuilder0 = builder.startDiv();
    DivBuilder divBuilder1 = divBuilder0.startDiv();
    assertEquals(divBuilder0, divBuilder1.<DivBuilder> end());
  }

  public void testEndUnmatchedTagName() {
    T builder = createElementBuilder();
    builder.startDiv();

    try {
      builder.end("span");
      fail("Expected IllegalStateException: Started div but ended span");
    } catch (IllegalStateException e) {
      // Expected.
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
    T builder = createElementBuilder();
    builder.startDiv().id("test").html(SafeHtmlUtils.fromString("test")).end();

    // Should not cause any errors.
    builder.startDiv().html(SafeHtmlUtils.fromString("hello"));
  }

  public void testIdNameAfterAppendHtml() {
    assertActionFailsAfterAppendHtml(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.id("value");
      }
    }, "Cannot add attribute after appending html");
  }

  public void testLangAfterAppendHtml() {
    assertActionFailsAfterAppendHtml(new BuilderCommand<T>() {
      @Override
      public void execute(T builder) {
        builder.lang("value");
      }
    }, "Cannot add attribute after appending html");
  }

  public void testStartSecondTopLevelElement() {
    T builder = createElementBuilder();
    builder.end(); // Close the top level attribute.

    try {
      startElement(builder);
      fail("Expected IllegalStateException: Cannot start multiple top level attributes");
    } catch (IllegalStateException e) {
      // Expected.
    }
  }

  /**
   * Test that you cannot add style properties after interrupting them with an
   * attribute.
   */
  public void testStyleTwice() {
    T builder = createElementBuilder();

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
  public void testTextAfterRetart() {
    T builder = createElementBuilder();
    builder.startDiv().id("test").text("test").end();

    // Should not cause any errors.
    builder.startDiv().text("hello");
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
    T builder = createElementBuilder();
    builder.html(SafeHtmlUtils.EMPTY_SAFE_HTML);

    try {
      action.execute(builder);
      fail("Expected IllegalStateException: " + message);
    } catch (IllegalStateException e) {
      // Expected.
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
    T builder = createElementBuilder();

    // Add a child.
    builder.startDiv().end();

    try {
      action.execute(builder);
      fail("Expected IllegalStateException: " + message);
    } catch (IllegalStateException e) {
      // Expected.
    }
  }

  /**
   * Create an {@link ElementBuilderBase} to test.
   */
  protected abstract T createElementBuilder();

  /**
   * Start a new element within an existing builder.
   * 
   * @param builder the existing builder
   * @return the builder for the new element
   */
  protected abstract T startElement(ElementBuilderBase<?> builder);
}
