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

import com.google.gwt.dom.builder.shared.HtmlParagraphBuilderTest;

/**
 * GWT tests for {@link DomParagraphBuilder}.
 */
public class GwtParagraphBuilderTest extends HtmlParagraphBuilderTest {

  @Override
  public String getModuleName() {
    return GWT_MODULE_NAME;
  }

  /**
   * Test that HTML can be set after ending one element and starting another.
   */
  @Override
  public void testHtmlAfterRestart() {
    /*
     * This test triggers an obscure bug in IE7 where you cannot set the
     * innerHTML of a child element within a paragraph if the innerHTML contains
     * a block element.
     * 
     * Disabling the method would prevent users from setting innerHTML that does
     * not contain block elements, which is allowed, and detecting when this
     * will throw an error would be difficult.
     */
  }
}
