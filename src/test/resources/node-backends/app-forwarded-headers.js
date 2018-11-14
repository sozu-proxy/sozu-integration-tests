// This backend return in the response all forwarded headers
const http = require('http')
const port = process.env.PORT || 8080

const requestHandler = (request, response) => {
    let res = {}
    res.forwarded = request.headers.forwarded
    res['x-forwarded-proto'] = request.headers['x-forwarded-proto']
    res['x-forwarded-for'] = request.headers['x-forwarded-for']
    res['x-forwarded-port'] = request.headers['x-forwarded-port']

    response.end(JSON.stringify(res))
}

const server = http.createServer(requestHandler)

server.listen(port, (err) => {
    if (err) {
        return console.log('something bad happened', err)
    }

    console.log(`server simple is listening on ${port}`)
})
