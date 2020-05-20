/*
 * LauncherSessionStatus.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.application.ui;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class LauncherSessionStatus extends Composite
{

   private static LauncherSessionStatusUiBinder uiBinder = GWT
         .create(LauncherSessionStatusUiBinder.class);

   interface LauncherSessionStatusUiBinder extends UiBinder<Widget, LauncherSessionStatus>
   {
   }

   public LauncherSessionStatus()
   {
      initWidget(uiBinder.createAndBindUi(this));
      Roles.getStatusRole().set(status_.getElement());
   }

   public String getMessage()
   {
      return message_.getText();
   }

   public void setStatus(String text)
   {
      status_.setVisible(true);
      status_.setText(text);
   }

   @UiField Label message_;
   @UiField Label status_;
}
