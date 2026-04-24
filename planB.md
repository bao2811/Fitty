Fitty là ứng dụng Android giúp người dùng xây dựng thể hình cá nhân hóa:
Nhập thông tin cơ thể: chiều cao, cân nặng, mục tiêu (giảm mỡ, tăng cơ…)
Tự động tạo lịch tập luyện cá nhân hóa
Hiển thị bài tập + hướng dẫn (GIF/video)
Theo dõi tiến trình (progress, body change)
Hệ thống achievement, badge, level
Tích hợp AI:
Phân tích body từ ảnh (bạn đã có)
Gợi ý plan thông minh hơn theo tiến độ
com.fitty
├── presentation/
│ ├── ui/
│ ├── viewmodel/
│
├── domain/
│ ├── usecase/
│ ├── model/
│
├── data/
│ ├── repository/
│ ├── local (Room)
│ ├── remote (API / AI)
│
├── core/
│ ├── utils/
│ ├── di/

UI tham khảo:
https://stitch.withgoogle.com/projects/8134373594429455678

3.1 Onboarding & Profile
Nhập:
chiều cao
cân nặng
tuổi
giới tính
Chọn mục tiêu:
giảm mỡ
tăng cơ
giữ dáng
👉 Output:
BMI, BMR
Suggested plan type

🔹 3.2 Workout Plan Engine
Logic:
Rule-based (ban đầu):
Beginner → full body
Intermediate → split push/pull/legs
Sau đó:
AI optimize plan
Data:
WorkoutPlan
WorkoutDay
Exercise
Set/Rep/Rest

🔹 3.3 Exercise Library
Danh sách bài tập:
Chest / Back / Legs / Abs
Mỗi bài có:
name
muscle group
difficulty
GIF / video
instructions
👉 Gợi ý:
Lưu local JSON hoặc Firebase

🔹 3.4 Workout Session (Core Feature)
Start workout
Tick completed sets
Timer nghỉ
Tracking:
calories (estimate)
time

🔹 3.5 Progress Tracking
Weight tracking
Body fat (estimate từ AI)
Charts:
line chart weight
progress photo

🔹 3.6 Achievement System 🏆
Badge:
First workout
7-day streak
100 workouts
Level system:
XP → Level → Unlock feature

🔹 3.7 AI Integration (điểm mạnh của bạn)
Bạn đã có body analysis → mở rộng thêm:
AI feature ideas:
Body scoring
fat %
muscle %
symmetry
Auto feedback
“Bạn cần tập thêm chân”
“Mỡ bụng cao → tăng cardio”
Adaptive plan
AI update plan mỗi tuần
(Advanced)
Pose correction (camera real-time)

🔹 3.8 UI/UX (Material 3)
Dark mode
Dynamic color
Components:
Card (exercise)
Progress bar
Bottom navigation
Tabs:
Home
Workout
Progress
Profile
