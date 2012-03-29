/*
 * MacSpellingEngine.mm
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

#include <core/spelling/MacSpellingEngine.hpp>

#include <iostream>

#include <boost/utility.hpp>
#include <boost/scoped_ptr.hpp>

#include <core/Error.hpp>

#import <Foundation/NSString.h>
#import <Foundation/NSArray.h>
#import <Foundation/NSAutoreleasePool.h>
#import <Foundation/NSException.h>
#import <AppKit/NSSpellChecker.h>

// convenience macro to catch objective-c exceptions and convert them
// into standard Error returns
#define CATCH_NS_EXCEPTION \
   @catch (NSException* ex) \
   { \
      std::string name = [[ex name] UTF8String]; \
      std::string reason = [[ex reason] UTF8String]; \
      return systemError(boost::system::errc::state_not_recoverable, \
                         name + " (" + reason + ")", \
                         ERROR_LOCATION); \
   }

namespace core {
namespace spelling {

namespace {

// utility class to allocate and drain an autorelease pool within a
// scope. this allows the caller to not have a global / event-loop based
// autorelease handler installed to successfully call the spelling api
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

} // anonymous namespace

struct MacSpellingEngine::Impl
{
};

MacSpellingEngine::MacSpellingEngine()
   : pImpl_(new Impl())
{
}

Error MacSpellingEngine::initialize()
{
   AutoreleaseContext arContext;

   // ensure the spell checker is available (and not damaged in some way)
   // by checking for its language within an exception block
   @try
   {
      [[NSSpellChecker sharedSpellChecker] language];
      return Success();
   }
   CATCH_NS_EXCEPTION

   // keep compiler happy
   return Success();
}

Error MacSpellingEngine::checkSpelling(const std::string& langId,
                                       const std::string& word,
                                       bool *pCorrect)
{
   AutoreleaseContext arContext;

   @try
   {
      NSSpellChecker* spellChecker = [NSSpellChecker sharedSpellChecker];
      NSString* nsWord = toNSString(word);
      NSArray* results = [spellChecker checkString: nsWord
                                       range: NSMakeRange(0, [nsWord length])
                                       types: NSTextCheckingTypeSpelling
                                       options: nil
                                       inSpellDocumentWithTag: 0
                                       orthography: NULL
                                       wordCount: NULL];

      *pCorrect = [results count] == 0;
      return Success();
   }
   CATCH_NS_EXCEPTION

   // keep compiler happy
   return Success();
}

Error MacSpellingEngine::suggestionList(const std::string& langId,
                                        const std::string& word,
                                        std::vector<std::string>* pSug)
{
   AutoreleaseContext arContext;

   @try
   {
      NSSpellChecker* spellChecker = [NSSpellChecker sharedSpellChecker];
      NSString* nsWord = toNSString(word);
      NSRange wordRange = NSMakeRange(0, [nsWord length]);
      NSArray* results = [spellChecker guessesForWordRange: wordRange
                                       inString: nsWord
                                       language: [spellChecker language]
                                       inSpellDocumentWithTag: 0];

      for (NSString* result in results)
         pSug->push_back(toStdString(result));

      return Success();
   }
   CATCH_NS_EXCEPTION

   // keep compiler happy
   return Success();
}

Error MacSpellingEngine::addWord(const std::string& langId,
                                 const std::string& word,
                                 bool *pAdded)
{
   AutoreleaseContext arContext;

   return Success();
}

Error MacSpellingEngine::removeWord(const std::string& langId,
                                    const std::string& word,
                                    bool *pRemoved)
{
   AutoreleaseContext arContext;

   return Success();
}


} // namespace spelling
} // namespace core 



