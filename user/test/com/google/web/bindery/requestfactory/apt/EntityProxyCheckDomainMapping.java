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

@Expected({
    @Expect(method = "domainGetIdStatic"),
    @Expect(method = "domainGetVersionStatic"),
    @Expect(method = "domainFindNotStatic", args = "Domain"),
    @Expect(method = "domainNoDefaultConstructor", args = {
        "Domain", "EntityProxyCheckDomainMapping", "RequestContext"}, warning = true)})
@ProxyFor(EntityProxyCheckDomainMapping.Domain.class)
@SuppressWarnings("requestfactory")
interface EntityProxyCheckDomainMapping extends EntityProxy {
  public static class Domain {
    public static String getFoo() {
      return null;
    }

    public static String getId() {
      return null;
    }

    public static String getVersion() {
      return null;
    }

    public Domain(@SuppressWarnings("unused") boolean ignored) {
    }

    public Domain findDomain(@SuppressWarnings("unused") String id) {
      return null;
    }

    String getNotPublic() {
      return null;
    }
  }

  @Expect(method = "domainMethodWrongModifier", args = {"false", "getFoo"})
  String getFoo();

  @Expect(method = "domainMissingMethod", args = "java.lang.String getMissingProperty()")
  String getMissingProperty();

  @Expect(method = "domainMethodNotPublic", args = "getNotPublic")
  String getNotPublic();

  @Expected({
      @Expect(method = "methodNoDomainPeer", args = {"java.lang.Object", "false"}, warning = true),
      @Expect(method = "untransportableType", args = "java.lang.Object")})
  Object getUntransportable();

  @Expect(method = "methodNoDomainPeer", args = {"java.lang.Object", "true"}, warning = true)
  void setUntransportable(
      @Expect(method = "untransportableType", args = "java.lang.Object") Object obj);

  @Expected({
      @Expect(method = "proxyOnlyGettersSetters"),
      @Expect(method = "domainMissingMethod", args = "java.lang.String notAProperty()")})
  String notAProperty();
}
