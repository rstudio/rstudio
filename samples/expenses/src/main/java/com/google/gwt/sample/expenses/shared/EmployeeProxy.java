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
package com.google.gwt.sample.expenses.shared;

import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.EntityProxyId;
import com.google.web.bindery.requestfactory.shared.ProxyFor;

/**
 * Employee DTO.
 */
@ProxyFor(com.google.gwt.sample.expenses.server.domain.Employee.class)
public interface EmployeeProxy extends EntityProxy {
  String getDepartment();

  String getDisplayName();

  /* 
   * TODO You shouldn't need to expose Ids like this.
   * Instead use EntityProxy.stableId() and RequestFactory.find() 
   */
  Long getId();

  String getPassword();

  EmployeeProxy getSupervisor();

  String getUserName();

  void setDepartment(String department);

  void setDisplayName(String displayName);

  void setPassword(String password);

  void setSupervisor(EmployeeProxy supervisor);

  void setUserName(String userName);

  EntityProxyId<EmployeeProxy> stableId();
}
