$(document).ready(function () {
   var wsId = $('#ws_id').attr('value');

   var urlString = 'ws://localhost:9000/fsm_client';

   var socket = new WebSocket(urlString);

   console.log(urlString);

   socket.onopen = function (event) {
      socket.send(wsId);
   };

   socket.onmessage = function (event) {
      var textArea = $('#message_area');
      textArea.text(textArea.text() + event.data + '<br/>');
   };
});

