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
package com.google.gwt.requestfactory.shared;

import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.valuestore.shared.Record;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * Marker interface for the RequestFactory code generator.
 */
public interface RequestFactory {
  static final String JSON_CONTENT_TYPE_UTF8 = "application/json; charset=utf-8";

  String SYNC = "SYNC";

  // TODO: this must be configurable
  String URL = "gwtRequest";

  Record create(Class token);
  
  void init(HandlerManager eventBus);

  // The following methods match the format for the generated sub-interfaces
  // and implementations are generated using the same code we use to generate
  // those. In order to ensure this happens, each of the request selectors
  // needs to be manually added to the requestSelectors list in
  // RequestFactoryGenerator.java
  LoggingRequest loggingRequest();
}
