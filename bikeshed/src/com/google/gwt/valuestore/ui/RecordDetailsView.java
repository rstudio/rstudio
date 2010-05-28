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

import com.google.gwt.app.util.IsWidget;
import com.google.gwt.user.client.ui.TakesValue;

/**
 * Implemented by views that show the details of an object.
 *
 * @param <R> the type of object to show
 */
public interface RecordDetailsView<R> extends TakesValue<R>, IsWidget {

  /**
   * Implemented by the owner of the view.
   */
  interface Delegate {
    void editClicked();
  }
  
  void setDelegate(Delegate delegate);
}