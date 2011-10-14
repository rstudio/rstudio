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
package com.google.gwt.validation.client.spi;

import com.google.gwt.core.client.GWT;

import javax.validation.ValidationProviderResolver;
import javax.validation.spi.BootstrapState;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * GWT {@link BootstrapState}.
 */
public final class GwtBootStrapState implements BootstrapState {

  public ValidationProviderResolver getDefaultValidationProviderResolver() {
    return GWT.create(ValidationProviderResolver.class);
  }

  public ValidationProviderResolver getValidationProviderResolver() {
    return null;
  }
}
