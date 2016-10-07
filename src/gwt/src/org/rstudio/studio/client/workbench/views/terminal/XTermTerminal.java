/*
 * XTermTerminal.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.terminal;

import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.resources.StaticDataResource;
import org.rstudio.studio.client.common.SuperDevMode;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermResources;

import com.google.gwt.dom.client.Element;
import com.google.inject.Inject;

public class XTermTerminal
{
   public static final native XTermTerminal getTerminal(Element el)
   /*-{
      for (; el != null; el = el.parentElement)
         if (el.$RStudioXTermTerminal != null)
            return el.$RStudioXTermTerminal;
   }-*/;
   
   private static final native void attachToWidget(Element el, XTermTerminal editor)
   /*-{
      el.$RStudioXTermTerminal= editor;
   }-*/;
   
   private static final native void detachFromWidget(Element el)
   /*-{
      el.$RStudioXTermTerminal = null;
   }-*/;

   @Inject
   public XTermTerminal()
   {
      widget_ = new XTermWidget();
   }
   
   private static final ExternalJavaScriptLoader getLoader(StaticDataResource release)
   {
      return getLoader(release, null);
   }
   
   private final XTermWidget widget_;
                                                           
   private static final ExternalJavaScriptLoader getLoader(StaticDataResource release,
                                                           StaticDataResource debug)
   {
      if (debug == null || !SuperDevMode.isActive())
         return new ExternalJavaScriptLoader(release.getSafeUri().asString());
      else
         return new ExternalJavaScriptLoader(debug.getSafeUri().asString());
   }
   
   private static final ExternalJavaScriptLoader xtermLoader_ =
         getLoader(XTermResources.INSTANCE.xtermjs(), XTermResources.INSTANCE.xtermjs() /*TODO uncompressed flavor */);
   
}
