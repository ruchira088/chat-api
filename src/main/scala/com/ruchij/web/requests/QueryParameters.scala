package com.ruchij.web.requests

import org.http4s.dsl.impl.QueryParamDecoderMatcherWithDefault

object QueryParameters {
  object PageSize extends QueryParamDecoderMatcherWithDefault[Int]("page-size", 10)
  object PageNumber extends QueryParamDecoderMatcherWithDefault[Int]("page-number", 0)
  object SearchTerm extends QueryParamDecoderMatcherWithDefault[String]("search-term", "")
}
