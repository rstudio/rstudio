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

import com.google.gwt.sample.expenses.client.place.Places;
import com.google.gwt.sample.expenses.shared.ExpensesEntityKey;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.HasValueList;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Values;

import java.util.ArrayList;
import java.util.List;

/**
 * Presenter that shows a list of entities and provides "edit" and "show"
 * commands for them.
 * 
 * @param <E> the type of entity listed
 */
public class EntityListPresenter<E extends ExpensesEntityKey<?>> implements
    HasValueList<Values<E>> {
  private final EntityListView view;
  private final List<Property<E, ?>> properties;
  private final Places places;

  public EntityListPresenter(String heading, EntityListView view,
      List<Property<E, ?>> properties, Places places) {
    this.view = view;
    view.setHeading(heading);
    this.properties = properties;
    this.places = places;

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
    List<EntityListView.Row> rows = new ArrayList<EntityListView.Row>();

    for (final Values<E> values : newValues) {
      final List<String> strings = new ArrayList<String>();
      for (Property<E, ?> property : properties) {
        strings.add(values.get(property).toString());
      }
      EntityListView.Row row = new EntityListView.Row() {

        public Command getEditCommand() {
          return places.getGoToEditFor(values);
        }

        public Command getShowDetailsCommand() {
          return places.getGoToDetailsFor(values);
        }

        public List<String> getValues() {
          return strings;
        }

      };
      rows.add(row);
    }

    view.setRowData(rows);
  }

  public void setValueListSize(int size, boolean exact) {
    throw new UnsupportedOperationException();
  }
}
