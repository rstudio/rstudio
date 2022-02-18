/*
 * ConsoleInterpreterVersion.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.console;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.events.ReticulateEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.views.PythonInterpreter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

public class ConsoleInterpreterVersion
   extends Composite
   implements ReticulateEvent.Handler

{
   @Inject
   private void initialize(Session session,
                           EventBus events)
   {
      session_ = session;
      events_ = events;
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
      
      container_ = new FlowPanel();
      label_ = new Label(rVersionLabel());
      
      rLogo_ = createLogo(
           StandardIcons.INSTANCE.rLogoSvg(),
           RES.styles().iconR(),
           isTabbedView);
      
      pythonLogo_ = createLogo(
            StandardIcons.INSTANCE.pythonLogoSvg(),
            RES.styles().iconPython(),
            isTabbedView);
      
      container_.addStyleName(RES.styles().container());
      
      label_.addStyleName(RES.styles().label());
      label_.addStyleName(ThemeStyles.INSTANCE.title());
      label_.addStyleName(isTabbedView
            ? RES.styles().labelTabbed()
            : RES.styles().labelUntabbed());
      ElementIds.assignElementId(label_, ElementIds.CONSOLE_INTERPRETER_VERSION + (isTabbedView ? "_tabbed" : ""));

      if (isPythonActive())
      {
         container_.add(pythonLogo_);
      }
      else
      {
         container_.add(rLogo_);
      }
      
      container_.add(label_);
      initWidget(container_);
      
      setVisible(true);
   }
   
   private HTML createLogo(TextResource resource,
                           String languageClass,
                           boolean isTabbedView)
   {
      HTML html = new HTML();
      html.setHTML(resource.getText());
      html.getElement().getStyle().setDisplay(Display.INLINE_BLOCK);
      
      Element svg = html.getElement().getFirstChildElement();
      
      // NOTE: GWT chokes when modifying the className
      // attribute of an SVG element so we do it "by hand" here
      addClassName(svg, RES.styles().icon());
      addClassName(svg, isTabbedView
            ? RES.styles().iconTabbed()
            : RES.styles().iconUntabbed());
      addClassName(svg, languageClass);
      
      return html;
   }
   
   private void adaptToR()
   {
      container_.remove(0);
      container_.insert(rLogo_, 0);
      label_.setText(rVersionLabel());
      
   }
   
   private void adaptToPython(PythonInterpreter info)
   {
      container_.remove(0);
      container_.insert(pythonLogo_, 0);
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
   
   
   public String rVersionLabel()
   {
      String version = constants_.unknownLabel();
      
      try
      {
         version = session_
               .getSessionInfo()
               .getRVersionsInfo()
               .getRVersion();
      }
      catch (Exception e)
      {
      }
      
      return "R " + version;
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
   
   private final FlowPanel container_;
   private final HTML rLogo_;
   private final HTML pythonLogo_;
   private final Label label_;
   
   // Injected ----
   private Session session_;
   private EventBus events_;
   
   // Resources ----
   
   public interface Styles extends CssResource
   {
      String container();
      
      String icon();
      String iconTabbed();
      String iconUntabbed();
      
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
