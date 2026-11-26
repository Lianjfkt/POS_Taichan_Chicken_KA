# Customizable Menu Modifiers CRUD & Product Association Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Membangun fitur CRUD dinamis untuk modifier menu (Level Pedas, Pilihan Sambal, Pelengkap & Alat Makan, Ekstra Topping & Kemasan), menghubungkan pilihan modifier per produk di Manajemen Menu, dan menyederhanakan alur kasir (1-tap langsung masuk keranjang untuk menu tanpa modifier, serta dialog kustomisasi dinamis yang ringkas untuk menu dengan modifier).

**Architecture:** Menggunakan Room Database dengan entitas `ModifierEntity` dan `ModifierDao`, memperluas `ProductEntity` & `Product` dengan boolean flags (`allowSpiceLevel`, `allowSambal`, `allowComplimentary`, `allowToppings`), memperluas `MenuManagementScreen` dengan Tab `Varian & Modifier` dan form edit produk baru, serta merefaktor `ModifierDialog` & `PosViewModel` agar menyaring opsi secara dinamis tanpa data hardcoded.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Room Database (SQLite), StateFlow / ViewModel, Kotlin Coroutines, JUnit4.

## Global Constraints
- Target platform: Android Native (Min SDK 24, Target SDK 34).
- Database migrations menggunakan Room destructive migration fallback atau skema Room versi 6.
- Preservasi data: Seed awal preset modifier saat tabel masih kosong agar modifier existing tetap tersedia.
- Supabase synchronization: Kolom baru di `products` dan tabel baru `modifiers` diselaraskan di `supabase_schema.sql` dan `SupabaseProductDto`.

---

### Task 1: Room DB Entity, DAO, Repository & Model Updates

**Files:**
- Create: `app/src/main/java/com/kapos/mobile/core/data/local/entity/ModifierEntity.kt`
- Create: `app/src/main/java/com/kapos/mobile/core/data/local/dao/ModifierDao.kt`
- Modify: `app/src/main/java/com/kapos/mobile/domain/model/ModifierModels.kt`
- Modify: `app/src/main/java/com/kapos/mobile/domain/model/Models.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/local/entity/Entities.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/repository/PosRepository.kt`
- Modify: `app/src/main/java/com/kapos/mobile/core/data/remote/dto/SupabaseDto.kt`
- Modify: `docs/supabase_schema.sql`
- Test: `app/src/test/java/com/kapos/mobile/core/data/local/ModifierDaoTest.kt`

**Interfaces:**
- Consumes: Room Database architecture, `ModifierCategory`, `Product` model.
- Produces: `ModifierDao` with CRUD operations, `PosRepository.observeAllModifiers()`, `PosRepository.saveModifier()`, `PosRepository.deleteModifier()`, updated `Product` model with `hasModifiers()`, `allowSpiceLevel`, `allowSambal`, `allowComplimentary`, `allowToppings`.

- [ ] **Step 1: Write failing unit test for Modifier data mapping and Product modifier helper**
```kotlin
package com.kapos.mobile.core.data.local

import com.kapos.mobile.domain.model.ModifierCategory
import com.kapos.mobile.domain.model.ModifierOption
import com.kapos.mobile.domain.model.Product
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModifierModelTest {
    @Test
    fun testProductHasModifiersHelper() {
        val drink = Product(
            id = "p1", categoryId = "cat_drinks", name = "Es Teh", price = 5000.0,
            allowSpiceLevel = false, allowSambal = false, allowComplimentary = false, allowToppings = false
        )
        assertFalse(drink.hasModifiers)

        val food = Product(
            id = "p2", categoryId = "cat_taichan", name = "Sate", price = 25000.0,
            allowSpiceLevel = true, allowSambal = true, allowComplimentary = false, allowToppings = true
        )
        assertTrue(food.hasModifiers)
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**
Run `./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.local.ModifierModelTest`

- [ ] **Step 3: Implement ModifierEntity, ModifierDao, and update Models & AppDatabase**
- Update `ModifierModels.kt` to include `SPICE_LEVEL` in `ModifierCategory` and add `isActive: Boolean`, `sortOrder: Int` to `ModifierOption`.
- Create `ModifierEntity.kt` and `ModifierDao.kt` (methods: `observeAll()`, `observeActive()`, `getAll()`, `upsert()`, `upsertAll()`, `deleteById()`, `toggleActive()`).
- Add `ModifierEntity` to `AppDatabase.kt` and bump database version to 6.
- Update `ProductEntity` & `Product` with `allowSpiceLevel`, `allowSambal`, `allowComplimentary`, `allowToppings`, and computed property `hasModifiers`.
- Update `PosRepository` to seed default modifiers if table is empty (seeding Lv.0–Lv.5, Sambal, Complimentary, and Extra toppings), and provide Flow/methods for modifier CRUD.
- Update `SupabaseProductDto` and `supabase_schema.sql` with new modifier fields.

- [ ] **Step 4: Run unit tests to confirm they pass**
Run `./gradlew testDebugUnitTest --tests com.kapos.mobile.core.data.local.ModifierModelTest`

- [ ] **Step 5: Commit changes**
`git add . && git commit -m "feat: add ModifierEntity, ModifierDao, and product modifier flags"`

---

### Task 2: Modifier Management UI (Tab Varian & Modifier in Menu Management)

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/menu/MenuManagementUiState.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/menu/MenuManagementViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/menu/MenuManagementScreen.kt`
- Test: `app/src/test/java/com/kapos/mobile/feature/menu/MenuManagementViewModelModifierTest.kt`

**Interfaces:**
- Consumes: `PosRepository` modifier observing and mutations.
- Produces: `MenuTab.MODIFIERS`, modifier filter chips, modifier cards, `AddEditModifierDialog`, `DeleteModifierConfirmDialog`.

- [ ] **Step 1: Write failing unit test for MenuManagementViewModel modifier actions**
```kotlin
package com.kapos.mobile.feature.menu

import com.kapos.mobile.domain.model.ModifierCategory
import com.kapos.mobile.domain.model.ModifierOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MenuManagementViewModelModifierTest {
    @Test
    fun testSelectModifierCategoryFilter() {
        val state = MenuManagementUiState(
            selectedModifierCategoryFilter = ModifierCategory.SAMBAL
        )
        assertEquals(ModifierCategory.SAMBAL, state.selectedModifierCategoryFilter)
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**
Run `./gradlew testDebugUnitTest --tests com.kapos.mobile.feature.menu.MenuManagementViewModelModifierTest`

- [ ] **Step 3: Implement MenuManagementUiState and MenuManagementViewModel modifier actions**
- Add `MenuTab.MODIFIERS("Varian & Modifier")` to `MenuTab`.
- Add `modifiers: List<ModifierOption>`, `selectedModifierCategoryFilter: ModifierCategory?`, `isAddEditModifierOpen: Boolean`, `modifierToEdit: ModifierOption?`, `modifierToDelete: ModifierOption?` to `MenuManagementUiState`.
- In `MenuManagementViewModel`: collect `posRepository.observeAllModifiers()`, implement `onSelectModifierFilter()`, `openAddModifier()`, `openEditModifier()`, `dismissModifierDialog()`, `saveModifier()`, `toggleModifierActive()`, `deleteModifier()`.

- [ ] **Step 4: Implement Compose UI for Modifier Management in MenuManagementScreen.kt**
- In `MenuManagementScreen.kt`:
  - Add tab button for `MenuTab.MODIFIERS`.
  - When `selectedTab == MenuTab.MODIFIERS`: render `ModifierManagementContent` showing category filter chips (`Semua`, `Level Pedas`, `Pilihan Sambal`, `Pelengkap & Alat`, `Ekstra Topping & Kemasan`), list of modifier cards with name, price badge, default badge, active switch, edit/delete actions, and floating/header `+ Tambah Modifier` button.
  - Implement `AddEditModifierDialog` for name, category selector, price input, and isDefault checkbox.
  - Implement delete confirmation dialog.

- [ ] **Step 5: Run tests and verify**
Run `./gradlew testDebugUnitTest --tests com.kapos.mobile.feature.menu.*`

- [ ] **Step 6: Commit changes**
`git add . && git commit -m "feat: implement Modifier Management CRUD UI in Menu Management"`

---

### Task 3: Product-Modifier Association in Add/Edit Product Dialog

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/menu/MenuManagementScreen.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/menu/MenuManagementViewModel.kt`
- Test: `app/src/test/java/com/kapos/mobile/feature/menu/MenuManagementUiStateTest.kt`

**Interfaces:**
- Consumes: `Product` with modifier flags.
- Produces: Checkbox controls in `AddEditProductDialog` with smart preset when switching category.

- [ ] **Step 1: Write test for smart defaults on category change in AddEditProductDialog**
Verify that selecting a Drink category unchecks spice & sambal by default, and selecting Sate/Chicken category checks spice, sambal, and toppings by default.

- [ ] **Step 2: Implement UI switches/checkboxes in AddEditProductDialog**
- In `AddEditProductDialog` of `MenuManagementScreen.kt`:
  - Add states for `allowSpiceLevel`, `allowSambal`, `allowComplimentary`, `allowToppings`.
  - Add "Kustomisasi & Modifier Kasir" section with 4 checkboxes.
  - When user selects a category:
    - If category is drinks/sides: auto-default flags to false.
    - If category is taichan/chicken: auto-default flags to true.
    - User can manually toggle any checkbox anytime.
  - Update `onSave` callback to pass the 4 boolean flags into `MenuManagementViewModel.saveProduct()`.

- [ ] **Step 3: Run unit tests**
Run `./gradlew testDebugUnitTest`

- [ ] **Step 4: Commit changes**
`git add . && git commit -m "feat: add product modifier toggles and smart category defaults to AddEditProductDialog"`

---

### Task 4: Streamlined POS Flow & Dynamic Compact ModifierDialog

**Files:**
- Modify: `app/src/main/java/com/kapos/mobile/feature/pos/PosViewModel.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/pos/PosUiState.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/pos/PosScreen.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/pos/ModifierDialog.kt`
- Modify: `app/src/main/java/com/kapos/mobile/feature/pos/ProductCard.kt`
- Test: `app/src/test/java/com/kapos/mobile/feature/pos/PosViewModelModifierTest.kt`

**Interfaces:**
- Consumes: `Product.hasModifiers`, active modifiers from repository.
- Produces: 1-tap fast add to cart for products without modifiers, dynamic compact `ModifierDialog` for products with modifiers.

- [ ] **Step 1: Write failing unit test for PosViewModel product click logic**
```kotlin
package com.kapos.mobile.feature.pos

import com.kapos.mobile.domain.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PosViewModelModifierTest {
    @Test
    fun testDrinkWithoutModifiersAddsDirectlyToCart() {
        val drink = Product(
            id = "d1", categoryId = "cat_drinks", name = "Es Teh", price = 5000.0,
            allowSpiceLevel = false, allowSambal = false, allowComplimentary = false, allowToppings = false
        )
        // Verify that click adds directly to cart and does not set selectedProductForModifier
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**
Run `./gradlew testDebugUnitTest --tests com.kapos.mobile.feature.pos.PosViewModelModifierTest`

- [ ] **Step 3: Implement PosViewModel 1-tap logic and load active modifiers**
- In `PosViewModel.kt`:
  - Observe `posRepository.observeActiveModifiers()` and store in `PosUiState.availableModifiers`.
  - In `onProductClick(product: Product)`:
    - If `!product.hasModifiers`: directly call `addToCart(product = product, quantity = 1)`.
    - If `product.hasModifiers`: set `selectedProductForModifier = product`.

- [ ] **Step 4: Refactor ModifierDialog.kt to use dynamic modifiers & conditional rendering**
- In `ModifierDialog.kt`:
  - Accept `modifiers: List<ModifierOption>` from `uiState.availableModifiers`.
  - Only show **Level Pedas** if `product.allowSpiceLevel == true` and spice options exist in database (filtering `ModifierCategory.SPICE_LEVEL`).
  - Only show **Pilihan Sambal** if `product.allowSambal == true` (filtering `ModifierCategory.SAMBAL`).
  - Only show **Pelengkap & Alat Makan** if `product.allowComplimentary == true` (filtering `ModifierCategory.COMPLIMENTARY`, filtering out utensils if drink, or filtering out straws if food).
  - Only show **Ekstra Topping & Kemasan** if `product.allowToppings == true` (filtering `ModifierCategory.EXTRA` and `ModifierCategory.PACKAGING`).
  - Keep Notes and Quantity Stepper.
- In `ProductCard.kt`:
  - Only display "Level 0–5" badge if `product.allowSpiceLevel` is true.

- [ ] **Step 5: Run unit tests to verify**
Run `./gradlew testDebugUnitTest --tests com.kapos.mobile.feature.pos.PosViewModelModifierTest`

- [ ] **Step 6: Commit changes**
`git add . && git commit -m "feat: streamline POS with 1-tap add to cart and dynamic compact ModifierDialog"`

---

### Task 5: End-to-End Build & Verification

**Files:**
- All modified and created files.

- [ ] **Step 1: Run complete test suite**
Run `./gradlew testDebugUnitTest` and ensure 100% of tests pass.

- [ ] **Step 2: Run assembleDebug to ensure clean build**
Run `./gradlew assembleDebug` to verify no compilation, resource, or lint blockers.

- [ ] **Step 3: Commit and summarize implementation**
`git add . && git commit -m "feat: complete customizable menu modifiers CRUD and product association"`
