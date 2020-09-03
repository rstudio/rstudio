/*
 * NewPlumberAPI.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.RegexUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.DecorativeImage;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.FormLabel;
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
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class NewPlumberAPI extends ModalDialog<NewPlumberAPI.Result>
{
   // extends JavaScriptObject for easy serialization (as client state)
   public static class Result extends JavaScriptObject
   {
      protected Result() {}

      public static Result create()
      {
         return create("", "");
      }

      public static native Result create(String appName,
                                         String appDir)
      /*-{
         return {
            "name": appName,
            "dir": appDir
         };
      }-*/;

      public final native String getAPIName() /*-{ return this["name"]; }-*/;
      public final native String getAPIDir() /*-{ return this["dir"]; }-*/;
   }

   private void addTextFieldValidator(HasKeyDownHandlers widget)
   {
      widget.addKeyDownHandler(event -> Scheduler.get().scheduleDeferred(() -> validateAPIName()));
   }

   private boolean isValidAPIName(String apiName)
   {
      return RE_VALID_API_NAME.test(apiName);
   }

   private void validateAPIName()
   {
      String apiName = apiNameTextBox_.getText().trim();
      if (apiName.isEmpty() || isValidAPIName(apiName))
         apiNameTextBox_.removeStyleName(RES.styles().invalidAPIName());
      else
         apiNameTextBox_.addStyleName(RES.styles().invalidAPIName());
   }

   private class PlumberAPIClientState extends JSObjectStateValue
   {
      public PlumberAPIClientState()
      {
         super("plumber",
               "new-plumber-application",
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
                        value.getString("dir"));
      }

      @Override
      protected JsObject getValue()
      {
         return result_.cast();
      }
   }

   private void loadAndPersistClientState()
   {
      if (clientStateValue_ == null)
         clientStateValue_ = new PlumberAPIClientState();
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
         String cachedDir = result_.getAPIDir();
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

   public NewPlumberAPI(String caption,
                        OperationWithInput<Result> operation)
   {
      super(caption, Roles.getDialogRole(), operation);
      RStudioGinjector.INSTANCE.injectMembers(this);

      setOkButtonCaption("Create");

      loadAndPersistClientState();

      VerticalPanel controls = new VerticalPanel();
      // Create individual widgets
      apiNameTextBox_ = new TextBox();
      DomUtils.disableSpellcheck(apiNameTextBox_);
      apiNameTextBox_.addStyleName(RES.styles().apiNameTextBox());
      DomUtils.setPlaceholder(apiNameTextBox_, "Name");
      addTextFieldValidator(apiNameTextBox_);
      FormLabel apiNameLabel = new FormLabel("API name:", apiNameTextBox_);
      apiNameLabel.addStyleName(RES.styles().label());
      controls.add(apiNameLabel);
      controls.add(apiNameTextBox_);
      directoryChooserTextBox_ = new DirectoryChooserTextBox(
         "Create within directory:",
         ElementIds.TextBoxButtonId.PLUMBER_DIR,
         null);
      directoryChooserTextBox_.setText(defaultParentDirectory());

      controls.add(new VerticalSpacer("12px"));
      controls.add(directoryChooserTextBox_);

      controls.add(new VerticalSpacer("20px"));
      container_ = new HorizontalPanel();
      DecorativeImage image =
         new DecorativeImage(new ImageResource2x(NewProjectResources.INSTANCE.plumberAppIcon2x()));
      image.addStyleName(RES.styles().image());
      container_.add(image);
      container_.add(controls);

      HelpLink plumberHelpLink_ = new HelpLink(
         "Plumber APIs",
         "about_plumber",
         false);
      plumberHelpLink_.getElement().getStyle().setMarginTop(4, Unit.PX);
      addLeftWidget(plumberHelpLink_);
   }

   @Override
   protected boolean validate(Result result)
   {
      String appName = result.getAPIName();
      if (!isValidAPIName(appName))
      {
         String message = appName.isEmpty()
               ? "The API name must not be empty"
               : "Invalid application name";

         globalDisplay_.showErrorMessage(
               "Invalid API Name",
               message,
               apiNameTextBox_);
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
      String name = apiNameTextBox_.getText().trim();

      // don't persist new parent directories if within a project
      String dirToUse = directoryChooserTextBox_.getText().trim();
      String dirToCache = context_.isProjectActive()
            ? result_.getAPIDir()
            : dirToUse;

      result_ = Result.create(name, dirToCache);
      return Result.create(name, dirToUse);
   }

   @Override
   protected void onUnload()
   {
      super.onUnload();
      session_.persistClientState();
   }

   private final HorizontalPanel container_;

   private final TextBox apiNameTextBox_;

   private final DirectoryChooserTextBox directoryChooserTextBox_;
   private static Result result_ = Result.create();
   private static PlumberAPIClientState clientStateValue_;

   private static final Pattern RE_VALID_API_NAME = Pattern.create(
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
      String label();
      String invalidAPIName();
      String apiNameTextBox();
   }

   public interface Resources extends ClientBundle
   {
      @Source("NewPlumberAPI.css")
      Styles styles();
   }

   private static final Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
}
