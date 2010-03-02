/*
 * Copyright 2010 Google Inc.
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

package com.google.gwt.uibinder.test.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

/**
 * A simple widget for i18n tests.
 */
public class I18nMessageTest extends Composite {

  /**
   * The UiBinder interface.
   */
  interface Binder extends UiBinder<Widget, I18nMessageTest> { }
  private static final Binder BINDER = GWT.create(Binder.class);

  /**
   * The message holder to use in the template.
   */
  public interface TestMessages extends Messages {
    @DefaultMessage("Message from Brazil")
    String messageBrazil();

    @DefaultMessage("Message from USA")
    String messageUsa();
  }

  public I18nMessageTest() {
    initWidget(BINDER.createAndBindUi(this));
  }
}
