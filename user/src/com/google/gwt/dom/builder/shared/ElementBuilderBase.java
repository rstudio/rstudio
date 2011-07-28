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
   */
  void end();

  /**
   * End the current element after checking that its tag is the specified
   * tagName.
   * 
   * @param tagName the expected tagName of the current element
   * @see #end()
   */
  void end(String tagName);

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endAnchor();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endArea();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endAudio();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endBase();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endBlockQuote();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endBody();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endBR();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endButton();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endCanvas();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endCol();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endColGroup();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endDiv();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endDList();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endFieldSet();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endForm();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endFrame();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endFrameSet();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endH1();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endH2();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endH3();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endH4();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endH5();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endH6();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endHead();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endHR();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endIFrame();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endImage();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endInput();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endLabel();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endLegend();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endLI();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endLink();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endMap();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endMeta();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endOList();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endOptGroup();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endOption();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endParagraph();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endParam();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endPre();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endQuote();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endScript();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endSelect();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endSource();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endSpan();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endStyle();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endTable();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endTableCaption();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endTBody();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endTD();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endTextArea();

  /**
   * End the current element. . *
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endTFoot();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endTH();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endTHead();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endTR();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endUList();

  /**
   * End the current element.
   * 
   * @throws IllegalStateException if the current element has the wrong tag
   * @see #end()
   */
  void endVideo();

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
   * Get the element depth of the current builder.
   */
  int getDepth();

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
   * Check if child elements are supported.
   * 
   * @return true if supported, false if not.
   */
  boolean isChildElementSupported();

  /**
   * Check if an end tag is forbidden for this element. If the end tag is
   * forbidden, then setting inner html or text or appending an element will
   * trigger an {@link UnsupportedOperationException}.
   * 
   * @return true if forbidden, false if not
   */
  boolean isEndTagForbidden();

  /**
   * Language code defined in RFC 1766.
   * 
   * @return this builder
   */
  T lang(String lang);

  /**
   * Append a anchor element.
   * 
   * @return the builder for the new element
   */
  AnchorBuilder startAnchor();

  /**
   * Append a area element.
   * 
   * @return the builder for the new element
   */
  AreaBuilder startArea();

  /**
   * Append a audio element.
   * 
   * @return the builder for the new element
   */
  AudioBuilder startAudio();

  /**
   * Append a base element.
   * 
   * @return the builder for the new element
   */
  BaseBuilder startBase();

  /**
   * Append a block quote element.
   * 
   * @return the builder for the new element
   */
  QuoteBuilder startBlockQuote();

  /**
   * Append a body element.
   * 
   * @return the builder for the new element
   */
  BodyBuilder startBody();

  /**
   * Append a br element.
   * 
   * @return the builder for the new element
   */
  BRBuilder startBR();

  /**
   * Append an &lt;input type='button'&gt; element.
   * 
   * @return the builder for the new element
   */
  InputBuilder startButtonInput();

  /**
   * Append a canvas element.
   * 
   * @return the builder for the new element
   */
  CanvasBuilder startCanvas();

  /**
   * Append an &lt;input type='check'&gt; element.
   * 
   * @return the builder for the new element
   */
  InputBuilder startCheckboxInput();

  /**
   * Append a tablecol element.
   * 
   * @return the builder for the new element
   */
  TableColBuilder startCol();

  /**
   * Append a tablecol element.
   * 
   * @return the builder for the new element
   */
  TableColBuilder startColGroup();

  /**
   * Append a div element.
   * 
   * @return the builder for the new element
   */
  DivBuilder startDiv();

  /**
   * Append a dlist element.
   * 
   * @return the builder for the new element
   */
  DListBuilder startDList();

  /**
   * Append a fieldset element.
   * 
   * @return the builder for the new element
   */
  FieldSetBuilder startFieldSet();

  /**
   * Append an &lt;input type='file'&gt; element.
   * 
   * @return the builder for the new element
   */
  InputBuilder startFileInput();

  /**
   * Append a form element.
   * 
   * @return the builder for the new element
   */
  FormBuilder startForm();

  /**
   * Append a frame element.
   * 
   * @return the builder for the new element
   */
  FrameBuilder startFrame();

  /**
   * Append a frameset element.
   * 
   * @return the builder for the new element
   */
  FrameSetBuilder startFrameSet();

  /**
   * Append a heading element.
   * 
   * @return the builder for the new element
   */
  HeadingBuilder startH1();

  /**
   * Append a heading element.
   * 
   * @return the builder for the new element
   */
  HeadingBuilder startH2();

  /**
   * Append a heading element.
   * 
   * @return the builder for the new element
   */
  HeadingBuilder startH3();

  /**
   * Append a heading element.
   * 
   * @return the builder for the new element
   */
  HeadingBuilder startH4();

  /**
   * Append a heading element.
   * 
   * @return the builder for the new element
   */
  HeadingBuilder startH5();

  /**
   * Append a heading element.
   * 
   * @return the builder for the new element
   */
  HeadingBuilder startH6();

  /**
   * Append a head element.
   * 
   * @return the builder for the new element
   */
  HeadBuilder startHead();

  /**
   * Append an &lt;input type='hidden'&gt; element.
   * 
   * @return the builder for the new element
   */
  InputBuilder startHiddenInput();

  /**
   * Append a hr element.
   * 
   * @return the builder for the new element
   */
  HRBuilder startHR();

  /**
   * Append a iframe element.
   * 
   * @return the builder for the new element
   */
  IFrameBuilder startIFrame();

  /**
   * Append a image element.
   * 
   * @return the builder for the new element
   */
  ImageBuilder startImage();

  /**
   * Append an &lt;input type='image'&gt; element.
   * 
   * @return the builder for the new element
   */
  InputBuilder startImageInput();

  /**
   * Append a label element.
   * 
   * @return the builder for the new element
   */
  LabelBuilder startLabel();

  /**
   * Append a legend element.
   * 
   * @return the builder for the new element
   */
  LegendBuilder startLegend();

  /**
   * Append a li element.
   * 
   * @return the builder for the new element
   */
  LIBuilder startLI();

  /**
   * Append a link element.
   * 
   * @return the builder for the new element
   */
  LinkBuilder startLink();

  /**
   * Append a map element.
   * 
   * @return the builder for the new element
   */
  MapBuilder startMap();

  /**
   * Append a meta element.
   * 
   * @return the builder for the new element
   */
  MetaBuilder startMeta();

  /**
   * Append a olist element.
   * 
   * @return the builder for the new element
   */
  OListBuilder startOList();

  /**
   * Append a optgroup element.
   * 
   * @return the builder for the new element
   */
  OptGroupBuilder startOptGroup();

  /**
   * Append an option element.
   * 
   * @return the builder for the new element
   */
  OptionBuilder startOption();

  /**
   * Append a paragraph element.
   * 
   * @return the builder for the new element
   */
  ParagraphBuilder startParagraph();

  /**
   * Append a param element.
   * 
   * @return the builder for the new element
   */
  ParamBuilder startParam();

  /**
   * Append an &lt;input type='password'&gt; element.
   * 
   * @return the builder for the new element
   */
  InputBuilder startPasswordInput();

  /**
   * Append a pre element.
   * 
   * @return the builder for the new element
   */
  PreBuilder startPre();

  /**
   * Append a button element with type "button".
   * 
   * @return the builder for the new element
   */
  ButtonBuilder startPushButton();

  /**
   * Append a quote element.
   * 
   * @return the builder for the new element
   */
  QuoteBuilder startQuote();

  /**
   * Append an &lt;input type='radio'&gt; element.
   * 
   * @param name name the name of the radio input (used for grouping)
   * @return the builder for the new element
   */
  InputBuilder startRadioInput(String name);

  /**
   * Append a button element with type "reset".
   * 
   * @return the builder for the new element
   */
  ButtonBuilder startResetButton();

  /**
   * Append an &lt;input type='reset'&gt; element.
   * 
   * @return the builder for the new element
   */
  InputBuilder startResetInput();

  /**
   * Append a script element.
   * 
   * @return the builder for the new element
   */
  ScriptBuilder startScript();

  /**
   * Append a select element.
   * 
   * @return the builder for the new element
   */
  SelectBuilder startSelect();

  /**
   * Append a source element.
   * 
   * @return the builder for the new element
   */
  SourceBuilder startSource();

  /**
   * Append a span element.
   * 
   * @return the builder for the new element
   */
  SpanBuilder startSpan();

  /**
   * Append a style element.
   * 
   * @return the builder for the new element
   */
  StyleBuilder startStyle();

  /**
   * Append a button element with type "submit".
   * 
   * @return the builder for the new element
   */
  ButtonBuilder startSubmitButton();

  /**
   * Append an &lt;input type='submit'&gt; element.
   * 
   * @return the builder for the new element
   */
  InputBuilder startSubmitInput();

  /**
   * Append a table element.
   * 
   * @return the builder for the new element
   */
  TableBuilder startTable();

  /**
   * Append a table caption element.
   * 
   * @return the builder for the new element
   */
  TableCaptionBuilder startTableCaption();

  /**
   * Append a tbody element.
   * 
   * @return the builder for the new element
   */
  TableSectionBuilder startTBody();

  /**
   * Append a td element.
   * 
   * @return the builder for the new element
   */
  TableCellBuilder startTD();

  /**
   * Append a textarea element.
   * 
   * @return the builder for the new element
   */
  TextAreaBuilder startTextArea();

  /**
   * Append an &lt;input type='text'&gt; element.
   * 
   * @return the builder for the new element
   */
  InputBuilder startTextInput();

  /**
   * Append a tfoot element.
   * 
   * @return the builder for the new element
   */
  TableSectionBuilder startTFoot();

  /**
   * Append a th element.
   * 
   * @return the builder for the new element
   */
  TableCellBuilder startTH();

  /**
   * Append a thead element.
   * 
   * @return the builder for the new element
   */
  TableSectionBuilder startTHead();

  /**
   * Append a tablerow element.
   * 
   * @return the builder for the new element
   */
  TableRowBuilder startTR();

  /**
   * Append a ulist element.
   * 
   * @return the builder for the new element
   */
  UListBuilder startUList();

  /**
   * Append a video element.
   * 
   * @return the builder for the new element
   */
  VideoBuilder startVideo();

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
