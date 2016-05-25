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
import org.rstudio.core.client.widget.VerticalSpacer;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.views.connections.ConnectionsPresenter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class NewSparkConnectionDialog extends ModalDialog<NewSparkConnectionDialog.Result>
{
   // extends JavaScriptObject for easy serialization (as client state)
   public static class Result extends JavaScriptObject
   {
      protected Result() {}
      
      public static final Result create()
      {
         return create("local", -1, "1.6.1", "2.6", false);
      }
      
      public static final native Result create(String master,
                                               int cores,
                                               String sparkVersion,
                                               String hadoopVersion,
                                               boolean reconnect)
      /*-{
         return {
            "master": master,
            "cores": cores,
            "spark_version": sparkVersion,
            "hadoop_version": hadoopVersion,
            "reconnect": reconnect
         };
      }-*/;
      
      public final native String getMaster() /*-{ return this.master; }-*/;
      public final native int getCores() /*-{ return this.cores; }-*/;
      public final native String getSparkVersion() /*-{ return this.spark_version; }-*/;
      public final native String getHadoopVersion() /*-{ return this.hadoop_version; }-*/;
      public final native boolean getReconnect() /*-{ return this.reconnect; }-*/;
   }
   
   @Inject
   private void initialize(Session session,
                           GlobalDisplay globalDisplay,
                           WorkbenchContext context)
   {
      session_ = session;
      globalDisplay_ = globalDisplay;
      context_ = context;
   }
   
   public NewSparkConnectionDialog(OperationWithInput<Result> operation)
   {
      super("Connect to Spark Cluster", operation);
      RStudioGinjector.INSTANCE.injectMembers(this);
      
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
      master_.selectAll();
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
     
      Grid masterGrid = new Grid(2, 2);
      masterGrid.addStyleName(RES.styles().grid());
      
      
      Label masterLabel = new Label("Master node:");
      masterLabel.addStyleName(RES.styles().label());
      masterGrid.setWidget(0, 0, masterLabel);
      master_ = new TextBox();
      master_.addStyleName(RES.styles().textBox());
      master_.setText("local");
      masterGrid.setWidget(0, 1, master_);
      
      Label coresLabel = new Label("Local cores:");
      masterGrid.setWidget(1, 0, coresLabel);
      ListBox cores = new ListBox();
      cores.addItem("8 cores (Default)");
      cores.addItem("7 cores");
      cores.addItem("6 cores");
      cores.addItem("5 cores");
      cores.addItem("4 cores");
      cores.addItem("3 cores");
      cores.addItem("2 cores");
      cores.addItem("1 core");
      masterGrid.setWidget(1, 1, cores);
    
      container.add(masterGrid);
      
      CheckBox autoReconnect = new CheckBox(
            "Automatically restore broken connections");
      container.add(autoReconnect);
    
      
      Grid versionGrid = new Grid(2, 2);
      versionGrid.addStyleName(RES.styles().grid());
      versionGrid.addStyleName(RES.styles().versionGrid());
     
      Label sparkLabel = new Label("Spark version:");
      sparkLabel.addStyleName(RES.styles().label());
      versionGrid.setWidget(0, 0, sparkLabel);
      ListBox sparkVersion = new ListBox();
      sparkVersion.addStyleName(RES.styles().spanningListBox());
      sparkVersion.addItem("Spark 1.6.1 (Default)", "1.6.1");
      sparkVersion.addItem("Spark 1.6.0", "1.6.0");
      sparkVersion.addItem("Spark 1.5.2", "1.5.2");
      sparkVersion.addItem("Spark 1.5.1", "1.5.1");
      sparkVersion.addItem("Spark 1.5.0", "1.5.0");
      versionGrid.setWidget(0, 1, sparkVersion);
      
      versionGrid.setWidget(1, 0, new Label("Hadoop version:"));
      ListBox hadoopVersion = new ListBox();
      hadoopVersion.addStyleName(RES.styles().spanningListBox());
      hadoopVersion.addItem("Hadoop 2.6 or higher (Default)", "2.6");
      hadoopVersion.addItem("Hadoop 2.4 or higher", "2.4");
      hadoopVersion.addItem("Hadoop 2.3", "2.3");
      hadoopVersion.addItem("Hadoop 1.X", "1.X");
      hadoopVersion.addItem("Cloudera Hadoop 4", "CDH4");
      versionGrid.setWidget(1, 1, hadoopVersion);
      
      container.add(versionGrid);
      
      
      CheckBox autoInstall = new CheckBox(
        "Automatically install Spark 1.6.1 on local system");
      autoInstall.addStyleName(RES.styles().installCheckBox());
      autoInstall.setValue(true);
      container.add(autoInstall);
      
      container.add(new VerticalSpacer("20px"));
      
      //container.add(new Label("Code"));
      //container.add(new Label("Insert into:"));
      
      
      return container;
   }

   @Override
   protected Result collectInput()
   {     
      // TODO: collect data
      Result result = Result.create();
      
      // TODO: update client state
      
      
      return result;
   }
   
   @Override
   protected void onUnload()
   {
      super.onUnload();
      session_.persistClientState();
   }
   
   private class NewSparkConnectionClientState extends JSObjectStateValue
   {
      public NewSparkConnectionClientState()
      {
         super(ConnectionsPresenter.MODULE_CONNECTIONS,
               "new-spark-connection-dialog",
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
   
   public static final String TYPE_SINGLE_FILE = "type_single_file";
   public static final String TYPE_MULTI_FILE  = "type_multi_file";
   
   private static NewSparkConnectionClientState clientStateValue_;
   
   private TextBox master_;
   
   // Injected ----
   private Session session_;
   @SuppressWarnings("unused")
   private GlobalDisplay globalDisplay_;
   @SuppressWarnings("unused")
   private WorkbenchContext context_;
   
   // Styles ----
   
   public interface Styles extends CssResource
   {
      String label();
      String grid();
      String versionGrid();
      String helpLink();
      String spanningListBox();
      String textBox();
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
   
   private static class State extends JavaScriptObject
   {
      protected State()
      {
      }
     
      
      public static final State create()
      {
         return create(emptyArray(),
                       emptyArray(),
                       emptyArray(),
                       Result.create());
      }
      
      public static final native State create(JsArrayString previousMasters,
                                              JsArrayString customSparkVersions,
                                              JsArrayString customHadoopVersions,
                                              Result lastResult) /*-{ 
         return {
            previous_masters: previousMasters,
            custom_spark_versions: customSparkVersions,
            custom_hadoop_versions: customHadoopVersions,
            last_result: lastResult
         }; 
      }-*/;
      
      public final native JsArrayString getPreviousMasters() /*-{ 
         return this.previous_masters; 
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
   
   private static State state_ = State.create();
   
}
