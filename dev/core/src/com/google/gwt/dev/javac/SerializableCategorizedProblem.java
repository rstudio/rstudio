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
package com.google.gwt.dev.javac;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import java.io.Serializable;

/**
 * Provides a way to serialize a {@link CategorizedProblem}.
 */
public class SerializableCategorizedProblem extends CategorizedProblem implements Serializable {
  private final int categoryId;
  private final String markerType;
  private final String[] arguments;
  private final String message;
  private final char[] originatingFileName;
  private final int sourceEnd;
  private final int sourceLineNumber;
  private final int sourceStart;
  private final boolean isError;
  private final boolean isWarning;
  private final String formattedString;

  SerializableCategorizedProblem(CategorizedProblem problem) {
    this.categoryId = problem.getCategoryID();
    this.markerType = problem.getMarkerType();
    this.arguments = problem.getArguments();
    this.message = problem.getMessage();
    this.originatingFileName = problem.getOriginatingFileName();
    this.sourceEnd = problem.getSourceEnd();
    this.sourceLineNumber = problem.getSourceLineNumber();
    this.sourceStart = problem.getSourceStart();
    this.isError = problem.isError();
    this.isWarning = problem.isWarning();
    this.formattedString = problem.toString();
  }

  @Override
  public String[] getArguments() {
    return arguments;
  }

  @Override
  public int getCategoryID() {
    return categoryId;
  }

  @Override
  public int getID() {
    return categoryId;
  }

  @Override
  public String getMarkerType() {
    return markerType;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public char[] getOriginatingFileName() {
    return originatingFileName;
  }

  @Override
  public int getSourceEnd() {
    return sourceEnd;
  }

  @Override
  public int getSourceLineNumber() {
    return sourceLineNumber;
  }

  @Override
  public int getSourceStart() {
    return sourceStart;
  }

  @Override
  public boolean isError() {
    return isError;
  }

  @Override
  public boolean isWarning() {
    return isWarning;
  }

  @Override
  public void setSourceEnd(int sourceEnd) {
    throw new RuntimeException("read only object");
  }

  @Override
  public void setSourceLineNumber(int lineNumber) {
    throw new RuntimeException("read only object");
  }

  @Override
  public void setSourceStart(int sourceStart) {
    throw new RuntimeException("read only object");
  }

  @Override
  public String toString() {
    return formattedString;
  }
}
