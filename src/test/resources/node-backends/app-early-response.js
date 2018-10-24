const http = require('http')
const port = process.env.PORT || 8080

const requestHandler = (request, response) => {
    response.statusCode = 401;
    response.end();
}

const server = http.createServer(requestHandler)

server.listen(port, (err) => {
    if (err) {
        return console.log('something bad happened', err)
    }

    console.log(`server simple is listening on ${port}`)
})
