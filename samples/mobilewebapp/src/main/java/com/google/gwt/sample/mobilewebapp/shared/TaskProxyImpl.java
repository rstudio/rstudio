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
package com.google.gwt.sample.mobilewebapp.shared;

import com.google.web.bindery.requestfactory.shared.EntityProxyId;

import java.util.Date;

/**
 * A task used in the task list.
 */
public class TaskProxyImpl implements TaskProxy {
  private Date dueDate;
  private Long id;
  private String name;
  private String notes;

  public TaskProxyImpl() {
  }

  public TaskProxyImpl(String name, String notes) {
    this.name = name;
    this.notes = notes;
  }

  public Date getDueDate() {
    return dueDate;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getNotes() {
    return notes;
  }

  public void setDueDate(Date dueDate) {
    this.dueDate = dueDate;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public EntityProxyId<?> stableId() {
    return null;
  }
}
