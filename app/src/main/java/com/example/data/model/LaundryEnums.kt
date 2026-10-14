package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class LaundryStatus(val key: String, val title: String, val stepIndex: Int) {
    DITERIMA("diterima", "Diterima", 0),
    DICUCI("dicuci", "Dicuci", 1),
    DIKERINGKAN("dikeringkan", "Dikeringkan", 2),
    FINISHING("finishing", "Finishing", 3),
    SIAP("siap", "Siap Diambil", 4),
    DIAMBIL("diambil", "Sudah Diambil", 5);

    fun next(): LaundryStatus? = when (this) {
        DITERIMA -> DICUCI
        DICUCI -> DIKERINGKAN
        DIKERINGKAN -> FINISHING
        FINISHING -> SIAP
        SIAP -> DIAMBIL
        DIAMBIL -> null
    }

    val isQueue: Boolean
        get() = this in listOf(DITERIMA, DICUCI, DIKERINGKAN, FINISHING)

    val color: Color
        get() = when (this) {
            DITERIMA -> StatusAccepted
            DICUCI -> StatusWashed
            DIKERINGKAN -> StatusDried
            FINISHING -> StatusFinishing
            SIAP -> StatusReady
            DIAMBIL -> StatusPickedUp
        }

    companion object {
        fun fromKey(key: String): LaundryStatus {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: DITERIMA
        }
    }
}

enum class PaymentMethod(val id: String, val displayName: String, val category: String) {
    CASH("TUNAI", "Tunai / Cash", "TUNAI"),
    QRIS("QRIS", "QRIS Dinamis", "DIGITAL"),
    GOPAY("GOPAY", "GoPay", "E-WALLET"),
    OVO("OVO", "OVO", "E-WALLET"),
    DANA("DANA", "DANA", "E-WALLET"),
    SHOPEEPAY("SHOPEEPAY", "ShopeePay", "E-WALLET"),
    TRANSFER_BCA("TRANSFER_BCA", "Transfer BCA", "BANK"),
    TRANSFER_MANDIRI("TRANSFER_MANDIRI", "Transfer Mandiri", "BANK"),
    TRANSFER_BRI("TRANSFER_BRI", "Transfer BRI", "BANK");

    companion object {
        fun fromId(id: String): PaymentMethod {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: CASH
        }
    }
}
