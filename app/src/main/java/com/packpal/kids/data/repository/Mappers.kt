package com.packpal.kids.data.repository

import com.packpal.kids.data.local.ItemEntity
import com.packpal.kids.data.local.TemplateWithCount
import com.packpal.kids.data.local.VerificationItemEntity
import com.packpal.kids.domain.Category
import com.packpal.kids.domain.Item
import com.packpal.kids.domain.TemplateSummary
import com.packpal.kids.domain.VerificationState
import com.packpal.kids.domain.VerifyEntry

fun ItemEntity.toDomain() = Item(id, templateId, name, note, Category.fromKey(category), iconKey, displayOrder)

fun TemplateWithCount.toDomain() = TemplateSummary(id, name, displayOrder, itemCount)

fun VerificationItemEntity.toDomain() = VerifyEntry(
    sourceItemId = sourceItemId,
    name = itemNameSnapshot,
    category = Category.fromKey(categorySnapshot),
    iconKey = iconKeySnapshot,
    state = VerificationState.fromKey(verificationState),
    inCurrentRound = inCurrentRound,
    order = displayOrder,
)

fun VerifyEntry.toEntity(attemptId: Long) = VerificationItemEntity(
    attemptId = attemptId,
    sourceItemId = sourceItemId,
    itemNameSnapshot = name,
    categorySnapshot = category.name,
    iconKeySnapshot = iconKey,
    verificationState = state.name,
    displayOrder = order,
    inCurrentRound = inCurrentRound,
)
