/*
 * SecondaryReposWidget.java
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
package org.rstudio.studio.client.common.repos;

import java.util.ArrayList;

import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.mirrors.model.CRANMirror;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

public class SecondaryReposWidget extends Composite
{
   public SecondaryReposWidget()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      repos_ = new ArrayList<>();
      
      VerticalPanel panel = new VerticalPanel();
      panel.addStyleName(RES.styles().panel());
      
      HorizontalPanel horizontal = new HorizontalPanel();
      listBox_ = new ListBox();
      listBox_.setMultipleSelect(false);
      listBox_.addStyleName(RES.styles().listBox());
      listBox_.getElement().<SelectElement>cast().setSize(6);
      horizontal.add(listBox_);
      
      VerticalPanel buttonPanel = new VerticalPanel();
      buttonPanel.addStyleName(RES.styles().buttonPanel());

      buttonAdd_ = createButton("Add...");
      buttonAdd_.addClickHandler(addButtonClicked_);
      buttonPanel.add(buttonAdd_);

      buttonRemove_ = createButton("Remove...");
      buttonRemove_.addClickHandler(removeButtonClicked_);
      buttonPanel.add(buttonRemove_);

      buttonUp_ = createButton("Up");
      buttonUp_.addClickHandler(upButtonClicked_);
      buttonPanel.add(buttonUp_);

      buttonDown_ = createButton("Down");
      buttonDown_.addClickHandler(downButtonClicked_);
      buttonPanel.add(buttonDown_);

      horizontal.add(buttonPanel);
      
      panel.add(horizontal);
      
      initWidget(panel);
   }

   @Inject
   void initialize(GlobalDisplay globalDisplay)
   {
      globalDisplay_ = globalDisplay;
   }
   
   private void updateRepos()
   {
      listBox_.clear();
      for (int i = 0; i < repos_.size(); i++)
         listBox_.addItem(repos_.get(i).getName());
   }
   
   public ArrayList<CRANMirror> getRepos()
   {
      return repos_;
   }
   
   public void setRepos(ArrayList<CRANMirror> repos)
   {
      repos_ = repos;
      updateRepos();
   }

   public void setCranRepoUrl(String cranUrl, boolean cranIsCustom)
   {
      cranRepoUrl_ = cranUrl;
      cranIsCustom_ = cranIsCustom;
   }

   /**
    * @return the widget that should be labeled for accessibility purposes
    */
   public Widget getLabeledWidget()
   {
      return listBox_;
   }
   
   private ClickHandler addButtonClicked_ = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event)
      {
         ArrayList<String> excluded = new ArrayList<>();
         for (int i = 0; i < repos_.size(); i++)
            excluded.add(repos_.get(i).getName());
         
         SecondaryReposDialog secondaryReposDialog = new SecondaryReposDialog(new OperationWithInput<CRANMirror>() {
            @Override
            public void execute(CRANMirror input)
            {
               repos_.add(input);
               updateRepos();
            }
         }, excluded, cranRepoUrl_, cranIsCustom_);
         
         secondaryReposDialog.showModal();
      }
   };
   
   private ClickHandler removeButtonClicked_ = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event)
      {
         // get selected index
         final int index = listBox_.getSelectedIndex();
         if (index != -1)
         {
            final String repo = listBox_.getValue(index);
            globalDisplay_.showYesNoMessage(
               MessageDialog.WARNING, 
               "Confirm Remove", 
               "Are you sure you want to remove the " + repo + " repository?",
               new Operation() {
                  @Override
                  public void execute()
                  {
                     repos_.remove(index);
                     updateRepos();
                  }
               },
               false);
         }
      }
   };

   private ClickHandler upButtonClicked_ = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event)
      {
         final int index = listBox_.getSelectedIndex();
         if (index > 0)
         {
            CRANMirror swap = repos_.get(index - 1);
            repos_.set(index - 1, repos_.get(index));
            repos_.set(index, swap);
            updateRepos();
            listBox_.setSelectedIndex(index - 1);
         }
      }
   };

   private ClickHandler downButtonClicked_ = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event)
      {
         final int index = listBox_.getSelectedIndex();
         if (index < repos_.size() - 1)
         {
            CRANMirror swap = repos_.get(index);
            repos_.set(index, repos_.get(index + 1));
            repos_.set(index + 1, swap);
            updateRepos();
            listBox_.setSelectedIndex(index + 1);
         }
      }
   };
   
   private SmallButton createButton(String caption)
   {
      SmallButton button = new SmallButton(caption);
      button.addStyleName(RES.styles().button());
      button.fillWidth();
      return button;
   }
   
   private final ListBox listBox_;
   private GlobalDisplay globalDisplay_;
   private ArrayList<CRANMirror> repos_;
   
   private String cranRepoUrl_;
   private boolean cranIsCustom_;

   private SmallButton buttonAdd_;
   private SmallButton buttonRemove_;
   private SmallButton buttonUp_;
   private SmallButton buttonDown_;
   
   static interface Styles extends CssResource
   {
      String listBox();
      String button();
      String buttonPanel();
      String panel();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("SecondaryReposWidget.css")
      Styles styles();
   }
   
   static Resources RES = (Resources)GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }
}
