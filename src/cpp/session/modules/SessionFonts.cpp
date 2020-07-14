/*
 * SessionFonts.cpp
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

#include "SessionFonts.hpp"

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/Exec.hpp>
#include <core/text/TemplateFilter.hpp>

#include <core/system/Xdg.hpp>

#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>

#include <session/SessionModuleContext.hpp>

#include <array>

using namespace rstudio::core;

#define kFontFolder    "fonts"
#define kFontsLocation "fonts/"
#define kFontFiles     kFontsLocation "files/"
#define kFontCss       kFontsLocation "css/"

namespace rstudio {
namespace session {
namespace modules {
namespace fonts {

namespace {

// File extensions that we recognize as font files
std::array<std::string, 5> s_fontExtensions = {
   ".eot",
   ".ttf",
   ".otf",
   ".woff",
   ".woff2"
};

// Indicates whether the file is a font file (i.e. has a known font file extension)
bool isFontFile(const FilePath& file)
{
   // Test the file extension against known font formats
   std::string ext = file.getExtensionLowerCase();
   for (const auto& fontExt: s_fontExtensions)
   {
      if (ext == fontExt)
         return true;
   }

   // No extensions match
   return false;
}

// The path to the user-specific font folder
core::FilePath userFontFolder()
{
   return core::system::xdg::userConfigDir().completeChildPath(kFontFolder);
}

// The path to the system-wide font folder
core::FilePath systemFontFolder()
{
   return core::system::xdg::systemConfigFile(kFontFolder);
}

// Generates a CSS snippet from a font file.
Error generateCssFromFile(const FilePath& file,
                          const std::string& fontName,
                          const std::vector<std::string>& parents,
                          std::string *pCss)
{
   pCss->append("@font-face {\n"
                "  font-family: \"" + fontName + "\";\n"
                "  src: url('../files/");

   // Build URL to font file from parent folders. 
   for (const auto& parent: parents)
   {
      pCss->append(parent + "/");
   }
   pCss->append(file.getFilename() + "');\n");

   // Convert each parent folder
   for (const auto& parent: parents)
   {
      // Check to see whether this directory looks like a font weight
      // font style.
      auto weight = safe_convert::stringTo<int>(parent);
      if (weight)
      {
         // Looks like a number, so generate a weight rule and recurse.
         pCss->append("  font-weight: " + parent + ";\n");
      }
      else if (parent == "oblique" || parent == "italic")
      {
         // It's a style
         pCss->append("  font-style: " + parent + ";\n");
      }
   }

   pCss->append("}\n\n");
   return Success();
}

// Generates CSS from a directory representing a number of variants of a single font face. The
// directory structure is used to provide font metadata. For instance, a font with regular (400) and
// bold (700) weights might be stored as follows:
//
// + Victor Mono/
// |
// +--+ 400/
// |  |
// |  +-- Victor Mono Regular.woff
// |  
// +--+ 700/
//    |
//    +-- Victor Mono Bold.woff
//
Error generateCssFromDir(const FilePath& dir,
                         const std::string& fontName,
                         const std::vector<std::string>& parents,
                         std::string *pCss)
{
   std::vector<FilePath> files;
   Error error = dir.getChildren(files);
   if (error)
   {
      return error;
   }
   
   for (const auto& file: files)
   {
      std::string name = file.getFilename();
      if (file.isDirectory())
      {
         // This is a directory, probably representing a font weight or style
         std::vector<std::string> newParents(parents);
         newParents.push_back(name);
         error = generateCssFromDir(file, fontName, newParents, pCss);
      }
      else if (isFontFile(file))
      {
         // This is an ordinary font file
         error = generateCssFromFile(file, fontName, parents, pCss);
      }

      if (error)
      {
         // Log and clear any errors encountered while processing this font
         LOG_ERROR(error);
         error = Success();
      }
   }

   return error;
}

// Handles an HTTP request for a specific font file.
void handleFontFileRequest(const http::Request& request,
                           http::Response* pResponse)
{
   std::string prefix = "/" kFontFiles;
   std::string fileName = http::util::pathAfterPrefix(request, prefix);
   
   // Check user font folder first
   FilePath fontFile = userFontFolder().completeChildPath(fileName);
   if (!fontFile.exists())
   {
      // Not found in user fonts; fall back to system
      fontFile = systemFontFolder().completeChildPath(fileName);
   }

   // Allow the browser to cache these (implies that a reload may be required when replacing font
   // files with new files that have the same name)
   pResponse->setIndefiniteCacheableFile(fontFile, request);
   return;
}

// Handles an HTTP request for font CSS. This will typically look something like:
//
// GET /fonts/css/Victor Mono.css
//
// The request is fulfilled by automatically generating the appropriate CSS @font-face rule(s) for
// the font and returning them in the body of the request.
void handleFontCssRequest(const http::Request& request,
                          http::Response* pResponse)
{
   Error error;
   std::string css;
   std::string prefix = "/"  kFontCss;
   std::string fileName = http::util::pathAfterPrefix(request, prefix);

   // Strip off ".css" to get name of font
   size_t idx = fileName.find(".css");
   if (idx != std::string::npos)
   {
      fileName = fileName.substr(0, idx);
   }
   
   // Enumerate user and system font directories
   std::vector<FilePath> dirs;
   dirs.push_back(userFontFolder());
   dirs.push_back(systemFontFolder());

   for (const auto& dir: dirs)
   {
      FilePath subDir = dir.completeChildPath(fileName);
      if (subDir.exists() && subDir.isDirectory())
      {
         // Folder full of font files
         std::vector<std::string> parents;
         parents.push_back(fileName);
         error = generateCssFromDir(subDir, fileName, parents, &css);
         if (error)
         {
            LOG_ERROR(error);
         }
      }
      else
      {
         // An individual font file; determine extension
         for (const auto& fontExt: s_fontExtensions)
         {
            FilePath fontFile = dir.completeChildPath(fileName + fontExt);
            if (fontFile.exists())
            {
               // Generate CSS for the font file
               error = generateCssFromFile(fontFile, fileName, std::vector<std::string>(), &css);
               if (error)
               {
                  LOG_ERROR(error);
               }

               // We found a matching font; bail here
               break;
            }
         }
      }
   }

   // It's okay if there was no matching font found at this point, since on RStudio Server the font
   // can be provided by the browser instead of the server. We will still generate a font-specific
   // stylesheet below.
   //
   // Append override rules for basic fixed-width elements. This stylesheet overrides a few key
   // styles in themeStyles.css with the specified font.
   //
   std::map<std::string,std::string> vars;
   vars["font"] = fileName;
   std::ostringstream oss;
   error = core::text::renderTemplate(
      session::options().rResourcesPath().completeChildPath("themes/css/fonts.css"), vars, oss);
   if (error)
   {
      LOG_ERROR(error);
   }
   else
   {
      css.append(oss.str());
   }

   // Return the accumulated stylesheet
   pResponse->setContentType("text/css");
   pResponse->setBody(css);
}

Error getInstalledFonts(const json::JsonRpcRequest& request, json::JsonRpcResponse* pResponse)
{
   Error error;

   // The array of installed fonts we'll build
   json::Array fonts;

   // Search user and system folders for installed fonts
   std::vector<FilePath> dirs;
   dirs.push_back(userFontFolder());
   dirs.push_back(systemFontFolder());

   for (const auto& dir: dirs)
   {
      if (!dir.exists())
         continue;

      std::vector<FilePath> files;
      error = dir.getChildren(files);
      if (error)
      {
         // These directories should be readable by everyone
         return error;
      }

      for (const auto& file: files)
      {
         // Fonts can be stored in ordinary files or in a directory with the name of the font
         if (isFontFile(file) || file.isDirectory())
         {
            fonts.push_back(file.getStem());
         }
      }
   }

   pResponse->setResult(fonts);

   return Success();
}

} // anonymous namespace

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_installed_fonts", getInstalledFonts))
      (bind(registerUriHandler, "/" kFontFiles, handleFontFileRequest))
      (bind(registerUriHandler, "/" kFontCss,   handleFontCssRequest));
   return initBlock.execute();
}

} // namespace fonts
} // namespace modules
} // namespace session
} // namespace rstudio
