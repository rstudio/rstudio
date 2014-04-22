/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.util;

/**
 * A small set of compile metrics that should be used to warn incremental compile users when a
 * module is getting too large.
 */
public class TinyCompileSummary {

  private int typesForGeneratorsCount;
  private int typesForAstCount;
  private int staticSourceFilesCount;
  private int generatedSourceFilesCount;
  private int cachedStaticSourceFilesCount;
  private int cachedGeneratedSourceFilesCount;

  public int getTypesForGeneratorsCount() {
    return typesForGeneratorsCount;
  }

  public void setTypesForGeneratorsCount(int typesForGeneratorsCount) {
    this.typesForGeneratorsCount = typesForGeneratorsCount;
  }

  public int getTypesForAstCount() {
    return typesForAstCount;
  }

  public void setTypesForAstCount(int typesForAstCount) {
    this.typesForAstCount = typesForAstCount;
  }

  public int getStaticSourceFilesCount() {
    return staticSourceFilesCount;
  }

  public void setStaticSourceFilesCount(int staticSourceFilesCount) {
    this.staticSourceFilesCount = staticSourceFilesCount;
  }

  public int getGeneratedSourceFilesCount() {
    return generatedSourceFilesCount;
  }

  public void setGeneratedSourceFilesCount(int generatedSourceFilesCount) {
    this.generatedSourceFilesCount = generatedSourceFilesCount;
  }

  public int getCachedStaticSourceFilesCount() {
    return cachedStaticSourceFilesCount;
  }

  public void setCachedStaticSourceFilesCount(int cachedStaticSourceFilesCount) {
    this.cachedStaticSourceFilesCount = cachedStaticSourceFilesCount;
  }

  public int getCachedGeneratedSourceFilesCount() {
    return cachedGeneratedSourceFilesCount;
  }

  public void setCachedGeneratedSourceFilesCount(int cachedGeneratedSourceFilesCount) {
    this.cachedGeneratedSourceFilesCount = cachedGeneratedSourceFilesCount;
  }
}