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

import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * HTML-based implementation of {@link ScriptBuilder}.
 */
public class HtmlScriptBuilder extends HtmlElementBuilderBase<ScriptBuilder> implements
    ScriptBuilder {

  HtmlScriptBuilder(HtmlBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public ScriptBuilder defer(String defer) {
    return trustedAttribute("defer", defer);
  }

  @Override
  public ScriptBuilder html(SafeHtml html) {
    throw new UnsupportedOperationException(UNSUPPORTED_HTML);
  }

  @Override
  public boolean isChildElementSupported() {
    return false;
  }

  @Override
  public ScriptBuilder src(String src) {
    return trustedAttribute("src", src);
  }

  @Override
  public ScriptBuilder type(String type) {
    return trustedAttribute("type", type);
  }
}
