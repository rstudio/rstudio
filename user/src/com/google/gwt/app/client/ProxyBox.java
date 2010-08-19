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

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.ValueBox;
import com.google.gwt.valuestore.shared.Record;

/**
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * <p>
 * A ValueBox that uses {@link com.google.gwt.app.client.ProxyParser} and
 * {@link com.google.gwt.app.client.ProxyRenderer}.
 *
 * @param <T> a proxy record
 */
public class ProxyBox<T extends Record> extends ValueBox<T> {

  public ProxyBox() {
    super(Document.get().createTextInputElement(), ProxyRenderer.<T>instance(),
        ProxyParser.<T>instance());
  }

  public T getValue() {
    // Until a sensible ProxyParser can be written
    return null;
  }
}
