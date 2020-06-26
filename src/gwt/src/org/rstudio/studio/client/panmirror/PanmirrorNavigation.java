/*
 * PanmirrorNavigation.java
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

package org.rstudio.studio.client.panmirror;


import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Base64Utils;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import jsinterop.annotations.JsType;


@JsType
public class PanmirrorNavigation
{    
   public static final String Pos = "pos";
   public static final String Id = "id";
   public static final String Href= "href";
   public static final String Heading = "heading";
   public static final String Top = "top";
   
   public static PanmirrorNavigation pos(int pos)
   {
      return new PanmirrorNavigation(Pos, Integer.toString(pos), pos);
   }
   
   public static PanmirrorNavigation id(String id)
   {
      return new PanmirrorNavigation(Id, id, 0);
   }
   
   public static PanmirrorNavigation href(String href)
   {
      return new PanmirrorNavigation(Href, href, 0);
   }
   
   public static PanmirrorNavigation heading(String heading)
   {
      return new PanmirrorNavigation(Heading, heading, 0);
   }
   
   public static PanmirrorNavigation top()
   {
      return new PanmirrorNavigation(Top, "2", 2);
   }
   
   public static PanmirrorNavigation fromSourcePosition(SourcePosition position)
   {
      String[] context = position.getContext().split(":");
      if (context.length == 3)
      {
         String type = context[1];
         String location = new String(Base64Utils.fromBase64(context[2]));
         return new PanmirrorNavigation(type, location, position.getRow());
      }
      else
      {
         return null;
      }
   }
   
   public static boolean isPanmirrorPosition(SourcePosition position)
   {
      String context = position.getContext();
      return context != null && context.startsWith(kPanmirrorContext + ":");
   }
   
   public static SourcePosition toSourcePosition(PanmirrorNavigation navigation)
   {
      List<String> context = new ArrayList<String>();
      context.add(kPanmirrorContext);
      context.add(navigation.type);
      context.add(Base64Utils.toBase64(navigation.location.getBytes()));
      return SourcePosition.create(StringUtil.join(context, ":"), navigation.pos, 0, -1);
   }
   
   
   public final String type;
   public final String location;
   public final int pos;
   
   private PanmirrorNavigation(String type, String location, int pos)
   {
      this.type = type;
      this.location = location;
      this.pos = pos;
   }
   
   private final static String kPanmirrorContext = "panmirror";
}
