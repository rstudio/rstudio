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

import com.google.gwt.valuestore.shared.ValueStore;
import com.google.gwt.valuestore.shared.Values;

import java.util.List;

/**
 * Marker interface for the RequestFactory code generator.
 */
public interface RequestFactory {

  /**
   * Implemented by the request objects created by this factory.
   */
  interface RequestObject {
    void fire();

    String getRequestData(String data);

    /**
     * @deprecated Here only until we can move everything into the post data
     */
    String getRequestUrl();

    void handleResponseText(String responseText);
  }

  /**
   * Implemented by the RPC service backing this factory.
   */
  interface Service {
    void fire(RequestObject request);
  }

  ValueStore getValueStore();

  // TODO actually a DeltaValueStore, List is an interim hack
  SyncRequest syncRequest(final List<Values<?>> deltaValueStore);

}
