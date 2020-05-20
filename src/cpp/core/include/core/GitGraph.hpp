/*
 * GitGraph.hpp
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
#ifndef CORE_GITGRAPH_HPP
#define CORE_GITGRAPH_HPP

#include <string>
#include <vector>

#include <boost/noncopyable.hpp>

namespace rstudio {
namespace core {
namespace gitgraph {

struct Column
{
   Column(int id, const std::string& preCommit, const std::string& postCommit)
      : id(id), preCommit(preCommit), postCommit(postCommit)
   {
   }

   // The ID of the column--useful for coloring.
   int id;

   // The commit that the column points to at the start. If the column
   // starts here the preCommit will be empty
   std::string preCommit;
   // The commit that the column points to at the end. If the column
   // terminates here the postCommit will be empty
   std::string postCommit;
};

// Represents one line of the graph.
class Line : public std::vector<Column>
{
public:
   // The nexus is the column that represents this row's commit.
   // There are potentially multiple columns that could be used
   // to represent the commit; we pick the leftmost one as this
   // results in the most orderly graphs.
   size_t nexus() const;

   // Prints out a machine-parsable string representation of this row
   std::string string() const;
};

// Encapsulates the state and logic used to build up a graph,
// based on repeated calls with commit-and-parent info.
// This class doesn't hold all of the result lines; the caller
// must decide what to do with the lines as they are created
// during calls to addCommit.
class GitGraph : boost::noncopyable
{
public:
   GitGraph() : nextColumnId_(0)
   {}

   // Call addCommit to yield the next line of the graph.
   // Note that GitGraph is stateful; each call to addCommit
   // builds on the state of previous calls to addCommit.
   // So it's important to call in reverse chronological or
   // topographic order.
   Line addCommit(const std::string& commit,
                  const std::vector<std::string>& parents);

private:
   int nextColumnId_;
   Line pendingLine_;
};

} // namespace gitgraph
} // namespace core
} // namespace rstudio

#endif // CORE_GITGRAPH_HPP
