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
import com.google.gwt.i18n.shared.AlternateMessageSelector.AlternateForm;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A {@link MessageTranslation} that is backed by a map, along with an ordered
 * list of keys.
 */
class StringMapMessageTranslation
    implements MessageTranslation {

  private final String defaultMessage;
  private final List<String> forms;
  private final Map<String, String> map;
  private final GwtLocale locale;

  public StringMapMessageTranslation(String defaultMessage,
      List<String> forms, Map<String, String> map, GwtLocale locale) {
    this.defaultMessage = defaultMessage;
    this.forms = forms;
    this.map = map;
    this.locale = locale;
  }

  public Iterable<AlternateFormMapping> getAllMessageForms() {
    return new Iterable<AlternateFormMapping>() {
      protected final Iterator<String> iter = forms.iterator();

      public Iterator<AlternateFormMapping> iterator() {
        return new Iterator<AlternateFormMapping>() {
          public boolean hasNext() {
            return iter.hasNext();
          }

          public AlternateFormMapping next() {
            String form = iter.next();
            return new AlternateFormMapping(Arrays.asList(new AlternateForm(
                form, form)), map.get(form));
          }

          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  public String getDefaultMessage() {
    return defaultMessage;
  }

  public GwtLocale getMatchedLocale() {
    return locale;
  }
}