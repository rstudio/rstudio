/*
 * UnsavedChangesDialog.java
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
package org.rstudio.studio.client.workbench.views.source;

import java.util.ArrayList;

import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.IdentityColumn;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;

public class UnsavedChangesDialog extends ModalDialog<ArrayList<EditingTarget>>
{
   public UnsavedChangesDialog(
         ArrayList<EditingTarget> dirtyTargets,
         final OperationWithInput<ArrayList<EditingTarget>> saveOperation)
   {
      super("Unsaved Changes", saveOperation);
      editingTargets_ = dirtyTargets;
      
      setOkButtonCaption("Save Selected");
           	     
      addLeftButton(new ThemedButton("Don't Save", new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
           closeDialog();
           saveOperation.execute(new ArrayList<EditingTarget>());
         } 
      }));    
   }

   @Override
   protected Widget createMainWidget()
   {
      // create cell table
      targetsCellTable_ = new CellTable<EditingTarget>(
                                          15,
                                          UnsavedChangesCellTableResources.INSTANCE,
                                          KEY_PROVIDER);
      selectionModel_ = new MultiSelectionModel<EditingTarget>(KEY_PROVIDER);
      targetsCellTable_.setSelectionModel(
         selectionModel_, 
         DefaultSelectionEventManager.<EditingTarget> createCheckboxManager());
      targetsCellTable_.setWidth("100%", true);
      
      // add columns
      addSelectionColumn();
      addIconColumn();
      addNameAndPathColumn();
      
      // hook-up data provider 
      dataProvider_ = new ListDataProvider<EditingTarget>();
      dataProvider_.setList(editingTargets_);
      dataProvider_.addDataDisplay(targetsCellTable_);
      targetsCellTable_.setPageSize(editingTargets_.size());
      
      // select all by default
      for (EditingTarget editingTarget : dataProvider_.getList())
         selectionModel_.setSelected(editingTarget, true);
      
      // enclose cell table in scroll panel
      ScrollPanel scrollPanel = new ScrollPanel();
      scrollPanel.setStylePrimaryName(RESOURCES.styles().editingTargetScrollPanel());
      scrollPanel.setWidget(targetsCellTable_);
      
      // main widget
      VerticalPanel panel = new VerticalPanel();
      Label captionLabel = new Label(
                           "The following documents have unsaved changes:");
      captionLabel.setStylePrimaryName(RESOURCES.styles().captionLabel());
      panel.add(captionLabel);
      panel.add(scrollPanel);      
      return panel;
   }
   
   private Column<EditingTarget, Boolean> addSelectionColumn()
   {
      Column<EditingTarget, Boolean> checkColumn = 
         new Column<EditingTarget, Boolean>(new CheckboxCell(true, false)) 
         {
            @Override
            public Boolean getValue(EditingTarget object)
            {
               return selectionModel_.isSelected(object);
            }   
         };
      checkColumn.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
      targetsCellTable_.addColumn(checkColumn); 
      targetsCellTable_.setColumnWidth(checkColumn, 25, Unit.PX);
      
      return checkColumn;
   }
  
   
   private Column<EditingTarget, ImageResource> addIconColumn()
   {
      Column<EditingTarget, ImageResource> iconColumn = 
         new Column<EditingTarget, ImageResource>(new ImageResourceCell()) {

            @Override
            public ImageResource getValue(EditingTarget object)
            {
               return object.getIcon();
            }
         };
      targetsCellTable_.addColumn(iconColumn);
      targetsCellTable_.setColumnWidth(iconColumn, 20, Unit.PX);
    
      return iconColumn;
   }
    
   private class NameAndPathCell extends AbstractCell<EditingTarget>
   {

      @Override
      public void render(
            com.google.gwt.cell.client.Cell.Context context,
            EditingTarget value, SafeHtmlBuilder sb)
      {
         if (value != null) 
         {
           Styles styles = RESOURCES.styles();
           
           String path = value.getPath();
           if (path != null)
           {
              SafeHtmlUtil.appendDiv(sb, 
                                     styles.targetName(), 
                                     value.getName().getValue());
           
              SafeHtmlUtil.appendDiv(sb, styles.targetPath(), path); 
           }
           else
           {
              SafeHtmlUtil.appendDiv(sb, 
                                     styles.targetUntitled(), 
                                     value.getName().getValue());
           }
         }
         
      }
      
   }
   
   private IdentityColumn<EditingTarget> addNameAndPathColumn()
   {
      IdentityColumn<EditingTarget> nameAndPathColumn = 
         new IdentityColumn<EditingTarget>(new NameAndPathCell());
      
      targetsCellTable_.addColumn(nameAndPathColumn);
      targetsCellTable_.setColumnWidth(nameAndPathColumn, 350, Unit.PX);
      return nameAndPathColumn;
   }
   
   @Override
   protected ArrayList<EditingTarget> collectInput()
   {
      return new ArrayList<EditingTarget>(selectionModel_.getSelectedSet());
   }

   @Override
   protected boolean validate(ArrayList<EditingTarget> input)
   {
      return true;
   }
   
   static interface Styles extends CssResource
   {
      String editingTargetScrollPanel();
      String captionLabel();
      String targetName();
      String targetPath();
      String targetUntitled();
   }

   static interface Resources extends ClientBundle
   {
      @Source("UnsavedChangesDialog.css")
      Styles styles();
   }

   static Resources RESOURCES = (Resources) GWT.create(Resources.class);

   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
   
   private static final ProvidesKey<EditingTarget> KEY_PROVIDER = 
      new ProvidesKey<EditingTarget>() {
         @Override
         public Object getKey(EditingTarget item)
         {
            return item.getId();
         }
    };
   
   private final ArrayList<EditingTarget> editingTargets_;
   
   private CellTable<EditingTarget> targetsCellTable_; 
   private ListDataProvider<EditingTarget> dataProvider_;
   private MultiSelectionModel<EditingTarget> selectionModel_;


}
