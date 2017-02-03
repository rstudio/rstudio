/*
 * NewConnectionShinyHost.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.connections.ui;


import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.shiny.model.ShinyServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.remote.RResult;
import org.rstudio.studio.client.shiny.events.ShinyFrameNavigatedEvent;
import org.rstudio.studio.client.workbench.views.connections.events.NewConnectionDialogUpdatedEvent;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext.NewConnectionInfo;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class NewConnectionShinyHost extends Composite
                                    implements ShinyFrameNavigatedEvent.Handler,
                                               NewConnectionDialogUpdatedEvent.Handler
{
   @Inject
   private void initialize(EventBus events,
                           GlobalDisplay globalDisplay,
                           ConnectionsServerOperations server,
                           ShinyServerOperations shinyServer)
   {
      events_ = events;
      globalDisplay_ = globalDisplay;
      server_ = server;
      shinyServer_ = shinyServer;
   }

   public void onBeforeActivate(Operation operation, NewConnectionInfo info)
   {
      events_.addHandler(ShinyFrameNavigatedEvent.TYPE, this);
      events_.addHandler(NewConnectionDialogUpdatedEvent.TYPE, this);
      
      initialize(operation, info);
   }
          
   public void onActivate(ProgressIndicator indicator)
   {
   }

   private void terminateShinyApp(final Operation operation)
   {
      shinyServer_.stopShinyApp(new ServerRequestCallback<Void>()
      {
         public void onResponseReceived(Void v)
         {
            operation.execute();
         }
         
         @Override
         public void onError(ServerError error)
         {
         }
      });
   }

   public void onDeactivate(Operation operation)
   {
      terminateShinyApp(operation);
   }
   
   public NewConnectionShinyHost()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      initWidget(createWidget());
   }

   private void showError(String errorMessage)
   {
      globalDisplay_.showErrorMessage("Error", errorMessage);
   }
   
   private void initialize(final Operation operation, final NewConnectionInfo info)
   {
      // initialize miniUI
      server_.launchEmbeddedShinyConnectionUI(info.getPackage(), info.getName(), new ServerRequestCallback<RResult<Void>>()
      {
         @Override
         public void onResponseReceived(RResult<Void> response)
         {
            if (response.failed()) {
               showError(response.errorMessage());
            }
            else {
               operation.execute();
            }
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }
   
   private Widget createWidget()
   {
      VerticalPanel container = new VerticalPanel();    
      
      // create iframe for miniUI
      frame_ = new RStudioFrame();
      frame_.setSize("100%", "140px");

      container.add(frame_);      
      
      // add the code panel     
      codePanel_ = new ConnectionCodePanel();
      codePanel_.addStyleName(RES.styles().dialogCodePanel());
      
      final Command updateCodeCommand = new Command() {
         @Override
         public void execute()
         {
            codePanel_.setCode("", null);
         }
      };
      updateCodeCommand.execute();

      Grid codeGrid = new Grid(1, 1);
      codeGrid.addStyleName(RES.styles().codeGrid());
      codeGrid.setCellPadding(0);
      codeGrid.setCellSpacing(0);
      codeGrid.setWidget(0, 0, codePanel_);
      container.add(codeGrid);
     
      return container;
   }

   public ConnectionOptions collectInput()
   {
      // collect the result
      ConnectionOptions result = ConnectionOptions.create(
         codePanel_.getCode(),
         codePanel_.getConnectVia());
      
      // return result
      return result;
   }

   @Override
   public void onShinyFrameNavigated(ShinyFrameNavigatedEvent event)
   {
      String url = event.getURL();
      
      if (Desktop.isDesktop())
         Desktop.getFrame().setShinyDialogUrl(url);

      frame_.setUrl(StringUtil.makeAbsoluteUrl(url));
   }
   
   @Override
   public void onNewConnectionDialogUpdated(NewConnectionDialogUpdatedEvent event)
   {
      codePanel_.setCode(event.getCode(), "");
   }
   
   public interface Styles extends CssResource
   {
      String helpLink();
      String codeViewer();
      String codeGrid();
      String codePanelHeader();
      String dialogCodePanel();
      String infoPanel();
      String leftLabel();
   }

   public interface Resources extends ClientBundle
   {
      @Source("NewConnectionShinyHost.css")
      Styles styles();
   }
   
   public static Resources RES = GWT.create(Resources.class);
   public static void ensureStylesInjected() 
   {
      RES.styles().ensureInjected();
   }
   
   private ConnectionCodePanel codePanel_;
     
   private EventBus events_;
   private RStudioFrame frame_;
   private GlobalDisplay globalDisplay_;
   private ConnectionsServerOperations server_;
   private ShinyServerOperations shinyServer_;
}
