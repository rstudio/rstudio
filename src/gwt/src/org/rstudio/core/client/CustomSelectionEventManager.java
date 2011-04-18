package org.rstudio.core.client;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.DefaultSelectionEventManager.EventTranslator;
import com.google.gwt.view.client.DefaultSelectionEventManager.SelectAction;

public class CustomSelectionEventManager<T>
{
   // custom event manager which adds (unmodified) single-click to unselect
   // behavior to the standard multi-selection model
   public static <T> DefaultSelectionEventManager<T> createClickToUnselectManager(
         final MultiSelectionModel<T> selectionModel)
   {
      return DefaultSelectionEventManager.createCustomManager(
         new EventTranslator<T>() { 
            @Override
            public boolean clearCurrentSelection(
                  CellPreviewEvent<T> event)
            {
               // if there is a single selected item and this is a click
               // on it then clear the current selection
               if (isUnselectClick(event))
               {
                  return true;
               }
               else
               {
                  // need to mirror behavior of:
                  //   DefaultSelectionEventManager.handleMultiSelectionEvent
                  // which basically says clear the current selection unless
                  // the control or meta key is down
                  NativeEvent nativeEvent = event.getNativeEvent();
                  return !nativeEvent.getCtrlKey() && !nativeEvent.getMetaKey();
               }
            }

            @Override
            public SelectAction translateSelectionEvent(
                  CellPreviewEvent<T> event)
            {
               // if there is a single selected item and this is a click 
               // on it then clear the current selection
               if (isUnselectClick(event))
                  return SelectAction.DESELECT;
               else
                  return SelectAction.DEFAULT;
            }

            private boolean isUnselectClick(CellPreviewEvent<T> event)
            {
               if ("click".equals(event.getNativeEvent().getType()) &&
                     selectionModel.isSelected(event.getValue()) && 
                     selectionModel.getSelectedSet().size() == 1)
               {
                  return true;
               }
               else
               {
                  return false;
               }
            }    
         });
   }
}
