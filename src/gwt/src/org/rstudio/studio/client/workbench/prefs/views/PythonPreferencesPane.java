/*
 * PythonPreferencesPane.java
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
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.InfoBar;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithButton;
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

public class PythonPreferencesPane extends PreferencesPane
{
   @Inject
   public PythonPreferencesPane(PythonDialogResources res,
                                PythonServerOperations server)
   {
      res_ = res;
      server_ = server;
      
      add(headerLabel("Python"));
      
      tbPythonInterpreter_ = new TextBoxWithButton(
            "Python interpreter:",
            "(No interpreter selected)",
            "Select...",
            null,
            ElementIds.TextBoxButtonId.PYTHON_DEFAULT_INTERPRETER,
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
      tbPythonInterpreter_.setText(PYTHON_PLACEHOLDER_TEXT);
      tbPythonInterpreter_.setReadOnly(false);
      add(lessSpaced(tbPythonInterpreter_));
      
      add(spaced(container_));
      
   }
   
   private void clearDescription()
   {
      container_.setWidget(new FlowPanel());
   }
   
   private void updateDescription()
   {
      String path = tbPythonInterpreter_.getText();
      if (StringUtil.isNullOrEmpty(path) ||
          StringUtil.equals(path, PYTHON_PLACEHOLDER_TEXT))
      {
         clearDescription();
         return;
      }
      
      server_.pythonDescribeInterpreter(
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
   
   private void updateDescription(PythonInterpreter info)
   {
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

   @Override
   protected void initialize(UserPrefs prefs)
   {
      String pythonPath = prefs.pythonDefaultInterpreter().getValue();
      if (!StringUtil.isNullOrEmpty(pythonPath))
         tbPythonInterpreter_.setText(pythonPath);
   }
   
   @Override
   public RestartRequirement onApply(UserPrefs prefs)
   {
      RestartRequirement requirement = super.onApply(prefs);
      
      String oldValue = prefs.pythonDefaultInterpreter().getGlobalValue();
      String newValue = tbPythonInterpreter_.getText();
      
      boolean isSet =
            !StringUtil.isNullOrEmpty(newValue) &&
            !StringUtil.equals(newValue, PYTHON_PLACEHOLDER_TEXT);
      
      if (isSet && !StringUtil.equals(oldValue, newValue))
      {
         prefs.pythonDefaultInterpreter().setGlobalValue(newValue);
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

   private final PythonDialogResources res_;
   private final PythonServerOperations server_;
   private final TextBoxWithButton tbPythonInterpreter_;
   private final SimplePanel container_ = new SimplePanel();
   
   private static final String PYTHON_PLACEHOLDER_TEXT = "(No interpreted selected)";

   private static Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }

 
}
