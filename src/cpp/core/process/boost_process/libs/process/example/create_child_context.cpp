// 
// Boost.Process 
// ~~~~~~~~~~~~~ 
// 
// Copyright (c) 2006, 2007 Julio M. Merino Vidal 
// Copyright (c) 2008 Ilya Sokolov, Boris Schaeling 
// Copyright (c) 2009 Boris Schaeling 
// Copyright (c) 2010 Felipe Tanus, Boris Schaeling 
// 
// Distributed under the Boost Software License, Version 1.0. (See accompanying 
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt) 
// 

#include <boost/process/all.hpp> 
#include <string> 
#include <vector> 
#include <utility> 
#include <iostream> 

void create_child_context_configuration() 
{ 
//[create_child_context_configuration 
    std::string exe = boost::process::find_executable_in_path("hostname"); 
    std::vector<std::string> args; 
    boost::process::context ctx; 
    ctx.process_name = "hostname"; 
    ctx.work_dir = "C:\\"; 
    ctx.env.insert(std::make_pair("new_variable", "value")); 
    boost::process::create_child(exe, args, ctx); 
//] 
} 

void create_child_context_null() 
{ 
//[create_child_context_null 
    std::string exe = boost::process::find_executable_in_path("hostname"); 
    std::vector<std::string> args; 
    boost::process::context ctx; 
    ctx.streams[boost::process::stdout_id] = boost::process::behavior::null(); 
    boost::process::create_child(exe, args, ctx); 
//] 
} 

void create_child_context_pipe() 
{ 
//[create_child_context_pipe 
    std::string exe = boost::process::find_executable_in_path("hostname"); 
    std::vector<std::string> args; 
    boost::process::context ctx; 
    ctx.streams[boost::process::stdout_id] = boost::process::behavior::pipe(); 
    boost::process::child c = boost::process::create_child(exe, args, ctx); 
    boost::process::pistream is(c.get_handle(boost::process::stdout_id)); 
    std::cout << is.rdbuf() << std::flush; 
//] 
} 

int main() 
{ 
    create_child_context_configuration(); 
    create_child_context_null(); 
    create_child_context_pipe(); 
} 
