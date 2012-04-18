/*
 * PreferencesDialogBase.java
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
package org.rstudio.core.client.prefs;


import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.ReloadEvent;

public abstract class PreferencesDialogBase<T> extends ModalDialogBase
{
   protected PreferencesDialogBase(String caption,
                                   String panelContainerStyle,
                                   boolean showApplyButton,
                                   PreferencesDialogPaneBase<T>[] panes)
   {
      super();
      setText(caption);
      panes_ = panes;
      
      PreferencesDialogBaseResources res = 
                                   PreferencesDialogBaseResources.INSTANCE;
      
      sectionChooser_ = new SectionChooser();
      
      ThemedButton okButton = new ThemedButton("OK", new ClickHandler() {
         public void onClick(ClickEvent event) 
         {
            attemptSaveChanges(new Operation() {
               @Override
               public void execute()
               {
                  closeDialog();
               }
            });        
         }
      });
      addOkButton(okButton);
      addCancelButton();
      
      if (showApplyButton)
      {
         addButton(new ThemedButton("Apply", new ClickHandler()
         {
            public void onClick(ClickEvent event)
            {
               attemptSaveChanges();
            }
         }));
      }
      
      progressIndicator_ = addProgressIndicator(false);
      panel_ = new DockLayoutPanel(Unit.PX);
      panel_.setStyleName(panelContainerStyle);
      container_ = new FlowPanel();
      container_.getElement().getStyle().setPaddingLeft(10, Unit.PX);

      addStyleName(res.styles().preferencesDialog());

       
      for (final PreferencesDialogPaneBase<T> pane : panes_)
      {
         sectionChooser_.addSection(pane.getIcon(), pane.getName());
         pane.setWidth("100%");
         pane.setDialog(this);
         pane.setProgressIndicator(progressIndicator_);
         container_.add(pane);
         setPaneVisibility(pane, false);
         pane.addEnsureVisibleHandler(new EnsureVisibleHandler()
         {
            public void onEnsureVisible(EnsureVisibleEvent event)
            {
               sectionChooser_.select(container_.getWidgetIndex(pane));
            }
         });
      }

      panel_.addWest(sectionChooser_, sectionChooser_.getDesiredWidth());
      panel_.add(container_);

      sectionChooser_.addSelectionHandler(new SelectionHandler<Integer>()
      {
         public void onSelection(SelectionEvent<Integer> e)
         {
            Integer index = e.getSelectedItem();

            if (currentIndex_ != null)
               setPaneVisibility(panes_[currentIndex_], false);

            currentIndex_ = index;

            if (currentIndex_ != null)
               setPaneVisibility(panes_[currentIndex_], true);
         }
      });

      sectionChooser_.select(0);
   }
   
   public void initialize(T prefs)
   {
      for (PreferencesDialogPaneBase<T> pane : panes_)
         pane.initialize(prefs);
   }
   
   public void activatePane(int index)
   {
      sectionChooser_.select(index);
   }
   
   private void setPaneVisibility(PreferencesDialogPaneBase<T> pane, boolean visible)
   {
      pane.getElement().getStyle().setDisplay(visible
                                              ? Display.BLOCK
                                              : Display.NONE);
   }

   @Override
   protected Widget createMainWidget()
   {
      return panel_;
   }
   
   protected void hidePane(int index)
   {
      sectionChooser_.hideSection(index);
   }
   
   protected void attemptSaveChanges()
   {
      attemptSaveChanges(null);
   }
   
   private void attemptSaveChanges(final Operation onCompleted)
   {
      if (validate())
      {
         // apply changes
         T prefs = createEmptyPrefs();
         boolean restartRequired = false;
         for (PreferencesDialogPaneBase<T> pane : panes_)
            if (pane.onApply(prefs))
               restartRequired = true;

         // perform save
         progressIndicator_.onProgress("Saving...");
         doSaveChanges(prefs, onCompleted, progressIndicator_, restartRequired);
      }
   }
   
   protected abstract T createEmptyPrefs();
   
   protected abstract void doSaveChanges(T prefs,
                                         Operation onCompleted,
                                         ProgressIndicator progressIndicator,
                                         boolean reload);

   protected void reload()
   {
      RStudioGinjector.INSTANCE.getEventBus().fireEvent(new ReloadEvent());
   }
   
   
   void forceClosed(final Command onClosed)
   {
      attemptSaveChanges(new Operation() {
         @Override
         public void execute()
         {
            closeDialog();
            onClosed.execute();
         }
      });      
   }
   
   private boolean validate()
   {
      for (PreferencesDialogPaneBase<T> pane : panes_)
         if (!pane.validate())
            return false;
      return true;
   }

   private DockLayoutPanel panel_;
   private PreferencesDialogPaneBase<T>[] panes_;
   private FlowPanel container_;
   private Integer currentIndex_;
   private final ProgressIndicator progressIndicator_;
   private final SectionChooser sectionChooser_;
}
