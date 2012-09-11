/*
 * WorkspaceObjectTable.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.workspace.table;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.inject.Inject;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.workspace.model.WorkspaceObjectInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class WorkspaceObjectTable
{
   public interface Observer
   {
      void editObject(String objectName);
      void viewObject(String objectName);
   }
   /**
    * The RowManager takes care of figuring out what values should go
    * into what rows in the table, and creating and removing those rows.
    * 
    * The table is divided vertically into sections, where each section
    * has one heading row followed by zero or more value rows.
    * 
    * Each element of the sections_ field is a sorted list of names
    * that appear in that section. 
    */
   private class RowManager
   {
      public RowManager()
      {
         clearTable() ;
      }
      
      public void clearTable()
      {
         while (table_.getRowCount() > 0)
            table_.removeRow(0) ;
         sections_.clear() ;
         
         for (int i = 0; i < SECTION_NAMES.length; i++)
         {
            table_.setText(i, 0, SECTION_NAMES[i]) ;
            table_.getFlexCellFormatter().setColSpan(i, 0, 2) ;
            table_.getRowFormatter().setStylePrimaryName(
                  i,
                  ThemeResources.INSTANCE.themeStyles().workspaceSectionHead());
            sections_.add(new ArrayList<WorkspaceObjectInfo>()) ;
            manageHeadingVisibility(i);
         }
      }
      
      /**
       * Returns the table row index where this object should go.
       * If the appropriate row didn't exist before, it now does.
       * If an object with the same name existed in a different
       * section, that reference has been removed.
       */
      public int getRowIndex(WorkspaceObjectInfo obj)
      {
         int sectionId = chooseSection(obj) ;
         
         ArrayList<WorkspaceObjectInfo> section = sections_.get(sectionId) ;
         int index = searchSection(section, obj.getName()) ;
         if (index >= 0)
            return index + getSectionStart(sectionId) ;
         
         // Just in case this name already exists but in a different section
         removeRow(obj.getName()) ;
         
         index = -(index+1) ;
         section.add(index, obj) ;
         int tableIndex = index + getSectionStart(sectionId) ;
         table_.insertRow(tableIndex) ;
         table_.getRowFormatter().setStylePrimaryName(
               tableIndex,
               ThemeResources.INSTANCE.themeStyles().workspaceDataRow());
         table_.getRowFormatter().addStyleName(
               tableIndex,
               FontSizer.getNormalFontSizeClass());
         manageHeadingVisibility(sectionId);
         return tableIndex ;
      }
      
      public boolean removeRow(String name)
      {
         for (int i = 0; i < sections_.size(); i++)
         {
            int index = searchSection(sections_.get(i), name) ;
            if (index >= 0)
            {
               table_.removeRow(getSectionStart(i) + index) ;
               sections_.get(i).remove(index) ;
               manageHeadingVisibility(i);
               return true ;
            }
         }
         return false ;
      }
      
      public ArrayList<String> getObjectNames()
      {
         ArrayList<String> objectNames = new ArrayList<String>();
         for (int i = 0; i < sections_.size(); i++)
         {
            ArrayList<WorkspaceObjectInfo> section = sections_.get(i);
            for (int j = 0; j<section.size(); j++)
               objectNames.add(section.get(j).getName());
         }
            
        return objectNames;
      }
      
      public WorkspaceObjectInfo getObjectForIndex(int index)
      {
         int pos = 0 ;
         for (int i = 0; i < sections_.size(); i++)
         {
            pos++ ;
            if (index < pos)
               return null ;
            
            ArrayList<WorkspaceObjectInfo> section = sections_.get(i) ;
            if (index - pos < section.size())
               return section.get(index - pos) ;
            
            pos += section.size();
         }
         return null ;
      }
      
      private int searchSection(ArrayList<WorkspaceObjectInfo> section,
                                String name)
      {
         for (int i = 0; i<section.size(); i++)
            if (section.get(i).getName().equals(name))
               return i;
        
         return -1;     
      }

      private int getSectionStart(int section)
      {
         int rows = 0 ;
         for (int i = 0; i < section; i++)
         {
            rows += 1 + sections_.get(i).size() ;
         }
         return rows + 1 ;
      }

      public int getSectionForRow(int row)
      {
         int rows = 0 ;
         for (int i = 0; i < sections_.size(); i++)
         {
            rows += 1 + sections_.get(i).size() ;
            if (rows > row)
               return i;
         }
         return 0;
      }

      private int chooseSection(WorkspaceObjectInfo obj)
      {
         String type = obj.getType();
         if (isData(type))
            return SEC_DATA ;
         if ("function".equals(type) && obj.getLength() == 1)
            return SEC_FUNC ;
         else
            return SEC_VAL ;
      }

      private void manageHeadingVisibility(int section)
      {
         int rowIndex = getSectionStart(section) - 1;
         table_.getRowFormatter().setVisible(rowIndex,
                                             sections_.get(section).size() > 0);
      }

      private final ArrayList<ArrayList<WorkspaceObjectInfo>> sections_
                                          = new ArrayList<ArrayList<WorkspaceObjectInfo>>();
      private static final int SEC_DATA = 0 ;
      private static final int SEC_VAL = 1 ;
      private static final int SEC_FUNC = 2 ;
      private final String[] SECTION_NAMES = {"Data", "Values", "Functions"} ;
   }
   
   
   @Inject
   public WorkspaceObjectTable(InlineEditorFactory inlineEditorFactory,
                               GlobalDisplay globalDisplay)
   {
      inlineEditorFactory_ = inlineEditorFactory;
      globalDisplay_ = globalDisplay ;
      table_ = new FlexTableEx() ;
      table_.setStylePrimaryName(ThemeResources.INSTANCE.themeStyles().workspace());
      table_.setCellSpacing(0) ;
      table_.setCellPadding(2) ;
      
      table_.getColumnFormatter().setWidth(0, "25%") ;
      table_.getColumnFormatter().setWidth(1, "75%") ;

      table_.addClickHandler(new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            HTMLTable.Cell cell = table_.getCellForEvent(event);
            if (cell == null)
               return;
            
            int row = cell.getRowIndex();
            WorkspaceObjectInfo object = rowManager_.getObjectForIndex(row);
            if (object == null)
               return;
            
            final String objectName = object.getName();
            if (objectName == null)
               return;
            

            if (editHandlers_.containsKey(objectName))
            {
               editHandlers_.get(objectName).onClick(event);
               return;
            }

            event.preventDefault();
            event.stopPropagation();

            int section = rowManager_.getSectionForRow(row);
            if (section == RowManager.SEC_DATA)
            {
               observer_.viewObject(objectName);
            }
            else
            {
               if (object.getLength() > 100)
               {
                  globalDisplay_.showYesNoMessage(
                        MessageDialog.WARNING, 
                        "Confirm Edit Object",
                        "Are you sure you want to interactively edit the '" +
                        objectName + "' object? (it has a length of " + 
                        object.getLength() + ")",
                        new Operation() {
                           @Override
                           public void execute()
                           {
                              observer_.editObject(objectName);
                           }
                        },
                        true);           
               }
               else
               {
                  observer_.editObject(objectName);
               }
            }
         }
      });
      
      table_.setWidth("100%");
      scrollPanel_ = new ScrollPanel(table_);
      
      rowManager_ = new RowManager() ;
   }

   public Object getView()
   {
      return scrollPanel_;
   }

   public void clearObjects()
   {
      editHandlers_.clear();
      rowManager_.clearTable() ;
   }

   public void removeObject(String name)
   {
      editHandlers_.remove(name);
      rowManager_.removeRow(name) ;
   }
   
   public ArrayList<String> getObjectNames()
   {
      return rowManager_.getObjectNames();
   }
   
   public void updateObject(WorkspaceObjectInfo object)
   {
      editHandlers_.remove(object.getName());

      int index = rowManager_.getRowIndex(object) ;
      String type = object.getType() ;
      if ("function".equals(type))
      {
         table_.getFlexCellFormatter().setColSpan(index, 0, 2) ;
         String signature = object.getValue().replaceFirst("^function ", 
                                                           object.getName()) ;
         table_.setText(index, 0, signature) ;
      }
      else
      {
         table_.setText(index, 0, object.getName()) ;
         ScalarEdit<?> editor = null ;
         if (object.getLength() == 1)
         {
            if (genericTypes.contains(type)
                && !"NO_VALUE".equals(object.getValue()))
            {
               editor = new ScalarEdit<String>(globalDisplay_,
                                               new RLiteralConversionStrategy(),
                                               object.getValue()) ;
            }
         }

         if (editor != null)
         {
            editHandlers_.put(object.getName(), editor);
            inlineEditorFactory_.create(object, editor) ;
            table_.setWidget(index, 1, editor);
         }
         else if (isData(type))
         {
            table_.setHTML(index, 1, DomUtils.textToHtml(object.getExtra()));
            table_.getRowFormatter().addStyleName(
                  index,
                  ThemeStyles.INSTANCE.workspaceDataFrameRow());
         }
         else
         {
            table_.setText(index, 1, type + "[" + object.getLength() + "]");
         }
      }
   }

   public void fireEvent(GwtEvent<?> gwtEvent)
   {
      handlerManager_.fireEvent(gwtEvent);
   }

   public void setObserver(Observer observer)
   {
      observer_ = observer;
   }

   private boolean isData(String type)
   {
      return "data.frame".equals(type) || "matrix".equals(type);
   }

   private RowManager rowManager_ ;
   private final HandlerManager handlerManager_ = new HandlerManager(null);
   private final HashMap<String, ClickHandler> editHandlers_ =
                                            new HashMap<String, ClickHandler>(); 
   private final InlineEditorFactory inlineEditorFactory_;
   private final GlobalDisplay globalDisplay_ ;
   private final FlexTableEx table_;
   private final ScrollPanel scrollPanel_;
   
   private static HashSet<String> genericTypes = new HashSet<String>() ;
   static {
      genericTypes.add("NULL") ;
      genericTypes.add("logical") ;
      genericTypes.add("double") ;
      genericTypes.add("numeric") ;
      genericTypes.add("integer") ;
      genericTypes.add("complex") ;
      genericTypes.add("character") ;
   }

   private Observer observer_;
}
