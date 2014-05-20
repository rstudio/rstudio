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
package java.lang;

import java.io.Serializable;
import java.util.Objects;

/**
 * Included for hosted mode source compatibility. Partially implemented
 * 
 * @skip
 */
public final class StackTraceElement implements Serializable {

  private String className;

  private String fileName;

  private int lineNumber;

  private String methodName;

  public StackTraceElement() {
  }

  public StackTraceElement(String className, String methodName,
      String fileName, int lineNumber) {
    assert className != null;
    assert methodName != null;
    this.className = className;
    this.methodName = methodName;
    this.fileName = fileName;
    this.lineNumber = lineNumber;
  }

  public String getClassName() {
    return className;
  }

  public String getFileName() {
    return fileName;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public String getMethodName() {
    return methodName;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof StackTraceElement) {
      StackTraceElement st = (StackTraceElement) other;
      return lineNumber == st.lineNumber
          && Objects.equals(methodName, st.methodName)
          && Objects.equals(className, st.className)
          && Objects.equals(fileName, st.fileName);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(lineNumber, className, methodName, fileName);
  }

  @Override
  public String toString() {
    return className + "." + methodName + "("
        + (fileName != null ? fileName : "Unknown Source")
        + (lineNumber >= 0 ? ":" + lineNumber : "") + ")";
  }
}
