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

import com.google.gwt.requestfactory.shared.EntityProxy;
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
public abstract class PropertyColumn<R extends EntityProxy, T> extends
    TextColumn<R> {
  private String displayName;
  private final String[] paths;

  public PropertyColumn(String property, String displayName, Class<T> clazz,
      ProxyRenderer<T> renderer) {
    this.displayName = displayName;
    this.paths = pathinate(property, renderer);
  }

  public PropertyColumn(String property, String displayName, Class<T> clazz,
      Renderer<T> renderer) {
    this.displayName = displayName;
    this.paths = new String[] {property};
  }

  public String getDisplayName() {
    return displayName;
  }

  public String[] getPaths() {
    return paths;
  }

  private String[] pathinate(String property, ProxyRenderer<T> renderer) {
    String[] rtn = new String[renderer.getPaths().length];
    int i = 0;
    for (String rendererPath : renderer.getPaths()) {
      rtn[i++] = property + "." + rendererPath;
    }

    return rtn;
  }
}
