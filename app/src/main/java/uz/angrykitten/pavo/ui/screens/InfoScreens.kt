package uz.angrykitten.pavo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import uz.angrykitten.pavo.ui.localization.tr
import uz.angrykitten.pavo.ui.theme.AccentSky
import uz.angrykitten.pavo.ui.theme.Brand
import uz.angrykitten.pavo.ui.theme.BrandDark
import uz.angrykitten.pavo.ui.theme.BrandLight

private val PageCardShape = RoundedCornerShape(30.dp)
private val InlineCardShape = RoundedCornerShape(24.dp)

private data class FaqItem(val question: String, val answer: String)
private data class PolicyItem(val title: String, val body: String)

// ─── FAQ content (composable so tr() works) ──────────────────────────────────
@Composable
private fun faqItems(): List<FaqItem> = listOf(
    FaqItem(
        tr("Pavo nima?", "What is Pavo?", "Что такое Pavo?"),
        tr(
            "Pavo — O'zbekistondagi hayvonlar bozori uchun mobil ilova. Itlar, mushuklar, qo'ylar, sigirlar, otlar va boshqa hayvonlarni sotish, asrab olish yoki juftlash uchun e'lonlar joylashtirishingiz mumkin.",
            "Pavo is a mobile marketplace for animals in Uzbekistan. You can post listings to sell, adopt, or stud-match dogs, cats, sheep, cows, horses and more.",
            "Pavo — мобильный маркетплейс животных в Узбекистане. Вы можете размещать объявления о продаже, усыновлении или вязке собак, кошек, овец, коров, лошадей и других животных."
        )
    ),
    FaqItem(
        tr("E'lon berish uchun nimalar kerak?", "How do I post a listing?", "Как подать объявление?"),
        tr(
            "Avval akkauntga kiring yoki ro'yxatdan o'ting. Keyin pastdagi '+' tugmasini bosib, hayvon turini, ma'lumotlarini, rasmlarini va aloqa ma'lumotlarini bosqichma-bosqich to'ldiring.",
            "First sign in or register. Then tap the '+' button at the bottom, select the animal type, fill in the details, add photos and contact information step by step.",
            "Сначала войдите или зарегистрируйтесь. Затем нажмите кнопку '+' внизу, выберите вид животного, заполните данные, добавьте фото и контактную информацию пошагово."
        )
    ),
    FaqItem(
        tr("E'lon berish pullikmi?", "Is posting free?", "Размещение платное?"),
        tr(
            "Hozirgi versiyada e'lon joylashtirish bepul. Keyingi versiyalarda premium ko'tarish yoki reklama xizmatlari qo'shilishi mumkin.",
            "Posting listings is free in the current version. Premium promotion or advertising features may be added in future versions.",
            "В текущей версии размещение объявлений бесплатно. В будущих версиях могут быть добавлены платное продвижение или рекламные функции."
        )
    ),
    FaqItem(
        tr("Sotuvchi bilan qanday bog'lanaman?", "How do I contact the seller?", "Как связаться с продавцом?"),
        tr(
            "E'lon tafsiloti sahifasida qo'ng'iroq, Telegram va ilova ichidagi chat tugmalari bor. Qulay usulni tanlashingiz mumkin.",
            "On the listing detail page you'll find call, Telegram and in-app chat buttons. Choose whichever is most convenient.",
            "На странице объявления есть кнопки звонка, Telegram и внутреннего чата. Выберите удобный способ."
        )
    ),
    FaqItem(
        tr("Saqlangan e'lonlarni qayerdan topaman?", "Where are my saved listings?", "Где мои сохранённые объявления?"),
        tr(
            "Pastki navigatsiyadagi Saqlangan bo'limida yurak bilan belgilangan barcha e'lonlar ko'rsatiladi.",
            "All heart-marked listings are shown in the Saved section of the bottom navigation.",
            "Все отмеченные сердечком объявления отображаются в разделе «Избранное» нижней навигации."
        )
    ),
    FaqItem(
        tr("Qaysi hayvonlar ro'yxatlash mumkin?", "What animals can be listed?", "Каких животных можно размещать?"),
        tr(
            "It, mushuk, qo'y, sigir, ot va boshqa hayvonlar. 'Boshqa' kategoriyasi noodatiy hayvonlarni ham qamrab oladi.",
            "Dogs, cats, sheep, cows, horses, and others. The 'Other' category covers any unusual animals.",
            "Собаки, кошки, овцы, коровы, лошади и другие. Категория «Другое» охватывает любых необычных животных."
        )
    ),
    FaqItem(
        tr("Akkauntimni qanday o'chiraman?", "How do I delete my account?", "Как удалить аккаунт?"),
        tr(
            "Profil → Sozlamalar sahifasiga o'ting. Pastki 'Xavfli zona' bo'limida akkauntni o'chirish tugmasi bor.",
            "Go to Profile → Settings. The 'Danger zone' section at the bottom has an account deletion button.",
            "Перейдите в Профиль → Настройки. В разделе «Опасная зона» внизу есть кнопка удаления аккаунта."
        )
    ),
    FaqItem(
        tr("Qaysi tillar mavjud?", "What languages are supported?", "Какие языки поддерживаются?"),
        tr(
            "Ilova O'zbek, Ingliz va Rus tillarini to'liq qo'llab-quvvatlaydi. Tilni Profil → Sozlamalar bo'limidan o'zgartirish mumkin.",
            "The app fully supports Uzbek, English and Russian. Change the language in Profile → Settings.",
            "Приложение полностью поддерживает узбекский, английский и русский языки. Изменить язык можно в Профиль → Настройки."
        )
    )
)

// ─── Privacy Policy content (composable so tr() works) ───────────────────────
@Composable
private fun policyItems(): List<PolicyItem> = listOf(
    PolicyItem(
        tr("1. Qanday ma'lumotlar yig'iladi", "1. What data is collected", "1. Какие данные собираются"),
        tr(
            "Hisob ma'lumotlari (ism, email, telefon), e'lon matnlari, hayvon tafsilotlari va ilova ichidagi faoliyat uchun zarur bo'lgan texnik ma'lumotlar yig'iladi.",
            "Account information (name, email, phone), listing texts, animal details, and technical data required for app functionality are collected.",
            "Собираются данные аккаунта (имя, email, телефон), тексты объявлений, данные о животных и технические данные, необходимые для работы приложения."
        )
    ),
    PolicyItem(
        tr("2. Ma'lumotlardan foydalanish", "2. How data is used", "2. Как используются данные"),
        tr(
            "Bu ma'lumotlar akkaunt yaratish, e'lonlarni chiqarish, chatni yuritish, foydalanuvchi tajribasini yaxshilash va xavfsizlikni ta'minlash uchun ishlatiladi.",
            "This data is used for account creation, publishing listings, operating chats, improving user experience and maintaining security.",
            "Эти данные используются для создания аккаунта, публикации объявлений, работы чата, улучшения пользовательского опыта и обеспечения безопасности."
        )
    ),
    PolicyItem(
        tr("3. Saqlash va xavfsizlik", "3. Storage & security", "3. Хранение и безопасность"),
        tr(
            "Loyihada Firebase (autentifikatsiya) va Supabase (ma'lumotlar bazasi, fayl saqlash) servislaridan foydalaniladi. Qat'iy ruxsat qoidalari (RLS) qo'llaniladi.",
            "The project uses Firebase (authentication) and Supabase (database, file storage). Strict Row Level Security (RLS) policies are applied.",
            "В проекте используются Firebase (аутентификация) и Supabase (база данных, хранение файлов). Применяются строгие политики Row Level Security (RLS)."
        )
    ),
    PolicyItem(
        tr("4. Uchinchi tomon servislar", "4. Third-party services", "4. Сторонние сервисы"),
        tr(
            "Autentifikatsiya, real-vaqt chat va ma'lumotlar bazasi uchun uchinchi tomon servislar ishlatiladi. Har bir servis o'zining maxfiylik qoidalariga ega.",
            "Third-party services are used for authentication, real-time chat and database. Each service has its own privacy policy.",
            "Для аутентификации, чата в реальном времени и базы данных используются сторонние сервисы. У каждого сервиса есть собственная политика конфиденциальности."
        )
    ),
    PolicyItem(
        tr("5. Foydalanuvchi huquqlari", "5. User rights", "5. Права пользователя"),
        tr(
            "Foydalanuvchi o'z profilini yangilashi, saqlangan ma'lumotlarni ko'rishi va akkauntni o'chirishni so'rashi mumkin. Bularning barchasi ilova ichida amalga oshiriladi.",
            "Users can update their profile, view saved data and request account deletion — all from within the app.",
            "Пользователи могут обновить профиль, просматривать сохранённые данные и запросить удаление аккаунта — всё это доступно внутри приложения."
        )
    ),
    PolicyItem(
        tr("6. Kelajakdagi talablar", "6. Future requirements", "6. Будущие требования"),
        tr(
            "To'liq versiyada rasmiy maxfiylik siyosati URL manzili, kontakt ma'lumotlari, rozilik boshqaruvi va moderatsiya siyosatlari qo'shilishi rejalashtirilgan.",
            "The full version is planned to include an official privacy policy URL, contact information, consent management and moderation policies.",
            "В полной версии планируется добавить официальный URL политики конфиденциальности, контактные данные, управление согласием и политики модерации."
        )
    )
)

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun FAQScreen(navController: NavController) {
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    val items = faqItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("Ko'p so'raladigan savollar", "FAQ", "Частые вопросы"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Orqaga", "Back", "Назад"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                InfoHeaderCard(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    title = tr("Ko'p so'raladigan savollar", "Frequently Asked Questions", "Часто задаваемые вопросы"),
                    subtitle = tr(
                        "Ilova, e'lonlar va akkaunt bo'yicha tez javoblar",
                        "Quick answers about the app, listings and account",
                        "Быстрые ответы о приложении, объявлениях и аккаунте"
                    )
                )
            }

            itemsIndexed(items) { index, item ->
                FaqCard(
                    item = item,
                    expanded = expandedIndex == index,
                    onToggle = { expandedIndex = if (expandedIndex == index) null else index }
                )
            }
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    val items = policyItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("Maxfiylik siyosati", "Privacy Policy", "Политика конфиденциальности"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Orqaga", "Back", "Назад"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            InfoHeaderCard(
                icon = Icons.Default.Shield,
                title = tr(
                    "Ma'lumotlaringiz qanday ishlatiladi",
                    "How your data is used",
                    "Как используются ваши данные"
                ),
                subtitle = tr(
                    "Qisqa va tushunarli maxfiylik siyosati",
                    "Short and clear privacy policy",
                    "Краткая и понятная политика конфиденциальности"
                )
            )

            items.forEach { item ->
                PolicySection(item = item)
            }

            Surface(
                shape = InlineCardShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Text(
                    text = tr(
                        "Eslatma: ishlab chiqarish versiyasi uchun yuridik jihatdan to'liq va tashqi URL bilan boshqariladigan siyosat tayyorlanishi kerak.",
                        "Note: a legally complete policy managed via an external URL should be prepared for the production version.",
                        "Примечание: для производственной версии необходимо подготовить юридически полную политику, доступную по внешнему URL."
                    ),
                    modifier = Modifier.padding(18.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun InfoHeaderCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        shape = PageCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(BrandDark, Brand, AccentSky.copy(alpha = 0.78f))
                    )
                )
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun FaqCard(
    item: FaqItem,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "faq_arrow")

    Card(
        shape = InlineCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Q", color = Brand, fontWeight = FontWeight.Black)
                }

                Text(
                    text = item.question,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Brand,
                    modifier = Modifier.rotate(rotation)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("A", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
                        }

                        Text(
                            text = item.answer,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicySection(item: PolicyItem) {
    Card(
        shape = InlineCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Brand)
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = item.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}
