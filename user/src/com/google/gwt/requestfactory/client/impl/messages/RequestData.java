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
package com.google.gwt.requestfactory.client.impl.messages;

import com.google.gwt.requestfactory.client.impl.EntityCodex;
import com.google.gwt.requestfactory.shared.ValueCodex;
import com.google.gwt.requestfactory.shared.impl.Constants;

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
   * Used by InstanceRequest subtypes to reset the instance object in the
   * <code>using</code> method.
   */
  public Object[] getParameters() {
    return parameters;
  }

  /**
   * Returns the string that encodes the request data.
   * 
   */
  public Map<String, String> getRequestMap(String contentData) {
    Map<String, String> requestMap = new HashMap<String, String>();
    requestMap.put(Constants.OPERATION_TOKEN,
        ValueCodex.encodeForJsonPayload(operation));
    if (parameters != null) {
      for (int i = 0; i < parameters.length; i++) {
        Object value = parameters[i];
        requestMap.put(Constants.PARAM_TOKEN + i,
            EntityCodex.encodeForJsonPayload(value));
      }
    }
    if (contentData != null) {
      // Yes, double-encoding
      requestMap.put(Constants.CONTENT_TOKEN,
          ValueCodex.encodeForJsonPayload(contentData));
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
      requestMap.put(Constants.PROPERTY_REF_TOKEN,
          ValueCodex.encodeForJsonPayload(props.toString()));
    }
    return requestMap;
  }
}
