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


import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ReloadableFrame;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarLabel;

import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.learning.model.LearningState;

public class LearningPane extends WorkbenchPane implements LearningPresenter.Display
{
   @Inject
   public LearningPane(Commands commands,
                       Session session)
   {
      super("Learning");
      ensureWidget();
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      titleLabel_ = new ToolbarLabel();
      toolbar.addLeftWidget(titleLabel_);
        
      return toolbar;
   }
   
   @Override 
   protected Widget createMainWidget()
   {  
      frame_ = new ReloadableFrame(false) ;
      frame_.setSize("100%", "100%");
      frame_.setStylePrimaryName("rstudio-HelpFrame") ;
      frame_.addLoadHandler(new LoadHandler() {

         @Override
         public void onLoad(LoadEvent event)
         {
            String title = StringUtil.notNull(
                           frame_.getWindow().getDocument().getTitle());
            titleLabel_.setText(title);
         }
      });

      return new AutoGlassPanel(frame_);
   }
   
   @Override
   protected void onLoad()
   {
      super.onLoad() ;

      if (!initialized_)
      {
         initialized_ = true;

         
      }
   }
   
   @Override
   public void load(LearningState state)
   {
      bringToFront();
       
      frame_.navigate("learning/");
   }
   
   
   private boolean initialized_ = false;
   
   private ToolbarLabel titleLabel_;
   
   private ReloadableFrame frame_ ;

}
