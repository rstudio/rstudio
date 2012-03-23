/*
 * MacSpellChecker.mm
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

#include <core/spelling/SpellChecker.hpp>

#include <boost/scoped_ptr.hpp>

#include <core/Error.hpp>

#import <Foundation/NSString.h>
#import <AppKit/NSSpellChecker.h>

namespace core {
namespace spelling {

namespace {

class MacSpellChecker : public SpellChecker
{
public:
   MacSpellChecker()
   {
   }

   virtual ~MacSpellChecker()
   {
      try
      {
      }
      catch(...)
      {
      }
   }

	Error initialize()
	{
      if([NSSpellChecker sharedSpellCheckerExists])
      {
         // do nothing...we are just executing this to make sure we
         // are linking to AppKit correctly
      }

		return Success();
	}

public:
   Error checkSpelling(const std::string& word, bool *pCorrect)
   {
      return Success();
   }

   Error suggestionList(const std::string& word, std::vector<std::string>* pSug)
   {
      return Success();
   }

   Error analyzeWord(const std::string& word, std::vector<std::string>* pResult)
   {
      return Success();
   }

   Error stemWord(const std::string& word, std::vector<std::string>* pResult)
   {
      return Success();
   }

   Error addWord(const std::string& word, bool *pAdded)
   {
      return Success();
   }

   Error removeWord(const std::string& word, bool *pRemoved)
   {
      return Success();
   }

   Error addDictionary(const FilePath& dicPath,
                       const std::string& key,
                       bool *pAdded)
   {
      return Success();
   }

private:
   NSString* toNSString(const std::string& str)
   {
      const char* cStr = str.c_str();
      NSString* nsStr = [[NSString alloc] initWithUTF8String: cStr];
      return nsStr;
   }

};

} // anonymous namespace

core::Error createMac(boost::shared_ptr<SpellChecker>* ppMac)
{
   // create the mac engine
   boost::shared_ptr<MacSpellChecker> pNew(new MacSpellChecker());

   // initialize it
   Error error = pNew->initialize();
   if (error)
      return error;

   // return
   *ppMac = boost::shared_static_cast<SpellChecker>(pNew);
   return Success();
}


} // namespace spelling
} // namespace core 



