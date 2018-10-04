const http = require('http')
const net = require('net')
const url = require('url')

const port = process.env.PORT || 8080


const requestHandler = (request, response) => {
    console.log(request.url)
    response.end('Hello Node.js Server!')
}

const server = http.createServer(requestHandler)

server.on('checkContinue', function(req, res) {
    console.log("on checkContinue")
    req.checkContinue = true
    handlePostFile(req, res)
})

function handlePostFile(req, res) {
    if (req.checkContinue === true) {
        req.checkContinue = false
        res.writeContinue()
    }
  
    var body = ''
    req.once('readable', function() {
        var chunk

        while ((chunk = req.read()) !== null) {
            body += chunk
        }

        console.log("finish to read")
        res.end()
    });
}

server.listen(port)
console.log(`server http-continue is listening on ${port}`)

