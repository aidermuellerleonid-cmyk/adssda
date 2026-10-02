package de.fitapp.app.ui.training

import de.fitapp.app.data.entity.MuscleGroup
import java.text.Collator
import java.util.Locale

/** Anzeigename einer Muskelgruppe (wird im Training und im Radardiagramm verwendet). */
fun MuscleGroup.label() = when (this) {
    MuscleGroup.BRUST -> "Brust"
    MuscleGroup.BEINE -> "Beine"
    MuscleGroup.BAUCH -> "Bauch"
    MuscleGroup.ARME -> "Arme"
    MuscleGroup.RUECKEN -> "Rücken"
    MuscleGroup.SCHULTERN -> "Schultern"
}

private val collator: Collator = Collator.getInstance(Locale.GERMAN)

/**
 * Auswahlliste fürs Training: 60 Übungen, je 10 pro große Muskelgruppe.
 * Gruppen und Übungen sind alphabetisch sortiert (Arme, Bauch, Beine, Brust, Rücken, Schultern).
 */
val exerciseCatalog: List<Pair<MuscleGroup, List<String>>> = listOf(
    MuscleGroup.ARME to listOf(
        "Bankdips", "Bizeps Curls", "French Press", "Hammer Curls", "Kabel-Curls",
        "Kickbacks", "Konzentrationscurls", "Overhead-Trizepsstrecken", "Scott Curls",
        "Trizepsdrücken am Kabel"
    ),
    MuscleGroup.BAUCH to listOf(
        "Ab-Roller", "Bauchmaschine", "Beinheben hängend", "Beinheben liegend", "Cable Crunches",
        "Crunches", "Russian Twists", "Seitliches Beugen", "Sit-ups", "Woodchopper"
    ),
    MuscleGroup.BEINE to listOf(
        "Abduktor", "Adduktor", "Ausfallschritte", "Beinbeuger", "Beinpresse", "Beinstrecker",
        "Hip Thrust", "Rumänisches Kreuzheben", "Squats", "Wadenheben"
    ),
    MuscleGroup.BRUST to listOf(
        "Bankdrücken", "Brustpresse", "Butterfly", "Cable Crossover", "Dips",
        "Fliegende Kurzhanteln", "Kurzhantel-Bankdrücken", "Liegestütze", "Schrägbankdrücken",
        "Schrägbank-Kurzhantel"
    ),
    MuscleGroup.RUECKEN to listOf(
        "Einarmiges Kurzhantelrudern", "Klimmzüge", "Kreuzheben", "Latzug", "Latzug eng",
        "Rudermaschine", "Rudern am Kabel", "Rudern vorgebeugt", "Rückenstrecker", "T-Bar-Rudern"
    ),
    MuscleGroup.SCHULTERN to listOf(
        "Arnold Press", "Face Pulls", "Frontheben", "Reverse Butterfly", "Schulterdrücken Kurzhantel",
        "Schulterdrücken Langhantel", "Schulterpresse", "Seitheben", "Seitheben am Kabel", "Shrugs"
    )
).map { (group, names) -> group to names.sortedWith(collator) }
