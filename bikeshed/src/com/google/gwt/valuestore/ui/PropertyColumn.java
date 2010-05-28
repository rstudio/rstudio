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
package com.google.gwt.valuestore.ui;

import com.google.gwt.app.util.PassthroughRenderer;
import com.google.gwt.app.util.Renderer;
import com.google.gwt.bikeshed.list.client.TextColumn;
import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Record;

/**
 * A column that displays a record property as a string.
 * 
 * @param <R> the type of record in this table
 * @param <T> value type of the property
 */
public class PropertyColumn<R extends Record, T> extends TextColumn<R> {
  public static <R extends Record> PropertyColumn<R, String> getStringPropertyColumn(
      Property<String> property) {
    return new PropertyColumn<R, String>(property,
        PassthroughRenderer.instance());
  }
  private final Renderer<T> renderer;

  private final Property<T> property;

  public PropertyColumn(Property<T> property, Renderer<T> renderer) {
    this.property = property;
    this.renderer = renderer;
  }

  public Property<T> getProperty() {
    return property;
  }

  @Override
  public String getValue(R object) {
    return renderer.render(object.get(property));
  }
}
