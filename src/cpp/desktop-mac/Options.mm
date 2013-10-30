/*
 * Options.mm
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


#include <core/FilePath.hpp>
#include <core/Random.hpp>
#include <core/SafeConvert.hpp>

#import <Foundation/NSString.h>
#import <Foundation/NSUserDefaults.h>
#import <Foundation/NSDictionary.h>

#import "Options.hpp"

using namespace core;

namespace desktop {
   
Options& options()
{
   static Options instance;
   return instance;
}
   
Options::Options()
   : runDiagnostics_(false)
{
   // initialize user defaults
   NSUserDefaults* prefs = [NSUserDefaults standardUserDefaults];
   NSDictionary* defs = [NSDictionary dictionaryWithObjectsAndKeys:
                             @"Lucida Grande", @"font.proportional",
                             @"Monaco", @"font.fixed",
                             @"1.0", @"view.zoomlevel",
                             nil];
                             
   [prefs registerDefaults:defs];
}
   
void Options::initFromCommandLine(NSArray* arguments)
{
   for (NSString* arg in arguments)
   {
      if ([arg isEqualToString: @"--run-diagnostics"])
         runDiagnostics_ = true;
   }
}

std::string Options::portNumber() const
{
   // lookup / generate on demand
   if (portNumber_.empty())
   {
      // Use a random-ish port number to avoid collisions between different
      // instances of rdesktop-launched rsessions
      int base = std::abs(random::uniformRandomInteger<int>());
      int port = (base % 40000) + 8080;
      portNumber_ = safe_convert::numberToString(port);
   }
   return portNumber_;
}
   
std::string Options::newPortNumber()
{
   portNumber_.clear();
   return portNumber();
}

std::string Options::proportionalFont() const
{
   NSUserDefaults* prefs = [NSUserDefaults standardUserDefaults];
   return [[prefs stringForKey: @"font.proportional"] UTF8String];
}

std::string Options::fixedWidthFont() const
{
   NSUserDefaults* prefs = [NSUserDefaults standardUserDefaults];
   return [[prefs stringForKey: @"font.fixed"] UTF8String];
}

void Options::setFixedWidthFont(std::string font)
{
   NSUserDefaults* prefs = [NSUserDefaults standardUserDefaults];
   [prefs setObject: [NSString stringWithUTF8String: font.c_str()]
             forKey: @"font.fixed"];
}

double Options::zoomLevel() const
{
   NSUserDefaults* prefs = [NSUserDefaults standardUserDefaults];
   NSString* zoomLevel = [prefs stringForKey: @"view.zoomlevel"];
   return [zoomLevel doubleValue];
}
   
void Options::setZoomLevel(double zoomLevel)
{
   NSUserDefaults* prefs = [NSUserDefaults standardUserDefaults];
   NSNumber* zoom = [NSNumber numberWithDouble: zoomLevel];
   [prefs setObject: [zoom stringValue] forKey: @"view.zoomlevel"];
}

FilePath Options::scriptsPath() const
{
   return FilePath();
}
   
void Options::setScriptsPath(const FilePath& scriptsPath)
{
      
}

core::FilePath Options::executablePath() const
{
   return FilePath();
}
   
FilePath Options::supportingFilePath() const
{
   return FilePath();
}

FilePath Options::wwwDocsPath() const
{
   return FilePath();
}

std::vector<std::string> Options::ignoredUpdateVersions() const
{
   return std::vector<std::string>();
}
   
void Options::setIgnoredUpdateVersions(const std::vector<std::string>& ignored)
{
      
}

FilePath Options::scratchTempDir(FilePath defaultPath)
{
   return FilePath();
}

void Options::cleanUpScratchTempDir()
{
      
}
   
   

} // namespace desktop

