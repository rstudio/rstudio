/*
 * RFunctionInformation.hpp
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

#ifndef CORE_R_UTIL_R_FUNCTION_INFORMATION_HPP
#define CORE_R_UTIL_R_FUNCTION_INFORMATION_HPP

// Utility classes used for collecting information about R packages
// (their functions and what they do)

#include <vector>
#include <string>
#include <map>

#include <core/Log.hpp>
#include <core/collection/Position.hpp>

#include <boost/optional.hpp>
#include <boost/logic/tribool.hpp>

namespace rstudio {
namespace core {
namespace r_util {

struct Binding
{
   Binding(const std::string& name,
           const std::string& origin)
      : name(name), origin(origin)
   {}
   
   friend std::ostream& operator <<(std::ostream& os,
                                    const Binding& self)
   {
      os << "[" << self.origin << "::" << self.name << "]";
      return os;
   }
   
   std::string name;
   std::string origin;
};

class FormalInformation
{
public:
   
   // ctor -- must be initialized with a name;
   // all other information is optional
   explicit FormalInformation(const std::string& name)
      : name_(name)
   {}
   
   void setDefaultValue(const std::string& defaultValue)
   {
      hasDefault_ = true;
      defaultValue_ = defaultValue;
   }
   
   void setHasDefaultValue(bool value)
   {
      hasDefault_ = value;
   }
   
   const std::string& name() const { return name_; }
   const boost::optional<std::string>& defaultValue() const { return defaultValue_; }
   boost::tribool hasDefault() const { return hasDefault_; }
   bool isUsed() const { return bool(isUsed_); }
   void setIsUsed(bool value) { isUsed_ = value; }
   bool isMissingnessHandled() const { return bool(isMissingnessHandled_); }
   void setMissingnessHandled(bool value) { isMissingnessHandled_ = value; }
   
private:
   std::string name_;
   
   // NOTE: It is possible for us to know that a particular
   // function has a default value, but not what that default
   // value is, hence why we have separate fields here.
   boost::optional<std::string> defaultValue_;
   boost::tribool hasDefault_;
   
   // Whether this formal is used in the body of its associated function
   boost::tribool isUsed_;
   
   // Whether this formal is tested in a `missing()` call
   boost::tribool isMissingnessHandled_;
   
   // private c-tor used as dummy 'no such formal', for friend classes
   friend class FunctionInformation;
   FormalInformation() {}
};

class FunctionInformation
{
public:
   
   // default ctor: we may not know the original binding
   // for this function
   FunctionInformation()
   {}
   
   // binding ctor: gives the 'origin' of this function name
   // (name + 'origin', which could be a package, namespace, env, ...)
   FunctionInformation(const std::string& name,
                       const std::string& origin)
      : binding_(Binding(name, origin))
   {}
   
   void addFormal(const std::string& name)
   {
      formals_.push_back(FormalInformation(name));
      formalNames_.push_back(name);
   }
   
   void addFormal(const FormalInformation& info)
   {
      formals_.push_back(info);
      formalNames_.push_back(info.name());
   }
   
   bool isPrimitive()
   {
      return bool(isPrimitive_);
   }
   
   void setIsPrimitive(bool isPrimitive)
   {
      isPrimitive_ = isPrimitive;
   }
   
   const std::vector<FormalInformation>& formals() const
   {
      return formals_;
   }
   
   std::vector<FormalInformation>& formals()
   {
      return formals_;
   }
   
   
   const std::vector<std::string>& getFormalNames() const
   {
      return formalNames_;
   }
   
   const boost::optional<std::string>& defaultValueForFormal(
         const std::string& formalName)
   {
      return infoForFormal(formalName).defaultValue();
   }
   
   FormalInformation& infoForFormal(const std::string& formalName)
   {
      std::size_t n = formals_.size();
      for (std::size_t i = 0; i < n; ++i)
         if (formals_[i].name() == formalName)
            return formals_[i];
      
      LOG_WARNING_MESSAGE("No such formal '" + formalName + "'");
      return noSuchFormal_;
   }
   
   const boost::optional<Binding>& binding() const
   {
      return binding_;
   }
   
   void setPerformsNse(bool performsNse) { performsNse_ = performsNse; }
   boost::tribool performsNse() const { return performsNse_; }
   
private:
   boost::optional<Binding> binding_;
   std::vector<FormalInformation> formals_;
   std::vector<std::string> formalNames_;
   boost::tribool isPrimitive_;
   boost::tribool performsNse_;
   
   // Provided so that 'infoForFormal' can return by reference
   FormalInformation noSuchFormal_;
};

typedef std::string FunctionName;
typedef std::map<FunctionName, FunctionInformation> FunctionInformationMap;

struct PackageInformation
{
   std::string package;
   std::vector<std::string> exports;
   std::vector<int> types;
   std::vector<std::string> datasets;
   FunctionInformationMap functionInfo;
};

inline std::map<std::string, std::vector<std::string> > infoToFormalMap(
      const std::map<std::string, FunctionInformation>& info)
{
   std::map<std::string, std::vector<std::string> > result;
   typedef std::map<std::string, FunctionInformation>::const_iterator const_iterator;
   for (const_iterator it = info.begin(); it != info.end(); ++it)
      result[it->first] = it->second.getFormalNames();
   return result;
}

} // namespace r_util
} // namespace core
} // namespace rstudio

#endif // CORE_R_UTIL_R_FUNCTION_INFORMATION_HPP
