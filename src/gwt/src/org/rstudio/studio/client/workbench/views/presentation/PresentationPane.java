/*
 * PresentationPane.java
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
package org.rstudio.studio.client.workbench.views.presentation;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarLabel;

import org.rstudio.studio.client.common.AutoGlassPanel;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;

public class PresentationPane extends WorkbenchPane implements Presentation.Display
{
   @Inject
   public PresentationPane(Commands commands, Session session)
   {
      super("Presentation");
      commands_ = commands;
      ensureWidget();
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      
      toolbar.addLeftWidget(commands_.presentationHome().createToolbarButton());
      toolbar.addLeftSeparator();
      titleLabel_ = new ToolbarLabel();
      toolbar.addLeftWidget(titleLabel_);
      
      toolbar.addRightWidget(commands_.presentationFullscreen().createToolbarButton());
      toolbar.addRightSeparator();
      toolbar.addRightWidget(commands_.refreshPresentation().createToolbarButton());
        
      return toolbar;
   }
   
   @Override 
   protected Widget createMainWidget()
   {  
      frame_ = new PresentationFrame(false, true, titleLabel_) ;
      frame_.setSize("100%", "100%");
      return new AutoGlassPanel(frame_);
   }
   
   @Override
   public void load(String url)
   {   
      frame_.navigate(url);
   }
   
   @Override
   public void clear()
   {
      frame_.setUrl("about:blank");
   }
   
   @Override
   public boolean hasSlides()
   {
      String href = frame_.getWindow().getLocationHref();
      return !"about:blank".equals(href);
   }
   
   @Override
   public void refresh(boolean resetAnchor)
   {
      frame_.reload(resetAnchor);
   }
   
   @Override
   public void pauseMedia()
   {
      pausePlayers(frame_.getWindow());
   }
   
   private static final native void pausePlayers(WindowEx window) /*-{
      window.pauseAllPlayers();
   }-*/;
   
   @Override
   public void home()
   {
      frame_.home();
   }
   
   @Override
   public void slide(int index)
   {
      frame_.slide(index);
   }
   
   @Override
   public void next()
   {
      frame_.next();
   }
   
   @Override
   public void prev()
   {
      frame_.prev();
   }
   
   @Override
   public Size getFrameSize()
   {
      return new Size(frame_.getOffsetWidth(), frame_.getOffsetHeight());
   }
   
   
   private ToolbarLabel titleLabel_;
   private PresentationFrame frame_ ;
   private final Commands commands_;

}
