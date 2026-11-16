/*
 * UpdateCheckResult.java
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
package org.rstudio.studio.client.application.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;

/**
 * The outcome of an update check: either the session's check_for_updates RPC
 * or the desktop frame's in-app check, which uses the same keys plus "status"
 * and "can-install".
 */
public class UpdateCheckResult extends JavaScriptObject
{
   protected UpdateCheckResult() {}

   public static UpdateCheckResult fromJson(String json)
   {
      return JsonUtils.safeEval(json).cast();
   }

   /**
    * Only set by the desktop frame: "available", "up-to-date", "unsupported"
    * (no in-app update support, fall back to the session check) or "error".
    */
   public final native String getStatus() /*-{
      return (this['status'] || "").trim();
   }-*/;

   public final native String getError() /*-{
      return (this['error'] || "").trim();
   }-*/;

   /** Whether the desktop frame can download and install this update itself. */
   public final native boolean canInstall() /*-{
      return this['can-install'] === true;
   }-*/;

   /**
    * Set by the desktop frame for release builds: the builds site knows what
    * is published, but the session's check (rstudio.org) is where a release
    * is withdrawn or marked urgent, so that check must run too and have the
    * final say -- see {@link #reconcile}.
    */
   public final native boolean confirmWithSession() /*-{
      return this['confirm-with-session'] === true;
   }-*/;

   /**
    * The answer to act on when both the desktop frame and the session have
    * checked: the desktop's installable update, with the urgency the session
    * gives it, when both name the same version; otherwise the session's,
    * whether it reports no update (the release was withdrawn) or another
    * version (the builds site still lists a withdrawn release, or is ahead
    * of rstudio.org), which is then offered as a manual download.
    */
   public static UpdateCheckResult reconcile(UpdateCheckResult desktop,
                                             UpdateCheckResult session)
   {
      String version = desktop.getUpdateVersion();
      if (version.isEmpty() || !version.equals(session.getUpdateVersion()))
         return session;
      return desktop.withUrgency(session.getUpdateUrgency());
   }

   private final native UpdateCheckResult withUrgency(int urgency) /*-{
      var result = {};
      for (var key in this)
         result[key] = this[key];
      result['update-urgent'] = String(urgency);
      return result;
   }-*/;

   public final native String getUpdateMessage() /*-{
      return (this['update-message'] || "").trim();
   }-*/;

   public final native int getUpdateUrgency() /*-{
      return parseInt(this['update-urgent']);
   }-*/;
   
   public final native String getUpdateUrl() /*-{
      return (this['update-url'] || "").trim();
   }-*/;

   public final native String getUpdateVersion() /*-{
      return (this['update-version'] || "").trim();
   }-*/;
}
