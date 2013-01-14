/*
 * LearningPane.java
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
package org.rstudio.studio.client.workbench.views.learning;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.widget.Toolbar;

import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

public class LearningPane extends WorkbenchPane implements LearningPresenter.Display
{
   @Inject
   public LearningPane(Commands commands,
                       Session session)
   {
      super("Learning");
      commands_ = commands;
      session_ = session;
      ensureWidget();
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
        
      return toolbar;
   }
   
   @Override 
   protected Widget createMainWidget()
   {      
      return new Label("Main Widget");
   }
   
   
   
   @SuppressWarnings("unused")
   private Commands commands_;
   @SuppressWarnings("unused")
   private Session session_;

}
