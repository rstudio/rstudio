/*
 * GwtSymbolMaps.hpp
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

#ifndef CORE_GWT_SYMBOL_MAPS_HPP
#define CORE_GWT_SYMBOL_MAPS_HPP

#include <string>

#include <boost/utility.hpp>
#include <boost/scoped_ptr.hpp>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {

class Error;
class FilePath;

namespace gwt {

struct StackElement
{
   StackElement() : lineNumber(0) {}
   std::string fileName;
   std::string className;
   std::string methodName;
   int lineNumber;
};

class SymbolMaps : boost::noncopyable
{
public:
   SymbolMaps();
   virtual ~SymbolMaps();

   Error initialize(const FilePath& symbolMapsPath);

   std::vector<StackElement> resymbolize(const std::vector<StackElement>& stack,
                                         const std::string& strongName);

   StackElement resymbolize(const StackElement& se,
                            const std::string& strongName);

private:
   struct Impl;
   boost::scoped_ptr<Impl> pImpl_;
};

} // namespace gwt
} // namespace core
} // namespace rstudio

#endif // CORE_GWT_SYMBOL_MAPS_HPP
