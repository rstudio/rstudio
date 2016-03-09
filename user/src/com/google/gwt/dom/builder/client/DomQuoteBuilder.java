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

import com.google.gwt.dom.builder.shared.QuoteBuilder;
import com.google.gwt.dom.client.QuoteElement;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.annotations.IsSafeUri;

/**
 * DOM-based implementation of {@link QuoteBuilder}.
 */
public class DomQuoteBuilder extends DomElementBuilderBase<QuoteBuilder, QuoteElement> implements
    QuoteBuilder {

  DomQuoteBuilder(DomBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public QuoteBuilder cite(SafeUri cite) {
    assertCanAddAttribute().setCite(cite);
    return this;
  }

  @Override
  public QuoteBuilder cite(@IsSafeUri String cite) {
    assertCanAddAttribute().setCite(cite);
    return this;
  }
}
