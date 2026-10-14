package com.example.data.model

data class DiscountPromo(
    val id: String,
    val title: String,
    val code: String,
    val discountPercent: Int = 0,
    val fixedDiscount: Long = 0L,
    val minOrder: Long = 0L,
    val description: String = ""
) {
    fun calculateDiscount(subtotal: Long): Long {
        if (subtotal < minOrder || subtotal <= 0L) return 0L
        return if (discountPercent > 0) {
            (subtotal * discountPercent / 100)
        } else {
            minOf(subtotal, fixedDiscount)
        }
    }

    companion object {
        val DEFAULT_PROMOS = listOf(
            DiscountPromo(
                id = "promo_weekend",
                title = "Weekend Care 10%",
                code = "WEEKEND10",
                discountPercent = 10,
                description = "Diskon 10% untuk cuci sepatu di akhir pekan"
            ),
            DiscountPromo(
                id = "promo_gajian",
                title = "Promo Gajian Payday 20%",
                code = "PAYDAY20",
                discountPercent = 20,
                minOrder = 50000L,
                description = "Diskon 20% min. transaksi Rp 50.000"
            ),
            DiscountPromo(
                id = "promo_potongan10",
                title = "Potongan Rp 10.000",
                code = "HEMAT10K",
                fixedDiscount = 10000L,
                minOrder = 40000L,
                description = "Potongan langsung Rp 10.000"
            ),
            DiscountPromo(
                id = "promo_member",
                title = "Voucher Member Setia 15%",
                code = "MEMBER15",
                discountPercent = 15,
                description = "Diskon 15% khusus member setia ADA Care"
            ),
            DiscountPromo(
                id = "promo_potongan15",
                title = "Hemat Cuci 2 Pasang Rp 15.000",
                code = "HEMAT15K",
                fixedDiscount = 15000L,
                minOrder = 70000L,
                description = "Potongan Rp 15.000 untuk transaksi di atas Rp 70.000"
            )
        )
    }
}
