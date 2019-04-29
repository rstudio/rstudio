#!/usr/bin/env Rscript

require(jsonlite)
require(stringi)

generate <- function (schemaPath) {
   # Extract prefs from JSON schema
   schema <- jsonlite::read_json(schemaPath)
   prefs <- schema$properties
   java <- ""
   
   for (pref in names(prefs)) {
      # Convert the preference name from camel case to snake case
      camel <- gsub("_(.)", "\\U\\1\\E", pref, perl = TRUE)
      def <- prefs[[pref]]
      
      # Convert JSON schema type to corresponding Java type
      type <- def[["type"]]
      if (type == "object") {
         # Rich objects are converted to JavaScript native types
         type <- paste0(toupper(substring(camel, 1, 1)), 
                        substring(camel, 2))
      } else if (type == "numeric" || type == "number") {
         # Numerics (not integers) become Double objects
         type <- "Double"
      } else if (type == "array") {
         # Arrays are presumed to be arrays of strings (we do not currently support
         # other types)
         type <- "JsArrayString"
      } else {
         # For all other types, we uppercase the type name to get a class name
         type <- paste0(toupper(substring(type, 1, 1)), 
                        substring(type, 2))
      }
      
      preftype <- def[["type"]]
      if (identical(preftype, "boolean")) {
         preftype <- "bool"
      } else if (identical(preftype, "numeric")) {
         preftype <- "dbl"
      } else if (identical(preftype, "array")) {
         preftype <- "object"
      }
      
      defaultval <- as.character(def[["default"]])
      if (identical(def[["type"]], "string")) {
         # Quote string defaults
         defaultval <- paste0("\"", defaultval, "\"")
      } else if (identical(def[["type"]], "boolean")) {
         # Convert Booleans from R-style (TRUE, FALSE) to Java style (true, false)
         defaultval <- tolower(defaultval)
      } else if (identical(def[["type"]], "numeric")) {
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
      
      java <- paste0(java,
         "   /**\n",
         "    * ", def[["description"]], "\n",
         "    */\n",
         "   public PrefValue<", type, "> ", camel, "()\n",
         "   {\n",
         "      return ", preftype, "(\"", pref, "\", ", defaultval, ");\n",
         "   }\n\n")
      
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
               proptype <- "JsArrayEx"
            } else if (identical(proptype, "string")) {
               proptype <- "String"
            } else if (identical(proptype, "integer")) {
               proptype <- "Integer"
            }
            java <- paste0(java,
              "      public final native ", proptype, " get",  propname, "() /*-{\n",
              "         return this.", prop, ";\n",
              "      }-*/;\n\n")
         }
         java <- paste0(java, "   }\n\n")
      }
      
      # Emit convenient enum constants if supplied
      if (!is.null(def[["enum"]])) {
         for (enumval in def[["enum"]]) {
            # Create syntactically valid variable name
            varname <- toupper(pref)
            valname <- toupper(gsub("[^A-Za-z0-9_]", "_", enumval))
            java <- paste0(java,
               "   public final static ", type, " ", varname, "_", 
               valname, " = \"", enumval, "\";\n"
            )
         }
         java <- paste0(java, "\n")
      }
      
   }
   
   # Return computed Java
   java
}

# Generate preferences
java <- generate("../../cpp/session/resources/schema/user-prefs-schema.json")
javaTemplate <- readLines("prefs/UserPrefsAccessor.java")
writeLines(gsub("%PREFS%", java, javaTemplate), 
           con = "../src/org/rstudio/studio/client/workbench/prefs/model/UserPrefsAccessor.java")

# Generate state
java <- generate("../../cpp/session/resources/schema/user-state-schema.json")
javaTemplate <- readLines("prefs/UserStateAccessor.java")
writeLines(gsub("%STATE%", java, javaTemplate), 
           con = "../src/org/rstudio/studio/client/workbench/prefs/model/UserStateAccessor.java")
