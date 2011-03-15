package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.EnsureVisibleHandler;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;

public class PreferencesDialog extends ModalDialog<Void>
{
   @Inject
   public PreferencesDialog(WorkbenchServerOperations server,
                            Session session,
                            PreferencesDialogResources res,
                            final SectionChooser sectionChooser,
                            Provider<GeneralPreferencesPane> pR,
                            EditingPreferencesPane source,
                            AppearancePreferencesPane appearance,
                            PaneLayoutPreferencesPane paneLayout,
                            GlobalDisplay globalDisplay)
   {
      super("Preferences", (OperationWithInput<Void>)null);

      addButton(new ThemedButton("Apply", new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            if (validate(Void.create()))
               onSuccess();
         }
      }));

      session_ = session;
      globalDisplay_ = globalDisplay;
      server_ = server;
      panel_ = new DockLayoutPanel(Unit.PX);
      panel_.setStyleName(res.styles().outer());
      container_ = new LayoutPanel();

      addStyleName(res.styles().preferencesDialog());

      panes_ = new PreferencesPane[] {pR.get(), source, appearance, paneLayout};

      for (final PreferencesPane pane : panes_)
      {
         sectionChooser.addSection(pane.getIcon(), pane.getName());
         container_.add(pane);
         container_.setWidgetLeftRight(pane, 12, Unit.PX, 0, Unit.PX);
         container_.setWidgetTopBottom(pane, 0, Unit.PX, 0, Unit.PX);
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

   private void setPaneVisibility(PreferencesPane pane, boolean visible)
   {
      Element el = container_.getWidgetContainerElement(pane);
      el.getStyle().setDisplay(visible ? Display.BLOCK : Display.NONE);
   }

   @Override
   protected Void collectInput()
   {
      return Void.create();
   }

   @Override
   protected boolean validate(Void input)
   {
      for (PreferencesPane pane : panes_)
         if (!pane.validate())
            return false;
      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      return panel_;
   }

   @Override
   protected void onSuccess()
   {
      for (PreferencesPane pane : panes_)
         pane.onApply();

      server_.setUiPrefs(session_.getSessionInfo().getUiPrefs(), new ServerRequestCallback<Void>()
      {
         @Override
         public void onError(ServerError error)
         {
            globalDisplay_.showErrorMessage("Error Saving Preferences",
                                            error.getUserMessage());
         }
      });
   }

   public static void ensureStylesInjected()
   {
      GWT.<PreferencesDialogResources>create(PreferencesDialogResources.class).styles().ensureInjected();
   }


   private DockLayoutPanel panel_;
   private PreferencesPane[] panes_;
   private LayoutPanel container_;
   private Integer currentIndex_;
   private final WorkbenchServerOperations server_;
   private final Session session_;
   private final GlobalDisplay globalDisplay_;
}
