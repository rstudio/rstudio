/*
 * TexEngine.hpp
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

#ifndef CORE_TEX_TEX_ENGINE_HPP
#define CORE_TEX_TEX_ENGINE_HPP

#include <string>

#include <boost/utility.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/function.hpp>

#include <core/system/Process.hpp>
#include <core/system/Environment.hpp>

namespace core {

class Error;
class FilePath;

namespace tex {

typedef boost::function<core::Error(
   const std::string&,
   const std::vector<std::string>&,
   const core::system::ProcessOptions&)> RunProgramFunction;

class TexEngine : boost::noncopyable
{
public:
   explicit TexEngine(const std::string& name)
      : name_(name)
   {
   }

   virtual ~TexEngine()
   {
   }

public:

   const std::string& name() const { return name_; }


   Error typeset(const core::system::Options& extraEnvironmentVars,
                 const FilePath& texFilePath,
                 const RunProgramFunction& runFunction);

private:
   virtual FilePath programFilePath() = 0;

private:
   std::string name_;
};

Error createPdfLatex(boost::shared_ptr<TexEngine>* ppEngine);


} // namespace tex
} // namespace core 


#endif // CORE_TEX_TEX_ENGINE_HPP

