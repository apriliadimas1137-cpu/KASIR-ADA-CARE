package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.Order
import com.example.data.model.ShopSettings
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

data class SavedReceipt(
    val fileUri: Uri,
    val absolutePath: String,
    val isSavedToGallery: Boolean
)

object ReceiptGenerator {

    /**
     * Menggambar nota kasir profesional ke dalam Android Bitmap beresolusi tinggi (720px width)
     * Menggunakan logo resmi ADA Care Studio dan menampilkan rincian promo/diskon
     */
    fun createReceiptBitmap(
        context: Context,
        order: Order,
        shopSettings: ShopSettings
    ): Bitmap {
        val width = 720
        val itemsCount = order.items.size
        val hasDiscount = order.discountAmount > 0
        // Hitung estimasi tinggi dinamis berdasarkan jumlah item dan diskon
        val estimatedHeight = 1250 + (itemsCount * 90) +
                (if (hasDiscount) 80 else 0) +
                (if (order.notes.isNotBlank()) 80 else 0)
        val bitmap = Bitmap.createBitmap(width, estimatedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Background kertas kasir
        canvas.drawColor(android.graphics.Color.parseColor("#FCFCF8"))

        // Paints
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#1A1D1C")
            typeface = Typeface.MONOSPACE
            textSize = 28f
        }

        val paintBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#1A1D1C")
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 34f
        }

        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#124B36") // Forest Green khas ADA Care
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 38f
            textAlign = Paint.Align.CENTER
        }

        val paintSubtitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#4A6B60")
            typeface = Typeface.MONOSPACE
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }

        val paintMuted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#666666")
            typeface = Typeface.MONOSPACE
            textSize = 24f
        }

        val paintDiscount = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#1B8A57")
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 28f
        }

        val paintDashed = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#B0B8B8")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        }

        var y = 45f

        // 2. Render Logo Resmi ADA Care Studio
        var drawnLogo = false
        try {
            val logoSrc = BitmapFactory.decodeResource(context.resources, R.drawable.logo_ada_care)
            if (logoSrc != null) {
                val targetLogoWidth = 240
                val targetLogoHeight = (logoSrc.height * targetLogoWidth) / logoSrc.width
                val scaledLogo = Bitmap.createScaledBitmap(logoSrc, targetLogoWidth, targetLogoHeight, true)
                val logoX = (width - targetLogoWidth) / 2f
                canvas.drawBitmap(scaledLogo, logoX, y, null)
                y += targetLogoHeight + 20f
                drawnLogo = true
            }
        } catch (e: Exception) {
            drawnLogo = false
        }

        if (!drawnLogo) {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#124B36")
                style = Paint.Style.FILL
            }
            val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 36f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawRoundRect(width / 2f - 45f, y, width / 2f + 45f, y + 60f, 16f, 16f, badgePaint)
            canvas.drawText("ADA", width / 2f, y + 43f, badgeTextPaint)
            y += 85f
        }

        // Nama Brand & Tagline
        canvas.drawText("ADA CARE STUDIO", width / 2f, y, paintTitle)
        y += 38f
        canvas.drawText("SHOES CARE & RESTORATION", width / 2f, y, paintSubtitle)
        y += 35f

        if (shopSettings.shopAddress.isNotBlank()) {
            canvas.drawText(shopSettings.shopAddress, width / 2f, y, paintSubtitle)
            y += 32f
        }
        if (shopSettings.shopPhone.isNotBlank()) {
            canvas.drawText("WhatsApp: ${shopSettings.shopPhone}", width / 2f, y, paintSubtitle)
            y += 32f
        }

        y += 12f
        fun drawDashedLine() {
            canvas.drawLine(40f, y, width - 40f, y, paintDashed)
            y += 38f
        }

        drawDashedLine()

        // 3. Info Nota & Antrian
        fun drawKeyValue(key: String, value: String, isBold: Boolean = false) {
            val p = if (isBold) paintBold else paintText
            canvas.drawText(key, 40f, y, paintText)
            val valWidth = p.measureText(value)
            canvas.drawText(value, width - 40f - valWidth, y, p)
            y += 40f
        }

        drawKeyValue("No. Nota", "SB-${order.id}", isBold = true)
        drawKeyValue("Tanggal", DateUtils.formatDateTime(order.createdAt))
        drawKeyValue("Antrian", "#${order.queueNumber} (${DateUtils.formatShortDate(order.createdAt)})")
        drawKeyValue("Pelanggan", order.customerName, isBold = true)
        if (order.customerPhone.isNotBlank()) {
            drawKeyValue("No. HP", order.customerPhone)
        }

        drawDashedLine()

        // 4. Daftar Layanan & Sepatu
        paintBold.textSize = 28f
        canvas.drawText("RINCIAN CUCIAN", 40f, y, paintBold)
        y += 38f

        for (item in order.items) {
            val itemLine = "${item.serviceName} x${item.quantity}"
            val priceStr = DateUtils.formatCurrency(item.price * item.quantity)
            canvas.drawText(itemLine, 40f, y, paintBold)
            val pWidth = paintBold.measureText(priceStr)
            canvas.drawText(priceStr, width - 40f - pWidth, y, paintBold)
            y += 34f

            if (item.shoeNotes.isNotBlank()) {
                canvas.drawText("• Sepatu: ${item.shoeNotes}", 60f, y, paintMuted)
                y += 34f
            }
        }

        if (order.notes.isNotBlank()) {
            y += 8f
            canvas.drawText("Catatan: ${order.notes}", 40f, y, paintMuted)
            y += 34f
        }

        drawDashedLine()

        // 5. Rincian Biaya, Diskon/Promo & Total
        val rawSubtotal = if (order.subtotalAmount > 0L) order.subtotalAmount else (order.totalAmount + order.discountAmount)

        if (hasDiscount) {
            // Subtotal
            val subtotalStr = DateUtils.formatCurrency(rawSubtotal)
            canvas.drawText("Subtotal", 40f, y, paintText)
            val subWidth = paintText.measureText(subtotalStr)
            canvas.drawText(subtotalStr, width - 40f - subWidth, y, paintText)
            y += 38f

            // Diskon / Promo
            val promoLabel = "Diskon (${order.discountName.ifBlank { "Promo" }})"
            val discountStr = "-${DateUtils.formatCurrency(order.discountAmount)}"
            canvas.drawText(promoLabel, 40f, y, paintDiscount)
            val discWidth = paintDiscount.measureText(discountStr)
            canvas.drawText(discountStr, width - 40f - discWidth, y, paintDiscount)
            y += 42f
        }

        // Total Akhir
        val totalStr = DateUtils.formatCurrency(order.totalAmount)
        paintBold.textSize = 38f
        canvas.drawText("TOTAL BIAYA", 40f, y + 6f, paintBold)
        val totWidth = paintBold.measureText(totalStr)
        canvas.drawText(totalStr, width - 40f - totWidth, y + 6f, paintBold)
        y += 56f

        val payStatus = if (order.isPaid) "LUNAS (${order.paymentMethod})" else "BELUM DIBAYAR"
        val payColor = if (order.isPaid) android.graphics.Color.parseColor("#1B8A57") else android.graphics.Color.parseColor("#C13A2D")
        val payPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = payColor
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 28f
        }
        canvas.drawText("Pembayaran", 40f, y, paintText)
        val payWidth = payPaint.measureText(payStatus)
        canvas.drawText(payStatus, width - 40f - payWidth, y, payPaint)
        y += 48f

        drawDashedLine()

        // 6. Estimasi Selesai & Status
        paintBold.textSize = 26f
        canvas.drawText("Estimasi Selesai:", 40f, y, paintText)
        y += 36f
        val estDateStr = DateUtils.formatFullDate(order.estimatedDate)
        paintBold.textSize = 30f
        canvas.drawText(estDateStr, 40f, y, paintBold)
        y += 38f

        if (order.estimatedNote.isNotBlank()) {
            canvas.drawText(order.estimatedNote, 40f, y, paintMuted)
            y += 34f
        }

        val statusText = "Status: ${order.currentStatus.title}"
        canvas.drawText(statusText, 40f, y, paintBold)
        y += 44f

        drawDashedLine()

        // 7. QR Code Pelacakan
        val qrSize = 160
        val qrBitmap = QrisHelper.generateQrBitmap("SB-${order.id}-${order.customerName}", qrSize)
        val qrX = (width - qrSize) / 2f
        canvas.drawBitmap(qrBitmap, qrX, y, null)
        y += qrSize + 22f

        // Footer Thank You
        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#4A6B60")
            typeface = Typeface.MONOSPACE
            textSize = 22f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Simpan nota ini sebagai bukti pengambilan sepatu.", width / 2f, y, footPaint)
        y += 28f
        canvas.drawText("Terima kasih telah memilih ADA Care Studio!", width / 2f, y, footPaint)

        // Crop bitmap to actual final y + padding
        val finalHeight = (y + 45f).toInt()
        return if (finalHeight in 100 until estimatedHeight) {
            Bitmap.createBitmap(bitmap, 0, 0, width, finalHeight)
        } else {
            bitmap
        }
    }

    /**
     * Simpan otomatis nota JPEG / PNG ke folder lokal perangkat (Pictures/KasirCuciSepatu)
     * dan cache aplikasi untuk sharing via FileProvider.
     */
    fun saveReceiptToDevice(
        context: Context,
        bitmap: Bitmap,
        orderId: String,
        isJpeg: Boolean = false
    ): SavedReceipt {
        val extension = if (isJpeg) "jpg" else "png"
        val mimeType = if (isJpeg) "image/jpeg" else "image/png"
        val compressFormat = if (isJpeg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
        val fileName = "Nota_ADACare_${orderId}_${System.currentTimeMillis()}.$extension"

        // 1. Simpan salinan ke direktori cache lokal untuk FileProvider
        val cacheDir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val cacheFile = File(cacheDir, fileName)
        FileOutputStream(cacheFile).use { out ->
            bitmap.compress(compressFormat, 100, out)
        }
        val fileUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )

        var publicPath = cacheFile.absolutePath
        var savedToGallery = false

        // 2. Simpan ke MediaStore / Folder Publik Perangkat (Pictures/KasirCuciSepatu)
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/KasirCuciSepatu")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (imageUri != null) {
                resolver.openOutputStream(imageUri)?.use { outStream: OutputStream ->
                    bitmap.compress(compressFormat, 100, outStream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
                savedToGallery = true
                publicPath = "Pictures/KasirCuciSepatu/$fileName"
            }
        } catch (e: Exception) {
            // MediaStore fallback
        }

        return SavedReceipt(
            fileUri = fileUri,
            absolutePath = publicPath,
            isSavedToGallery = savedToGallery
        )
    }
}
