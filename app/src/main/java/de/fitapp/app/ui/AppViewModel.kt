package de.fitapp.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.fitapp.app.FitApplication
import de.fitapp.app.data.entity.*
import de.fitapp.app.data.food.BarcodeLookupResult
import de.fitapp.app.util.CalorieCalculator
import de.fitapp.app.util.MuscleProgressCalculator
import de.fitapp.app.util.MuscleProgressResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Unterscheidet "wird noch aus der DB geladen" von "es existiert (noch) kein Profil". */
sealed class ProfileState {
    object Loading : ProfileState()
    data class Loaded(val profile: UserProfile?) : ProfileState()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FitApplication
    private val db = app.database
    private val foodRepository = app.foodRepository

    val profileState: StateFlow<ProfileState> =
        db.userProfileDao().observeProfile()
            .map<UserProfile?, ProfileState> { ProfileState.Loaded(it) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, ProfileState.Loading)

    // Bequemer Zugriff auf das Profil, falls schon geladen (z. B. für Berechnungen).
    val profile: StateFlow<UserProfile?> =
        profileState
            .map { (it as? ProfileState.Loaded)?.profile }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val today: Long get() = LocalDate.now().toEpochDay()

    val todayEntries: StateFlow<List<DiaryEntryEntity>> =
        db.diaryDao().observeForDay(today)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val exerciseLogs: StateFlow<List<ExerciseLogEntity>> =
        db.exerciseDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val weightEntries: StateFlow<List<WeightEntryEntity>> =
        db.weightDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Fließt zusammen mit dem Profil ein, da die Bewertung Alter, Geschlecht und Körpergewicht
    // der Person berücksichtigt (siehe StrengthBenchmark).
    val muscleProgress: StateFlow<List<MuscleProgressResult>> =
        combine(exerciseLogs, profile) { logs, prof -> MuscleProgressCalculator.calculate(logs, prof) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { foodRepository.ensureSeeded() }
    }

    // ---------- Profil / Onboarding ----------

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            db.userProfileDao().upsert(profile)
        }
    }

    fun estimateCalories(
        age: Int, heightCm: Int, weightKg: Float,
        gender: Gender, activityLevel: ActivityLevel, goal: Goal
    ) = CalorieCalculator.estimate(age, heightCm, weightKg, gender, activityLevel, goal)

    // ---------- Ernährung ----------

    suspend fun searchFood(query: String) = foodRepository.search(query)

    suspend fun lookupBarcode(barcode: String): BarcodeLookupResult = foodRepository.lookupBarcode(barcode)

    suspend fun saveCustomFood(
        name: String, kcal: Float, protein: Float, carbs: Float, sugar: Float, fat: Float, fiber: Float
    ) = foodRepository.saveCustomFood(name, kcal, protein, carbs, sugar, fat, fiber)

    fun addDiaryEntry(food: FoodItemEntity, grams: Float, mealType: MealType) {
        val factor = grams / 100f
        viewModelScope.launch {
            db.diaryDao().insert(
                DiaryEntryEntity(
                    foodItemId = food.id,
                    foodName = food.name,
                    gramAmount = grams,
                    mealType = mealType,
                    dateEpochDay = today,
                    kcal = food.kcalPer100g * factor,
                    protein = food.proteinPer100g * factor,
                    carbs = food.carbsPer100g * factor,
                    sugar = food.sugarPer100g * factor,
                    fat = food.fatPer100g * factor,
                    fiber = food.fiberPer100g * factor,
                    valueQuality = food.source
                )
            )
        }
    }

    fun deleteDiaryEntry(entry: DiaryEntryEntity) {
        viewModelScope.launch {
            db.diaryDao().delete(entry)
        }
    }

    // ---------- Training ----------

    /**
     * Speichert das Maximalgewicht einer Übung. Pro Übung gibt es nur einen Eintrag:
     * Wird dieselbe Übung erneut eingetragen, ersetzt der neue Wert den alten.
     * Wiederholungen/Sätze werden nicht abgefragt (intern reps = 1, sets = 1, damit das
     * Datenbankschema unverändert bleibt).
     */
    fun saveMaxWeight(name: String, group: MuscleGroup, weightKg: Float) {
        viewModelScope.launch {
            db.exerciseDao().deleteByName(name)
            db.exerciseDao().insert(
                ExerciseLogEntity(
                    exerciseName = name, muscleGroup = group,
                    weightKg = weightKg, reps = 1, sets = 1, dateEpochDay = today
                )
            )
        }
    }

    // ---------- Gewicht ----------

    fun addWeightEntry(weightKg: Float, dateEpochDay: Long = today) {
        viewModelScope.launch {
            db.weightDao().insert(WeightEntryEntity(dateEpochDay = dateEpochDay, weightKg = weightKg))
        }
    }
}
