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
package com.google.gwt.junit;

import com.google.gwt.core.client.GWTBridge;
import com.google.gwt.dev.About;
import com.google.gwt.i18n.client.Messages;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A dummy implementation of {@link GWTBridge}, which instantiates nothing.
 *
 * @see GWTMockUtilities
 */
class GWTDummyBridge extends GWTBridge {
  private static final Logger logger = Logger.getLogger(GWTDummyBridge.class.getName());

  private boolean fakeMessages = false;
  
  /**
   * Returns null.
   */
  @Override
  public <T> T create(Class<?> classLiteral) {
    if (fakeMessages && (classLiteral != null ) && Messages.class.isAssignableFrom(classLiteral)) {
      return (T) FakeMessagesMaker.create((Class<? extends Messages>) classLiteral);
    } else {
      return null;
    }
  }
  
  /**
   * Makes the create() method return mock/fake Messages, when the specified
   * class is assignable to Messages.
   */
  public void enableMockMessages() {
    fakeMessages = true;
  }

  /**
   * Returns the current version of GWT ({@link About#getGwtVersionNum()}).
   */
  @Override
  public String getVersion() {
    return About.getGwtVersionNum();
  }

  /**
   * Returns false.
   */
  @Override
  public boolean isClient() {
    return false;
  }

  /**
   * Logs the message and throwable to the standard logger, with level {@link
   * Level#SEVERE}.
   */
  @Override
  public void log(String message, Throwable e) {
    logger.log(Level.SEVERE, message, e);
  }
}
