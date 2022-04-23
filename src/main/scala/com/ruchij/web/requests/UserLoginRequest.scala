package com.ruchij.web.requests

import com.ruchij.dao.user.models.Email
import com.ruchij.services.authentication.models.Password

case class UserLoginRequest(email: Email, password: Password)
