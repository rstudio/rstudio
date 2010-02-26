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
package com.google.gwt.bikeshed.list.shared;

/**
 * A model for a list.
 * 
 * @param <T> the data type of records in the list
 */
public interface ListModel<T> {

  /**
   * Add a {@link ListHandler} to the model.
   * 
   * @param handler the {@link ListHandler}
   */
  ListRegistration addListHandler(ListHandler<T> handler);
}
