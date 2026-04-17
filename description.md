Fitty is designed as an AI-powered fitness and nutrition mobile application that helps users set goals, receive personalized workout plans, track calories from meal images, evaluate body condition, consult an AI chatbot, and stay motivated through streaks, reminders, and achievements.

The proposal below is structured to satisfy the assignment criteria: complete functionality, logical screen flow, proper UI design, uniqueness, performance, error handling, and implementation of advanced features without depending entirely on third-party libraries.

1. Product vision

Fitty is a personalized fitness companion for beginners and intermediate users who want a mobile experience that feels intelligent, motivational, and simple. The app should not look like a generic gym tracker. Instead, it should feel like a personal coach + nutrition assistant + visual progress dashboard.

Core value proposition:

Help users set a realistic health or workout goal
Turn that goal into a clear workout and nutrition plan
Continuously track progress
Use AI to reduce manual work:
meal calories from photo
body assessment
chatbot consultation
Keep users engaged with streaks, reminders, and achievements

The app should communicate three things visually:

Clarity — users always know what to do next
Progress — users can see improvement over time
Personalization — plans and suggestions feel tailored to them 2. Design philosophy

Fitty should follow a modern Compose + Material 3 design language. Official Android guidance recommends using Jetpack Compose, unidirectional data flow, state hoisting, ViewModel as screen-level state holder, Navigation for screen transitions, and Material 3 for consistent theming and UI behavior. For longer lists, Compose recommends lazy components such as LazyColumn and LazyRow, and for deferred background tasks Android recommends WorkManager. DataStore is recommended for preferences and lightweight settings storage, while Health Connect can be used when integrating health and fitness data with user permission. Camera capture can be handled through CameraX.

So the design direction should be:

Minimal but energetic
Data-rich without looking crowded
Large visual cards for motivation
Strong hierarchy
Rounded shapes and health-oriented color system
Animated but lightweight
One primary action per screen
Simple bottom navigation + clear onboarding flow 3. Target users

Fitty can support three primary user types:

User type 1: Beginner

Wants to lose weight or get healthier
Does not know what exercises to do
Needs reminders and encouragement
Benefits most from visual guidance and AI assistant

User type 2: Goal-driven fitness user

Wants muscle gain, fat loss, better endurance
Wants scheduled workouts and streaks
Wants charts and metrics

User type 3: Nutrition-conscious user

Wants calorie and meal tracking
Prefers photo-based food logging instead of manual entry
Needs quick guidance from chatbot

Because of these user groups, the UI must be:

easy enough for beginners
informative enough for motivated users
fast enough for repeated daily use 4. Information architecture

The most logical structure for Fitty is a 5-tab bottom navigation system.

Bottom navigation tabs
Home
Plan
Track
Coach
Profile

This structure is more logical than putting everything into one dashboard because the features are conceptually different:

Home = overview and motivation
Plan = workout planning and schedule
Track = body, meals, calories, progress
Coach = chatbot and AI suggestions
Profile = settings, goals, achievements, preferences

This also aligns well with Compose Navigation patterns where each tab can be its own nested navigation graph.

5. Complete screen flow
   5.1 Splash screen

Purpose:

load app state
check login
check onboarding completion
prefetch user profile and daily plan

UI:

centered Fitty logo
subtle animated pulse ring
tagline: “Your AI fitness partner”
background gradient from deep teal to lime accent

Behavior:

if first launch → onboarding
if logged in and onboarded → Home
if not logged in → Auth
5.2 Authentication flow

Screens:

Welcome
Sign in
Sign up
Forgot password

UI design:

clean white or soft dark surface
large hero illustration of fitness silhouette
segmented switch: Sign In / Create Account
rounded text fields
social login buttons
“Continue as Guest” optional

Basic fields:

email
password
display name
age
gender
height
weight

Why this matters:
User profile data is required for calories estimation, body assessment context, training plans, BMI/TDEE, and reminder personalization.

5.3 Onboarding and goal setup flow

This is one of the most important product flows.

Screens:

Choose your primary goal
Enter body stats
Select training level
Choose available workout days
Choose workout location/equipment
Nutrition preference
Reminders and daily schedule
Preview personalized plan
Screen 1: Goal selection

Choices:

Lose weight
Gain muscle
Maintain fitness
Improve endurance
Improve flexibility
Build healthy habits

UI:

large selectable cards
each card has icon, short explanation, and expected outcome
selected card animates with border glow and scale

Design pattern:

Single-choice card selection pattern
Immediate feedback pattern
Screen 2: Body stats

Fields:

age
sex
height
weight
target weight
body fat estimate optional

UI:

wheel pickers or numeric fields
body silhouette side illustration
Screen 3: Fitness level

Choices:

Beginner
Intermediate
Advanced
Screen 4: Available days

Calendar chips:

Monday to Sunday
select preferred training duration: 20 / 30 / 45 / 60 mins
Screen 5: Equipment

Choices:

Home no equipment
Home basic equipment
Gym full equipment
Screen 6: Nutrition profile

Choices:

Standard
High-protein
Vegetarian
Vegan
Low-carb
Flexible dieting
Screen 7: Reminder schedule

User sets:

wake up time
workout reminder
meal reminder
hydration reminder
sleep reminder
Screen 8: Plan preview

Show:

suggested weekly workout split
calorie target
water target
expected pace
motivational summary

This flow is crucial for the grading criterion of logical function flow because it directly connects user input to downstream app behavior.

6. Main screen designs
   6.1 Home screen

The Home screen should be a smart dashboard, not just a list.

Layout structure

Top section:

greeting: “Good morning, Anna”
profile avatar
streak chip
notification icon

Hero card:

today’s readiness or daily summary
“Today’s Goal: 30 min strength + 2,100 kcal target”
CTA button: “Start today”

Middle content:

Daily progress rings
calories consumed
workout completion
hydration
step progress
Today’s workout card
Recent meal summary
AI insight card
“You consumed more carbs yesterday than usual”
“Consider a protein-rich breakfast today”
Streak and achievement highlight

Bottom section:

shortcut row
Log Meal
Start Workout
AI Body Scan
Ask Coach
Design patterns used
Dashboard pattern
Progressive disclosure
Card-based information grouping
Quick actions pattern
Motivational feedback loop
Visual style
layered cards with rounded corners
progress rings with strong contrast
minimal text but high-value info
use motion for ring progress and streak flames
6.2 Plan screen

This screen should answer: What should I do this week?

Main sections
Weekly calendar strip
Today / This week segmented tab
Workout plan cards
Personalization explanation
Replace workout option
Workout card content
workout title
duration
target area
intensity
estimated calories burned
equipment
CTA: Start / View details / Replace
Workout detail screen

Contains:

exercise list
sets/reps/time
instructions
GIF or simple animation
alternative movement if user has no equipment
notes from AI coach
voice guidance toggle
Personalization logic visible in UI

The app should explain why the plan exists:

“Built for fat loss”
“Based on 4 available workout days”
“Adjusted for beginner level”
“Low-impact due to knee sensitivity”

This increases trust and makes the app feel intelligent.

Advanced UX feature

Add a “Plan Generator Sheet”
User can modify:

available time today
soreness level
energy level
home or gym today

Then the plan updates.

This makes the product more unique and satisfies the requirement for creative, suitable functionality.

6.3 Track screen

Track is the analytics center.

Tabs inside Track:

Meals
Body
Progress
Stats
Meals tab

Functions:

meal image upload or camera capture
AI calorie estimation
food history
macro summary

UI:

top CTA: “Snap your meal”
meal timeline for breakfast/lunch/dinner/snacks
card per meal with:
image thumbnail
food name
estimated calories
confidence score
edit button
Body tab

Functions:

AI body evaluation from user photos
body comparison timeline
circumference or weight logs

UI:

two-column before/after gallery
body scan CTA
body metrics summary:
weight trend
estimated body composition trend
posture note
symmetry note
progress note

Important UX note:
This feature must avoid harmful or judgmental language.
Use neutral phrasing like:

“Progress toward leaner composition”
“Posture may be improved by core and back strengthening”
“Visual changes detected in shoulder definition”
Progress tab

Charts:

weight over time
calories over time
workout adherence
streak days
muscle group frequency
Stats tab

Show:

total workouts
best streak
calories tracked
hours trained
achievements unlocked
6.4 Coach screen

This is the conversational AI hub.

Functions:

chatbot for nutrition
workout advice
recovery advice
weekly planning
daily check-in
Layout

Top:

AI coach avatar
status: “Ready to help”
suggested prompts chips:
“What should I eat after training?”
“Adjust my plan for today”
“I missed yesterday’s session”
“Suggest a 20-minute workout”

Middle:

conversation thread
assistant bubbles
user bubbles
inline cards from AI
meal suggestions
workout modifications
hydration advice

Bottom:

text field
mic button
send button
attach meal photo button
Design patterns
Conversational UI pattern
Suggestion chips
Context-aware action cards
Multimodal assistant pattern
Unique feature

The chatbot should not only reply with text. It should also generate:

mini workout cards
meal cards
weekly adjustment suggestion
“Apply to plan” action button

This is much more useful than a simple chat interface.

6.5 Profile screen

Sections:

avatar and user info
goal summary
current body stats
reminder settings
linked devices / Health Connect
privacy and AI consent
achievements gallery
subscription if any
theme mode
logout

The Profile screen is also where users update:

fitness level
injury notes
allergies
dietary preferences
equipment access 7. Additional core features to make the app more complete

You asked for extra basic functions so the app feels more complete. These should be added:

7.1 Workout session mode

A full-screen session player:

current exercise
timer
sets/reps counter
video/GIF instruction
rest timer
skip / replace / done
heart rate if connected

This turns the plan into an actual usable workout product.

7.2 Daily check-in

Every day the user can answer:

energy today
sleep quality
soreness
motivation level

This allows lighter personalization and adds a “smart coach” feel.

7.3 Water tracker

Simple but useful:

tap-to-log water
daily target
hydration reminders
7.4 Sleep and recovery panel

If user manually logs or integrates device data:

sleep duration
sleep quality
recovery recommendation
7.5 Habit tracker

Examples:

slept before 11pm
drank enough water
no sugary drinks
8k steps
trained today

This reinforces streak behavior.

7.6 Social-lite sharing

Optional:

share achievement card
share streak card
share progress milestone image

No full social network needed, just lightweight sharing.

7.7 Admin/content system

For a production-ready app, exercise content, nutrition tips, and chatbot prompt templates should come from backend-configurable content.

8. Detailed UI components

The app should use a consistent component library.

Core UI components
PrimaryButton
SecondaryButton
OutlinedActionChip
GoalCard
WorkoutCard
MealCard
ProgressRing
MetricTile
InsightCard
AchievementBadge
CoachBubble
EmptyStateView
ErrorStateCard
ImageCaptureBottomSheet
ConfirmationDialog
EditableMetricSheet
Reusable design rules
corner radius: 16–24dp
spacing system: 4 / 8 / 12 / 16 / 24 / 32
large tap targets
clear contrast
strong headline sizes on dashboards
secondary text smaller and muted
cards should never exceed readable density 9. Color system and visual identity

Suggested palette:

Primary: energetic green
Secondary: teal
Accent: warm orange for streaks and achievements
Error: soft red
Background: off-white or deep charcoal in dark mode

Meaning:

green = progress, health, success
teal = trust, calm, wellness
orange = motivation, fire, streak energy

Use Material 3 theming and dynamic color where supported. Material 3 on Android supports updated theming and dynamic personalization, which is a strong fit for a health app.

Typography:

bold, rounded headings
clean sans serif for body text
numbers large and emphasized for metrics

Motion:

soft spring animations for selection
progress ring animation
streak flame pulse
bottom sheet smooth transitions 10. Navigation logic

A logical navigation model is essential for the assignment.

Recommended route graph:

Splash
AuthGraph
Welcome
SignIn
SignUp
OnboardingGraph
Goal
BodyStats
FitnessLevel
Schedule
Equipment
Nutrition
Reminder
PlanPreview
MainGraph
Home
Plan
WorkoutDetails
SessionMode
Track
MealCapture
MealResult
BodyScan
BodyAnalysisDetail
StatsDetail
Coach
Chat
GeneratedPlanResult
Profile
EditProfile
NotificationSettings
ConnectedApps

Compose supports navigation between composables through the Navigation component, while newer Navigation 3 guidance focuses even more on Compose-centric back stack control and adaptive experiences.

11. Recommended Android architecture

To match the course direction and modern Android best practices, use:

Architecture pattern

MVVM + Clean-ish layered architecture + UDF

Structure:

UI layer
Domain layer
Data layer
UI layer

Built with Jetpack Compose.
Each screen gets:

Composable screen
UiState data class
UiEvent
UiEffect for one-time actions
ViewModel

This aligns well with official Compose architecture guidance: Compose works best with unidirectional data flow, state holders, and ViewModel-managed screen state.

Domain layer

Use cases:

GenerateWorkoutPlanUseCase
AnalyzeMealImageUseCase
EstimateCaloriesUseCase
AnalyzeBodyPhotoUseCase
SendChatMessageUseCase
UpdateStreakUseCase
ScheduleReminderUseCase
CalculateTdeeUseCase
Data layer

Repositories:

UserRepository
WorkoutRepository
NutritionRepository
TrackingRepository
ChatRepository
ReminderRepository
CameraRepository

Storage choices:

Room for structured offline data like workouts, meal logs, progress logs
DataStore for preferences such as reminder time, theme, units, onboarding completion, chatbot settings
remote API for AI tasks and cloud sync

DataStore is the official replacement direction for lightweight settings and works with coroutines and Flow.

12. Compose design patterns to explicitly use

Since the assignment asks for design patterns and implementation techniques, these should be stated clearly.

12.1 Unidirectional Data Flow

Every screen:

ViewModel exposes immutable UiState
screen sends events to ViewModel
ViewModel processes logic and updates state
Compose recomposes only the required UI

This is directly aligned with official Compose UI architecture guidance.

12.2 State hoisting

Reusable components like:

meal form
goal picker
workout intensity slider
reminder time picker

should be stateless where possible:

value
onValueChange

Official Compose guidance explicitly recommends hoisting state to the lowest common ancestor and exposing immutable state plus events.

12.3 Single source of truth

The ViewModel holds screen state, not scattered mutable states across multiple composables. Official ViewModel guidance describes it as a screen-level state holder and business logic holder.

12.4 Repository pattern

Repositories isolate data source complexity:

local DB
remote API
cache
AI service
Health Connect
12.5 Use case pattern

Important for advanced features and grading clarity.
Each major feature becomes a domain use case.

12.6 Result-state pattern

Use sealed UI state:

Loading
Success
Empty
Error

Useful for meal scan, body analysis, chatbot reply, and plan generation.

12.7 Optimistic UI

For quick feedback:

meal added to timeline before cloud sync finishes
streak updates instantly after session completion
12.8 Skeleton loading / shimmer

Improves perceived performance on AI-heavy screens.

12.9 Adaptive layout strategy

Even though this is a mobile app, Fitty should still support different screen sizes and foldables. Android provides adaptive layout guidance and adaptive Material 3 libraries for changing navigation and layout based on window configurations.

13. Technology stack
    Core Android technologies
    Kotlin
    Jetpack Compose
    Material 3
    Navigation Compose
    ViewModel
    Coroutines
    Flow
    Room
    DataStore
    WorkManager
    CameraX

These technologies match modern Android architecture and official documentation for Compose apps and background tasks.

Useful supporting technologies
Hilt for dependency injection
Retrofit / Ktor client for APIs
Coil for image loading
Kotlin Serialization
Health Connect integration
ML Kit
TensorFlow Lite optional
Firebase Auth / Firestore optional
Firebase Cloud Messaging for reminders and cloud-triggered notifications optional
Why these choices make sense
Compose simplifies UI and state-driven rendering
ViewModel survives configuration changes
WorkManager is appropriate for deferred background reminders and sync jobs
CameraX simplifies camera integration across devices
DataStore fits lightweight preferences
Health Connect is relevant for fitness data interoperability
ML Kit can support on-device image understanding and pose/body-related assistive features

Official Android docs describe ViewModel for screen-level state, WorkManager for persistent background tasks, DataStore for preferences, CameraX for image capture, and Health Connect for health and fitness data integration. ML Kit provides image labeling, object detection/tracking, and pose detection capabilities.

14. Advanced feature implementation strategy

The assignment specifically asks for advanced functionality not fully dependent on libraries. So the right strategy is: use libraries as building blocks, but implement Fitty-specific logic yourself.

14.1 AI meal calorie tracking by image

Feature flow:

User opens meal capture
Takes photo with CameraX or selects from gallery
App preprocesses image
ML pipeline detects food items
Nutrition logic estimates calories and macros
User confirms or edits result
Store in meal log
UI states
camera ready
capturing
analyzing
results
manual correction
saved
Technical approach

Do not depend only on a single “magic API”.
Use a hybrid pipeline:

CameraX for capture
ML Kit image labeling or object detection as baseline
optional custom TensorFlow Lite model for food classification
your own calorie estimation logic layer using recognized classes + portion heuristics + nutrition database
manual correction UI for reliability

This is much stronger academically than simply calling a third-party app service.

ML Kit officially supports image labeling and object detection/tracking, while CameraX handles photo capture.

UX note

Always show:

recognized foods
confidence
editable quantity
calorie estimate range

This reduces trust issues.

14.2 AI body evaluation

Feature flow:

User opens body assessment
App asks for front/side/back photos
Explain privacy and safe-use notice
Analyze shape/posture/body change indicators
Produce neutral, supportive insights
Save comparison snapshot
Important ethical rule

The feature must not shame users.
It should focus on:

posture
visible symmetry
definition progress
consistency
recommendation areas
Technical approach

Possible hybrid:

CameraX for guided photo capture
pose/keypoint detection with ML Kit pose detection
custom analysis rules
optional segmentation or edge measurement
comparison engine using previous scans

ML Kit documents pose detection as a real-time body position capability.

UI

Use guided frame overlays:

front silhouette outline
pose alignment hint
“Stand straight in full body view”
“Good lighting improves accuracy”

This makes the feature feel professional and improves results.

14.3 Chatbot for nutrition and training

Feature flow:

User writes question
Chat context includes:
goal
recent workouts
calorie trend
meal history
reminder status
AI returns answer
Fitty transforms answer into structured action cards
Technical recommendation

Do not build it as plain text chat only.
Create:

conversation engine
domain prompt builder
tool/action layer
UI renderer for cards

The app logic should decide when to show:

“Add this to my plan”
“Replace tomorrow workout”
“Save this meal suggestion”
“Generate grocery list”

This is how you make the chatbot count as a complex self-built feature rather than a wrapped API.

14.4 Personalized reminders and coaching

Use:

WorkManager for scheduled and persistent background work
optionally AlarmManager for exact time-specific reminders if needed
local notification channels

Android recommends WorkManager for persistent deferred work and documents AlarmManager for time-based operations where appropriate.

Reminder examples:

workout in 30 minutes
log lunch
drink water
continue streak before midnight
weekly progress review
Smart reminder logic

Instead of static reminders, use:

did user already complete workout?
did user already log meals?
time zone
quiet hours
weekend schedule

This makes the app much more intelligent.

15. Personalization engine

A strong Fitty concept needs an explicit personalization model.

Input data:

primary goal
body data
exercise level
workout history
meal logs
body scan summaries
check-ins
available equipment
preferred schedule

Output personalization:

weekly training split
daily workout recommendation
food suggestions
intensity adjustment
reminder timing
recovery advice
chatbot context

This should be represented in code as a UserFitnessProfile and PersonalizationSnapshot.

16. Error handling design

Your assignment explicitly includes “errors that appear,” so this section is very important.

Common error cases and UI handling
16.1 Network error

Examples:

chatbot unavailable
meal analysis API timeout
plan sync failed

UI:

inline error card
retry button
fallback cached data
16.2 Camera permission denied

UI:

educational permission sheet
explain why access is needed
button to open settings
16.3 Poor image quality

For meal scan or body scan:

“Image is too dark”
“Food not fully visible”
“Body not fully inside frame”

UI:

immediate quality feedback before analysis
16.4 AI uncertainty

Never pretend the model is perfectly accurate.
Show:

confidence level
“Please verify the result”
edit options
16.5 Empty states

Examples:

no workouts yet
no meals logged
no streak yet
no body history

UI:

friendly illustration
one clear CTA
16.6 Background task failure

If reminder scheduling fails:

show non-blocking toast/snackbar
log issue
allow manual retry
16.7 Large image or upload failure

Compress locally
retry later
store pending state

This section directly helps the grading area about visible errors and robustness.

17. Performance strategy

The app must be visually rich but still responsive.

Official Compose guidance notes that large data sets should use lazy lists, and Compose performance guidance emphasizes minimizing unnecessary recomposition and structuring state carefully.

So Fitty should use:

LazyColumn / LazyRow for long content
immutable UI state objects
state hoisting
image compression before upload
local caching
pagination for long logs
background analysis off main thread
separate loading states per card, not whole-screen blocking
derived state for computed UI
avoid recomposing the whole dashboard when only one metric changes

For AI-heavy image screens:

preview low-resolution version first
run analysis asynchronously
cache last result
use staged rendering 18. Accessibility and usability

To satisfy design quality:

large touch targets
readable font sizes
semantic labels for icons
support dark mode
strong contrast for progress metrics
avoid relying on color alone
captions for charts
haptic confirmation on important actions
voice-friendly coach interaction optional 19. Suggested database design

Main entities:

User
Goal
WorkoutPlan
WorkoutDay
Exercise
WorkoutSession
MealLog
FoodItem
CalorieEstimation
BodyAssessment
ProgressMetric
Achievement
StreakRecord
ReminderSetting
ChatMessage
CoachSuggestion

Relationships:

one user → many workout sessions
one user → many meal logs
one user → many body assessments
one workout plan → many workout days
one meal log → many food items 20. Example user journey

A good way to prove screen logic is to show one complete flow.

User journey:

User installs Fitty
Signs up
Completes onboarding:
lose weight
beginner
4 days/week
home workouts
Sees weekly plan preview
Lands on Home:
today’s 25-minute cardio + bodyweight session
Logs breakfast by taking photo
AI estimates oatmeal + banana + milk
User corrects quantity
Starts workout session
Completes session and increases streak
Opens chatbot:
“What should I eat for dinner today?”
AI suggests high-protein meal based on calorie budget
End of week:
user sees trend chart and achievement badge

This flow proves the app’s logic is coherent.

21. How this satisfies the assignment criteria
    21.1 Complete functionality

The app includes:

goal setup
workout plan suggestion
stats and reminders
streak and achievements
AI calorie tracking
AI body evaluation
chatbot consultation
plus additional completion features:
workout session mode
hydration
habits
progress analytics
recovery check-in
21.2 Logical screens/functions

The flow is:

onboarding defines profile
profile drives plan
plan drives daily actions
tracking updates progress
AI coach uses tracked data
streaks and achievements reinforce behavior

This is a strong and logical product loop.

21.3 UI follows design requirements

The design uses:

clear hierarchy
Material 3
consistent cards
meaningful color roles
Compose-friendly reusable components
21.4 Unique and suitable design

The unique points are:

AI action cards inside chatbot
visual body progress assessment
meal image + editable confidence flow
adaptive plan generator sheet
daily readiness personalization
21.5 Errors handled

The app explicitly handles:

network issues
permission denial
image quality problems
uncertain AI output
empty states
reminder failures
21.6 Complex features not fully dependent on libraries

The complex parts are not “library only” because:

calorie estimation includes your own post-processing logic
body assessment includes your own rule engine and comparison logic
chatbot includes custom context, actions, and card rendering
reminders use personalized scheduling logic
workout personalization is app-owned logic 22. Recommended implementation roadmap
Phase 1: Foundation
auth
onboarding
home
bottom navigation
profile
local database
settings
Phase 2: Fitness core
workout plan
workout details
workout session mode
streaks
achievements
reminder engine
Phase 3: Nutrition core
meal logging
image capture
calorie result screen
manual correction
Phase 4: AI features
chatbot
body evaluation
recommendation engine
Phase 5: polish
animations
chart screens
accessibility
error handling
performance optimization
adaptive layout improvements 23. Final design recommendation

If you want Fitty to score well, do not present it as only a “fitness tracker.” Present it as:

Fitty — an AI-powered personalized fitness, nutrition, and body progress assistant built with Jetpack Compose using a modern Android architecture.

The strongest version of Fitty is:

visually clean
highly structured
personalized from onboarding
practical for daily use
technically explainable
rich in advanced features
careful about errors and performance

In other words, it should look like a product that could actually be shipped.
