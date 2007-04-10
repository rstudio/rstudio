/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.junit.benchmarks;

/**
 * Benchmark Category information. Part of the overall MetaData for a
 * Benchmark. It is the backing store for com.google.gwt.junit.client.Category.
 */
class CategoryImpl {

  private String className;
  private String description;
  private String name;

  public CategoryImpl( String className, String name, String description ) {
    this.className = className;
    this.name = name;
    this.description = description;
  }

  public String getClassName() {
    return className;
  }

  public String getDescription() {
    return description;
  }
  
  public String getName() {
    return name;
  }
}
