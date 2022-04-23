package com.ruchij.dao.user.models

import org.joda.time.DateTime

case class User(id: String, createdAt: DateTime, email: Email, firstName: String, lastName: String)