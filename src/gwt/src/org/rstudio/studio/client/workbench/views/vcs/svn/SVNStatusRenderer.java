/*
 * SVNStatusRenderer.java
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
package org.rstudio.studio.client.workbench.views.vcs.svn;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable.ChangelistTableCellTableResources;

public class SVNStatusRenderer implements SafeHtmlRenderer<String>
{
   interface StatusResources extends ClientBundle
   {
      @Source("images/statusAdded.png")
      ImageResource statusAdded();
      @Source("images/statusConflicted.png")
      ImageResource statusConflicted();
      @Source("images/statusDeleted.png")
      ImageResource statusDeleted();
      @Source("images/statusExternal.png")
      ImageResource statusExternal();
      @Source("images/statusIgnored.png")
      ImageResource statusIgnored();
      @Source("images/statusMissing.png")
      ImageResource statusMissing();
//      @Source("images/statusMerged")
//      ImageResource statusMerged();
      @Source("images/statusModified.png")
      ImageResource statusModified();
      @Source("images/statusNone.png")
      ImageResource statusNone();
      @Source("images/statusObstructed.png")
      ImageResource statusObstructed();
      @Source("images/statusUnversioned.png")
      ImageResource statusUnversioned();
   }

   public SVNStatusRenderer()
   {
   }

   @Override
   public SafeHtml render(String str)
   {
      if (str.length() != 1)
         return SafeHtmlUtils.fromString(str);

      ImageResource img = imgForStatus(str.charAt(0));

      if (img == null)
         return SafeHtmlUtils.fromString(str);

      SafeHtmlBuilder builder = new SafeHtmlBuilder();
      builder.append(SafeHtmlUtils.fromTrustedString(
            "<span " +
            "class=\"" + ctRes_.cellTableStyle().status() + "\" " +
            "title=\"" +
            SafeHtmlUtils.htmlEscape(descForStatus(str)) +
            "\">"));

      builder.append(SafeHtmlUtils.fromTrustedString(AbstractImagePrototype.create(
            img).getHTML()));

      builder.appendHtmlConstant("</span>");

      return builder.toSafeHtml();
   }

   private String descForStatus(String str)
   {
      char c = str.charAt(0);
      
      switch (c)
      {
         case 'A':
            return "Added";
         case 'C':
            return "Conflicted";
         case 'D':
            return "Deleted";
         case 'X':
            return "External";
         case 'I':
            return "Ignored";
         case '!':
            return "Missing";
//         case 'G':
//            return resources_.statusMerged();
         case 'M':
            return "Modified";
         case ' ':
            return "";
         case '~':
            return "Obstructed";
         case '?':
            return "Unversioned";
         default:
            return "";
      }
      
   }

  
   
   private ImageResource imgForStatus(char c)
   {
      switch (c)
      {
         case 'A':
            return resources_.statusAdded();
         case 'C':
            return resources_.statusConflicted();
         case 'D':
            return resources_.statusDeleted();
         case 'X':
            return resources_.statusExternal();
         case 'I':
            return resources_.statusIgnored();
         case '!':
            return resources_.statusMissing();
//         case 'G':
//            return resources_.statusMerged();
         case 'M':
            return resources_.statusModified();
         case ' ':
            return resources_.statusNone();
         case '~':
            return resources_.statusObstructed();
         case '?':
            return resources_.statusUnversioned();
         default:
            return null;
      }
   }

   @Override
   public void render(String str, SafeHtmlBuilder builder)
   {
      SafeHtml safeHtml = render(str);
      if (safeHtml != null)
         builder.append(safeHtml);
   }

   private static final StatusResources resources_ = GWT.create(StatusResources.class);
   private static final ChangelistTableCellTableResources ctRes_ = GWT.create(ChangelistTableCellTableResources.class);
}
