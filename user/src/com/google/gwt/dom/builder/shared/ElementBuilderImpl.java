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

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * Base implementation of {@link ElementBuilderBase} that handles state, but
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
   * A node in the builder stack.
   */
  private static class StackNode {
    private final ElementBuilderBase<?> builder;
    private StackNode next;
    private final String tagName;

    public StackNode(String tagName, ElementBuilderBase<?> builder) {
      this.builder = builder;
      this.tagName = tagName;
    }
  }

  /**
   * A stack that allows quick access to its top element.
   * 
   * <p>
   * FastPeekStack is implemented using a simple linked list of nodes to avoid
   * the dynamic casts associated with the emulated version of
   * {@link java.util.ArrayList}. When constructing a large DOM structure, such
   * as a table, the dynamic casts in ArrayList can significantly degrade
   * performance.
   * </p>
   */
  private class FastPeekStack {

    private static final String EMPTY_STACK_MESSAGE = "There are no elements on the stack.";

    /**
     * The top item in the stack.
     */
    private StackNode top;
    private int size = 0;

    public boolean isEmpty() {
      return (top == null);
    }

    public StackNode peek() {
      assertNotEmpty();
      return top;
    }

    /**
     * Pop the current {@link StackNode} and return it.
     * 
     * <p>
     * The popped node will be recycled and should not be saved.
     * </p>
     * 
     * @return the popped node
     */
    public StackNode pop() {
      assertNotEmpty();
      StackNode toRet = top;
      top = top.next;
      size--;
      return toRet;
    }

    public void push(ElementBuilderBase<?> builder, String tagName) {
      StackNode node = new StackNode(tagName, builder);
      node.next = top;
      top = node;
      size++;
    }

    public int size() {
      return size;
    }

    /**
     * Assert that the stack is not empty.
     * 
     * @throws IllegalStateException if empty
     */
    private void assertNotEmpty() {
      if (isEmpty()) {
        throw new IllegalStateException(EMPTY_STACK_MESSAGE);
      }
    }
  }

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
  private final FastPeekStack stack = new FastPeekStack();

  public ElementBuilderImpl() {
    if (HTML_TAG_REGEX == null) {
      // Starts with a-z, plus 0 or more alphanumeric values (case insensitive).
      HTML_TAG_REGEX = RegExp.compile("^[a-z][a-z0-9]*$", "i");
    }
  }

  public void end() {
    endImpl(getCurrentTagName());
  }

  public void end(String tagName) {
    // Verify the tag name matches the expected tag.
    String topItem = getCurrentTagName();
    if (!topItem.equalsIgnoreCase(tagName)) {
      throw new IllegalStateException("Specified tag \"" + tagName
          + "\" does not match the current element \"" + topItem + "\"");
    }

    // End the element.
    endImpl(topItem);
  }

  public void endStyle() {
    if (!isStyleOpen) {
      throw new IllegalStateException(
          "Attempting to close a style attribute, but the style attribute isn't open");
    }
    maybeCloseStyleAttribute();
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

  public int getDepth() {
    return stack.size();
  }

  public void html(SafeHtml html) {
    assertStartTagOpen("html cannot be set on an element that already "
        + "contains other content or elements.");
    lockCurrentElement();
    doHtmlImpl(html);
  }

  public void onStart(String tagName, ElementBuilderBase<?> builder) {
    if (isEmpty) {
      isEmpty = false;
    } else if (stack.isEmpty()) {
      // Check that we aren't creating another top level element.
      throw new IllegalStateException("You can only build one top level element.");
    } else {
      // Check that the element supports children.
      assertEndTagNotForbidden("child elements");
      if (!getCurrentBuilder().isChildElementSupported()) {
        throw new UnsupportedOperationException(getCurrentTagName()
            + " does not support child elements.");
      }
    }

    // Check that asElement hasn't already been called.
    if (isHtmlOrTextAdded) {
      throw new IllegalStateException("Cannot append an element after setting text of html.");
    }

    // Validate the tagName.
    assertValidTagName(tagName);

    maybeCloseStartTag();
    stack.push(builder, tagName);
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
    lockCurrentElement();
    doTextImpl(text);
  }

  /**
   * Assert that the builder is in a state where an attribute can be added.
   * 
   * @throw {@link IllegalStateException} if the start tag is closed
   */
  protected void assertCanAddAttributeImpl() {
    assertStartTagOpen("Attributes cannot be added after appending HTML or adding a child "
        + "element.");
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
   * Self-close the start tag. This method is called for elements that forbid
   * the end tag.
   */
  protected abstract void doEndStartTagImpl();

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
    while (!stack.isEmpty()) {
      end();
    }
  }

  /**
   * Lock the current element, preventing any additional changes to it. The only
   * valid option is to call {@link #end()}.
   */
  protected void lockCurrentElement() {
    maybeCloseStartTag();
    assertEndTagNotForbidden("html");
    isHtmlOrTextAdded = true;
  }

  /**
   * Assert that the current builder does not forbid end tags.
   * 
   * @param operation the operation that the user is attempting
   * @throw {@link UnsupportedOperationException} if not supported
   */
  private void assertEndTagNotForbidden(String operation) {
    if (getCurrentBuilder().isEndTagForbidden()) {
      throw new UnsupportedOperationException(getCurrentTagName() + " does not support "
          + operation);
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
   * End the current element without checking the tag name.
   * 
   * @param tagName the tag name to end
   */
  private void endImpl(String tagName) {
    // Close the start tag.
    maybeCloseStartTag();

    /*
     * End the tag. The tag name is safe because it comes from the stack, and
     * tag names are checked before they are added to the stack.
     */
    if (getCurrentBuilder().isEndTagForbidden()) {
      doEndStartTagImpl();
    } else {
      doEndTagImpl(tagName);
    }

    // Popup the item off the top of the stack.
    isStartTagOpen = false; // Closed because this element was added.
    isStyleClosed = true; // Too late to add styles.
    stack.pop();

    /*
     * If this element was added, then we did not add html or text to the
     * parent.
     */
    isHtmlOrTextAdded = false;
  }

  /**
   * Get the builder at the top of the stack.
   * 
   * @return an {@link ElementBuilderBase}
   * @throws IllegalStateException if there are no elements on the stack
   */
  private ElementBuilderBase<?> getCurrentBuilder() {
    return stack.peek().builder;
  }

  /**
   * Get the tag name of the element at the top of the stack.
   * 
   * @return a tag name
   * @throws IllegalStateException if there are no elements on the stack
   */
  private String getCurrentTagName() {
    return stack.peek().tagName;
  }

  /**
   * Close the start tag if it is still open.
   */
  private void maybeCloseStartTag() {
    maybeCloseStyleAttribute();
    if (isStartTagOpen) {
      isStartTagOpen = false;
      /*
       * Close the start tag, unless the end tag is forbidden. If the end tag is
       * forbidden, the only valid call is to #end(), which will self end the
       * start tag.
       */
      if (!getCurrentBuilder().isEndTagForbidden()) {
        doCloseStartTagImpl();
      }
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
