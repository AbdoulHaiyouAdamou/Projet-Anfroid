# LinguaAI — Apprends l'anglais en conversant avec des tuteurs IA

LinguaAI est une application Android (Kotlin + Jetpack Compose) qui aide les
francophones à progresser en anglais grâce à des tuteurs IA propulsés par
l'API Gemini de Google. Chaque message est analysé, corrigé et commenté en
français, puis le tuteur répond en anglais naturel adapté à ton niveau CEFR.

## Fonctionnalités

- **Tuteurs IA** avec accents et personnalités variés (US, UK, Canada, Business)
  et un coach personnalisable (nom, accent, genre, niveau, personnalité).
- **Correction en temps réel** : chaque phrase est analysée, l'erreur expliquée
  en français et la version corrigée proposée.
- **Leçons de grammaire** (articles, present perfect, phrasal verbs, faux amis)
  avec quiz intégrés.
- **Scénarios de conversation** (entretien d'embauche, restaurant, hôtel, etc.).
- **Flashcards** avec répétition espacée (spaced repetition).
- **Gamification** : XP, pièces et niveaux pour rester motivé.
- **Mode hors-ligne** : un simulateur de secours prend le relais si aucune clé
  API n'est configurée.

## Stack technique

- **Langage** : Kotlin
- **UI** : Jetpack Compose (Material 3)
- **Architecture** : MVVM (ViewModel + Repository)
- **Persistance** : Room
- **Réseau** : Retrofit + OkHttp + Moshi
- **IA** : Google Gemini API — `gemini-3.5-flash` (réponses rapides) et `gemini-3.1-pro-preview` (mode avancé / conversations profondes)

## Lancer le projet en local

**Prérequis :** [Android Studio](https://developer.android.com/studio)

1. Ouvre le projet dans Android Studio et laisse-le importer/synchroniser Gradle.
2. Ajoute ta clé Gemini nommée `GEMINI_API_KEY` dans le panneau **Secrets** de
   Google AI Studio (elle est injectée au runtime via `BuildConfig.GEMINI_API_KEY`).
   En local hors AI Studio, tu peux aussi la placer dans un fichier `.env`
   à la racine (voir `.env.example`). Sans clé valide, l'app bascule sur le
   simulateur hors-ligne.
3. Lance l'app sur un émulateur ou un appareil physique.

## Roadmap / à améliorer

- [ ] Découper `MainActivity.kt` (monolithe de ~150 Ko) en écrans + navigation.
- [ ] Pour une publication Play Store hors AI Studio, prévoir un backend proxy
      afin de ne pas embarquer la clé Gemini dans l'APK.
- [ ] Changer le `namespace` / `applicationId` générique `com.example`.
- [ ] Ajouter des tests unitaires (ViewModel, Repository) et UI.
- [ ] Préparer la fiche et les assets Play Store.

---

Projet initialement généré via Google AI Studio, puis retravaillé.
