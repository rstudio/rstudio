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

/**
 * Factory for creating element builders.
 * 
 * <p>
 * Use {@link ElementBuilderFactory#get()} to fetch the builder factory
 * optimized for the browser platform.
 * </p>
 * 
 * <p>
 * If you are using the builder on a server, use
 * {@link HtmlBuilderFactory#get()} instead. {@link HtmlBuilderFactory} can
 * construct a {@link com.google.gwt.safehtml.shared.SafeHtml} string and will
 * work on the server. Other implementations may only work on a browser client.
 * </p>
 * 
 * <p>
 * Element builder methods can be chained together as with a traditional
 * builder:
 * </p>
 * 
 * <pre>
 * DivBuilder divBuilder = ElementBuilderFactory.get().createDivBuilder();
 * divBuilder.id("myId").text("Hello World!").endDiv();
 * </pre>
 * 
 * <p>
 * See this example: {@example
 * com.google.gwt.examples.dom.builder.ElementBuilderFactoryChainingExample}.
 * </p>
 * 
 * <p>
 * Alternatively, builders can be used as separate objects and operated on
 * individually. This may be the preferred method if you are creating a complex
 * or dynamic element. The code below produces the same output as the code
 * above.
 * </p>
 * 
 * <pre>
 * DivBuilder divBuilder = ElementBuilderFactory.get().createDivBuilder();
 * divBuilder.id("myId");
 * divBuilder.text("Hello World!");
 * divBuilder.endDiv();
 * </pre>
 * 
 * <p>
 * See an example: {@example
 * com.google.gwt.examples.dom.builder.ElementBuilderFactoryNonChainingExample}.
 * </p>
 * 
 * <p>
 * You can also mix chaining and non-chaining methods when appropriate. For
 * example, you can add attributes to an element by chaining methods, but use a
 * separate builder object for each separate element.
 * </p>
 * 
 * <p>
 * NOTE: Builders always operate on the current element. For example, in the
 * code below, we create two divBuilders, one a child of the other. However,
 * they are actually the same builder instance! Implementations of
 * ElementBuilderFactory use a single instance of each builder type to improve
 * performance. The implication is that all element builders operate on the
 * current element, so the call to <code>divBuilder0.id("div1")</code> will set
 * the "id" of the child div, and is functionally equivalent to
 * <code>divBuilder1.id("div1")</code>. Its important to always call end()
 * before resuming work on the previous element builder.
 * </p>
 * 
 * <pre>
 * DivBuilder divBuilder0 = ElementBuilderFactory.get().createDivBuilder();
 * DivBuilder divBuilder1 = divBuilder0.startDiv();
 * divBuilder0.id("div1"); // Operates on the first element!
 * </pre>
 */
public abstract class ElementBuilderFactory {

  private static ElementBuilderFactory instance;

  /**
   * Get the instance of the {@link ElementBuilderFactory}.
   * 
   * @return the {@link ElementBuilderFactory}
   */
  public static ElementBuilderFactory get() {
    if (instance == null) {
      if (GWT.isClient()) {
        instance = GWT.create(ElementBuilderFactory.class);
      } else {
        // The DOM implementation will not work on the server.
        instance = HtmlBuilderFactory.get();
      }
    }
    return instance;
  }

  /**
   * Created from static factory method.
   */
  protected ElementBuilderFactory() {
  }

  public abstract AnchorBuilder createAnchorBuilder();

  public abstract AreaBuilder createAreaBuilder();

  public abstract AudioBuilder createAudioBuilder();

  public abstract BaseBuilder createBaseBuilder();

  public abstract QuoteBuilder createBlockQuoteBuilder();

  public abstract BodyBuilder createBodyBuilder();

  public abstract BRBuilder createBRBuilder();

  public abstract InputBuilder createButtonInputBuilder();

  public abstract CanvasBuilder createCanvasBuilder();

  public abstract InputBuilder createCheckboxInputBuilder();

  public abstract TableColBuilder createColBuilder();

  public abstract TableColBuilder createColGroupBuilder();

  public abstract DivBuilder createDivBuilder();

  public abstract DListBuilder createDListBuilder();

  public abstract FieldSetBuilder createFieldSetBuilder();

  public abstract InputBuilder createFileInputBuilder();

  public abstract FormBuilder createFormBuilder();

  public abstract FrameBuilder createFrameBuilder();

  public abstract FrameSetBuilder createFrameSetBuilder();

  public abstract HeadingBuilder createH1Builder();

  public abstract HeadingBuilder createH2Builder();

  public abstract HeadingBuilder createH3Builder();

  public abstract HeadingBuilder createH4Builder();

  public abstract HeadingBuilder createH5Builder();

  public abstract HeadingBuilder createH6Builder();

  public abstract HeadBuilder createHeadBuilder();

  public abstract InputBuilder createHiddenInputBuilder();

  public abstract HRBuilder createHRBuilder();

  public abstract IFrameBuilder createIFrameBuilder();

  public abstract ImageBuilder createImageBuilder();

  public abstract InputBuilder createImageInputBuilder();

  public abstract LabelBuilder createLabelBuilder();

  public abstract LegendBuilder createLegendBuilder();

  public abstract LIBuilder createLIBuilder();

  public abstract LinkBuilder createLinkBuilder();

  public abstract MapBuilder createMapBuilder();

  public abstract MetaBuilder createMetaBuilder();

  public abstract OListBuilder createOListBuilder();

  public abstract OptGroupBuilder createOptGroupBuilder();

  public abstract OptionBuilder createOptionBuilder();

  public abstract ParagraphBuilder createParagraphBuilder();

  public abstract ParamBuilder createParamBuilder();

  public abstract InputBuilder createPasswordInputBuilder();

  public abstract PreBuilder createPreBuilder();

  public abstract ButtonBuilder createPushButtonBuilder();

  public abstract QuoteBuilder createQuoteBuilder();

  /**
   * Create a builder for an &lt;input type='radio'&gt; element.
   * 
   * @param name name the name of the radio input (used for grouping)
   * @return the builder for the new element
   */
  public abstract InputBuilder createRadioInputBuilder(String name);

  public abstract ButtonBuilder createResetButtonBuilder();

  public abstract InputBuilder createResetInputBuilder();

  public abstract ScriptBuilder createScriptBuilder();

  public abstract SelectBuilder createSelectBuilder();

  public abstract SourceBuilder createSourceBuilder();

  public abstract SpanBuilder createSpanBuilder();

  public abstract StyleBuilder createStyleBuilder();

  public abstract ButtonBuilder createSubmitButtonBuilder();

  public abstract InputBuilder createSubmitInputBuilder();

  public abstract TableBuilder createTableBuilder();

  public abstract TableCaptionBuilder createTableCaptionBuilder();

  public abstract TableSectionBuilder createTBodyBuilder();

  public abstract TableCellBuilder createTDBuilder();

  public abstract TextAreaBuilder createTextAreaBuilder();

  public abstract InputBuilder createTextInputBuilder();

  public abstract TableSectionBuilder createTFootBuilder();

  public abstract TableCellBuilder createTHBuilder();

  public abstract TableSectionBuilder createTHeadBuilder();

  public abstract TableRowBuilder createTRBuilder();

  public abstract UListBuilder createUListBuilder();

  public abstract VideoBuilder createVideoBuilder();

  /**
   * Create an {@link ElementBuilder} for an arbitrary tag name. The tag name
   * will will not be checked or escaped. The calling code should be carefully
   * reviewed to ensure that the provided tag name will not cause a security
   * issue if including in an HTML document. In general, this means limiting the
   * code to HTML tagName constants supported by the HTML specification.
   * 
   * 
   * @param tagName the tag name of the new element
   * @return an {@link ElementBuilder} used to build the element
   */
  public abstract ElementBuilder trustedCreate(String tagName);
}
