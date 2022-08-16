/*
 * PythonPreferencesPaneBase.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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
import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.InfoBar;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.views.python.PythonInterpreterListEntryUi;
import org.rstudio.studio.client.workbench.prefs.views.python.PythonInterpreterSelectionDialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

public abstract class PythonPreferencesPaneBase<T> extends PreferencesDialogPaneBase<T>
{
   public PythonPreferencesPaneBase(String width,
                                    String placeholderText,
                                    boolean isProjectOptions)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      add(headerLabel(constants_.headerPythonLabel()));
      
      mismatchWarningBar_ = new InfoBar(InfoBar.WARNING);
      mismatchWarningBar_.setText(
            constants_.mismatchWarningBarText());
      mismatchWarningBar_.setVisible(false);
      add(mismatchWarningBar_);
      
      tbPythonInterpreter_ = new TextBoxWithButton(
            constants_.tbPythonInterpreterText(),
            null,
            placeholderText,
            constants_.tbPythonActionText(),
            new HelpButton("using_python", constants_.helpRnwButtonLabel()),
            ElementIds.TextBoxButtonId.PYTHON_PATH,
            true,
            true,
            new ClickHandler()
            {
               @Override
               public void onClick(ClickEvent event)
               {
                  getProgressIndicator().onProgress(constants_.progressIndicatorText());
                  
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
                                          String path = input == null ? "" : input.getPath();
                                          tbPythonInterpreter_.setText(path);
                                       }
                                    });
                        
                        dialog.showModal(true);
                     }
                     
                     @Override
                     public void onError(ServerError error)
                     {
                        String message =
                              constants_.onDependencyErrorMessage() +
                              error.getUserMessage();
                        getProgressIndicator().onError(message);
                        
                        Debug.logError(error);
                     }
                  });
               }
            });
      
      tbPythonInterpreter_.useNativePlaceholder();
      
      tbPythonInterpreter_.addValueChangeHandler((ValueChangeEvent<String> event) ->
      {
         updateDescription();
      });
      
      tbPythonInterpreter_.getTextBox().addDomHandler((KeyDownEvent event) ->
      {
         if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
         {
            event.stopPropagation();
            event.preventDefault();
            tbPythonInterpreter_.blur();
         }
         else if (event.getNativeKeyCode() == KeyCodes.KEY_ESCAPE)
         {
            event.stopPropagation();
            event.preventDefault();
            if (lastValue_ != null)
               tbPythonInterpreter_.setText(lastValue_);
            tbPythonInterpreter_.blur();
         }
      }, KeyDownEvent.getType());
      
      tbPythonInterpreter_.addDomHandler((BlurEvent event) ->
      {
         updateDescription();
      }, BlurEvent.getType());
      
      // save the contents of the text box on focus
      // (we'll restore the value if the user blurs via the Escape key)
      tbPythonInterpreter_.getTextBox().addFocusHandler((FocusEvent event) ->
      {
         lastValue_ = tbPythonInterpreter_.getText();
      });
      
      Element tbEl = tbPythonInterpreter_.getTextBox().getElement();
      tbEl.addClassName(ModalDialogBase.ALLOW_ENTER_KEY_CLASS);
      tbEl.addClassName(ModalDialogBase.ALLOW_ESCAPE_KEY_CLASS);
      
      tbPythonInterpreter_.setWidth(width);
      tbPythonInterpreter_.setReadOnly(false);
      add(spaced(tbPythonInterpreter_));
      
      add(interpreterDescription_);
      
      if (!isProjectOptions)
      {
         cbAutoUseProjectInterpreter_ =
               new CheckBox(constants_.cbAutoUseProjectInterpreter());
         
         initialAutoUseProjectInterpreter_ = prefs_.pythonProjectEnvironmentAutomaticActivate().getGlobalValue();
         cbAutoUseProjectInterpreter_.setValue(initialAutoUseProjectInterpreter_);
         
         cbAutoUseProjectInterpreter_.getElement().setTitle(
               constants_.cbAutoUseProjectInterpreterMessage());

         add(lessSpaced(cbAutoUseProjectInterpreter_));
      }
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
      interpreterDescription_.setWidget(new FlowPanel());
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
         tbPythonInterpreter_.setText("");
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
            reason = constants_.invalidReasonLabel();
         
         InfoBar bar = new InfoBar(InfoBar.WARNING);
         bar.setText(reason);
         interpreterDescription_.setWidget(bar);
      }
      else
      {
         PythonInterpreterListEntryUi ui = new PythonInterpreterListEntryUi(info);
         ui.addStyleName(RES.styles().interpreterDescription());
         
         String type = info.getType();
         
         if (type == null)
         {
            type = constants_.unknownType();
         }
         else if (type == "virtualenv")
         {
            type = constants_.virtualEnvironmentType();
         }
         else if (type == "conda")
         {
            type = constants_.condaEnvironmentType();
         }
         else if (type == "system")
         {
            type = constants_.systemInterpreterType();
         }
         
         ui.getPath().setText("[" + type + "]");
         interpreterDescription_.setWidget(ui);
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
      boolean isSet = !StringUtil.isNullOrEmpty(requestedPath);
      
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
      return constants_.headerPythonLabel();
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
      
      // read current Python path
      String newValue = tbPythonInterpreter_.getText().trim();
      
      // for project preferences, use project-relative path to interpreter
      if (isProjectPrefs)
      {
         FileSystemItem projDir = session_.getSessionInfo().getActiveProjectDir();
         if (projDir.exists() && newValue.startsWith(projDir.getPath()))
            newValue = newValue.substring(projDir.getLength() + 1);
      }
      else
      {
         boolean newAutoActivateValue = cbAutoUseProjectInterpreter_.getValue();
         if (newAutoActivateValue != initialAutoUseProjectInterpreter_)
         {
            prefs_.pythonProjectEnvironmentAutomaticActivate().setGlobalValue(newAutoActivateValue);
            requirement.setSessionRestartRequired(true);
         }
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

   protected final InfoBar mismatchWarningBar_;
   protected final TextBoxWithButton tbPythonInterpreter_;
   protected final SimplePanel interpreterDescription_ = new SimplePanel();
   
   protected CheckBox cbAutoUseProjectInterpreter_;
   protected boolean initialAutoUseProjectInterpreter_;
   
   protected String initialPythonPath_;
   protected PythonInterpreter interpreter_;
   
   protected boolean updatingDescription_;

   protected PythonDialogResources res_;
   protected PythonServerOperations server_;
   protected Session session_;
   protected UserPrefs prefs_;
   
   private String lastValue_ = null;
   
   
   protected static Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);

}
