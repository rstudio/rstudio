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

import com.google.gwt.app.place.ProxyEditView;
import com.google.gwt.requestfactory.shared.EntityProxy;

import java.util.Map;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Implemented by objects that provide data binding support for 
 * instances of {@link ProxyEditView}. 
 * 
 * @param <R> the Record type
 * @param <V> the View type
 */
public interface EditorSupport<R extends EntityProxy, V extends ProxyEditView<R>> {
  String[] getPaths();
  
  void init(final V view);

  boolean isChanged(V view);

  void setEnabled(V view, boolean enabled);
  
  void setValue(V view, R value);

  void showErrors(V view, Map<String, String> errorMap);
}
