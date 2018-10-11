const fs = require('fs')

const port = process.env.PORT || 1026
const file = process.env.FILE || "app.js"

require('http').createServer((req, res) => {

    if (req.method === 'POST') {
        let body = ''
        req.on('data', chunk => {
	  console.log("received chunk: "+chunk.length)
	  body += chunk.toString()
        })
        req.on('end', () => {
	  res.end('ok')
        })
    }
    else {
        const src = fs.createReadStream(file, { highWaterMark: 4096 })
        src.on('data', (chunk) => {
	  console.log("chunk: "+chunk.length);
	  res.write(chunk)
        })
        src.on('end', () => {
	  res.end()
        })
    }
}).listen(port)

console.log(`server app-chunk-response is listening on ${port} with file ${file}`)