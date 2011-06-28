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
package com.google.gwt.examples.dom.builder;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.builder.shared.DivBuilder;
import com.google.gwt.dom.builder.shared.ElementBuilderFactory;
import com.google.gwt.dom.builder.shared.OptionBuilder;
import com.google.gwt.dom.builder.shared.SelectBuilder;
import com.google.gwt.dom.builder.shared.StylesBuilder;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

/**
 * Example of using {@link ElementBuilderFactory} to construct an element using
 * each builder as an object.
 */
public class ElementBuilderFactoryNonChainingExample implements EntryPoint {

  @Override
  public void onModuleLoad() {
    /*
     * Create a builder for the outermost element. The initial state of the
     * builder is a started element ready for attributes (eg. "<div").
     */
    DivBuilder divBuilder = ElementBuilderFactory.get().createDivBuilder();

    // Add attributes to the div.
    divBuilder.id("myId");
    divBuilder.title("This is a div");

    // Add style properties to the div.
    StylesBuilder divStyle = divBuilder.style();
    divStyle.trustedBackgroundColor("red");
    divStyle.endStyle();

    // Append a child select element to the div.
    SelectBuilder selectBuilder = divBuilder.startSelect();

    // Append three options to the select element.
    for (int i = 0; i < 3; i++) {
      OptionBuilder optionBuilder = selectBuilder.startOption();
      optionBuilder.value("value" + i);
      optionBuilder.text("Option " + i);
      optionBuilder.endOption();
    }

    /*
     * End the select and div elements. Note that ending the remaining elements
     * before calling asElement() below is optional, but a good practice. If we
     * did not call endOption() above, we would append each option element to
     * the preceeding option element, which is not what we want.
     * 
     * In general, you must pay close attention to ensure that you close
     * elements correctly.
     */
    selectBuilder.endSelect();
    divBuilder.endDiv();

    // Get the element out of the builder.
    Element div = divBuilder.finish();

    // Attach the element to the page.
    Document.get().getBody().appendChild(div);
  }
}