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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SafeHtml;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility implementation of {@link ElementBuilderBase} that handles state, but
 * nothing else.
 * 
 * <p>
 * DO NOT USE THIS CLASS. This class is an implementation class and may change
 * in the future.
 * </p>
 * 
 * This class is used to ensure that the HTML and DOM implementations throw the
 * same exceptions, even if something is valid in one and not the other. For
 * example, the DOM implementation supports changing an attribute after setting
 * inner HTML, but the HTML version does not, so they should both throw an
 * error. Otherwise, they would not be swappable.
 */
public abstract class ElementBuilderImpl {

  /**
   * A regex for matching valid HTML tags.
   */
  private static RegExp HTML_TAG_REGEX;

  private boolean asElementCalled;

  /**
   * True if the top most element has not yet been added.
   */
  private boolean isEmpty = true;

  /**
   * True if HTML of text has been added.
   */
  private boolean isHtmlOrTextAdded;

  /**
   * True if the start tag is still open (open bracket '&lt;' but no close
   * bracket '&gt;').
   */
  private boolean isStartTagOpen;

  /**
   * True if the style attribute has been opened and closed.
   */
  private boolean isStyleClosed;

  /**
   * True if the style attribute is open.
   */
  private boolean isStyleOpen;

  /**
   * The stack of element builders.
   */
  private final List<ElementBuilderBase<?>> stackBuilders = new ArrayList<ElementBuilderBase<?>>();

  /**
   * The stack of tag names.
   */
  private final List<String> stackTags = new ArrayList<String>();

  public ElementBuilderImpl() {
    if (HTML_TAG_REGEX == null) {
      // Starts with a-z, plus 0 or more alphanumeric values (case insensitive).
      HTML_TAG_REGEX = RegExp.compile("^[a-z][a-z0-9]*$", "i");
    }
  }

  public ElementBuilderBase<?> end() {
    // Get the top item (also verifies there is a top item).
    String tagName = getCurrentTagName();

    // Close the start tag.
    maybeCloseStartTag();

    /*
     * End the tag. The tag name is safe because it comes from the stack, and
     * tag names are checked before they are added to the stack.
     */
    doEndTagImpl(tagName);

    // Popup the item off the top of the stack.
    isStartTagOpen = false; // Closed because this element was added.
    isStyleClosed = true; // Too late to add styles.
    stackTags.remove(stackTags.size() - 1);
    stackBuilders.remove(stackBuilders.size() - 1);

    /*
     * If this element was added, then we did not add html or text to the
     * parent.
     */
    isHtmlOrTextAdded = false;

    return getCurrentBuilder();
  }

  public ElementBuilderBase<?> end(String tagName) {
    // Verify the tag name matches the expected tag.
    String topItem = getCurrentTagName();
    if (!topItem.equalsIgnoreCase(tagName)) {
      throw new IllegalStateException("Specified tag \"" + tagName
          + "\" does not match the current element \"" + topItem + "\"");
    }

    // End the element.
    return end();
  }

  public ElementBuilderBase<?> endStyle() {
    if (!isStyleOpen) {
      throw new IllegalStateException(
          "Attempting to close a style attribute, but the style attribute isn't open");
    }
    maybeCloseStyleAttribute();
    return getCurrentBuilder();
  }

  /**
   * Return the built DOM as an {@link Element}.
   * 
   * @return the {@link Element} that was built
   */
  public Element finish() {
    if (!GWT.isClient()) {
      throw new RuntimeException("asElement() can only be called from GWT client code.");
    }
    if (asElementCalled) {
      throw new IllegalStateException("asElement() can only be called once.");
    }
    asElementCalled = true;

    // End all open tags.
    endAllTags();

    return doFinishImpl();
  }

  public void html(SafeHtml html) {
    assertStartTagOpen("html cannot be set on an element that already "
        + "contains other content or elements.");
    maybeCloseStartTag();
    isHtmlOrTextAdded = true;
    doHtmlImpl(html);
  }

  public void onStart(String tagName, ElementBuilderBase<?> builder) {
    // Check that we aren't creating another top level element.
    if (isEmpty) {
      isEmpty = false;
    } else if (stackTags.size() == 0) {
      throw new IllegalStateException("You can only build one top level element.");
    }

    // Check that asElement hasn't already been called.
    if (isHtmlOrTextAdded) {
      throw new IllegalStateException("Cannot append an element after setting text of html.");
    }

    // Validate the tagName.
    assertValidTagName(tagName);

    maybeCloseStartTag();
    stackTags.add(tagName);
    stackBuilders.add(builder);
    isStartTagOpen = true;
    isStyleOpen = false;
    isStyleClosed = false;
    isHtmlOrTextAdded = false;
  }

  /**
   * Get the {@link StylesBuilder} used to add style properties to the current
   * element.
   * 
   * @return a {@link StylesBuilder}
   */
  public abstract StylesBuilder style();

  public void text(String text) {
    assertStartTagOpen("text cannot be set on an element that already "
        + "contains other content or elements.");
    maybeCloseStartTag();
    isHtmlOrTextAdded = true;
    doTextImpl(text);
  }

  /**
   * Assert that the builder is in a state where an attribute can be added.
   * 
   * @throw {@link IllegalStateException} if the start tag is closed
   */
  protected void assertCanAddAttributeImpl() {
    assertStartTagOpen("Attributes cannot be added after appending HTML or adding a child element.");
    maybeCloseStyleAttribute();
  }

  /**
   * Assert that a style property can be added, and setup the state as if one is
   * about to be added.
   */
  protected void assertCanAddStylePropertyImpl() {
    assertStartTagOpen("Style properties cannot be added after appending HTML or adding a child "
        + "element.");

    // Check if a style attribute already exists.
    if (isStyleClosed) {
      throw new IllegalStateException(
          "Style properties must be added at the same time. If you already added style properties,"
              + " you cannot add more after adding non-style attributes.");
    }

    // Open the style attribute.
    if (!isStyleOpen) {
      isStyleOpen = true;
      doOpenStyleImpl();
    }
  }

  /**
   * Assert that the specified tag name is valid.
   * 
   * @throws IllegalArgumentException if not valid
   */
  protected void assertValidTagName(String tagName) {
    if (!HTML_TAG_REGEX.test(tagName)) {
      throw new IllegalArgumentException("The specified tag name is invalid: " + tagName);
    }
  }

  /**
   * Close the start tag.
   */
  protected abstract void doCloseStartTagImpl();

  /**
   * Close the style attribute.
   */
  protected abstract void doCloseStyleAttributeImpl();

  /**
   * End the specified tag.
   * 
   * @param tagName the name of the tag to end
   */
  protected abstract void doEndTagImpl(String tagName);

  /**
   * Return the build element.
   * 
   * @return the element
   */
  protected abstract Element doFinishImpl();

  /**
   * Set the specified html as the inner HTML of the current element.
   * 
   * @param html the HTML to set
   */
  protected abstract void doHtmlImpl(SafeHtml html);

  /**
   * Open the style attribute.
   */
  protected abstract void doOpenStyleImpl();

  /**
   * Set the specified text as the inner text of the current element.
   * 
   * @param text the text to set
   */
  protected abstract void doTextImpl(String text);

  /**
   * End all open tags, including the root element.
   * 
   * <p>
   * Doing so also ensures that all builder methods will throw an exception
   * because the stack is empty, and a new element cannot be started.
   * </p>
   */
  protected void endAllTags() {
    while (!stackTags.isEmpty()) {
      end();
    }
  }

  /**
   * Assert that the start tag is still open.
   * 
   * @param message the error message if the start tag is not open
   * @throw {@link IllegalStateException} if the start tag is closed
   */
  private void assertStartTagOpen(String message) {
    if (!isStartTagOpen) {
      throw new IllegalStateException(message);
    }
  }

  /**
   * Get the builder at the top of the stack.
   * 
   * @return an {@link ElementBuilderBase}, or null if there are non left
   */
  private ElementBuilderBase<?> getCurrentBuilder() {
    int stackSize = stackBuilders.size();
    return (stackSize == 0) ? null : stackBuilders.get(stackSize - 1);
  }

  /**
   * Get the tag name of the element at the top of the stack.
   * 
   * @return a tag name
   * @throws IllegalStateException if there are no elements on the stack
   */
  private String getCurrentTagName() {
    // Verify there is something on the stack.
    int stackSize = stackTags.size();
    if (stackSize == 0) {
      throw new IllegalStateException("There are no elements on the stack.");
    }

    return stackTags.get(stackSize - 1);
  }

  /**
   * Close the start tag if it is still open.
   */
  private void maybeCloseStartTag() {
    maybeCloseStyleAttribute();
    if (isStartTagOpen) {
      isStartTagOpen = false;
      doCloseStartTagImpl();
    }
  }

  /**
   * Close the style attribute if it is still open.
   */
  private void maybeCloseStyleAttribute() {
    if (isStyleOpen) {
      isStyleOpen = false;
      isStyleClosed = true;
      doCloseStyleAttributeImpl();
    }
  }
}
