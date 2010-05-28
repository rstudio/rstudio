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

import com.google.gwt.valuestore.shared.Property;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.valuestore.ui.RecordEditView;

import java.util.Map;
import java.util.Set;

/**
 * The DataBinder base class for all the editor functionality.
 * 
 * @param <R> the Record type
 * @param <V> the View type
 */
public interface EditorSupport<R extends Record, V extends RecordEditView<R>> {
  Set<Property<?>> getProperties();
  
  void init(final V view);

  boolean isChanged(V view);

  void setEnabled(V view, boolean enabled);
  
  void setValue(V view, R value);

  void showErrors(V view, Map<String, String> errorMap);
}
