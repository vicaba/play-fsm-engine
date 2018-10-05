var socket = new WebSocket("ws://www.example.com/socketserver");

var urlString = window.location.href;
var url = new URL(url_string);

var actorId = url.getParameter("actorId");


socket.onopen = function (event) {
  socket.send(actorId);
};