/*
 * AceThemeResources.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.themes;

import com.google.gwt.resources.client.ClientBundle;
import org.rstudio.core.client.resources.StaticDataResource;

public interface AceThemeResources extends ClientBundle
{
   @Source("textmate.css")
   StaticDataResource textmate();

   @Source("eclipse.css")
   StaticDataResource eclipse();

   @Source("idle_fingers.css")
   StaticDataResource idle_fingers();

   @Source("twilight.css")
   StaticDataResource twilight();

   @Source("cobalt.css")
   StaticDataResource cobalt();

   @Source("solarized_light.css")
   StaticDataResource solarized();

   @Source("solarized_dark.css")
   StaticDataResource solarizedDark();

   @Source("tomorrow.css")
   StaticDataResource tomorrow();

   @Source("tomorrow_night.css")
   StaticDataResource tomorrow_night();
   
   @Source("tomorrow_night_blue.css")
   StaticDataResource tomorrow_night_blue();
   
   @Source("tomorrow_night_bright.css")
   StaticDataResource tomorrow_night_bright();  
   
   @Source("tomorrow_night_eighties.css")
   StaticDataResource tomorrow_night_eighties(); 
}
