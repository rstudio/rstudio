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
package com.google.gwt.i18n.rebind;

import com.google.gwt.i18n.rebind.util.AbstractResource;
import com.google.gwt.user.rebind.AbstractGeneratorClassCreator;
import com.google.gwt.user.rebind.AbstractMethodCreator;

import java.util.Locale;

/**
 * A <code>AbstractMethodCreator</code> specialized for
 * <code>ConstantsImplCreator</code>.
 */
abstract class AbstractLocalizableMethodCreator extends AbstractMethodCreator {
  /**
   * Constructor for <code>AbstractLocalizableMethodCreator</code>.
   * 
   * @param classCreator Creator associated with this method creator
   */
  public AbstractLocalizableMethodCreator(
      AbstractGeneratorClassCreator classCreator) {
    super(classCreator);
  }

  /**
   * Enables caching to store computed values.
   */
  protected void enableCache() {
    ((ConstantsImplCreator) currentCreator).setNeedCache(true);
  }

  /**
   * Gets the associated locale.
   * 
   * @return the locale
   */
  protected Locale getLocale() {
    return getResources().getLocale();
  }

  /**
   * Get the resources associated with this class.
   * 
   * @return associated resources.
   */
  protected AbstractResource getResources() {
    return ((ConstantsImplCreator) currentCreator).getResourceBundle();
  }
}
