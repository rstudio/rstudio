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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.i18n.client.PluralRule.PluralForm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
   * Exception indicating a require resource was not found.
   */
  public static class MissingResourceException extends RuntimeException {
    private String key;
    private String method;
    private List<AbstractResource> searchedResources;
    private String during;

    public MissingResourceException(String key,
        List<AbstractResource> searchedResources) {
      super("No resource found for key '" + key + "'");
      this.key = key;
      this.searchedResources = searchedResources;
    }

    public String getDuring() {
      return during;
    }

    public String getKey() {
      return key;
    }

    public String getMethod() {
      return method;
    }

    public List<AbstractResource> getSearchedResources() {
      return searchedResources;
    }

    public void setDuring(String during) {
      this.during = during;
    }

    public void setMethod(String method) {
      this.method = method;
    }
  }

  /**
   * Error messages concerning missing keys should include the defined keys if
   * the number of keys is below this threshold.
   */
  public static final int REPORT_KEYS_THRESHOLD = 30;

  protected static String getExtendedKey(String key, String extension) {
    if (extension != null) {
      key += '[' + extension + ']';
    }
    return key;
  }

  private final List<AbstractResource> alternativeParents = new ArrayList<AbstractResource>();

  private Set<String> keySet;

  private Map<String, PluralForm[]> pluralFormMap = new HashMap<String, PluralForm[]>();

  private String localeName;

  private String path;

  private AbstractResource primaryParent;

  /**
   * Walk up the resource inheritance tree until we find one which is an
   * instance of AnnotationsResource.
   * 
   * This is needed so the original Annotations metadata can be used even in
   * inherited resources, such as from a properties file for a specific locale.
   * 
   * TODO(jat): really bad code smell -- the superclass should not know about a
   * particular implementation, but I couldn't think of a decent way around it.
   * Long term, this whole structure needs to be be redone to make use of
   * resources that fundamentally don't look like property files to be used
   * effectively. I don't think it is feasible to do that in time for 1.5, so I
   * am continuing with this hacky solution but we need to look at this after
   * 1.5 is done.
   */
  public AnnotationsResource getAnnotationsResource() {
    AbstractResource resource = this;
    while (resource != null && !(resource instanceof AnnotationsResource)) {
      resource = resource.primaryParent;
    }
    return (AnnotationsResource) resource;
  }

  public Collection<String> getExtensions(String key) {
    return new ArrayList<String>();
  }

  /**
   * @see java.util.ResourceBundle#getLocale()
   */
  public String getLocaleName() {
    return localeName;
  }

  /**
   * @see java.util.ResourceBundle#getObject(java.lang.String)
   */
  public final Object getObject(String key) {
    Object s = getObjectAux(key, true, true);
    return s;
  }

  public PluralForm[] getPluralForms(String key) {
    return pluralFormMap.get(key);
  }

  /**
   * Get a string (with optional extension) and fail if not present.
   * 
   * @param logger
   * @param key
   * @param extension
   * @return the requested string
   */
  public final String getRequiredStringExt(TreeLogger logger, String key,
      String extension) {
    return extension == null ? (String) getObjectAux(key, true, true) : null;
  }

  /**
   * @see java.util.ResourceBundle#getString(java.lang.String)
   */
  public final String getString(String key) {
    return (String) getObjectAux(key, true);
  }

  /**
   * Get a key with an extension. Identical to getString() if extension is null.
   * 
   * @param key to lookup
   * @param extension extension of the key, nullable
   * @return string or null
   */
  public String getStringExt(String key, String extension) {
    return extension == null ? getString(key) : null;
  }

  /**
   * Keys associated with this resource.
   * 
   * @return keys
   */
  public Set<String> keySet() {
    if (keySet == null) {
      keySet = new HashSet<String>();
      addToKeySet(keySet);
      if (primaryParent != null) {
        primaryParent.addToKeySet(keySet);
      }
      for (int i = 0; i < alternativeParents.size(); i++) {
        AbstractResource element = alternativeParents.get(i);
        keySet.addAll(element.keySet());
      }
    }
    return keySet;
  }

  public void setPluralForms(String key, PluralForm[] pluralForms) {
    pluralFormMap.put(key, pluralForms);
  }

  @Override
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

  abstract void addToKeySet(Set<String> s);

  void checkKeys() {
    // If I don't have a parent, then I am a default node so do not need to
    // conform
    if (primaryParent == null) {
      return;
    }
    for (String key : keySet()) {
      if (primaryParent.getObjectAux(key, true) == null) {
        for (int i = 0; i < alternativeParents.size(); i++) {
          AbstractResource alt = alternativeParents.get(i);
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
    try {
      return getObjectAux(key, useAlternativeParents, false);
    } catch (MissingResourceException e) {
      // Can't happen since we pass required=false
      throw new RuntimeException("Unexpected MissingResourceException", e);
    }
  }

  final Object getObjectAux(String key, boolean useAlternativeParents,
      boolean required) throws MissingResourceException {
    ArrayList<AbstractResource> searched = new ArrayList<AbstractResource>();
    searched.add(this);
    Object s = handleGetObject(key);
    if (s != null) {
      return s;
    }
    AbstractResource parent = this.getPrimaryParent();
    if (parent != null) {
      // Primary parents should not look at their alternative parents
      searched.add(parent);
      s = parent.getObjectAux(key, false);
    }
    if ((s == null) && (alternativeParents.size() > 0)
        && (useAlternativeParents)) {
      for (int i = 0; (i < alternativeParents.size()) && (s == null); i++) {
        // Alternate parents may look at their alternative parents.
        AbstractResource altParent = alternativeParents.get(i);
        searched.add(altParent);
        s = altParent.getObjectAux(key, true);
      }
    }
    if (s == null && required) {
      throw new MissingResourceException(key, searched);
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

  void setLocaleName(String locale) {
    this.localeName = locale;
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
      AbstractResource element = alternativeParents.get(i);
      element.toVerboseStringAux(indent + 1, buf);
    }
  }
}
