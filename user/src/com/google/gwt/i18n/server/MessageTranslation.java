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
package com.google.gwt.i18n.server;

import com.google.gwt.i18n.server.Message.AlternateFormMapping;
import com.google.gwt.i18n.shared.GwtLocale;

/**
 * A translation of a single message.
 */
public interface MessageTranslation {

  /**
   * Get all the alternate forms of this message.
   * 
   * @return an iteration of {@link AlternateFormMapping} instances
   */
  Iterable<AlternateFormMapping> getAllMessageForms();

  /**
   * Get the message to use if nothing else matches.
   *
   * @return the default message
   */
  String getDefaultMessage();

  /**
   * Get the locale for this translation.
   * 
   * @return locale instance
   */
  GwtLocale getMatchedLocale();
}
