/*
 * EnvironmentObjects.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class EnvironmentObjects extends Composite
{

   interface Binder extends UiBinder<Widget, EnvironmentObjects>
   {

   }

   public void setContextDepth(int contextDepth)
   {
      this.contextDepth
            .setText("Context depth is " + contextDepth);
   }

   public void addObject(String obj)
   {
      Label objectLabel = new Label();
      objectLabel.setText(obj);
      this.objectList.add(objectLabel);
   }
   
   public void clearObjects()
   {
      this.objectList.clear();
   }

   public EnvironmentObjects()
   {
      initWidget(GWT.<Binder> create(Binder.class).createAndBindUi(this));
   }

   @UiField
   HTMLPanel objectList;
   @UiField
   Label contextDepth;
}
