package com.example.settled.data.remote

import com.example.settled.data.local.CardEntity
import com.example.settled.data.local.PaymentLogEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDataSource @Inject constructor() {

    private val db = FirebaseFirestore.getInstance()

    private fun cardsRef(uid: String) = db.collection("users").document(uid).collection("cards")
    private fun logsRef(uid: String)  = db.collection("users").document(uid).collection("payment_logs")

    suspend fun syncCard(uid: String, entity: CardEntity) {
        cardsRef(uid).document(entity.id).set(entity.toMap(), SetOptions.merge()).await()
    }

    suspend fun deleteCard(uid: String, cardId: String) {
        cardsRef(uid).document(cardId).update("isDeleted", true).await()
    }

    suspend fun syncPaymentLog(uid: String, log: PaymentLogEntity) {
        logsRef(uid).document(log.id).set(log.toMap(), SetOptions.merge()).await()
    }

    suspend fun deleteLogForCycle(uid: String, cardId: String, month: Int, year: Int) {
        val query = logsRef(uid)
            .whereEqualTo("cardId", cardId)
            .whereEqualTo("cycleMonth", month)
            .whereEqualTo("cycleYear", year)
            .get().await()
        val batch = db.batch()
        query.documents.forEach { batch.delete(it.reference) }
        if (!query.isEmpty) batch.commit().await()
    }

    suspend fun deleteAllUserData(uid: String) {
        val batch = db.batch()
        cardsRef(uid).get().await().documents.forEach { batch.delete(it.reference) }
        logsRef(uid).get().await().documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    suspend fun fetchAllCards(uid: String): List<CardEntity> {
        return cardsRef(uid).get().await().documents.mapNotNull { doc ->
            runCatching {
                CardEntity(
                    id            = doc.id,
                    bankName      = doc.getString("bankName") ?: return@mapNotNull null,
                    cardName      = doc.getString("cardName") ?: return@mapNotNull null,
                    lastFourDigits = doc.getString("lastFourDigits") ?: "",
                    statementDay  = (doc.getLong("statementDay") ?: return@mapNotNull null).toInt(),
                    dueDay        = (doc.getLong("dueDay") ?: return@mapNotNull null).toInt(),
                    createdAt     = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    isDeleted     = doc.getBoolean("isDeleted") ?: false
                )
            }.getOrNull()
        }
    }

    suspend fun fetchAllLogs(uid: String): List<PaymentLogEntity> {
        return logsRef(uid).get().await().documents.mapNotNull { doc ->
            runCatching {
                PaymentLogEntity(
                    id         = doc.id,
                    cardId     = doc.getString("cardId") ?: return@mapNotNull null,
                    type       = doc.getString("type") ?: return@mapNotNull null,
                    platform   = doc.getString("platform") ?: return@mapNotNull null,
                    timestamp  = doc.getLong("timestamp") ?: return@mapNotNull null,
                    cycleMonth = (doc.getLong("cycleMonth") ?: return@mapNotNull null).toInt(),
                    cycleYear  = (doc.getLong("cycleYear") ?: return@mapNotNull null).toInt()
                )
            }.getOrNull()
        }
    }

    private fun CardEntity.toMap() = mapOf(
        "bankName"       to bankName,
        "cardName"       to cardName,
        "lastFourDigits" to lastFourDigits,
        "statementDay"   to statementDay,
        "dueDay"         to dueDay,
        "createdAt"      to createdAt,
        "isDeleted"      to isDeleted
    )

    private fun PaymentLogEntity.toMap() = mapOf(
        "cardId"     to cardId,
        "type"       to type,
        "platform"   to platform,
        "timestamp"  to timestamp,
        "cycleMonth" to cycleMonth,
        "cycleYear"  to cycleYear
    )
}
