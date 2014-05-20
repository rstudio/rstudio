/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.user.server.rpc;

import com.google.gwt.user.client.rpc.CustomFieldSerializerTestService;
import com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetFactory.SerializableSubclass;
import com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetValidator;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.ManuallySerializedClass;
import com.google.gwt.user.client.rpc.ManuallySerializedImmutableClass;

import javax.servlet.ServletContext;

/**
 * Servlet used by the
 * {@link com.google.gwt.user.client.rpc.CustomFieldSerializerTest
 * CustomFieldSerializerTest} unit test.
 */
public class CustomFieldSerializerTestServiceImpl extends RemoteServiceServlet
    implements CustomFieldSerializerTestService {

  /**
   * Filters log messages to avoid logging spurious warnings during successful
   * tests.
   */
  private ServletContext wrappedServletContext;

  @Override
  public ManuallySerializedClass echo(
      ManuallySerializedClass unserializableClass) {
    if (!CustomFieldSerializerTestSetValidator.isValid(unserializableClass)) {
      throw new RuntimeException();
    }

    return unserializableClass;
  }

  @Override
  public ManuallySerializedImmutableClass[] echo(
      ManuallySerializedImmutableClass[] manuallySerializableImmutables) {
    if (!CustomFieldSerializerTestSetValidator.isValid(manuallySerializableImmutables)) {
      throw new RuntimeException();
    }

    return manuallySerializableImmutables;
  }

  @Override
  public SerializableSubclass echo(SerializableSubclass serializableClass) {
    if (!CustomFieldSerializerTestSetValidator.isValid(serializableClass)) {
      throw new RuntimeException();
    }

    return serializableClass;
  }

  /**
   * Overrides the default servlet context to filter expected log messages.
   */
  @Override
  public ServletContext getServletContext() {
    if (wrappedServletContext == null) {
      wrappedServletContext = new LogFilterServletContext(
          super.getServletContext()) {
        @Override
        protected boolean shouldLog(Throwable t, String msg) {
          if (t instanceof IncompatibleRemoteServiceException
              && t.getMessage().contains(
                  "com.google.gwt.user.client.rpc.CustomFieldSerializerTestSetFactory$UnserializableSubclass")) {
            return false;
          }
          return true;
        }
      };
    }
    return wrappedServletContext;
  }
}
