/*
 * NewConnectionDialog.java
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

package org.rstudio.studio.client.workbench.views.connections.ui;

import java.util.HashSet;

import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext;
import org.rstudio.studio.client.workbench.views.connections.model.SparkVersion;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;

public class NewConnectionDialog extends ModalDialog<ConnectionOptions>
{
   @Inject
   private void initialize(UIPrefs uiPrefs)
   {
      uiPrefs_ = uiPrefs;
   }
   
   public NewConnectionDialog(NewConnectionContext context,
                              OperationWithInput<ConnectionOptions> operation)
   {
      super("New Connection", operation);
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      context_ = context;
      
      setOkButtonCaption("Connect");
           
      HelpLink helpLink = new HelpLink(
            "Using Spark with RStudio",
            "using_spark",
            false);
      helpLink.addStyleName(RES.styles().helpLink());
      addLeftWidget(helpLink);   
   }
   
   @Override
   protected void onDialogShown()
   {
      super.onDialogShown();
      FocusHelper.setFocusDeferred(master_);

      // initialize miniUI
      String code = "runGadget(sparklyr" + "::" + "connections_spark_shinyapp" + ")";
      events_.fireEvent(new SendToConsoleEvent(code, 
                                               true, 
                                               false, 
                                               false));
   }
   
   @Override
   protected boolean validate(ConnectionOptions result)
   {
      return true;
   }
   
   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel container = new VerticalPanel();    
      
      // create iframe for miniUI
      final IFrame miniUIFrame = new IFrame();
      container.add(miniUIFrame);      
      
      // add the code panel     
      codePanel_ = new ConnectionCodePanel();
      codePanel_.addStyleName(RES.styles().dialogCodePanel());
      final Command updateOKButtonCommand = new Command() {
         @Override
         public void execute()
         {
            if (codePanel_.getConnectVia().equals(ConnectionOptions.CONNECT_COPY_TO_CLIPBOARD))
               setOkButtonCaption("Copy");
            else
               setOkButtonCaption("Connect");
         }
      };

      updateOKButtonCommand.execute();
      codePanel_.addConnectViaChangeHandler(new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            updateOKButtonCommand.execute();
         }
      });
      
      final Command updateCodeCommand = new Command() {
         @Override
         public void execute()
         {
            // TODO: codePanel_.setCode(code, null);
         }
      };
      updateCodeCommand.execute();

      Grid codeGrid = new Grid(1, 1);
      codeGrid.addStyleName(RES.styles().codeGrid());
      codeGrid.setWidget(0, 0, codePanel_);
      container.add(codeGrid);
     
      return container;
   }
   
   public interface Styles extends CssResource
   {
      String helpLink();
      String codeViewer();
      String codeGrid();
      String codePanelHeader();
      String dialogCodePanel();
   }

   public interface Resources extends ClientBundle
   {
      @Source("NewConnectionDialog.css")
      Styles styles();
   }
   
   public static Resources RES = GWT.create(Resources.class);
   public static void ensureStylesInjected() 
   {
      RES.styles().ensureInjected();
   }
   
   private final NewConnectionContext context_;
   
   private ConnectionCodePanel codePanel_;
     
   private UIPrefs uiPrefs_;
}
