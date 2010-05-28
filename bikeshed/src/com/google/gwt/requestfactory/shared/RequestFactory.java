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
import com.google.gwt.valuestore.shared.DeltaValueStore;
import com.google.gwt.valuestore.shared.Record;
import com.google.gwt.valuestore.shared.ValueStore;

import java.util.Map;
import java.util.Set;

/**
 * Marker interface for the RequestFactory code generator.
 */
public interface RequestFactory {

  // TODO all these inner interfaces are clutter, move them to their own files

  /**
   * Implemented by the configuration class used by
   * {@link com.google.gwt.requestfactory.server.RequestFactoryServlet
   * RequestFactoryServlet}.
   */
  interface Config {
    Map<String, RequestDefinition> requestDefinitions();

    Set<Class<? extends Record>> recordTypes();
  }

  /**
   * Implemented by enums that define the mapping between request objects and
   * service methods.
   */
  interface RequestDefinition {
    /**
     * Returns the name of the (domain) class that contains the method to be
     * invoked on the server.
     */
    String getDomainClassName();

    /**
     * Returns the name of the method to be invoked on the server.
     */
    String getDomainMethodName();

    /**
     * Returns the parameter types of the method to be invoked on the server.
     */
    Class<?>[] getParameterTypes();

    /**
     * Returns the return type of the method to be invoked on the server.
     */
    Class<?> getReturnType();

    /**
     * Returns true if the request returns Lists of {@link #getReturnType},
     * false for single instances.
     */
    boolean isReturnTypeList();

    /**
     * Returns the name.
     */
    String name();
  }

  /**
   * Implemented by the request objects created by this factory.
   */
  interface RequestObject<T> {
    void fire();

    String getRequestData();

    void handleResponseText(String responseText);

    RequestObject<T> to(Receiver<T> receiver);
  }

  // TODO: this must be configurable
  String URL = "/expenses/data";

  String SYNC = "SYNC";

  ValueStore getValueStore();

  void init(HandlerManager eventBus);

  SyncRequest syncRequest(DeltaValueStore deltaValueStore);

  /**
   * The write operation enum used in DeltaValueStore.
   */
  enum WriteOperation {
    CREATE, UPDATE, DELETE
  }
}
