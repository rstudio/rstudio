/*
 * InstallPackageDialog.java
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
package org.rstudio.studio.client.workbench.views.packages.ui;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemContext;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.CaptionWithHelp;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.MultipleItemSuggestTextBox;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallContext;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallOptions;
import org.rstudio.studio.client.workbench.views.packages.model.PackageInstallRequest;
import org.rstudio.studio.client.workbench.views.packages.model.PackagesServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.SimplePanel;
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
      super("Install Packages", Roles.getDialogRole(), operation);
      
      installContext_ = installContext;
      defaultInstallOptions_ = defaultInstallOptions;
      server_ = server;
      globalDisplay_ = globalDisplay;

      setOkButtonCaption("Install");
}

  
   @Override
   protected PackageInstallRequest collectInput()
   {
      // package install options
      String libraryPath = installContext_.getWriteableLibraryPaths().get(
                                          libraryListBox_.getSelectedIndex());
      boolean installDependencies = installDependenciesCheckBox_.getValue();
      PackageInstallOptions options =  PackageInstallOptions.create(
                                                   installFromRepository(),
                                                   libraryPath, 
                                                   installDependencies); 
      
      if (installFromRepository())
      {
         return new PackageInstallRequest(packagesTextBox_.getItems(), options);
      }
      else
      {
         return new PackageInstallRequest(archiveFilePath_, options);
      }     
   }
   
   
   @Override
   protected boolean validate(PackageInstallRequest request)
   {
      // check for package name
      if (installFromRepository() && (request.getPackages().size() == 0) ||
          !installFromRepository() && request.getLocalPackage() == null)
      {
         globalDisplay_.showErrorMessage(
               "No Package Selected", 
               "You must specify the package to install.",
               getPackageInputWidget());
         
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
      
      // source type
      packageSourceListBox_ = new ListBox();
      packageSourceListBox_.setStylePrimaryName(RESOURCES.styles().packageSourceListBox());
      packageSourceListBox_.addStyleName(RESOURCES.styles().extraBottomPad());
      JsArrayString repos = installContext_.selectedRepositoryNames();
      if (repos.length() == 1)
      {
         packageSourceListBox_.addItem("Repository (" + repos.get(0) + ")");
      }
      else
      {
         StringBuilder reposItem = new StringBuilder();
         reposItem.append("Repository (");
         for (int i=0; i<repos.length(); i++)
         {
            if (i != 0)
               reposItem.append(", ");
            reposItem.append(repos.get(i));
         }
         reposItem.append(")");
         packageSourceListBox_.addItem(reposItem.toString());
      }
      packageSourceListBox_.addItem("Package Archive File (" + 
                                    installContext_.packageArchiveExtension() +
                                    ")");
      reposCaption_ = new CaptionWithHelp("Install from:",
                                          "Configuring Repositories",
                                          "configuring_repositories",
                                          packageSourceListBox_);
      reposCaption_.setIncludeVersionInfo(false);
      reposCaption_.setWidth("100%");
      mainPanel.add(reposCaption_); 
      mainPanel.add(packageSourceListBox_);
      
      // source panel container
      sourcePanel_ = new SimplePanel();
      sourcePanel_.setStylePrimaryName(RESOURCES.styles().packageSourcePanel());
      
      // repos source panel
      reposSourcePanel_ = new FlowPanel();
      packagesTextBox_ = new MultipleItemSuggestTextBox();
      packagesSuggestBox_ = new SuggestBox(new PackageOracle(),
                                           packagesTextBox_);
      packagesSuggestBox_.setWidth("100%");
      packagesSuggestBox_.setLimit(20);
      packagesSuggestBox_.addStyleName(RESOURCES.styles().extraBottomPad());
      FormLabel packagesLabel = new FormLabel(
                      "Packages (separate multiple with space or comma):", packagesSuggestBox_);
      packagesLabel.setStylePrimaryName(RESOURCES.styles().packagesLabel());
      reposSourcePanel_.add(packagesLabel);
      reposSourcePanel_.add(packagesSuggestBox_);
      sourcePanel_.setWidget(reposSourcePanel_);
      mainPanel.add(sourcePanel_);
         
      // archive source panel
      packageArchiveFile_ = new TextBoxWithButton(
                                              "Package archive:",
                                              "",
                                              "Browse...",
                                              null,
                                              ElementIds.TextBoxButtonId.PACKAGE_ARCHIVE,
                                              true,
                                              browseForArchiveClickHandler_);
            
      // create check box here because manageUIState accesses it
      installDependenciesCheckBox_ = new CheckBox();
      
      if (defaultInstallOptions_.getInstallFromRepository())
         packageSourceListBox_.setSelectedIndex(0);
      else
         packageSourceListBox_.setSelectedIndex(1);
      manageUIState();
      
      packageSourceListBox_.addChangeHandler(new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            manageUIState();
            
            if (!installFromRepository())
               packageArchiveFile_.click();
         } 
      });
      
      // library list box
      libraryListBox_ = new ListBox();
      libraryListBox_.setWidth("100%");
      libraryListBox_.addStyleName(RESOURCES.styles().extraBottomPad());
      JsArrayString libPaths = installContext_.getWriteableLibraryPaths();
      int selectedIndex = 0;
      for (int i=0; i<libPaths.length(); i++)
      {
         String libPath = libPaths.get(i);
         
         if (!installContext_.isDevModeOn())
         {
            if (defaultInstallOptions_.getLibraryPath() == libPath)
               selectedIndex = i;
         }
         
         if (libPath == installContext_.getDefaultLibraryPath())
            libPath = libPath + " [Default]";
         
         libraryListBox_.addItem(libPath);
        
      }
      libraryListBox_.setSelectedIndex(selectedIndex); 

      FormLabel libraryListLabel = new FormLabel("Install to Library:", libraryListBox_);
      mainPanel.add(libraryListLabel);
      mainPanel.add(libraryListBox_);
      
      // install dependencies check box
      installDependenciesCheckBox_.addStyleName(RESOURCES.styles().installDependenciesCheckBox());
      installDependenciesCheckBox_.setText("Install dependencies");
      installDependenciesCheckBox_.setValue(
                           defaultInstallOptions_.getInstallDependencies());
      mainPanel.add(installDependenciesCheckBox_);
      
      mainPanel.add(new HTML("<br/>"));
      
      return mainPanel;
   }
   
   @Override
   protected void focusInitialControl()
   {
      if (installFromRepository())
         FocusHelper.setFocusDeferred(packagesSuggestBox_);
      else
         FocusHelper.setFocusDeferred(packageSourceListBox_);
   }
   
   private void manageUIState()
   {
      if (installFromRepository())
      {
         reposCaption_.setHelpVisible(true);
         sourcePanel_.setWidget(reposSourcePanel_);
         FocusHelper.setFocusDeferred(packagesSuggestBox_);
         installDependenciesCheckBox_.setVisible(true);
      }
      else
      {
         reposCaption_.setHelpVisible(false);
         sourcePanel_.setWidget(packageArchiveFile_);
         FocusHelper.setFocusDeferred(packageArchiveFile_);
         installDependenciesCheckBox_.setVisible(false);
      }
   }
   
   private boolean installFromRepository()
   {
      return packageSourceListBox_.getSelectedIndex() == 0;
   }
   
   private CanFocus getPackageInputWidget()
   {
      if (installFromRepository())
         return new CanFocus() {
            @Override
            public void focus()
            {
               packagesSuggestBox_.setFocus(true);
               
            }};
      else
         return packageArchiveFile_;
   }
   
   private ClickHandler browseForArchiveClickHandler_ = new ClickHandler() {

      @Override
      public void onClick(ClickEvent event)
      {
         fileDialogs_.openFile(
               "Select Package Archive",
               fileSystemContext_,
               defaultArchiveDir_,
               new ProgressOperationWithInput<FileSystemItem>()
               {
                  public void execute(FileSystemItem input, 
                                      ProgressIndicator indicator)
                  {
                     indicator.onCompleted();
                     
                     if (input == null)
                        return;
                     
                     // update default archive dir
                     defaultArchiveDir_ = input.getParentPath();
                     
                     // set archive file path
                     archiveFilePath_ = input;
                     
                     // update UI
                     packageArchiveFile_.setText(
                        StringUtil.shortPathName(input, "gwt-TextBox", 280));
                  }
               });
         
      }
      
   };
   
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
      String packageSourcePanel();
      String configureRepositoriesImage();
      String configureRepositoriesLink();
      String packagesLabel();
      String extraBottomPad();
      String installDependenciesCheckBox();
      String packageSourceListBox();
      String packageFileTextBox();
      String packageFileBrowseButton();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("InstallPackageDialog.css")
      Styles styles();
   }
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
   
   private final PackageInstallContext installContext_;
   private final PackageInstallOptions defaultInstallOptions_;
   private final PackagesServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   
   private CaptionWithHelp reposCaption_;
   private ListBox packageSourceListBox_;
   
   private SimplePanel sourcePanel_;
   private FlowPanel reposSourcePanel_;
   private TextBoxWithButton packageArchiveFile_ = null;
   
   private MultipleItemSuggestTextBox packagesTextBox_ = null;
   private SuggestBox packagesSuggestBox_ = null;
   private ListBox libraryListBox_ = null;
   private CheckBox installDependenciesCheckBox_ = null;
   
   FileSystemItem archiveFilePath_ = null;
  
   private static FileSystemItem defaultArchiveDir_ = FileSystemItem.home();
   
   private final FileSystemContext fileSystemContext_ =
      RStudioGinjector.INSTANCE.getRemoteFileSystemContext();
   
   private final FileDialogs fileDialogs_ = 
      RStudioGinjector.INSTANCE.getFileDialogs();
}
