// This backend return his ID pass by env var
const http = require('http')
const port = process.env.PORT || 8080
const id = process.env.ID || "condor"

const requestHandler = (request, response) => {
    console.log(request.url)
    response.end(id)
}

const server = http.createServer(requestHandler)

server.listen(port, (err) => {
    if (err) {
        return console.log('something bad happened', err)
    }

    console.log(`app-id is listening on ${port}`)
})