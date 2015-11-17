package org.rstudio.studio.client.workbench.views.source;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SelectWidget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class NewShinyWebApplication extends ModalDialog<NewShinyWebApplication.Result>
{
   public static class Result
   {
      public Result(String appName, String appType, String appDir)
      {
         appName_ = appName;
         appType_ = appType;
         appDir_ = appDir;
      }
      
      public String getAppName() { return appName_; }
      public String getAppType() { return appType_; }
      public String getAppDir() { return appDir_; }
      
      private String appName_;
      private String appType_;
      private String appDir_;
   }
   
   private static class VerticalSpacer extends Composite
   {
      public VerticalSpacer(String height)
      {
         FlowPanel panel = new FlowPanel();
         panel.setHeight(height);
         initWidget(panel);
      }
   }
   
   public NewShinyWebApplication(String caption, 
                                 FileSystemItem workingDirectory,
                                 OperationWithInput<Result> operation)
   {
      super(caption, operation);
      
      container_ = new VerticalPanel();
      
      // Create individual widgets
      appNameLabel_ = new Label("Application Name:");
      appNameLabel_.addStyleName(RES.styles().label());
      
      appNameTextBox_ = new TextBox();
      appNameTextBox_.getElement().getStyle().setMarginLeft(10, Unit.PX);
      appNameTextBox_.getElement().setAttribute("placeholder", "Name");
      appNameTextBox_.setText(defaultAppName(workingDirectory));
      
      appTypeLabel_ = new Label("Application Type:");
      appTypeLabel_.addStyleName(RES.styles().label());
      
      appTypeSelectWidget_ = new SelectWidget(
            "",
            new String[] {
                  "Single File (app.R)",
                  "Multiple File (ui.R/server.R)",
            },
            new String[] {
                  TYPE_SINGLE_FILE,
                  TYPE_MULTI_FILE
            },
            false,
            true,
            false);
      
      appTypeSelectWidget_.getElement().getStyle().setMarginTop(0, Unit.PX);
      appTypeSelectWidget_.getElement().getStyle().setMarginBottom(0, Unit.PX);
      
      directoryChooserTextBox_ = new DirectoryChooserTextBox("Directory:", null);
      directoryChooserTextBox_.setText(workingDirectory.getPath());
      
      // Add them to parent
      Grid appNameTypeGrid = new Grid(3, 2);
      appNameTypeGrid.setWidget(0, 0, appNameLabel_);
      appNameTypeGrid.setWidget(0, 1, appNameTextBox_);
      
      appNameTypeGrid.setWidget(1, 0, new VerticalSpacer("4px"));
      appNameTypeGrid.setWidget(1, 1, new VerticalSpacer("4px"));
      
      appNameTypeGrid.setWidget(2, 0, appTypeLabel_);
      appNameTypeGrid.setWidget(2, 1, appTypeSelectWidget_);
      container_.add(appNameTypeGrid);
      
      container_.add(new VerticalSpacer("12px"));
      container_.add(directoryChooserTextBox_);
      
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            appNameTextBox_.setFocus(true);
         }
      });
   }
   
   private String defaultAppName(FileSystemItem workingDir)
   {
      String path = workingDir.getPath();
      int lastSlashIndex = path.lastIndexOf('/');
      if (lastSlashIndex == -1)
         return "";
      
      return path.substring(lastSlashIndex + 1);
   }

   @Override
   protected Widget createMainWidget()
   {
      return container_;
   }

   @Override
   protected Result collectInput()
   {
      return new Result(
            appNameTextBox_.getText().trim(),
            appTypeSelectWidget_.getValue(),
            directoryChooserTextBox_.getText());
   }
   
   public static final String TYPE_SINGLE_FILE = "type_single_file";
   public static final String TYPE_MULTI_FILE  = "type_multi_file";
   
   private VerticalPanel container_;
   
   private Label   appNameLabel_;
   private TextBox appNameTextBox_;
   
   private Label   appTypeLabel_;
   private SelectWidget appTypeSelectWidget_;
   
   private DirectoryChooserTextBox directoryChooserTextBox_;
   
   // Styles ----
   
   public interface Styles extends CssResource
   {
      String label();
   }

   public interface Resources extends ClientBundle
   {
      @Source("NewShinyWebApplication.css")
      Styles styles();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
   
}
