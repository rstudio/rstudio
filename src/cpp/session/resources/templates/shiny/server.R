
# This is the server logic for a Shiny web application.
# You can find out more about building applications with Shiny here:
# 
# http://www.rstudio.com/shiny/
#

library(shiny)

shinyServer(function(input, output) {
   
  output$distPlot <- renderPlot({
     
    # generate and plot an rnorm distribution with the requested
    # number of observations
    dist <- rnorm(input$obs)
    hist(dist)
    
  })
  
})
