package org.rstudio.studio.client.vcs.ui;

import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.Server;
import org.rstudio.studio.client.vcs.VCSApplicationView;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class VCSApplicationWindow extends Composite
                                  implements VCSApplicationView,
                                             RequiresResize,
                                             ProvidesResize
{
   @Inject
   public VCSApplicationWindow(GlobalDisplay globalDisplay,
                               final Server server)
   {
      applicationPanel_ = new LayoutPanel();
      Label label = new Label("VCS Application");
      label.addClickHandler(new ClickHandler() {

         @Override
         public void onClick(ClickEvent event)
         {

         }
 
      });
      applicationPanel_.add(label);
   }
   
   @Override 
   public Widget getWidget()
   {
      return applicationPanel_;
   }

   @Override
   public void onResize()
   {
      applicationPanel_.onResize();
      
   }

   
   private LayoutPanel applicationPanel_;
}
