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
package com.google.gwt.user.client.ui;

import java.util.Map;

/**
 * Implemented by widgets that let the user pick from a set
 * of values.
 * 
 * @param <T> the value type
 */
public interface HasValueMap<T> {
  /**
   * @param values A map of acceptable values and their display strings
   */
  void setValues(Map<? extends T, String> values);
}
