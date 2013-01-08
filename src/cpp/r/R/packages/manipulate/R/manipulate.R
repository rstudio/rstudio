#
# manipulate.R
#
# Copyright (C) 2009-11 by RStudio, Inc.
#
# Unless you have received this program directly from RStudio pursuant
# to the terms of a commercial license agreement with RStudio, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#


slider <- function(min, max, initial = min, label = NULL, step = NULL, ticks = TRUE)
{
  # validate inputs
  if (!is.numeric(initial) || !is.numeric(min) || !is.numeric(max))
    stop("min, max, amd initial must all be numeric values")
  else if (initial < min)
    stop(paste("slider initial value", initial, "is less than the specified minimum"))
  else if (initial > max)
    stop(paste("slider initial value", initial, "is greater than the specified maximum"))
  else if (min > max)
    stop(paste("slider maximum is greater than minimum"))
  else if ( !is.null(step) )
  {
    if ( !is.numeric(step) )
      stop("step is not a numeric value")
    if ( step > (max - min) )
      stop("step is greater than range")
  }
  else if ( !is.logical(ticks) )
    stop("ticks is not a logical value")
  else if ( !is.null(label) && !is.character(label) )
    stop("label is not a character value")
  
  # serialize "default" step as -1
  if ( is.null(step) )
    step <- -1
  
  # create slider and return it
  slider <- list(type = 0,
                 min = min,
                 max = max,
                 initialValue = initial,
                 label = label,
                 step = step,
                 ticks = ticks)
  class(slider) <- "manipulator.slider"
  return (slider)
}

picker <- function(..., initial = NULL, label = NULL)
{
  # get values
  values <- resolveVariableArguments(list(...))
  
  # get value names
  valueNames <- names(values)
  if (is.null(valueNames))
    valueNames <- character(length(values))
 
  # default missing names to choice values
  missingNames <- valueNames == ""
  valueNames[missingNames] <- paste(values)[missingNames]
  names(values) <- valueNames
  
  # validate inputs
  if ( length(values) < 1 )
  {
    stop("picker choices must contain at least one value")
  }
  else if ( length(valueNames) != length(unique(valueNames)) )
  {
    stop("picker choices must have unique names (duplicate detected)")
  }
  else if ( !is.null(initial) )
  {
    if (length(initial) != 1)
      stop("initial must be a single object")
    else if ( !(as.character(initial) %in% valueNames) )
      stop("initial doesn't match one of the supplied choices") 
  }
  else if ( !is.null(label) && !is.character(label) )
  {
    stop("label is not a character value")
  }

  # provide default value if necessary
  if ( is.null(initial) )
    initial <- valueNames[1]

  # create picker
  picker <- list(type = 1,
                 choices = valueNames,
                 values = values,
                 initialValue = initial,
                 label = label)
  class(picker) <- "manipulator.picker"
  return (picker) 
}

checkbox <- function(initial = FALSE, label = NULL)
{
  # validate inputs
  if ( !is.logical(initial) )
    stop("initial must be a logical")
  else if ( !is.null(label) && !is.character(label) )
    stop("label is not a character value")
  
  # create checkbox and return it
  checkbox <- list(type = 2,
                   initialValue = initial,
                   label = label)
  class(checkbox) <- "manipulator.checkbox"
  return (checkbox)
}

button <- function(label)
{
  # validate inputs
  if ( !is.null(label) && !is.character(label) )
    stop("label is not a character value")
  
  # create button and return it
  button <- list(type = 3, 
                 initialValue = FALSE,
                 label = label)
  class(button) <- "manipulator.button"
  return (button)
}

manipulatorGetState <- function(name)
{
  if ( hasActiveManipulator() )
  {
    value <- NULL
    try(silent = TRUE,
        value <- get(name, 
                     envir = get(".state", envir = activeManipulator()))
    )
    return (value)
  }
  else
  {
    stop("no plot manipulator currently active")
  }
}

manipulatorSetState <- function(name, value)
{
  if ( hasActiveManipulator() )
  {
     assign(name, value, envir = get(".state", envir = activeManipulator()))
     ensureManipulatorSaved()
     invisible(NULL)
  }
  else
  {
    stop("no plot manipulator currently active")
  }
}

manipulatorMouseClick <- function()
{
  if ( hasActiveManipulator() )
  {
    # if there is no .mouseClick then create a NULL one (the existence of 
    # the .mouseClick object is a sentinel indicating that we should 
    # callback the manipulate function every time the plot is clicked)
    if (!exists(".mouseClick", envir = activeManipulator()))
      assign(".mouseClick", NULL, envir = activeManipulator())
      
    # return the .mouseClick
    get(".mouseClick", envir = activeManipulator())
  }
  else
  {
    stop("no plot manipulator currently active")
  }
}

manipulate <- function(`_expr`, ...)
{
  # create new list container for the manipulator
  manipulator <- new.env(parent = parent.frame())
  manipulator$.id <- createUUID()
  
  # save the unevaluated expression as the code
  manipulator$.code <- substitute(`_expr`)
  
  # save a human readable version of the code (specify control = NULL
  # to make the display as close to the original text as possible)
  manipulator$.codeAsText <- deparse(substitute(`_expr`), control = NULL)
  
  # get the controls and control names
  controls <- resolveVariableArguments(list(...))
  controlNames <- names(controls)

  # validate that all controls have unique names
  duplicatedIndex <- anyDuplicated(controlNames)
  if (duplicatedIndex > 0)
    stop(paste("duplicated control name:", controlNames[[duplicatedIndex]]))

  # save the controls and their names into the manipulator
  manipulator$.controls <- controls
  manipulator$.variables <- controlNames
  
  # establish state
  manipulator$.state <- new.env(parent = globalenv())
  
  # establish 'user visible' values (indirection btw e.g. picker choice & value)
  manipulator$.userVisibleValues <- new.env(parent = globalenv())

  # iterate over the names and controls, adding the default values to the env
  manipulator$.buttonNames <- character()
  for (name in names(controls))
  {
    # check the name
    if (name == "")
      stop("all controls passed to manipulate must be named")
    
    # confirm that this is in fact a control
    control <- controls[[name]]
    if ( ! (class(control) %in% c("manipulator.slider",
                                  "manipulator.picker",
                                  "manipulator.checkbox",
                                  "manipulator.button")) )
    {
      stop(paste("argument", name, "is not a control"))
    }
      
    # keep a special side list of button controls (so we can 
    # always set them to FALSE after execution)
     if (inherits(control, "manipulator.button"))
       manipulator$.buttonNames <- append(manipulator$.buttonNames, name)
                                    
    # assign the control's default into the list
    setManipulatorValue(manipulator, name, control$initialValue)
  }

  # execute the manipulator -- will execute the code and attach it
  # to the first plot (if any) generated by the execution
  executeAndAttachManipulator(manipulator)
  
  # return invisibly
  invisible(NULL)
}


