/*
 * PreferencesDialogBaseResources.java
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
package org.rstudio.core.client.prefs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

public interface PreferencesDialogBaseResources extends ClientBundle
{
   public interface Styles extends CssResource
   {
      String preferencesDialog();

      String sectionChooser();
      String sectionChooserInner();
      String section();
      String activeSection();
      String indent();
      String tight();
      String nudgeRight();
      String nudgeRightPlus();
      String spaced();
      String lessSpaced();
      String extraSpaced();
      String textBoxWithChooser();
      String infoLabel();
      String headerLabel();
      String spacedBefore();
   }

   @Source("PreferencesDialogBase.css")
   Styles styles();
   
   ImageResource iconCodeEditing();
   ImageResource iconCompilePdf();
   ImageResource iconR();
   ImageResource iconSpelling();
   ImageResource iconSourceControl();
   ImageResource iconPublishing();
   
   static PreferencesDialogBaseResources INSTANCE = (PreferencesDialogBaseResources)GWT.create(PreferencesDialogBaseResources.class) ;
}
