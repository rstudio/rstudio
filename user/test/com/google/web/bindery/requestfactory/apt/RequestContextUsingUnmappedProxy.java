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

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;

@Service(RequestContextUsingUnmappedProxy.Domain.class)
interface RequestContextUsingUnmappedProxy extends RequestContext {
  public static class Domain {
  }

  @Expected({
      @Expect(method = "methodNoDomainPeer", args = {
          "com.google.web.bindery.requestfactory.apt.EntityProxyMissingDomainType", "false"}, warning = true),
      @Expect(method = "methodNoDomainPeer", args = {
          "com.google.web.bindery.requestfactory.apt.EntityProxyMissingDomainType", "true"}, warning = true)})
  Request<EntityProxyMissingDomainType> cannotVerify(EntityProxyMissingDomainType proxy);
}
