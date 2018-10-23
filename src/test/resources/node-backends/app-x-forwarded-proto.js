const http = require('http')
const port = process.env.PORT || 8080

const requestHandler = (request, response) => {
    response.end(request.headers['x-forwarded-proto'])
}

const server = http.createServer(requestHandler)

server.listen(port, (err) => {
    if (err) {
        return console.log('something bad happened', err)
    }

    console.log(`server simple is listening on ${port}`)
})