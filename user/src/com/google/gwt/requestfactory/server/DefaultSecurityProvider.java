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
package com.google.gwt.requestfactory.server;

import com.google.gwt.requestfactory.shared.Service;
import com.google.gwt.requestfactory.shared.ServiceName;

/**
 * A security provider that enforces
 * {@link com.google.gwt.requestfactory.shared.Service} annotations.
 */
class DefaultSecurityProvider implements RequestSecurityProvider {

  public void checkClass(Class<?> clazz) throws SecurityException {
    Service service = clazz.getAnnotation(Service.class);
    ServiceName serviceName = clazz.getAnnotation(ServiceName.class);
    String className;
    if (service != null) {
      className = service.value().getName();
    } else if (serviceName != null) {
      className = serviceName.value();
    } else {
      throw new SecurityException("Class " + clazz.getName()
          + " does not have a @Service annotation.");
    }
    try {
      Class.forName(className);
    } catch (ClassNotFoundException e) {
      throw new SecurityException("Class " + className
          + " from @Service annotation on " + clazz + " cannot be loaded.");
    }
  }

  public String encodeClassType(Class<?> type) {
    return type.getName();
  }

  public String mapOperation(String operationName) throws SecurityException {
    return operationName;
  }
}
