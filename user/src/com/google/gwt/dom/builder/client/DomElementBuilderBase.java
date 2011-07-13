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
package com.google.gwt.dom.builder.client;

import com.google.gwt.dom.builder.shared.AbstractElementBuilderBase;
import com.google.gwt.dom.builder.shared.AnchorBuilder;
import com.google.gwt.dom.builder.shared.AreaBuilder;
import com.google.gwt.dom.builder.shared.AudioBuilder;
import com.google.gwt.dom.builder.shared.BRBuilder;
import com.google.gwt.dom.builder.shared.BaseBuilder;
import com.google.gwt.dom.builder.shared.BodyBuilder;
import com.google.gwt.dom.builder.shared.ButtonBuilder;
import com.google.gwt.dom.builder.shared.CanvasBuilder;
import com.google.gwt.dom.builder.shared.DListBuilder;
import com.google.gwt.dom.builder.shared.DivBuilder;
import com.google.gwt.dom.builder.shared.ElementBuilder;
import com.google.gwt.dom.builder.shared.ElementBuilderBase;
import com.google.gwt.dom.builder.shared.FieldSetBuilder;
import com.google.gwt.dom.builder.shared.FormBuilder;
import com.google.gwt.dom.builder.shared.FrameBuilder;
import com.google.gwt.dom.builder.shared.FrameSetBuilder;
import com.google.gwt.dom.builder.shared.HRBuilder;
import com.google.gwt.dom.builder.shared.HeadBuilder;
import com.google.gwt.dom.builder.shared.HeadingBuilder;
import com.google.gwt.dom.builder.shared.IFrameBuilder;
import com.google.gwt.dom.builder.shared.ImageBuilder;
import com.google.gwt.dom.builder.shared.InputBuilder;
import com.google.gwt.dom.builder.shared.LIBuilder;
import com.google.gwt.dom.builder.shared.LabelBuilder;
import com.google.gwt.dom.builder.shared.LegendBuilder;
import com.google.gwt.dom.builder.shared.LinkBuilder;
import com.google.gwt.dom.builder.shared.MapBuilder;
import com.google.gwt.dom.builder.shared.MetaBuilder;
import com.google.gwt.dom.builder.shared.OListBuilder;
import com.google.gwt.dom.builder.shared.OptGroupBuilder;
import com.google.gwt.dom.builder.shared.OptionBuilder;
import com.google.gwt.dom.builder.shared.ParagraphBuilder;
import com.google.gwt.dom.builder.shared.ParamBuilder;
import com.google.gwt.dom.builder.shared.PreBuilder;
import com.google.gwt.dom.builder.shared.QuoteBuilder;
import com.google.gwt.dom.builder.shared.ScriptBuilder;
import com.google.gwt.dom.builder.shared.SelectBuilder;
import com.google.gwt.dom.builder.shared.SourceBuilder;
import com.google.gwt.dom.builder.shared.SpanBuilder;
import com.google.gwt.dom.builder.shared.StyleBuilder;
import com.google.gwt.dom.builder.shared.TableBuilder;
import com.google.gwt.dom.builder.shared.TableCaptionBuilder;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableColBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.builder.shared.TableSectionBuilder;
import com.google.gwt.dom.builder.shared.TextAreaBuilder;
import com.google.gwt.dom.builder.shared.UListBuilder;
import com.google.gwt.dom.builder.shared.VideoBuilder;
import com.google.gwt.dom.client.Element;

/**
 * Implementation of {@link ElementBuilderBase} that delegates to a
 * {@link DomBuilderImpl}.
 * 
 * <p>
 * Subclasses of {@link DomElementBuilderBase} operate directly on the
 * {@link Element} being built.
 * </p>
 * 
 * @param <R> the builder type returned from build methods
 * @param <E> the {@link Element} type
 */
public class DomElementBuilderBase<R extends ElementBuilderBase<?>, E extends Element> extends
    AbstractElementBuilderBase<R> {

  private final DomBuilderImpl delegate;

  /**
   * Construct a new {@link DomElementBuilderBase}.
   * 
   * @param delegate the delegate that builds the element
   */
  DomElementBuilderBase(DomBuilderImpl delegate) {
    this(delegate, false);
  }

  /**
   * Construct a new {@link DomElementBuilderBase}.
   * 
   * @param delegate the delegate that builds the element
   * @param isEndTagForbidden true if the end tag is forbidden for this element
   */
  DomElementBuilderBase(DomBuilderImpl delegate, boolean isEndTagForbidden) {
    super(delegate, isEndTagForbidden);
    this.delegate = delegate;
  }

  @Override
  public R attribute(String name, int value) {
    assertCanAddAttribute().setAttribute(name, String.valueOf(value));
    return getReturnBuilder();
  }

  @Override
  public R attribute(String name, String value) {
    assertCanAddAttribute().setAttribute(name, value);
    return getReturnBuilder();
  }

  @Override
  public R className(String className) {
    assertCanAddAttribute().setClassName(className);
    return getReturnBuilder();
  }

  @Override
  public R dir(String dir) {
    assertCanAddAttribute().setDir(dir);
    return getReturnBuilder();
  }

  @Override
  public R draggable(String draggable) {
    assertCanAddAttribute().setDraggable(draggable);
    return getReturnBuilder();
  }

  @Override
  public R id(String id) {
    assertCanAddAttribute().setId(id);
    return getReturnBuilder();
  }

  @Override
  public R lang(String lang) {
    assertCanAddAttribute().setLang(lang);
    return getReturnBuilder();
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
  public InputBuilder startCheckboxInput() {
    return delegate.startCheckboxInput();
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
    assertCanAddAttribute().setTabIndex(tabIndex);
    return getReturnBuilder();
  }

  @Override
  public R title(String title) {
    assertCanAddAttribute().setTitle(title);
    return getReturnBuilder();
  }

  @Override
  public ElementBuilder trustedStart(String tagName) {
    return delegate.trustedStart(tagName);
  }

  /**
   * Assert that the builder is in a state where an attribute can be added.
   * 
   * @return the element on which the attribute can be set
   */
  protected E assertCanAddAttribute() {
    /*
     * An explicit parameterized return type on cast() is required by some javac
     * compilers.
     */
    return delegate.assertCanAddAttribute().<E> cast();
  }

  DomBuilderImpl getDelegate() {
    return delegate;
  }
}
