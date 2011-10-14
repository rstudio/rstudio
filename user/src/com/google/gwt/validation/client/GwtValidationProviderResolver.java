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
package com.google.gwt.validation.client;

import com.google.gwt.core.client.GWT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.ValidationProviderResolver;
import javax.validation.spi.ValidationProvider;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * The default GWT {@link ValidationProviderResolver}. This always returns the
 * single default ValidationProvider using {@link GWT#create(Class)}.
 */
public final class GwtValidationProviderResolver implements
    ValidationProviderResolver {

  private static final List<ValidationProvider<?>> defaultList =
      Collections.unmodifiableList(createValidationProviderList());

  private static ArrayList<ValidationProvider<?>> createValidationProviderList() {
    ArrayList<ValidationProvider<?>> temp = new ArrayList<ValidationProvider<?>>();
    ValidationProvider<?> provider = GWT.create(ValidationProvider.class);
    temp.add(provider);
    return temp;
  }

  public List<ValidationProvider<?>> getValidationProviders() {
    return defaultList;
  }
}
