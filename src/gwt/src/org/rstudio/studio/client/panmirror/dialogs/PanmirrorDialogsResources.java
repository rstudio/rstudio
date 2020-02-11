/*
 * PanmirrorDialogsResources.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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


package org.rstudio.studio.client.panmirror.dialogs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface PanmirrorDialogsResources extends ClientBundle
{
   public interface Styles extends CssResource
   {
      String dialog();
      String spaced();
      String textArea();
      String checkBox();
      String fullWidth();
      String infoLabel();
   }

   @Source("PanmirrorDialogsStyles.css")
   Styles styles();
   
   public static PanmirrorDialogsResources INSTANCE = (PanmirrorDialogsResources)GWT.create(PanmirrorDialogsResources.class) ;
   

   public static void ensureStylesInjected()
   {
      INSTANCE.styles().ensureInjected();
   }
}
