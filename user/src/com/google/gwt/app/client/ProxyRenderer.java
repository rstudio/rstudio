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
package com.google.gwt.app.client;

import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.valuestore.shared.Record;

/**
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * <p>
 * Renderer of Record values
 * @param <T> a record type
 */
public class ProxyRenderer<T extends Record> extends AbstractRenderer<T> {
  private static ProxyRenderer INSTANCE;

  /**
   * @return the instance
   */
  public static <T extends Record> Renderer<T> instance() {
    if (INSTANCE == null) {
      INSTANCE = new ProxyRenderer<T>();
    }
    return INSTANCE;
  }

  protected ProxyRenderer() {
  }

  public String render(T object) {
    return toString(object == null ? null : object.getId());
  }
}
