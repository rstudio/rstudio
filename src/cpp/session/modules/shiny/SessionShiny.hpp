/*
 * SessionShiny.hpp
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

#ifndef SESSION_SHINY_HPP
#define SESSION_SHINY_HPP

#include <core/json/Json.hpp>

namespace rstudio {
namespace core {
   class Error;
}
}
 
namespace rstudio {
namespace session {
namespace modules { 
namespace shiny {

enum ShinyFileType 
{
   // Not a Shiny file
   ShinyNone,

   // A file in a Shiny directory, such as ui.R, server.R, global.R, etc.
   ShinyDirectory,

   // A standalone Shiny file with any other name, ending with a call to
   // shinyApp()
   ShinySingleFile,

   // A standalone Shiny file that can be executed directly, ending with a 
   // call to runApp()
   ShinySingleExecutable,

   // A Shiny R Markdown document 
   ShinyDocument
};

bool isShinyRMarkdownDocument(const core::FilePath& filePath);

ShinyFileType getShinyFileType(const core::FilePath& filePath);
ShinyFileType getShinyFileType(const core::FilePath& filePath,
                               const std::string& contents);
ShinyFileType shinyTypeFromExtendedType(const std::string& extendedType);

core::Error initialize();
                       
} // namespace shiny
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_SHINY_HPP
