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

import com.google.gwt.i18n.client.ConstantsWithLookup;

import java.util.MissingResourceException;

/**
 * <strong>EXPERIMENTAL</strong> and subject to change. Do not use this in
 * production code.
 * <p>
 * ValidationMessageResolver using a {@link ConstantsWithLookup} source.
 */
public abstract class AbstractValidationMessageResolver {
  private final ConstantsWithLookup messages;

  protected AbstractValidationMessageResolver(ConstantsWithLookup messages) {
    this.messages = messages;
  }

  public final String get(String key) {
    try {
      // Replace "." with "_" in the key to match what ConstantsImplCreator
      // does.
      return key == null ? null : messages.getString(key.replace(".", "_"));
    } catch (MissingResourceException e) {
      return null;
    }
  }
}
