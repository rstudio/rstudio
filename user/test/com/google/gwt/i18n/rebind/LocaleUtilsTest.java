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
package com.google.gwt.i18n.rebind;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.DefaultConfigurationProperty;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.shell.FailErrorLogger;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Test for {@link LocaleUtils}.
 */
public class LocaleUtilsTest extends TestCase {

  /**
   * Mock config property that gets the values in the constructor.
   */
  public class MockConfigProperty extends DefaultConfigurationProperty {

    /**
     * @param name 
     * @param values 
     */
    public MockConfigProperty(String name, String... values) {
      super(name, Arrays.asList(values));
    }
  }

  private static class MockPropertyOracle implements PropertyOracle {

    private final Map<String, ConfigurationProperty> configProperties;
    private final Map<String, SelectionProperty> selectionProperties;
    
    /**
     * 
     */
    public MockPropertyOracle() {
      configProperties = new TreeMap<String, ConfigurationProperty>();
      selectionProperties = new TreeMap<String, SelectionProperty>();
    }

    public ConfigurationProperty getConfigurationProperty(String propertyName)
        throws BadPropertyValueException {
      ConfigurationProperty prop = configProperties.get(propertyName);
      if (prop == null) {
        throw new BadPropertyValueException(propertyName);
      }
      return prop;
    }

    @Deprecated
    public String getPropertyValue(TreeLogger logger, String propertyName)
        throws BadPropertyValueException {
     SelectionProperty prop = getSelectionProperty(logger, propertyName);
     return prop.getCurrentValue();
    }

    @Deprecated
    public String[] getPropertyValueSet(TreeLogger logger, String propertyName)
        throws BadPropertyValueException {
      SelectionProperty prop = getSelectionProperty(logger, propertyName);
      SortedSet<String> possibleValues = prop.getPossibleValues();
      return possibleValues.toArray(new String[possibleValues.size()]);
    }

    public SelectionProperty getSelectionProperty(TreeLogger logger, String propertyName)
        throws BadPropertyValueException {
      SelectionProperty prop = selectionProperties.get(propertyName);
      if (prop == null) {
        throw new BadPropertyValueException(propertyName);
      }
      return prop;
    }

    public void setProperty(ConfigurationProperty prop) {
      configProperties.put(prop.getName(), prop);
    }

    public void setProperty(SelectionProperty prop) {
      selectionProperties.put(prop.getName(), prop);
    }    
  }

  /**
   * Mock selection property.
   */
  private static class MockSelectionProperty implements SelectionProperty {

    private String fallbackValue;
    private final String name;
    private final SortedSet<String> possibleValues;
    private String value;

    /**
     * Initialize mock selection property. 
     */
    public MockSelectionProperty(String name, String... possibleValues) {
      this.name = name;
      this.possibleValues = new TreeSet<String>();
      this.possibleValues.addAll(Arrays.asList(possibleValues));
      if (possibleValues.length > 0) {
        value = possibleValues[0];
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      MockSelectionProperty other = (MockSelectionProperty) obj;
      if (fallbackValue == null) {
        if (other.fallbackValue != null) {
          return false;
        }
      } else if (!fallbackValue.equals(other.fallbackValue)) {
        return false;
      }
      if (!name.equals(other.name) || !possibleValues.equals(other.possibleValues)) {
        return false;
      }
      if (value == null) {
        if (other.value != null) {
          return false;
        }
      } else if (!value.equals(other.value)) {
        return false;
      }
      return true;
    }

    public String getCurrentValue() {
      return value;
    }

    public String getFallbackValue() {
      return fallbackValue;
    }

    public List<? extends Set<String>> getFallbackValues(String value) {
      return Collections.emptyList();
    }

    public String getName() {
      return name;
    }

    public SortedSet<String> getPossibleValues() {
      return possibleValues;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((fallbackValue == null) ? 0 : fallbackValue.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((possibleValues == null) ? 0 : possibleValues.hashCode());
      result = prime * result + ((value == null) ? 0 : value.hashCode());
      return result;
    }

    /**
     * @param value the value to set
     */
    public void setCurrentValue(String value) {
      this.value = value;
    }
  }

  /**
   * @param set
   * @param key
   */
  private static <T> void assertContains(Set<T> set, T key) {
    assertTrue(set + " should have contained " + key, set.contains(key));
  }
  private GeneratorContext ctx;
  private MockConfigProperty localeCookieParam;
  private MockSelectionProperty localeProp;
  private MockConfigProperty localeQueryParam;
  private TreeLogger logger = new FailErrorLogger();
  private PropertyOracle props;

  private MockConfigProperty rtLocaleProp;

  /**
   * Initialize mocks for tests.
   */
  public LocaleUtilsTest() {
    MockPropertyOracle mock = new MockPropertyOracle();
    localeProp = new MockSelectionProperty(LocaleUtils.PROP_LOCALE, "es_419", "es", "en");
    mock.setProperty(localeProp);
    localeQueryParam = new MockConfigProperty(LocaleUtils.PROP_LOCALE_QUERY_PARAM, "query-param");
    mock.setProperty(localeQueryParam);
    localeCookieParam = new MockConfigProperty(LocaleUtils.PROP_LOCALE_COOKIE, "cookie-name");
    mock.setProperty(localeCookieParam);
    rtLocaleProp = new MockConfigProperty(LocaleUtils.PROP_RUNTIME_LOCALES, "es_AR", "es_CO",
        "es_ES", "es_GB", "en_Dsrt");
    mock.setProperty(rtLocaleProp);
    props = mock;
  }

  /**
   * Test method for {@link com.google.gwt.i18n.rebind.LocaleUtils#getAllCompileLocales()}.
   */
  public void testGetAllCompileLocales() {
    LocaleUtils localeUtils = LocaleUtils.getInstance(logger, props, ctx);
    GwtLocaleFactory factory = LocaleUtils.getLocaleFactory();
    Set<GwtLocale> locales = localeUtils.getAllCompileLocales();
    assertEquals(3, locales.size());
    assertContains(locales, factory.fromString("es_419"));
    assertContains(locales, factory.fromString("es"));
    assertContains(locales, factory.fromString("en"));
  }

  /**
   * Test method for {@link com.google.gwt.i18n.rebind.LocaleUtils#getAllLocales()}.
   */
  public void testGetAllLocales() {
    LocaleUtils localeUtils = LocaleUtils.getInstance(logger, props, ctx);
    GwtLocaleFactory factory = LocaleUtils.getLocaleFactory();
    Set<GwtLocale> locales = localeUtils.getAllLocales();
    assertEquals(7, locales.size());
    assertContains(locales, factory.fromString("es_419"));
    assertContains(locales, factory.fromString("es"));
    assertContains(locales, factory.fromString("en"));
    assertContains(locales, factory.fromString("es_AR"));
    assertContains(locales, factory.fromString("es_CO"));
    assertContains(locales, factory.fromString("es_ES"));
    assertContains(locales, factory.fromString("es_GB"));
  }

  /**
   * Test method for {@link com.google.gwt.i18n.rebind.LocaleUtils#getCompileLocale()}.
   */
  public void testGetCompileLocale() {
    LocaleUtils localeUtils = LocaleUtils.getInstance(logger, props, ctx);
    assertEquals("es_419", localeUtils.getCompileLocale().toString());
  }

  /**
   * Test method for {@link com.google.gwt.i18n.rebind.LocaleUtils#getCookie()}.
   */
  public void testGetCookie() {
    LocaleUtils localeUtils = LocaleUtils.getInstance(logger, props, ctx);
    assertEquals("cookie-name", localeUtils.getCookie());
  }

  /**
   * Test caching of {@link LocaleUtils} instances.
   */
  public void testGetInstanceCaching() {
    LocaleUtils localeUtils = LocaleUtils.getInstance(logger, props, ctx);
    assertSame(localeUtils, LocaleUtils.getInstance(logger, props, ctx));
  }

  /**
   * Test method for {@link com.google.gwt.i18n.rebind.LocaleUtils#getQueryParam()}.
   */
  public void testGetQueryParam() {
    LocaleUtils localeUtils = LocaleUtils.getInstance(logger, props, ctx);
    assertEquals("query-param", localeUtils.getQueryParam());
  }

  /**
   * Test method for {@link com.google.gwt.i18n.rebind.LocaleUtils#getRuntimeLocales()}.
   */
  public void testGetRuntimeLocales() {
    LocaleUtils localeUtils = LocaleUtils.getInstance(logger, props, ctx);
    GwtLocaleFactory factory = LocaleUtils.getLocaleFactory();
    Set<GwtLocale> locales = localeUtils.getRuntimeLocales();
    assertEquals(2, locales.size());
    assertContains(locales, factory.fromString("es_AR"));
    assertContains(locales, factory.fromString("es_CO"));

    localeProp.setCurrentValue("es");
    LocaleUtils localeUtils2 = LocaleUtils.getInstance(logger, props, ctx);
    localeProp.setCurrentValue("es_419");
    assertNotSame(localeUtils, localeUtils2);

    // check that we don't pick up runtime locales that are under a more-specific compile locale
    locales = localeUtils2.getRuntimeLocales();
    assertEquals(2, locales.size());
    assertContains(locales, factory.fromString("es_ES"));
    assertContains(locales, factory.fromString("es_GB"));
  }
}
