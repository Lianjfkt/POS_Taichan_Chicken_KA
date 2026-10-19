package com.kapos.mobile.core.data.mock

import com.kapos.mobile.domain.model.Category
import com.kapos.mobile.domain.model.Product

object SampleData {
    val categories = listOf(
        Category(id = "cat_taichan", name = "Sate Taichan", emoji = "🍢", color = "#0B5C73", sortOrder = 1),
        Category(id = "cat_chicken", name = "Ayam Goreng", emoji = "🍗", color = "#E8A33D", sortOrder = 2),
        Category(id = "cat_drinks", name = "Minuman", emoji = "🥤", color = "#3C7A8C", sortOrder = 3),
        Category(id = "cat_sides", name = "Nasi & Ekstra", emoji = "🍚", color = "#2E9E6B", sortOrder = 4)
    )

    val products = listOf(
        Product(
            id = "prod_tc_daging",
            categoryId = "cat_taichan",
            name = "Sate Taichan Daging (10 Tsk)",
            price = 25000.0,
            costPrice = 14000.0,
            emoji = "🍢",
            spiceLevel = "Pedas",
            description = "Daging ayam fillet empuk gurih dengan perasan jeruk nipis & sambal rawit khas KA."
        ),
        Product(
            id = "prod_tc_kulit",
            categoryId = "cat_taichan",
            name = "Sate Taichan Kulit Crispy",
            price = 22000.0,
            costPrice = 11000.0,
            emoji = "🍢",
            spiceLevel = "Pedas",
            description = "Kulit ayam renyah di luar kenyal di dalam, dibakar dengan bumbu gurih."
        ),
        Product(
            id = "prod_tc_campur",
            categoryId = "cat_taichan",
            name = "Sate Taichan Campur (Daging + Kulit)",
            price = 24000.0,
            costPrice = 13000.0,
            emoji = "🍢",
            spiceLevel = "Pedas",
            description = "Kombinasi 5 tusuk daging fillet dan 5 tusuk kulit renyah."
        ),
        Product(
            id = "prod_tc_paha",
            categoryId = "cat_taichan",
            name = "Sate Taichan Paha Juicy",
            price = 27000.0,
            costPrice = 15000.0,
            emoji = "🍢",
            spiceLevel = "Pedas",
            description = "Daging paha ayam pilihan ekstra juicy berlumur kaldu gurih."
        ),
        Product(
            id = "prod_ay_geprek",
            categoryId = "cat_chicken",
            name = "Ayam Goreng Crispy Sambal Taichan",
            price = 20000.0,
            costPrice = 11000.0,
            emoji = "🍗",
            spiceLevel = "Ekstra Pedas",
            description = "Potongan ayam goreng tepung renyah disiram sambal taichan pedas segar."
        ),
        Product(
            id = "prod_ay_bakar",
            categoryId = "cat_chicken",
            name = "Ayam Bakar Madu Gurih",
            price = 22000.0,
            costPrice = 12000.0,
            emoji = "🍗",
            spiceLevel = "Sedang",
            description = "Ayam ungkep bumbu rempah dengan olesan madu bakar aroma asap."
        ),
        Product(
            id = "prod_nasi",
            categoryId = "cat_sides",
            name = "Nasi Putih Pulen",
            price = 5000.0,
            costPrice = 2500.0,
            emoji = "🍚",
            description = "Nasi hangat ditaburi bawang goreng harum."
        ),
        Product(
            id = "prod_lontong",
            categoryId = "cat_sides",
            name = "Lontong Daun",
            price = 4000.0,
            costPrice = 2000.0,
            emoji = "🍙",
            description = "Lontong daun lembut pendamping sempurna sate taichan."
        ),
        Product(
            id = "prod_sambal_xtra",
            categoryId = "cat_sides",
            name = "Ekstra Sambal Taichan & Kaldu",
            price = 3000.0,
            costPrice = 1000.0,
            emoji = "🌶️",
            description = "Cup sambal rawit merah pedas segar + kaldu jamur gurih."
        ),
        Product(
            id = "prod_teh_manis",
            categoryId = "cat_drinks",
            name = "Es Teh Manis Jumbo",
            price = 5000.0,
            costPrice = 1500.0,
            emoji = "🥤",
            description = "Teh melati wangi dingin segar pelepas dahaga pedas."
        ),
        Product(
            id = "prod_jeruk_hangat",
            categoryId = "cat_drinks",
            name = "Es Jeruk Murni",
            price = 8000.0,
            costPrice = 3500.0,
            emoji = "🍊",
            description = "Perasan jeruk peras asli dengan es batu segar."
        ),
        Product(
            id = "prod_mineral",
            categoryId = "cat_drinks",
            name = "Air Mineral Botol 600ml",
            price = 4000.0,
            costPrice = 2200.0,
            emoji = "💧",
            description = "Air mineral dingin/biasa."
        )
    )

    val customers = listOf(
        com.kapos.mobile.domain.model.Customer(
            id = "cust_budi",
            name = "Budi Santoso",
            phone = "081234567890",
            points = 120,
            totalSpend = 450000.0,
            orderCount = 6
        ),
        com.kapos.mobile.domain.model.Customer(
            id = "cust_siti",
            name = "Siti Aminah",
            phone = "081987654321",
            points = 250,
            totalSpend = 920000.0,
            orderCount = 12
        ),
        com.kapos.mobile.domain.model.Customer(
            id = "cust_ahmad",
            name = "Ahmad Fauzi",
            phone = "085678901234",
            points = 60,
            totalSpend = 210000.0,
            orderCount = 3
        ),
        com.kapos.mobile.domain.model.Customer(
            id = "cust_dewi",
            name = "Dewi Lestari",
            phone = "087712345678",
            points = 350,
            totalSpend = 1450000.0,
            orderCount = 18
        )
    )
}
