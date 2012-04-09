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

import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.InstanceRequest;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;

/**
 * Tests Request and InstanceRequest methods bound to methods with the wrong
 * static modifier.
 */
@Service(RequestContextWithMismatchedInstance.Domain.class)
interface RequestContextWithMismatchedInstance extends RequestContext {
  static class Domain {
    public static Domain findDomain(@SuppressWarnings("unused") String id) {
      return null;
    }

    public static void staticMethod() {
    }

    public String getId() {
      return null;
    }

    public String getVersion() {
      return null;
    }

    public void instanceMethod() {
    }
  }

  @ProxyFor(Domain.class)
  interface Proxy extends EntityProxy {
  }

  @Expect(method = "domainMethodWrongModifier", args = {"true", "instanceMethod"})
  Request<Void> instanceMethod();

  @Expect(method = "domainMethodWrongModifier", args = {"false", "staticMethod"})
  InstanceRequest<Proxy, Void> staticMethod();
}
