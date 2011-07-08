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
package com.google.web.bindery.requestfactory.apt;

import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;
import com.google.web.bindery.requestfactory.shared.ValueProxy;

/**
 * Checks that proxy property accessors match exactly and that context method
 * arguments must match exactly.
 */
@Service(RequestContextWithMismatchedBoxes.Domain.class)
interface RequestContextWithMismatchedBoxes extends RequestContext {
  @Expect(method = "domainMissingMethod", args = "java.lang.Void checkBoxed(int)")
  Request<Void> checkBoxed(int value);

  @Expect(method = "domainMissingMethod", args = "java.lang.Void checkPrimitive(java.lang.Integer)")
  Request<Void> checkPrimitive(Integer value);

  @ProxyFor(Domain.class)
  interface ProxyMismatchedGetterA extends ValueProxy {
    @Expect(method = "domainMissingMethod", args = "int getBoxed()")
    int getBoxed();

    @Expect(method = "domainMissingMethod", args = "java.lang.Integer getPrimitive()")
    Integer getPrimitive();
  }

  static class Domain {
    static void checkBoxed(@SuppressWarnings("unused") Integer value) {
    }

    static void checkPrimitive(@SuppressWarnings("unused") int value) {
    }

    Integer getBoxed() {
      return null;
    }

    int getPrimitive() {
      return 0;
    }
  }
}
