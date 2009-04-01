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
package com.google.gwt.benchmarks;

/**
 * The benchmark metadata for a single benchmark method.
 */
class MetaData {

  private String className;

  private String methodName;

  private String sourceCode;

  private String testDescription;

  private String testName;

  public MetaData(String className, String methodName, String sourceCode,
      String testName, String testDescription) {
    this.className = className;
    this.methodName = methodName;
    this.sourceCode = sourceCode;
    this.testName = testName;
    this.testDescription = testDescription;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MetaData)) {
      return false;
    }

    MetaData md = (MetaData) obj;

    return md.className.equals(className) && md.methodName.equals(methodName);
  }

  public String getClassName() {
    return className;
  }

  public String getMethodName() {
    return methodName;
  }

  public String getSourceCode() {
    return sourceCode;
  }

  public String getTestDescription() {
    return testDescription;
  }

  public String getTestName() {
    return testName;
  }

  @Override
  public int hashCode() {
    int result;
    result = (className != null ? className.hashCode() : 0);
    result = 29 * result + (methodName != null ? methodName.hashCode() : 0);
    return result;
  }
}
