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
package com.google.web.bindery.requestfactory.gwt.ui.client;

import com.google.gwt.view.client.ProvidesKey;
import com.google.web.bindery.requestfactory.shared.EntityProxy;

/**
 * An {@link EntityProxy}-aware key provider, handy for use with
 * {@link com.google.gwt.view.client.SelectionModel} and various cell widgets.
 * 
 * @see com.google.gwt.user.cellview.client.CellBrowser
 * @see com.google.gwt.user.cellview.client.CellList
 * @see com.google.gwt.user.cellview.client.CellTable
 * 
 * @param <P> the proxy type
 */
public class EntityProxyKeyProvider<P extends EntityProxy> implements ProvidesKey<P> {
  /**
   * Returns the key Object for the given item.
   * 
   * @param item an item of type P
   */
  public Object getKey(P item) {
    return item == null ? null : item.stableId();
  }
}
