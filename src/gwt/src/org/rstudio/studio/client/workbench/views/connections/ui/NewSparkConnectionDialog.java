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

import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.reditor.EditorLanguage;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.connections.ConnectionsPresenter;
import org.rstudio.studio.client.workbench.views.connections.model.HadoopVersion;
import org.rstudio.studio.client.workbench.views.connections.model.NewSparkConnectionContext;
import org.rstudio.studio.client.workbench.views.connections.model.SparkVersion;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditorWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.EditSession;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;

public class NewSparkConnectionDialog extends ModalDialog<NewSparkConnectionDialog.Result>
{
   // extends JavaScriptObject for easy serialization (as client state)
   public static class Result extends JavaScriptObject
   {
      protected Result() {}
      
      public static final Result create()
      {
         return create(null, false, null, null, null, null, null);
      }
      
      public static final native Result create(String master,
                                               boolean reconnect,
                                               String cores,
                                               String sparkVersion,
                                               String hadoopVersion,
                                               String connectCode,
                                               String connectVia)
      /*-{
         return {
            "master": master,
            "reconnect": reconnect,
            "cores": cores,
            "spark_version": sparkVersion,
            "hadoop_version": hadoopVersion,
            "connect_code": connectCode,
            "connect_via": connectVia
         };
      }-*/;
      
      public final native String getMaster() /*-{ return this.master; }-*/;
      public final native boolean getReconnect() /*-{ return this.reconnect; }-*/;
      public final native String getCores() /*-{ return this.cores; }-*/;
      public final native String getSparkVersion() /*-{ return this.spark_version; }-*/;
      public final native String getHadoopVersion() /*-{ return this.hadoop_version; }-*/;
      public final native String getConnectCode() /*-{ return this.connect_code; }-*/;
      public final native String getConnectVia() /*-{ return this.connect_via; }-*/;
      
      public static String CONNECT_R_CONSOLE = "connect-r-console";
      public static String CONNECT_NEW_R_SCRIPT = "connect-new-r-script";
      public static String CONNECT_NEW_R_NOTEBOOK = "connect-new-r-notebook";
      public static String CONNECT_COPY_TO_CLIPBOARD = "connect-copy-to-clipboard";
   }
   
   @Inject
   private void initialize(Session session, UIPrefs uiPrefs)
   {
      session_ = session;
      uiPrefs_ = uiPrefs;
   }
   
   public NewSparkConnectionDialog(NewSparkConnectionContext context,
                                   OperationWithInput<Result> operation)
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
   protected boolean validate(Result result)
   {
      return true;
   }
   
   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel container = new VerticalPanel();    
      
      // master
      final Grid masterGrid = new Grid(2, 2);
      masterGrid.addStyleName(RES.styles().grid());
      masterGrid.addStyleName(RES.styles().masterGrid());
      Label masterLabel = new Label("Master:");
      masterLabel.addStyleName(RES.styles().label());
      masterGrid.setWidget(0, 0, masterLabel);
      master_ = new SparkMasterChooser(context_);
      master_.addStyleName(RES.styles().spanningInput());
      if (lastResult_.getMaster() != null)
         master_.setSelection(lastResult_.getMaster());
      masterGrid.setWidget(0, 1, master_);
      Label coresLabel = new Label("Local cores:");
      masterGrid.setWidget(1, 0, coresLabel);
      cores_ = new ListBox();
      cores_.addItem("Auto (" + context_.getCores() + ")", "auto");
      for (int i = context_.getCores(); i>0; i--)
      {
         String value = String.valueOf(i);
         String item = value;
         cores_.addItem(item, value);     
      }
      setValue(cores_, lastResult_.getCores());
      masterGrid.setWidget(1, 1, cores_);
      container.add(masterGrid);

      // auto-reconnect
      autoReconnect_ = new CheckBox(
            "Reconnect automatically if connection is dropped");
      autoReconnect_.setValue(lastResult_.getReconnect());
      container.add(autoReconnect_);
    
      // manage visiblity of master UI components
      final Command manageMasterUI = new Command() {
         @Override
         public void execute()
         {
            boolean local = master_.isLocalMaster(master_.getSelection());
            autoReconnect_.setVisible(!local);
            if (local)
               masterGrid.removeStyleName(RES.styles().remote());
            else
               masterGrid.addStyleName(RES.styles().remote());
         }
      };
      manageMasterUI.execute();   
      master_.addSelectionChangeHandler(
                           commandSelectionChangeHandler(manageMasterUI));
      
      // versions
      Grid versionGrid = new Grid(2, 2);
      versionGrid.addStyleName(RES.styles().grid());
      versionGrid.addStyleName(RES.styles().versionGrid());
      Label sparkLabel = new Label("Spark version:");
      sparkLabel.addStyleName(RES.styles().label());
      versionGrid.setWidget(0, 0, sparkLabel);
      sparkVersion_ = new ListBox();
      sparkVersion_.addStyleName(RES.styles().spanningInput());
      final JsArray<SparkVersion> sparkVersions = context_.getSparkVersions();
      for (int i = 0; i<sparkVersions.length(); i++)
      {
         String version = sparkVersions.get(i).getNumber();
         sparkVersion_.addItem("Spark " + version, version);
      }
      if (lastResult_.getSparkVersion() != null)
         setValue(sparkVersion_, lastResult_.getSparkVersion());
      else
         setValue(sparkVersion_, context_.getDefaultSparkVersion());
      versionGrid.setWidget(0, 1, sparkVersion_);  
      versionGrid.setWidget(1, 0, new Label("Hadoop version:"));
      hadoopVersion_ = new ListBox();
      hadoopVersion_.addStyleName(RES.styles().spanningInput());
      final Command updateHadoopVersionsCommand = new Command() {
         @Override
         public void execute()
         {
            String sparkVersionNumber = sparkVersion_.getSelectedValue();
            for (int i = 0; i<sparkVersions.length(); i++)
            {
               SparkVersion sparkVersion = sparkVersions.get(i);
               if (sparkVersion.getNumber().equals(sparkVersionNumber))
               {
                  JsArray<HadoopVersion> hadoopVersions = 
                                             sparkVersion.getHadoopVersions();
                  hadoopVersion_.clear();
                  for (int h = 0; h<hadoopVersions.length(); h++)
                  {
                     HadoopVersion hadoopVersion = hadoopVersions.get(h);
                     String label = hadoopVersion.getLabel();
                     if (h == 0)
                        label = label + " (Default)";
                     hadoopVersion_.addItem(label, hadoopVersion.getId());
                  }
                  break;
               }
            }
            
         }
      };
      updateHadoopVersionsCommand.execute();
      if (lastResult_.getHadoopVersion() != null)
         setValue(hadoopVersion_, lastResult_.getHadoopVersion());
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
            SparkVersion sparkVersion = 
              context_.getSparkVersions()
                             .get(sparkVersion_.getSelectedIndex());
            HadoopVersion hadoopVersion = sparkVersion.getHadoopVersions()
                             .get(hadoopVersion_.getSelectedIndex());
            boolean remote = !master_.isLocalMaster(master_.getSelection());
            
            infoPanel.setVisible(!hadoopVersion.isInstalled());
            if (!hadoopVersion.isInstalled())
               infoPanel.update(sparkVersion, hadoopVersion, remote);
         }  
      };
      updateInfoPanel.execute();
      master_.addSelectionChangeHandler(commandSelectionChangeHandler(updateInfoPanel));
      sparkVersion_.addChangeHandler(commandChangeHandler(updateInfoPanel));
      hadoopVersion_.addChangeHandler(commandChangeHandler(updateInfoPanel));
      
      // add the code viewer
      Grid codeGrid = new Grid(2, 1);
      codeGrid.addStyleName(RES.styles().codeGrid());
          
      HorizontalPanel codeHeaderPanel = new HorizontalPanel();
      codeHeaderPanel.setWidth("100%");
      Label codeLabel = new Label("Code:");
      codeHeaderPanel.add(codeLabel);
      codeHeaderPanel.setCellHorizontalAlignment(
               codeLabel, HasHorizontalAlignment.ALIGN_LEFT);
      HorizontalPanel connectPanel = new HorizontalPanel();
      Label connectLabel = new Label("Connect from:");
      connectLabel.addStyleName(RES.styles().leftLabel());
      connectPanel.add(connectLabel);
      connectVia_ = new ListBox();
      connectVia_.addItem("R Console", Result.CONNECT_R_CONSOLE);
      //connectVia_.addItem("New R Script", Result.CONNECT_NEW_R_SCRIPT);
      //if (uiPrefs_.enableRNotebooks().getValue())
      //   connectVia_.addItem("New R Notebook", Result.CONNECT_NEW_R_NOTEBOOK);
      connectVia_.addItem("Copy to Clipboard", 
                          Result.CONNECT_COPY_TO_CLIPBOARD);
      setValue(connectVia_, lastResult_.getConnectVia());
      final Command updateConnectViaUI = new Command() {
         @Override
         public void execute()
         {
            if (connectVia_.getSelectedValue().equals(Result.CONNECT_COPY_TO_CLIPBOARD))
            {
               setOkButtonCaption("Copy");
               if (codeViewer_ != null)
               {
                  codeViewer_.getEditor().getSession().getSelection().selectAll();
                  codeViewer_.getEditor().focus();
               }
            }
            else
            {
               if (codeViewer_ != null)
                  codeViewer_.getEditor().getSession().getSelection().moveCursorTo(0, 0, false);
               setOkButtonCaption("Connect");
            }
         }
      };
      updateConnectViaUI.execute();
      connectVia_.addChangeHandler(commandChangeHandler(updateConnectViaUI));
      
      connectPanel.add(connectVia_);
      codeHeaderPanel.add(connectPanel);
      codeHeaderPanel.setCellHorizontalAlignment(
            connectPanel, HasHorizontalAlignment.ALIGN_RIGHT); 
      codeGrid.setWidget(0, 0, codeHeaderPanel);
      
      codeViewer_ = new AceEditorWidget(false);
      codeViewer_.addStyleName(RES.styles().codeViewer());
      codeViewer_.getEditor().getSession().setEditorMode(
            EditorLanguage.LANG_R.getParserName(), false);
      codeViewer_.getEditor().getSession().setUseWrapMode(true);
      codeViewer_.getEditor().getSession().setWrapLimitRange(20, 120);
      codeViewer_.getEditor().getRenderer().setShowGutter(false);
      codeViewer_.addCursorChangedHandler(new CursorChangedHandler() {
         @Override
         public void onCursorChanged(CursorChangedEvent event)
         {
            EditSession session = codeViewer_.getEditor().getSession();
            String selectedCode = session.getTextRange(session.getSelection().getRange());
            if (selectedCode.trim().equals(session.getValue().trim()))
               setValue(connectVia_, Result.CONNECT_COPY_TO_CLIPBOARD);
         }
      });
      final Command updateCodeCommand = new Command() {
         @Override
         public void execute()
         {  
            StringBuilder builder = new StringBuilder();
            
            // connect to master
            builder.append("library(rspark)\n");
            builder.append("sc <- spark_connect(\"");
            builder.append(master_.getSelection());
           
            // spark version
            builder.append("\", version = \"");
            builder.append(sparkVersion_.getSelectedValue());
            builder.append("\"");
            
            // hadoop version if not default
            if (hadoopVersion_.getSelectedIndex() != 0)
            {
               builder.append(", hadoop_version = \"");
               builder.append(hadoopVersion_.getSelectedValue());
               builder.append("\"");
            }
            
            // cores if not default
            if (cores_.getSelectedIndex() != 0)
            {
               builder.append(", cores = ");
               if (cores_.getSelectedIndex() == 0)
                  builder.append("\"auto\"");
               else
                  builder.append(Integer.parseInt(cores_.getSelectedValue()));
            }
            
            // reconnect if appropriate and not default
            if (!master_.isLocalMaster(master_.getSelection()) &&
                autoReconnect_.getValue())
            {
               builder.append(", reconnect = TRUE");
            }
            
            builder.append(")");
            codeViewer_.setCode(builder.toString());
         }
      };
      updateCodeCommand.execute();
      master_.addSelectionChangeHandler(commandSelectionChangeHandler(updateCodeCommand));
      autoReconnect_.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            updateCodeCommand.execute();
         } 
      });
      cores_.addChangeHandler(commandChangeHandler(updateCodeCommand));
      sparkVersion_.addChangeHandler(commandChangeHandler(updateCodeCommand));
      hadoopVersion_.addChangeHandler(commandChangeHandler(updateCodeCommand));
      codeGrid.setWidget(1, 0, codeViewer_);
      container.add(codeGrid);
     
     
      return container;
   }

   @Override
   protected Result collectInput()
   {     
      // collect the result
      Result result = Result.create(
            master_.getSelection(),
            autoReconnect_.getValue(),
            cores_.getSelectedValue(),
            sparkVersion_.getSelectedValue(),
            hadoopVersion_.getSelectedValue(),
            codeViewer_.getEditor().getSession().getValue(),
            connectVia_.getSelectedValue());
      
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
   
   private class NewSparkConnectionClientState extends JSObjectStateValue
   {
      public NewSparkConnectionClientState()
      {
         super(ConnectionsPresenter.MODULE_CONNECTIONS,
               "last-spark-connection-dialog-result",
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
            lastResult_ = Result.create();
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
   private static Result lastResult_ = Result.create();
   
  
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
   private CheckBox autoReconnect_;
   private ListBox cores_;
   private ListBox sparkVersion_;
   private ListBox hadoopVersion_;  
   private ListBox connectVia_;
   private AceEditorWidget codeViewer_;
      
   private Session session_;
   private UIPrefs uiPrefs_;
}
