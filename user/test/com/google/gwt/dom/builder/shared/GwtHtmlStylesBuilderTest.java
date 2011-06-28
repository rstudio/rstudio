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
 * Gwt tests for {@link HtmlStylesBuilder}.
 */
public class GwtHtmlStylesBuilderTest extends GwtStylesBuilderTestBase {

  public void testToHyphenatedForm() {
    assertEquals("simple", HtmlStylesBuilder.toHyphenatedForm("simple"));
    assertEquals("camel-case", HtmlStylesBuilder.toHyphenatedForm("camelCase"));
    assertEquals("camel-case-multiple-humps", HtmlStylesBuilder
        .toHyphenatedForm("camelCaseMultipleHumps"));
    assertEquals("already-hyphenated", HtmlStylesBuilder.toHyphenatedForm("already-hyphenated"));
    assertEquals("already-hyphenated-twice", HtmlStylesBuilder
        .toHyphenatedForm("already-hyphenated-twice"));
  }

  @Override
  protected ElementBuilderFactory getElementBuilderFactory() {
    return HtmlBuilderFactory.get();
  }
}
