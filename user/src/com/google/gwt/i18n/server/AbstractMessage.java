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

import com.google.gwt.i18n.client.Constants.DefaultStringMapValue;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.i18n.client.LocalizableResource.Description;
import com.google.gwt.i18n.client.LocalizableResource.GenerateKeys;
import com.google.gwt.i18n.client.LocalizableResource.Key;
import com.google.gwt.i18n.client.LocalizableResource.Meaning;
import com.google.gwt.i18n.client.Messages.AlternateMessage;
import com.google.gwt.i18n.client.Messages.DefaultMessage;
import com.google.gwt.i18n.client.Messages.PluralCount;
import com.google.gwt.i18n.client.Messages.Select;
import com.google.gwt.i18n.server.MessageFormatUtils.MessageStyle;
import com.google.gwt.i18n.server.MessageUtils.KeyGeneratorException;
import com.google.gwt.i18n.shared.AlternateMessageSelector;
import com.google.gwt.i18n.shared.AlternateMessageSelector.AlternateForm;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.GwtLocaleFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base implementation of {@link Message}.
 */
public abstract class AbstractMessage implements Message {

  private List<AlternateForm> defaultForms;

  private String defaultMessage;

  private boolean isStringMap;

  private String key = null;

  private final GwtLocaleFactory localeFactory;

  private GwtLocale matchedLocale;

  private String meaning;

  private final MessageInterface msgIntf;

  private MessageStyle messageStyle;

  private AlternateMessageSelector[] selectors;

  private int[] selectorParams;

  private MessageTranslation overrideDefault;

  public AbstractMessage(GwtLocaleFactory localeFactory,
      MessageInterface msgIntf) {
    this.localeFactory = localeFactory;
    this.msgIntf = msgIntf;
  }

  public void accept(MessageVisitor mv) throws MessageProcessingException {
    accept(mv, null);
  }

  public void accept(MessageVisitor mv, GwtLocale locale)
      throws MessageProcessingException {

    ensureSelectorParams();
    List<Parameter> params = getParameters();
    int numSelectors = selectorParams.length;
    String[] lastForm = new String[numSelectors];

    // lookup the translation to use
    MessageTranslation trans = null;
    if (locale != null) {
      for (GwtLocale search : locale.getCompleteSearchList()) {
        trans = getTranslation(search);
        if (trans != null) {
          break;
        }
      }
    }
    if (trans == null) {
      trans = this;
    }

    for (AlternateFormMapping mapping : trans.getAllMessageForms()) {
      List<AlternateForm> forms = mapping.getForms();
      boolean allOther = true;
      for (int i = 0; i < forms.size(); ++i) {
        lastForm[i] = forms.get(i).getName();
        if (!AlternateMessageSelector.OTHER_FORM_NAME.equals(lastForm[i])) {
          allOther = false;
        }
      }
      mv.visitTranslation(lastForm, allOther, messageStyle,
          mapping.getMessage());
    }

    mv.endMessage(this, trans);
  }

  public int compareTo(Message o) {
    return getKey().compareTo(o.getKey());
  }

  public Iterable<AlternateFormMapping> getAllMessageForms() {
    if (overrideDefault != null) {
      return overrideDefault.getAllMessageForms();
    }
    List<AlternateFormMapping> mapping = new ArrayList<AlternateFormMapping>();
    List<Parameter> params = getParameters();
    int[] selectorIndices = getSelectorParameterIndices();
    int numSelectors = selectorIndices.length;

    // add the default form
    if (!isStringMap) {
      mapping.add(new AlternateFormMapping(defaultForms, getDefaultMessage()));
    }

    // look for alternate forms
    String[] altMessages = null;
    if (isStringMap) {
      DefaultStringMapValue smv = getAnnotation(DefaultStringMapValue.class);
      if (smv != null) {
        altMessages = smv.value();
      }
    } else {
      altMessages = getAlternateMessages();
    }
    if (altMessages == null) {
      return mapping;
    }
    int n = altMessages.length;
    // TODO(jat): check for even number?
    for (int msgIdx = 0; msgIdx < n; msgIdx += 2) {
      addMapping(mapping, numSelectors, altMessages[msgIdx],
          altMessages[msgIdx + 1]);
    }

    // sort into lexicographic order and return
    Collections.sort(mapping);
    return mapping;
  }

  public abstract <A extends Annotation> A getAnnotation(Class<A> annotClass);

  public String getDefaultMessage() {
    if (overrideDefault != null) {
      return overrideDefault.getDefaultMessage();
    }
    return defaultMessage;
  }

  public String getDescription() {
    Description descAnnot = getAnnotation(Description.class);
    if (descAnnot != null) {
      return descAnnot.value();
    }
    return null;
  }

  public String getKey() {
    KeyGeneratorException keyGenException = null;
    if (key == null) {
      Key keyAnnot = getAnnotation(Key.class);
      if (keyAnnot != null) {
        key = keyAnnot.value();
      } else {
        GenerateKeys keyGenAnnot = getAnnotation(GenerateKeys.class);
        try {
          KeyGenerator keyGen = MessageUtils.getKeyGenerator(keyGenAnnot);
          key = keyGen.generateKey(this);
        } catch (KeyGeneratorException e) {
          keyGenException  = e;
        }
      }
    }
    if (key == null) {
      GenerateKeys keyGenAnnot = getAnnotation(GenerateKeys.class);
      // If we were unable to get a key, things will fail later.  Instead, fail
      // here where the backtrace has useful information about the cause.
      throw new RuntimeException("null key on "
          + getMessageInterface().getQualifiedName() + "." + getMethodName()
          + ", @GenerateKeys=" + keyGenAnnot + ", defmsg=" + defaultMessage
          + ", meaning=" + meaning + ", @DefaultMessage="
          + getAnnotation(DefaultMessage.class) + ", @Meaning="
          + getAnnotation(Meaning.class) + ", override=" + overrideDefault,
          keyGenException);
    }
    return key;
  }

  public GwtLocale getMatchedLocale() {
    if (overrideDefault != null) {
      return overrideDefault.getMatchedLocale();
    }
    return matchedLocale;
  }

  public String getMeaning() {
    return meaning;
  }

  public MessageInterface getMessageInterface() { 
    return msgIntf;
  }

  public MessageStyle getMessageStyle() {
    return messageStyle;
  }

  public abstract String getMethodName();

  public abstract List<Parameter> getParameters();

  public abstract Type getReturnType();

  public int[] getSelectorParameterIndices() {
    if (selectorParams == null) {
      ensureSelectorParams();
    }
    return selectorParams;
  }

  public abstract MessageTranslation getTranslation(GwtLocale locale);

  public abstract boolean isAnnotationPresent(
      Class<? extends Annotation> annotClass);

  protected void addMapping(List<AlternateFormMapping> mapping,
      int numSelectors, String joinedForms, String msg) {
    String[] formNames = joinedForms.split("\\|");
    if (formNames.length != numSelectors) {
      // TODO(jat): warn about invalid number of forms
      return;
    }
    List<AlternateForm> forms = new ArrayList<AlternateForm>();
    boolean nonOther = false;
    for (int selIdx = 0; selIdx < numSelectors; ++selIdx) {
      String formName = formNames[selIdx];
      if (!selectors[selIdx].isFormAcceptable(formName)) {
        // TODO(jat): warn about invalid form
        nonOther = false;
        break;
      }
      if (isStringMap || !AlternateMessageSelector.OTHER_FORM_NAME.equals(formName)) {
        nonOther = true;
      }
      forms.add(new AlternateForm(formName, formName));
    }
    if (!nonOther) {
      // TODO(jat): warn about all others in alternate form
    } else {
      mapping.add(new AlternateFormMapping(forms, msg));
    }
  }

  protected List<AlternateForm> defaultForms() {
    return defaultForms;
  }

  /**
   * Get the alternate message forms from either an AlternateMessages annotation
   * or a PluralText annotation.
   */
  @SuppressWarnings("deprecation")
  protected String[] getAlternateMessages() {
    AlternateMessage altMsgAnnot = getAnnotation(AlternateMessage.class);
    if (altMsgAnnot != null) {
      return altMsgAnnot.value();
    }
    // avoid deprecation warning for the import
    com.google.gwt.i18n.client.Messages.PluralText pluralTextAnnot
        = getAnnotation(com.google.gwt.i18n.client.Messages.PluralText.class);
    if (pluralTextAnnot != null) {
      return pluralTextAnnot.value();
    }
    return null;
  }

  protected GwtLocale getDefaultLocale() {
    DefaultLocale defLocaleAnnot = getAnnotation(DefaultLocale.class);
    String defLocale = defLocaleAnnot != null ? defLocaleAnnot.value()
        : DefaultLocale.DEFAULT_LOCALE;
    return localeFactory.fromString(defLocale);
  }

  protected GwtLocaleFactory getLocaleFactory() {
    return localeFactory;
  }

  /**
   * Called by subclasses to complete initialization, after ensuring that calls
   * to {@link #getAnnotation(Class)} will function properly.
   */
  protected void init() {
    matchedLocale = getDefaultLocale();
    if (isAnnotationPresent(DefaultMessage.class)) {
      messageStyle = MessageStyle.MESSAGE_FORMAT;
      DefaultMessage defMsgAnnot = getAnnotation(DefaultMessage.class);
      defaultMessage = defMsgAnnot.value();
    } else if (isAnnotationPresent(DefaultStringMapValue.class)) {
      messageStyle = MessageStyle.PLAIN;
      processStringMap(getAnnotation(DefaultStringMapValue.class));
      isStringMap = true;
    } else {
      messageStyle = MessageStyle.PLAIN;
      defaultMessage = MessageUtils.getConstantsDefaultValue(this);
    }
    Meaning meaningAnnot = getAnnotation(Meaning.class);
    if (meaningAnnot != null) {
      meaning = meaningAnnot.value();
    } else {
      meaning = null;
    }
    if (overrideDefault == null) {
      // if the external source has a default entry, use it for the base
      // message.
      overrideDefault = getTranslation(localeFactory.getDefault());
      if (overrideDefault == this) {
        overrideDefault = null;
      }
    }
    List<Parameter> params = getParameters();
    int[] selectorIndices = getSelectorParameterIndices();
    int numSelectors = selectorIndices.length;
    defaultForms = new ArrayList<AlternateForm>();
    selectors = new AlternateMessageSelector[numSelectors];
    for (int i = 0; i < numSelectors; ++i) {
      int selIdx = selectorIndices[i];
      if (selIdx < 0) {
        // string map
        selectors[i] = new AlternateMessageSelector() {
          public boolean isFormAcceptable(String form) {
            return true;
          }
        };
      } else {
        selectors[i] = params.get(selIdx).getAlternateMessageSelector();
        defaultForms.add(AlternateMessageSelector.OTHER_FORM);
      }
    }
  }

  protected boolean isStringMap() {
    return isStringMap;
  }

  private void ensureSelectorParams() {
    if (isAnnotationPresent(DefaultStringMapValue.class)) {
      selectorParams = new int[] { -1 };
      return;
    }
    List<Integer> selectorIdx = new ArrayList<Integer>();
    List<Parameter> params = getParameters();
    int n = params.size();
    for (int i = 0; i < n; ++i) {
      Parameter param = params.get(i);
      if (param.isAnnotationPresent(PluralCount.class)
           || param.isAnnotationPresent(Select.class)) {
        selectorIdx.add(i);
      }
    }
    n = selectorIdx.size();
    selectorParams = new int[n];
    for (int i = 0; i < n; ++i) {
      selectorParams[i] = selectorIdx.get(i);
    }
  }

  private void processStringMap(DefaultStringMapValue dsmv) {
    String[] keyValues = dsmv.value();
    StringBuilder buf = new StringBuilder();
    boolean needComma = false;
    Map<String, String> map = new HashMap<String, String>();
    List<String> sortedKeys = new ArrayList<String>();
    for (int i = 0; i < keyValues.length; i += 2) {
      sortedKeys.add(keyValues[i]);
      map.put(keyValues[i], keyValues[i + 1]);
      if (needComma) {
        buf.append(',');
      } else {
        needComma = true;
      }
      buf.append(MessageUtils.quoteComma(keyValues[i]));
    }
    defaultMessage = buf.toString();
    // sets overrideDefault, but this may be reset if there is an external
    // translation for the default locale for this map
    Collections.sort(sortedKeys);
    overrideDefault = new StringMapMessageTranslation(defaultMessage,
        sortedKeys, map, matchedLocale);
  }
}
