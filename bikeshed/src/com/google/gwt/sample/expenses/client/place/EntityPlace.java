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
package com.google.gwt.sample.expenses.client.place;

import com.google.gwt.sample.expenses.shared.ExpensesEntity;

/**
 * A place in the app focused on a particular {@link ExpensesEntity}
 */
public abstract class EntityPlace extends ExpensesScaffoldPlace {
  private final ExpensesEntity<?> entityRef;

  public EntityPlace(ExpensesEntity<?> entityRef) {
    assert null != entityRef;
    this.entityRef = entityRef;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    EntityPlace other = (EntityPlace) obj;
    return entityRef.getId().equals(other.entityRef.getId());
  }

  public ExpensesEntity<?> getEntity() {
    return entityRef;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + entityRef.getId().hashCode();
    return result;
  }
}
