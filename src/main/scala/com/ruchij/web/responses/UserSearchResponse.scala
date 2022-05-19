package com.ruchij.web.responses

import com.ruchij.dao.user.models.User

case class UserSearchResponse(searchTerm: String, pageNumber: Int, pageSize: Int, users: Seq[User])
