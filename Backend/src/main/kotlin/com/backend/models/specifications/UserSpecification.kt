package com.backend.models.specifications

import com.backend.models.entities.User
import com.backend.models.enums.AccountStatus
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification

object UserSpecification {

    private const val LIKE_ESCAPE = '\\'

    fun withFilters(username: String?, accountStatus: AccountStatus?): Specification<User> {
        return Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>()

            username?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }?.let { text ->
                predicates.add(cb.like(root.get("username"), "%${escapeLike(text)}%", LIKE_ESCAPE))
            }

            if(accountStatus != null) {
                predicates.add(cb.equal(root.get<AccountStatus>("accountStatus"), accountStatus))
            }

            cb.and(*predicates.toTypedArray())
        }
    }

    private fun escapeLike(text: String): String = text
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
}