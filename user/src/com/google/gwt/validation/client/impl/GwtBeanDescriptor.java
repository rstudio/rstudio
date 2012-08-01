/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.validation.client.impl;

import com.google.gwt.validation.client.ValidationGroupsMetadata;

import javax.validation.metadata.BeanDescriptor;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * Marker interface used by GWT to generate a {@link BeanDescriptor} for a
 * specific class T.
 * 
 * @param <T>
 */
public interface GwtBeanDescriptor<T> extends BeanDescriptor {
  void setValidationGroupsMetadata(ValidationGroupsMetadata validationGroupsMetadata);
}
