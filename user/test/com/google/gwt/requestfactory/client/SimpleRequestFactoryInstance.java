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
package com.google.gwt.requestfactory.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.requestfactory.client.impl.RecordImpl;
import com.google.gwt.requestfactory.client.impl.RecordSchema;
import com.google.gwt.requestfactory.shared.SimpleFooRecord;
import com.google.gwt.requestfactory.shared.SimpleRequestFactory;

/**
 * Easy access for RequestFactory tests.
 */
public class SimpleRequestFactoryInstance {
  private static SimpleRequestFactory factory;

  public static SimpleRequestFactory factory() {
    if (factory == null) {
      factory = GWT.create(SimpleRequestFactory.class);
    }

    return factory;
  }
  
  public static RecordSchema<?> schema() {
    return ((RecordImpl) factory().create(SimpleFooRecord.class)).getSchema();
  }
}
