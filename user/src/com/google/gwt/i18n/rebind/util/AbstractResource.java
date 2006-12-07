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
package com.google.gwt.i18n.rebind.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;

/**
 * AbstractResource serves the same purpose as java
 * ResourceBundle/PropertyResourceBundle.
 * <p>
 * Each <code>Resource</code> belongs to a resource tree, indicated by the
 * path attribute.
 * <p>
 * AbstractResource uses a Factory pattern rather than a single static method to
 * load itself given an abstract string path.
 * <p>
 * One advanced feature which should not be used outside the core GWT system is
 * that resources can have more than one parent, for instance pets_en_US could
 * have pets_en as one parent and animals_en_US as another. The alternative
 * parents have lower precedence than any primary parent. Each alternative
 * parent is associated with a separate resource tree.
 */
public abstract class AbstractResource {
  /**
   * Error messages concerning missing keys should include the defined keys if
   * the number of keys is below this threshold.
   */
  public static final int REPORT_KEYS_THRESHOLD = 30;

  private final List alternativeParents = new ArrayList();

  private Set keySet;

  private Locale locale;

  private String path;

  private AbstractResource primaryParent;

  /**
   * @see java.util.ResourceBundle#getLocale()
   */
  public Locale getLocale() {
    return locale;
  }

  /**
   * @see java.util.ResourceBundle#getObject(java.lang.String)
   */
  public final Object getObject(String key) {
    Object s = getObjectAux(key, true);
    if (s == null) {
      String msg = "Cannot find '" + key + "' in " + this;
      Set keys = this.keySet();
      if (keys.size() < REPORT_KEYS_THRESHOLD) {
        msg = msg + ", keys found:\n\t" + keys;
      }
      throw new MissingResourceException(msg, key, key);
    }
    return s;
  }

  /**
   * @see java.util.ResourceBundle#getString(java.lang.String)
   */
  public final String getString(String key) {
    return (String) getObject(key);
  }

  /**
   * Keys associated with this resource.
   * 
   * @return keys
   */
  public Set keySet() {
    if (keySet == null) {
      keySet = new HashSet();
      addToKeySet(keySet);
      if (primaryParent != null) {
        primaryParent.addToKeySet(keySet);
      }
      for (int i = 0; i < alternativeParents.size(); i++) {
        AbstractResource element = (AbstractResource) alternativeParents.get(i);
        keySet.addAll(element.keySet());
      }
    }
    return keySet;
  }

  public String toString() {
    return "resource for " + path;
  }

  /**
   * A multi-line representation of this object.
   * 
   * @return verbose string
   */
  public String toVerboseString() {
    StringBuffer b = new StringBuffer();
    toVerboseStringAux(0, b);
    return b.toString();
  }

  void addAlternativeParent(AbstractResource parent) {
    if (parent != null) {
      alternativeParents.add(parent);
    }
  }

  abstract void addToKeySet(Set s);

  void checkKeys() {
    // If I don't have a parent, then I am a default node so do not need to
    // conform
    if (primaryParent == null) {
      return;
    }
    Iterator keys = this.keySet().iterator();
    while (keys.hasNext()) {
      String key = (String) keys.next();
      if (primaryParent.getObjectAux(key, true) == null) {
        for (int i = 0; i < alternativeParents.size(); i++) {
          AbstractResource alt = (AbstractResource) alternativeParents.get(i);
          if (alt.getObjectAux(key, true) != null) {
            break;
          }
        }

        throw new IllegalArgumentException(
            key
                + " is not a valid resource key as it does not occur in the default version of "
                + this + " nor in any of " + alternativeParents);
      }
    }
  }

  final Object getObjectAux(String key, boolean useAlternativeParents) {
    Object s = handleGetObject(key);
    if (s != null) {
      return s;
    }
    AbstractResource parent = this.getPrimaryParent();
    if (parent != null) {
      // Primary parents should not look at their alternative parents
      s = parent.getObjectAux(key, false);
    }
    if ((s == null) && (alternativeParents.size() > 0)
        && (useAlternativeParents)) {
      for (int i = 0; (i < alternativeParents.size()) && (s == null); i++) {
        // Alternate parents may look at their alternative parents.
        AbstractResource altParent = (AbstractResource) alternativeParents.get(i);
        s = altParent.getObjectAux(key, true);
      }
    }
    return s;
  }

  String getPath() {
    return path;
  }

  AbstractResource getPrimaryParent() {
    return primaryParent;
  }

  abstract Object handleGetObject(String key);

  void setLocale(Locale locale) {
    this.locale = locale;
  }

  void setPath(String path) {
    this.path = path;
  }

  void setPrimaryParent(AbstractResource primaryParent) {
    if (primaryParent == null) {
      return;
    }
    this.primaryParent = primaryParent;
  }

  private void newLine(int indent, StringBuffer buf) {
    buf.append("\n");
    for (int i = 0; i < indent; i++) {
      buf.append("\t");
    }
  }

  private void toVerboseStringAux(int indent, StringBuffer buf) {
    newLine(indent, buf);
    buf.append(toString());
    if (primaryParent != null) {
      newLine(indent, buf);
      buf.append("Primary Parent: ");
      primaryParent.toVerboseStringAux(indent + 1, buf);
    }
    for (int i = 0; i < alternativeParents.size(); i++) {
      newLine(indent, buf);
      buf.append("Alternate Parent: ");
      AbstractResource element = (AbstractResource) alternativeParents.get(i);
      element.toVerboseStringAux(indent + 1, buf);
    }
  }
}
