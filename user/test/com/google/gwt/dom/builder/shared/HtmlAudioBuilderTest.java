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
 * Tests for {@link HtmlAudioBuilder}.
 */
public class HtmlAudioBuilderTest extends ElementBuilderTestBase<AudioBuilder> {

  @Override
  protected AudioBuilder createElementBuilder(ElementBuilderFactory factory) {
    return factory.createAudioBuilder();
  }

  @Override
  protected void endElement(ElementBuilderBase<?> builder) {
    builder.endAudio();
  }

  @Override
  protected AudioBuilder startElement(ElementBuilderBase<?> builder) {
    return builder.startAudio();
  }
}
