package com.ruchij.web.requests

import com.ruchij.services.messages.models.Message

case class UserPushMessageRequest(receiverId: String, message: Message)
