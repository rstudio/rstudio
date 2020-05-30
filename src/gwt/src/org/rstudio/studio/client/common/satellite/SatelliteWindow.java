/*
 * SatelliteWindow.java
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
package org.rstudio.studio.client.common.satellite;


import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.widget.AriaLiveStatusWidget;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.ModalDialogTracker;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Timing;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ThemeChangedEvent;
import org.rstudio.studio.client.application.ui.RStudioThemes;
import org.rstudio.studio.client.common.satellite.events.SatelliteWindowEventHandlers;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.ProvidesResize;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.inject.Provider;


public abstract class SatelliteWindow extends Composite
                                      implements RequiresResize, 
                                                 ProvidesResize,
                                                 SatelliteWindowEventHandlers
{
   public SatelliteWindow(Provider<EventBus> pEventBus,
                          Provider<FontSizeManager> pFontSizeManager)
   {
      // save references
      pEventBus_ = pEventBus;
      pFontSizeManager_ = pFontSizeManager;

      pEventBus_.get().addHandler(ThemeChangedEvent.TYPE, this);
      pEventBus_.get().addHandler(AriaLiveStatusEvent.TYPE, this);
      
      // occupy full client area of the window
      if (!allowScrolling())
         Window.enableScrolling(false);
      Window.setMargin("0px");

      // create application panel
      mainPanel_ = new LayoutPanel();
      ElementIds.assignElementId(mainPanel_.getElement(), ElementIds.SATELLITE_PANEL);

      // Register an event handler for themes so it will be triggered after a
      // UIPrefsChangedEvent updates the theme. Do this after SessionInit (if we
      // do it beforehand we'll trigger the event before the SessionInfo object
      // arrives with the theme settings)
      pEventBus.get().addHandler(SessionInitEvent.TYPE, (evt) ->
      {
         UserPrefs userPrefs = RStudioGinjector.INSTANCE.getUserPrefs();
         userPrefs.editorTheme().bind(theme -> pEventBus_.get().fireEvent(new ThemeChangedEvent()));
         userPrefs.globalTheme().bind(theme -> pEventBus_.get().fireEvent(new ThemeChangedEvent()));
      });

      // aria-live status announcements
      ariaLiveStatusWidget_ = new AriaLiveStatusWidget();
      mainPanel_.add(ariaLiveStatusWidget_);
      A11y.setVisuallyHidden(mainPanel_.getWidgetContainerElement(ariaLiveStatusWidget_));

      // init widget
      initWidget(mainPanel_);
   }

   public boolean supportsThemes()
   {
      return false;
   }

   @Override
   public void onThemeChanged(ThemeChangedEvent event)
   {
      // By default, we only apply the flat theme to match other dialogs, then
      // specific satellites can opt in to full theming using `supportsThemes()`.
      if (supportsThemes())
      {
         RStudioThemes.initializeThemes(
            RStudioGinjector.INSTANCE.getUserPrefs(),
            RStudioGinjector.INSTANCE.getUserState(),
            Document.get(),
            mainPanel_.getElement());
            
         RStudioGinjector.INSTANCE.getAceThemes().applyTheme(Document.get());
      }
   }

   protected boolean allowScrolling()
   {
      return false;
   }

   // show the satellite window (subclasses shouldn't override this method,
   // rather they should override the abstract onInitialize method)
   public void show(JavaScriptObject params)
   {
      // react to font size changes
      EventBus eventBus = pEventBus_.get();
      eventBus.addHandler(ChangeFontSizeEvent.TYPE, changeFontSizeEvent ->
      {
            FontSizer.setNormalFontSize(Document.get(), changeFontSizeEvent.getFontSize());
      });
      FontSizeManager fontSizeManager = pFontSizeManager_.get();
      FontSizer.setNormalFontSize(Document.get(), fontSizeManager.getSize());

      // disable no handler assertions
      AppCommand.disableNoHandlerAssertions();
      
      // allow subclasses to initialize
      onInitialize(mainPanel_, params);
   }

   @Override
   public void onResize()
   {
      mainPanel_.onResize(); 
   }

   @Override
   public void onAriaLiveStatus(AriaLiveStatusEvent event)
   {
      int delayMs = (event.getTiming() == Timing.IMMEDIATE) ?
            0 : RStudioGinjector.INSTANCE.getUserPrefs().typingStatusDelayMs().getValue();
      if (!ModalDialogTracker.dispatchAriaLiveStatus(event.getMessage(), delayMs, event.getSeverity()))
         ariaLiveStatusWidget_.reportStatus(event.getMessage(), delayMs, event.getSeverity());
   }

   abstract protected void onInitialize(LayoutPanel mainPanel, 
                                        JavaScriptObject params);
   
   protected LayoutPanel getMainPanel()
   {
      return mainPanel_;
   }

   private final Provider<EventBus> pEventBus_;
   private final Provider<FontSizeManager> pFontSizeManager_;
   private LayoutPanel mainPanel_;
   private final AriaLiveStatusWidget ariaLiveStatusWidget_;
}
