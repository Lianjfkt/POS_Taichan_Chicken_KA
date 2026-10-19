package com.kapos.mobile.core.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.kapos.mobile.domain.model.Order
import com.kapos.mobile.domain.model.OrderItem
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun exportOrdersToCsv(
        context: Context,
        periodName: String,
        orders: List<Order>,
        orderItems: List<OrderItem>
    ): File {
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val timestamp = fileTimestampFormat.format(Date())
        val fileName = "Laporan_Transaksi_${periodName.replace(" ", "_")}_$timestamp.csv"
        val file = File(reportsDir, fileName)

        FileWriter(file).use { writer ->
            // Header Info
            writer.append("LAPORAN TRANSAKSI PENJUALAN TAICHAN & CHICKEN KA\n")
            writer.append("Periode: ,$periodName\n")
            writer.append("Waktu Ekspor: ,${dateFormat.format(Date())}\n")
            writer.append("Total Transaksi: ,${orders.size}\n\n")

            // Transactions Table
            writer.append("No Order,Tanggal & Waktu,Kasir,Pelanggan,No HP,Tipe Pesanan,No Meja,Metode Bayar,Status Bayar,Subtotal,Diskon,Total,Alasan Void\n")

            for (order in orders) {
                val dateStr = dateFormat.format(Date(order.createdAt))
                val customerName = (order.customerName ?: "-").replace(",", " ")
                val phone = (order.customerPhone ?: "-").replace(",", " ")
                val table = order.tableNo ?: "-"
                val voidReason = (order.voidReason ?: "").replace(",", " ")

                writer.append("${order.orderNo},")
                writer.append("$dateStr,")
                writer.append("${order.cashierName.replace(",", " ")},")
                writer.append("$customerName,")
                writer.append("$phone,")
                writer.append("${order.orderType.name},")
                writer.append("$table,")
                writer.append("${order.paymentMethod.name},")
                writer.append("${order.paymentStatus.name},")
                writer.append("${order.subtotal.toLong()},")
                writer.append("${order.discount.toLong()},")
                writer.append("${order.total.toLong()},")
                writer.append("$voidReason\n")
            }

            // Summary Table by Menu Items
            if (orderItems.isNotEmpty()) {
                writer.append("\n\nRINCIAN PENJUALAN PER MENU\n")
                writer.append("Nama Produk,Jumlah Terjual (Porsi),Harga Satuan,Total Penjualan\n")

                val groupedItems = orderItems.groupBy { it.productName }
                for ((productName, items) in groupedItems) {
                    val totalQty = items.sumOf { it.quantity }
                    val samplePrice = items.firstOrNull()?.price ?: 0.0
                    val totalSales = items.sumOf { it.subtotal }

                    writer.append("${productName.replace(",", " ")},")
                    writer.append("$totalQty,")
                    writer.append("${samplePrice.toLong()},")
                    writer.append("${totalSales.toLong()}\n")
                }
            }
        }

        return file
    }

    fun exportFinancialSummaryToCsv(
        context: Context,
        periodName: String,
        cashierName: String,
        grossRevenue: Double,
        discount: Double,
        netRevenue: Double,
        cogs: Double,
        grossProfit: Double,
        expenses: Double,
        netProfit: Double,
        totalTransactions: Int,
        cashSales: Double,
        nonCashSales: Double
    ): File {
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }

        val timestamp = fileTimestampFormat.format(Date())
        val fileName = "Laba_Rugi_${periodName.replace(" ", "_")}_$timestamp.csv"
        val file = File(reportsDir, fileName)

        FileWriter(file).use { writer ->
            writer.append("LAPORAN LABA RUGI & FINANSIAL TAICHAN & CHICKEN KA\n")
            writer.append("Periode: ,$periodName\n")
            writer.append("Petugas / Shift: ,$cashierName\n")
            writer.append("Waktu Ekspor: ,${dateFormat.format(Date())}\n\n")

            writer.append("KOMPONEN FINANSIAL,NOMINAL (RP)\n")
            writer.append("Penjualan Kotor (Gross Revenue),${grossRevenue.toLong()}\n")
            writer.append("Potongan Diskon,${discount.toLong()}\n")
            writer.append("Penjualan Bersih (Net Revenue),${netRevenue.toLong()}\n")
            writer.append("Harga Pokok Penjualan (HPP Modal),${cogs.toLong()}\n")
            writer.append("Laba Kotor (Gross Profit),${grossProfit.toLong()}\n")
            writer.append("Pengeluaran Kas Keluar (Operasional),${expenses.toLong()}\n")
            writer.append("Laba Bersih (Net Profit),${netProfit.toLong()}\n\n")

            writer.append("METODE PEMBAYARAN,NOMINAL (RP)\n")
            writer.append("Tunai (Cash / Laci),${cashSales.toLong()}\n")
            writer.append("Non-Tunai (QRIS & Transfer),${nonCashSales.toLong()}\n\n")

            writer.append("STATISTIK OPERASIONAL,NILAI\n")
            writer.append("Total Transaksi Lunas,${totalTransactions} Struk\n")
            val aov = if (totalTransactions > 0) (netRevenue / totalTransactions).toLong() else 0L
            writer.append("Rata-rata Order (Basket Size),Rp $aov\n")
        }

        return file
    }

    fun shareCsvFile(context: Context, file: File, chooserTitle: String = "Bagikan Laporan CSV") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
