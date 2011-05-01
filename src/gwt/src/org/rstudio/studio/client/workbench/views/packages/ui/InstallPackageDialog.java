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

import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.MultipleItemSuggestTextBox;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallContext;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallOptions;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallRequest;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;


public class InstallPackageDialog extends ModalDialog<PackageInstallRequest>
{
   public InstallPackageDialog(
                           PackageInstallContext installContext,
                           PackageInstallOptions defaultInstallOptions,
                           PackagesServerOperations server,
                           GlobalDisplay globalDisplay,
                           OperationWithInput<PackageInstallRequest> operation)
{
      super("Install Packages", operation);
      
      installContext_ = installContext;
      defaultInstallOptions_ = defaultInstallOptions;
      server_ = server;
      globalDisplay_ = globalDisplay;

      setOkButtonCaption("Install");
}

  
   @Override
   protected PackageInstallRequest collectInput()
   {
      // packages
      List<String> packages = packagesTextBox_.getItems();
      
      // library
      String libraryPath = installContext_.getWriteableLibraryPaths().get(
                                          libraryListBox_.getSelectedIndex());
      
      // install dependencies
      boolean installDependencies = installDependenciesCheckBox_.getValue();
      
      return new PackageInstallRequest(packages, 
                                       PackageInstallOptions.create(
                                                         libraryPath, 
                                                         installDependencies)); 
   }
   
   
   @Override
   protected boolean validate(PackageInstallRequest request)
   {
      // check for package name
      if (request.getPackages().size() == 0)
      {
         globalDisplay_.showErrorMessage(
               "Package Name Required", 
               "You must provide the name of the packages to install.",
               packagesSuggestBox_);
         
         return false;
      }
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
      mainPanel.setSpacing(2);
      mainPanel.setStylePrimaryName(RESOURCES.styles().mainWidget());
      
      mainPanel.add(new Label("Packages (separate multiple with space or comma):"));
      
      packagesTextBox_ = new MultipleItemSuggestTextBox();
      packagesSuggestBox_ = new SuggestBox(new PackageOracle(),
                                           packagesTextBox_);
      packagesSuggestBox_.setWidth("100%");
      packagesSuggestBox_.setLimit(20);
      packagesSuggestBox_.addStyleName(RESOURCES.styles().extraBottomPad());
      mainPanel.add(packagesSuggestBox_);
         
      
      mainPanel.add(new Label("Install to Library:"));
      
      // library list box
      libraryListBox_ = new ListBox();
      libraryListBox_.setWidth("100%");
      libraryListBox_.addStyleName(RESOURCES.styles().extraBottomPad());
      JsArrayString libPaths = installContext_.getWriteableLibraryPaths();
      int selectedIndex = 0;
      for (int i=0; i<libPaths.length(); i++)
      {
         String libPath = libPaths.get(i);
         
         if (defaultInstallOptions_.getLibraryPath().equals(libPath))
            selectedIndex = i;
         
         if (libPath.equals(installContext_.getDefaultLibraryPath()))
            libPath = libPath + " [Default]";
         
         libraryListBox_.addItem(libPath);
        
      }
      libraryListBox_.setSelectedIndex(selectedIndex); 
      mainPanel.add(libraryListBox_);
      
      // install dependencies check box
      installDependenciesCheckBox_ = new CheckBox();
      installDependenciesCheckBox_.addStyleName(RESOURCES.styles().installDependenciesCheckBox());
      installDependenciesCheckBox_.setText("Install dependencies");
      installDependenciesCheckBox_.setValue(
                           defaultInstallOptions_.getInstallDependencies());
      mainPanel.add(installDependenciesCheckBox_);
      
      mainPanel.add(new HTML("<br/>"));
      
      return mainPanel;
   }
   
   @Override
   protected void onDialogShown()
   {
      FocusHelper.setFocusDeferred(packagesSuggestBox_);
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
   }
   
   static interface Styles extends CssResource
   {
      String mainWidget();
      String extraBottomPad();
      String installDependenciesCheckBox();
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
   
   private final PackageInstallContext installContext_;
   private final PackageInstallOptions defaultInstallOptions_;
   private final PackagesServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   
   private MultipleItemSuggestTextBox packagesTextBox_ = null;
   private SuggestBox packagesSuggestBox_ = null;
   private ListBox libraryListBox_ = null;
   private CheckBox installDependenciesCheckBox_ = null;
   
}
