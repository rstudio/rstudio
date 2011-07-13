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
 * HTML-based implementation of {@link ParamBuilder}.
 */
public class HtmlParamBuilder extends HtmlElementBuilderBase<ParamBuilder> implements ParamBuilder {

  HtmlParamBuilder(HtmlBuilderImpl delegate) {
    super(delegate, true);
  }

  @Override
  public ParamBuilder name(String name) {
    return trustedAttribute("name", name);
  }

  @Override
  public ParamBuilder value(String value) {
    return trustedAttribute("value", value);
  }
}
