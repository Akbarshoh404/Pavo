package uz.angrykitten.pavo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class City(
    val id: Int,
    val name: String,
    val slug: String
)

@Serializable
data class District(
    val id: Int,
    val city_id: Int,
    val name: String,
    val slug: String
)

@Serializable
data class Animal(
    val id: String,
    val user_id: String,
    val title: String,
    val description: String,
    val listing_type: String,   // "sale" | "adoption" | "stud"
    val animal_type: String,    // "dog" | "cat" | "sheep" | "cow" | "horse" | "other"
    val breed: String,
    val age_months: Int,
    val weight_kg: Double,
    val city_id: Int,
    val city_name: String,
    val district_id: Int,
    val district_name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val price: Double,
    val currency: String,       // "USD" | "UZS"
    val is_active: Boolean,
    val views_count: Int,
    val images: List<String>,
    val seller_name: String,
    val seller_phone: String,
    val seller_telegram: String,
    val seller_avatar: String,
    val vaccination_status: String? = null,
    val has_pedigree: Boolean = false
)

@Serializable
data class AppData(
    val cities: List<City>,
    val districts: List<District>,
    val animals: List<Animal>
)
