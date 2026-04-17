Absolutely — here is a detailed screen-by-screen UI specification for the Fitty mobile app, written in English and oriented toward Jetpack Compose, Android architecture best practices, and your assignment requirements. The recommendations below are grounded in official Android guidance for Compose UI architecture, state hoisting, Navigation, DataStore, and WorkManager, plus official Google guidance for ML Kit and Health Connect.

1. Suggested tech stack

Core stack

Kotlin
Jetpack Compose
Material 3
Navigation Compose
ViewModel
Kotlin Coroutines + Flow
Room
DataStore
WorkManager
CameraX
Hilt
Coil
Retrofit or Ktor
Health Connect
ML Kit
Optional TensorFlow Lite for more advanced food recognition or posture/body models

This stack fits the official Android direction: Compose works best with unidirectional data flow and ViewModel-based state holders; Navigation Compose is the standard for multi-screen Compose apps; DataStore is the recommended modern solution for app settings; WorkManager is the recommended library for persistent background work; Health Connect is the current Android health data platform; and ML Kit supports pose detection and image labeling for camera-based AI features.

How to split storage

Room: workouts, meal logs, body assessments, progress history, achievements, chat history cache
DataStore: onboarding completion, theme, reminders, selected units, notification preferences, chatbot preferences
Remote API: chatbot, cloud sync, advanced AI inference if on-device models are not enough

DataStore stores key-value or typed objects asynchronously and transactionally using coroutines and Flow, which makes it a strong fit for preferences and lightweight settings.

2. Architecture and Compose implementation model

Use MVVM + repository + use cases + unidirectional data flow.

Recommended layers:

UI layer: Composables, screen state, navigation
Domain layer: use cases such as GenerateWorkoutPlanUseCase, AnalyzeMealImageUseCase, UpdateStreakUseCase
Data layer: repositories, local DB, remote APIs, camera/image pipeline, Health Connect integration

Each screen should have:

UiState data class
UiEvent sealed interface
ViewModel
stateless reusable child composables where possible

Compose officially recommends unidirectional data flow, state holders, and event-driven UI. State should be hoisted to the lowest common ancestor, and when business logic is involved that state owner is often the ViewModel.

Example structure

HomeScreen()
HomeUiState
HomeEvent
HomeViewModel

State rule

Screen state lives in ViewModel
Small local UI-only state can use remember
Input fields can use hoisted value + onValueChange

This follows official state hoisting guidance.

3. Navigation structure and user flow

Use a root navigation graph with three major flows:

Auth flow
Onboarding flow
Main app flow

Inside the main app flow, use bottom navigation with 5 tabs:

Home
Plan
Track
Coach
Profile

Navigation Compose is the standard way to navigate between composables in a Compose app, and Android also provides dedicated Compose components for navigation bars.

Main user flow

Splash → Welcome/Auth
Auth → Onboarding
Onboarding → Plan Preview
Plan Preview → Main App
Main App tabs:
Home → quick access to daily actions
Plan → workouts and sessions
Track → meal/body/progress data
Coach → chatbot
Profile → settings and achievements 4. Exact layout of each screen
4.1 Splash screen

Purpose

initialize app
check auth
check onboarding status
preload user profile and daily dashboard summary

Exact layout

Full-screen vertical gradient background
Center:
Fitty logo
App name: Fitty
Subtitle: Your AI fitness partner
Bottom:
small loading indicator
version text

Buttons / inputs

none

Design pattern

Launch/loading pattern
silent state resolution

Compose notes

Box for full-screen
Column centered
CircularProgressIndicator
navigation side effect from ViewModel once startup checks finish
4.2 Welcome screen

Purpose

first impression
entry to login or account creation

Exact layout

Top 35%:
hero illustration of healthy lifestyle / abstract fitness silhouette
Middle:
headline: Train smarter, eat better, stay consistent
short description
Bottom stacked buttons:
Create Account
Sign In
text button: Continue as Guest

Buttons

Create Account
Sign In
Continue as Guest

Design pattern

Hero onboarding entry
single-primary-action hierarchy

Compose notes

Scaffold
Column
large Button for primary CTA
TextButton for guest mode
4.3 Sign in screen

Exact layout

Top app bar:
back arrow
title: Sign In
Body:
Email text field
Password text field
Forgot password?
Sign In button
divider with “or”
Continue with Google
footer text: Don’t have an account? Create one

Input fields

email
password

Buttons

Sign In
Continue with Google
Forgot password
Create one

Cards / tabs

none

Design pattern

Form pattern
inline validation pattern

Compose notes

OutlinedTextField
password visual transformation
validation errors shown under field
ViewModel exposes email, password, isSubmitting, errorMessage
4.4 Create account screen

Exact layout

Top app bar
Scrollable form:
Full name
Email
Password
Confirm password
Date of birth or age
Gender selector
Height
Weight
checkbox: agree to terms
Create Account button

Input fields

name
email
password
confirm password
age / DOB
gender
height
weight

Buttons

Create Account

Design pattern

Long-form onboarding form
validation with progressive error feedback

Compose notes

LazyColumn
field-specific validation
keep form state in ViewModel
use rememberSaveable only for purely local UI state if needed 5. Onboarding flow
5.1 Goal selection screen

Purpose
User defines primary fitness goal.

Exact layout

Top app bar with step indicator: Step 1 of 7
Title: What is your main goal?
Grid of large selectable cards:
Lose Weight
Gain Muscle
Maintain Fitness
Improve Endurance
Improve Flexibility
Build Healthy Habits
Bottom sticky area:
Continue button

Cards
Each goal card contains:

icon
title
one-line description
selected state border and glow

Design pattern

Single-select choice card pattern
progressive onboarding

Compose notes

LazyVerticalGrid
stateless GoalCard(selected, onClick)
selection state in ViewModel
5.2 Body metrics screen

Exact layout

Top app bar with progress
Title: Tell us about your body
Input section:
Age
Height
Weight
Target weight
Optional body fat %
side illustration or body silhouette
Bottom:
Back
Continue

Input fields

age
height
weight
target weight
body fat optional

Design pattern

Guided data entry
contextual input grouping

Compose notes

Use numeric filtering
LazyColumn
validate realistic ranges
5.3 Fitness level screen

Exact layout

Title: What is your current fitness level?
3 large vertical cards:
Beginner
Intermediate
Advanced
Each card includes examples:
Beginner: “0–2 workouts/week”
Intermediate: “3–4 workouts/week”
Advanced: “5+ structured sessions/week”
Bottom:
Continue

Design pattern

Assisted self-classification
5.4 Weekly availability screen

Exact layout

Title: When can you work out?
Day chips:
Mon Tue Wed Thu Fri Sat Sun
Duration segmented chips:
20 min / 30 min / 45 min / 60 min
Time preference chips:
Morning / Afternoon / Evening
Bottom:
Continue

Buttons / inputs

day chips
duration chips
time preference chips

Design pattern

Multi-select schedule builder

Compose notes

FlowRow or lazy rows for chips
hoist selected values to ViewModel
5.5 Equipment and location screen

Exact layout

Title: Where do you usually train?
Cards:
Home, no equipment
Home, basic equipment
Gym
Mix of home and gym
Secondary section:
optional injury note
optional mobility limitations
Bottom:
Continue

Pattern

personalization setup
conditional form reveal
5.6 Nutrition preference screen

Exact layout

Title: What best matches your eating style?
Choice cards:
Standard
High Protein
Vegetarian
Vegan
Low Carb
Flexible
Optional toggles:
lactose-free
nut allergy
avoid seafood
Continue button

Pattern

single select + optional modifiers
5.7 Reminder preferences screen

Exact layout

Title: Set your reminders
Toggles and time pickers:
Workout reminder [toggle + time]
Meal reminder [toggle + breakfast/lunch/dinner]
Water reminder [toggle + frequency]
Sleep reminder [toggle + time]
Bottom:
Continue

Compose notes

persist preferences to DataStore
later schedule background tasks with WorkManager

WorkManager is recommended for persistent background work that should continue across app restarts and device reboots.

5.8 Plan preview screen

Exact layout

Header: Your Fitty starter plan
Summary cards:
Goal
Calories target
Workout days
Weekly split
Estimated pace
Main hero card:
“Your first week”
Monday: Full body
Wednesday: Cardio + core
Friday: Strength
Saturday: Mobility
CTA:
Start My Plan
secondary text button: Adjust preferences

Design pattern

confirmation summary
explainable personalization 6. Main app screens
6.1 Home screen

Purpose
Daily dashboard.

Exact layout

Top app bar:
greeting: Good morning, Alex
profile avatar
notification bell
Hero card:
Today’s target
readiness summary
button: Start Today
Horizontal progress ring row:
workouts
calories
water
steps
Section: Today’s workout
WorkoutCard
Section: Meals today
quick meal summary card
Section: AI insight
insight card with action
Section: Streak
current streak flame card
Floating action button:

- opens quick actions bottom sheet

Quick actions bottom sheet

Log Meal
Start Workout
Ask Coach
Body Scan

Buttons

Start Today
Quick action items

Cards

Today summary card
Workout card
Meal summary card
AI insight card
Streak card

Design pattern

Dashboard pattern
quick action pattern
motivational feedback pattern

Compose notes

Scaffold
LazyColumn
FloatingActionButton
each section isolated as reusable composable
load sections independently so one failed API does not block entire screen

Compose architecture guidance strongly supports state-driven sections and event-based UI updates.

6.2 Plan screen

Purpose
Shows current workout program.

Exact layout

Top app bar title: My Plan
Tab row:
Today
This Week
Library
Under Today tab:
schedule date strip
Today workout card
“Why this workout?” explanation card
buttons:
Start Workout
Replace
View Details
Under This Week tab:
vertical weekly cards
Under Library tab:
filter chips:
strength
fat loss
mobility
cardio
exercise/workout list

Workout detail screen

Header image/banner
Title + duration + calories + difficulty
Exercise list cards:
exercise name
reps/sets/time
thumbnail
note
Bottom sticky CTA:
Start Session

Session mode screen

Large current exercise panel
timer
set counter
rest timer
buttons:
Previous
Pause
Next
Complete Exercise
collapsible section:
instruction
target muscle
safer variation

Design pattern

Task flow pattern
explainable recommendation pattern

Compose notes

nested navigation inside Plan tab
stable keys for workout lists
use LazyColumn
keep timer state in ViewModel or dedicated session manager
6.3 Track screen

Use top tabs inside Track:

Meals
Body
Progress
Stats
Meals tab

Exact layout

Header row:
title: Meals
button: Scan Meal
Daily calorie summary card
Meal timeline:
Breakfast
Lunch
Dinner
Snacks
Each meal card:
image thumbnail
detected foods
calories
edit icon
confidence badge

Meal capture screen

camera preview
capture button centered at bottom
gallery button left
flash button right
guidance overlay: “Keep the entire meal in frame”

Meal result screen

image preview
detected items chips
estimated calories/macros card
editable quantity rows
buttons:
Save Meal
Re-analyze
Edit Manually

Design pattern

Scan → Review → Confirm
human-in-the-loop AI pattern

Compose notes

CameraX for capture
analysis state:
Idle
Capturing
Analyzing
Success
Error
do not lock the whole app while analyzing

ML Kit image labeling can recognize entities in images and returns confidence values, while custom image models are also supported.

Body tab

Exact layout

Title: Body Progress
primary card:
Start Body Scan
guide card:
front / side / back photo instructions
history carousel:
previous scans
metric summary:
posture note
composition trend
symmetry note
confidence note

Body scan capture screen

full-screen guided overlay
pose frame outline
text hint: “Stand straight, full body visible, good lighting”
button: Capture Front / Side / Back

Body analysis result screen

photo comparison slider or before/after cards
section cards:
posture observation
visible changes
suggested focus area
buttons:
Save Assessment
Retake
Ask Coach

Design pattern

Guided AI capture
privacy-aware analysis flow

Compose notes

pose quality checks before upload
clear consent UI
never use harmful language in results

ML Kit pose detection supports static images and returns body landmarks; for static images it recommends at least 480x360 resolution, and if the subject is not fully in frame the returned landmarks have lower in-frame confidence.

Progress tab

Exact layout

chart cards:
Weight trend
Weekly workouts
Calories tracked
Streak trend
date range selector:
7D / 30D / 90D / 1Y

Design pattern

analytics dashboard
Stats tab

Exact layout

metric tiles in grid:
total workouts
best streak
meals logged
calories tracked
hours trained
achievement gallery
share card button
6.4 Coach screen

Purpose
Chatbot and AI recommendations.

Exact layout

Top app bar:
title: Fitty Coach
settings icon
Suggested prompt chips:
What should I eat after training?
Adjust my workout today
I missed yesterday’s session
Plan my week
Chat area:
conversation bubbles
Bottom composer:
text field
image attach button
microphone button
send button

Special assistant cards inside chat

Workout suggestion card
Meal suggestion card
Grocery list card
Recovery advice card

Buttons

Send
Attach
Mic
Apply to Plan
Save Meal Suggestion

Design pattern

Conversational UI
AI action card pattern
multimodal assistant pattern

Compose notes

LazyColumn(reverseLayout = true) or chat list pattern
input state hoisted
ViewModel maintains message list and pending response state
action cards should produce typed events, not raw string parsing in UI
6.5 Profile screen

Exact layout

Header:
avatar
name
level badge
edit profile button
Section cards:
Current Goal
Body Metrics
Reminder Settings
Nutrition Preferences
Achievements
Linked Health Apps
Privacy & AI
Theme / Units
footer:
Log Out

Buttons

Edit Profile
Manage Goals
Notification Settings
Connect Health Data
Log Out

Design pattern

account center / settings hub

Compose notes

DataStore-backed settings
Health Connect permission flow should be launched only when user explicitly chooses to connect health data

Health Connect is Android’s health and fitness platform, with SDK-based permission handling and data access to supported health and workout types.

7. Reusable components

Use a small design system.

Components:

FittyTopBar
PrimaryButton
SecondaryButton
GoalCard
WorkoutCard
MealCard
MetricTile
ProgressRing
InsightCard
StreakCard
CoachMessageBubble
CoachActionCard
EmptyStateCard
ErrorStateCard
SectionHeader
ChipGroup
DateStrip

Design rules

rounded corners: 16–24dp
spacing: 8 / 12 / 16 / 24
bold, readable metrics
one primary CTA per visible area
avoid clutter 8. Design patterns per screen

Splash

launch state resolver

Auth

validated form pattern

Goal setup

guided onboarding
stepper flow
progressive disclosure

Home

dashboard
quick actions
motivational feedback loop

Plan

task planning
explainable personalization

Track / Meals

scan-review-confirm
AI confidence + user correction

Track / Body

guided capture
privacy-first analysis

Coach

conversational assistant
action card recommendations

Profile

settings hub
editable profile center 9. Background processing and reminder implementation

Use WorkManager for:

workout reminder scheduling
hydration reminder scheduling
streak warning near end of day
background sync for logs
retrying failed uploads

WorkManager is recommended when the task must run reliably even if the app is backgrounded, restarted, or the device reboots.

Reminder logic examples

if workout incomplete by 6 PM → send reminder
if no meals logged by lunch → send meal logging reminder
if current streak is at risk → send motivational alert 10. Performance and implementation notes

Compose screens should be built to avoid unnecessary recomposition. Use immutable UiState, isolate section composables, and prefer lazy containers for feed-like screens. Official Compose guidance emphasizes state-driven rendering and careful state placement, while Android also provides lazy list components for efficient scrolling UIs.

Practical Fitty rules:

use LazyColumn and LazyRow
avoid large bitmap processing on the main thread
compress images before upload
show partial loading per card
cache recent meal/body analysis results
use stable list keys
keep chat rendering incremental
defer WorkManager initialization if you want to optimize startup path, since Android documents on-demand initialization as a startup optimization option. 11. Error handling notes per feature

Meal scan

“Image too dark”
“Food not fully visible”
“Could not estimate calories confidently”
buttons: Retake / Edit Manually

Body scan

“Body not fully inside frame”
“Lighting too poor for accurate analysis”
Retake button

Chatbot

network timeout
show retry inline

Reminders

if schedule creation fails, store pending state and retry

Permissions

educational rationale screen before requesting camera or health data access

This error-state approach fits a robust Compose UI model where screens render Loading, Success, Empty, and Error states rather than assuming success.

12. Suggested package structure
    com.fitty
    ├── core
    │ ├── ui
    │ ├── designsystem
    │ ├── utils
    │ └── model
    ├── data
    │ ├── local
    │ ├── remote
    │ ├── repository
    │ └── mapper
    ├── domain
    │ ├── model
    │ ├── repository
    │ └── usecase
    ├── feature_auth
    ├── feature_onboarding
    ├── feature_home
    ├── feature_plan
    ├── feature_track
    │ ├── meals
    │ ├── body
    │ ├── progress
    │ └── stats
    ├── feature_coach
    ├── feature_profile
    └── navigation
13. Best final positioning for your report

Describe Fitty as:

“Fitty is an AI-powered personalized fitness and nutrition mobile application built with Jetpack Compose. The application combines goal-driven onboarding, personalized workout planning, image-based calorie tracking, body-progress analysis, streak motivation, and an AI chatbot into a cohesive, performance-oriented Android architecture.”

That framing clearly shows:

complete functions
logical screen flow
modern UI/UX
unique advanced features
good engineering decisions
