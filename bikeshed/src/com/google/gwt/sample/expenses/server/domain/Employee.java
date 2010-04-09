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
package com.google.gwt.sample.expenses.server.domain;

import java.util.List;

// @javax.persistence.Entity
/**
 * The Employee domain object.
 */
public class Employee implements Entity {
  public static List<Employee> findAllEmployees() {
    return Storage.INSTANCE.findAllEmployees();
  }

  public static Employee findEmployee(Long id) {
    return Storage.INSTANCE.findEmployee(id);
  }

  public static Employee findEmployeeByUserName(String userName) {
    return Storage.INSTANCE.findEmployeeByUserName(userName);
  }

  private final Long id;

  private final Integer version;

//  @javax.validation.constraints.Size(min = 2, max = 30)
  private String userName;

//  @javax.validation.constraints.Size(min = 2, max = 30)
  private String displayName;

  // @javax.persistence.ManyToOne(targetEntity =
  // com.google.io.expenses.server.domain.Employee.class)
  // @javax.persistence.JoinColumn
  private Employee supervisor;

  public Employee() {
    this(null, null);
  }

  Employee(Long id, Integer version) {
    this.id = id;
    this.version = version;
  }

  public <T> T accept(EntityVisitor<T> visitor) {
    return visitor.visit(this);
  }

  public String getDisplayName() {
    return displayName;
  }

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @return the supervisor
   */
  public Employee getSupervisor() {
    return supervisor;
  }

  /**
   * @return the userName
   */
  public String getUserName() {
    return userName;
  }

  /**
   * @return the version
   */
  public Integer getVersion() {
    return version;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * @param supervisor the supervisor to set
   */
  public void setSupervisor(Employee supervisor) {
    this.supervisor = supervisor;
  }

  /**
   * @param userName the userName to set
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }
}
