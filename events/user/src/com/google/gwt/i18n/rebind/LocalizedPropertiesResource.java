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

import org.apache.tapestry.util.text.LocalizedProperties;

import java.io.IOException;
import java.io.InputStream;
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
    public AbstractResource load(InputStream m) {
      LocalizedPropertiesResource bundle = new LocalizedPropertiesResource(m);
      return bundle;
    }
  }

  private LocalizedProperties props;

  public LocalizedPropertiesResource(InputStream m) {
    props = new LocalizedProperties();
    try {
      props.load(m, Util.DEFAULT_ENCODING);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load " + this.getPath(), e);
    }
  }

  @Override
  public void addToKeySet(Set<String> s) {
    for (Object keyObj : props.getPropertyMap().keySet()) {
      String key = (String) keyObj;
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
  public String getStringExt(String key, String extension) {
    if (extension != null) {
      String s = getStringExt(getExtendedKey(key, extension), null);
      if (s != null) {
        return s;
      }
    }
    return props.getProperty(key);
  }

  @Override
  public boolean notEmpty() {
    return props != null && !props.getPropertyMap().isEmpty();
  }

  @Override
  public String toString() {
    return getPath();
  }
}
