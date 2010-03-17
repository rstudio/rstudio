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
package com.google.gwt.sample.expenses.client;

import com.google.gwt.requestfactory.shared.EntityRef;
import com.google.gwt.user.client.ui.HasValueList;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Values;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for showing a list of entities.
 * 
 * @param <E> the type of entity listed
 */
public class EntityList<E extends EntityRef<E>> implements
    HasValueList<Values<E>> {
  protected final EntityListView view;
  protected final List<Property<E, ?>> properties;

  public EntityList(String heading, EntityListView view, List<Property<E, ?>> properties) {
    this.view = view;
    view.setHeading(heading);
    this.properties = properties;

    List<String> names = new ArrayList<String>();
    for (Property<E, ?> property : properties) {
      names.add(property.getName());
    }
    this.view.setColumnNames(names);
  }

  public void editValueList(boolean replace, int index,
      List<Values<E>> newValues) {
    throw new UnsupportedOperationException();
  }

  public void setValueList(List<Values<E>> newValues) {
    List<List<String>> strings = new ArrayList<List<String>>();

    for (Values<E> values : newValues) {
      List<String> row = new ArrayList<String>();
      for (Property<E, ?> property : properties) {
        row.add(values.get(property).toString());
      }
      strings.add(row);
    }

    view.setValues(strings);
  }

  public void setValueListSize(int size, boolean exact) {
    throw new UnsupportedOperationException();
  }
}
