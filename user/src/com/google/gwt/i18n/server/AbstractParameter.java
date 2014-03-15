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

import com.google.gwt.i18n.client.Localizable;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.i18n.client.Messages.PluralCount;
import com.google.gwt.i18n.client.Messages.Select;
import com.google.gwt.i18n.client.PluralRule;
import com.google.gwt.i18n.client.impl.plurals.DefaultRule;
import com.google.gwt.i18n.shared.AlternateMessageSelector;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import java.lang.annotation.Annotation;

/**
 * Base class for implementations of {@link Parameter}.
 */
public abstract class AbstractParameter implements Parameter {

  /**
   * Instantiate a plural rule class.
   *
   * @param pluralClass base plural rule class
   * @param locale
   * @return {@link PluralRule} instance for the specified locale
   */
  public static PluralRule getLocalizedPluralRule(
      Class<? extends PluralRule> pluralClass, GwtLocale locale) {
    // TODO(jat): is this the right place for this method?

    // Handle annotation default value
    if (PluralRule.class == pluralClass) {
      pluralClass = DefaultRule.class;
    }
    if (!Localizable.class.isAssignableFrom(pluralClass)) {
      try {
        return pluralClass.newInstance();
      } catch (InstantiationException e) {
        // TODO: log
        return null;
      } catch (IllegalAccessException e) {
        // TODO: log
        return null;
      }
    }
    String fqcn = pluralClass.getCanonicalName();
    for (GwtLocale search : locale.getCompleteSearchList()) {
      String cn = fqcn;
      if (!search.isDefault()) {
        cn += "_" + search.getAsString();
      }
      try {
        Class<?> clazz = Class.forName(cn);
        pluralClass = clazz.asSubclass(PluralRule.class);
        return pluralClass.newInstance();
      } catch (ClassCastException e) {
        // TODO(jat): log, but keep looking
      } catch (ClassNotFoundException e) {
        // expected, continue looking
      } catch (InstantiationException e) {
        // TODO(jat): log, but keep looking
      } catch (IllegalAccessException e) {
        // TODO(jat): log, but keep looking
      }
    }
    return null;
  }

  protected final GwtLocaleFactory localeFactory;

  protected final int index;

  protected final Type type;

  private AlternateMessageSelector altMsgSelector;

  public AbstractParameter(GwtLocaleFactory localeFactory, int index,
      Type type) {
    this.localeFactory = localeFactory;
    this.index = index;
    this.type = type;
  }

  public synchronized AlternateMessageSelector getAlternateMessageSelector() {
    if (altMsgSelector == null) {
      altMsgSelector = computeAlternateMessageSelector();
    }
    return altMsgSelector;
  }

  public abstract <A extends Annotation> A getAnnotation(Class<A> annotClass);

  public int getIndex() {
    return index;
  }

  public abstract String getName();

  public Type getType() {
    return type;
  }

  public boolean isAnnotationPresent(Class<? extends Annotation> annotClass) {
    return getAnnotation(annotClass) != null;
  }

  private AlternateMessageSelector computeAlternateMessageSelector() {
    PluralCount pluralAnnot = getAnnotation(PluralCount.class);
    if (pluralAnnot != null) {
      Class<? extends PluralRule> pluralClass = pluralAnnot.value();
      // TODO(jat): this seems redundant with other processing
      DefaultLocale defLocaleAnnot = getAnnotation(
          DefaultLocale.class);
      String defaultLocale = null;
      if (defLocaleAnnot != null) {
        defaultLocale = defLocaleAnnot.value();
      } else {
        defaultLocale = DefaultLocale.DEFAULT_LOCALE;
      }
      PluralRule pluralRule = getLocalizedPluralRule(pluralClass,
          localeFactory.fromString(defaultLocale));
      return new PluralRuleAdapter(pluralRule);
    }
    Select selectAnnot = getAnnotation(Select.class);
    if (selectAnnot != null) {
      final String[] validValues = type.getEnumValues();
      return new AlternateMessageSelector() {
        public boolean isFormAcceptable(String form) {
          if (validValues == null || AlternateMessageSelector.OTHER_FORM_NAME.equals(form)) {
            return true;
          }
          for (String value : validValues) {
            if (value.equals(form)) {
              return true;
            }
          }
          return false;
        }
      };
    }
    return null;
  }
}