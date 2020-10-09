/*
 * PreferencesDialogBase.java
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
package org.rstudio.core.client.prefs;


import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationQuit;
import org.rstudio.studio.client.application.events.ReloadEvent;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.model.Session;

public abstract class PreferencesDialogBase<T> extends ModalDialogBase
{
   protected PreferencesDialogBase(String caption,
                                   String panelContainerStyle,
                                   String panelContainerStyleNoChooser,
                                   boolean showApplyButton,
                                   List<PreferencesDialogPaneBase<T>> panes)
   {
      super(Roles.getDialogRole());
      setText(caption);
      
      panes_ = panes;
      panelContainerStyle_ = panelContainerStyle;
      panelContainerStyleNoChooser_ = panelContainerStyleNoChooser;

      PreferencesDialogBaseResources res = PreferencesDialogBaseResources.INSTANCE;

      sectionChooser_ = new SectionChooser(caption);

      ThemedButton okButton = new ThemedButton(
            "OK",
            clickEvent -> attemptSaveChanges(() -> closeDialog()));
      addOkButton(okButton, ElementIds.PREFERENCES_CONFIRM);
      addCancelButton();

      if (showApplyButton)
      {
         addButton(new ThemedButton("Apply",
                                    clickEvent -> attemptSaveChanges()),
                                    ElementIds.DIALOG_APPLY_BUTTON);
      }

      progressIndicator_ = addProgressIndicator(false);
      panel_ = new DockLayoutPanel(Unit.PX);
      panel_.setStyleName(panelContainerStyle_);
      container_ = new FlowPanel();
      container_.getElement().getStyle().setPaddingLeft(10, Unit.PX);

      addStyleName(res.styles().preferencesDialog());

      for (final PreferencesDialogPaneBase<T> pane : panes_)
      {
         Id sectionTabId = sectionChooser_.addSection(pane.getIcon(), pane.getName());
         pane.getElement().setId(SectionChooser.getTabPanelId(sectionTabId).getAriaValue());
         Roles.getTabpanelRole().setAriaLabelledbyProperty(pane.getElement(), sectionTabId);
         pane.setWidth("100%");
         pane.setDialog(this);
         pane.setProgressIndicator(progressIndicator_);
         container_.add(pane);
         setPaneVisibility(pane, false);
         pane.addEnsureVisibleHandler(
               ensureVisibleEvent -> sectionChooser_.select(container_.getWidgetIndex(pane)));
      }

      panel_.addWest(sectionChooser_, sectionChooser_.getDesiredWidth());
      panel_.add(container_);

      sectionChooser_.addSelectionHandler(selectionEvent ->
      {
         Integer index = selectionEvent.getSelectedItem();

         // SectionChooser is first focusable control in the modal dialog, and it
         // uses a roving tabindex to determine the focused section tab, so notify dialog
         // when selected tab changes (except the initial selection, which happens before
         // the dialog is fully assembled and thus nothing is focusable, yet)
         if (currentIndex_ != null)
            refreshFocusableElements();

         if (currentIndex_ != null)
            setPaneVisibility(panes_.get(currentIndex_), false);

         currentIndex_ = index;

         if (currentIndex_ != null)
            setPaneVisibility(panes_.get(currentIndex_), true);
      });

      sectionChooser_.select(0);
   }

   public void initialize(T prefs)
   {
      for (PreferencesDialogPaneBase<T> pane : panes_)
      {
         pane.initialize(prefs);
      }
   }

   public void activatePane(int index)
   {
      sectionChooser_.select(index);
   }

   public void activatePane(Class<?> clazz)
   {
      for (int i = 0; i < panes_.size(); i++)
      {
         if (panes_.get(i).getClass() == clazz)
         {
            activatePane(i);
            break;
         }
      }
   }

   public void setShowPaneChooser(boolean showPaneChooser)
   {
      panel_.setWidgetHidden(sectionChooser_, !showPaneChooser);
      if (showPaneChooser)
      {
         panel_.removeStyleName(panelContainerStyleNoChooser_);
         panel_.addStyleName(panelContainerStyle_);
      }
      else
      {
         panel_.removeStyleName(panelContainerStyle_);
         panel_.addStyleName(panelContainerStyleNoChooser_);
      }
   }

   private void setPaneVisibility(PreferencesDialogPaneBase<T> pane, boolean visible)
   {
      pane.setPaneVisible(visible);
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

   protected void hidePane(Class<?> clazz)
   {
      for (int i = 0; i < panes_.size(); i++)
      {
         if (panes_.get(i).getClass() == clazz)
         {
            hidePane(i);
            break;
         }
      }
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
         RestartRequirement restartRequirement = new RestartRequirement();
         for (PreferencesDialogPaneBase<T> pane : panes_)
         {
            restartRequirement.mergeRequirements(pane.onApply(prefs));
         }

         // perform save
         progressIndicator_.onProgress("Saving...");
         doSaveChanges(prefs, onCompleted, progressIndicator_, restartRequirement);
      }
   }

   protected abstract T createEmptyPrefs();

   protected abstract void doSaveChanges(T prefs,
                                         Operation onCompleted,
                                         ProgressIndicator progressIndicator,
                                         RestartRequirement restartRequirement);
   
   protected void handleRestart(GlobalDisplay display,
                                ApplicationQuit quit,
                                Session session,
                                RestartRequirement requirement)
   {
      boolean restartIde =
            requirement.getDesktopRestartRequired() ||
            (requirement.getUiReloadRequired() &&
             requirement.getSessionRestartRequired());
      
      if (restartIde)
      {
         restart(display, quit, session);
      }
      else if (requirement.getUiReloadRequired())
      {
         reload();
      }
      else if (requirement.getSessionRestartRequired())
      {
         restartSession(display);
      }
   }

   protected void reload()
   {
      RStudioGinjector.INSTANCE.getEventBus().fireEvent(new ReloadEvent());
   }

   protected void restart(GlobalDisplay globalDisplay,
                          ApplicationQuit quit,
                          Session session)
   {
      globalDisplay.showYesNoMessage(
            GlobalDisplay.MSG_QUESTION,
            "Restart Required",
            "You need to restart RStudio in order for these changes to take effect. " +
                  "Do you want to do this now?",
            () -> onRestart(quit, session),
            true);
   }
   
   private void onRestart(ApplicationQuit quit,
                          Session session)
   {
      closeDialog();
      quit.doRestart(session);
   }
   
   protected void restartSession(GlobalDisplay display)
   {
      display.showYesNoMessage(
            GlobalDisplay.MSG_QUESTION,
            "Restart Required",
            "You need to restart the R session in order for these changes to take effect. " +
            "Do you want to do this now?",
            () -> onRestartSession(),
            true);
   }
   
   private void onRestartSession()
   {
      closeDialog();
      RStudioGinjector.INSTANCE.getCommands().restartR().execute();
   }

   void forceClosed(final Command onClosed)
   {
      attemptSaveChanges(() ->
      {
         closeDialog();
         onClosed.execute();
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
   private List<PreferencesDialogPaneBase<T>> panes_;
   private FlowPanel container_;
   private Integer currentIndex_;
   private final ProgressIndicator progressIndicator_;
   private final SectionChooser sectionChooser_;
   private final String panelContainerStyle_;
   private final String panelContainerStyleNoChooser_;
}
