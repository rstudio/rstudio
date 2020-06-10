/*
 * TexSynctex.cpp
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

#include <core/tex/TexSynctex.hpp>

#include <iostream>

#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/StringUtils.hpp>

#include <core/system/System.hpp>

#include "synctex/synctex_parser.h"

namespace rstudio {
namespace core {
namespace tex {

namespace {

PdfLocation pdfLocationFromNode(synctex_node_t node)
{
   int page = ::synctex_node_page(node);

   float x = ::synctex_node_box_visible_h(node);
   float y = ::synctex_node_box_visible_v(node) -
             ::synctex_node_box_visible_height(node);
   float w = ::synctex_node_box_visible_width(node);
   float h = ::synctex_node_box_visible_depth(node) +
             ::synctex_node_box_visible_height(node);

   return PdfLocation(page, x, y, w, h);
}

} // anonymous namespace

std::ostream& operator << (std::ostream& stream, const SourceLocation& loc)
{
   stream << loc.file() << " [" << loc.line() << ", " << loc.column() << "]";
   return stream;
}

std::ostream& operator << (std::ostream& stream, const PdfLocation& loc)
{
   stream << loc.page() << " {" <<
             loc.x() << ", " <<
             loc.y() << ", " <<
             loc.width() << ", " <<
             loc.height() << "}";

   return stream;
}

struct Synctex::Impl
{
   Impl() : scanner(nullptr) {}

   FilePath pdfPath;
   synctex_scanner_t scanner;
};


Synctex::Synctex()
  : pImpl_(new Impl())
{
}

Synctex::~Synctex()
{
   try
   {
      if (pImpl_->scanner != nullptr)
      {
         ::synctex_scanner_free(pImpl_->scanner);
         pImpl_->scanner = nullptr;
      }
   }
   catch(...)
   {

   }
}


bool Synctex::parse(const FilePath& pdfPath)
{
   using namespace rstudio::core::string_utils;
   pImpl_->pdfPath = pdfPath;
   std::string path = utf8ToSystem(pdfPath.getAbsolutePath());
   std::string buildDir = utf8ToSystem(pdfPath.getParent().getAbsolutePath());

   pImpl_->scanner = ::synctex_scanner_new_with_output_file(path.c_str(),
                                                            buildDir.c_str(),
                                                            1);
   return pImpl_->scanner != nullptr;
}

PdfLocation Synctex::forwardSearch(const SourceLocation& location)
{
   PdfLocation pdfLocation;

   // first determine the synctex local name for the input file
   std::string name = synctexNameForInputFile(location.file());
   if (name.empty())
      return PdfLocation();

   // run the query
   int result = ::synctex_display_query(pImpl_->scanner,
                                        name.c_str(),
                                        location.line(),
                                        location.column());
   if (result > 0)
   {
      synctex_node_t node = synctex_next_result(pImpl_->scanner);
      if (node != nullptr)
         pdfLocation = pdfLocationFromNode(node);
   }

   return pdfLocation;
}

SourceLocation Synctex::inverseSearch(const PdfLocation& location)
{
   SourceLocation sourceLocation;

   int result = ::synctex_edit_query(pImpl_->scanner,
                                     location.page(),
                                     location.x(),
                                     location.y());
   if (result > 0)
   {
      synctex_node_t node = synctex_next_result(pImpl_->scanner);
      if (node != nullptr)
      {
         // get the filename then normalize it
         std::string name = ::synctex_scanner_get_name(
                                                   pImpl_->scanner,
                                                   ::synctex_node_tag(node));
         std::string adjustedName = normalizeSynctexName(name);

         // might be relative or might be absolute, complete it against the
         // pdf's parent directory to cover both cases
         FilePath filePath = pImpl_->pdfPath.getParent().completePath(adjustedName);

         // fully normalize
         Error error = core::system::realPath(filePath, &filePath);
         if (error)
            LOG_ERROR(error);

         // return source location
         sourceLocation = SourceLocation(filePath,
                                         ::synctex_node_line(node),
                                         ::synctex_node_column(node));
      }
   }

   return sourceLocation;
}


PdfLocation Synctex::topOfPageContent(int page)
{
   // get the sheet contents
   synctex_node_t sheetNode = ::synctex_sheet_content(pImpl_->scanner, page);
   if (sheetNode == nullptr)
      return PdfLocation();

   // iterate through the nodes looking for a box
   synctex_node_t node = sheetNode;
   while((node = ::synctex_node_next(node)))
   {
      // look for the first hbox
      synctex_node_type_t nodeType = ::synctex_node_type(node);
      if (nodeType == synctex_node_type_hbox)
         return pdfLocationFromNode(node);
   }

   // couldn't find a box, just return the sheet
   return pdfLocationFromNode(sheetNode);
}

std::string Synctex::synctexNameForInputFile(const FilePath& inputFile)
{
   // get the base directory for the input file
   FilePath parentPath = inputFile.getParent();

   // iterate through the known input files looking for a match
   synctex_node_t node = ::synctex_scanner_input(pImpl_->scanner);
   while (node != nullptr)
   {
      // get tex name then normalize it for comparisons
      std::string name = ::synctex_scanner_get_name(pImpl_->scanner,
                                                    ::synctex_node_tag(node));
      std::string adjustedName = normalizeSynctexName(name);

      // complete the name against the parent path -- if it is equal to
      // the input file that that's the one we are looking for
      FilePath synctexPath = parentPath.completePath(adjustedName);
      if (synctexPath.isEquivalentTo(inputFile))
         return name;

      // next node
      node = ::synctex_node_sibling(node);
   }

   // not found
   return std::string();
}

std::string normalizeSynctexName(const std::string& name)
{
   // trim it (on windows if it's the last available name it can include
   // some additional whitespace at the end)
   std::string adjustedName = boost::algorithm::trim_copy(name);

   // if it starts with a ./ then trim that
   if (boost::algorithm::starts_with(name, "./") && (name.length() > 2))
      adjustedName = name.substr(2);

   return adjustedName;
}

} // namespace tex
} // namespace core 
} // namespace rstudio



