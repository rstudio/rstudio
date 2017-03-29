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

#include <string>
#include <map>
#include <set>
#include <algorithm>

#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/regex.hpp>
#include <boost/algorithm/string/split.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Thread.hpp>
#include <core/RegexUtils.hpp>
#include <core/SafeConvert.hpp>
#include <core/FileSerializer.hpp>

// NOTE: this is a port of the following GWT class:
// (our rev was 11565, we should track to future changes)

// https://code.google.com/p/google-web-toolkit/source/browse/trunk/user/src/com/google/gwt/core/server/impl/StackTraceDeobfuscator.java?r=11565

namespace rstudio {
namespace core {
namespace gwt {

namespace {

const char * const SYMBOL_DATA_UNKNOWN = "";
const int LINE_NUMBER_UNKNOWN = -1;

ReadCollectionAction parseSymbolMapLine(
                          const std::string& line,
                          std::pair<const std::string,std::string>* pMapEntry,
                          std::set<std::string>* pSymbolsLeftToFind)
{
   // bail if we have no more symbols left to find
   if (pSymbolsLeftToFind->empty())
      return ReadCollectionTerminate;

   // ignore comments and empty lines
   if (line.empty() || (line[0] == '#'))
      return ReadCollectionIgnoreLine;

   size_t commaPos = line.find_first_of(',');
   if (commaPos == std::string::npos)
      return ReadCollectionIgnoreLine;

   // HACK: workaround the fact that std::map uses const for the Key
   std::string* pFirst = const_cast<std::string*>(&(pMapEntry->first)) ;
   *pFirst = line.substr(0, commaPos);

   pMapEntry->second = line.substr(commaPos+1);

   pSymbolsLeftToFind->erase(*pFirst);

   return ReadCollectionAddLine;
}

class SymbolCache : boost::noncopyable
{
public:
   void putAll(const std::string& strongName,
               const std::map<std::string,std::string>& symbolMap)
   {
      if (strongName.empty() || symbolMap.empty())
         return;

      LOCK_MUTEX(mutex_)
      {
         cache_[strongName].insert(symbolMap.begin(), symbolMap.end());
      }
      END_LOCK_MUTEX
   }

   std::map<std::string,std::string> getAll(
                                 const std::string& strongName,
                                 const std::set<std::string>& symbols)
   {
      std::map<std::string,std::string> toReturn;

      LOCK_MUTEX(mutex_)
      {
         if (strongName.empty() ||
             (cache_.find(strongName) == cache_.end()) ||
             symbols.empty())
         {
            return toReturn;
         }

          std::map<std::string,std::string>& map = cache_[strongName];
          BOOST_FOREACH(const std::string& symbol, symbols)
          {
             std::map<std::string,std::string>::const_iterator it =
                                                         map.find(symbol);
             if (it != map.end())
                toReturn.insert(*it);
          }
      }
      END_LOCK_MUTEX

      return toReturn;
   }

private:
   boost::mutex mutex_;
   std::map<std::string, std::map<std::string,std::string> > cache_;
};

} // anonymous namespace


struct SymbolMaps::Impl
{
   FilePath symbolMapsPath;
   SymbolCache symbolCache;

   std::string loadOneSymbol(const std::string& strongName,
                             const std::string& symbol)
   {
      std::set<std::string> symbolSet;
      symbolSet.insert(symbol);
      std::map<std::string,std::string> map = loadSymbolMap(strongName,
                                                            symbolSet);

      return map[symbol];
   }

   std::map<std::string,std::string> loadSymbolMap(
                           const std::string& strongName,
                           const std::set<std::string>& requiredSymbols)
   {
      // cache lookup first
      std::map<std::string,std::string> toReturn = symbolCache.getAll(
                                                               strongName,
                                                               requiredSymbols);

      // did that satisfy the request fully?
      if (toReturn.size() == requiredSymbols.size())
         return toReturn;

      // lookup additional symbols by reading the file
      std::set<std::string> symbolsLeftToFind = requiredSymbols;

      // read it from disk if it exists
      FilePath mapPath = symbolMapsPath.childPath(strongName + ".symbolMap");
      if (mapPath.exists())
      {
         Error error = readCollectionFromFile
                                    <std::map<std::string,std::string> >(
                 mapPath,
                 &toReturn,
                 boost::bind(parseSymbolMapLine, _1, _2, &symbolsLeftToFind));
         if (error)
            LOG_ERROR(error);

      }

      // mark all remaining symbols as having been looked for
      BOOST_FOREACH(const std::string& symbol, symbolsLeftToFind)
      {
         toReturn[symbol] = SYMBOL_DATA_UNKNOWN;
      }

      // add the return results to the cache
      symbolCache.putAll(strongName, toReturn);

      // return the results
      return toReturn;
   }
};


SymbolMaps::SymbolMaps()
   : pImpl_(new Impl())
{
}

SymbolMaps::~SymbolMaps()
{
   try
   {
   }
   catch(...)
   {
   }
}

Error SymbolMaps::initialize(const FilePath& symbolMapsPath)
{
   pImpl_->symbolMapsPath = symbolMapsPath;
   return Success();
}

std::vector<StackElement> SymbolMaps::resymbolize(
                                  const std::vector<StackElement>& stack,
                                  const std::string& strongName)
{
   // warm the symbol cache
   std::set<std::string> requiredSymbols;
   BOOST_FOREACH(const StackElement& stackElement, stack)
   {
      requiredSymbols.insert(stackElement.methodName);
   }
   pImpl_->loadSymbolMap(strongName, requiredSymbols);

   // perform the resymbolization
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

   std::string steFilename = se.fileName;
   std::string symbolData = pImpl_->loadOneSymbol(strongName, se.methodName);

   // detect whether we are source map capable and extract the column if we are
   // (note: were not currently using source maps but we still ported this
   // code from the GWT Java implementation in case we want to add the
   // sourcemap support later)
   bool sourceMapCapable = false;
   int column = 1;
   if (!steFilename.empty())
   {
      // column information is encoded after '@' for sourceMap capable browsers
      size_t columnMarkerPos = steFilename.find_first_of('@');
      if (columnMarkerPos != std::string::npos)
      {
         try
         {
            std::string colStr = steFilename.substr(columnMarkerPos+1);
            column = boost::lexical_cast<int>(colStr);
            sourceMapCapable = true;
         }
         catch(boost::bad_lexical_cast&)
         {
         }
         steFilename = steFilename.substr(0, columnMarkerPos);
      }
   }

   // extract the details
   if (!symbolData.empty())
   {
      std::vector<std::string> parts;
      split(parts, symbolData, is_any_of(","));
      if (parts.size() == 6)
      {
         boost::regex re("@?([^:]+)::([^(]+)(\\((.*)\\))?");
         boost::smatch match;
         if (regex_utils::search(parts[0], match, re))
         {
            declaringClass = match[1];
            methodName = match[2];
         }
         else
         {
            declaringClass = se.className;
            methodName = se.methodName;
         }

         // parts[3] contains the source file URI or "Unknown"
         fileName = "";
         if (parts[3] != "Unknown")
         {
            fileName = parts[3];

            // normalize filenames that are generated by custom linkers
            size_t pos = fileName.find("file:");
            if (pos == 0)
            {
               std::string genPrefix("src/gwt/gen/");
               pos = fileName.find(genPrefix);
               if (pos != std::string::npos)
                  fileName = fileName.substr(pos + genPrefix.length());
            }
         }

         lineNumber = se.lineNumber;

         // When lineNumber is LINE_NUMBER_UNKNOWN, either because
         // compiler.stackMode is not emulated or
         // compiler.emulatedStack.recordLineNumbers is false, use the method
         // declaration line number from the symbol map.
         if (lineNumber == LINE_NUMBER_UNKNOWN ||
             (sourceMapCapable && column == -1))
         {
            // Safari will send line numbers, with col == -1, we need to
            // use symbolMap in this case
            lineNumber = safe_convert::stringTo<int>(parts[4], lineNumber);
         }

         fragmentId = safe_convert::stringTo<int>(parts[5], fragmentId);
      }
   }

   // anonymous function, try to use <fragmentNum>.js:line to determine
   // fragment id
   if (fragmentId == -1 && !steFilename.empty())
   {
      // fragment identifier encoded in filename
      boost::regex re(".*(\\d+)\\.js");
      boost::smatch match;
      if (regex_utils::search(steFilename, match, re))
      {
         fragmentId = safe_convert::stringTo<int>(match[1], fragmentId);
      }
      else if (boost::algorithm::contains(steFilename, strongName))
      {
         fragmentId = 0;
      }
   }

   // try to refine location with sourceMap
   // int jsLineNumber = se.lineNumber;

   // NOTE: not yet implemented -- the upside of this appears to be that
   // we could get the precise line location of the exception

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

   return se;
}

} // namespace gwt
} // namespace core
} // namespace rstudio


