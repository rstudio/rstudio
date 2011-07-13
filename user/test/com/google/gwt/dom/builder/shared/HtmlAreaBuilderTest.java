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
 * Tests for {@link HtmlAreaBuilder}.
 */
public class HtmlAreaBuilderTest extends ElementBuilderTestBase<AreaBuilder> {

  @Override
  protected AreaBuilder createElementBuilder(ElementBuilderFactory factory) {
    return factory.createAreaBuilder();
  }

  @Override
  protected void endElement(ElementBuilderBase<?> builder) {
    builder.endArea();
  }

  @Override
  protected AreaBuilder startElement(ElementBuilderBase<?> builder) {
    return builder.startArea();
  }
}
