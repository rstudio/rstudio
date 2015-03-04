/*
 * RSConnectDeploy.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.rsconnect.ui;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.model.RSConnectAccount;
import org.rstudio.studio.client.rsconnect.model.RSConnectApplicationInfo;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class RSConnectDeploy extends Composite
{
   private static RSConnectDeployUiBinder uiBinder = GWT
         .create(RSConnectDeployUiBinder.class);

   interface RSConnectDeployUiBinder extends UiBinder<Widget, RSConnectDeploy>
   {
   }
   
   public interface DeployStyle extends CssResource
   {
      String accountAnchor();
      String accountList();
      String controlLabel();
      String deployLabel();
      String dropListControl();
      String fileList();
      String firstControlLabel();
      String gridControl();
      String launchCheck();
      String normalStatus();
      String otherStatus();
      String rootCell();
      String source();
      String sourceDestLabels();
      String statusLabel();
      String transferArrow();
      String urlAnchor();
      String validateError();
   }
   
   public interface DeployResources extends ClientBundle
   {  
      ImageResource publishShinyIllustration();
      ImageResource publishRmdIllustration();
      ImageResource publishPlotIllustration();

      @Source("RSConnectDeploy.css")
      DeployStyle style();
   }
   
   public static DeployResources RESOURCES = GWT.create(DeployResources.class);
   
   public RSConnectDeploy(final RSConnectServerOperations server, 
                          final RSAccountConnector connector,    
                          final GlobalDisplay display,
                          final Session session, 
                          boolean forDocument)
   {
      forDocument_ = forDocument;
      accountList = new RSConnectAccountList(server, display, false);
      initWidget(uiBinder.createAndBindUi(this));
      style_ = RESOURCES.style();
      
      deployIllustration_.setResource(forDocument ?
                           RESOURCES.publishRmdIllustration() :
                           RESOURCES.publishShinyIllustration());

      // Validate the application name on every keystroke
      appName.addKeyUpHandler(new KeyUpHandler()
      {
         @Override
         public void onKeyUp(KeyUpEvent event)
         {
            validateAppName();
         }
      });
      // Invoke the "add account" wizard
      addAccountAnchor.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            connector.showAccountWizard(false, new OperationWithInput<Boolean>() 
            {
               @Override
               public void execute(Boolean successful)
               {
                  if (successful)
                  {
                     accountList.refreshAccountList();
                  }
               }
            });
            
            event.preventDefault();
            event.stopPropagation();
         }
      });
      addFileButton_.setVisible(forDocument);
      addFileButton_.getElement().getStyle().setMarginLeft(0, Unit.PX);
      addFileButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent arg0)
         {
            if (onFileAddClick_ != null) 
            {
               onFileAddClick_.execute();
            }
         }
      });
   }
   
   public void setSourceDir(String dir)
   {
      dir = StringUtil.shortPathName(FileSystemItem.createDir(dir), 250);
      deployLabel_.setText(dir);
   }
   
   public void setNewAppName(String name)
   {
      appName.setText(name);
   }

   public void setDefaultAccount(RSConnectAccount account)
   {
      accountList.selectAccount(account);
   }
   
   public void setAccountList(JsArray<RSConnectAccount> accounts)
   {
      accountList.setAccountList(accounts);
   }
   
   public RSConnectAccount getSelectedAccount()
   {
      return accountList.getSelectedAccount();
   }
   
   public String getSelectedApp()
   {
      int idx = appList.getSelectedIndex();
      return idx >= 0 ? 
            appList.getItemText(idx) :
            null;
   }
   
   public void setAppList(List<String> apps, String selected)
   {
      appList.clear();
      int selectedIdx = 0;
      if (apps != null)
      {
         selectedIdx = Math.max(0, apps.size() - 1);
         
         for (int i = 0; i < apps.size(); i++)
         {
            String app = apps.get(i);
            appList.addItem(app);
            if (app.equals(selected))
            {
               selectedIdx = i;
            }
         }
      }
      appList.addItem("Create New");
      appList.setSelectedIndex(selectedIdx);
   }
   
   public void setFileList(JsArrayString files, String[] unchecked)
   {
      if (forDocument_)
      {
         fileChecks_ = new ArrayList<CheckBox>();
      }
      
      for (int i = 0; i < files.length(); i++)
      {
         boolean checked = true;
         if (unchecked != null)
         {
            for (int j = 0; j < unchecked.length; j++)
            {
               if (unchecked[j].equals(files.get(i)))
               {
                  checked = false; 
                  break;
               }
            }
         }
         addFile(files.get(i), checked);
      }
   }
   
   public void addFileToList(String path)
   {
      addFile(path, true);
   }
   
   public void setFileCheckEnabled(String path, boolean enabled)
   {
      if (fileChecks_ == null)
         return;

      for (int i = 0; i < fileChecks_.size(); i++)
      {
         if (fileChecks_.get(i).getText().equals(path))
         {
            fileChecks_.get(i).setEnabled(enabled);
         }
      }
   }
   
   public ArrayList<String> getFileList()
   {
      return getCheckedFileList(true);
   }
   
   public ArrayList<String> getIgnoredFileList()
   {
      return getCheckedFileList(false);
   }
   
   public String getNewAppName()
   {
      return appName.getText();
   }
   
   public void showAppInfo(RSConnectApplicationInfo info)
   {
      if (info == null)
      {
         appInfoPanel.setVisible(false);
         nameLabel.setVisible(true);
         appName.setVisible(true);
         validateAppName();
         return;
      }

      setAppNameValid(true);
      urlAnchor.setText(info.getUrl());
      urlAnchor.setHref(info.getUrl());
      String status = info.getStatus();
      statusLabel.setText(status);
      statusLabel.setStyleName(style_.statusLabel() + " " + 
              (status.equals("running") ?
                    style_.normalStatus() :
                    style_.otherStatus()));

      appInfoPanel.setVisible(true);
      nameLabel.setVisible(false);
      appName.setVisible(false);
      nameValidatePanel.setVisible(false);
   }
   
   public HandlerRegistration addAccountChangeHandler(ChangeHandler handler)
   {
      return accountList.addChangeHandler(handler);
   }

   public HandlerRegistration addAppChangeHandler(ChangeHandler handler)
   {
      return appList.addChangeHandler(handler);
   }
   
   public void setOnDeployEnabled(Command cmd)
   {
      onDeployEnabled_ = cmd;
   }
   
   public void setOnDeployDisabled(Command cmd)
   {
      onDeployDisabled_ = cmd;
   }
   
   public void setOnFileAddClick(Command cmd)
   {
      onFileAddClick_ = cmd;
   }
   
   public DeployStyle getStyle()
   {
      return style_;
   }
   
   private void validateAppName()
   {
      String app = appName.getText();
      RegExp validReg = RegExp.compile("^[A-Za-z0-9_-]{4,63}$");
      setAppNameValid(validReg.test(app));
   }
   
   private void setAppNameValid(boolean isValid)
   {
      nameValidatePanel.setVisible(!isValid);
      if (isValid && onDeployEnabled_ != null)
         onDeployEnabled_.execute();
      else if (!isValid && onDeployDisabled_ != null)
         onDeployDisabled_.execute();
   }

   private void addFile(String path, boolean checked)
   {
      if (forDocument_)
      {
         CheckBox fileCheck = new CheckBox(path);
         fileCheck.setValue(checked);
         fileListPanel_.add(fileCheck);
         fileChecks_.add(fileCheck);
      }
      else
      {
         fileListPanel_.add(new Label(path));
      }
   }
   
   private ArrayList<String> getCheckedFileList(boolean checked)
   {
      ArrayList<String> files = new ArrayList<String>();
      if (fileChecks_ == null)
         return files;
      for (int i = 0; i < fileChecks_.size(); i++)
      {
         if (fileChecks_.get(i).getValue() == checked)
         {
            files.add(fileChecks_.get(i).getText());
         }
      }
      return files;
   }
   
   @UiField Image deployIllustration_;
   @UiField Anchor urlAnchor;
   @UiField Anchor addAccountAnchor;
   @UiField Label nameLabel;
   @UiField InlineLabel statusLabel;
   @UiField(provided=true) RSConnectAccountList accountList;
   @UiField ListBox appList;
   @UiField TextBox appName;
   @UiField HTMLPanel appInfoPanel;
   @UiField HTMLPanel nameValidatePanel;
   @UiField VerticalPanel fileListPanel_;
   @UiField InlineLabel deployLabel_;
   @UiField ThemedButton addFileButton_;
   
   private ArrayList<CheckBox> fileChecks_;
   
   private Command onDeployEnabled_;
   private Command onDeployDisabled_;
   private Command onFileAddClick_;

   private final DeployStyle style_;
   private final boolean forDocument_;
}
