/*
 * AceThemeResources.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text.themes;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import org.rstudio.core.client.resources.StaticDataResource;

public interface AceThemeResources extends ClientBundle
{
   AceThemeResources INSTANCE = GWT.create(AceThemeResources.class);
   
   @Source("ambiance.css")
   StaticDataResource ambiance();

   @Source("chaos.css")
   StaticDataResource chaos();

   @Source("chrome.css")
   StaticDataResource chrome();

   @Source("clouds_midnight.css")
   StaticDataResource clouds_midnight();

   @Source("clouds.css")
   StaticDataResource clouds();

   @Source("cobalt.css")
   StaticDataResource cobalt();

   @Source("crimson_editor.css")
   StaticDataResource crimson_editor();

   @Source("dawn.css")
   StaticDataResource dawn();

   @Source("dreamweaver.css")
   StaticDataResource dreamweaver();

   @Source("eclipse.css")
   StaticDataResource eclipse();

   @Source("idle_fingers.css")
   StaticDataResource idle_fingers();

   @Source("katzenmilch.css")
   StaticDataResource katzenmilch();

   @Source("kr_theme.css")
   StaticDataResource kr_theme();

   @Source("merbivore_soft.css")
   StaticDataResource merbivore_soft();

   @Source("merbivore.css")
   StaticDataResource merbivore();

   @Source("mono_industrial.css")
   StaticDataResource mono_industrial();

   @Source("monokai.css")
   StaticDataResource monokai();

   @Source("pastel_on_dark.css")
   StaticDataResource pastel_on_dark();

   @Source("solarized_dark.css")
   StaticDataResource solarized_dark();

   @Source("solarized_light.css")
   StaticDataResource solarized_light();

   @Source("textmate.css")
   StaticDataResource textmate();

   @Source("tomorrow_night_blue.css")
   StaticDataResource tomorrow_night_blue();

   @Source("tomorrow_night_bright.css")
   StaticDataResource tomorrow_night_bright();

   @Source("tomorrow_night_eighties.css")
   StaticDataResource tomorrow_night_eighties();

   @Source("tomorrow_night.css")
   StaticDataResource tomorrow_night();

   @Source("tomorrow.css")
   StaticDataResource tomorrow();

   @Source("twilight.css")
   StaticDataResource twilight();

   @Source("vibrant_ink.css")
   StaticDataResource vibrant_ink();

   @Source("xcode.css")
   StaticDataResource xcode();
   
}
