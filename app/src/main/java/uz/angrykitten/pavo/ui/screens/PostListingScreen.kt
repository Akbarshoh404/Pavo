package uz.angrykitten.pavo.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import uz.angrykitten.pavo.data.model.Animal
import uz.angrykitten.pavo.ui.localization.tr
import uz.angrykitten.pavo.ui.navigation.Screen
import uz.angrykitten.pavo.ui.theme.Brand
import uz.angrykitten.pavo.ui.viewmodel.AppViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostListingScreen(viewModel: AppViewModel, navController: NavController) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val userId by viewModel.userId.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userPhone by viewModel.userPhone.collectAsStateWithLifecycle()

    if (!isLoggedIn) {
        GuestPromptScreen(
            title = tr("E'lon berish", "Post listing", "Подать объявление"),
            message = tr("E'lon berish uchun tizimga kirish zarur", "Sign in to post a listing", "Войдите, чтобы подать объявление"),
            navController = navController
        )
        return
    }

    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 5

    // Form state
    var listingType by remember { mutableStateOf("sale") }
    var animalType by remember { mutableStateOf("dog") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var ageMonths by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var vaccinationStatus by remember { mutableStateOf("") }
    var hasPedigree by remember { mutableStateOf(false) }
    var selectedCityId by remember { mutableStateOf<Int?>(null) }
    var selectedCityName by remember { mutableStateOf("") }
    var selectedDistrictId by remember { mutableStateOf<Int?>(null) }
    var selectedDistrictName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var phone by remember { mutableStateOf("") }
    var telegram by remember { mutableStateOf("") }
    var stepError by remember { mutableStateOf<String?>(null) }

    val uploadedImages = remember { mutableStateListOf<String>() }
    var isUploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Collect reactively — updates automatically when Supabase data loads
    val cities by viewModel.cityList.collectAsStateWithLifecycle()

    LaunchedEffect(userPhone) {
        if (phone.isBlank() && !userPhone.isNullOrBlank()) {
            phone = userPhone!!.removePrefix("+998")
        }
    }

    val stepTitles = listOf(
        tr("Muomala turi", "Listing type", "Тип объявления"),
        tr("Hayvon haqida", "Animal info", "О животном"),
        tr("Joylashuv", "Location", "Местоположение"),
        tr("Narx va aloqa", "Price & contact", "Цена и контакты"),
        tr("Ko'rib chiqish", "Review", "Проверка")
    )

    // Pre-read localised strings so non-composable lambdas (onClick, onFailure, canProceed) can use them
    val errBreed        = tr("Zotini kiriting", "Enter breed", "Введите породу")
    val errAge          = tr("Yoshini kiriting", "Enter age", "Введите возраст")
    val errCity         = tr("Shaharni tanlang", "Select a city", "Выберите город")
    val errPrice        = tr("Narxni kiriting", "Enter price", "Введите цену")
    val errPhone        = tr("Telefon raqamni kiriting", "Enter phone number", "Введите номер телефона")
    val errImageUpload  = tr("Rasm yuklashda xatolik", "Image upload failed", "Ошибка загрузки фото")
    val labelDog        = tr("It", "Dog", "Собака")
    val labelCat        = tr("Mushuk", "Cat", "Кошка")
    val labelSheep      = tr("Qo'y", "Sheep", "Овца")
    val labelCow        = tr("Sigir", "Cow", "Корова")
    val labelHorse      = tr("Ot", "Horse", "Лошадь")
    val labelAnimal     = tr("Hayvon", "Animal", "Животное")

    fun canProceed(): Pair<Boolean, String?> = when (currentStep) {
        1 -> when {
            breed.isBlank()     -> false to errBreed
            ageMonths.isBlank() -> false to errAge
            else                -> true to null
        }
        2 -> if (selectedCityId != null) true to null else false to errCity
        3 -> when {
            listingType != "adoption" && price.isBlank() -> false to errPrice
            phone.isBlank() -> false to errPhone
            else            -> true to null
        }
        else -> true to null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with progress
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    tr("E'lon berish", "Post listing", "Подать объявление"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(totalSteps) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(
                                    if (index <= currentStep) Brand
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "${currentStep + 1}/$totalSteps: ${stepTitles[currentStep]}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Step content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (currentStep) {
                0 -> AnimalListingTypeStep(listingType, animalType, onListingType = { listingType = it }, onAnimalType = { animalType = it })
                1 -> AnimalInfoStep(
                    title = title, description = description, breed = breed,
                    ageMonths = ageMonths, weightKg = weightKg,
                    vaccinationStatus = vaccinationStatus, hasPedigree = hasPedigree,
                    uploadedImages = uploadedImages, isUploading = isUploading,
                    onTitle = { title = it }, onDescription = { description = it },
                    onBreed = { breed = it }, onAgeMonths = { ageMonths = it },
                    onWeightKg = { weightKg = it }, onVaccinationStatus = { vaccinationStatus = it },
                    onHasPedigree = { hasPedigree = it },
                    onImagePicked = { uri ->
                        scope.launch {
                            isUploading = true
                            viewModel.uploadImage(uri).onSuccess { url ->
                                uploadedImages.add(url)
                            }.onFailure { err ->
                                // Show the actual error (includes bucket/policy hints)
                                stepError = err.message ?: errImageUpload
                            }
                            isUploading = false
                        }
                    },
                    onRemoveImage = { uploadedImages.remove(it) }
                )
                2 -> AnimalLocationStep(
                    cities = cities, selectedCityId = selectedCityId,
                    selectedCityName = selectedCityName, selectedDistrictId = selectedDistrictId,
                    selectedDistrictName = selectedDistrictName, address = address,
                    viewModel = viewModel,
                    onCitySelected = { id, name -> selectedCityId = id; selectedCityName = name; selectedDistrictId = null; selectedDistrictName = "" },
                    onDistrictSelected = { id, name -> selectedDistrictId = id; selectedDistrictName = name },
                    onAddress = { address = it }
                )
                3 -> AnimalPriceContactStep(
                    price = price, currency = currency, phone = phone, telegram = telegram,
                    listingType = listingType,
                    onPrice = { price = it }, onCurrency = { currency = it },
                    onPhone = { phone = it }, onTelegram = { telegram = it }
                )
                4 -> AnimalReviewStep(
                    listingType = listingType, animalType = animalType,
                    title = title, breed = breed, ageMonths = ageMonths, weightKg = weightKg,
                    cityName = selectedCityName, districtName = selectedDistrictName,
                    price = price, currency = currency
                )
            }

            stepError?.let { err ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Navigation buttons
        Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep--; stepError = null },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Orqaga", "Back", "Назад"), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Button(
                    onClick = {
                        if (currentStep < totalSteps - 1) {
                            val (ok, err) = canProceed()
                            if (ok) { currentStep++; stepError = null }
                            else stepError = err
                        } else {
                            val autoTitle = title.ifBlank {
                                val typeLabel = when (animalType) {
                                    "dog"   -> labelDog
                                    "cat"   -> labelCat
                                    "sheep" -> labelSheep
                                    "cow"   -> labelCow
                                    "horse" -> labelHorse
                                    else    -> labelAnimal
                                }
                                "$typeLabel — $breed"
                            }
                            val animal = Animal(
                                id = UUID.randomUUID().toString(),
                                user_id = userId ?: "local_user",
                                title = autoTitle,
                                description = description,
                                listing_type = listingType,
                                animal_type = animalType,
                                breed = breed,
                                age_months = ageMonths.toIntOrNull() ?: 0,
                                weight_kg = weightKg.toDoubleOrNull() ?: 0.0,
                                city_id = selectedCityId ?: 1,
                                city_name = selectedCityName.ifBlank { "Toshkent" },
                                district_id = selectedDistrictId ?: 1,
                                district_name = selectedDistrictName.ifBlank { "" },
                                address = address,
                                latitude = 41.3,
                                longitude = 69.2,
                                price = if (listingType == "adoption") 0.0 else price.toDoubleOrNull() ?: 0.0,
                                currency = currency,
                                is_active = true,
                                views_count = 0,
                                images = if (uploadedImages.isEmpty())
                                    listOf("https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=800")
                                else uploadedImages.toList(),
                                seller_name = userName ?: "Foydalanuvchi",
                                seller_phone = if (phone.isNotBlank()) "+998$phone" else "",
                                seller_telegram = if (telegram.isNotBlank()) telegram.removePrefix("@") else "",
                                seller_avatar = "",
                                vaccination_status = vaccinationStatus.ifBlank { null },
                                has_pedigree = hasPedigree
                            )
                            viewModel.postListing(animal)
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.PostListing.route) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand)
                ) {
                    Text(
                        if (currentStep == totalSteps - 1)
                            tr("E'lon berish", "Post listing", "Подать")
                        else
                            tr("Keyingisi", "Next", "Далее"),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (currentStep < totalSteps - 1) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp), tint = Color.White)
                    }
                }
            }
        }
    }
}

// ── Step 0: Listing type + animal type ──────────────────────────────────────

@Composable
fun AnimalListingTypeStep(
    listingType: String,
    animalType: String,
    onListingType: (String) -> Unit,
    onAnimalType: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column {
            Text(
                tr("Muomala turi", "Listing type", "Тип объявления"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimalOptionCard(Icons.Default.Sell, tr("Sotuv", "Sale", "Продажа"), listingType == "sale", { onListingType("sale") }, Modifier.weight(1f))
                AnimalOptionCard(Icons.Default.Favorite, tr("Asrab olish", "Adoption", "Приютить"), listingType == "adoption", { onListingType("adoption") }, Modifier.weight(1f))
                AnimalOptionCard(Icons.Default.Pets, tr("Juftlash", "Stud", "Вязка"), listingType == "stud", { onListingType("stud") }, Modifier.weight(1f))
            }
        }
        Column {
            Text(
                tr("Hayvon turi", "Animal type", "Вид животного"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            val types = listOf(
                Triple(Icons.Default.Pets, "dog", tr("It", "Dog", "Собака")),
                Triple(Icons.Default.Pets, "cat", tr("Mushuk", "Cat", "Кошка")),
                Triple(Icons.Default.Agriculture, "sheep", tr("Qo'y", "Sheep", "Овца")),
                Triple(Icons.Default.Agriculture, "cow", tr("Sigir", "Cow", "Корова")),
                Triple(Icons.Default.DirectionsRun, "horse", tr("Ot", "Horse", "Лошадь")),
                Triple(Icons.Default.Category, "other", tr("Boshqa", "Other", "Другое"))
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                types.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (icon, type, label) ->
                            AnimalOptionCard(icon, label, animalType == type, { onAnimalType(type) }, Modifier.weight(1f))
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimalOptionCard(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = if (selected)
            androidx.compose.foundation.BorderStroke(2.dp, Brand)
        else
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Brand.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon, null,
                tint = if (selected) Brand else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Brand else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ── Step 1: Animal details + photos ─────────────────────────────────────────

@Composable
fun AnimalInfoStep(
    title: String, description: String, breed: String,
    ageMonths: String, weightKg: String,
    vaccinationStatus: String, hasPedigree: Boolean,
    uploadedImages: List<String>, isUploading: Boolean,
    onTitle: (String) -> Unit, onDescription: (String) -> Unit,
    onBreed: (String) -> Unit, onAgeMonths: (String) -> Unit,
    onWeightKg: (String) -> Unit, onVaccinationStatus: (String) -> Unit,
    onHasPedigree: (Boolean) -> Unit,
    onImagePicked: (android.net.Uri) -> Unit,
    onRemoveImage: (String) -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { onImagePicked(it) } }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            tr("Hayvon tafsilotlari", "Animal details", "Данные о животном"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        // Photo upload
        Text(tr("Rasmlar", "Photos", "Фото"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uploadedImages.size) { index ->
                val url = uploadedImages[index]
                Box(modifier = Modifier.size(100.dp)) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { onRemoveImage(url) },
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
            item {
                if (isUploading) {
                    Box(
                        modifier = Modifier.size(100.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Brand) }
                } else if (uploadedImages.size < 10) {
                    Card(
                        modifier = Modifier.size(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        onClick = {
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            Text(tr("Qo'shish", "Add", "Добавить"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        PostFormField(tr("E'lon sarlavhasi (ixtiyoriy)", "Listing title (optional)", "Заголовок (необязательно)"), title, onTitle)
        PostFormField(tr("Tavsif", "Description", "Описание"), description, onDescription, singleLine = false, minLines = 3)
        PostFormField(tr("Zoti *", "Breed *", "Порода *"), breed, onBreed)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PostFormField(tr("Yoshi (oy) *", "Age (months) *", "Возраст (мес) *"), ageMonths, onAgeMonths, KeyboardType.Number, modifier = Modifier.weight(1f))
            PostFormField(tr("Vazni (kg)", "Weight (kg)", "Вес (кг)"), weightKg, onWeightKg, KeyboardType.Number, modifier = Modifier.weight(1f))
        }
        PostFormField(tr("Vaksinatsiya holati", "Vaccination status", "Статус вакцинации"), vaccinationStatus, onVaccinationStatus)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = hasPedigree, onCheckedChange = onHasPedigree, colors = CheckboxDefaults.colors(checkedColor = Brand))
            Text(tr("Nasl hujjati bor", "Has pedigree certificate", "Есть родословная"), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

// ── Step 2: Location ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalLocationStep(
    cities: List<uz.angrykitten.pavo.data.model.City>,
    selectedCityId: Int?,
    selectedCityName: String,
    selectedDistrictId: Int?,
    selectedDistrictName: String,
    address: String,
    viewModel: AppViewModel,
    onCitySelected: (Int, String) -> Unit,
    onDistrictSelected: (Int, String) -> Unit,
    onAddress: (String) -> Unit
) {
    // Re-read when city changes OR when the cities list itself updates (data loaded from Supabase)
    val districts = remember(selectedCityId, cities) { viewModel.getDistricts(selectedCityId) }
    var cityExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(tr("Joylashuv", "Location", "Местоположение"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        ExposedDropdownMenuBox(expanded = cityExpanded, onExpandedChange = { cityExpanded = it }) {
            OutlinedTextField(
                value = selectedCityName.ifBlank { tr("Shaharni tanlang", "Select city", "Выберите город") },
                onValueChange = {},
                readOnly = true,
                label = { Text(tr("Shahar", "City", "Город")) },
                leadingIcon = { Icon(Icons.Default.LocationCity, null, tint = Brand) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(cityExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand)
            )
            ExposedDropdownMenu(expanded = cityExpanded, onDismissRequest = { cityExpanded = false }) {
                cities.forEach { city ->
                    DropdownMenuItem(
                        text = { Text(city.name) },
                        onClick = { onCitySelected(city.id, city.name); cityExpanded = false }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(expanded = districtExpanded, onExpandedChange = { if (selectedCityId != null) districtExpanded = it }) {
            OutlinedTextField(
                value = selectedDistrictName.ifBlank { tr("Tumanni tanlang", "Select district", "Выберите район") },
                onValueChange = {},
                readOnly = true,
                label = { Text(tr("Tuman", "District", "Район")) },
                leadingIcon = { Icon(Icons.Default.Map, null, tint = Brand) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(districtExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand),
                enabled = selectedCityId != null
            )
            ExposedDropdownMenu(expanded = districtExpanded, onDismissRequest = { districtExpanded = false }) {
                districts.forEach { district ->
                    DropdownMenuItem(
                        text = { Text(district.name) },
                        onClick = { onDistrictSelected(district.id, district.name); districtExpanded = false }
                    )
                }
            }
        }

        PostFormField(tr("Aniq manzil (ixtiyoriy)", "Exact address (optional)", "Точный адрес (необязательно)"), address, onAddress)
    }
}

// ── Step 3: Price & contact ──────────────────────────────────────────────────

@Composable
fun AnimalPriceContactStep(
    price: String, currency: String, phone: String, telegram: String,
    listingType: String,
    onPrice: (String) -> Unit, onCurrency: (String) -> Unit,
    onPhone: (String) -> Unit, onTelegram: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(tr("Narx va aloqa", "Price & contact", "Цена и контакты"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        if (listingType == "adoption") {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Brand.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Brand.copy(alpha = 0.3f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Favorite, null, tint = Brand)
                    Text(tr("Asrab olish uchun bepul", "Free for adoption", "Бесплатно для принятия"), fontWeight = FontWeight.SemiBold, color = Brand)
                }
            }
        } else {
            PostFormField(tr("Narx *", "Price *", "Цена *"), price, onPrice, KeyboardType.Number)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("USD" to "$", "UZS" to "so'm").forEach { (c, label) ->
                    FilterChip(
                        selected = currency == c,
                        onClick = { onCurrency(c) },
                        label = { Text("$c ($label)") },
                        shape = RoundedCornerShape(18.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Brand,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        Text(tr("Aloqa ma'lumotlari", "Contact details", "Контактные данные"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        PhoneFieldUz(tr("Telefon raqam *", "Phone number *", "Телефон *"), phone, onPhone)
        OutlinedTextField(
            value = telegram,
            onValueChange = { input ->
                // Strip leading @ so we store just the username
                onTelegram(input.removePrefix("@").filter { it.isLetterOrDigit() || it == '_' })
            },
            label = { Text(tr("Telegram foydalanuvchi nomi", "Telegram username", "Имя пользователя Telegram")) },
            placeholder = { Text("username") },
            prefix = { Text("@", fontWeight = FontWeight.Bold) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand, focusedLabelColor = Brand),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            singleLine = true,
            supportingText = { Text(tr("Ixtiyoriy — xabar yozish uchun", "Optional — for direct messaging", "Необязательно — для личных сообщений"), style = MaterialTheme.typography.labelSmall) }
        )
    }
}

// ── Step 4: Review ───────────────────────────────────────────────────────────

@Composable
fun AnimalReviewStep(
    listingType: String, animalType: String,
    title: String, breed: String, ageMonths: String, weightKg: String,
    cityName: String, districtName: String,
    price: String, currency: String
) {
    val listingLabel = when (listingType) {
        "sale" -> tr("Sotuv", "Sale", "Продажа")
        "adoption" -> tr("Asrab olish", "Adoption", "Приютить")
        "stud" -> tr("Juftlash", "Stud", "Вязка")
        else -> listingType
    }
    val typeLabel = when (animalType) {
        "dog" -> tr("It", "Dog", "Собака")
        "cat" -> tr("Mushuk", "Cat", "Кошка")
        "sheep" -> tr("Qo'y", "Sheep", "Овца")
        "cow" -> tr("Sigir", "Cow", "Корова")
        "horse" -> tr("Ot", "Horse", "Лошадь")
        else -> tr("Boshqa", "Other", "Другое")
    }
    val location = listOfNotNull(districtName.ifBlank { null }, cityName.ifBlank { null }).joinToString(", ").ifBlank { tr("(kiritilmagan)", "(not entered)", "(не указано)") }
    val priceText = if (listingType == "adoption") tr("Bepul", "Free", "Бесплатно")
                    else if (price.isNotBlank()) "$price $currency"
                    else tr("(kiritilmagan)", "(not entered)", "(не указано)")

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(tr("E'lonni ko'rib chiqish", "Review listing", "Проверка объявления"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        Card(
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                ReviewIconRow(Icons.Default.Sell, tr("Muomala turi", "Listing type", "Тип"), listingLabel)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                ReviewIconRow(Icons.Default.Pets, tr("Hayvon turi", "Animal type", "Вид"), typeLabel)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                ReviewIconRow(Icons.Default.Stars, tr("Zoti", "Breed", "Порода"), breed.ifBlank { tr("(kiritilmagan)", "(not entered)", "(не указано)") })
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                if (ageMonths.isNotBlank()) {
                    ReviewIconRow(Icons.Default.Cake, tr("Yoshi", "Age", "Возраст"), "$ageMonths ${tr("oy", "months", "мес.")}")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                }
                if (weightKg.isNotBlank()) {
                    ReviewIconRow(Icons.Default.Scale, tr("Vazni", "Weight", "Вес"), "$weightKg kg")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                }
                ReviewIconRow(Icons.Default.LocationOn, tr("Joylashuv", "Location", "Местоположение"), location)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                ReviewIconRow(Icons.Default.AttachMoney, tr("Narx", "Price", "Цена"), priceText)
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Brand),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(tr("Tayyor!", "Ready!", "Готово!"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tr("E'lon berish tugmasini bossangiz, e'lon darhol faollashadi.", "Press 'Post listing' to publish immediately.", "Нажмите 'Подать', чтобы опубликовать сразу."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── ReviewIconRow (used in AnimalReviewStep) ─────────────────────────────────

@Composable
fun ReviewIconRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Shared helpers ───────────────────────────────────────────────────────────

@Composable
fun PostFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand, focusedLabelColor = Brand),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        minLines = minLines
    )
}

@Composable
fun PhoneFieldUz(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }.take(9)
            onValueChange(digits)
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Brand, focusedLabelColor = Brand),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        prefix = {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text("+998", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
        },
        placeholder = { Text("XX XXX XX XX") },
        leadingIcon = { Icon(Icons.Default.Phone, null, tint = Brand, modifier = Modifier.size(20.dp)) }
    )
}
