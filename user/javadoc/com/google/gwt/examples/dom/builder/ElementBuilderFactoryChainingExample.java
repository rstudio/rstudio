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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

/**
 * Example of using {@link ElementBuilderFactory} to construct an element by
 * chaining methods.
 */
public class ElementBuilderFactoryChainingExample implements EntryPoint {

  @Override
  public void onModuleLoad() {
    /*
     * Create a builder for the outermost element. The initial state of the
     * builder is a started element ready for attributes (eg. "<div").
     */
    DivBuilder divBuilder = ElementBuilderFactory.get().createDivBuilder();

    /*
     * Build the element.
     * 
     * First, we set the element's id to "myId", then set its title to
     * "This is a div". Next, we set the background-color style property to
     * "red". Finally, we set some inner text to "Hello World!". When we are
     * finished, we end the div.
     * 
     * When building elements, the order of methods matters. Attributes and
     * style properties must be added before setting inner html/text or
     * appending children. This is because the string implementation cannot
     * concatenate an attribute after child content has been added.
     * 
     * Note that endStyle() takes the builder type that we want to return, which
     * must be the "parent" builder. endDiv() does not need the optional
     * argument because we are finished building the element.
     */
    divBuilder.id("myId").title("This is a div");
    divBuilder.style().trustedBackgroundColor("red").endStyle();
    divBuilder.text("Hello World!").endDiv();

    // Get the element out of the builder.
    Element div = divBuilder.finish();

    // Attach the element to the page.
    Document.get().getBody().appendChild(div);
  }
}