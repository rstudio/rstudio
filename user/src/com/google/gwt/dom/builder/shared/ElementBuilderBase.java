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

import com.google.gwt.dom.client.Element;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * Base class for element builders used to builder DOM elements.
 * 
 * @param <T> the builder type returns from build methods
 */
public interface ElementBuilderBase<T extends ElementBuilderBase<?>> {

  /**
   * Add an integer attribute to the object.
   * 
   * @return this builder
   */
  T attribute(String name, int value);

  /**
   * Add a string attribute to the object.
   * 
   * @return this builder
   */
  T attribute(String name, String value);

  /**
   * The class attribute of the element. This attribute has been renamed due to
   * conflicts with the "class" keyword exposed by many languages.
   * 
   * @return this builder
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-class">W3C
   *      HTML Specification</a>
   */
  T className(String className);

  /**
   * Specifies the base direction of directionally neutral text and the
   * directionality of tables.
   * 
   * @return this builder
   */
  T dir(String dir);

  /**
   * Changes the draggable attribute to one of {@link Element#DRAGGABLE_AUTO},
   * {@link Element#DRAGGABLE_FALSE}, or {@link Element#DRAGGABLE_TRUE}.
   * 
   * @param draggable a String constant
   * @return this builder
   */
  T draggable(String draggable);

  /**
   * End the current element without checking its type.
   * 
   * <p>
   * By default, this method returns the {@link ElementBuilderBase} instance for
   * the parent element, or null if this is the root element.
   * </p>
   * 
   * <pre>
   * DivBuilder div = ElementBuilderFactory.get().createDivBuilder();
   * SelectBuilder select = div.startSelect();
   * ElementBuilderBase&lt;?&gt; sameAs_div = select.end();
   * </pre>
   * 
   * <p>
   * You can cast the return type by parameterizing the return value. If the
   * parameterized type does not match the builder type of this element's
   * parent, a {@link ClassCastException} is thrown.
   * </p>
   * 
   * <pre>
   * DivBuilder div = ElementBuilderFactory.get().createDivBuilder();
   * SelectBuilder select = div.startSelect();
   * DivBuilder sameAs_div = select.&lt;DivBuilder&gt;end();
   * </pre>
   * 
   * @param <B> the type of the parent element
   * @return the {@link ElementBuilderBase} for the parent element, or null if
   *         the current element does not have a parent
   * @throws ClassCastException if the return type does not match the builder
   *           type of this element's parent
   */
  <B extends ElementBuilderBase<?>> B end();

  /**
   * End the current element after checking that its tag is the specified
   * tagName.
   * 
   * @param tagName the expected tagName of the current element
   * @return the {@link ElementBuilderBase} for the parent element, or null if
   *         the current element does not have a parent
   * @throws IllegalStateException if the current element does not match the
   *           expected tagName
   * @throws ClassCastException if the parent builder does not match the
   *           specified class
   * @see #end()
   */
  <B extends ElementBuilderBase<?>> B end(String tagName);

  /**
   * End the current element.
   * 
   * @param <B> the type of the parent element
   * @return the {@link ElementBuilderBase} for the parent element, or null if
   *         the current element does not have a parent
   * @throws IllegalStateException if the current element has the wrong tag
   * @throws ClassCastException if the parent builder does not match the
   *           specified class
   * @see #end()
   */
  <B extends ElementBuilderBase<?>> B endDiv();

  /**
   * End the current element.
   * 
   * @param <B> the type of the parent element
   * @return the {@link ElementBuilderBase} for the parent element, or null if
   *         the current element does not have a parent
   * @throws IllegalStateException if the current element has the wrong tag
   * @throws ClassCastException if the parent builder does not match the
   *           specified class
   * @see #end()
   */
  <B extends ElementBuilderBase<?>> B endOption();

  /**
   * End the current element.
   * 
   * @param <B> the type of the parent element
   * @return the {@link ElementBuilderBase} for the parent element, or null if
   *         the current element does not have a parent
   * @throws IllegalStateException if the current element has the wrong tag
   * @throws ClassCastException if the parent builder does not match the
   *           specified class
   * @see #end()
   */
  <B extends ElementBuilderBase<?>> B endSelect();

  /**
   * Return the built DOM as an {@link Element}.
   * 
   * <p>
   * Any lingering open elements are automatically closed. Once you call
   * {@link #finish()}, you can not longer call any other methods in this class.
   * </p>
   * 
   * @return the {@link Element} that was built
   * @throws IllegalStateException if called twice
   */
  Element finish();

  /**
   * Append html within the node.
   * 
   * <p>
   * Once you append HTML to the element, you can no longer set attributes.
   * 
   * @param html the HTML to append
   * @return this builder
   */
  T html(SafeHtml html);

  /**
   * Set the id.
   * 
   * @param id the id
   * @return this builder
   */
  T id(String id);

  /**
   * Language code defined in RFC 1766.
   * 
   * @return this builder
   */
  T lang(String lang);

  /**
   * Append a div element.
   * 
   * @return the builder for the new element
   */
  DivBuilder startDiv();

  /**
   * Append an option element.
   * 
   * @return the builder for the new element
   */
  OptionBuilder startOption();

  /**
   * Append a select element.
   * 
   * @return the builder for the new element
   */
  SelectBuilder startSelect();

  /**
   * Start the {@link StylesBuilder} used to add style properties to the style
   * attribute of the current element.
   * 
   * @return the {@link StylesBuilder}
   */
  StylesBuilder style();

  /**
   * Set the tab index.
   * 
   * @param tabIndex the tab index
   * @return this builder
   */
  T tabIndex(int tabIndex);

  /**
   * Append text within the node.
   * 
   * <p>
   * Once you append text to the element, you can no longer set attributes.
   * </p>
   * 
   * <p>
   * A string-based implementation will escape the text to prevent
   * HTML/javascript code from executing. DOM based implementations are not
   * required to escape the text if they directly set the innerText of an
   * element.
   * </p>
   * 
   * @param text the text to append
   * @return this builder
   */
  T text(String text);

  /**
   * The element's advisory title.
   * 
   * @return this builder
   */
  T title(String title);

  /**
   * Append a new element with the specified trusted tag name. The tag name will
   * will not be checked or escaped. The calling code should be carefully
   * reviewed to ensure that the provided tag name will not cause a security
   * issue if including in an HTML document. In general, this means limiting the
   * code to HTML tagName constants supported by the HTML specification.
   * 
   * @param tagName the tag name
   * @return the {@link ElementBuilder} for the new element
   */
  ElementBuilder trustedStart(String tagName);
}
