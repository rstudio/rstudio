/*
 * WarningBar.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.theme.res.ThemeResources;

public class WarningBar extends Composite
{
   public WarningBar()
   {
      icon_ = new Image(ThemeResources.INSTANCE.warning());
      initWidget(binder.createAndBindUi(this));
   }

   public String getText()
   {
      return label_.getText();
   }

   public void setText(String text)
   {
      label_.setText(text);
   }

   public int getHeight()
   {
      return 19;
   }

   @UiField(provided = true)
   Image icon_;
   @UiField
   Label label_;

   interface MyBinder extends UiBinder<Widget, WarningBar>{}
   private static MyBinder binder = GWT.create(MyBinder.class);
}
