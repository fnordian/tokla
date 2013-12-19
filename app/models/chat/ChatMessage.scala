package models.chat

class ChatMessage(val sender : String, val message : String) {

  var timeStamp: Long = System.currentTimeMillis / 1000

}
