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
   };

   socket.onmessage = function (event) {
      var msg = JSON.parse(event.data);
      console.log("Json received " + event.data);
      var color;

      switch (msg.type) {
         case "connected":
            color = "blue";
            break;
         case "stateChanged":
            color = "green";
            break;
         case "otherInfo":
            color = "brown";
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

