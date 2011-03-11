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

import java.util.List;

/**
 * Keeps track of control breaks for different forms of selectors, and calls
 * begin/end Selector/Form methods on one or more {@link MessageFormVisitor}
 * instances when appropriate.
 */
public class FormVisitorDriver {

  private int numSelectors;
  private MessageFormVisitor[] formVisitors;
  private String[] lastForm;
  private int[] selectorParams;
  private List<Parameter> params;

  /**
   * Cleanup processing a message, including closing any outstanding
   * {@link MessageFormVisitor} calls.  Typically called from
   * {@link MessageVisitor#endMessage(Message, MessageTranslation)}.
   * 
   * @param msg
   * @throws MessageProcessingException
   */
  public void endMessage(Message msg) throws MessageProcessingException {
    // close all outstanding selectors
    for (int i = numSelectors; i-- > 0; ) {
      if (lastForm[i] != null && formVisitors[i] != null) {
        formVisitors[i].endForm(i, lastForm[i]);
        formVisitors[i].endSelector(i, getSelectorParam(i));
      }
    }

    formVisitors = null;
    lastForm = null;
  }

  /**
   * Prepare for processing a new message.  Typically called from
   * {@link MessageInterfaceVisitor#visitMessage(Message, MessageTranslation)}.
   * 
   * @param msg
   */
  public void initialize(Message msg) {
    initialize(msg, null);
  }

  /**
   * Prepare for processing a new message.  Typically called from
   * {@link MessageInterfaceVisitor#visitMessage(Message, MessageTranslation)}.
   * 
   * @param msg
   * @param formVisitor a single {@link MessageFormVisitor} to be used for all
   *     selector forms, or null if none or will be supplied later
   */
  public void initialize(Message msg, MessageFormVisitor formVisitor) {
    selectorParams = msg.getSelectorParameterIndices();
    numSelectors = selectorParams.length;
    formVisitors = new MessageFormVisitor[numSelectors];
    lastForm = new String[numSelectors];
    params = msg.getParameters();
    if (formVisitor != null) {
      for (int i = 0; i < numSelectors; ++i) {
        formVisitors[i] = formVisitor;
      }
    }
  }

  /**
   * Set a visitor for forms at a particular selector level. Typically called
   * from
   * {@link MessageInterfaceVisitor#visitMessage(Message, MessageTranslation)}.
   *
   * @param level
   * @param visitor
   */
  public void setFormVisitor(int level, MessageFormVisitor visitor) {
    formVisitors[level] = visitor;
  }

  /**
   * Call methods on supplied {@link MessageFormVisitor} instances according to
   * which forms have changed.  Typically called from
   * {@link MessageVisitor#visitTranslation(String[], boolean, com.google.gwt.i18n.server.MessageFormatUtils.MessageStyle, String)}.
   * 
   * @param formNames
   * @throws MessageProcessingException
   */
  public void visitForms(String[] formNames)
      throws MessageProcessingException {

    // find where the changes were
    int firstDifferent = 0;
    while (firstDifferent < numSelectors && formNames[
        firstDifferent].equals(lastForm[firstDifferent])) {
      ++firstDifferent;
    }

    // close nested selectors/forms deeper than the change
    for (int i = numSelectors; i-- > firstDifferent; ) {
      if (lastForm[i] != null && formVisitors[i] != null) {
        formVisitors[i].endForm(i, lastForm[i]);
        if (i > firstDifferent) {
          formVisitors[i].endSelector(i, getSelectorParam(i));
        }
      }
    }

    // open all nested selectors from here
    for (int i = firstDifferent; i < numSelectors; ++i) {
      if ((i > firstDifferent || lastForm[i] == null)
          && formVisitors[i] != null) {
        formVisitors[i].beginSelector(i, getSelectorParam(i));
      }
      lastForm[i] = formNames[i];
      if (formVisitors[i] != null) {
        formVisitors[i].beginForm(i, lastForm[i]);
      }
    }
  }

  private Parameter getSelectorParam(int i) {
    int index = selectorParams[i];
    return index < 0 ? null : params.get(index);
  }
}
