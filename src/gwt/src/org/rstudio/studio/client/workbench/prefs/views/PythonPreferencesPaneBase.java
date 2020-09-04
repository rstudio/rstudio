/*
 * PythonPreferencesPaneBase.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.prefs.PreferencesDialogPaneBase;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.InfoBar;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.views.python.PythonInterpreterListEntryUi;
import org.rstudio.studio.client.workbench.prefs.views.python.PythonInterpreterSelectionDialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

public abstract class PythonPreferencesPaneBase<T> extends PreferencesDialogPaneBase<T>
{
   public PythonPreferencesPaneBase(String width,
                                    String placeholderText)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      placeholderText_ = placeholderText;
      
      add(headerLabel("Python"));
      
      mismatchWarningBar_ = new InfoBar(InfoBar.WARNING);
      mismatchWarningBar_.setText(
            "The active Python interpreter has been changed by an R startup script.");
      mismatchWarningBar_.setVisible(false);
      add(mismatchWarningBar_);
      
      tbPythonInterpreter_ = new TextBoxWithButton(
            "Python interpreter:",
            placeholderText_,
            "Select...",
            null,
            ElementIds.TextBoxButtonId.PYTHON_PATH,
            true,
            new ClickHandler()
            {
               @Override
               public void onClick(ClickEvent event)
               {
                  getProgressIndicator().onProgress("Finding interpreters...");
                  
                  server_.pythonFindInterpreters(new ServerRequestCallback<PythonInterpreters>()
                  {
                     @Override
                     public void onResponseReceived(final PythonInterpreters response)
                     {
                        getProgressIndicator().onCompleted();
                        
                        PythonInterpreterSelectionDialog dialog =
                              new PythonInterpreterSelectionDialog(
                                    response.getPythonInterpreters(),
                                    new OperationWithInput<PythonInterpreter>()
                                    {
                                       @Override
                                       public void execute(PythonInterpreter input)
                                       {
                                          String path = input.getPath();
                                          tbPythonInterpreter_.setText(path);
                                       }
                                    });
                        
                        dialog.showModal(true);
                     }
                     
                     @Override
                     public void onError(ServerError error)
                     {
                        String message =
                              "Error finding Python interpreters: " +
                              error.getUserMessage();
                        getProgressIndicator().onError(message);
                        
                        Debug.logError(error);
                     }
                  });
               }
            });
      
      tbPythonInterpreter_.addValueChangeHandler((ValueChangeEvent<String> event) ->
      {
         updateDescription();
      });
      
      tbPythonInterpreter_.addDomHandler((KeyDownEvent event) ->
      {
         if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
         {
            event.stopPropagation();
            event.preventDefault();
            tbPythonInterpreter_.blur();
         }
      }, KeyDownEvent.getType());
      
      tbPythonInterpreter_.addDomHandler((BlurEvent event) ->
      {
         updateDescription();
      }, BlurEvent.getType());
      
      tbPythonInterpreter_.getTextBox().getElement().addClassName(
            ModalDialogBase.ALLOW_ENTER_KEY_CLASS);
      
      tbPythonInterpreter_.setWidth(width);
      tbPythonInterpreter_.setText(placeholderText_);
      tbPythonInterpreter_.setReadOnly(false);
      add(spaced(tbPythonInterpreter_));
      
      add(container_);
      
   }
   
   @Inject
   private void initialize(PythonDialogResources res,
                           PythonServerOperations server,
                           Session session,
                           UserPrefs prefs)
   {
      res_ = res;
      server_ = server;
      session_ = session;
      prefs_ = prefs;
   }
   
   protected void clearDescription()
   {
      container_.setWidget(new FlowPanel());
   }
   
   protected void updateDescription()
   {
      // avoid recursive calls
      if (updatingDescription_)
         return;
      
      try
      {
         updatingDescription_ = true;
         updateDescriptionImpl();
      }
      finally
      {
         updatingDescription_ = false;
      }
   }
   
   private void updateDescriptionImpl()
   {
      String path = tbPythonInterpreter_.getText().trim();
      
      // reset to default when empty
      if (StringUtil.isNullOrEmpty(path))
      {
         tbPythonInterpreter_.setText(placeholderText_);
         clearDescription();
         return;
      }
      
      // clear description when using default
      if (StringUtil.equals(path, placeholderText_))
      {
         clearDescription();
         return;
      }
      
      server_.pythonInterpreterInfo(
            path,
            new ServerRequestCallback<PythonInterpreter>()
            {
               @Override
               public void onResponseReceived(PythonInterpreter info)
               {
                  updateDescriptionImpl(info);
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   private void updateDescriptionImpl(PythonInterpreter info)
   {
      interpreter_ = info;
         
      if (!info.isValid())
      {
         String reason = info.getInvalidReason();
         if (StringUtil.isNullOrEmpty(reason))
            reason = "The selected Python interpreter appears to be invalid.";
         
         InfoBar bar = new InfoBar(InfoBar.WARNING);
         bar.setText(reason);
         container_.setWidget(bar);
      }
      else
      {
         PythonInterpreterListEntryUi ui = new PythonInterpreterListEntryUi(info);
         ui.addStyleName(RES.styles().interpreterDescription());
         
         String type = info.getType();
         
         if (type == null)
         {
            type = "[Unknown]";
         }
         else if (type == "virtualenv")
         {
            type = "Virtual Environment";
         }
         else if (type == "conda")
         {
            type = "Conda Environment";
         }
         else if (type == "system")
         {
            type = "System Interpreter";
         }
         
         ui.getPath().setText("[" + type + "]");
         container_.setWidget(ui);
      }
   }
   
   protected void checkForMismatch(PythonInterpreter activeInterpreter)
   {
      // nothing to do if there isn't an active interpreter
      if (StringUtil.isNullOrEmpty(activeInterpreter.getPath()))
      {
         setMismatchBarVisible(false);
         return;
      }
      
      // nothing to do if the user hasn't changed the configured Python
      String requestedPath = tbPythonInterpreter_.getText();
      boolean isSet =
            !StringUtil.isNullOrEmpty(requestedPath) &&
            !StringUtil.equals(requestedPath, placeholderText_);
      
      if (!isSet)
      {
         setMismatchBarVisible(false);
         return;
      }
      
      // toggle visibility
      boolean mismatch =
            !StringUtil.equals(requestedPath, activeInterpreter.getPath());
      
      setMismatchBarVisible(mismatch);
   }
   
   private void setMismatchBarVisible(boolean visible)
   {
      mismatchWarningBar_.setVisible(visible);
      
      if (visible)
      {
         mismatchWarningBar_.addStyleName(RES.styles().mismatchBar());
      }
      else
      {
         mismatchWarningBar_.addStyleName(RES.styles().mismatchBar());
      }
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(res_.iconPython2x());
   }

   @Override
   public String getName()
   {
      return "Python";
   }
   
   protected void initialize(String pythonPath)
   {
      initialPythonPath_ = pythonPath;
      
      if (!StringUtil.isNullOrEmpty(pythonPath))
      {
         tbPythonInterpreter_.setText(pythonPath);
         updateDescription();
      }
      
      server_.pythonActiveInterpreter(new ServerRequestCallback<PythonInterpreter>()
      {
         @Override
         public void onResponseReceived(PythonInterpreter response)
         {
            checkForMismatch(response);
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }
   
   protected RestartRequirement onApply(boolean isProjectPrefs,
                                        CommandWithArg<PythonInterpreter> update)
   {
      RestartRequirement requirement = new RestartRequirement();
      
      // read current Python path (normalize placeholder text if set)
      String newValue = tbPythonInterpreter_.getText().trim();
      if (StringUtil.equals(newValue, placeholderText_))
         newValue = "";
      
      // for project preferences, use project-relative path to interpreter
      if (isProjectPrefs)
      {
         FileSystemItem projDir = session_.getSessionInfo().getActiveProjectDir();
         if (projDir.exists() && newValue.startsWith(projDir.getPath()))
            newValue = newValue.substring(projDir.getLength() + 1);
      }
      
      // check if the interpreter appears to have been set by the user
      boolean isValidInterpreterSet =
            interpreter_ != null &&
            interpreter_.isValid() &&
            !StringUtil.isNullOrEmpty(newValue);
      
      // if an interpreter was set, update to the new value
      if (isValidInterpreterSet)
      {
         update.execute(interpreter_);
      }
      else
      {
         update.execute(PythonInterpreter.create());
      }
      
      // restart the IDE if the python path has been changed
      // (we'd prefer to just restart the session but that's not enough
      // to refresh requisite project preferences, or so it seems)
      if (!StringUtil.equals(initialPythonPath_, newValue))
      {
         if (isProjectPrefs)
         {
            requirement.setRestartRequired();
         }
         else
         {
            requirement.setSessionRestartRequired(true);
         }
      }
      
      return requirement;
   }
   
   
   public interface Styles extends CssResource
   {
      String overrideLabel();
      String interpreterDescription();
      String mismatchBar();
   }

   public interface Resources extends ClientBundle
   {
      @Source("PythonPreferencesPane.css")
      Styles styles();
   }

   protected final String placeholderText_;
   
   protected final InfoBar mismatchWarningBar_;
   protected final TextBoxWithButton tbPythonInterpreter_;
   protected final SimplePanel container_ = new SimplePanel();
   
   protected String initialPythonPath_;
   protected PythonInterpreter interpreter_;
   
   protected boolean updatingDescription_;

   protected PythonDialogResources res_;
   protected PythonServerOperations server_;
   protected Session session_;
   protected UserPrefs prefs_;
   
   
   protected static Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }

 
}
