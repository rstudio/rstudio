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
package com.google.gwt.user.client.ui;

import java.util.EventObject;

/**
 * Event object containing information about form submission events.
 */
public class FormSubmitCompleteEvent extends EventObject {

  private String results;

  /**
   * Creates a new event with information about the source and submit results.
   * 
   * @param source the object sending the event
   * @param results the result text returned by the server
   */
  public FormSubmitCompleteEvent(Object source, String results) {
    super(source);
    this.results = results;
  }

  /**
   * Gets the result text of the form submission.
   * 
   * @return the result text, or <code>null</code> if there was an error
   *         reading the result text
   * @tip The result text can be <code>null</code> as a result of submitting a
   *      form to a different domain.
   */
  public String getResults() {
    return results;
  }
}
