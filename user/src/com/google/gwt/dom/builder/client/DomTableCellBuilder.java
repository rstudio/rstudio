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

import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.client.TableCellElement;

/**
 * DOM-based implementation of {@link TableCellBuilder}.
 */
public class DomTableCellBuilder extends DomElementBuilderBase<TableCellBuilder, TableCellElement>
    implements TableCellBuilder {

  DomTableCellBuilder(DomBuilderImpl delegate) {
    super(delegate);
  }

  @Override
  public TableCellBuilder align(String align) {
    assertCanAddAttribute().setAlign(align);
    return this;
  }

  @Override
  public TableCellBuilder ch(String ch) {
    assertCanAddAttribute().setCh(ch);
    return this;
  }

  @Override
  public TableCellBuilder chOff(String chOff) {
    assertCanAddAttribute().setChOff(chOff);
    return this;
  }

  @Override
  public TableCellBuilder colSpan(int colSpan) {
    assertCanAddAttribute().setColSpan(colSpan);
    return this;
  }

  @Override
  public TableCellBuilder headers(String headers) {
    assertCanAddAttribute().setHeaders(headers);
    return this;
  }

  @Override
  public TableCellBuilder rowSpan(int rowSpan) {
    assertCanAddAttribute().setRowSpan(rowSpan);
    return this;
  }

  @Override
  public TableCellBuilder vAlign(String vAlign) {
    assertCanAddAttribute().setVAlign(vAlign);
    return this;
  }
}
