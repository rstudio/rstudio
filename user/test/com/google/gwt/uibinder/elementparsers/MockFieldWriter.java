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
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.uibinder.rebind.DesignTimeUtils;
import com.google.gwt.uibinder.rebind.FieldWriter;
import com.google.gwt.uibinder.rebind.FieldWriterType;
import com.google.gwt.uibinder.rebind.IndentedWriter;
import com.google.gwt.uibinder.rebind.MonitoredLogger;
import com.google.gwt.uibinder.rebind.model.OwnerField;

final class MockFieldWriter implements FieldWriter {
  private final String tag;

  MockFieldWriter(String tag) {
    this.tag = tag;
  }

  @Override
  public void addAttachStatement(String format, Object... args) {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public void addDetachStatement(String format, Object... args) {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public void addStatement(String format, Object... args) {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public JClassType getAssignableType() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public int getBuildPrecedence() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public FieldWriterType getFieldType() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public String getHtml() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public String getInitializer() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public JClassType getInstantiableType() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public String getNextReference() {
    return tag;
  }

  @Override
  public String getName() {
    return tag;
  }

  @Override
  public String getQualifiedSourceName() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public JType getReturnType(String[] path, MonitoredLogger logger) {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public String getSafeHtml() {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public void needs(FieldWriter f) {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public void setBuildPrecedence(int precedence) {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public void setHtml(String html) {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public void setInitializer(String initializer) {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public void write(IndentedWriter w) {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public void writeFieldBuilder(IndentedWriter w, int getterCount,
      OwnerField ownerField) {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }

  @Override
  public void writeFieldDefinition(IndentedWriter w, TypeOracle typeOracle,
      OwnerField ownerField, DesignTimeUtils designTime, int getterCount,
      boolean useLazyWidgetBuilders) {
    throw new UnsupportedOperationException("Auto-generated method stub");
  }
}