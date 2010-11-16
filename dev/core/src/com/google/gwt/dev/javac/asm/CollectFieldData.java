/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.javac.asm;

import com.google.gwt.dev.asm.AnnotationVisitor;
import com.google.gwt.dev.asm.commons.EmptyVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * Collect data from a single field.
 */
public class CollectFieldData extends EmptyVisitor {

  private final List<CollectAnnotationData> annotations = new ArrayList<CollectAnnotationData>();
  private final int access;
  private final String name;
  private final String desc;
  private final String signature;
  private final Object value;

  public CollectFieldData(int access, String name, String desc,
      String signature, Object value) {
    this.access = access;
    this.name = name;
    this.desc = desc;
    this.signature = signature;
    this.value = value;
  }

  /**
   * @return the access
   */
  public int getAccess() {
    return access;
  }

  /**
   * @return the annotations
   */
  public List<CollectAnnotationData> getAnnotations() {
    return annotations;
  }

  /**
   * @return the desc
   */
  public String getDesc() {
    return desc;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the signature
   */
  public String getSignature() {
    return signature;
  }

  /**
   * @return the value
   */
  public Object getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "field " + name;
  }

  @Override
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    CollectAnnotationData av = new CollectAnnotationData(desc, visible);
    annotations.add(av);
    return av;
  }
}
