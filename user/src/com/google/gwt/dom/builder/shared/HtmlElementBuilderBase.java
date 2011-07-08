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

import com.google.gwt.dom.client.TitleElement;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * Implementation of {@link ElementBuilderBase} that delegates to an
 * {@link HtmlBuilderImpl}.
 * 
 * <p>
 * Subclasses of {@link HtmlElementBuilderBase} act as typed wrappers around a
 * shared {@link ElementBuilderBase} implementation that handles the actual
 * building. The wrappers merely delegate to the shared implementation, so
 * wrapper instances can be reused, avoiding object creation. This approach is
 * necessary so that the return value of common methods, such as
 * {@link #id(String)}, return a typed builder instead of the generic
 * {@link ElementBuilderBase}.
 * </p>
 * 
 * @param <R> the builder type returned from build methods
 */
public class HtmlElementBuilderBase<R extends ElementBuilderBase<?>> extends
    AbstractElementBuilderBase<R> {

  private final HtmlBuilderImpl delegate;

  /**
   * Construct a new {@link HtmlElementBuilderBase}.
   * 
   * @param delegate the delegate that builds the element
   */
  HtmlElementBuilderBase(HtmlBuilderImpl delegate) {
    this(delegate, false);
  }

  /**
   * Construct a new {@link HtmlElementBuilderBase}.
   * 
   * @param delegate the delegate that builds the element
   * @param isEndTagForbidden true if the end tag is forbidden for this element
   */
  HtmlElementBuilderBase(HtmlBuilderImpl delegate, boolean isEndTagForbidden) {
    super(delegate, isEndTagForbidden);
    this.delegate = delegate;
  }

  /**
   * Return the HTML as a {@link SafeHtml} string.
   */
  public SafeHtml asSafeHtml() {
    return delegate.asSafeHtml();
  }

  @Override
  public R attribute(String name, String value) {
    delegate.attribute(name, value);
    return getReturnBuilder();
  }

  @Override
  public R className(String className) {
    return attribute("class", className);
  }

  @Override
  public R dir(String dir) {
    return attribute("dir", dir);
  }

  @Override
  public R draggable(String draggable) {
    return attribute("draggable", draggable);
  }

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
  @SuppressWarnings("unchecked")
  public <B extends ElementBuilderBase<?>> B endTitle() {
    return (B) end(TitleElement.TAG);
  }

  @Override
  public R id(String id) {
    return attribute("id", id);
  }

  @Override
  public R lang(String lang) {
    return attribute("lang", lang);
  }

  @Override
  public AnchorBuilder startAnchor() {
    return delegate.startAnchor();
  }

  @Override
  public AreaBuilder startArea() {
    return delegate.startArea();
  }

  @Override
  public AudioBuilder startAudio() {
    return delegate.startAudio();
  }

  @Override
  public BaseBuilder startBase() {
    return delegate.startBase();
  }

  @Override
  public QuoteBuilder startBlockQuote() {
    return delegate.startBlockQuote();
  }

  @Override
  public BodyBuilder startBody() {
    return delegate.startBody();
  }

  @Override
  public BRBuilder startBR() {
    return delegate.startBR();
  }

  @Override
  public InputBuilder startButtonInput() {
    return delegate.startButtonInput();
  }

  @Override
  public CanvasBuilder startCanvas() {
    return delegate.startCanvas();
  }

  @Override
  public InputBuilder startCheckInput() {
    return delegate.startCheckInput();
  }

  @Override
  public TableColBuilder startCol() {
    return delegate.startCol();
  }

  @Override
  public TableColBuilder startColGroup() {
    return delegate.startColGroup();
  }

  @Override
  public DivBuilder startDiv() {
    return delegate.startDiv();
  }

  @Override
  public DListBuilder startDList() {
    return delegate.startDList();
  }

  @Override
  public FieldSetBuilder startFieldSet() {
    return delegate.startFieldSet();
  }

  @Override
  public InputBuilder startFileInput() {
    return delegate.startFileInput();
  }

  @Override
  public FormBuilder startForm() {
    return delegate.startForm();
  }

  @Override
  public FrameBuilder startFrame() {
    return delegate.startFrame();
  }

  @Override
  public FrameSetBuilder startFrameSet() {
    return delegate.startFrameSet();
  }

  @Override
  public HeadingBuilder startH1() {
    return delegate.startH1();
  }

  @Override
  public HeadingBuilder startH2() {
    return delegate.startH2();
  }

  @Override
  public HeadingBuilder startH3() {
    return delegate.startH3();
  }

  @Override
  public HeadingBuilder startH4() {
    return delegate.startH4();
  }

  @Override
  public HeadingBuilder startH5() {
    return delegate.startH5();
  }

  @Override
  public HeadingBuilder startH6() {
    return delegate.startH6();
  }

  @Override
  public HeadBuilder startHead() {
    return delegate.startHead();
  }

  @Override
  public InputBuilder startHiddenInput() {
    return delegate.startHiddenInput();
  }

  @Override
  public HRBuilder startHR() {
    return delegate.startHR();
  }

  @Override
  public IFrameBuilder startIFrame() {
    return delegate.startIFrame();
  }

  @Override
  public ImageBuilder startImage() {
    return delegate.startImage();
  }

  @Override
  public InputBuilder startImageInput() {
    return delegate.startImageInput();
  }

  @Override
  public LabelBuilder startLabel() {
    return delegate.startLabel();
  }

  @Override
  public LegendBuilder startLegend() {
    return delegate.startLegend();
  }

  @Override
  public LIBuilder startLI() {
    return delegate.startLI();
  }

  @Override
  public LinkBuilder startLink() {
    return delegate.startLink();
  }

  @Override
  public MapBuilder startMap() {
    return delegate.startMap();
  }

  @Override
  public MetaBuilder startMeta() {
    return delegate.startMeta();
  }

  @Override
  public OListBuilder startOList() {
    return delegate.startOList();
  }

  @Override
  public OptGroupBuilder startOptGroup() {
    return delegate.startOptGroup();
  }

  @Override
  public OptionBuilder startOption() {
    return delegate.startOption();
  }

  @Override
  public ParagraphBuilder startParagraph() {
    return delegate.startParagraph();
  }

  @Override
  public ParamBuilder startParam() {
    return delegate.startParam();
  }

  @Override
  public InputBuilder startPasswordInput() {
    return delegate.startPasswordInput();
  }

  @Override
  public PreBuilder startPre() {
    return delegate.startPre();
  }

  @Override
  public ButtonBuilder startPushButton() {
    return delegate.startPushButton();
  }

  @Override
  public QuoteBuilder startQuote() {
    return delegate.startQuote();
  }

  @Override
  public InputBuilder startRadioInput(String name) {
    return delegate.startRadioInput(name);
  }

  @Override
  public ButtonBuilder startResetButton() {
    return delegate.startResetButton();
  }

  @Override
  public InputBuilder startResetInput() {
    return delegate.startResetInput();
  }

  @Override
  public ScriptBuilder startScript() {
    return delegate.startScript();
  }

  @Override
  public SelectBuilder startSelect() {
    return delegate.startSelect();
  }

  @Override
  public SourceBuilder startSource() {
    return delegate.startSource();
  }

  @Override
  public SpanBuilder startSpan() {
    return delegate.startSpan();
  }

  @Override
  public StyleBuilder startStyle() {
    return delegate.startStyle();
  }

  @Override
  public ButtonBuilder startSubmitButton() {
    return delegate.startSubmitButton();
  }

  @Override
  public InputBuilder startSubmitInput() {
    return delegate.startSubmitInput();
  }

  @Override
  public TableBuilder startTable() {
    return delegate.startTable();
  }

  @Override
  public TableCaptionBuilder startTableCaption() {
    return delegate.startTableCaption();
  }

  @Override
  public TableSectionBuilder startTBody() {
    return delegate.startTBody();
  }

  @Override
  public TableCellBuilder startTD() {
    return delegate.startTD();
  }

  @Override
  public TextAreaBuilder startTextArea() {
    return delegate.startTextArea();
  }

  @Override
  public InputBuilder startTextInput() {
    return delegate.startTextInput();
  }

  @Override
  public TableSectionBuilder startTFoot() {
    return delegate.startTFoot();
  }

  @Override
  public TableCellBuilder startTH() {
    return delegate.startTH();
  }

  @Override
  public TableSectionBuilder startTHead() {
    return delegate.startTHead();
  }

  /**
   * Append a title element.
   * 
   * @return the builder for the new element
   */
  public TitleBuilder startTitle() {
    return delegate.startTitle();
  }

  @Override
  public TableRowBuilder startTR() {
    return delegate.startTR();
  }

  @Override
  public UListBuilder startUList() {
    return delegate.startUList();
  }

  @Override
  public VideoBuilder startVideo() {
    return delegate.startVideo();
  }

  @Override
  public R tabIndex(int tabIndex) {
    return attribute("tabIndex", tabIndex);
  }

  @Override
  public R title(String title) {
    return attribute("title", title);
  }

  @Override
  public ElementBuilder trustedStart(String tagName) {
    return delegate.trustedStart(tagName);
  }
}
