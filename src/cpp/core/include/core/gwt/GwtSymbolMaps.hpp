/*
 * GwtSymbolMaps.hpp
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

#ifndef CORE_GWT_SYMBOL_MAPS_HPP
#define CORE_GWT_SYMBOL_MAPS_HPP

#include <string>
#include <map>

#include <boost/utility.hpp>
#include <boost/regex.hpp>

#include <core/FilePath.hpp>

namespace core {

class Error;

namespace gwt {

struct StackElement
{
   StackElement() : lineNumber(0) {}
   std::string fileName;
   std::string className;
   std::string methodName;
   int lineNumber;
};

typedef std::map<std::string,std::string> SymbolMap;

class SymbolMaps : boost::noncopyable
{
public:
   SymbolMaps() :
      jnsiRegex_("@?([^:]+)::([^(]+)(\\((.*)\\))?")
   {
   }

   Error initialize(const FilePath& symbolMapsPath);

   std::vector<StackElement> resymbolize(const std::vector<StackElement>& stack,
                                         const std::string& strongName);

   StackElement resymbolize(const StackElement& se,
                            const std::string& strongName);


private:
   const SymbolMap& mapForStrongName(const std::string& strongName);

private:
   FilePath symbolMapsPath_;
   typedef std::map<std::string,SymbolMap> SymbolMapCache;
   SymbolMapCache symbolMapCache_;
   boost::regex jnsiRegex_;
};

} // namespace gwt
} // namespace core

#endif // CORE_GWT_SYMBOL_MAPS_HPP
