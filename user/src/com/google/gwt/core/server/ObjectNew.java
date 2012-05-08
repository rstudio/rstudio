/*
 * Copyright 2012 Google Inc.
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
package com.google.gwt.core.server;

import com.google.gwt.core.server.ServerGwtBridge.ClassInstantiatorBase;
import com.google.gwt.core.server.ServerGwtBridge.Properties;

/**
 * A class instantiator that simple news the requested class.  Used as a last
 * resort.
 */
final class ObjectNew extends ClassInstantiatorBase {
  @SuppressWarnings("unchecked")
  @Override
  public <T> T create(Class<?> clazz, Properties properties) {
    return tryCreate((Class<T>) clazz);
  }
}