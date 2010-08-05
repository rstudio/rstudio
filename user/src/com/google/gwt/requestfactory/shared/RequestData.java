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

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * A class that encapsulates the parameters and method name to be invoked on the
 * server.
 * 
 * TODO: add appropriate unit tests.
 */
public class RequestData {

  public static final String CONTENT_TOKEN = "contentData";
  public static final String OPERATION_TOKEN = "operation";
  public static final String PARAM_TOKEN = "param";

  // TODO: non-final is a hack for now.
  private String operation;
  private final Object[] parameters;

  public RequestData(String operation, Object[] parameters) {
    this.operation = operation;
    this.parameters = parameters;
  }

  /**
   * Returns the string that encodes the request data.
   * 
   */
  public Map<String, String> getRequestMap(String contentData) {
    Map<String, String> requestMap = new HashMap<String, String>();
    // nasty hack, remove.
    if (operation.endsWith("persist") || operation.endsWith("remove")) {
      operation = RequestFactory.SYNC;
    }
    requestMap.put(OPERATION_TOKEN, operation);
    if (parameters != null) {
      for (int i = 0; i < parameters.length; i++) {
        Object value = parameters[i];
        requestMap.put(PARAM_TOKEN + i, value.toString());
      }
    }
    if (contentData != null) {
      requestMap.put(CONTENT_TOKEN, contentData);
    }
    return requestMap;
  }
}
