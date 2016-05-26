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

import java.util.List;

import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.VerticalSpacer;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.views.connections.ConnectionsPresenter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
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
         return create("local", false, -1, "1.6.1", "2.6", true);
      }
      
      public static final native Result create(String master,
                                               boolean reconnect,
                                               int cores,
                                               String sparkVersion,
                                               String hadoopVersion,
                                               boolean autoInstall)
      /*-{
         return {
            "master": master,
            "reconnect": reconnect,
            "cores": cores,
            "spark_version": sparkVersion,
            "hadoop_version": hadoopVersion,
            "auto_install": autoInstall
         };
      }-*/;
      
      public final native String getMaster() /*-{ return this.master; }-*/;
      public final native boolean getReconnect() /*-{ return this.reconnect; }-*/;
      public final native int getCores() /*-{ return this.cores; }-*/;
      public final native String getSparkVersion() /*-{ return this.spark_version; }-*/;
      public final native String getHadoopVersion() /*-{ return this.hadoop_version; }-*/;
      public final native boolean getAutoInstall() /*-{ return this.auto_install; }-*/;
   }
   
   @Inject
   private void initialize(Session session)
   {
      session_ = session;
   }
   
   public static class Context
   {
      public Context(List<String> remoteServers)
      {
         remoteServers_ = remoteServers;
      }
      
      public List<String> getRemoteServers()
      {
         return remoteServers_;
      }
      
      
      private final List<String> remoteServers_;
   }
   
   public NewSparkConnectionDialog(Context context,
                                   OperationWithInput<Result> operation)
   {
      super("Connect to Spark Cluster", operation);
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      context_ = context;
      
      loadAndPersistClientState();
           
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
     
      // get previous result to populate controls
      Result lastResult = state_.getLastResult();
      
      // master
      final Grid masterGrid = new Grid(2, 2);
      masterGrid.addStyleName(RES.styles().grid());
      masterGrid.addStyleName(RES.styles().masterGrid());
      Label masterLabel = new Label("Master node:");
      masterLabel.addStyleName(RES.styles().label());
      masterGrid.setWidget(0, 0, masterLabel);
      master_ = new SparkMasterChooser(context_.getRemoteServers());
      master_.addStyleName(RES.styles().spanningInput());
      master_.setSelection(lastResult.getMaster());
      masterGrid.setWidget(0, 1, master_);
      Label coresLabel = new Label("Local cores:");
      masterGrid.setWidget(1, 0, coresLabel);
      cores_ = new ListBox();
      cores_.addItem("8 (Default)", "8");
      cores_.addItem("7");
      cores_.addItem("6");
      cores_.addItem("5");
      cores_.addItem("4");
      cores_.addItem("3");
      cores_.addItem("2");
      cores_.addItem("1");
      setValue(cores_, String.valueOf(lastResult.getCores()));
      masterGrid.setWidget(1, 1, cores_);
      container.add(masterGrid);

      // auto-reconnect
      autoReconnect_ = new CheckBox(
            "Reconnect automatically if connection is dropped");
      autoReconnect_.setValue(lastResult.getReconnect());
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
      master_.addSelectionChangeHandler(new SelectionChangeEvent.Handler()
      { 
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            manageMasterUI.execute();
         }
      });
      
      // versions
      Grid versionGrid = new Grid(2, 2);
      versionGrid.addStyleName(RES.styles().grid());
      versionGrid.addStyleName(RES.styles().versionGrid());
      Label sparkLabel = new Label("Spark version:");
      sparkLabel.addStyleName(RES.styles().label());
      versionGrid.setWidget(0, 0, sparkLabel);
      sparkVersion_ = new ListBox();
      sparkVersion_.addStyleName(RES.styles().spanningInput());
      sparkVersion_.addItem("Spark 1.6.1 (Default)", "1.6.1");
      sparkVersion_.addItem("Spark 1.6.0", "1.6.0");
      sparkVersion_.addItem("Spark 1.5.2", "1.5.2");
      sparkVersion_.addItem("Spark 1.5.1", "1.5.1");
      sparkVersion_.addItem("Spark 1.5.0", "1.5.0");
      setValue(sparkVersion_, lastResult.getSparkVersion());
      versionGrid.setWidget(0, 1, sparkVersion_);  
      versionGrid.setWidget(1, 0, new Label("Hadoop version:"));
      hadoopVersion_ = new ListBox();
      hadoopVersion_.addStyleName(RES.styles().spanningInput());
      hadoopVersion_.addItem("Hadoop 2.6 or higher (Default)", "2.6");
      hadoopVersion_.addItem("Hadoop 2.4 or higher", "2.4");
      hadoopVersion_.addItem("Hadoop 2.3", "2.3");
      hadoopVersion_.addItem("Hadoop 1.X", "1.X");
      hadoopVersion_.addItem("Cloudera Hadoop 4", "CDH4");
      setValue(hadoopVersion_, lastResult.getHadoopVersion());
      versionGrid.setWidget(1, 1, hadoopVersion_); 
      container.add(versionGrid);
      
      // auto install
      autoInstall_ = new CheckBox();
      autoInstall_.addStyleName(RES.styles().installCheckBox());
      autoInstall_.setValue(true);
      container.add(autoInstall_);
      
      final Command updateInstallTextCommand = new Command() {
         @Override
         public void execute()
         {
            autoInstall_.setText("Install Spark " + 
                                 sparkVersion_.getSelectedValue() +
                                 " runtime on local system");
            
         }
      };
      updateInstallTextCommand.execute();
      sparkVersion_.addChangeHandler(new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            updateInstallTextCommand.execute();
         }
      });
      
      
      // connection code
      container.add(new VerticalSpacer("20px"));
      //container.add(new Label("Code"));
      //container.add(new Label("Insert into:"));
      
      
      return container;
   }

   @Override
   protected Result collectInput()
   {     
      // collect the result
      Result result = Result.create(
            master_.getSelection(),
            autoReconnect_.getValue(),
            Integer.parseInt(cores_.getSelectedValue()),
            sparkVersion_.getSelectedValue(),
            hadoopVersion_.getSelectedValue(),
            autoInstall_.getValue());
      
      // update client state
      state_ = State.create(result);
      
      // return result
      return result;
   }
   
   @Override
   protected void onUnload()
   {
      super.onUnload();
      session_.persistClientState();
   }
   
   private boolean setValue(ListBox listBox, String value)
   {
      for (int i = 0; i < listBox.getItemCount(); i++)
         if (value.equals(listBox.getValue(i)))
         {
            listBox.setSelectedIndex(i);
            return true;
         }
      return false;
   }
   
   private static class State extends JavaScriptObject
   {
      protected State()
      {
      }
      
      public static final State create()
      {
         return create(emptyArray(),
                       emptyArray(),
                       Result.create());
      }
      
      public static final State create(Result lastResult)
      {
         return create(emptyArray(),
                       emptyArray(),
                       lastResult);
      }
      
      public static final native State create(JsArrayString customSparkVersions,
                                              JsArrayString customHadoopVersions,
                                              Result lastResult) /*-{ 
         return {
            custom_spark_versions: customSparkVersions,
            custom_hadoop_versions: customHadoopVersions,
            last_result: lastResult
         }; 
      }-*/;
           
      public final native JsArrayString getCustomSparkVersions() /*-{
         return this.custom_spark_versions;
      }-*/;
      
      
      public final native JsArrayString getCustomHadoopVersions() /*-{
         return this.custom_hadoop_versions;
      }-*/;
      
      public final native Result getLastResult() /*-{ 
         return this.last_result; 
      }-*/;
      
      private final native static JsArrayString emptyArray() /*-{
         return [];
      }-*/;
   } 
   
   private class NewSparkConnectionClientState extends JSObjectStateValue
   {
      public NewSparkConnectionClientState()
      {
         super(ConnectionsPresenter.MODULE_CONNECTIONS,
               "spark-connection-dialog",
               ClientState.PERSISTENT,
               session_.getSessionInfo().getClientState(),
               false);
      }

      @Override
      protected void onInit(JsObject value)
      {
         if (value != null)
            state_ = value.cast();
         else
            state_ = State.create();
      }

      @Override
      protected JsObject getValue()
      {
         return state_.cast();
      }
   }
   
   private final void loadAndPersistClientState()
   {
      if (clientStateValue_ == null)
         clientStateValue_ = new NewSparkConnectionClientState();
   }  
   private static NewSparkConnectionClientState clientStateValue_;
   private static State state_ = State.create();
   
  
   public interface Styles extends CssResource
   {
      String label();
      String grid();
      String versionGrid();
      String masterGrid();
      String remote();
      String helpLink();
      String spanningInput();
      String installCheckBox();
   }

   public interface Resources extends ClientBundle
   {
      @Source("NewSparkConnectionDialog.css")
      Styles styles();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
   
   private final Context context_;
   
   private SparkMasterChooser master_;
   private CheckBox autoReconnect_;
   private ListBox cores_;
   private ListBox sparkVersion_;
   private ListBox hadoopVersion_;
   private CheckBox autoInstall_;
  
      
   private Session session_;
}
