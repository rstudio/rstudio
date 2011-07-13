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

/**
 * Tests for {@link HtmlHeadingBuilder}.
 */
public class HtmlHeadingBuilderTest extends ElementBuilderTestBase<HeadingBuilder> {

  /**
   * Test that the start and end tags match for all Heading elements.
   */
  public void testEndAll() {
    for (ElementBuilderFactory factory : getFactories()) {
      HeadingBuilder h1 = factory.createH1Builder();
      h1.endH1();

      HeadingBuilder h2 = factory.createH2Builder();
      h2.endH2();

      HeadingBuilder h3 = factory.createH3Builder();
      h3.endH3();

      HeadingBuilder h4 = factory.createH4Builder();
      h4.endH4();

      HeadingBuilder h5 = factory.createH5Builder();
      h5.endH5();

      HeadingBuilder h6 = factory.createH6Builder();
      h6.endH6();
    }
  }

  @Override
  protected HeadingBuilder createElementBuilder(ElementBuilderFactory factory) {
    return factory.createH1Builder();
  }

  @Override
  protected void endElement(ElementBuilderBase<?> builder) {
    builder.endH1();
  }

  @Override
  protected HeadingBuilder startElement(ElementBuilderBase<?> builder) {
    return builder.startH1();
  }
}
