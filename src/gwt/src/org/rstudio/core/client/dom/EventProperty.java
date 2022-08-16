/*
 * EventProperty.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.dom;

import com.google.gwt.dom.client.NativeEvent;

public class EventProperty
{
   public static final native String key(NativeEvent event) /*-{ return event.key; }-*/;
   
   // This helper is provided to avoid GWT's normalization of MouseEvent
   // buttons in the 'event.getButton()' accessor.
   public static final native int button(NativeEvent event)
   /*-{
      return event.button;
   }-*/;
   
   public static final int MOUSE_MAIN       = 0;
   public static final int MOUSE_AUXILIARY  = 1;
   public static final int MOUSE_SECONDARY  = 2;
   public static final int MOUSE_BACKWARD   = 3;
   public static final int MOUSE_FORWARD    = 4;
}

