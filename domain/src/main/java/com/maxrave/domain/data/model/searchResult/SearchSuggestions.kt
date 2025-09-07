package com.maxrave.domain.data.model.searchResult

import com.maxrave.domain.data.type.SearchResultType

data class SearchSuggestions(
    val queries: List<String>,
    val recommendedItems: List<SearchResultType>,
)