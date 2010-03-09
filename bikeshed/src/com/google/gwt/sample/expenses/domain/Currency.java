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
package com.google.gwt.sample.expenses.domain;

/**
 * Models a type of currency.
 */
// @javax.persistence.Entity
public class Currency implements Entity {
//  @javax.validation.constraints.Size(min = 3, max = 3)
  private String code;

//  @javax.validation.constraints.Size(min = 2, max = 30)
  private String name;

  private final Long id;

  private final Integer version;

  public Currency() {
    id = null;
    version = null;
  }

  Currency(Long id, Integer version) {
    this.id = id;
    this.version = version;
  }

  public <T> T accept(EntityVisitor<T> visitor) {
    return visitor.visit(this);
  }

  /**
   * @return the code
   */
  public String getCode() {
    return code;
  }

  /**
   * @return the id
   */
  public Long getId() {
    return id;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the version
   */
  public Integer getVersion() {
    return version;
  }

  /**
   * @param code the code to set
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }
}
