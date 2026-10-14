package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.Order
import com.example.data.model.ShopSettings

object WhatsAppHelper {

    fun normalizePhoneNumber(phone: String): String {
        var clean = phone.replace(Regex("[^0-9]"), "")
        if (clean.startsWith("0")) {
            clean = "62" + clean.substring(1)
        } else if (clean.startsWith("8")) {
            clean = "62" + clean
        }
        return clean
    }

    fun buildOrderMessage(order: Order, shopSettings: ShopSettings): String {
        val lines = mutableListOf<String>()
        lines.add("Halo *${order.customerName}*, terima kasih sudah mempercayakan perawatan sepatu Anda ke *${shopSettings.shopName}*.")
        lines.add("")
        lines.add("📋 *NOTA SB-${order.id}*")
        lines.add("Antrian: #${order.queueNumber} (${DateUtils.formatShortDate(order.createdAt)})")
        lines.add("--------------------------------")
        for (item in order.items) {
            val shoeNote = if (item.shoeNotes.isNotBlank()) " (${item.shoeNotes})" else ""
            lines.add("• ${item.serviceName} x${item.quantity}$shoeNote : ${DateUtils.formatCurrency(item.price * item.quantity)}")
        }
        lines.add("--------------------------------")
        if (order.discountAmount > 0) {
            val subtotal = if (order.subtotalAmount > 0) order.subtotalAmount else (order.totalAmount + order.discountAmount)
            lines.add("Subtotal: ${DateUtils.formatCurrency(subtotal)}")
            lines.add("🎁 Diskon (${order.discountName.ifBlank { "Promo" }}): -${DateUtils.formatCurrency(order.discountAmount)}")
        }
        val payStatus = if (order.isPaid) "LUNAS (${order.paymentMethod})" else "BELUM LUNAS"
        lines.add("*TOTAL: ${DateUtils.formatCurrency(order.totalAmount)}* ($payStatus)")
        lines.add("Estimasi selesai: *${DateUtils.formatFullDate(order.estimatedDate)}*")
        if (order.estimatedNote.isNotBlank()) {
            lines.add("Catatan: ${order.estimatedNote}")
        }
        lines.add("")
        lines.add("Status pengerjaan terkini: *${order.currentStatus.title}*")
        lines.add("Kami akan mengabari kembali saat sepatu Anda siap diambil. Terima kasih!")

        return lines.joinToString("\n")
    }

    fun buildPromoBroadcastMessage(
        customerName: String,
        promoTitle: String,
        promoCode: String,
        promoDesc: String,
        shopSettings: ShopSettings
    ): String {
        return """
            Halo Kak *${customerName.ifBlank { "Sobat ADA" }}*! 👋👟
            Ada kabar spesial dari *${shopSettings.shopName}* (*ADA CARE STUDIO*).

            🎉 *${promoTitle.uppercase()}*
            $promoDesc
            Gunakan Kode Promo: *${promoCode}*

            Yuk rawat dan segarkan kembali sepatu kesayanganmu sebelum promo berakhir!
            ${if (shopSettings.shopAddress.isNotBlank()) "📍 Lokasi: " + shopSettings.shopAddress else ""}
            ${if (shopSettings.shopPhone.isNotBlank()) "📱 Info & Reservasi: " + shopSettings.shopPhone else ""}

            Sampai jumpa di ADA Care Studio! ✨
        """.trimIndent()
    }

    fun buildReadyMessage(order: Order, shopSettings: ShopSettings): String {
        val itemsList = order.items.joinToString(", ") { it.serviceName }
        val payNote = if (order.isPaid) "Sudah Lunas" else "Total ${DateUtils.formatCurrency(order.totalAmount)} (Bayar saat pengambilan)"
        return """
            Halo *${order.customerName}*, kabar gembira! 🎉
            Sepatu Anda (*$itemsList*) sudah selesai diproses dan *SIAP DIAMBIL* di *${shopSettings.shopName}*.

            No. Nota: *SB-${order.id}*
            Status Pembayaran: *$payNote*
            ${if (shopSettings.shopAddress.isNotBlank()) "Alamat: " + shopSettings.shopAddress else ""}

            Ditunggu kedatangannya ya! Terima kasih banyak.
        """.trimIndent()
    }

    /**
     * Mengirim pesan nota beserta lampiran gambar nota (JPEG/PNG) langsung ke WhatsApp
     */
    fun shareToWhatsApp(
        context: Context,
        phoneNumber: String,
        message: String,
        imageUri: Uri? = null
    ) {
        val normPhone = normalizePhoneNumber(phoneNumber)
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                if (imageUri != null) {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                putExtra(Intent.EXTRA_TEXT, message)
                if (normPhone.length >= 9) {
                    putExtra("jid", "$normPhone@s.whatsapp.net")
                }
            }

            // Coba target WhatsApp resmi terlebih dahulu
            intent.setPackage("com.whatsapp")
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // Coba WhatsApp Business
                intent.setPackage("com.whatsapp.w4b")
                try {
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    // Fallback ke general chooser
                    intent.setPackage(null)
                    val chooser = Intent.createChooser(intent, "Kirim Nota via WhatsApp")
                    context.startActivity(chooser)
                }
            }
        } catch (err: Exception) {
            // Fallback direct wa.me link jika intent bermasalah
            try {
                val waUrl = if (normPhone.length >= 9) {
                    "https://wa.me/$normPhone?text=" + Uri.encode(message)
                } else {
                    "https://wa.me/?text=" + Uri.encode(message)
                }
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                context.startActivity(browserIntent)
            } catch (e3: Exception) {
                Toast.makeText(context, "Tidak dapat membuka WhatsApp", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
