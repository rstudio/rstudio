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
 * Builds an label element.
 */
public interface LabelBuilder extends ElementBuilderBase<LabelBuilder> {

  /**
   * A single character access key to give access to the form control.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-accesskey">W3C
   *      HTML Specification</a>
   */
  LabelBuilder accessKey(String accessKey);

  /**
   * This attribute links this label with another form control by id attribute.
   * 
   * @see <a
   *      href="http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-for">W3C
   *      HTML Specification</a>
   */
  LabelBuilder htmlFor(String htmlFor);
}
