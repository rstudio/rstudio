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
 * Tests for {@link HtmlTableSectionBuilder}.
 */
public class HtmlTableSectionBuilderTest extends ElementBuilderTestBase<TableSectionBuilder> {

  /**
   * Test that the start and end tags match for all table section elements.
   */
  public void testEndAll() {
    for (ElementBuilderFactory factory : getFactories()) {
      TableSectionBuilder tbody = factory.createTBodyBuilder();
      tbody.endTBody();

      TableSectionBuilder thead = factory.createTHeadBuilder();
      thead.endTHead();

      TableSectionBuilder tfoot = factory.createTFootBuilder();
      tfoot.endTFoot();
    }
  }

  @Override
  protected TableSectionBuilder createElementBuilder(ElementBuilderFactory factory) {
    return factory.createTBodyBuilder();
  }

  @Override
  protected void endElement(ElementBuilderBase<?> builder) {
    builder.endTBody();
  }

  @Override
  protected TableSectionBuilder startElement(ElementBuilderBase<?> builder) {
    return builder.startTBody();
  }
}
