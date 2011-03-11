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

import com.google.gwt.i18n.server.MessageFormatUtils.MessageStyle;
import com.google.gwt.i18n.shared.GwtLocale;
import com.google.gwt.i18n.shared.AlternateMessageSelector.AlternateForm;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Describes a single translatable message.
 */
public interface Message extends Comparable<Message>, MessageTranslation {

  /**
   * Mapping for a group of particular alternate forms to the message to use. 
   */
  public static class AlternateFormMapping
      implements Comparable<AlternateFormMapping> {
    private final List<AlternateForm> forms;
    private final String message;
    
    public AlternateFormMapping(List<AlternateForm> forms, String message) {
      this.forms = forms;
      this.message = message;
    }

    public int compareTo(AlternateFormMapping o) {
      for (int i = 0; i < forms.size(); ++i) {
        if (i >= o.forms.size()) {
          // equal to this point and {@code o} is shorter
          return -1;
        }
        int c = forms.get(i).compareTo(o.forms.get(i));
        if (c != 0) {
          return c;
        }
      }
      if (o.forms.size() > forms.size()) {
        return 1;
      }
      return 0;
    }

    public List<AlternateForm> getForms() {
      return forms;
    }

    public String getMessage() {
      return message;
    }

    @Override
    public String toString() {
      return forms.toString() + " => " + message;
    }
  }

  /**
   * Accept a {@link MessageVisitor}.
   *
   * @param v {@link MessageVisitor} to call
   * @throws MessageProcessingException if thrown by the visitor or its
   *     sub-visitors
   */
  void accept(MessageVisitor v) throws MessageProcessingException;

  /**
   * Accept a {@link MessageVisitor}, using translations from the requested
   * locale.
   *
   * @param v {@link MessageVisitor} to call
   * @param locale locale to use for translations, or null to use the messages
   *     present in the source
   * @throws MessageProcessingException if thrown by the visitor or its
   *     sub-visitors
   */
  void accept(MessageVisitor v, GwtLocale locale)
      throws MessageProcessingException;

  /**
   * Messages are ordered by their keys.
   * 
   * @return -1 if this message is before {@code o}, 0 if they are equal, or
   *     1 if this is message is after {code o}
   */
  int compareTo(Message o);

  /**
   * Get the list of all possible messages.  If there are not alternate
   * message selectors, there will be a single entry with an empty list and
   * the default value.
   * 
   * @return a list of all message forms, lexicographically sorted by the
   *    alternate forms for each message
   */
  Iterable<AlternateFormMapping> getAllMessageForms();

  /**
   * Return the requested annotation present on this message, including parents
   * if the annotation is inherited.
   * 
   * @param annotClass
   * @return an annotation instance or null if not found
   */
  <A extends Annotation> A getAnnotation(Class<A> annotClass);

  /**
   * Return the default form of this message.
   * 
   * @return default message or null if not provided
   */
  String getDefaultMessage();

  /**
   * Return the description of this message.
   * 
   * @return description or null if not provided
   */
  String getDescription();

  /**
   * Return the key associated with this message.
   * 
   * @return key to use for message lookups
   */
  String getKey();

  /**
   * Return the meaning of this message.
   * 
   * @return meaning or null if not provided
   */
  String getMeaning();

  /**
   * Return the {@link MessageInterface} this message is associated with.
   * 
   * @return a {@link MessageInterface} instance
   */
  MessageInterface getMessageInterface();

  /**
   * Return the message style (ie, quoting and argument rules) of this message.
   * 
   * @return MessageStyle instance for this message
   */
  MessageStyle getMessageStyle();

  /**
   * Return the name of the method for this message - this should generally only
   * be used in providing error messages.
   * 
   * @return default message or null if not provided
   */
  String getMethodName();

  /**
   * Get the parameters defined for this message.
   * 
   * @return a possibly empty list of parameters
   */
  List<Parameter> getParameters();

  /**
   * Return the declared return type for this message.
   *
   * @return the declared return type
   */
  Type getReturnType();

  /**
   * Get the list of parameters controlling alternate message selection.
   * 
   * @return possibly empty array of indices into the list returned by
   *     {@link #getParameters()}
   */
  int[] getSelectorParameterIndices();

  /**
   * Get an appropriate translation for this message for a given locale.
   * 
   * @param locale a locale to get a translation for, or null to retrieve the
   *    message in the source
   * @return a non-null {@link MessageTranslation} instance - if locale is null,
   *     or no better match is found, {@code this} must be returned
   */
  MessageTranslation getTranslation(GwtLocale locale);

  /**
   * Check if a specified annotation is present on this message (including
   * via inheritance if the annotation is inherited).
   * 
   * @param annotClass
   * @return true if the annotation is present
   */
  boolean isAnnotationPresent(Class<? extends Annotation> annotClass);

  /**
   * Return true if this method is a varargs method.
   *
   * @return true if this method is a varargs method
   */
  boolean isVarArgs();
}