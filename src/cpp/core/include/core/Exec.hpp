/*
 * Exec.hpp
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

#ifndef CORE_EXEC_HPP
#define CORE_EXEC_HPP

#include <vector>

#include <boost/function.hpp>

namespace rstudio {
namespace core {

class Error;
   
class ExecBlock
{
public:
   typedef boost::function<core::Error()> Function;

public:
   ExecBlock() {}
  
    // COPYING: via compiler (copyable members)
   
   // add to the block
   ExecBlock& add(Function function);
   
   // easy init style (based on idiom in boost::program_options)
   class EasyInit;
   EasyInit addFunctions() { return EasyInit(this); }
   
   // execute the block
   core::Error execute() const;
   
   // allow an ExecBlock to act as a boost::function<core::Error()>
   core::Error operator()() const;
   
public:
   // easy init helper class
   class EasyInit
   {
   public:
      EasyInit(ExecBlock* pExecBlock) : pExecBlock_(pExecBlock) {}
      EasyInit& operator()(Function function) 
      {
         pExecBlock_->add(function);
         return *this;
      }
   private:
      ExecBlock* pExecBlock_;
   };
private:
   std::vector<Function> functions_;
};
   

} // namespace core 
} // namespace rstudio

#endif // CORE_EXEC_HPP

