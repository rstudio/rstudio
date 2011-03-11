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

import com.google.gwt.i18n.client.Constants.DefaultBooleanValue;
import com.google.gwt.i18n.client.Constants.DefaultDoubleValue;
import com.google.gwt.i18n.client.Constants.DefaultFloatValue;
import com.google.gwt.i18n.client.Constants.DefaultIntValue;
import com.google.gwt.i18n.client.Constants.DefaultStringArrayValue;
import com.google.gwt.i18n.client.Constants.DefaultStringMapValue;
import com.google.gwt.i18n.client.Constants.DefaultStringValue;
import com.google.gwt.i18n.client.LocalizableResource.GenerateKeys;
import com.google.gwt.i18n.client.Messages.PluralCount;
import com.google.gwt.i18n.client.Messages.Select;
import com.google.gwt.i18n.server.keygen.MethodNameKeyGenerator;

/**
 * Utilities for processing GWT i18n messages.
 */
public class MessageUtils {

  /**
   * An exception signaling {@link #getKeyGenerator(GenerateKeys)} was unable
   * to process the annotation.
   */
  public static class KeyGeneratorException extends Exception {

    /**
     * @param message
     */
    public KeyGeneratorException(String message) {
      super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public KeyGeneratorException(String message, Throwable cause) {
      super(message, cause);
    } 
  }

  public static Class<?>[] SELECTOR_ANOTATIONS = new Class<?>[] {
      PluralCount.class, Select.class
  };

  public static String getConstantsDefaultValue(Message msg) {
    DefaultStringValue dsv = msg.getAnnotation(DefaultStringValue.class);
    if (dsv != null) {
      return dsv.value();
    }
    DefaultIntValue div = msg.getAnnotation(DefaultIntValue.class);
    if (div != null) {
      return String.valueOf(div.value());
    }
    DefaultBooleanValue dbv = msg.getAnnotation(DefaultBooleanValue.class);
    if (dbv != null) {
      return String.valueOf(dbv.value());
    }
    DefaultDoubleValue ddv = msg.getAnnotation(DefaultDoubleValue.class);
    if (ddv != null) {
      return String.valueOf(ddv.value());
    }
    DefaultFloatValue dfv = msg.getAnnotation(DefaultFloatValue.class);
    if (dfv != null) {
      return String.valueOf(dfv.value());
    }
    DefaultStringArrayValue dsav = msg.getAnnotation(
        DefaultStringArrayValue.class);
    if (dsav != null) {
      StringBuilder buf = new StringBuilder();
      boolean needComma = false;
      for (String value : dsav.value()) {
        if (needComma) {
          buf.append(',');
        } else {
          needComma = true;
        }
        buf.append(MessageUtils.quoteComma(value));
      }
      return buf.toString();
    }
    DefaultStringMapValue dsmv = msg.getAnnotation(DefaultStringMapValue.class);
    if (dsmv != null) {
      String[] values = dsmv.value();
      StringBuilder buf = new StringBuilder();
      boolean needComma = false;
      for (int i = 0; i < values.length; i += 2) {
        if (needComma) {
          buf.append(',');
        } else {
          needComma = true;
        }
        buf.append(MessageUtils.quoteComma(values[i]));
      }
      return buf.toString();
    }
    return null;
  }

  @SuppressWarnings("deprecation")
  public static KeyGenerator getKeyGenerator(GenerateKeys keyGenAnnot)
      throws KeyGeneratorException {
    if (keyGenAnnot == null) {
      return new MethodNameKeyGenerator();
    }
    String keyGenClassName = keyGenAnnot.value();
    Throwable caught = null;
    try {
      Class<?> clazz = Class.forName(keyGenClassName);
      if (KeyGenerator.class.isAssignableFrom(clazz)) {
        Class<? extends KeyGenerator> kgClass = clazz.asSubclass(
            KeyGenerator.class);
        return kgClass.newInstance();
      }
      if (com.google.gwt.i18n.rebind.keygen.KeyGenerator.class.isAssignableFrom(
          clazz)) {
        Class<? extends com.google.gwt.i18n.rebind.keygen.KeyGenerator> kgClass
            = clazz.asSubclass(com.google.gwt.i18n.rebind.keygen.KeyGenerator.class);
        return new KeyGeneratorAdapter(kgClass.newInstance());
      }
      throw new KeyGeneratorException(keyGenClassName
          + " in @GenerateKeys must implement KeyGenerator");
    } catch (ClassNotFoundException e) {
      caught = e;
    } catch (InstantiationException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    }
    throw new KeyGeneratorException("Unable to process @GenerateKeys('"
        + keyGenClassName + "'): " + caught.getMessage(), caught);
  }

  public static String quoteComma(String value) {
    return value.replace(",", "\\,");
  }
}
