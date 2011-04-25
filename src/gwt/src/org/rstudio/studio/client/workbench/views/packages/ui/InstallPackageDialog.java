/*
 * InstallPackageDialog.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.packages.ui;


import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.packages.model.InstallOptions;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallContext;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;


public class InstallPackageDialog extends ModalDialog<InstallOptions>
{
   public InstallPackageDialog(String installRepository,
                               PackageInstallContext installContext,
                               PackagesServerOperations server,
                               GlobalDisplay globalDisplay,
                               OperationWithInput<InstallOptions> operation)
   {
      super("Install Package", operation);
      initialInstallRepository_ = installRepository;
      installContext_ = installContext;
      server_ = server;
      globalDisplay_ = globalDisplay;
      
      setOkButtonCaption("Install");
   }
   
   @Override
   protected InstallOptions collectInput()
   {
      String packageName = packageNameSuggestBox_.getText().trim();
      return InstallOptions.create(installRepository(), packageName);
   }
   
   @Override
   protected boolean validate(InstallOptions options)
   {
      // check for a repository
      if (options.getRepository() != null && 
          options.getRepository().length() == 0)
      {
         globalDisplay_.showErrorMessage(
               "Repository URL Required",
               "You must provide the URL of the repository to install " +
               "the package from.",
               repositoryURLTextBox_);
   
         return false;
      }
      
      // check for package name
      else if (options.getPackageName().length() == 0)
      {
         globalDisplay_.showErrorMessage(
               "Package Name Required", 
               "You must provide the name of the package to install.",
               packageNameSuggestBox_);
         
         return false;
      }
      
      // input is valid!
      else
      {
         return true;
      }
   }

   @Override
   protected Widget createMainWidget()
   {  
      // vertical panel
      VerticalPanel mainPanel = new VerticalPanel();
      mainPanel.setSpacing(5);
      mainPanel.setStylePrimaryName(RESOURCES.styles().mainWidget());
      
      // grid hosts main UI
      Grid grid = new Grid(2,2);
      grid.setWidth("100%");
      
      // repository
      grid.setWidget(0, 0, new Label("Repository:"));
      repositoryListBox_ = new ListBox();
      repositoryListBox_.setStylePrimaryName(RESOURCES.styles().repositoryListBox());
      repositoryListBox_.addItem("CRAN");
      repositoryListBox_.addItem("Other Repository");
      repositoryListBox_.setVisibleItemCount(1);  
      
      grid.setWidget(1, 0, repositoryListBox_);
      
      // package name
      grid.setWidget(0, 1, new Label("Package name:"));
      packageNameSuggestBox_ = new SuggestBox(new PackageOracle());
      packageNameSuggestBox_.setWidth("100%");
      packageNameSuggestBox_.setLimit(20);
      grid.setWidget(1, 1, packageNameSuggestBox_);
      repositoryListBox_.addChangeHandler(new ChangeHandler() {
         public void onChange(ChangeEvent event)
         {
            if (repositoryListBox_.getSelectedIndex() == 0)
            {
               repositoryURLGrid_.setVisible(false);
               packageNameSuggestBox_.setFocus(true);
            }
            else
            {
               repositoryURLGrid_.setVisible(true);
               repositoryURLTextBox_.setFocus(true);  
            }  
         }   
      });
      
      // add grid to main panel
      mainPanel.add(grid);
      
      // other repository grid
      repositoryURLGrid_ = new Grid(2, 1);
      repositoryURLGrid_.setWidget(0, 0, new Label("Repository URL:"));
      repositoryURLTextBox_ = new TextBox();
      repositoryURLTextBox_.setStylePrimaryName(RESOURCES.styles().repositoryURLTextBox());
      repositoryURLGrid_.setWidget(1, 0, repositoryURLTextBox_);
      mainPanel.add(repositoryURLGrid_);
      repositoryURLGrid_.setVisible(false);
      
      // initial UI based on initial install repository
      if (initialInstallRepository_ == null)
      {
         repositoryListBox_.setSelectedIndex(0);
         repositoryURLGrid_.setVisible(false);
      }
      else
      {
         repositoryListBox_.setSelectedIndex(1);
         repositoryURLGrid_.setVisible(true);
         repositoryURLTextBox_.setText(initialInstallRepository_);
      }
      
      // return the widget
      return mainPanel;
   }
   
   @Override
   protected void onDialogShown()
   {
      FocusHelper.setFocusDeferred(packageNameSuggestBox_);
   }

   private class PackageOracle extends MultiWordSuggestOracle
   {
      PackageOracle()
      {
         // no separators (strict prefix match)
         super("");
         
         server_.availablePackages(null,
                                   new ServerRequestCallback<JsArrayString>() {
            @Override
            public void onResponseReceived(JsArrayString packages)
            {
               for (int i=0; i<packages.length(); i++)
                  add(packages.get(i));
            }

            @Override
            public void onError(ServerError error)
            {
               Debug.log("Error querying for packages: " + 
                         error.getUserMessage());
            }  
         });
      }
      
      @Override
      public void requestSuggestions(SuggestOracle.Request request, 
                                     SuggestOracle.Callback callback) 
      {
         // only return suggestions for CRAN
         if (installRepository() == null)
            super.requestSuggestions(request, callback);
      }
   }
  
   private String installRepository()
   {
      String repos = null;
      if (repositoryListBox_.getSelectedIndex() == 1)
         repos = repositoryURLTextBox_.getText().trim();
      return repos;
   }
   
   
   private ListBox repositoryListBox_;
   private SuggestBox packageNameSuggestBox_;
   private Grid repositoryURLGrid_;
   private TextBox repositoryURLTextBox_;
  
   private final String initialInstallRepository_ ;
   @SuppressWarnings("unused")
   private final PackageInstallContext installContext_;
   private final PackagesServerOperations server_;
   private final GlobalDisplay globalDisplay_;
  
   static interface Styles extends CssResource
   {
      String mainWidget();
      String repositoryListBox();
      String repositoryURLTextBox();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("InstallPackageDialog.css")
      Styles styles();
   }
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
}
