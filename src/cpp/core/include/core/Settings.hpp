/*
 * Settings.hpp
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

#ifndef CORE_SETTINGS_HPP
#define CORE_SETTINGS_HPP

#include <string>
#include <map>

#include <boost/utility.hpp>
#include <boost/function.hpp>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {

class Error;

class Settings : boost::noncopyable
{
public:
   Settings();
   virtual ~Settings();
   // COPYING: boost::noncopyable

   Error initialize(const FilePath& filePath);

public:
   void set(const std::string& name, const std::string& value);
   void set(const std::string& name, int value);
   void set(const std::string& name, double value);
   void set(const std::string& name, bool value);

   bool contains(const std::string& name) const;
   std::string get(const std::string& name, 
                   const std::string& defaultValue = std::string()) const;
   int getInt(const std::string& name, int defaultValue = 0) const;
   double getDouble(const std::string& name, double defaultValue = 0) const;
   bool getBool(const std::string& name, bool defaultValue = false) const;

   void forEach(const boost::function<void(const std::string&,
                                           const std::string&)>& func) const;

   void beginUpdate();
   void endUpdate();

   const FilePath& filePath() const { return settingsFile_; }

private:
   void writeSettings();

private:
   FilePath settingsFile_;
   std::map<std::string, std::string> settingsMap_;
   bool updatePending_;
   bool isDirty_;
};

}
}

#endif // CORE_SETTINGS_HPP

