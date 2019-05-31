#!/usr/bin/env Rscript

require(jsonlite)
require(stringi)

capitalize <- function(s) {
   paste0(toupper(substring(s, 1, 1)), substring(s, 2))
}

javaenum <- function(def, pref, type, indent) {
   java <- ""
   # Emit convenient enum constants if supplied
   if (!is.null(def[["enum"]])) {
      for (enumval in def[["enum"]]) {
         # Create syntactically valid variable name
         varname <- toupper(pref)
         valname <- toupper(gsub("[^A-Za-z0-9_]", "_", enumval))
         java <- paste0(java, indent,
            "public final static ", type, " ", varname, "_", 
            valname, " = \"", enumval, "\";\n"
         )
      }
      java <- paste0(java, "\n")
   }
   java
}

cppenum <- function(def, pref, type, indent) {
   cpp <- ""
   # Emit convenient enum constants if supplied
   if (!is.null(def[["enum"]])) {
      for (enumval in def[["enum"]]) {
         # Create syntactically valid variable name
         varname <- paste0("k", capitalize(pref))
         valname <- gsub("[^A-Za-z0-9_]", "_", enumval)
         valname <- gsub("_(.)", "\\U\\1\\E", valname, perl = TRUE)
         cpp <- paste0(cpp, indent,
            "#define ", varname, capitalize(valname), " \"", enumval, "\"\n")
      }
   }
   cpp
}

generate <- function (schemaPath, className) {
   # Extract prefs from JSON schema
   schema <- jsonlite::read_json(schemaPath)
   prefs <- schema$properties
   java <- ""
   cpp <- ""
   hpp <- ""
   
   cppprefenum <- paste0("enum ", capitalize(className), "\n{\n")
   cppstrings <- ""
   
   for (pref in names(prefs)) {
      # Convert the preference name from camel case to snake case
      camel <- gsub("_(.)", "\\U\\1\\E", pref, perl = TRUE)
      def <- prefs[[pref]]
      
      # Convert JSON schema type to corresponding Java type
      type <- def[["type"]]
      if (type == "object") {
         # Rich objects are converted to JavaScript native types
         type <- capitalize(camel) 
      } else if (type == "numeric" || type == "number") {
         # Numerics (not integers) become Double objects
         type <- "Double"
      } else if (type == "array") {
         # Arrays are presumed to be arrays of strings (we do not currently support
         # other types)
         type <- "JsArrayString"
      } else {
         # For all other types, we uppercase the type name to get a class name
         type <- capitalize(type)
      }
      
      preftype <- def[["type"]]
      if (identical(preftype, "boolean")) {
         preftype <- "bool"
         cpptype <- "bool"
      } else if (identical(preftype, "numeric") || identical(preftype, "number")) {
         preftype <- "dbl"
         cpptype <- "double"
      } else if (identical(preftype, "array")) {
         preftype <- "object"
         cpptype <- "core::json::Array"
      } else if (identical(preftype, "integer")) {
         preftype <- "integer"
         cpptype <- "int"
      } else if (identical(preftype, "string")) {
         preftype <- "string"
         cpptype <- "std::string"
      } else if (identical(preftype, "object")) {
         preftype <- "object"
         cpptype <- "core::json::Object"
      }
      
      defaultval <- as.character(def[["default"]])
      if (identical(def[["type"]], "string")) {
         # Quote string defaults
         defaultval <- paste0("\"", defaultval, "\"")
      } else if (identical(def[["type"]], "boolean")) {
         # Convert Booleans from R-style (TRUE, FALSE) to Java style (true, false)
         defaultval <- tolower(defaultval)
      } else if (identical(def[["type"]], "numeric") || 
                 identical(def[["type"]], "number")) {
         # Ensure floating point numbers have a decimal
         if (!grepl(".", defaultval, fixed = TRUE)) {
            defaultval <- paste0(defaultval, ".0")
         }
      } else if (identical(def[["type"]], "array")) {
         # We currently only support string arrays
         defaultval <- sapply(defaultval, function(d) { 
            paste0("\"", d, "\"")}
         )
         defaultval <- paste0("JsArrayUtil.createStringArray(", paste(defaultval, collapse = ", "), ")")
      } else if (identical(def[["type"]], "object")) {
         # No current mechanism for construction default objects
         defaultval <- "null"
      }
      
      cppstrings <- paste0(cppstrings,
                           "#define k", capitalize(camel), " \"", pref, "\"\n")
      cppstrings <- paste0(cppstrings, cppenum(def, camel, type, ""))
      comment <- paste0(
         "   /**\n",
         "    * ", def[["description"]], "\n",
         "    */\n")
      
      java <- paste0(java,
         comment,
         "   public PrefValue<", type, "> ", camel, "()\n",
         "   {\n",
         "      return ", preftype, "(\"", pref, "\", ", defaultval, ");\n",
         "   }\n\n")
      
      hpp <- paste0(hpp, comment,
                    "   ", cpptype, " ", camel, "();\n")
      hpp <- paste0(hpp, 
                    "   core::Error set", capitalize(camel), "(", cpptype, " val);\n\n")
      cpp <- paste0(cpp, 
         "/**\n",
         " * ", def[["description"]], "\n",
         " */\n",
         cpptype, " ", className, "::", camel, "()\n",
         "{\n",
         "   return readPref<", cpptype, ">(\"", pref, "\");\n",
         "}\n\n",
         "core::Error ", className, "::set", capitalize(camel), "(", cpptype, " val)\n",
         "{\n",
         "   return writePref(\"", pref, "\", val);\n",
         "}\n\n")
      
      # Emit JSNI for object types
      if (identical(def[["type"]], "object")) {
         java <- paste0(java,
            "   public static class ", type, " extends JavaScriptObject\n",
            "   {\n",
            "      protected ", type, "() {} \n\n")
         props <- def[["properties"]]
         for (prop in names(props)) {
            propdef <- props[[prop]]
            propname <- gsub("(^|_)(.)", "\\U\\2\\E", prop, perl = TRUE)
            proptype <- propdef[["type"]]
            if (identical(proptype, "array")) {
               proptype <- "JsArrayString"
               if (!is.null(propdef[["items"]])) {
                  enumtype <- capitalize(propdef[["items"]][["type"]])
                  java <- paste0(java, javaenum(propdef[["items"]], propname, 
                                                enumtype, "      "))
               }
            } else if (identical(proptype, "string")) {
               proptype <- "String"
            } else if (identical(proptype, "integer")) {
               proptype <- "Integer"
            }
            java <- paste0(java,
              "      public final native ", proptype, " get",  propname, "() /*-{\n",
              "         return this.", prop, ";\n",
              "      }-*/;\n\n")
            cppstrings <- paste0(cppstrings,
                                 "#define k", capitalize(camel), propname, " \"", prop, "\"\n")
         }
         java <- paste0(java, "   }\n\n")
      }
      
      # add enums if present
      java <- paste0(java, javaenum(def, pref, type, "   "))
   }
   
   cppeprefnum <- paste0(cppprefenum, "   ", className, "Max\n};\n\n")
   hpp <- paste0(cppstrings, "\n",
                 "class ", className, ": public Preferences\n", 
                 "{\n",
                 "public:\n",
                 hpp,
                 "};\n")
   
   # Return computed Java
   list(
      java = java,
      cpp = cpp,
      hpp = hpp)
}

# Generate preferences
result <- generate("../../cpp/session/resources/schema/user-prefs-schema.json",
                   "UserPrefValues")
template <- readLines("prefs/UserPrefsAccessor.java")
writeLines(gsub("%PREFS%", result$java, template), 
           con = "../src/org/rstudio/studio/client/workbench/prefs/model/UserPrefsAccessor.java")
template <- readLines("prefs/UserPrefValues.hpp")
writeLines(gsub("%PREFS%", result$hpp, template), 
           con = "../../cpp/session/include/session/prefs/UserPrefValues.hpp")
template <- readLines("prefs/UserPrefValues.cpp")
writeLines(gsub("%PREFS%", result$cpp, template), 
           con = "../../cpp/session/prefs/UserPrefValues.cpp")

# Generate state
result <- generate("../../cpp/session/resources/schema/user-state-schema.json",
                   "UserStateValues")
javaTemplate <- readLines("prefs/UserStateAccessor.java")
writeLines(gsub("%STATE%", result$java, javaTemplate), 
           con = "../src/org/rstudio/studio/client/workbench/prefs/model/UserStateAccessor.java")
template <- readLines("prefs/UserStateValues.hpp")
writeLines(gsub("%STATE%", result$hpp, template), 
           con = "../../cpp/session/include/session/prefs/UserStateValues.hpp")
template <- readLines("prefs/UserStateValues.cpp")
writeLines(gsub("%STATE%", result$cpp, template), 
           con = "../../cpp/session/prefs/UserStateValues.cpp")
