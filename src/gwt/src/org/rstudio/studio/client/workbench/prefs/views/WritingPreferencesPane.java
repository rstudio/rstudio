/*
 * WritingPreferencesPane.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.studio.client.common.prefs.WeaveRnwSelectWidget;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

public class WritingPreferencesPane extends PreferencesPane
{
   @Inject
   public WritingPreferencesPane(UIPrefs prefs,
                                 PreferencesDialogResources res)
   {
      prefs_ = prefs;
      res_ = res;
      PreferencesDialogBaseResources baseRes = PreferencesDialogBaseResources.INSTANCE;

      Label pdfCompilationLabel = new Label("Compile PDF");
      pdfCompilationLabel.addStyleName(baseRes.styles().headerLabel());
      nudgeRight(pdfCompilationLabel);
      add(pdfCompilationLabel);
      
      
      Label perProjectLabel = new Label(
          "NOTE: Compile PDF options can also be set on a per-project basis. " +
          "Options for new projects are based on the defaults below.");
      perProjectLabel.addStyleName(baseRes.styles().infoLabel());
      nudgeRight(perProjectLabel);
      add(perProjectLabel);
    
      
      defaultSweaveEngine_ = new WeaveRnwSelectWidget(
                                 "Default method for weaving Rnw files:");
      defaultSweaveEngine_.getElement().getStyle().setMarginTop(7, Unit.PX);
      defaultSweaveEngine_.setValue(
                              prefs.defaultSweaveEngine().getGlobalValue());
      add(defaultSweaveEngine_);
   }

  


   @Override
   public ImageResource getIcon()
   {
      return PreferencesDialogBaseResources.INSTANCE.iconWriting();
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   @Override
   public String getName()
   {
      return "Writing";
   }

   @Override
   protected void initialize(RPrefs prefs)
   {
   }
   
   @Override
   public void onApply(RPrefs rPrefs)
   {
      super.onApply(rPrefs);
      prefs_.defaultSweaveEngine().setGlobalValue(
                                    defaultSweaveEngine_.getValue());
   }

   private final UIPrefs prefs_;
   @SuppressWarnings("unused")
   private final PreferencesDialogResources res_;
   
   private WeaveRnwSelectWidget defaultSweaveEngine_;
   
}
