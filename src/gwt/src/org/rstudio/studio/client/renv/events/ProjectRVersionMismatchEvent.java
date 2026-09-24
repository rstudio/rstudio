/*
 * ProjectRVersionMismatchEvent.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.renv.events;

import org.rstudio.studio.client.renv.model.RVersionInstall;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * Fired when the project asks for a version of R (via its renv lockfile or
 * project file) that differs from the one running.
 */
public class ProjectRVersionMismatchEvent extends GwtEvent<ProjectRVersionMismatchEvent.Handler>
{
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class Data
   {
      public String requested_version;
      public String current_version;

      // "lockfile" or "project"
      public String source;

      // whether this build of RStudio supports the requested version
      public boolean supported;

      // the matching installed R, or null when none was found
      public RVersionInstall installed;

      // whether RStudio can install the requested version here
      public boolean can_install;
   }

   public interface Handler extends EventHandler
   {
      void onProjectRVersionMismatch(ProjectRVersionMismatchEvent event);
   }

   public ProjectRVersionMismatchEvent(Data data)
   {
      data_ = data;
   }

   public Data getData()
   {
      return data_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onProjectRVersionMismatch(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<>();
}
