/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.dev.util.Util;
import com.google.gwt.i18n.shared.GwtLocale;

import org.apache.tapestry.util.text.LocalizedProperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Resource wrapper for localized properties.
 */
class LocalizedPropertiesResource extends AbstractResource {

  static class Factory extends ResourceFactory {
    @Override
    public String getExt() {
      return "properties";
    }

    @Override
    public AbstractResource load(InputStream m, GwtLocale locale) {
      LocalizedPropertiesResource bundle = new LocalizedPropertiesResource(m,
          locale);
      return bundle;
    }
  }

  private Map<String, MultipleFormEntry> entries;

  @SuppressWarnings("unchecked")
  public LocalizedPropertiesResource(InputStream m, GwtLocale locale) {
    super(locale);
    LocalizedProperties props = new LocalizedProperties();
    try {
      props.load(m, Util.DEFAULT_ENCODING);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load " + this.getPath(), e);
    }
    entries = new HashMap<String, MultipleFormEntry>();
    for (Object propEntryObj : props.getPropertyMap().entrySet()) {
      Map.Entry<String, String> propEntry = (Entry<String, String>) propEntryObj;
      String key = propEntry.getKey().trim();
      String value = propEntry.getValue();
      int startBracket = key.indexOf('[');
      int endBracket = key.indexOf(']', startBracket + 1);
      String form = null;
      if (startBracket >= 0 && endBracket == key.length() - 1) {
        form = key.substring(startBracket + 1, endBracket);
        key = key.substring(0, startBracket);
      }
      MultipleFormEntry entry = entries.get(key);
      if (entry == null) {
        entry = new MultipleFormEntry(key);
        entries.put(key, entry);
      }
      entry.addForm(form, value);
    }
  }

  @Override
  public void addToKeySet(Set<String> s) {
    s.addAll(entries.keySet());
    for (String key : entries.keySet()) {
      /*
       * Remove plural forms from the key list.  They will be looked up as
       * extensions to the base key (@see getStringExt()).
       */
      if (key.indexOf('[') < 0) {
        s.add(key);
      }
    }
  }

  @Override
  public ResourceEntry getEntry(String key) {
    return entries.get(key);
  }

  @Override
  public String getStringExt(String key, String extension) {
    ResourceEntry entry = getEntry(key);
    return entry == null ? null : entry.getForm(extension);
  }

  @Override
  public boolean notEmpty() {
    return !entries.isEmpty();
  }

  @Override
  public String toString() {
    return getPath();
  }
}
