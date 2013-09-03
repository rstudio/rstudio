/*
 * EnvironmentObjects.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;

import org.rstudio.core.client.cellview.AutoHidingSplitLayoutPanel;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.studio.client.workbench.views.environment.EnvironmentPane;
import org.rstudio.studio.client.workbench.views.environment.model.CallFrame;
import org.rstudio.studio.client.workbench.views.environment.model.RObject;
import org.rstudio.studio.client.workbench.views.environment.view.CallFramePanel.CallFramePanelHost;

import java.util.*;

public class EnvironmentObjects extends ResizeComposite
   implements CallFramePanelHost,
              EnvironmentObjectDisplay.Host
{
   // Public interfaces -------------------------------------------------------

   public interface Binder extends UiBinder<Widget, EnvironmentObjects>
   {
   }

   // Constructor -------------------------------------------------------------

   public EnvironmentObjects(EnvironmentObjectsObserver observer)
   {
      observer_ = observer;
      contextDepth_ = 0;
      environmentName_ = EnvironmentPane.GLOBAL_ENVIRONMENT_NAME;

      objectDisplayType_ = OBJECT_LIST_VIEW;
      objectDataProvider_ = new ListDataProvider<RObjectEntry>();
      objectSort_ = new RObjectEntrySort();

      // set up the call frame panel
      callFramePanel_ = new CallFramePanel(observer_, this);

      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));

      splitPanel.addSouth(callFramePanel_, 150);
      splitPanel.setWidgetMinSize(callFramePanel_, style.headerRowHeight());
      
      setObjectDisplay(objectDisplayType_);
      
      FontSizer.applyNormalFontSize(this);
   }

   // Public methods ----------------------------------------------------------

   @Override
   public void onResize()
   {
      super.onResize();
      if (pendingCallFramePanelSize_)
      {
         autoSizeCallFramePanel();
      }
   }
   
   public void setContextDepth(int contextDepth)
   {
      if (contextDepth > 0)
      {
         splitPanel.setWidgetHidden(callFramePanel_, false);
         splitPanel.onResize();
      }
      else if (contextDepth == 0)
      {
         callFramePanel_.clearCallFrames();
         splitPanel.setWidgetHidden(callFramePanel_, true);
      }
      contextDepth_ = contextDepth;
   }

   public void addObject(RObject obj)
   {
      int idx = indexOfExistingObject(obj.getName());
      RObjectEntry newEntry = entryFromRObject(obj);
      boolean added = false;

      // if the object is already in the environment, just update the value
      if (idx >= 0)
      {
         final RObjectEntry oldEntry = objectDataProvider_.getList().get(idx);

         if (oldEntry.rObject.getType().equals(obj.getType()))
         {
            // type did not change; update in-place and preserve expansion flag
            newEntry.expanded = oldEntry.expanded;
            objectDataProvider_.getList().set(idx, newEntry);
            added = true;
         }
         else
         {
            // types did change, do a full add/remove
            objectDataProvider_.getList().remove(idx);
         }
         
      }
      if (!added)
      {
         RObjectEntry entry = entryFromRObject(obj);
         idx = indexOfNewObject(entry);
         objectDataProvider_.getList().add(idx, entry);
      }
      updateCategoryLeaders(true);
      objectDisplay_.getRowElement(idx).scrollIntoView();
   }

   public void removeObject(String objName)
   {
      int idx = indexOfExistingObject(objName);
      if (idx >= 0)
      {
         objectDataProvider_.getList().remove(idx);
      }

      updateCategoryLeaders(true);
   }
   
   public void clearObjects()
   {
      objectDataProvider_.getList().clear();
   }

   // bulk add for objects--used on init or environment switch
   public void addObjects(JsArray<RObject> objects)
   {
      // create an entry for each object and sort the array
      int numObjects = objects.length();
      ArrayList<RObjectEntry> objectEntryList = new ArrayList<RObjectEntry>();
      for (int i = 0; i < numObjects; i++)
      {
         RObjectEntry entry = entryFromRObject(objects.get(i));
         objectEntryList.add(entry);
      }
      Collections.sort(objectEntryList, objectSort_);

      // push the list into the UI and update category leaders
      objectDataProvider_.getList().addAll(objectEntryList);
      updateCategoryLeaders(false);

      if (useStatePersistence())
      {
         setDeferredState();
      }
   }

   public void setCallFrames(JsArray<CallFrame> frameList)
   {
      callFramePanel_.setCallFrames(frameList, contextDepth_);

      // if the parent panel has layout information, auto-size the call frame
      // panel (let GWT go first so the call frame panel visibility has 
      // taken effect) 
      if (splitPanel.getOffsetHeight() > 0)
      {
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {            
            @Override
            public void execute()
            {
               autoSizeCallFramePanel();
            }
         });
      }
      else
      {
         // wait until the split panel has layout information to compute the 
         // correct size of the call frame panel
         pendingCallFramePanelSize_ = true;
      }
   }
   
   public void setEnvironmentName(String environmentName)
   {
      environmentNameLabel_.setText(environmentName);
      environmentEmptyMessage_.setText(contextDepth_ > 0 ?
                                       EMPTY_FUNCTION_ENVIRONMENT_MESSAGE :
                                       environmentName + 
                                          " is empty");
      environmentName_ = environmentName;
   }

   public int getScrollPosition()
   {
      return objectDisplay_.getScrollPanel().getVerticalScrollPosition();
   }

   public void setScrollPosition(int scrollPosition)
   {
      deferredScrollPosition_ = scrollPosition;
   }

   public void setExpandedObjects(JsArrayString objects)
   {
      deferredExpandedObjects_ = objects;
   }

   public void updateLineNumber (int newLineNumber)
   {
      callFramePanel_.updateLineNumber(newLineNumber);
   }
   
   public void setFilterText (String filterText)
   {
      filterText_ = filterText.toLowerCase();

      // Iterate over each entry in the list, and toggle its visibility based 
      // on whether it matches the current filter text.
      List<RObjectEntry> objects = objectDataProvider_.getList();
      for (int i = 0; i < objects.size(); i++)
      {
         RObjectEntry entry = objects.get(i);
         boolean visible = matchesFilter(entry.rObject);
         // Redraw the object if its visibility status has changed, or if it's
         // visible (for visible entries we need to update the search highlight)
         if (visible != entry.visible || visible)
         {
            entry.visible = visible;
            objectDisplay_.redrawRow(i);
         }
      }

      updateCategoryLeaders(true);
   }
   
   public int getObjectDisplay()
   {
      return objectDisplayType_;
   }

   public void setObjectDisplay(int type)
   {
      // if we already have an active display of this type, do nothing
      if (type == objectDisplayType_ && 
          objectDisplay_ != null)
      {
         return;
      }
      
      // clean up previous object display, if we had one
      if (objectDisplay_ != null)
      {
         objectDataProvider_.removeDataDisplay(objectDisplay_);
         splitPanel.remove(objectDisplay_);
      }
      // create the new object display and wire it to the data source
      if (type == OBJECT_LIST_VIEW)
      {
         objectDisplay_ = new EnvironmentObjectList(this, observer_);
         objectSort_.setSortType(RObjectEntrySort.SORT_AUTO);
      }
      else if (type == OBJECT_GRID_VIEW)
      {
         objectDisplay_ = new EnvironmentObjectGrid(this, observer_);
         objectSort_.setSortType(RObjectEntrySort.SORT_COLUMN);
      }

      Collections.sort(objectDataProvider_.getList(), objectSort_);
      objectDataProvider_.addDataDisplay(objectDisplay_);

      // disable persistent and transient row selection (currently necessary
      // because we emit more than one row per object and the DataGrid selection
      // behaviors aren't designed to work that way)
      objectDisplay_.setSelectionModel(new NoSelectionModel<RObjectEntry>(
              RObjectEntry.KEY_PROVIDER));
      objectDisplay_.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
      objectDisplay_.getScrollPanel().addScrollHandler(new ScrollHandler()
      {
         @Override
         public void onScroll(ScrollEvent event)
         {
            if (useStatePersistence())
            {
               deferredScrollPosition_ = getScrollPosition();
               observer_.setPersistedScrollPosition(deferredScrollPosition_);
            }
         }
      });

      objectDisplay_.setEmptyTableWidget(buildEmptyGridMessage());
      objectDisplay_.setStyleName(style.objectGrid() + " " + style.environmentPanel());
      splitPanel.add(objectDisplay_);
      objectDisplayType_ = type;
   }

   // CallFramePanelHost implementation ---------------------------------------

   @Override
   public void minimizeCallFramePanel()
   {
      callFramePanelHeight_ = splitPanel.getWidgetSize(callFramePanel_).intValue();
      splitPanel.setWidgetSize(callFramePanel_, style.headerRowHeight());
   }

   @Override
   public void restoreCallFramePanel()
   {
      splitPanel.setWidgetSize(callFramePanel_, callFramePanelHeight_);
      callFramePanel_.onResize();
   }

   // EnvironmentObjectsDisplay.Host implementation ---------------------------

   @Override
   public boolean enableClickableObjects()
   {
      return contextDepth_ < 2;
   }
   
   // we currently only set and/or get persisted state at the root context
   // level.
   @Override
   public boolean useStatePersistence()
   {
      return environmentName_.equals(EnvironmentPane.GLOBAL_ENVIRONMENT_NAME);
   }
   
   @Override
   public String getFilterText()
   {
      return filterText_;
   }

   // Private methods: object management --------------------------------------

   private int indexOfExistingObject(String objectName)
   {
      List<RObjectEntry> objects = objectDataProvider_.getList();

      // find the position of the object in the list--we can't use binary
      // search here since we're matching on names and the list isn't sorted
      // by name (it's sorted by type, then name)
      int index;
      boolean foundObject = false;
      for (index = 0; index < objects.size(); index++)
      {
         if (objects.get(index).rObject.getName() == objectName)
         {
            foundObject = true;
            break;
         }
      }

      return foundObject ? index : -1;
   }

   // returns the position a new object entry should occupy in the table
   private int indexOfNewObject(RObjectEntry obj)
   {
      List<RObjectEntry> objects = objectDataProvider_.getList();
      int numObjects = objects.size();
      int idx;
      // consider: can we use binary search here?
      for (idx = 0; idx < numObjects; idx++)
      {
         if (objectSort_.compare(obj, objects.get(idx)) < 0)
         {
            break;
         }
      }
      return idx;
   }

   // after adds or removes, we need to tag the new category-leading objects
   private void updateCategoryLeaders(boolean redrawUpdatedRows)
   {
      List<RObjectEntry> objects = objectDataProvider_.getList();

      // whether or not we've found a leader for each category
      Boolean[] leaders = { false, false, false };
      boolean foundFirstObject = false;

      for (int i = 0; i < objects.size(); i++)
      {
         RObjectEntry entry = objects.get(i);
         if (!entry.visible)
            continue;
         if (!foundFirstObject)
         {
            entry.isFirstObject = true;
            foundFirstObject = true;
         }
         else
         {
            entry.isFirstObject = false;
         }
         int category = entry.getCategory();
         Boolean leader = entry.isCategoryLeader;
         // if we haven't found a leader for this category yet, make this object
         // the leader if it isn't already
         if (!leaders[category])
         {
            leaders[category] = true;
            if (!leader)
            {
               entry.isCategoryLeader = true;
            }
         }
         // if this object is marked as the leader but we've already found a
         // leader, unmark it
         else if (leader)
         {
            entry.isCategoryLeader = false;
         }

         // if we changed the leader flag, redraw the row
         if (leader != entry.isCategoryLeader
             && redrawUpdatedRows)
         {
            objectDisplay_.redrawRow(i);
         }
      }
   }

   // Private methods: DataGrid setup -----------------------------------------

   // create each column for the data grid
   private Widget buildEmptyGridMessage()
   {
      HTMLPanel messagePanel = new HTMLPanel("");
      messagePanel.setStyleName(style.emptyEnvironmentPanel());
      environmentNameLabel_ = new Label(EnvironmentPane.GLOBAL_ENVIRONMENT_NAME);
      environmentNameLabel_.setStyleName(style.emptyEnvironmentName());
      environmentEmptyMessage_ = new Label(EMPTY_GLOBAL_ENVIRONMENT_MESSAGE);
      environmentEmptyMessage_.setStyleName(style.emptyEnvironmentMessage());
      messagePanel.add(environmentNameLabel_);
      messagePanel.add(environmentEmptyMessage_);
      return messagePanel;
   }

   private void autoSizeCallFramePanel()
   {
      // after setting the frames, resize the call frame panel to neatly 
      // wrap the new list, up to a maximum of 2/3 of the height of the 
      // split panel.
      int desiredCallFramePanelSize = 
            callFramePanel_.getDesiredPanelHeight();
      
      if (splitPanel.getOffsetHeight() > 0)
      {
         desiredCallFramePanelSize = Math.min(
                 desiredCallFramePanelSize,
                 (int)(0.66 * splitPanel.getOffsetHeight()));
      }
                  
      // if the panel is minimized, just update the cached height so it'll 
      // get set to what we want when/if the panel is restored
      if (callFramePanel_.isMinimized())
      {
         callFramePanelHeight_ = desiredCallFramePanelSize;
      }
      else
      {
         splitPanel.setWidgetSize(
               callFramePanel_, desiredCallFramePanelSize);
         callFramePanel_.onResize();
         objectDisplay_.onResize();
      }
      
      pendingCallFramePanelSize_ = false;
   }


   // Private methods: state persistence --------------------------------------

   private void setDeferredState()
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            if (deferredExpandedObjects_ != null)
            { 
               // loop through the objects in the list and check to see if each
               // is marked expanded in the persisted list of expanded objects
               List<RObjectEntry> objects = objectDataProvider_.getList();
               for (int idxObj = 0; idxObj < objects.size(); idxObj++)
               {
                  for (int idxExpanded = 0;
                       idxExpanded < deferredExpandedObjects_.length();
                       idxExpanded++)
                  {
                     if (objects.get(idxObj).rObject.getName() ==
                         deferredExpandedObjects_.get(idxExpanded))
                     {
                        objects.get(idxObj).expanded = true;
                        objectDisplay_.redrawRow(idxObj);
                     }
                  }
               }
            }

            // set the cached scroll position
            objectDisplay_.getScrollPanel().setVerticalScrollPosition(
                    deferredScrollPosition_);

         }
      });
   }

   private boolean matchesFilter(RObject obj)
   {
      if (filterText_.isEmpty())
         return true;
      return obj.getName().toLowerCase().contains(filterText_) ||
             obj.getValue().toLowerCase().contains(filterText_);
   }
   
   private RObjectEntry entryFromRObject(RObject obj)
   {
      return new RObjectEntry(obj, matchesFilter(obj));
   }
   
   private final static String EMPTY_GLOBAL_ENVIRONMENT_MESSAGE =
           "Global environment is empty";
   private final static String EMPTY_FUNCTION_ENVIRONMENT_MESSAGE =
           "Function environment is empty";

   public static final int OBJECT_LIST_VIEW = 0;
   public static final int OBJECT_GRID_VIEW = 1;

   @UiField EnvironmentStyle style;
   @UiField AutoHidingSplitLayoutPanel splitPanel;

   EnvironmentObjectDisplay objectDisplay_;
   CallFramePanel callFramePanel_;
   Label environmentNameLabel_;
   Label environmentEmptyMessage_;

   private ListDataProvider<RObjectEntry> objectDataProvider_;
   private RObjectEntrySort objectSort_;

   private EnvironmentObjectsObserver observer_;
   private int contextDepth_;
   private int callFramePanelHeight_;
   private int objectDisplayType_ = OBJECT_LIST_VIEW;
   private String filterText_ = ""; 
   private String environmentName_;

   // deferred settings--set on load but not applied until we have data.
   private int deferredScrollPosition_ = 0;
   private JsArrayString deferredExpandedObjects_;
   private boolean pendingCallFramePanelSize_ = false;
}
