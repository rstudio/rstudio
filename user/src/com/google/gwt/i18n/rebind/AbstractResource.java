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
import com.google.gwt.i18n.shared.GwtLocale;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
   * Exception indicating a required resource was not found.
   */
  public static class MissingResourceException extends RuntimeException {
    private String during;
    private String key;
    private String method;
    private List<AbstractResource> searchedResources;

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
   * Definition of a single entry for a resource.
   */
  public interface ResourceEntry {

    /**
     * Retrieve a particular form for this entry.
     *
     * @param form form to retrieve (null for the default)
     * @return null if the requested form is not present
     */
    String getForm(String form);

    /**
     * Returns a list of forms associated with this entry.
     *
     * The default form (also the only form for anything other than messages
     * with plural support) is always available and not present in this list.
     */
    Collection<String> getForms();

    /**
     * Returns key for this entry (must not be null).
     */
    String getKey();
  }

  /**
   * Encapsulates an ordered set of resources to search for translations.
   */
  public static class ResourceList extends AbstractList<AbstractResource>
      implements Set<AbstractResource> {

    private List<AbstractResource> list = new ArrayList<AbstractResource>();

    private Map<String, PluralForm[]> pluralForms = new HashMap<String, PluralForm[]>();

    private Set<AbstractResource> set = new HashSet<AbstractResource>();

    @Override
    public boolean add(AbstractResource element) {
      if (set.contains(element)) {
        return false;
      }
      set.add(element);
      return list.add(element);
    }

    @Override
    public void add(int index, AbstractResource element) {
      if (set.contains(element)) {
        throw new IllegalArgumentException("Duplicate element");
      }
      set.add(element);
      list.add(index, element);
    }

    /**
     * Add all keys known by this ResourceList to the specified set.
     *
     * @param s set to add keys to
     */
    public void addToKeySet(Set<String> s) {
      for (AbstractResource resource : list) {
        resource.addToKeySet(s);
      }
    }

    /**
     * From the list of locales matched for any resources in this resource list,
     * choose the one that is least derived from the original search locale.
     * @param logger logger to use
     * @param locale originally requested locale
     * @return least derived matched locale
     */
    public GwtLocale findLeastDerivedLocale(TreeLogger logger,
        GwtLocale locale) {
      List<GwtLocale> searchList = locale.getCompleteSearchList();
      Map<GwtLocale, Integer> derivedIndex = new HashMap<GwtLocale, Integer>();
      for (int i = 0; i < searchList.size(); ++i) {
        derivedIndex.put(searchList.get(i), i);
      }
      GwtLocale defaultLocale = LocaleUtils.getLocaleFactory().getDefault();
      GwtLocale best = defaultLocale;
      int bestIdx = Integer.MAX_VALUE;
      for (int i = 0; i < list.size(); ++i) {
        GwtLocale matchLocale = list.get(i).getMatchLocale();
        Integer wrappedIdx = derivedIndex.get(matchLocale);
        if (wrappedIdx == null) {
          // We had an @DefaultLocale for a locale not present in this
          // permutation -- treat it as the default locale.
          wrappedIdx = derivedIndex.get(defaultLocale);
          if (wrappedIdx == null) {
            // shouldn't happen
            assert false : "No default locale in search list";
            continue;
          }
        }
        int idx = wrappedIdx;
        if (idx < bestIdx) {
          bestIdx = idx;
          best = matchLocale;
        }
      }
      return best;
    }

    @Override
    public AbstractResource get(int index) {
      return list.get(index);
    }

    /**
     * Returns the first AnnotationsResource containing a specified key.
     *
     * @param logger
     * @param key
     * @return first AnnotationsResource containing key, or null if none
     */
    public AnnotationsResource getAnnotationsResource(TreeLogger logger,
        String key) {
      for (AbstractResource resource : list) {
        if (resource instanceof AnnotationsResource
            && resource.keySet.contains(key)) {
          return (AnnotationsResource) resource;
        }
      }
      return null;
    }

    /**
     * Get an entry from the first resource in this list containing a match.
     *
     * @param key
     * @return a ResourceEntry instance
     */
    public ResourceEntry getEntry(String key) {
      for (AbstractResource resource : list) {
        ResourceEntry e = resource.getEntry(key);
        if (e != null) {
          return e;
        }
      }
      return null;
    }

    /**
     * Returns the list of extensions available for a given key.
     *
     * @param key
     * @return collection of extensions for the given key
     */
    public Collection<String> getExtension(String key) {
      Set<String> extensions = new HashSet<String>();
      for (AbstractResource resource : list) {
        extensions.addAll(resource.getExtensions(key));
      }
      return extensions;
    }

    /**
     * Returns the list of plural forms for a given key.
     *
     * @param key
     * @return array of plural forms.
     */
    public PluralForm[] getPluralForms(String key) {
      return pluralForms.get(key);
    }

    /**
     * Returns a translation for a key, or throw an exception.
     *
     * @param key
     * @return translated string for key
     * @throws MissingResourceException
     */
    public String getRequiredString(String key)
        throws MissingResourceException {
      String val = getString(key);
      if (val == null) {
        throw new MissingResourceException(key, list);
      }
      return val;
    }

    /**
     * Returns a translation for a key/extension, or throw an exception.
     *
     * @param key
     * @param ext key extension, null if none
     * @return translated string for key
     * @throws MissingResourceException
     */
    public String getRequiredStringExt(String key, String ext)
        throws MissingResourceException {
      String val = getStringExt(key, ext);
      if (val == null) {
        throw new MissingResourceException(getExtendedKey(key, ext), list);
      }
      return val;
    }

    /**
     * Returns a translation for a key, or null if not found.
     *
     * @param key
     * @return translated string for key
     */
    public String getString(String key) {
      for (AbstractResource resource : list) {
        String s = resource.getStringExt(key, null);
        if (s != null) {
          return s;
        }
      }
      return null;
    }

    /**
     * Returns a translation for a key/extension, or null if not found.
     *
     * @param key
     * @param extension key extension, null if none
     * @return translated string for key
     */
    public String getStringExt(String key, String extension) {
      for (AbstractResource resource : list) {
        String s = resource.getStringExt(key, extension);
        if (s != null) {
          return s;
        }
      }
      return null;
    }

    @Override
    public int indexOf(Object o) {
      return list.indexOf(o);
    }

    @Override
    public Iterator<AbstractResource> iterator() {
      return list.iterator();
    }

    /**
     * Returns set of keys present across all resources.
     */
    public Set<String> keySet() {
      Set<String> keySet = new HashSet<String>();
      for (AbstractResource resource : list) {
        keySet.addAll(resource.keySet());
      }
      return keySet;
    }

    @Override
    public int lastIndexOf(Object o) {
      return list.lastIndexOf(o);
    }

    @Override
    public AbstractResource remove(int index) {
      AbstractResource element = list.remove(index);
      set.remove(element);
      return element;
    }

    /**
     * Set the plural forms associated with a given message.
     *
     * @param key
     * @param forms
     */
    public void setPluralForms(String key, PluralForm[] forms) {
      if (!pluralForms.containsKey(key)) {
        pluralForms.put(key, forms);
      }
    }

    @Override
    public int size() {
      return list.size();
    }
  }

  /**
   * Implementation of ResourceEntry that supports multiple forms per entry.
   */
  protected static class MultipleFormEntry implements ResourceEntry {

    private final String key;
    private final Map<String, String> values = new HashMap<String, String>();
    private final Set<String> forms = new HashSet<String>();

    public MultipleFormEntry(String key) {
      this.key = key;
    }

    public void addForm(String form, String value) {
      values.put(form, value);
      if (form != null) {
        forms.add(form);
      }
    }

    public String getForm(String form) {
      return values.get(form);
    }

    public Collection<String> getForms() {
      return forms;
    }

    public String getKey() {
      return key;
    }
  }

  /**
   * A simple resource entry with no alternate forms, only a key and a value.
   */
  protected static class SimpleEntry implements ResourceEntry {

    private final String key;
    private final String value;

    public SimpleEntry(String key, String value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      SimpleEntry other = (SimpleEntry) obj;
      return key.equals(other.key) && value.equals(other.value);
    }

    public String getForm(String form) {
      return form != null ? null : value;
    }

    public Collection<String> getForms() {
      return Collections.emptyList();
    }

    public String getKey() {
      return key;
    }

    @Override
    public int hashCode() {
      return key.hashCode() + 31 * value.hashCode();
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

  protected GwtLocale matchLocale;

  private Set<String> keySet;

  private String path;

  public AbstractResource(GwtLocale matchLocale) {
    this.matchLocale = matchLocale;
  }

  /**
   * Returns an entry in this resource.
   *
   * @param key
   * @return ResourceEntry instance
   */
  public ResourceEntry getEntry(String key) {
    String value = getString(key);
    return value == null ? null : new SimpleEntry(key, value);
  }

  /**
   * @param key
   */
  public Collection<String> getExtensions(String key) {
    return new ArrayList<String>();
  }

  /**
   * Get a string and fail if not present.
   *
   * @param key
   * @return the requested string
   */
  public final String getRequiredString(String key) {
    return getRequiredStringExt(key, null);
  }

  /**
   * Get a string (with optional extension) and fail if not present.
   *
   * @param key
   * @param extension
   * @return the requested string
   */
  public final String getRequiredStringExt(String key, String extension) {
    String s = getStringExt(key, extension);
    if (s == null) {
      ArrayList<AbstractResource> list = new ArrayList<AbstractResource>();
      list.add(this);
      throw new MissingResourceException(key, list);
    }
    return s;
  }

  /**
   * Get a key.
   *
   * @param key key to lookup
   * @return the string for the given key or null if not found
   * @see java.util.ResourceBundle#getString(java.lang.String)
   */
  public final String getString(String key) {
    return getStringExt(key, null);
  }

  /**
   * Get a key with an extension. Identical to getString() if extension is null.
   *
   * @param key to lookup
   * @param extension extension of the key, nullable
   * @return string or null
   */
  public abstract String getStringExt(String key, String extension);

  /**
   * Keys associated with this resource.
   *
   * @return keys
   */
  public Set<String> keySet() {
    if (keySet == null) {
      keySet = new HashSet<String>();
      addToKeySet(keySet);
    }
    return keySet;
  }

  /**
   * Returns true if this resource has any keys.
   */
  public boolean notEmpty() {
    return !keySet.isEmpty();
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

  abstract void addToKeySet(Set<String> s);

  GwtLocale getMatchLocale() {
    return matchLocale;
  }

  String getPath() {
    return path;
  }

  void setPath(String path) {
    this.path = path;
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
  }
}
