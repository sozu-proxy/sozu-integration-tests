const WebSocketServer = require('ws').Server;
const port = process.env.PORT || 8080


const ws = new WebSocketServer({
    port: port,
});

ws.on('open', function open() {
    console.log("client opened");
    ws.send('opened');
});

ws.on('message', function(data, flags) {
    console.log("message: "+message);
    ws.send(data);
});

ws.on('connection', function connection(c) {
    console.log("got connection");
    c.on('message', function message(data) {
        console.log("message: "+data);
        c.send(data);
    });
});

console.log(`listening on ${port}`)