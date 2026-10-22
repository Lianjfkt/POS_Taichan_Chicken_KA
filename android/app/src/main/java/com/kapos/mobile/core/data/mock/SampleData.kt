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
            description = "Daging ayam fillet empuk gurih dengan perasan jeruk nipis & sambal rawit khas KA.",
            allowSpiceLevel = true,
            allowSambal = true,
            allowComplimentary = true,
            allowToppings = true
        ),
        Product(
            id = "prod_tc_kulit",
            categoryId = "cat_taichan",
            name = "Sate Taichan Kulit Crispy",
            price = 22000.0,
            costPrice = 11000.0,
            emoji = "🍢",
            spiceLevel = "Pedas",
            description = "Kulit ayam renyah di luar kenyal di dalam, dibakar dengan bumbu gurih.",
            allowSpiceLevel = true,
            allowSambal = true,
            allowComplimentary = true,
            allowToppings = true
        ),
        Product(
            id = "prod_tc_campur",
            categoryId = "cat_taichan",
            name = "Sate Taichan Campur (Daging + Kulit)",
            price = 24000.0,
            costPrice = 13000.0,
            emoji = "🍢",
            spiceLevel = "Pedas",
            description = "Kombinasi 5 tusuk daging fillet dan 5 tusuk kulit renyah.",
            allowSpiceLevel = true,
            allowSambal = true,
            allowComplimentary = true,
            allowToppings = true
        ),
        Product(
            id = "prod_tc_paha",
            categoryId = "cat_taichan",
            name = "Sate Taichan Paha Juicy",
            price = 27000.0,
            costPrice = 15000.0,
            emoji = "🍢",
            spiceLevel = "Pedas",
            description = "Daging paha ayam pilihan ekstra juicy berlumur kaldu gurih.",
            allowSpiceLevel = true,
            allowSambal = true,
            allowComplimentary = true,
            allowToppings = true
        ),
        Product(
            id = "prod_ay_geprek",
            categoryId = "cat_chicken",
            name = "Ayam Goreng Crispy Sambal Taichan",
            price = 20000.0,
            costPrice = 11000.0,
            emoji = "🍗",
            spiceLevel = "Ekstra Pedas",
            description = "Potongan ayam goreng tepung renyah disiram sambal taichan pedas segar.",
            allowSpiceLevel = true,
            allowSambal = true,
            allowComplimentary = true,
            allowToppings = true
        ),
        Product(
            id = "prod_ay_bakar",
            categoryId = "cat_chicken",
            name = "Ayam Bakar Madu Gurih",
            price = 22000.0,
            costPrice = 12000.0,
            emoji = "🍗",
            spiceLevel = "Sedang",
            description = "Ayam ungkep bumbu rempah dengan olesan madu bakar aroma asap.",
            allowSpiceLevel = true,
            allowSambal = true,
            allowComplimentary = true,
            allowToppings = true
        ),
        Product(
            id = "prod_nasi",
            categoryId = "cat_sides",
            name = "Nasi Putih Pulen",
            price = 5000.0,
            costPrice = 2500.0,
            emoji = "🍚",
            description = "Nasi hangat ditaburi bawang goreng harum.",
            allowSpiceLevel = false,
            allowSambal = false,
            allowComplimentary = false,
            allowToppings = false
        ),
        Product(
            id = "prod_lontong",
            categoryId = "cat_sides",
            name = "Lontong Daun",
            price = 4000.0,
            costPrice = 2000.0,
            emoji = "🍙",
            description = "Lontong daun lembut pendamping sempurna sate taichan.",
            allowSpiceLevel = false,
            allowSambal = false,
            allowComplimentary = false,
            allowToppings = false
        ),
        Product(
            id = "prod_sambal_xtra",
            categoryId = "cat_sides",
            name = "Ekstra Sambal Taichan & Kaldu",
            price = 3000.0,
            costPrice = 1000.0,
            emoji = "🌶️",
            description = "Cup sambal rawit merah pedas segar + kaldu jamur gurih.",
            allowSpiceLevel = false,
            allowSambal = false,
            allowComplimentary = false,
            allowToppings = false
        ),
        Product(
            id = "prod_teh_manis",
            categoryId = "cat_drinks",
            name = "Es Teh Manis Jumbo",
            price = 5000.0,
            costPrice = 1500.0,
            emoji = "🥤",
            description = "Teh melati wangi dingin segar pelepas dahaga pedas.",
            allowSpiceLevel = false,
            allowSambal = false,
            allowComplimentary = false,
            allowToppings = false
        ),
        Product(
            id = "prod_jeruk_hangat",
            categoryId = "cat_drinks",
            name = "Es Jeruk Murni",
            price = 8000.0,
            costPrice = 3500.0,
            emoji = "🍊",
            description = "Perasan jeruk peras asli dengan es batu segar.",
            allowSpiceLevel = false,
            allowSambal = false,
            allowComplimentary = false,
            allowToppings = false
        ),
        Product(
            id = "prod_mineral",
            categoryId = "cat_drinks",
            name = "Air Mineral Botol 600ml",
            price = 4000.0,
            costPrice = 2200.0,
            emoji = "💧",
            description = "Air mineral dingin/biasa.",
            allowSpiceLevel = false,
            allowSambal = false,
            allowComplimentary = false,
            allowToppings = false
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

    val rawMaterials = listOf(
        com.kapos.mobile.domain.model.RawMaterial(
            id = "mat_cabe",
            name = "Cabe Rawit Merah (Sambal)",
            category = com.kapos.mobile.domain.model.RawMaterialCategory.BUMBU_SAMBAL,
            unit = "gram",
            currentStock = 2500.0,
            minStock = 500.0,
            costPerUnit = 50.0
        ),
        com.kapos.mobile.domain.model.RawMaterial(
            id = "mat_bawang",
            name = "Bawang Putih & Bumbu Kaldu",
            category = com.kapos.mobile.domain.model.RawMaterialCategory.BUMBU_SAMBAL,
            unit = "gram",
            currentStock = 3000.0,
            minStock = 500.0,
            costPerUnit = 35.0
        ),
        com.kapos.mobile.domain.model.RawMaterial(
            id = "mat_jeruk_nipis",
            name = "Jeruk Nipis Segar",
            category = com.kapos.mobile.domain.model.RawMaterialCategory.BUMBU_SAMBAL,
            unit = "buah",
            currentStock = 150.0,
            minStock = 30.0,
            costPerUnit = 1000.0
        ),
        com.kapos.mobile.domain.model.RawMaterial(
            id = "mat_ayam_fillet",
            name = "Daging Ayam Fillet",
            category = com.kapos.mobile.domain.model.RawMaterialCategory.PROTEIN,
            unit = "gram",
            currentStock = 15000.0,
            minStock = 2500.0,
            costPerUnit = 55.0
        ),
        com.kapos.mobile.domain.model.RawMaterial(
            id = "mat_kulit_ayam",
            name = "Kulit Ayam Segar",
            category = com.kapos.mobile.domain.model.RawMaterialCategory.PROTEIN,
            unit = "gram",
            currentStock = 8000.0,
            minStock = 1500.0,
            costPerUnit = 35.0
        ),
        com.kapos.mobile.domain.model.RawMaterial(
            id = "mat_ayam_potong",
            name = "Ayam Potong Segar",
            category = com.kapos.mobile.domain.model.RawMaterialCategory.PROTEIN,
            unit = "potong",
            currentStock = 40.0,
            minStock = 10.0,
            costPerUnit = 9000.0
        ),
        com.kapos.mobile.domain.model.RawMaterial(
            id = "mat_beras",
            name = "Beras Pulen",
            category = com.kapos.mobile.domain.model.RawMaterialCategory.KARBO,
            unit = "gram",
            currentStock = 20000.0,
            minStock = 3000.0,
            costPerUnit = 15.0
        ),
        com.kapos.mobile.domain.model.RawMaterial(
            id = "mat_lontong",
            name = "Lontong Daun",
            category = com.kapos.mobile.domain.model.RawMaterialCategory.KARBO,
            unit = "pcs",
            currentStock = 50.0,
            minStock = 15.0,
            costPerUnit = 1500.0
        ),
        com.kapos.mobile.domain.model.RawMaterial(
            id = "mat_minyak",
            name = "Minyak Goreng",
            category = com.kapos.mobile.domain.model.RawMaterialCategory.BUMBU_SAMBAL,
            unit = "ml",
            currentStock = 10000.0,
            minStock = 2000.0,
            costPerUnit = 18.0
        ),
        com.kapos.mobile.domain.model.RawMaterial(
            id = "mat_tusuk_sate",
            name = "Tusuk Sate Bambu",
            category = com.kapos.mobile.domain.model.RawMaterialCategory.PACKAGING,
            unit = "pcs",
            currentStock = 2500.0,
            minStock = 500.0,
            costPerUnit = 50.0
        ),
        com.kapos.mobile.domain.model.RawMaterial(
            id = "mat_cup_sambal",
            name = "Cup Sambal Mini + Tutup",
            category = com.kapos.mobile.domain.model.RawMaterialCategory.PACKAGING,
            unit = "pcs",
            currentStock = 300.0,
            minStock = 50.0,
            costPerUnit = 300.0
        ),
        com.kapos.mobile.domain.model.RawMaterial(
            id = "mat_teh_jumbo",
            name = "Teh Melati Jumbo",
            category = com.kapos.mobile.domain.model.RawMaterialCategory.MINUMAN,
            unit = "pcs",
            currentStock = 100.0,
            minStock = 20.0,
            costPerUnit = 800.0
        ),
        com.kapos.mobile.domain.model.RawMaterial(
            id = "mat_gula",
            name = "Gula Pasir",
            category = com.kapos.mobile.domain.model.RawMaterialCategory.MINUMAN,
            unit = "gram",
            currentStock = 5000.0,
            minStock = 1000.0,
            costPerUnit = 18.0
        ),
        com.kapos.mobile.domain.model.RawMaterial(
            id = "mat_cup_minuman",
            name = "Cup Plastik 16oz + Sedotan",
            category = com.kapos.mobile.domain.model.RawMaterialCategory.PACKAGING,
            unit = "pcs",
            currentStock = 250.0,
            minStock = 50.0,
            costPerUnit = 400.0
        ),
        com.kapos.mobile.domain.model.RawMaterial(
            id = "mat_air_mineral",
            name = "Air Mineral Botol 600ml",
            category = com.kapos.mobile.domain.model.RawMaterialCategory.MINUMAN,
            unit = "botol",
            currentStock = 48.0,
            minStock = 12.0,
            costPerUnit = 2200.0
        )
    )

    val recipeItems = listOf(
        // Sate Taichan Daging
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r1", "prod_tc_daging", "mat_ayam_fillet", 150.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r2", "prod_tc_daging", "mat_tusuk_sate", 10.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r3", "prod_tc_daging", "mat_cabe", 25.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r4", "prod_tc_daging", "mat_jeruk_nipis", 1.0),

        // Sate Taichan Kulit
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r5", "prod_tc_kulit", "mat_kulit_ayam", 120.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r6", "prod_tc_kulit", "mat_tusuk_sate", 10.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r7", "prod_tc_kulit", "mat_cabe", 25.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r8", "prod_tc_kulit", "mat_jeruk_nipis", 1.0),

        // Sate Taichan Campur
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r9", "prod_tc_campur", "mat_ayam_fillet", 75.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r10", "prod_tc_campur", "mat_kulit_ayam", 60.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r11", "prod_tc_campur", "mat_tusuk_sate", 10.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r12", "prod_tc_campur", "mat_cabe", 25.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r13", "prod_tc_campur", "mat_jeruk_nipis", 1.0),

        // Ayam Goreng Taichan
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r14", "prod_ay_geprek", "mat_ayam_potong", 1.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r15", "prod_ay_geprek", "mat_minyak", 50.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r16", "prod_ay_geprek", "mat_cabe", 30.0),

        // Nasi Putih
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r17", "prod_nasi", "mat_beras", 120.0),

        // Lontong
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r18", "prod_lontong", "mat_lontong", 1.0),

        // Ekstra Sambal
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r19", "prod_sambal_xtra", "mat_cabe", 50.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r20", "prod_sambal_xtra", "mat_jeruk_nipis", 1.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r21", "prod_sambal_xtra", "mat_cup_sambal", 1.0),

        // Es Teh Manis
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r22", "prod_teh_manis", "mat_teh_jumbo", 1.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r23", "prod_teh_manis", "mat_gula", 25.0),
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r24", "prod_teh_manis", "mat_cup_minuman", 1.0),

        // Air Mineral
        com.kapos.mobile.core.data.local.entity.RecipeItemEntity("r25", "prod_mineral", "mat_air_mineral", 1.0)
    )
}
