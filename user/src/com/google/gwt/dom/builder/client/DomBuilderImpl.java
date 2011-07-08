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

import com.google.gwt.dom.builder.shared.ElementBuilderBase;
import com.google.gwt.dom.builder.shared.ElementBuilderImpl;
import com.google.gwt.dom.builder.shared.InputBuilder;
import com.google.gwt.dom.builder.shared.StylesBuilder;
import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.QuoteElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.TableColElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.safehtml.shared.SafeHtml;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of methods in
 * {@link com.google.gwt.dom.builder.shared.ElementBuilderBase} used to render
 * Elements using DOM manipulation.
 */
class DomBuilderImpl extends ElementBuilderImpl {

  /*
   * Common element builders are created on initialization to avoid null checks.
   * Less common element builders are created lazily to avoid unnecessary object
   * creation.
   */
  private DomAnchorBuilder anchorBuilder;
  private DomAreaBuilder areaBuilder;
  private DomAudioBuilder audioBuilder;
  private DomBaseBuilder baseBuilder;
  private DomBodyBuilder bodyBuilder;
  private DomBRBuilder brBuilder;
  private DomButtonBuilder buttonBuilder;
  private DomCanvasBuilder canvasBuilder;
  private final DomDivBuilder divBuilder = new DomDivBuilder(this);
  private DomDListBuilder dListBuilder;
  private final DomElementBuilder elementBuilder = new DomElementBuilder(this);
  private DomFieldSetBuilder fieldSetBuilder;
  private DomFormBuilder formBuilder;
  private DomFrameBuilder frameBuilder;
  private DomFrameSetBuilder frameSetBuilder;
  private DomHeadBuilder headBuilder;
  private DomHeadingBuilder headingBuilder;
  private DomHRBuilder hrBuilder;
  private DomIFrameBuilder iFrameBuilder;
  private DomImageBuilder imageBuilder;
  private final DomInputBuilder inputBuilder = new DomInputBuilder(this);
  private DomLabelBuilder labelBuilder;
  private DomLegendBuilder legendBuilder;
  private final DomLIBuilder liBuilder = new DomLIBuilder(this);
  private DomLinkBuilder linkBuilder;
  private DomMapBuilder mapBuilder;
  private DomMetaBuilder metaBuilder;
  private DomOListBuilder oListBuilder;
  private final DomOptionBuilder optionBuilder = new DomOptionBuilder(this);
  private DomOptGroupBuilder optGroupBuilder;
  private DomParagraphBuilder paragraphBuilder;
  private DomParamBuilder paramBuilder;
  private DomPreBuilder preBuilder;
  private DomQuoteBuilder quoteBuilder;
  private DomScriptBuilder scriptBuilder;
  private DomSelectBuilder selectBuilder;
  private DomSourceBuilder sourceBuilder;
  private final DomSpanBuilder spanBuilder = new DomSpanBuilder(this);
  private final StylesBuilder stylesBuilder = new DomStylesBuilder(this);
  private DomStyleBuilder styleBuilder;
  private DomTableBuilder tableBuilder;
  private final DomTableCellBuilder tableCellBuilder = new DomTableCellBuilder(this);
  private DomTableCaptionBuilder tableCaptionBuilder;
  private DomTableColBuilder tableColBuilder;
  private final DomTableRowBuilder tableRowBuilder = new DomTableRowBuilder(this);
  private DomTableSectionBuilder tableSectionBuilder;
  private DomTextAreaBuilder textAreaBuilder;
  private DomUListBuilder uListBuilder;
  private DomVideoBuilder videoBuilder;

  private Element root;

  /**
   * The element at the top of the stack.
   * 
   * With normal usage, the current element will be accessed repeatedly to add
   * attributes and styles. We maintain the current element outside of the stack
   * to avoid a list access on each operation.
   */
  private Element currentElement;
  private final List<Element> stackElements = new ArrayList<Element>();

  @Override
  public ElementBuilderBase<?> end() {
    ElementBuilderBase<?> builder = super.end();
    popElement();
    return builder;
  }

  public DomAnchorBuilder startAnchor() {
    if (anchorBuilder == null) {
      anchorBuilder = new DomAnchorBuilder(this);
    }
    return start(Document.get().createAnchorElement(), anchorBuilder);
  }

  public DomAreaBuilder startArea() {
    if (areaBuilder == null) {
      areaBuilder = new DomAreaBuilder(this);
    }
    return start(Document.get().createAreaElement(), areaBuilder);
  }

  public DomAudioBuilder startAudio() {
    if (audioBuilder == null) {
      audioBuilder = new DomAudioBuilder(this);
    }
    return start(Document.get().createAudioElement(), audioBuilder);
  }

  public DomBaseBuilder startBase() {
    if (baseBuilder == null) {
      baseBuilder = new DomBaseBuilder(this);
    }
    return start(Document.get().createBaseElement(), baseBuilder);
  }

  public DomQuoteBuilder startBlockQuote() {
    return startQuote(Document.get().createBlockQuoteElement());
  }

  public DomBodyBuilder startBody() {
    if (bodyBuilder == null) {
      bodyBuilder = new DomBodyBuilder(this);
    }
    return start(Document.get().createElement("body"), bodyBuilder);
  }

  public DomBRBuilder startBR() {
    if (brBuilder == null) {
      brBuilder = new DomBRBuilder(this);
    }
    return start(Document.get().createBRElement(), brBuilder);
  }

  public InputBuilder startButtonInput() {
    return startInput(Document.get().createButtonInputElement());
  }

  public DomCanvasBuilder startCanvas() {
    if (canvasBuilder == null) {
      canvasBuilder = new DomCanvasBuilder(this);
    }
    return start(Document.get().createCanvasElement(), canvasBuilder);
  }

  public InputBuilder startCheckInput() {
    return startInput(Document.get().createCheckInputElement());
  }

  public DomTableColBuilder startCol() {
    return startTableCol(Document.get().createColElement());
  }

  public DomTableColBuilder startColGroup() {
    return startTableCol(Document.get().createColGroupElement());
  }

  public DomDivBuilder startDiv() {
    return start(Document.get().createDivElement(), divBuilder);
  }

  public DomDListBuilder startDList() {
    if (dListBuilder == null) {
      dListBuilder = new DomDListBuilder(this);
    }
    return start(Document.get().createDLElement(), dListBuilder);
  }

  public DomFieldSetBuilder startFieldSet() {
    if (fieldSetBuilder == null) {
      fieldSetBuilder = new DomFieldSetBuilder(this);
    }
    return start(Document.get().createFieldSetElement(), fieldSetBuilder);
  }

  public InputBuilder startFileInput() {
    return startInput(Document.get().createFileInputElement());
  }

  public DomFormBuilder startForm() {
    if (formBuilder == null) {
      formBuilder = new DomFormBuilder(this);
    }
    return start(Document.get().createFormElement(), formBuilder);
  }

  public DomFrameBuilder startFrame() {
    if (frameBuilder == null) {
      frameBuilder = new DomFrameBuilder(this);
    }
    return start(Document.get().createFrameElement(), frameBuilder);
  }

  public DomFrameSetBuilder startFrameSet() {
    if (frameSetBuilder == null) {
      frameSetBuilder = new DomFrameSetBuilder(this);
    }
    return start(Document.get().createFrameSetElement(), frameSetBuilder);
  }

  public DomHeadingBuilder startH1() {
    return startHeading(1);
  }

  public DomHeadingBuilder startH2() {
    return startHeading(2);
  }

  public DomHeadingBuilder startH3() {
    return startHeading(3);
  }

  public DomHeadingBuilder startH4() {
    return startHeading(4);
  }

  public DomHeadingBuilder startH5() {
    return startHeading(5);
  }

  public DomHeadingBuilder startH6() {
    return startHeading(6);
  }

  public DomHeadBuilder startHead() {
    if (headBuilder == null) {
      headBuilder = new DomHeadBuilder(this);
    }
    return start(Document.get().createHeadElement(), headBuilder);
  }

  public InputBuilder startHiddenInput() {
    return startInput(Document.get().createHiddenInputElement());
  }

  public DomHRBuilder startHR() {
    if (hrBuilder == null) {
      hrBuilder = new DomHRBuilder(this);
    }
    return start(Document.get().createHRElement(), hrBuilder);
  }

  public DomIFrameBuilder startIFrame() {
    if (iFrameBuilder == null) {
      iFrameBuilder = new DomIFrameBuilder(this);
    }
    return start(Document.get().createIFrameElement(), iFrameBuilder);
  }

  public DomImageBuilder startImage() {
    if (imageBuilder == null) {
      imageBuilder = new DomImageBuilder(this);
    }
    return start(Document.get().createImageElement(), imageBuilder);
  }

  public InputBuilder startImageInput() {
    return startInput(Document.get().createImageInputElement());
  }

  /**
   * Start an input using the specified InputElement.
   */
  public DomInputBuilder startInput(InputElement input) {
    return start(input, inputBuilder);
  }

  public DomLabelBuilder startLabel() {
    if (labelBuilder == null) {
      labelBuilder = new DomLabelBuilder(this);
    }
    return start(Document.get().createLabelElement(), labelBuilder);
  }

  public DomLegendBuilder startLegend() {
    if (legendBuilder == null) {
      legendBuilder = new DomLegendBuilder(this);
    }
    return start(Document.get().createLegendElement(), legendBuilder);
  }

  public DomLIBuilder startLI() {
    return start(Document.get().createLIElement(), liBuilder);
  }

  public DomLinkBuilder startLink() {
    if (linkBuilder == null) {
      linkBuilder = new DomLinkBuilder(this);
    }
    return start(Document.get().createLinkElement(), linkBuilder);
  }

  public DomMapBuilder startMap() {
    if (mapBuilder == null) {
      mapBuilder = new DomMapBuilder(this);
    }
    return start(Document.get().createMapElement(), mapBuilder);
  }

  public DomMetaBuilder startMeta() {
    if (metaBuilder == null) {
      metaBuilder = new DomMetaBuilder(this);
    }
    return start(Document.get().createMetaElement(), metaBuilder);
  }

  public DomOListBuilder startOList() {
    if (oListBuilder == null) {
      oListBuilder = new DomOListBuilder(this);
    }
    return start(Document.get().createOLElement(), oListBuilder);
  }

  public DomOptGroupBuilder startOptGroup() {
    if (optGroupBuilder == null) {
      optGroupBuilder = new DomOptGroupBuilder(this);
    }
    return start(Document.get().createOptGroupElement(), optGroupBuilder);
  }

  public DomOptionBuilder startOption() {
    return start(Document.get().createOptionElement(), optionBuilder);
  }

  public DomParagraphBuilder startParagraph() {
    if (paragraphBuilder == null) {
      paragraphBuilder = new DomParagraphBuilder(this);
    }
    return start(Document.get().createPElement(), paragraphBuilder);
  }

  public DomParamBuilder startParam() {
    if (paramBuilder == null) {
      paramBuilder = new DomParamBuilder(this);
    }
    return start(Document.get().createParamElement(), paramBuilder);
  }

  public InputBuilder startPasswordInput() {
    return startInput(Document.get().createPasswordInputElement());
  }

  public DomPreBuilder startPre() {
    if (preBuilder == null) {
      preBuilder = new DomPreBuilder(this);
    }
    return start(Document.get().createPreElement(), preBuilder);
  }

  public DomButtonBuilder startPushButton() {
    return startButton(Document.get().createPushButtonElement());
  }

  public DomQuoteBuilder startQuote() {
    return startQuote(Document.get().createQElement());
  }

  public InputBuilder startRadioInput(String name) {
    return startInput(Document.get().createRadioInputElement(name));
  }

  public DomButtonBuilder startResetButton() {
    return startButton(Document.get().createResetButtonElement());
  }

  public InputBuilder startResetInput() {
    return startInput(Document.get().createSubmitInputElement());
  }

  public DomScriptBuilder startScript() {
    if (scriptBuilder == null) {
      scriptBuilder = new DomScriptBuilder(this);
    }
    return start(Document.get().createScriptElement(), scriptBuilder);
  }

  public DomSelectBuilder startSelect() {
    if (selectBuilder == null) {
      selectBuilder = new DomSelectBuilder(this);
    }
    return start(Document.get().createSelectElement(), selectBuilder);
  }

  public DomSourceBuilder startSource() {
    if (sourceBuilder == null) {
      sourceBuilder = new DomSourceBuilder(this);
    }
    return start(Document.get().createSourceElement(), sourceBuilder);
  }

  public DomSpanBuilder startSpan() {
    return start(Document.get().createSpanElement(), spanBuilder);
  }

  public DomStyleBuilder startStyle() {
    if (styleBuilder == null) {
      styleBuilder = new DomStyleBuilder(this);
    }
    return start(Document.get().createStyleElement(), styleBuilder);
  }

  public DomButtonBuilder startSubmitButton() {
    return startButton(Document.get().createSubmitButtonElement());
  }

  public InputBuilder startSubmitInput() {
    return startInput(Document.get().createSubmitInputElement());
  }

  public DomTableBuilder startTable() {
    if (tableBuilder == null) {
      tableBuilder = new DomTableBuilder(this);
    }
    return start(Document.get().createTableElement(), tableBuilder);
  }

  public DomTableCaptionBuilder startTableCaption() {
    if (tableCaptionBuilder == null) {
      tableCaptionBuilder = new DomTableCaptionBuilder(this);
    }
    return start(Document.get().createCaptionElement(), tableCaptionBuilder);
  }

  public DomTableSectionBuilder startTBody() {
    return startTableSection(Document.get().createTBodyElement());
  }

  public DomTableCellBuilder startTD() {
    return start(Document.get().createTDElement(), tableCellBuilder);
  }

  public DomTextAreaBuilder startTextArea() {
    if (textAreaBuilder == null) {
      textAreaBuilder = new DomTextAreaBuilder(this);
    }
    return start(Document.get().createTextAreaElement(), textAreaBuilder);
  }

  public DomTableSectionBuilder startTFoot() {
    return startTableSection(Document.get().createTFootElement());
  }

  public DomTableCellBuilder startTH() {
    return start(Document.get().createTHElement(), tableCellBuilder);
  }

  public DomTableSectionBuilder startTHead() {
    return startTableSection(Document.get().createTHeadElement());
  }

  public DomTableRowBuilder startTR() {
    return start(Document.get().createTRElement(), tableRowBuilder);
  }

  public DomUListBuilder startUList() {
    if (uListBuilder == null) {
      uListBuilder = new DomUListBuilder(this);
    }
    return start(Document.get().createULElement(), uListBuilder);
  }

  public DomVideoBuilder startVideo() {
    if (videoBuilder == null) {
      videoBuilder = new DomVideoBuilder(this);
    }
    return start(Document.get().createVideoElement(), videoBuilder);
  }

  @Override
  public StylesBuilder style() {
    return stylesBuilder;
  }

  public DomElementBuilder trustedStart(String tagName) {
    /*
     * Validate the tag before trying to create the element, or the browser may
     * throw a JS error and prevent us from triggering an
     * IllegalArgumentException.
     */
    assertValidTagName(tagName);
    return start(Document.get().createElement(tagName), elementBuilder);
  }

  @Override
  protected void doCloseStartTagImpl() {
    // No-op.
  }

  @Override
  protected void doCloseStyleAttributeImpl() {
    // No-op.
  }

  @Override
  protected void doEndStartTagImpl() {
    // No-op.
  }

  @Override
  protected void doEndTagImpl(String tagName) {
    // No-op.
  }

  @Override
  protected Element doFinishImpl() {
    return root;
  }

  @Override
  protected void doHtmlImpl(SafeHtml html) {
    getCurrentElement().setInnerHTML(html.asString());
  }

  @Override
  protected void doOpenStyleImpl() {
    // No-op.
  }

  @Override
  protected void doTextImpl(String text) {
    getCurrentElement().setInnerText(text);
  }

  @Override
  protected void lockCurrentElement() {
    // Overridden for visibility.
    super.lockCurrentElement();
  }

  /**
   * Assert that the builder is in a state where an attribute can be added.
   * 
   * @return the element on which the attribute can be set
   * @throw {@link IllegalStateException} if the start tag is closed
   */
  Element assertCanAddAttribute() {
    assertCanAddAttributeImpl();
    return getCurrentElement();
  }

  /**
   * Assert that the builder is in a state where a style property can be added.
   * 
   * @return the {@link Style} on which the property can be set
   * @throw {@link IllegalStateException} if the style is not accessible
   */
  Style assertCanAddStyleProperty() {
    assertCanAddStylePropertyImpl();
    return getCurrentElement().getStyle();
  }

  /**
   * Get the element current being built.
   */
  Element getCurrentElement() {
    if (currentElement == null) {
      throw new IllegalStateException("There are no elements on the stack.");
    }
    return currentElement;
  }

  InputBuilder startTextInput() {
    return startInput(Document.get().createTextInputElement());
  }

  private void popElement() {
    Element toRet = getCurrentElement();
    int itemCount = stackElements.size(); // Greater than or equal to one.
    stackElements.remove(itemCount - 1);
    if (itemCount == 1) {
      currentElement = null;
    } else {
      currentElement = stackElements.get(itemCount - 2);
    }
  }

  private void pushElement(Element e) {
    stackElements.add(e);
    currentElement = e;
  }

  /**
   * Start a child element.
   * 
   * @param element the element to start
   * @param builder the builder used to builder the new element
   */
  private <B extends ElementBuilderBase<?>> B start(Element element, B builder) {
    onStart(element.getTagName(), builder);

    // Set the root element.
    if (root == null) {
      // This is the new root element.
      root = element;
    } else {
      // Appending to the current element.
      getCurrentElement().appendChild(element);
    }

    // Add the element to the stack.
    pushElement(element);

    return builder;
  }

  /**
   * Start a button using the specified {@link ButtonElement}.
   */
  private DomButtonBuilder startButton(ButtonElement button) {
    if (buttonBuilder == null) {
      buttonBuilder = new DomButtonBuilder(this);
    }
    return start(button, buttonBuilder);
  }

  /**
   * Start one of the many headers.
   */
  private DomHeadingBuilder startHeading(int level) {
    if (headingBuilder == null) {
      headingBuilder = new DomHeadingBuilder(this);
    }
    return start(Document.get().createHElement(level), headingBuilder);
  }

  /**
   * Start a quote or blockquote.
   */
  private DomQuoteBuilder startQuote(QuoteElement quote) {
    if (quoteBuilder == null) {
      quoteBuilder = new DomQuoteBuilder(this);
    }
    return start(quote, quoteBuilder);
  }

  /**
   * Start a table col or colgroup.
   */
  private DomTableColBuilder startTableCol(TableColElement element) {
    if (tableColBuilder == null) {
      tableColBuilder = new DomTableColBuilder(this);
    }
    return start(element, tableColBuilder);
  }

  /**
   * Start a table section using the specified {@link TableSectionElement}.
   */
  private DomTableSectionBuilder startTableSection(TableSectionElement section) {
    if (tableSectionBuilder == null) {
      tableSectionBuilder = new DomTableSectionBuilder(this);
    }
    return start(section, tableSectionBuilder);
  }
}
