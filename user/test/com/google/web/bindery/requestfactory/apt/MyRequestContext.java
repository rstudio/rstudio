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
import com.google.web.bindery.requestfactory.shared.JsonRpcProxy;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.ProxyForName;
import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.ValueProxy;

/*
 * No error about a missing mapping expected because this type isn't referenced
 * from a RequestFactory.
 */
interface MyRequestContext extends RequestContext {
  @Expect(method = "proxyMustBeAnnotated", args = "ProxyMissingAnnotation")
  interface ProxyMissingAnnotation extends EntityProxy {
  }

  @Expected({
      @Expect(method = "proxyMissingDomainType", args = "bad", warning = true),
      @Expect(method = "redundantAnnotation", args = "ProxyForName"),
      @Expect(method = "redundantAnnotation", args = "JsonRpcProxy")})
  @ProxyFor(ProxyWithRedundantAnnotations.Domain.class)
  @ProxyForName("bad")
  @JsonRpcProxy
  interface ProxyWithRedundantAnnotations extends ValueProxy {
    static class Domain {
    }
  }

  /**
   * Because this is not referenced from the context, it shouldn't generate an
   * error.
   */
  interface UnusedProxyBase extends EntityProxy {
  }

  @Expect(method = "untransportableType", args = "java.lang.Object")
  InstanceRequest<ProxyWithRedundantAnnotations, Object> badInstanceReturn();

  @Expect(method = "contextRequiredReturnTypes", args = {"Request", "InstanceRequest"})
  String badMethod();

  Request<Void> badParam(@Expect(method = "untransportableType", args = "java.lang.Object") Object o);

  @Expect(method = "untransportableType", args = "java.lang.Object")
  Request<Object> badReturn();

  Request<ProxyMissingAnnotation> forceAnnotation();

  @Expect(method = "rawType")
  @SuppressWarnings("rawtypes")
  InstanceRequest rawInstance();

  @Expect(method = "rawType")
  @SuppressWarnings("rawtypes")
  Request rawRequest();
}