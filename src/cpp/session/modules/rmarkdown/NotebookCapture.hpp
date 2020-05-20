/*
 * NotebookCapture.hpp
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

#ifndef SESSION_NOTEBOOK_CAPTURE_HPP
#define SESSION_NOTEBOOK_CAPTURE_HPP

#include <shared_core/json/Json.hpp>

#include <boost/noncopyable.hpp>
#include <string>

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

// NotebookCapture is an abstract class representing an object that captures
// content for a notebook chunk. Because the details required to initiate
// capture vary, this class is not fully RAII; instead, it keeps track of
// whether it is "connected" (capturing output), and disconnects itself when it
// goes out of scope.
class NotebookCapture : boost::noncopyable
{
public:
   NotebookCapture();
   virtual ~NotebookCapture();
   virtual void connect();
   virtual void disconnect();
   virtual void onExprComplete();
   bool connected();

   // gives capturing context a chance to handle a condition; returns true if
   // the condition was handled by the context
   virtual bool onCondition(Condition condition, const std::string& message);

protected:
   void beginConditionCapture();
   bool capturingConditions();
   core::json::Value endConditionCapture();

private:
   bool connected_;
   boost::shared_ptr<core::json::Array> conditions_;
};

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_NOTEBOOK_CAPTURE_HPP
