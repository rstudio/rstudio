/*
 * ConsoleInterpreterVersion.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.model.ApplicationServerOperations;
import org.rstudio.studio.client.application.model.RVersionSpec;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.events.ReticulateEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.views.PythonInterpreter;
import org.rstudio.studio.client.workbench.views.console.shell.ConsoleLanguageTracker;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ConsoleInterpreterVersion
   extends Composite
   implements ReticulateEvent.Handler

{
   @Inject
   private void initialize(Session session,
                           Commands commands,
                           EventBus events,
                           ConsoleLanguageTracker tracker,
                           DependencyManager depoman,
                           ApplicationServerOperations server)
   {
      session_ = session;
      commands_ = commands;
      events_ = events;
      tracker_ = tracker;
      depoman_ = depoman;
      server_ = server;
   }
   
   public ConsoleInterpreterVersion()
   {
      this(false);
   }
   
   // isTabbedView is used to control styling based on whether
   // this widget is displayed in the "tabbed" version of the Console Pane
   // versus the "untabbed" version (when no other tabs are available)
   public ConsoleInterpreterVersion(boolean isTabbedView)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      events_.addHandler(ReticulateEvent.TYPE, this);
      container_ = new HorizontalPanel();
      label_ = new Label(constants_.unknownLabel());
      setRVersionLabel();
      
      rLogo_ = createLogo(StandardIcons.INSTANCE.rLogoSvg(), RES.styles().iconR(), isTabbedView);
      
      pythonLogo_ = createLogo(StandardIcons.INSTANCE.pythonLogoSvg(), RES.styles().iconPython(),
            isTabbedView);
      
      logoContainer_ = new FlowPanel(SpanElement.TAG);
      container_.addStyleName(RES.styles().container());
      container_.addDomHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            ToolbarPopupMenu menu = new ToolbarPopupMenu();
            
            menu.addItem(new MenuItem("R", () -> {
               tracker_.adaptToLanguage(ConsoleLanguageTracker.LANGUAGE_R, () -> {
                  commands_.activateConsole().execute();
               });
            }));
            
            menu.addItem(new MenuItem("Python", () -> {
               tracker_.adaptToLanguage(ConsoleLanguageTracker.LANGUAGE_PYTHON, () -> {
                  commands_.activateConsole().execute();
               });
            }));
            
            logoContainer_.addStyleName(RES.styles().menuClicked());
            menu.addCloseHandler(new CloseHandler<PopupPanel>()
            {
               @Override
               public void onClose(CloseEvent<PopupPanel> event)
               {
                  logoContainer_.removeStyleName(RES.styles().menuClicked());
               }
            });
            
            menu.showRelativeTo(container_);
         }
      }, ClickEvent.getType());
      
      label_.addStyleName(RES.styles().label());
      label_.addStyleName(ThemeStyles.INSTANCE.title());
      label_.addStyleName(isTabbedView ? RES.styles().labelTabbed() : RES.styles().labelUntabbed());
      ElementIds.assignElementId(label_,
            ElementIds.CONSOLE_INTERPRETER_VERSION + (isTabbedView ? "_tabbed" : ""));

      if (isPythonActive())
      {
         logoContainer_.add(pythonLogo_);
      }
      else
      {
         logoContainer_.add(rLogo_);
      }
      container_.add(logoContainer_);
      
      container_.add(label_);
      initWidget(container_);
      
      setVisible(true);
   }
   
   private Widget createLogo(TextResource resource,
                           String languageClass,
                           boolean isTabbedView)
   {
      HorizontalPanel panel = new HorizontalPanel();
      
      HTML html = new HTML();
      html.setHTML(resource.getText());
      html.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
      
      Element svg = html.getElement().getFirstChildElement();
      svg.setAttribute("role", "presentation");

      // NOTE: GWT chokes when modifying the className
      // attribute of an SVG element so we do it "by hand" here
      addClassName(svg, RES.styles().icon());
      addClassName(svg, isTabbedView
            ? RES.styles().iconTabbed()
            : RES.styles().iconUntabbed());
      addClassName(svg, languageClass);
      
      panel.add(html);
      
      Image downArrow = new Image(new ImageResource2x(ThemeResources.INSTANCE.menuDownArrow2x()));
      downArrow.getElement().addClassName("rstudio-themes-inverts");
      downArrow.addStyleName(RES.styles().iconDownArrow());
      
      // this control is not operable via keyboard or screen reader, so for now we'll just hide it
      // from screen readers altogether instead of having an unlabeled image
      downArrow.setAltText("");
      panel.add(downArrow);
      
      return panel;
   }
   
   public void adaptToR()
   {
      logoContainer_.remove(0);
      logoContainer_.insert(rLogo_, 0);
      setRVersionLabel();
   }

   private void adaptToPython(PythonInterpreter info)
   {
      logoContainer_.remove(0);
      logoContainer_.insert(pythonLogo_, 0);
      label_.setText(pythonVersionLabel(info));
   }
   
   private boolean isPythonActive()
   {
      boolean active = false;
      
      // use try-catch block in case session info isn't ready yet
      try
      {
         active = session_.getSessionInfo().getPythonReplActive();
      }
      catch (Exception e)
      {
      }
      
      return active;
   }
   
   @Override
   public void onReticulate(ReticulateEvent event)
   {
      String type = event.getType();
      
      if (StringUtil.equals(type, ReticulateEvent.TYPE_REPL_INITIALIZED))
      {
         PythonInterpreter info = event.getPayload().cast();
         adaptToPython(info);
      }
      else if (StringUtil.equals(type, ReticulateEvent.TYPE_REPL_TEARDOWN))
      {
         adaptToR();
      }
   }

   private void setRVersionLabel()
   {
      server_.getRVersion(new ServerRequestCallback<RVersionSpec>()
      {
         @Override
         public void onResponseReceived(RVersionSpec versionSpec)
         {
            label_.setText("R " + versionSpec.getVersion());
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
            label_.setText("Error fetching R version");
         }
      });
   }
   
   private String pythonVersionLabel(PythonInterpreter info)
   {
      return "Python " + info.getVersion();
   }
   
   public int getWidth()
   {
      return 64;
   }
   
   public int getHeight()
   {
      return 18;
   }
   
   private final native void addClassName(Element element, String className)
   /*-{
      element.classList.add(className);
   }-*/;
   
   private final HorizontalPanel container_;
   private final FlowPanel logoContainer_;
   private final Widget rLogo_;
   private final Widget pythonLogo_;
   private final Label label_;
   
   // Injected ----
   private Session session_;
   private Commands commands_;
   private EventBus events_;
   private ConsoleLanguageTracker tracker_;
   private DependencyManager depoman_;
   private ApplicationServerOperations server_;
   
   // Resources ----
   
   public interface Styles extends CssResource
   {
      String container();
      String menuClicked();
      
      String icon();
      String iconTabbed();
      String iconUntabbed();
      String iconDownArrow();
      
      String iconR();
      String iconPython();
      
      String label();
      String labelTabbed();
      String labelUntabbed();
   }

   public interface Resources extends ClientBundle
   {
      @Source("ConsoleInterpreterVersion.css")
      Styles styles();
   }

   private static Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }
   private static final ConsoleConstants constants_ = GWT.create(ConsoleConstants.class);

}
