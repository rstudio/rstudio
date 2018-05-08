/*
 * SecondaryReposDialog.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
package org.rstudio.studio.client.common.repos;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SimplePanelWithProgress;
import org.rstudio.core.client.widget.images.ProgressImages;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.mirrors.model.CRANMirror;
import org.rstudio.studio.client.common.repos.model.SecondaryReposServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.Void;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.google.inject.Inject;

public class SecondaryReposDialog extends ModalDialog<CRANMirror>
{
   public SecondaryReposDialog(OperationWithInput<CRANMirror> operation,
                               ArrayList<String> excluded)
   {
      super("Retrieving list of secondary repos...", operation);
      
      excluded_ = excluded;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Override
   protected CRANMirror collectInput()
   {
      if (!StringUtil.isNullOrEmpty(nameTextBox_.getText()) &&
          !StringUtil.isNullOrEmpty(urlTextBox_.getText()))
      {
         CRANMirror cranMirror = CRANMirror.empty();

         cranMirror.setName(nameTextBox_.getText());
         cranMirror.setURL(urlTextBox_.getText());

         cranMirror.setHost("Custom");

         return cranMirror;
      }
      else if (listBox_ != null && listBox_.getSelectedIndex() >= 0)
      {
         return repos_.get(listBox_.getSelectedIndex());
      }
      else
      {
         return null;
      }
   }

   @Inject
   void initialize(GlobalDisplay globalDisplay,
                   SecondaryReposServerOperations server)
   {
      globalDisplay_ = globalDisplay;
      secondaryReposServer_ = server;
   }

   @Override
   protected boolean validate(CRANMirror input)
   {
      if (input == null)
      {
         globalDisplay_.showErrorMessage("Error", 
                                         "Please select or input a CRAN repo");
         return false;
      }
      
      
      if (excluded_.contains(input.getName()))
      {
         globalDisplay_.showErrorMessage("Error", 
               "The repo " + input.getName() + " is already included");
         return false;
      }
      
      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel root = new VerticalPanel();

      HorizontalPanel customPanel = new HorizontalPanel();
      customPanel.setStylePrimaryName(RESOURCES.styles().customPanel());
      root.add(customPanel);

      VerticalPanel namePanel = new VerticalPanel();
      namePanel.setStylePrimaryName(RESOURCES.styles().namePanel());
      Label nameLabel = new Label("Name:");
      namePanel.add(nameLabel);
      nameTextBox_ = new TextBox();
      namePanel.add(nameTextBox_);
      customPanel.add(namePanel);

      VerticalPanel urlPanel = new VerticalPanel();
      Label urlLabel = new Label("Url:");
      urlPanel.add(urlLabel);
      urlTextBox_ = new TextBox();
      urlTextBox_.setStylePrimaryName(RESOURCES.styles().urlTextBox());
      urlPanel.add(urlTextBox_);
      customPanel.add(urlPanel);

      Label reposLabel = new Label("Available repos:");
      reposLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      root.add(reposLabel);

      final SimplePanelWithProgress panel = new SimplePanelWithProgress(
         ProgressImages.createLargeGray());
      root.add(panel);

      panel.setStylePrimaryName(RESOURCES.styles().mainWidget());
         
      // show progress (with delay)
      panel.showProgress(200);
      
      // query data source for packages
      secondaryReposServer_.getSecondaryRepos(new SimpleRequestCallback<JsArray<CRANMirror>>() {

         @Override 
         public void onResponseReceived(JsArray<CRANMirror> repos)
         {   
            // keep internal list of mirrors 
            repos_ = new ArrayList<CRANMirror>(repos.length());
            
            // create list box and select default item
            listBox_ = new ListBox();
            listBox_.setMultipleSelect(false);
            listBox_.setVisibleItemCount(10);
            listBox_.setWidth("100%");
            if (repos.length() > 0)
            {
               for(int i=0; i<repos.length(); i++)
               {
                  CRANMirror repo = repos.get(i);

                  if (!StringUtil.isNullOrEmpty(repo.getName()) &&
                      !repo.getName().toLowerCase().equals("cran"))
                  {
                     repos_.add(repo);
                     listBox_.addItem(repo.getName(), repo.getURL());
                  }
               }
               
               listBox_.setSelectedIndex(0);
               enableOkButton(true);
            }
            
            panel.setWidget(listBox_);
            
            setText("Add Secondary Repo");
          
            listBox_.addDoubleClickHandler(new DoubleClickHandler() {
               @Override
               public void onDoubleClick(DoubleClickEvent event)
               {
                  clickOkButton();              
               }
            });
            
            final int kDefaultPanelHeight = 265;
            if (listBox_.getOffsetHeight() > kDefaultPanelHeight)
               panel.setHeight(listBox_.getOffsetHeight() + "px");
            
            FocusHelper.setFocusDeferred(listBox_);
         }
         
         @Override
         public void onError(ServerError error)
         {
            closeDialog();
            super.onError(error);
         }
      });
      
      return root;
   }
   
   static interface Styles extends CssResource
   {
      String mainWidget();
      String customPanel();
      String namePanel();
      String urlTextBox();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("SecondaryReposDialog.css")
      Styles styles();
   }
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
   
   private SecondaryReposServerOperations secondaryReposServer_ = null;
   private GlobalDisplay globalDisplay_ = null;
   private ArrayList<CRANMirror> repos_ = null;
   private ListBox listBox_ = null;
   private TextBox nameTextBox_ = null;
   private TextBox urlTextBox_ = null;
   private ArrayList<String> excluded_;
}
