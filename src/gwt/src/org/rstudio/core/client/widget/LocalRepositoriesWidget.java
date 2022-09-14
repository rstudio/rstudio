/*
 * LocalRepositoriesWidget.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.core.client.widget;

import java.util.ArrayList;

import org.rstudio.core.client.CoreClientConstants;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;

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

public class LocalRepositoriesWidget extends Composite
{
   public LocalRepositoriesWidget()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      VerticalPanel panel = new VerticalPanel();
      
      HorizontalPanel hp = new HorizontalPanel();
      listBox_ = new ListBox();
      listBox_.setMultipleSelect(true);
      listBox_.addStyleName(RES.styles().listBox());
      listBox_.getElement().<SelectElement>cast().setSize(3);
      hp.add(listBox_);
      
      VerticalPanel buttonPanel = new VerticalPanel();
      SmallButton buttonAdd = createButton(constants_.buttonAddCaption());
      buttonAdd.addClickHandler(addButtonClicked_);
      buttonPanel.add(buttonAdd);
      SmallButton buttonRemove = createButton(constants_.buttonRemoveCaption());
      buttonRemove.addClickHandler(removeButtonClicked_);
      buttonPanel.add(buttonRemove);
      hp.add(buttonPanel);
      
      panel.add(new LabelWithHelp(constants_.localReposText(),
            "packrat_local_repos", constants_.localReposTitle(),
            listBox_));
      panel.add(hp);
      
      initWidget(panel);
      
   }
   
   @Inject
   void initialize(FileDialogs fileDialogs,
                   RemoteFileSystemContext fileSystemContext)
   {
      fileDialogs_ = fileDialogs;
      fileSystemContext_ = fileSystemContext;
   }
   
   public void addItem(String item)
   {
      listBox_.addItem(item);
   }
   
   public ArrayList<String> getItems() {
      ArrayList<String> items = new ArrayList<>();
      int numItems = listBox_.getItemCount();
      for (int i = 0; i < numItems; ++i) {
         items.add(listBox_.getItemText(i));
      }
      return items;
   }
   
   
   private ClickHandler addButtonClicked_ = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event)
      {
         fileDialogs_.chooseFolder(
               constants_.addLocalRepoText(),
               fileSystemContext_,
               FileSystemItem.home(),
               new ProgressOperationWithInput<FileSystemItem>() {

                  @Override
                  public void execute(FileSystemItem input,
                        ProgressIndicator indicator)
                  {
                     indicator.onCompleted();
                     if (input == null)
                        return;

                     listBox_.addItem(input.getPath());

                  }

               }); 
      }
   };
  
   
   private ClickHandler removeButtonClicked_ = new ClickHandler() {
      @Override
      public void onClick(ClickEvent event)
      {
         int i = 0;
         while (i < listBox_.getItemCount())
         {
            if (listBox_.isItemSelected(i))
            {
               listBox_.removeItem(i);
               i = 0;
            }
            else
            {
               ++i;
            }
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
   
   static interface Styles extends CssResource
   {
      String helpButton();
      String listBox();
      String button();
   }
   
   static interface Resources extends ClientBundle
   {
      @Source("LocalRepositoriesWidget.css")
      Styles styles();
   }
   
   private final ListBox listBox_;
   private FileDialogs fileDialogs_;
   private RemoteFileSystemContext fileSystemContext_;
   
   static Resources RES = (Resources)GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }
   private static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);
}
