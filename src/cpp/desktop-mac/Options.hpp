/*
 * Options.hpp
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

#ifndef DESKTOP_OPTIONS_HPP
#define DESKTOP_OPTIONS_HPP

#include <string>
#include <vector>

#include <boost/utility.hpp>

#include <core/FilePath.hpp>

#import <Foundation/NSArray.h>

namespace desktop {

class Options;
Options& options();
   
class Options : boost::noncopyable
{
private:
   Options();
   friend Options& options();
   
public:
   void initFromCommandLine(NSArray* arguments);

   std::string portNumber() const;
   std::string newPortNumber();
   
   std::string sharedSecret() const { return sharedSecret_; }
   void setSharedSecret(const std::string& secret) { sharedSecret_ = secret; }
   
   std::string proportionalFont() const;
   std::string fixedWidthFont() const;
   void setFixedWidthFont(std::string font);
   
   double zoomLevel() const;
   void setZoomLevel(double zoomLevel);
      
   core::FilePath scriptsPath() const;
   void setScriptsPath(const core::FilePath& scriptsPath);
   
   core::FilePath executablePath() const;
   core::FilePath supportingFilePath() const;
   
   core::FilePath wwwDocsPath() const;
      
   std::vector<std::string> ignoredUpdateVersions() const;
   void setIgnoredUpdateVersions(const std::vector<std::string>& ignored);
   
   bool runDiagnostics() { return runDiagnostics_; }
   
private:
   std::string sharedSecret_;
   core::FilePath scriptsPath_;
   mutable core::FilePath executablePath_;
   mutable core::FilePath supportingFilePath_;
   mutable std::string portNumber_;
   bool runDiagnostics_;
   
   
};
   
} // namespace desktop

#endif // DESKTOP_OPTIONS_HPP