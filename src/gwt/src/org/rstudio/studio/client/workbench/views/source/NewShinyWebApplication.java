/*
 * NewShinyWebApplication.java
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
package org.rstudio.studio.client.workbench.views.source;

import org.rstudio.core.client.RegexUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.VerticalSpacer;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.projects.ui.newproject.NewProjectResources;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class NewShinyWebApplication extends ModalDialog<NewShinyWebApplication.Result>
{
   // extends JavaScriptObject for easy serialization (as client state)
   public static class Result extends JavaScriptObject
   {
      protected Result() {}
      
      public static final Result create()
      {
         return create("", "", "");
      }
      
      public static final native Result create(String appName,
                                               String appType,
                                               String appDir)
      /*-{
         return {
            "name": appName,
            "type": appType,
            "dir": appDir
         };
      }-*/;
      
      public final native String getAppName() /*-{ return this["name"]; }-*/;
      public final native String getAppType() /*-{ return this["type"]; }-*/;
      public final native String getAppDir() /*-{ return this["dir"]; }-*/;
   }
   
   private void addTextFieldValidator(HasKeyDownHandlers widget)
   {
      widget.addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  validateAppName();
               }
            });
         }
      });
   }
   
   private boolean isValidAppName(String appName)
   {
      return RE_VALID_APP_NAME.test(appName);
   }
   
   private void validateAppName()
   {
      String appName = appNameTextBox_.getText().trim();
      if (appName.isEmpty() || isValidAppName(appName))
         appNameTextBox_.removeStyleName(RES.styles().invalidAppName());
      else
         appNameTextBox_.addStyleName(RES.styles().invalidAppName());
   }
   
   private class ShinyWebApplicationClientState extends JSObjectStateValue
   {
      public ShinyWebApplicationClientState()
      {
         super("shiny",
               "new-shiny-application",
               ClientState.PERSISTENT,
               session_.getSessionInfo().getClientState(),
               false);
      }

      @Override
      protected void onInit(JsObject value)
      {
         result_ = (value == null) ?
               Result.create() :
                  Result.create(
                        value.getString("name"),
                        value.getString("type"),
                        value.getString("dir"));
      }

      @Override
      protected JsObject getValue()
      {
         return result_.cast();
      }
   }
   
   private final void loadAndPersistClientState()
   {
      if (clientStateValue_ == null)
         clientStateValue_ = new ShinyWebApplicationClientState();
   }
   
   private String defaultParentDirectory()
   {
      String dir;
      
      // if we're in a project, use the project directory
      if (context_.isProjectActive())
      {
         dir = context_.getActiveProjectDir().getPath();
      }
      else
      {
         // otherwise, use the sticky value (if it exists)
         String cachedDir = result_.getAppDir();
         dir = StringUtil.isNullOrEmpty(cachedDir)
               ? "~"
               : cachedDir;
      }
      
      return dir;
      
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
   
   public NewShinyWebApplication(String caption, 
                                 OperationWithInput<Result> operation)
   {
      super(caption, operation);
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      setOkButtonCaption("Create");
      
      loadAndPersistClientState();
        
      controls_ = new VerticalPanel();
      
      // Create individual widgets
      appNameLabel_ = new Label("Application name:");
      appNameLabel_.addStyleName(RES.styles().label());
      
      appNameTextBox_ = new TextBox();
      appNameTextBox_.getElement().setAttribute("spellcheck", "false");
      appNameTextBox_.addStyleName(RES.styles().textBox());
      appNameTextBox_.addStyleName(RES.styles().appNameTextBox());
      appNameTextBox_.getElement().setAttribute("placeholder", "Name");
      addTextFieldValidator(appNameTextBox_);
      
      appTypeLabel_ = new Label("Application type:");
      appTypeLabel_.addStyleName(RES.styles().label());
      appTypeLabel_.getElement().getStyle().setMarginTop(2, Unit.PX);
      
      VerticalPanel radioPanel = new VerticalPanel();
      appTypeSingleFileButton_ = new RadioButton("shiny", "Single File (app.R)");
      appTypeMultipleFileButton_ = new RadioButton("shiny", "Multiple File (ui.R/server.R)");
      radioPanel.add(appTypeSingleFileButton_);
      radioPanel.add(appTypeMultipleFileButton_);
      
      String cachedAppType = result_.getAppType();
      if (TYPE_SINGLE_FILE.equals(cachedAppType))
         appTypeSingleFileButton_.setValue(true);
      else if (TYPE_MULTI_FILE.equals(cachedAppType))
         appTypeMultipleFileButton_.setValue(true);
      else
         appTypeSingleFileButton_.setValue(true);
      
      directoryChooserTextBox_ = new DirectoryChooserTextBox("Create within directory:", null);
      directoryChooserTextBox_.setText(defaultParentDirectory());
      directoryChooserTextBox_.addStyleName(RES.styles().textBox());
      
      // Add them to parent
      Grid appNameTypeGrid = new Grid(3, 2);
      appNameTypeGrid.addStyleName(RES.styles().grid());
      appNameTypeGrid.setWidget(0, 0, appNameLabel_);
      appNameTypeGrid.setWidget(0, 1, appNameTextBox_);
      
      appNameTypeGrid.setWidget(1, 0, new VerticalSpacer("9px"));
      appNameTypeGrid.setWidget(1, 1, new VerticalSpacer("9px"));
      
      appNameTypeGrid.getCellFormatter().setVerticalAlignment(2, 0, HasVerticalAlignment.ALIGN_TOP);
      appNameTypeGrid.setWidget(2, 0, appTypeLabel_);
      appNameTypeGrid.setWidget(2, 1, radioPanel);
      controls_.add(appNameTypeGrid);
      
      controls_.add(new VerticalSpacer("12px"));
      controls_.add(directoryChooserTextBox_);
      
      controls_.add(new VerticalSpacer("20px"));
      
      container_ = new HorizontalPanel();
      Image image = new Image(NewProjectResources.INSTANCE.shinyAppIcon2x());
      image.addStyleName(RES.styles().image());
      container_.add(image);
      container_.add(controls_);
      
      
      shinyHelpLink_ = new HelpLink(
            "Shiny Web Applications",
            "about_shiny",
            false);
      shinyHelpLink_.getElement().getStyle().setMarginTop(4, Unit.PX);
      addLeftWidget(shinyHelpLink_);
   }
   
   @Override
   protected void onDialogShown()
   {
      super.onDialogShown();
      appNameTextBox_.setFocus(true);
   }
   
   @Override
   protected boolean validate(Result result)
   {
      String appName = result.getAppName();
      if (!isValidAppName(appName))
      {
         String message = appName.isEmpty()
               ? "The application name must not be empty"
               : "Invalid application name";
         
         globalDisplay_.showErrorMessage(
               "Invalid Application Name",
               message,
               appNameTextBox_);
         return false;
      }
      
      return true;
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return container_;
   }

   @Override
   protected Result collectInput()
   {
      String name = appNameTextBox_.getText().trim();
      
      String type = "";
      if (appTypeSingleFileButton_.getValue())
         type = TYPE_SINGLE_FILE;
      else if (appTypeMultipleFileButton_.getValue())
         type = TYPE_MULTI_FILE;
      
      // don't persist new parent directories if within a project
      String dirToUse = directoryChooserTextBox_.getText().trim();
      String dirToCache = context_.isProjectActive()
            ? result_.getAppDir()
            : dirToUse;
            
      result_ = Result.create(name, type, dirToCache);
      return Result.create(name, type, dirToUse);
   }
   
   @Override
   protected void onUnload()
   {
      super.onUnload();
      session_.persistClientState();
   }
   
   public static final String TYPE_SINGLE_FILE = "type_single_file";
   public static final String TYPE_MULTI_FILE  = "type_multi_file";
   
   private HorizontalPanel container_;
   private VerticalPanel controls_;
   
   private Label   appNameLabel_;
   private TextBox appNameTextBox_;
   
   private Label   appTypeLabel_;
   private RadioButton appTypeSingleFileButton_;
   private RadioButton appTypeMultipleFileButton_;
   
   private DirectoryChooserTextBox directoryChooserTextBox_;
   
   private HelpLink shinyHelpLink_;
   
   private static Result result_ = Result.create();
   private static ShinyWebApplicationClientState clientStateValue_;
   
   
   private static final Pattern RE_VALID_APP_NAME = Pattern.create(
         "^\\s*" +
         "[" + RegexUtil.wordCharacter() + "]" +
         "[" + RegexUtil.wordCharacter() + "._-]*" +
         "\\s*$", "");
   
   // Injected ----
   private Session session_;
   private GlobalDisplay globalDisplay_;
   private WorkbenchContext context_;
   
   // Styles ----
   
   public interface Styles extends CssResource
   {
      String image();
      String grid();
      String label();
      String invalidAppName();
      String appNameTextBox();
      String textBox();
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
