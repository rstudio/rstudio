/*
 * PreferencesDialog.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;

public class PreferencesDialog extends ModalDialogBase
{
   @Inject
   public PreferencesDialog(WorkbenchServerOperations server,
                            Session session,
                            PreferencesDialogResources res,
                            final SectionChooser sectionChooser,
                            Provider<GeneralPreferencesPane> pR,
                            ProjectsPreferencesPane projects,
                            EditingPreferencesPane source,
                            AppearancePreferencesPane appearance,
                            PaneLayoutPreferencesPane paneLayout)
   {
      super();

      setText("Options");
      
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
      
      addButton(new ThemedButton("Apply", new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            attemptSaveChanges();
         }
      }));

      session_ = session;
      progressIndicator_ = addProgressIndicator(false);
      server_ = server;
      panel_ = new DockLayoutPanel(Unit.PX);
      panel_.setStyleName(res.styles().outer());
      container_ = new FlowPanel();
      container_.getElement().getStyle().setPaddingLeft(12, Unit.PX);

      addStyleName(res.styles().preferencesDialog());

      if (session_.getSessionInfo().isProjectsEnabled())
      {
         panes_ = new PreferencesPane[] {pR.get(),
                                        projects,
                                        source, 
                                        appearance, 
                                        paneLayout};
      }
      else
      {
         panes_ = new PreferencesPane[] {pR.get(),
                                         source, 
                                         appearance, 
                                         paneLayout};
      }
         
      for (final PreferencesPane pane : panes_)
      {
         sectionChooser.addSection(pane.getIcon(), pane.getName());
         pane.setWidth("100%");
         container_.add(pane);
         setPaneVisibility(pane, false);
         pane.addEnsureVisibleHandler(new EnsureVisibleHandler()
         {
            public void onEnsureVisible(EnsureVisibleEvent event)
            {
               sectionChooser.select(container_.getWidgetIndex(pane));
            }
         });
      }

      panel_.addWest(sectionChooser, sectionChooser.getDesiredWidth());
      panel_.add(container_);

      sectionChooser.addSelectionHandler(new SelectionHandler<Integer>()
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

      sectionChooser.select(0);
   }
   
   public void initializeRPrefs(RPrefs rPrefs)
   {
      for (PreferencesPane pane : panes_)
         pane.initializeRPrefs(rPrefs);
   }

   private void setPaneVisibility(PreferencesPane pane, boolean visible)
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

   private void attemptSaveChanges()
   {
      attemptSaveChanges(null);
   }
   
   private void attemptSaveChanges(final Operation onCompleted)
   {
      if (validate())
      {
         progressIndicator_.onProgress("Saving options...");
         
         // apply changes
         RPrefs rPrefs = RPrefs.createEmpty();
         for (PreferencesPane pane : panes_)
            pane.onApply(rPrefs);

         // save changes
         server_.setPrefs(
            rPrefs, 
            session_.getSessionInfo().getUiPrefs(),
            new SimpleRequestCallback<Void>() {

               @Override
               public void onResponseReceived(Void response)
               {
                  progressIndicator_.onCompleted();
                  if (onCompleted != null)
                     onCompleted.execute();
               }

               @Override
               public void onError(ServerError error)
               {
                  progressIndicator_.onError(error.getUserMessage());
               }           
            });  
      }
   }
   
   private boolean validate()
   {
      for (PreferencesPane pane : panes_)
         if (!pane.validate())
            return false;
      return true;
   }

   public static void ensureStylesInjected()
   {
      GWT.<PreferencesDialogResources>create(PreferencesDialogResources.class).styles().ensureInjected();
   }


   private DockLayoutPanel panel_;
   private PreferencesPane[] panes_;
   private FlowPanel container_;
   private Integer currentIndex_;
   private final WorkbenchServerOperations server_;
   private final Session session_;
   private final ProgressIndicator progressIndicator_;
}
