// $Id: MessageInterpolator.java 17620 2009-10-04 19:19:28Z hardy.ferentschik $
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
//Changed by Google
package javax.validation;

import javax.validation.metadata.ConstraintDescriptor;

import com.google.gwt.i18n.shared.GwtLocale;

/**
 * Interpolate a given constraint violation message.
 * Implementations should be as tolerant as possible on syntax errors.
 *<p>
 * Modified to use {@link GwtLocale} instead of {@link java.util.Locale}.
 * 
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public interface MessageInterpolator {
  /**
   * Interpolate the message template based on the contraint validation context.
   * The locale is defaulted according to the <code>MessageInterpolator</code>
   * implementation. See the implementation documentation for more detail.
   *
   * @param messageTemplate The message to interpolate.
   * @param context contextual information related to the interpolation
   *
   * @return Interpolated error message.
   */
  String interpolate(String messageTemplate, Context context);

  /**
   * Interpolate the message template based on the contraint validation context.
   * The <code>Locale</code> used is provided as a parameter.
   *
   * @param messageTemplate The message to interpolate.
   * @param context contextual information related to the interpolation
   * @param locale the locale targeted for the message
   *
   * @return Interpolated error message.
   */
  String interpolate(String messageTemplate, Context context,  GwtLocale locale);

  /**
   * Information related to the interpolation context
   */
  interface Context {
    /**
     * @return ConstraintDescriptor corresponding to the constraint being validated
     */
    ConstraintDescriptor<?> getConstraintDescriptor();

    /**
     * @return value being validated
     */
    Object getValidatedValue();
  }
}
