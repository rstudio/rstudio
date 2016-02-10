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

import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.annotations.IsSafeUri;

/**
 * HTML-based implementation of {@link FormBuilder}.
 */
public class HtmlFormBuilder extends HtmlElementBuilderBase<FormBuilder> implements FormBuilder {

  HtmlFormBuilder(HtmlBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public FormBuilder acceptCharset(String acceptCharset) {
    return trustedAttribute("acceptCharset", acceptCharset);
  }

  @Override
  public FormBuilder action(SafeUri action) {
    return action(action.asString());
  }

  @Override
  public FormBuilder action(@IsSafeUri String action) {
    return trustedAttribute("action", action);
  }

  @Override
  public FormBuilder enctype(String enctype) {
    return trustedAttribute("enctype", enctype);
  }

  @Override
  public FormBuilder method(String method) {
    return trustedAttribute("method", method);
  }

  @Override
  public FormBuilder name(String name) {
    return trustedAttribute("name", name);
  }

  @Override
  public FormBuilder target(String target) {
    return trustedAttribute("target", target);
  }
}
