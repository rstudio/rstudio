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
 * Creates a new entity of the type of the receiver.
 */
@SuppressWarnings("unchecked")
// We guarantee same type at runtime
public class CreationVisitor<E extends Entity> implements EntityVisitor<E> {
  private final long id;
  private final int version;

  /**
   * @param entity whose id and version will be copied. Used to create empty
   *          delta.
   */
  public CreationVisitor(E entity) {
    id = entity.getId();
    version = entity.getVersion();
  }

  /**
   * @param id of the new Entity
   * @param i
   */
  public CreationVisitor(long id, int version) {
    this.id = id;
    this.version = version;
  }

  public E visit(Currency currency) {
    return (E) new Currency(id, version);
  }

  public E visit(Employee employee) {
    return (E) new Employee(id, version);
  }

  public E visit(Report report) {
    return (E) new Report(id, version);
  }

  public E visit(ReportItem reportItem) {
    return (E) new ReportItem(id, version);
  }
}
