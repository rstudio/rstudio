/*
 * NewSparkConnectionDialog.java
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

import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.connections.ConnectionsPresenter;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.NewSparkConnectionContext;
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

public class NewSparkConnectionDialog extends ModalDialog<ConnectionOptions>
{
   @Inject
   private void initialize(Session session, UIPrefs uiPrefs)
   {
      session_ = session;
      uiPrefs_ = uiPrefs;
   }
   
   public NewSparkConnectionDialog(NewSparkConnectionContext context,
                                   OperationWithInput<ConnectionOptions> operation)
   {
      super("Connect to Spark Cluster", operation);
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      context_ = context;
      
      loadAndPersistClientState();
      
      setOkButtonCaption("Connect");
           
      HelpLink helpLink = new HelpLink(
            "Using Spark with RStudio",
            "about_shiny",
            false);
      helpLink.addStyleName(RES.styles().helpLink());
      addLeftWidget(helpLink);   
   }
   
   @Override
   protected void onDialogShown()
   {
      super.onDialogShown();
      FocusHelper.setFocusDeferred(master_);
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
      
      // master
      final Grid masterGrid = new Grid(1, 2);
      masterGrid.addStyleName(RES.styles().grid());
      masterGrid.addStyleName(RES.styles().masterGrid());
      masterGrid.addStyleName(RES.styles().remote());
      Label masterLabel = new Label("Master:");
      masterLabel.addStyleName(RES.styles().label());
      masterGrid.setWidget(0, 0, masterLabel);
      master_ = new SparkMasterChooser(context_);
      master_.addStyleName(RES.styles().spanningInput());
      if (lastResult_.getMaster() != null)
         master_.setSelection(lastResult_.getMaster());
      masterGrid.setWidget(0, 1, master_);
      container.add(masterGrid);
 
      // versions
      Grid versionGrid = new Grid(2, 2);
      versionGrid.addStyleName(RES.styles().grid());
      versionGrid.addStyleName(RES.styles().versionGrid());
      Label sparkLabel = new Label("Spark version:");
      sparkLabel.addStyleName(RES.styles().label());
      versionGrid.setWidget(0, 0, sparkLabel);
      sparkVersion_ = new ListBox();
      sparkVersion_.addStyleName(RES.styles().spanningInput());
      final JsArray<SparkVersion> sparkVersions = getAvailableSparkVersions();
      String defaultVersionNumber = null;
      HashSet<String> numbers =  new HashSet<String>();
      for (int i = 0; i<sparkVersions.length(); i++)
      {
         SparkVersion sparkVersion = sparkVersions.get(i);
         String number = sparkVersion.getSparkVersionNumber();
         if (!numbers.contains(number))
         {
            numbers.add(number);
            sparkVersion_.addItem("Spark " + number, number);
         }
         if (sparkVersion.isDefault())
            defaultVersionNumber = number;
      }
      // set default (from last result if possible)
      SparkVersion defaultSparkVersion = getDefaultSparkVersion();
      if (defaultSparkVersion != null)
      {
         if (!setValue(sparkVersion_, defaultSparkVersion.getSparkVersionNumber()))
         {
            // failsafe
            setValue(sparkVersion_, defaultVersionNumber);
         }
      }
      else
      {
         setValue(sparkVersion_, defaultVersionNumber);
      }
      versionGrid.setWidget(0, 1, sparkVersion_); 
      
      versionGrid.setWidget(1, 0, new Label("Hadoop version:"));
      hadoopVersion_ = new ListBox();
      hadoopVersion_.addStyleName(RES.styles().spanningInput());
      final Command updateHadoopVersionsCommand = new Command() {
         @Override
         public void execute()
         {
            boolean firstExecution = hadoopVersion_.getItemCount() == 0;
            int defaultIndex = 0;
            hadoopVersion_.clear();
            String sparkVersionNumber = sparkVersion_.getSelectedValue();
            for (int i = 0; i<sparkVersions.length(); i++)
            {
               SparkVersion sparkVersion = sparkVersions.get(i);
               if (sparkVersion.getSparkVersionNumber().equals(sparkVersionNumber))
               {
                  hadoopVersion_.addItem(sparkVersion.getHadoopVersionLabel(),
                                         sparkVersion.getId());
                  if (sparkVersion.isHadoopDefault())
                     defaultIndex = hadoopVersion_.getItemCount() - 1;
               }
            }
            
            // if this is the first execution and we have lastResult_ 
            // then set the index to that, otherwise use defaultIndex
            SparkVersion defaultSparkVersion = getDefaultSparkVersion();
            if (firstExecution && (defaultSparkVersion != null))
            {
               if (!setValue(hadoopVersion_, defaultSparkVersion.getId()))
               {
                  // failsafe
                  hadoopVersion_.setSelectedIndex(defaultIndex);
               }
            }
            else
            {
               hadoopVersion_.setSelectedIndex(defaultIndex);
            }
         }
      };
      updateHadoopVersionsCommand.execute();
      versionGrid.setWidget(1, 1, hadoopVersion_); 
      sparkVersion_.addChangeHandler(
            commandChangeHandler(updateHadoopVersionsCommand));
      
      container.add(versionGrid);
      
      // info regarding installation
      final InstallInfoPanel infoPanel = new InstallInfoPanel();
      container.add(infoPanel);
      
      // update info panel state
      Command updateInfoPanel = new Command() {
         @Override
         public void execute()
         {   
            SparkVersion sparkVersion = getSelectedSparkVersion();
            
            boolean remote = !master_.isLocalMaster(master_.getSelection());
            
            infoPanel.setVisible(!sparkVersion.isInstalled());
            if (!sparkVersion.isInstalled())
               infoPanel.update(sparkVersion, remote);
         }  
      };
      updateInfoPanel.execute();
      master_.addSelectionChangeHandler(commandSelectionChangeHandler(updateInfoPanel));
      sparkVersion_.addChangeHandler(commandChangeHandler(updateInfoPanel));
      hadoopVersion_.addChangeHandler(commandChangeHandler(updateInfoPanel));
      
      
      // add the code panel     
      codePanel_ = new ConnectionCodePanel();
      codePanel_.addStyleName(RES.styles().dialogCodePanel());
      codePanel_.setConnectVia(lastResult_.getConnectVia());
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
            StringBuilder builder = new StringBuilder();
            
            // connect to master
            builder.append("library(rspark)\n");    
            builder.append("sc <- spark_connect(master = \"");
            builder.append(master_.getSelection());
           
            // spark version
            SparkVersion sparkVersion = getSelectedSparkVersion();
            builder.append("\", version = \"");
            builder.append(sparkVersion.getSparkVersionNumber());
            builder.append("\"");
            
            // hadoop version if not default
            if (!sparkVersion.isHadoopDefault())
            {
               builder.append(", hadoop_version = \"");
               builder.append(sparkVersion.getHadoopVersionNumber());
               builder.append("\"");
            }
                     
            builder.append(")");
            
            codePanel_.setCode(builder.toString(), null);
         }
      };
      updateCodeCommand.execute();
      master_.addSelectionChangeHandler(commandSelectionChangeHandler(updateCodeCommand));
      sparkVersion_.addChangeHandler(commandChangeHandler(updateCodeCommand));
      hadoopVersion_.addChangeHandler(commandChangeHandler(updateCodeCommand));
      
      Grid codeGrid = new Grid(1, 1);
      codeGrid.addStyleName(RES.styles().codeGrid());
      codeGrid.setWidget(0, 0, codePanel_);
      container.add(codeGrid);
     
 
      return container;
   }

   @Override
   protected ConnectionOptions collectInput()
   {     
      // collect the result
      ConnectionOptions result = ConnectionOptions.create(
            master_.getSelection(),
            !master_.isLocalMaster(master_.getSelection()),
            getSelectedSparkVersion(),
            codePanel_.getCode(),
            codePanel_.getConnectVia());
      
      // update client state
      lastResult_ = result;
      
      // return result
      return result;
   }
   
   @Override
   protected void onUnload()
   {
      super.onUnload();
      session_.persistClientState();
   }
   
   private ChangeHandler commandChangeHandler(final Command command) 
   {
      return new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            if (getSelectedSparkVersion() != null)
               command.execute();
         }   
      };
   }
   
   private SelectionChangeEvent.Handler commandSelectionChangeHandler(
         final Command command)
   {
      return new SelectionChangeEvent.Handler()
      { 
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            if (getSelectedSparkVersion() != null)
               command.execute();;
         }
      };
   }
   
   private boolean setValue(ListBox listBox, String value)
   {
      for (int i = 0; i < listBox.getItemCount(); i++)
         if (listBox.getValue(i).equals(value))
         {
            listBox.setSelectedIndex(i);
            return true;
         }
      return false;
   }
   
   private JsArray<SparkVersion> getAvailableSparkVersions()
   {
      return context_.getSparkVersions();
   }
   
   private SparkVersion getDefaultSparkVersion()
   {
      JsArray<SparkVersion> sparkVersions = getAvailableSparkVersions();
      
      // use the last result
      if (lastResult_.getSparkVersion() != null)
      {
         String versionId = lastResult_.getSparkVersion().getId();
         for (int i=0; i<sparkVersions.length(); i++)
         {
            SparkVersion sparkVersion = sparkVersions.get(i);
            if (sparkVersion.getId().equals(versionId))
               return sparkVersion;
         }
      }
      
      // if we didn't get a match then scan for default within the data
      for (int i=0; i<sparkVersions.length(); i++)
      {
         SparkVersion sparkVersion = sparkVersions.get(i);
         if (sparkVersion.isDefault())
            return sparkVersion;
      }
      
      // failsafe is just to return the first version
      return sparkVersions.get(0);
   }
   
   private SparkVersion getSelectedSparkVersion()
   {
      if (hadoopVersion_.getItemCount() > 0 &&
          hadoopVersion_.getSelectedIndex() != -1)
      {
         String id = hadoopVersion_.getValue(hadoopVersion_.getSelectedIndex());
         JsArray<SparkVersion> sparkVersions = getAvailableSparkVersions();
         for (int i = 0; i < sparkVersions.length(); i++) 
         {
            SparkVersion sparkVersion = sparkVersions.get(i);
            if (sparkVersion.getId().equals(id))
               return sparkVersion;
         }
      }
     
      return null;
   }
   
   private class NewSparkConnectionClientState extends JSObjectStateValue
   {
      public NewSparkConnectionClientState()
      {
         super(ConnectionsPresenter.MODULE_CONNECTIONS,
               "newsparkconnection-dialogresult",
               ClientState.PERSISTENT,
               session_.getSessionInfo().getClientState(),
               false);
      }

      @Override
      protected void onInit(JsObject value)
      {
         if (value != null)
            lastResult_ = value.cast();
         else
            lastResult_ = ConnectionOptions.create();
      }

      @Override
      protected JsObject getValue()
      {
         return lastResult_.cast();
      }
   }
   
   private final void loadAndPersistClientState()
   {
      if (clientStateValue_ == null)
         clientStateValue_ = new NewSparkConnectionClientState();
   }  
   private static NewSparkConnectionClientState clientStateValue_;
   private static ConnectionOptions lastResult_ = ConnectionOptions.create();
   
  
   public interface Styles extends CssResource
   {
      String label();
      String grid();
      String versionGrid();
      String masterGrid();
      String remote();
      String helpLink();
      String spanningInput();
      String infoPanel();
      String codeViewer();
      String codeGrid();
      String leftLabel();
      String componentNotInstalledWidget();
      String componentInstallLink();
      String codePanelHeader();
      String dialogCodePanel();
   }

   public interface Resources extends ClientBundle
   {
      @Source("NewSparkConnectionDialog.css")
      Styles styles();
   }
   
   public static Resources RES = GWT.create(Resources.class);
   public static void ensureStylesInjected() 
   {
      RES.styles().ensureInjected();
   }
   
   private final NewSparkConnectionContext context_;
   
   private SparkMasterChooser master_;
   private ListBox sparkVersion_;
   private ListBox hadoopVersion_;
 
   private ConnectionCodePanel codePanel_;
      
   private Session session_;

   @SuppressWarnings("unused")
   private UIPrefs uiPrefs_;
}
