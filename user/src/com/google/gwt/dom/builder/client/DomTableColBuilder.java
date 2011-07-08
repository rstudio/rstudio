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

import com.google.gwt.dom.builder.shared.TableColBuilder;
import com.google.gwt.dom.client.TableColElement;

/**
 * DOM-based implementation of {@link TableColBuilder}.
 */
public class DomTableColBuilder extends DomElementBuilderBase<TableColBuilder, TableColElement>
    implements TableColBuilder {

  DomTableColBuilder(DomBuilderImpl delegate) {
    super(delegate, true);
  }

  @Override
  public TableColBuilder align(String align) {
    assertCanAddAttribute().setAlign(align);
    return this;
  }

  @Override
  public TableColBuilder ch(String ch) {
    assertCanAddAttribute().setCh(ch);
    return this;
  }

  @Override
  public TableColBuilder chOff(String chOff) {
    assertCanAddAttribute().setChOff(chOff);
    return this;
  }

  @Override
  public TableColBuilder span(int span) {
    assertCanAddAttribute().setSpan(span);
    return this;
  }

  @Override
  public TableColBuilder vAlign(String vAlign) {
    assertCanAddAttribute().setVAlign(vAlign);
    return this;
  }

  @Override
  public TableColBuilder width(String width) {
    assertCanAddAttribute().setWidth(width);
    return this;
  }
}
