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

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.prefs.PreferencesDialogPaneBase;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.InfoBar;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.views.python.PythonInterpreterListEntryUi;
import org.rstudio.studio.client.workbench.prefs.views.python.PythonInterpreterSelectionDialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

public abstract class PythonPreferencesPaneBase<T> extends PreferencesDialogPaneBase<T>
{
   public PythonPreferencesPaneBase(String placeholderText)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      placeholderText_ = placeholderText;
      
      add(headerLabel("Python"));
      
      mismatchWarningBar_ = new InfoBar(InfoBar.WARNING);
      mismatchWarningBar_.setText(
            "The active Python interpreter has been changed by an R startup script.");
      mismatchWarningBar_.setVisible(false);
      add(spaced(mismatchWarningBar_));
      
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
      
      tbPythonInterpreter_.addDomHandler((BlurEvent event) ->
      {
         updateDescription();
      }, BlurEvent.getType());
      
      tbPythonInterpreter_.setWidth("420px");
      tbPythonInterpreter_.setText(placeholderText_);
      tbPythonInterpreter_.setReadOnly(false);
      add(lessSpaced(tbPythonInterpreter_));
      
      add(spaced(container_));
      
   }
   
   @Inject
   private void initialize(PythonDialogResources res,
                           PythonServerOperations server,
                           UserPrefs prefs)
   {
      res_ = res;
      server_ = server;
      prefs_ = prefs;
   }
   
   protected void clearDescription()
   {
      container_.setWidget(new FlowPanel());
   }
   
   protected void updateDescription()
   {
      String path = tbPythonInterpreter_.getText();
      if (StringUtil.isNullOrEmpty(path) ||
          StringUtil.equals(path, placeholderText_))
      {
         clearDescription();
         return;
      }
      
      server_.pythonInterpreterInfo(
            tbPythonInterpreter_.getText(),
            new ServerRequestCallback<PythonInterpreter>()
            {
               @Override
               public void onResponseReceived(PythonInterpreter info)
               {
                  updateDescription(info);
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   protected void updateDescription(PythonInterpreter info)
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
         ui.addStyleName(RES.styles().description());
         
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
         mismatchWarningBar_.setVisible(false);
         return;
      }
      
      // nothing to do if the user hasn't changed the configured Python
      String requestedPath = tbPythonInterpreter_.getText();
      boolean isSet =
            !StringUtil.isNullOrEmpty(requestedPath) &&
            !StringUtil.equals(requestedPath, placeholderText_);
      
      if (!isSet)
      {
         mismatchWarningBar_.setVisible(false);
         return;
      }
      
      // toggle visibility
      boolean mismatch =
            !StringUtil.equals(requestedPath, activeInterpreter.getPath());
      
      mismatchWarningBar_.setVisible(mismatch);
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
   
   protected void initialize(boolean useProjectPrefs)
   {
      String pythonPath = useProjectPrefs
            ? prefs_.pythonPath().getValue()
            : prefs_.pythonPath().getGlobalValue();
      
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
   
   public RestartRequirement onApply(boolean useProjectPrefs)
   {
      RestartRequirement requirement = new RestartRequirement();
      
      String oldValue = useProjectPrefs
            ? prefs_.pythonPath().getValue()
            : prefs_.pythonPath().getGlobalValue();
            
      String newValue = tbPythonInterpreter_.getText();
      
      boolean isSet =
            interpreter_ != null &&
            interpreter_.isValid() &&
            !StringUtil.isNullOrEmpty(newValue) &&
            !StringUtil.equals(newValue, placeholderText_);
      
      if (isSet && !StringUtil.equals(oldValue, newValue))
      {
         if (useProjectPrefs)
         {
            prefs_.pythonType().setProjectValue(interpreter_.getType());
            prefs_.pythonVersion().setProjectValue(interpreter_.getVersion());
            prefs_.pythonPath().setProjectValue(interpreter_.getPath());
         }
         else
         {
            prefs_.pythonType().setGlobalValue(interpreter_.getType());
            prefs_.pythonVersion().setGlobalValue(interpreter_.getVersion());
            prefs_.pythonPath().setGlobalValue(interpreter_.getPath());
            
         }
         requirement.setSessionRestartRequired(true);
      }
      
      return requirement;
   }
   

   public interface Styles extends CssResource
   {
      String description();
      String invalid();
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
   
   protected PythonInterpreter interpreter_;

   protected PythonDialogResources res_;
   protected PythonServerOperations server_;
   protected UserPrefs prefs_;
   
   
   protected static Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }

 
}
