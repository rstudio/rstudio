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

//   @Source("clouds.css")
//   StaticDataResource clouds();
//
//   @Source("clouds_midnight.css")
//   StaticDataResource clouds_midnight();
//
//   @Source("dawn.css")
//   StaticDataResource dawn();
//
//   @Source("kr_theme.css")
//   StaticDataResource kr_theme();
//
//   @Source("merbivore.css")
//   StaticDataResource merbivore();
//
//   @Source("merbivore_soft.css")
//   StaticDataResource merbivore_soft();
//
//   @Source("mono_industrial.css")
//   StaticDataResource mono_industrial();
//
//   @Source("monokai.css")
//   StaticDataResource monokai();
//
//   @Source("pastel_on_dark.css")
//   StaticDataResource pastel_on_dark();
//
//   @Source("vibrant_ink.css")
//   StaticDataResource vibrant_ink();
}
