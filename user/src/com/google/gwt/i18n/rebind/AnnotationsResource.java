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

import static com.google.gwt.i18n.rebind.AnnotationUtil.getClassAnnotation;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.i18n.client.Constants.DefaultBooleanValue;
import com.google.gwt.i18n.client.Constants.DefaultDoubleValue;
import com.google.gwt.i18n.client.Constants.DefaultFloatValue;
import com.google.gwt.i18n.client.Constants.DefaultIntValue;
import com.google.gwt.i18n.client.Constants.DefaultStringArrayValue;
import com.google.gwt.i18n.client.Constants.DefaultStringMapValue;
import com.google.gwt.i18n.client.Constants.DefaultStringValue;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.i18n.client.LocalizableResource.Description;
import com.google.gwt.i18n.client.LocalizableResource.GenerateKeys;
import com.google.gwt.i18n.client.LocalizableResource.Key;
import com.google.gwt.i18n.client.LocalizableResource.Meaning;
import com.google.gwt.i18n.client.Messages.AlternateMessage;
import com.google.gwt.i18n.client.Messages.DefaultMessage;
import com.google.gwt.i18n.client.Messages.Example;
import com.google.gwt.i18n.client.Messages.Optional;
import com.google.gwt.i18n.client.Messages.PluralCount;
import com.google.gwt.i18n.client.Messages.Select;
import com.google.gwt.i18n.client.PluralRule;
import com.google.gwt.i18n.server.KeyGenerator;
import com.google.gwt.i18n.server.Message;
import com.google.gwt.i18n.server.MessageInterface;
import com.google.gwt.i18n.server.MessageUtils;
import com.google.gwt.i18n.server.MessageUtils.KeyGeneratorException;
import com.google.gwt.i18n.shared.GwtLocale;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * AbstractResource implementation which looks up text annotations on classes.
 */
@SuppressWarnings("deprecation")
public class AnnotationsResource extends AbstractResource {

  /**
   * An exception indicating there was some problem with an annotation.
   * 
   * A caller receiving this exception should log the human-readable message and
   * treat it as a fatal error.
   */
  public static class AnnotationsError extends Exception {

    public AnnotationsError(String msg) {
      super(msg);
    }
  }

  /**
   * Class for argument information, used for export.
   */
  public static class ArgumentInfo {

    public String example;
    public boolean isPluralCount;
    public String name;
    public boolean optional;
    public Class<? extends PluralRule> pluralRuleClass;
    public boolean isSelect;

    public ArgumentInfo(String name) {
      this.name = name;
    }
  }

  private static class EntryWrapper implements ResourceEntry {

    private final String key;
    private final MethodEntry entry;

    public EntryWrapper(String key, MethodEntry entry) {
      this.key = key;
      this.entry = entry;
    }

    public String getForm(String form) {
      return form == null ? entry.text : entry.altText.get(form);
    }

    public Collection<String> getForms() {
      return entry.altText.keySet();
    }

    public String getKey() {
      return key;
    }
  }

  /**
   * Class to keep annotation information about a particular method.
   */
  private static class MethodEntry {

    // Strings used in toString for formatting.
    private static final String DETAILS_PREFIX = " (";
    private static final String DETAILS_SEPARATOR = ", ";

    public ArrayList<ArgumentInfo> arguments;
    public String description;
    public String meaning;
    public Map<String, String> altText;
    public String text;

    public MethodEntry(String text, String meaning) {
      this.text = text;
      this.meaning = meaning;
      altText = new HashMap<String, String>();
      arguments = new ArrayList<ArgumentInfo>();
    }

    public void addAlternateText(String altForm, String altMessage) {
      altText.put(altForm, altMessage);
    }

    public ArgumentInfo addArgument(String argName) {
      ArgumentInfo argInfo = new ArgumentInfo(argName);
      arguments.add(argInfo);
      return argInfo;
    }

    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder();
      buf.append(text);
      String prefix = DETAILS_PREFIX;
      if (meaning != null) {
        buf.append(prefix).append("meaning=").append(meaning);
        prefix = DETAILS_SEPARATOR;
      }
      if (description != null) {
        buf.append(prefix).append("desc=").append(description);
        prefix = DETAILS_SEPARATOR;
      }
      if (DETAILS_SEPARATOR == prefix) {
        buf.append(')');
      }
      return buf.toString();
    }
  }

  /**
   * Returns the key for a given method.
   *
   * If null is returned, an error message has already been logged.
   *
   * @param logger 
   * @param keyGenerator 
   * @param method 
   * @param isConstants 
   * @return null if unable to get or compute the key for this method, otherwise
   *         the key is returned
   */
  public static String getKey(TreeLogger logger, KeyGenerator keyGenerator,
      JMethod method, boolean isConstants) {
    Key key = method.getAnnotation(Key.class);
    if (key != null) {
      return key.value();
    }
    String text;
    try {
      text = getTextString(method, null, isConstants);
    } catch (AnnotationsError e) {
      return null;
    }
    if (keyGenerator == null) {
      // Gracefully handle the case of an invalid KeyGenerator classname
      return null;
    }
    MessageInterface msgIntf = new KeyGenMessageInterface(
        method.getEnclosingType());
    Message msg = new KeyGenMessage(method);
    String keyStr = keyGenerator.generateKey(msg);
    if (keyStr == null) {
      if (text == null) {
        logger.log(
            TreeLogger.ERROR,
            "Key generator "
                + keyGenerator.getClass().getName()
                + " requires the default value be specified in an annotation for method "
                + method.getName(), null);
      } else {
        logger.log(TreeLogger.ERROR, "Key generator "
            + keyGenerator.getClass().getName()
            + " was unable to compute a key value for method "
            + method.getName(), null);
      }
    }
    return keyStr;
  }

  /**
   * Returns a suitable key generator for the specified class.
   * 
   * @param targetClass 
   * @return KeyGenerator instance, guaranteed to not be null 
   * @throws AnnotationsError if a specified KeyGenerator cannot be created
   */
  public static KeyGenerator getKeyGenerator(JClassType targetClass)
      throws AnnotationsError {
    GenerateKeys generator = getClassAnnotation(targetClass, GenerateKeys.class);
    try {
      return MessageUtils.getKeyGenerator(generator);
    } catch (KeyGeneratorException e) {
      throw new AnnotationsError(e.getMessage());
    }
  }

  /**
   * Return the text string from annotations for a particular method.
   * 
   * @param method the method to retrieve text
   * @param map if not null, add keys for DefaultStringMapValue to this map
   * @param isConstants true if the method is in a subinterface of Constants
   * @throws AnnotationsError if the annotation usage is incorrect
   * @return the text value to use for this method, as if read from a properties
   *         file, or null if there are no annotations.
   */
  private static String getTextString(JMethod method,
      Map<String, MethodEntry> map, boolean isConstants)
      throws AnnotationsError {
    JType returnType = method.getReturnType();
    DefaultMessage defaultText = method.getAnnotation(DefaultMessage.class);
    DefaultStringValue stringValue = method.getAnnotation(DefaultStringValue.class);
    DefaultStringArrayValue stringArrayValue = method.getAnnotation(DefaultStringArrayValue.class);
    DefaultStringMapValue stringMapValue = method.getAnnotation(DefaultStringMapValue.class);
    DefaultIntValue intValue = method.getAnnotation(DefaultIntValue.class);
    DefaultFloatValue floatValue = method.getAnnotation(DefaultFloatValue.class);
    DefaultDoubleValue doubleValue = method.getAnnotation(DefaultDoubleValue.class);
    DefaultBooleanValue booleanValue = method.getAnnotation(DefaultBooleanValue.class);
    int constantsCount = 0;
    if (stringValue != null) {
      constantsCount++;
      if (!returnType.getQualifiedSourceName().equals("java.lang.String")) {
        throw new AnnotationsError(
            "@DefaultStringValue can only be used with a method returning String");
      }
    }
    if (stringArrayValue != null) {
      constantsCount++;
      JArrayType arrayType = returnType.isArray();
      if (arrayType == null
          || !arrayType.getComponentType().getQualifiedSourceName().equals(
              "java.lang.String")) {
        throw new AnnotationsError(
            "@DefaultStringArrayValue can only be used with a method returning String[]");
      }
    }
    if (stringMapValue != null) {
      constantsCount++;
      JRawType rawType = returnType.getErasedType().isRawType();
      boolean error = false;
      if (rawType == null
          || !rawType.getQualifiedSourceName().equals("java.util.Map")) {
        error = true;
      } else {
        JParameterizedType paramType = returnType.isParameterized();
        if (paramType != null) {
          JType[] args = paramType.getTypeArgs();
          if (args.length != 2
              || !args[0].getQualifiedSourceName().equals("java.lang.String")
              || !args[1].getQualifiedSourceName().equals("java.lang.String")) {
            error = true;
          }
        }
      }
      if (error) {
        throw new AnnotationsError(
            "@DefaultStringMapValue can only be used with a method "
                + "returning Map or Map<String,String>");
      }
    }
    if (intValue != null) {
      constantsCount++;
      JPrimitiveType primType = returnType.isPrimitive();
      if (primType != JPrimitiveType.INT) {
        throw new AnnotationsError(
            "@DefaultIntValue can only be used with a method returning int");
      }
    }
    if (floatValue != null) {
      constantsCount++;
      JPrimitiveType primType = returnType.isPrimitive();
      if (primType != JPrimitiveType.FLOAT) {
        throw new AnnotationsError(
            "@DefaultFloatValue can only be used with a method returning float");
      }
    }
    if (doubleValue != null) {
      constantsCount++;
      JPrimitiveType primType = returnType.isPrimitive();
      if (primType != JPrimitiveType.DOUBLE) {
        throw new AnnotationsError(
            "@DefaultDoubleValue can only be used with a method returning double");
      }
    }
    if (booleanValue != null) {
      constantsCount++;
      JPrimitiveType primType = returnType.isPrimitive();
      if (primType != JPrimitiveType.BOOLEAN) {
        throw new AnnotationsError(
            "@DefaultBooleanValue can only be used with a method returning boolean");
      }
    }
    if (!isConstants) {
      if (constantsCount > 0) {
        throw new AnnotationsError(
            "@Default*Value is not permitted on a Messages interface; see @DefaultText");
      }
      if (defaultText != null) {
        return defaultText.value();
      }
    } else {
      if (defaultText != null) {
        throw new AnnotationsError(
            "@DefaultText is not permitted on a Constants interface; see @Default*Value");
      }
      if (constantsCount > 1) {
        throw new AnnotationsError(
            "No more than one @Default*Value annotation may be used on a method");
      }
      if (stringValue != null) {
        return stringValue.value();
      } else if (intValue != null) {
        return Integer.toString(intValue.value());
      } else if (floatValue != null) {
        return Float.toString(floatValue.value());
      } else if (doubleValue != null) {
        return Double.toString(doubleValue.value());
      } else if (booleanValue != null) {
        return Boolean.toString(booleanValue.value());
      } else if (stringArrayValue != null) {
        StringBuilder buf = new StringBuilder();
        boolean firstString = true;
        for (String str : stringArrayValue.value()) {
          str = str.replace("\\", "\\\\");
          str = str.replace(",", "\\,");
          if (!firstString) {
            buf.append(',');
          } else {
            firstString = false;
          }
          buf.append(str);
        }
        return buf.toString();
      } else if (stringMapValue != null) {
        StringBuilder buf = new StringBuilder();
        boolean firstString = true;
        String[] entries = stringMapValue.value();
        if ((entries.length & 1) != 0) {
          throw new AnnotationsError(
              "Odd number of strings supplied to @DefaultStringMapValue");
        }
        for (int i = 0; i < entries.length; i += 2) {
          String key = entries[i];
          String value = entries[i + 1];

          if (map != null) {
            // add key=value part to map
            MethodEntry entry = new MethodEntry(value, null);
            map.put(key, entry);
          }

          // add the key to the master entry
          key = key.replace("\\", "\\\\");
          key = key.replace(",", "\\,");
          if (!firstString) {
            buf.append(',');
          } else {
            firstString = false;
          }
          buf.append(key);
        }
        return buf.toString();
      }
    }
    return null;
  }

  private Map<String, MethodEntry> map;

  /**
   * Create a resource that supplies data from i18n-related annotations.
   * 
   * @param logger
   * @param clazz
   * @param locale
   * @param isConstants
   * @throws AnnotationsError if there is a fatal error while processing
   *           annotations
   */
  public AnnotationsResource(TreeLogger logger, JClassType clazz,
      GwtLocale locale, boolean isConstants) throws AnnotationsError {
    super(locale);
    KeyGenerator keyGenerator = getKeyGenerator(clazz);
    map = new HashMap<String, MethodEntry>();
    setPath(clazz.getQualifiedSourceName());
    String defLocaleValue = null;
    
    // If the class has an embedded locale in it, use that for the default
    String className = clazz.getSimpleSourceName();
    int underscore = className.indexOf('_');
    if (underscore >= 0) {
      defLocaleValue = className.substring(underscore + 1);
    }
    
    // If there is an annotation declaring the default locale, use that
    DefaultLocale defLocaleAnnot = getClassAnnotation(clazz,
        DefaultLocale.class);
    if (defLocaleAnnot != null) {
      defLocaleValue = defLocaleAnnot.value();
    }
    GwtLocale defLocale = LocaleUtils.getLocaleFactory().fromString(
        defLocaleValue);
    if (!locale.isDefault() && !locale.equals(defLocale)) {
      logger.log(TreeLogger.WARN, "Default locale " + defLocale + " on "
          + clazz.getQualifiedSourceName() + " doesn't match " + locale);
      return;
    }
    matchLocale = defLocale;
    for (JMethod method : clazz.getMethods()) {
      String meaningString = null;
      Meaning meaning = method.getAnnotation(Meaning.class);
      if (meaning != null) {
        meaningString = meaning.value();
      }
      String textString = getTextString(method, map, isConstants);
      if (textString == null) {
        // ignore ones without some value annotation
        continue;
      }
      String key = null;
      Key keyAnnot = method.getAnnotation(Key.class);
      if (keyAnnot != null) {
        key = keyAnnot.value();
      } else {
        Message msg = new KeyGenMessage(method);
        key = keyGenerator.generateKey(msg);
        if (key == null) {
          throw new AnnotationsError("Could not compute key for "
              + method.getEnclosingType().getQualifiedSourceName() + "."
              + method.getName() + " using " + keyGenerator);
        }
      }
      MethodEntry entry = new MethodEntry(textString, meaningString);
      map.put(key, entry);
      Description description = method.getAnnotation(Description.class);
      if (description != null) {
        entry.description = description.value();
      }
      // use full name to avoid deprecation warnings in the imports
      com.google.gwt.i18n.client.Messages.PluralText pluralText = method
          .getAnnotation(com.google.gwt.i18n.client.Messages.PluralText.class);
      if (pluralText != null) {
        String[] pluralForms = pluralText.value();
        if ((pluralForms.length & 1) != 0) {
          throw new AnnotationsError(
              "Odd number of strings supplied to @PluralText: must be"
              + " pairs of form names and messages");
        }
        for (int i = 0; i + 1 < pluralForms.length; i += 2) {
          entry.addAlternateText(pluralForms[i], pluralForms[i + 1]);
        }
      }
      AlternateMessage altMsg = method.getAnnotation(AlternateMessage.class);
      if (altMsg != null) {
        if (pluralText != null) {
          throw new AnnotationsError("May not have both @AlternateMessage"
              + " and @PluralText");
        }
        String[] altForms = altMsg.value();
        if ((altForms.length & 1) != 0) {
          throw new AnnotationsError(
              "Odd number of strings supplied to @AlternateMessage: must be"
              + " pairs of values and messages");
        }
        for (int i = 0; i + 1 < altForms.length; i += 2) {
          entry.addAlternateText(altForms[i], altForms[i + 1]);
        }
      }
      for (JParameter param : method.getParameters()) {
        ArgumentInfo argInfo = entry.addArgument(param.getName());
        Optional optional = param.getAnnotation(Optional.class);
        if (optional != null) {
          argInfo.optional = true;
        }
        PluralCount pluralCount = param.getAnnotation(PluralCount.class);
        if (pluralCount != null) {
          argInfo.isPluralCount = true;
        }
        Example example = param.getAnnotation(Example.class);
        if (example != null) {
          argInfo.example = example.value();
        }
        Select select = param.getAnnotation(Select.class);
        if (select != null) {
          argInfo.isSelect = true;
        }
      }
    }
  }

  @Override
  public void addToKeySet(Set<String> s) {
    s.addAll(map.keySet());
  }

  public Iterable<ArgumentInfo> argumentsIterator(String key) {
    MethodEntry entry = map.get(key);
    return entry != null ? entry.arguments : null;
  }

  public String getDescription(String key) {
    MethodEntry entry = map.get(key);
    return entry == null ? null : entry.description;
  }

  @Override
  public ResourceEntry getEntry(String key) {
    MethodEntry entry = map.get(key);
    return entry == null ? null : new EntryWrapper(key, entry);
  }

  @Override
  public Collection<String> getExtensions(String key) {
    MethodEntry entry = map.get(key);
    return entry == null ? new ArrayList<String>() : entry.altText.keySet();
  }

  public String getMeaning(String key) {
    MethodEntry entry = map.get(key);
    return entry == null ? null : entry.meaning;
  }

  @Override
  public String getStringExt(String key, String extension) {
    MethodEntry entry = map.get(key);
    if (entry == null) {
      return null;
    }
    if (extension != null) {
      return entry.altText.get(extension);
    } else {
      return entry.text;
    }
  }

  @Override
  public boolean notEmpty() {
    return !map.isEmpty();
  }

  @Override
  public String toString() {
    return "Annotations from class " + getPath() + " @" + getMatchLocale();
  }
}
