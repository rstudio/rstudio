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

#include <iostream>

#include <boost/utility.hpp>
#include <boost/scoped_ptr.hpp>

#include <core/Error.hpp>

#import <Foundation/NSString.h>
#import <Foundation/NSAutoreleasePool.h>
#import <AppKit/NSSpellChecker.h>

namespace core {
namespace spelling {

namespace {

class AutoreleaseContext : boost::noncopyable
{
public:
   AutoreleaseContext()
   {
      pool_ = [[NSAutoreleasePool alloc] init];
   }

   ~AutoreleaseContext()
   {
      try
      {
         [pool_ release];
      }
      catch(...)
      {
      }
   }

private:
   NSAutoreleasePool* pool_;
};

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
      AutoreleaseContext arContext;


		return Success();
	}

public:
   Error checkSpelling(const std::string& word, bool *pCorrect)
   {
      AutoreleaseContext arContext;

      return Success();
   }

   Error suggestionList(const std::string& word, std::vector<std::string>* pSug)
   {
      AutoreleaseContext arContext;

      return Success();
   }

   Error analyzeWord(const std::string& word, std::vector<std::string>* pResult)
   {
      AutoreleaseContext arContext;

      return Success();
   }

   Error stemWord(const std::string& word, std::vector<std::string>* pResult)
   {
      AutoreleaseContext arContext;

      return Success();
   }

   Error addWord(const std::string& word, bool *pAdded)
   {
      AutoreleaseContext arContext;

      return Success();
   }

   Error removeWord(const std::string& word, bool *pRemoved)
   {
      AutoreleaseContext arContext;

      return Success();
   }

   Error addDictionary(const FilePath& dicPath,
                       const std::string& key,
                       bool *pAdded)
   {
      AutoreleaseContext arContext;

      return Success();
   }

private:


   NSString* toNSString(const std::string& str)
   {
      const char* cStr = str.c_str();
      return [NSString stringWithUTF8String: cStr];
   }

   std::string toStdString(NSString* nsString)
   {
      const char* cStr = [nsString UTF8String];
      return std::string(cStr);
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



