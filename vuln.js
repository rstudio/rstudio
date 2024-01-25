const response = await fetch('https://example.com')
const text = await response.text()

const div = document.getElementById('link')
div.innerHTML = `<div>${text}</div>`
