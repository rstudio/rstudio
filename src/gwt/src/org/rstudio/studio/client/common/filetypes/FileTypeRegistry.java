/*
 * FileTypeRegistry.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.filetypes;

import com.google.gwt.resources.client.ImageResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.reditor.EditorLanguage;

import java.util.HashMap;

@Singleton
public class FileTypeRegistry
{
   private static final FileIconResources ICONS = FileIconResources.INSTANCE;

   public static final TextFileType TEXT =
         new TextFileType("text", "Text File", EditorLanguage.LANG_PLAIN, "",
                          ICONS.iconText(),
                          true,
                          false, false, false, false);

   public static final TextFileType R =
         new TextFileType("r_source", "R Script", EditorLanguage.LANG_R, ".R",
                          ICONS.iconRdoc(),
                          false,
                          true, true, true, false);

   public static final TextFileType RD =
      new TextFileType("r_doc", "R Documentation", EditorLanguage.LANG_TEX, ".Rd",
                       ICONS.iconTex(),
                       true,
                       false, false, false, false);

   public static final TextFileType SWEAVE =
         new TextFileType("sweave", "Sweave Document", EditorLanguage.LANG_SWEAVE, ".Rnw",
                          ICONS.iconTex(),
                          true,
                          false, true, true, true);

   public static final TextFileType TEX =
         new TextFileType("tex", "TeX Document", EditorLanguage.LANG_TEX, ".tex",
                          ICONS.iconTex(),
                          true,
                          false, false, false, true);

   public static final RDataType RDATA = new RDataType();

   public static final DataFrameType DATAFRAME = new DataFrameType();
   public static final UrlContentType URLCONTENT = new UrlContentType();

   public static final BrowserType BROWSER = new BrowserType();
   
   @Inject
   public FileTypeRegistry(EventBus eventBus)
   {
      eventBus_ = eventBus;

      FileIconResources icons = ICONS;

      register("", TEXT, icons.iconText());
      register("*.txt", TEXT, icons.iconText());
      register("*.log", TEXT, icons.iconText());
      register("README", TEXT, icons.iconText());
      register("*.r", R, icons.iconRdoc());
      register(".rprofile", R, icons.iconRprofile());
      register(".rhistory", R, icons.iconRhistory());
      register("*.rnw", SWEAVE, icons.iconTex());
      register("*.snw", SWEAVE, icons.iconTex());
      register("*.nw", SWEAVE, icons.iconText());
      register("*.tex", TEX, icons.iconTex());
      register("*.latex", TEX, icons.iconTex());
      register("*.rd", RD, icons.iconTex());
      register("*.rdata", RDATA, icons.iconRdata());
      defaultType_ = BROWSER;

      registerIcon(".jpg", icons.iconPng());
      registerIcon(".jpeg", icons.iconPng());
      registerIcon(".gif", icons.iconPng());
      registerIcon(".bmp", icons.iconPng());
      registerIcon(".tiff", icons.iconPng());
      registerIcon(".tif", icons.iconPng());
      registerIcon(".png", icons.iconPng());

      registerIcon(".pdf", icons.iconPdf());
      registerIcon(".csv", icons.iconCsv());

      for (FileType fileType : FileType.ALL_FILE_TYPES)
      {
         assert !fileTypesByTypeName_.containsKey(fileType.getTypeId());
         fileTypesByTypeName_.put(fileType.getTypeId(), fileType);
      }
   }

   public void openFile(FileSystemItem file)
   {
      FileType fileType = getTypeForFile(file);
      if (fileType != null)
         fileType.openFile(file, eventBus_);
   }

   public void editFile(FileSystemItem file)
   {
      FileType fileType = getTypeForFile(file);
      if (!(fileType instanceof TextFileType))
         fileType = TEXT;

      if (fileType != null)
         fileType.openFile(file, eventBus_);
   }

   public FileType getTypeByTypeName(String name)
   {
      return fileTypesByTypeName_.get(name);
   }

   public FileType getTypeForFile(FileSystemItem file)
   {
      if (file != null)
      {
         String filename = file.getName().toLowerCase();
         FileType result = fileTypesByFilename_.get(filename);
         if (result != null)
            return result;

         String extension = FileSystemItem.getExtensionFromPath(filename);
         result = fileTypesByFileExtension_.get(extension);
         if (result != null)
            return result;
      }
      return defaultType_;
   }

   public TextFileType getTextTypeForFile(FileSystemItem file)
   {
      FileType type = getTypeForFile(file);
      if (type instanceof TextFileType)
         return (TextFileType) type;
      else
         return TEXT;
   }

   public ImageResource getIconForFile(FileSystemItem file)
   {
      if (file.isDirectory())
      {
         if (file.isPublicFolder())
            return ICONS.iconPublicFolder();
         else
            return ICONS.iconFolder();
      }

      ImageResource icon = iconsByFilename_.get(file.getName().toLowerCase());
      if (icon != null)
         return icon;
      icon = iconsByFileExtension_.get(file.getExtension().toLowerCase());
      if (icon != null)
         return icon;

      return ICONS.iconText();
   }

   private void register(String filespec, FileType fileType, ImageResource icon)
   {
      if (filespec.startsWith("*."))
      {
         String ext = filespec.substring(1).toLowerCase();
         if (ext.equals("."))
            ext = "";
         fileTypesByFileExtension_.put(ext,
                                       fileType);
         if (icon != null)
            iconsByFileExtension_.put(ext, icon);
      }
      else if (filespec.length() == 0)
      {
         fileTypesByFileExtension_.put("", fileType);
         if (icon != null)
            iconsByFileExtension_.put("", icon);
      }
      else
      {
         assert filespec.indexOf("*") < 0 : "Unexpected filespec format";
         fileTypesByFilename_.put(filespec.toLowerCase(), fileType);
         if (icon != null)
            iconsByFileExtension_.put(filespec.toLowerCase(), icon);
      }
   }

   private void registerIcon(String extension, ImageResource icon)
   {
      iconsByFileExtension_.put(extension, icon);
   }

   private final HashMap<String, FileType> fileTypesByFileExtension_ =
         new HashMap<String, FileType>();
   private final HashMap<String, FileType> fileTypesByFilename_ =
         new HashMap<String, FileType>();
   private final HashMap<String, FileType> fileTypesByTypeName_ =
         new HashMap<String, FileType>();
   private final HashMap<String, ImageResource> iconsByFileExtension_ =
         new HashMap<String, ImageResource>();
   private final HashMap<String, ImageResource> iconsByFilename_ =
         new HashMap<String, ImageResource>();
   private final FileType defaultType_;
   private final EventBus eventBus_;
}
