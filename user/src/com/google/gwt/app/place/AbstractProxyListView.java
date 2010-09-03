/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.app.place;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.requestfactory.shared.EntityProxy;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.HasData;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Abstract implementation of ProxyListView. Subclasses must call {@link #init}
 * with the root widget, its {@link CellTable}, and a list of
 * {@link PropertyColumn}.
 * 
 * @param <P> the type of the proxy
 */
public abstract class AbstractProxyListView<P extends EntityProxy> extends
    Composite implements ProxyListView<P> {

  private CellTable<P> table;
  private Set<String> paths = new HashSet<String>();
  private Delegate<P> delegate;

  public HasData<P> asHasData() {
    return table;
  }

  public AbstractProxyListView<P> asWidget() {
    return this;
  }

  public String[] getPaths() {
    return paths.toArray(new String[paths.size()]);
  }

  public void setDelegate(final Delegate<P> delegate) {
    this.delegate = delegate;
  }

  protected void init(Widget root, CellTable<P> table, Button newButton,
      List<PropertyColumn<P, ?>> columns) {
    super.initWidget(root);
    this.table = table;

    for (PropertyColumn<P, ?> column : columns) {
      table.addColumn(column, column.getDisplayName());
      paths.addAll(Arrays.asList(column.getPaths()));
    }

    newButton.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        delegate.createClicked();
      }
    });
  }

  @Override
  protected void initWidget(Widget widget) {
    throw new UnsupportedOperationException(
        "AbstractRecordListView must be initialized via "
            + "init(Widget CellTable<R> List<PropertyColumn<R, ?>> ) ");
  }
}
