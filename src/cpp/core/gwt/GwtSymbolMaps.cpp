/*
 * GwtSymbolMaps.cpp
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

#include <core/gwt/GwtSymbolMaps.hpp>

#include <algorithm>

#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/algorithm/string/split.hpp>

#include <core/FileSerializer.hpp>

namespace core {
namespace gwt {

namespace {


ReadCollectionAction parseSymbolMapLine(
                          const std::string& line,
                          std::pair<const std::string,std::string>* pMapEntry)
{
   // ignore comments and empty lines
   if (line.empty() || (line[0] == '#'))
      return ReadCollectionIgnoreLine;

   int commaPos = line.find_first_of(',');
   if (commaPos == std::string::npos)
      return ReadCollectionIgnoreLine;

   // HACK: workaround the fact that std::map uses const for the Key
   std::string* pFirst = const_cast<std::string*>(&(pMapEntry->first)) ;
   *pFirst = line.substr(0, commaPos);

   pMapEntry->second = line.substr(commaPos+1);

   return ReadCollectionAddLine;
}

} // anonymous namespace

Error SymbolMaps::initialize(const FilePath& symbolMapsPath)
{
   symbolMapsPath_ = symbolMapsPath;
   return Success();
}

std::vector<StackElement> SymbolMaps::resymbolize(
                                  const std::vector<StackElement>& stack,
                                  const std::string& strongName)
{
   std::vector<StackElement> resymbolizedStack;
   BOOST_FOREACH(const StackElement& se, stack)
   {
      resymbolizedStack.push_back(resymbolize(se, strongName));
   }
   return resymbolizedStack;
}

StackElement SymbolMaps::resymbolize(const StackElement& se,
                                     const std::string& strongName)
{
   using namespace boost::algorithm;

   // values we will fill in
   std::string declaringClass, methodName, fileName;
   int lineNumber = -1, fragmentId = -1;
   fragmentId = -1;

   // get the symbol data
   const SymbolMap& symbolMap = mapForStrongName(strongName);
   SymbolMap::const_iterator it = symbolMap.find(se.methodName);
   std::string symbolData = (it != symbolMap.end()) ? it->second : "";

   // extract the details
   if (!symbolData.empty())
   {

      std::vector<std::string> parts;
      split(parts, symbolData, is_any_of(","));
      if (parts.size() == 6)
      {
         boost::smatch match;
         if (boost::regex_search(parts[0], match, jnsiRegex_))
         {
            declaringClass = match[1];
            methodName = match[2];
         }
         else
         {
            declaringClass = se.className;
            methodName = se.methodName;
         }
      }
   }

   if (!declaringClass.empty())
   {
      StackElement element;
      element.className = declaringClass;
      element.methodName = methodName;
      element.fileName = fileName;
      element.lineNumber = lineNumber;
      return element;
   }
   else // if anything goes wrong just return the original
   {
      return se;
   }
}

const SymbolMap& SymbolMaps::mapForStrongName(const std::string& strongName)
{
   // populate the cache if it isn't alrady
   if (symbolMapCache_.find(strongName) == symbolMapCache_.end())
   {
      // insert the map into the cache
      SymbolMap& map = symbolMapCache_[strongName];

      // read it from disk if it exists
      FilePath symbolMapPath = symbolMapsPath_.childPath(strongName +
                                                         ".symbolMap");
      if (symbolMapPath.exists())
      {
         Error error = core::readCollectionFromFile<SymbolMap>(
                                                 symbolMapPath,
                                                 &map,
                                                 parseSymbolMapLine);
         if (error)
            LOG_ERROR(error);
      }
   }

   return symbolMapCache_[strongName];
}


} // namespace gwt
} // namespace core


