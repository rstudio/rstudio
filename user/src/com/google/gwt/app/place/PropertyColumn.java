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

import com.google.gwt.requestfactory.shared.Property;
import com.google.gwt.requestfactory.shared.Record;
import com.google.gwt.text.shared.PassthroughRenderer;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.user.cellview.client.TextColumn;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * A column that displays a record property as a string. NB: Property objects
 * will soon go away, and this column class will hopefully replaced by a (much
 * simpler to use) code generated system.
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
  private final String[] paths;

  public PropertyColumn(Property<T> property, ProxyRenderer<T> renderer) {
    this.property = property;
    this.renderer = renderer;
    this.paths = pathinate(property, renderer);
  }

  public PropertyColumn(Property<T> property, Renderer<T> renderer) {
    this.property = property;
    this.renderer = renderer;
    this.paths = new String[] {property.getName()};
  }

  public String getDisplayName() {
    return property.getDisplayName();
  }

  public String[] getPaths() {
    return paths;
  }

  @Override
  public String getValue(R object) {
    return renderer.render(object.get(property));
  }

  private String[] pathinate(Property<T> property, ProxyRenderer<T> renderer) {
    String[] rtn = new String[renderer.getPaths().length];
    int i = 0;
    for (String rendererPath : renderer.getPaths()) {
      rtn[i++] = property.getName() + "." + rendererPath;
    }

    return rtn;
  }
}
