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

import com.google.gwt.dom.builder.shared.ElementBuilder;
import com.google.gwt.dom.builder.shared.ElementBuilderFactory;
import com.google.gwt.dom.builder.shared.InputBuilder;
import com.google.gwt.dom.builder.shared.TableColBuilder;

/**
 * Factory for creating element builders that construct elements using DOM
 * manipulation.
 */
public class DomBuilderFactory extends ElementBuilderFactory {

  private static DomBuilderFactory instance;

  /**
   * Get the instance of the {@link DomBuilderFactory}.
   * 
   * <p>
   * Use {@link ElementBuilderFactory#get()} to fetch a factory optimized for
   * the browser client. However, you can use this factory directly if you want
   * to force the builders to build elements use DOM manipulation.
   * </p>
   * 
   * @return the {@link ElementBuilderFactory}
   */
  public static DomBuilderFactory get() {
    if (instance == null) {
      instance = new DomBuilderFactory();
    }
    return instance;
  }

  /**
   * Created from static factory method.
   */
  public DomBuilderFactory() {
  }

  @Override
  public DomAnchorBuilder createAnchorBuilder() {
    return impl().startAnchor();
  }

  @Override
  public DomAreaBuilder createAreaBuilder() {
    return impl().startArea();
  }

  @Override
  public DomAudioBuilder createAudioBuilder() {
    return impl().startAudio();
  }

  @Override
  public DomBaseBuilder createBaseBuilder() {
    return impl().startBase();
  }

  @Override
  public DomQuoteBuilder createBlockQuoteBuilder() {
    return impl().startBlockQuote();
  }

  @Override
  public DomBodyBuilder createBodyBuilder() {
    return impl().startBody();
  }

  @Override
  public DomBRBuilder createBRBuilder() {
    return impl().startBR();
  }

  @Override
  public InputBuilder createButtonInputBuilder() {
    return impl().startButtonInput();
  }

  @Override
  public DomCanvasBuilder createCanvasBuilder() {
    return impl().startCanvas();
  }

  @Override
  public InputBuilder createCheckboxInputBuilder() {
    return impl().startCheckboxInput();
  }

  @Override
  public DomTableColBuilder createColBuilder() {
    return impl().startCol();
  }

  @Override
  public TableColBuilder createColGroupBuilder() {
    return impl().startColGroup();
  }

  @Override
  public DomDivBuilder createDivBuilder() {
    return impl().startDiv();
  }

  @Override
  public DomDListBuilder createDListBuilder() {
    return impl().startDList();
  }

  @Override
  public DomFieldSetBuilder createFieldSetBuilder() {
    return impl().startFieldSet();
  }

  @Override
  public InputBuilder createFileInputBuilder() {
    return impl().startFileInput();
  }

  @Override
  public DomFormBuilder createFormBuilder() {
    return impl().startForm();
  }

  @Override
  public DomFrameBuilder createFrameBuilder() {
    return impl().startFrame();
  }

  @Override
  public DomFrameSetBuilder createFrameSetBuilder() {
    return impl().startFrameSet();
  }

  @Override
  public DomHeadingBuilder createH1Builder() {
    return impl().startH1();
  }

  @Override
  public DomHeadingBuilder createH2Builder() {
    return impl().startH2();
  }

  @Override
  public DomHeadingBuilder createH3Builder() {
    return impl().startH3();
  }

  @Override
  public DomHeadingBuilder createH4Builder() {
    return impl().startH4();
  }

  @Override
  public DomHeadingBuilder createH5Builder() {
    return impl().startH5();
  }

  @Override
  public DomHeadingBuilder createH6Builder() {
    return impl().startH6();
  }

  @Override
  public DomHeadBuilder createHeadBuilder() {
    return impl().startHead();
  }

  @Override
  public InputBuilder createHiddenInputBuilder() {
    return impl().startHiddenInput();
  }

  @Override
  public DomHRBuilder createHRBuilder() {
    return impl().startHR();
  }

  @Override
  public DomIFrameBuilder createIFrameBuilder() {
    return impl().startIFrame();
  }

  @Override
  public DomImageBuilder createImageBuilder() {
    return impl().startImage();
  }

  @Override
  public InputBuilder createImageInputBuilder() {
    return impl().startImageInput();
  }

  @Override
  public DomLabelBuilder createLabelBuilder() {
    return impl().startLabel();
  }

  @Override
  public DomLegendBuilder createLegendBuilder() {
    return impl().startLegend();
  }

  @Override
  public DomLIBuilder createLIBuilder() {
    return impl().startLI();
  }

  @Override
  public DomLinkBuilder createLinkBuilder() {
    return impl().startLink();
  }

  @Override
  public DomMapBuilder createMapBuilder() {
    return impl().startMap();
  }

  @Override
  public DomMetaBuilder createMetaBuilder() {
    return impl().startMeta();
  }

  @Override
  public DomOListBuilder createOListBuilder() {
    return impl().startOList();
  }

  @Override
  public DomOptGroupBuilder createOptGroupBuilder() {
    return impl().startOptGroup();
  }

  @Override
  public DomOptionBuilder createOptionBuilder() {
    return impl().startOption();
  }

  @Override
  public DomParagraphBuilder createParagraphBuilder() {
    return impl().startParagraph();
  }

  @Override
  public DomParamBuilder createParamBuilder() {
    return impl().startParam();
  }

  @Override
  public InputBuilder createPasswordInputBuilder() {
    return impl().startPasswordInput();
  }

  @Override
  public DomPreBuilder createPreBuilder() {
    return impl().startPre();
  }

  @Override
  public DomButtonBuilder createPushButtonBuilder() {
    return impl().startPushButton();
  }

  @Override
  public DomQuoteBuilder createQuoteBuilder() {
    return impl().startQuote();
  }

  @Override
  public InputBuilder createRadioInputBuilder(String name) {
    return impl().startRadioInput(name);
  }

  @Override
  public DomButtonBuilder createResetButtonBuilder() {
    return impl().startResetButton();
  }

  @Override
  public InputBuilder createResetInputBuilder() {
    return impl().startResetInput();
  }

  @Override
  public DomScriptBuilder createScriptBuilder() {
    return impl().startScript();
  }

  @Override
  public DomSelectBuilder createSelectBuilder() {
    return impl().startSelect();
  }

  @Override
  public DomSourceBuilder createSourceBuilder() {
    return impl().startSource();
  }

  @Override
  public DomSpanBuilder createSpanBuilder() {
    return impl().startSpan();
  }

  @Override
  public DomStyleBuilder createStyleBuilder() {
    return impl().startStyle();
  }

  @Override
  public DomButtonBuilder createSubmitButtonBuilder() {
    return impl().startSubmitButton();
  }

  @Override
  public InputBuilder createSubmitInputBuilder() {
    return impl().startSubmitInput();
  }

  @Override
  public DomTableBuilder createTableBuilder() {
    return impl().startTable();
  }

  @Override
  public DomTableCaptionBuilder createTableCaptionBuilder() {
    return impl().startTableCaption();
  }

  @Override
  public DomTableSectionBuilder createTBodyBuilder() {
    return impl().startTBody();
  }

  @Override
  public DomTableCellBuilder createTDBuilder() {
    return impl().startTD();
  }

  @Override
  public DomTextAreaBuilder createTextAreaBuilder() {
    return impl().startTextArea();
  }

  @Override
  public InputBuilder createTextInputBuilder() {
    return impl().startTextInput();
  }

  @Override
  public DomTableSectionBuilder createTFootBuilder() {
    return impl().startTFoot();
  }

  @Override
  public DomTableCellBuilder createTHBuilder() {
    return impl().startTH();
  }

  @Override
  public DomTableSectionBuilder createTHeadBuilder() {
    return impl().startTHead();
  }

  @Override
  public DomTableRowBuilder createTRBuilder() {
    return impl().startTR();
  }

  @Override
  public DomUListBuilder createUListBuilder() {
    return impl().startUList();
  }

  @Override
  public DomVideoBuilder createVideoBuilder() {
    return impl().startVideo();
  }

  @Override
  public ElementBuilder trustedCreate(String tagName) {
    return impl().trustedStart(tagName);
  }

  private DomBuilderImpl impl() {
    return new DomBuilderImpl();
  }
}
