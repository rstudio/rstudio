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
package com.google.gwt.requestfactory.shared.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * <span style="color:red">Experimental API: This class is still under rapid
 * development, and is very likely to be deleted. Use it at your own risk.
 * </span>
 * </p>
 * A class that encapsulates the parameters and method name to be invoked on the
 * server.
 */
public class RequestData {

  public static final String CONTENT_TOKEN = "contentData";
  public static final String OPERATION_TOKEN = "operation";
  public static final String PARAM_TOKEN = "param";
  public static final String PROPERTY_REF_TOKEN = "propertyRefs";

  public static final String RESULT_TOKEN = "result";

  public static final String RELATED_TOKEN = "related";

  public static final String SIDE_EFFECTS_TOKEN = "sideEffects";
  
  public static final String VIOLATIONS_TOKEN = "violations";
  
  /**
   * Property on a proxy JSO that holds its encoded server side data store id.
   */
  public static final String ENCODED_ID_PROPERTY = "!id";
  
  /**
   * Id property that server entity objects are required to define 
   */
  public static final String ENTITY_ID_PROPERTY = "id";
  

  // TODO: non-final is a hack for now.
  private final String operation;
  private final Object[] parameters;

  private final Set<String> propertyRefs;

  public RequestData(String operation, Object[] parameters,
      Set<String> propertyRefs) {
    this.operation = operation;
    this.parameters = parameters;
    this.propertyRefs = propertyRefs;
  }

  /**
   * Returns the string that encodes the request data.
   * 
   */
  public Map<String, String> getRequestMap(String contentData) {
    Map<String, String> requestMap = new HashMap<String, String>();
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

    if (propertyRefs != null && !propertyRefs.isEmpty()) {
      StringBuffer props = new StringBuffer();
      Iterator<String> propIt = propertyRefs.iterator();
      while (propIt.hasNext()) {
        props.append(propIt.next());
        if (propIt.hasNext()) {
          props.append(",");
        }
      }
      requestMap.put(PROPERTY_REF_TOKEN, props.toString());
    }
    return requestMap;
  }
}
