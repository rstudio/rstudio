/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.sample.mail.client;

/**
 * A simple structure containing the basic components of an email.
 */
public final class MailItem {

  /**
   * The sender's name.
   */
  public String sender;

  /**
   * The sender's email.
   */
  public String email;

  /**
   * The email subject line.
   */
  public String subject;

  /**
   * The email's HTML body.
   */
  public String body;

  /**
   * Read flag.
   */
  public boolean read;

  public MailItem(String sender, String email, String subject, String body) {
    this.sender = sender;
    this.email = email;
    this.subject = subject;
    this.body = body;
  }
}
