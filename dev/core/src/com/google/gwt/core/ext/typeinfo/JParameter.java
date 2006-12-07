/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.core.ext.typeinfo;

public class JParameter implements HasMetaData {

  private final HasMetaData metaData = new MetaData();

  private final String name;

  private JType type;

  private final JAbstractMethod enclosingMethod;

  public JParameter(JAbstractMethod enclosingMethod, JType type, String name) {
    this.enclosingMethod = enclosingMethod;
    this.type = type;
    this.name = name;

    enclosingMethod.addParameter(this);
  }

  public void addMetaData(String tagName, String[] values) {
    metaData.addMetaData(tagName, values);
  }

  public JAbstractMethod getEnclosingMethod() {
    return enclosingMethod;
  }

  public String[][] getMetaData(String tagName) {
    return metaData.getMetaData(tagName);
  }

  public String[] getMetaDataTags() {
    return metaData.getMetaDataTags();
  }

  public String getName() {
    return name;
  }

  public JType getType() {
    return type;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(type.getQualifiedSourceName());
    sb.append(" ");
    sb.append(getName());
    return sb.toString();
  }

  // Called when parameter types are found to be parameterized
  void setType(JType type) {
    this.type = type;
  }
}
