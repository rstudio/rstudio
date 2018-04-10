// !preview r2d3 data=data.frame(x = rnorm(50), y = rnorm(50))
//
// R2D3: https://rstudio.github.io/r2d3
//

const padding = 25;

const x_scale = d3.scaleLinear().range([padding, width - padding]);
x_scale.domain(d3.extent(data, d => d.x));

const y_scale = d3.scaleLinear().range([height - padding, padding]);
y_scale.domain(d3.extent(data, d => d.y));
    
svg.selectAll('.scatter_points')
  .data(data)
  .enter().append('circle')
    .attr('cx', d => x_scale(d.x))
    .attr('cy', d => y_scale(d.y))
    .attr('r', 5);
