/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.benchmarks.viewer.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;

/**
 * A data object for Benchmark.
 */
public class Benchmark implements IsSerializable {

  private String className;

  private String description;

  private String name;

  private List<Result> results;

  private String sourceCode;

  public String getClassName() {
    return className;
  }

  public String getDescription() {
    return description;
  }

  public String getName() {
    return name;
  }

  public List<Result> getResults() {
    return results;
  }

  public String getSourceCode() {
    return sourceCode;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setResults(List<Result> results) {
    this.results = results;
  }

  public void setSourceCode(String sourceCode) {
    this.sourceCode = sourceCode;
  }
}
