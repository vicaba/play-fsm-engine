function copyToClipboard() {
   var text = document.getElementById("clip_id");

   text.focus();
   text.select();

   document.execCommand("copy");
}