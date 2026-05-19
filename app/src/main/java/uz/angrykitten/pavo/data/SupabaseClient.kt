package uz.angrykitten.pavo.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlin.time.Duration.Companion.minutes

object SupabaseClientProvider {

    // ── Replace ANON_KEY with your real key ──────────────────────────────────
    // Dashboard → Settings → API → "anon public"
    const val SUPABASE_URL = "https://injnswemzdxtrcduefvf.supabase.co"
    const val ANON_KEY     = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imluam5zd2VtemR4dHJjZHVlZnZmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzkxMjAyMzEsImV4cCI6MjA5NDY5NjIzMX0.4Vjszfc6P2OPQGwEjr__DC3Kf9tbY4UhnoGxauyEv0k"  // ← PASTE YOUR REAL KEY HERE

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = ANON_KEY
    ) {
        install(Postgrest)
        install(Storage) {
            transferTimeout = 5.minutes
        }
    }
}
