$(document).ready(function () {
   var wsId = $("#ws_id").attr("value");

   var urlString = "ws://localhost:9000/fsm_client";

   var socket = new WebSocket(urlString);
   console.log("socket creado");
   console.log(urlString);

   socket.onopen = function (event) {
      console.log("Enviando id");

      var msg = {
        type: "connection_request",
        wsId: wsId
      };
      console.log();

      socket.send(JSON.stringify(msg));
      console.log("id enviado");

      setInterval(function () { ping(socket) }, 5000);
   };

   socket.onmessage = function (event) {
      var msg = JSON.parse(event.data);
      console.log("Json received " + event.data);
      var color;

      switch (msg.type) {
         case "fsm_ended":
            color = "red";
            break;
         case "pong":
            console.log("pong");
            return;
         case "connected":
            color = "blue";
            break;
         case "stateChanged":
            color = "green";
            break;
         case "otherInfo":
            color = "LightSeaGreen";
            break;
         default:
            color = "black";
      }

      writeMessage(msg.message, color);
   };
});

function writeMessage(text, color) {
   var message = "<p style=\"color:" + color + "\">" + text + "</p>";

   var feedContent = $("#feed_content");

   var newFeed = "<div class=\"media\">" +
         "<div class=\"media-body\">" +
         "<h4 class=\"media-heading\"> </h4>" +
         "<p>" + message + "</p>" +
         "</div>" +
         "</div>"
   ;

   feedContent.append(newFeed);
}

function ping(socket) {
   var msg = {
      type: "ping"
   };
   console.log("ping");

   socket.send(JSON.stringify(msg));
}



