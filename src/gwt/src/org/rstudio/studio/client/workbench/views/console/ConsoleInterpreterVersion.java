/*
 * ConsoleInterpreterVersion.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.events.ReticulateEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.views.PythonInterpreter;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
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
      icon_ = new Image(StandardIcons.INSTANCE.rLogoSvg().getSafeUri());
      
      container_.addStyleName(RES.styles().container());
      
      label_.addStyleName(RES.styles().label());
      label_.addStyleName(ThemeStyles.INSTANCE.title());
      label_.addStyleName(isTabbedView
            ? RES.styles().labelTabbed()
            : RES.styles().labelUntabbed());
      
      icon_.addStyleName(RES.styles().icon());
      
      icon_.addStyleName(isTabbedView
            ? RES.styles().iconTabbed()
            : RES.styles().iconUntabbed());
      
      container_.add(icon_);
      container_.add(label_);
      initWidget(container_);
      
      setVisible(true);
   }
   
   private void adaptToR()
   {
      icon_.removeStyleName(RES.styles().iconPython());
      icon_.addStyleName(RES.styles().iconR());
      icon_.setUrl(StandardIcons.INSTANCE.rLogoSvg().getSafeUri());
      label_.setText(rVersionLabel());
   }
   
   private void adaptToPython(PythonInterpreter info)
   {
      icon_.removeStyleName(RES.styles().iconR());
      icon_.addStyleName(RES.styles().iconPython());
      icon_.setUrl(StandardIcons.INSTANCE.pythonLogoSvg().getSafeUri());
      label_.setText(pythonVersionLabel(info));
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
   
   
   private String rVersionLabel()
   {
      String version = "(unknown)";
      
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
   
   private final FlowPanel container_;
   private final Image icon_;
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

}
