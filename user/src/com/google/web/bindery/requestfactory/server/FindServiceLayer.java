/*
 * Copyright 2011 Google Inc.
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
package com.google.web.bindery.requestfactory.server;

import com.google.web.bindery.requestfactory.server.impl.FindService;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.impl.Constants;
import com.google.web.bindery.requestfactory.shared.impl.FindRequest;

import java.lang.reflect.Method;

/**
 * Allows the use of a very short operation name for the find method. This also
 * avoids the need to introduce special-case code for FindRequest into
 * RequestFactoryInterfaceValidator.
 */
public class FindServiceLayer extends ServiceLayerDecorator {

  @Override
  public Method resolveDomainMethod(String operation) {
    if (Constants.FIND_METHOD_OPERATION.equals(operation)) {
      Throwable ex;
      try {
        return FindService.class.getMethod("find", Object.class);
      } catch (SecurityException e) {
        ex = e;
      } catch (NoSuchMethodException e) {
        ex = e;
      }
      die(ex, "Could not retrieve %s.find() method", FindService.class.getCanonicalName());
    }
    return super.resolveDomainMethod(operation);
  }

  @Override
  public Class<? extends RequestContext> resolveRequestContext(String operation) {
    if (Constants.FIND_METHOD_OPERATION.equals(operation)) {
      return FindRequest.class;
    }
    return super.resolveRequestContext(operation);
  }

  @Override
  public Method resolveRequestContextMethod(String operation) {
    if (Constants.FIND_METHOD_OPERATION.equals(operation)) {
      Throwable ex;
      try {
        return FindRequest.class.getMethod("find", EntityProxyId.class);
      } catch (SecurityException e) {
        ex = e;
      } catch (NoSuchMethodException e) {
        ex = e;
      }
      die(ex, "Could not retrieve %s.find() method", FindRequest.class.getCanonicalName());
    }
    return super.resolveRequestContextMethod(operation);
  }
}
