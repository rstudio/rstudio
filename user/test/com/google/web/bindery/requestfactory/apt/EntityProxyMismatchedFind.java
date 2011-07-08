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
import com.google.web.bindery.requestfactory.shared.ProxyFor;

@Expect(method = "domainMissingFind", args = {
    "com.google.web.bindery.requestfactory.apt.EntityProxyMismatchedFind.Domain", "Domain",
    "java.lang.String", "EntityProxyMismatchedFind"}, warning = true)
@ProxyFor(EntityProxyMismatchedFind.Domain.class)
@SuppressWarnings("requestfactory")
interface EntityProxyMismatchedFind extends EntityProxy {
  public static class Domain {
    public static Domain findDomain(@SuppressWarnings("unused") Integer id) {
      return null;
    }

    public String getId() {
      return null;
    }

    public String getVersion() {
      return null;
    }
  }
}
