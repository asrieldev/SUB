package com.studentutilityhub.app

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val prefsName = "student_utility_hub"
    private val notificationPermissionRequestCode = 1204
    private val dateTimeFormat = SimpleDateFormat("EEE, d MMM yyyy HH:mm", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
    private val dayNameFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMMM d", Locale.getDefault())
    private val expenseCategories = listOf("Food", "Transport", "School", "Rent", "Bills", "Health", "Shopping", "Entertainment", "Savings", "General")
    private val incomeTypes = listOf("Allowance", "Salary", "Scholarship", "Part-time job", "Freelance", "Family support", "Refund", "Gift", "Other")
    private val scheduleItems = mutableListOf<ScheduleItem>()
    private val tasks = mutableListOf<TaskItem>()
    private val expenses = mutableListOf<Expense>()
    private val incomes = mutableListOf<Income>()
    private val reminderItems = mutableListOf<ReminderItem>()
    private var walletBalance = 0.0
    private var studentName = "Student"
    private var studentEmail = ""
    private var studentSchool = ""
    private var studentCourse = ""
    private var studentCampus = "Campus"
    private var weatherCity = "Campus"
    private var weatherTemp = "14°C"
    private var preferredCurrency = "EUR"
    private var selectedDayOffset = 0
    private var notificationsEnabled = true
    private var notificationLeadMinutes = 15
    private var isSplashVisible = true
    private var isLoggedIn = false
    private var darkMode = false
    private var ecoMode = false
    private var language = "English"
    private var activeTab = "Dashboard"
    private lateinit var root: LinearLayout
    private lateinit var database: AppDatabase
    private var osmMapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = AppDatabase.get(this)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName
        createNotificationChannel()
        requestNotificationPermission()
        loadState()
        saveState()
        scheduleAllNotifications()
        showSplashScreen()
        Handler(Looper.getMainLooper()).postDelayed({
            isSplashVisible = false
            render()
        }, 1500)
    }

    override fun onResume() {
        super.onResume()
        osmMapView?.onResume()
    }

    override fun onPause() {
        osmMapView?.onPause()
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == notificationPermissionRequestCode && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            scheduleAllNotifications()
        }
    }

    private fun render() {
        if (isSplashVisible) {
            showSplashScreen()
            return
        }
        if (!isLoggedIn) {
            showLoginScreen()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = bg()
            window.navigationBarColor = bg()
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg())
        }

        root.addView(header())
        root.addView(content(), LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(nav())
        setContentView(root)
    }

    private fun showSplashScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = Color.BLACK
            window.navigationBarColor = Color.BLACK
        }
        val splash = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK)
            addView(ImageView(context).apply {
                setImageResource(R.drawable.sb_logo)
                background = roundedBackground(Color.WHITE, dp(42))
                setPadding(dp(18), dp(18), dp(18), dp(18))
                startAnimation(AnimationSet(true).apply {
                    addAnimation(AlphaAnimation(0.12f, 1f).apply { duration = 900 })
                    addAnimation(ScaleAnimation(0.82f, 1f, 0.82f, 1f, 1, 0.5f, 1, 0.5f).apply { duration = 900 })
                })
            }, LinearLayout.LayoutParams(dp(150), dp(150)))
            addView(TextView(context).apply {
                text = "SUB"
                textSize = 22f
                setTypeface(null, 1)
                setTextColor(Color.WHITE)
                setPadding(0, dp(16), 0, 0)
            })
            addView(TextView(context).apply {
                text = "Student Utility Board"
                textSize = 14f
                setTextColor(Color.rgb(220, 220, 220))
                setPadding(0, dp(4), 0, 0)
            })
        }
        setContentView(splash)
    }

    private fun showLoginScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = bg()
            window.navigationBarColor = bg()
        }
        val nameInput = EditText(this).apply {
            hint = "Your name"
            setText(if (studentName == "Student") "" else studentName)
            setSingleLine(true)
            textSize = 16f
        }
        val loginRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(bg())
            addView(logoImage(dp(104)))
            addView(TextView(context).apply {
                text = "SUB"
                textSize = 36f
                setTypeface(null, 1)
                setTextColor(fg())
                setPadding(0, dp(14), 0, 0)
            })
            addView(TextView(context).apply {
                text = "Sign in to personalize your student dashboard."
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(muted())
                setPadding(0, dp(8), 0, dp(22))
            })
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(22), dp(20), dp(22), dp(20))
                background = roundedBackground(Color.WHITE, dp(28))
                addView(nameInput)
                addView(actionButton("Continue") {
                    val value = nameInput.text.toString().trim()
                    studentName = value.ifEmpty { "Student" }
                    isLoggedIn = true
                    saveState()
                    render()
                })
            }, LinearLayout.LayoutParams(-1, -2))
        }
        setContentView(loginRoot)
    }

    private fun header(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(22), dp(22), dp(22), dp(14))
        setBackgroundColor(bg())

        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = activeTitle()
                textSize = 28f
                setTextColor(fg())
                setTypeface(null, 1)
            })
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(logoImage(dp(50)).apply {
            contentDescription = "Profile settings"
            setOnClickListener {
                activeTab = "Settings"
                render()
            }
        })
    }

    private fun content(): View = ScrollView(this).apply {
        clipToPadding = false
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(4), dp(18), dp(18))
            when (activeTab) {
                "Schedule" -> scheduleScreen(this)
                "Expenses" -> expensesScreen(this)
                "Reminders" -> remindersScreen(this)
                "Services" -> servicesScreen(this)
                "Settings" -> settingsScreen(this)
                else -> dashboardScreen(this)
            }
        })
    }

    private fun nav(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(8), dp(8), dp(8), dp(8))
        background = roundedBackground(Color.BLACK, dp(28))
        listOf("Dashboard", "Schedule", "Expenses", "Reminders", "Services", "Settings").forEach { tab ->
            addView(Button(context).apply {
                text = shortName(tab)
                textSize = 11f
                setAllCaps(false)
                setTextColor(if (tab == activeTab) Color.BLACK else Color.WHITE)
                background = roundedBackground(if (tab == activeTab) Color.WHITE else Color.TRANSPARENT, dp(18))
                setOnClickListener {
                    activeTab = tab
                    render()
                }
            }, LinearLayout.LayoutParams(0, dp(52), 1f).apply {
                setMargins(dp(2), 0, dp(2), 0)
            })
        }
    }.withNavMargins()

    private fun dashboardScreen(parent: LinearLayout) {
        parent.addView(weatherHeroCard())
        parent.addView(dateStrip())
        parent.addView(dashboardShortcuts())
        parent.addView(metricRow())
        val selectedSchedules = scheduleItems.filter { isSameSelectedDay(it.startTimeMillis) }.sortedBy { it.startTimeMillis }
        val selectedReminders = reminderItems.filter { isSameSelectedDay(it.startTimeMillis) }.sortedBy { it.startTimeMillis }
        parent.addView(sectionTitle("Your plan"))
        if (selectedSchedules.isEmpty() && selectedReminders.isEmpty()) {
            parent.addView(emptyStateCard(
                title = "No plan for ${selectedDayLabel()}",
                message = "Add your first class or reminder to shape the day.",
                actionLabel = "Add class",
                action = { addClassDialog() }
            ))
        } else {
            val planTiles = selectedSchedules.map {
                PlanTile(it.title, timeFormat.format(Date(it.startTimeMillis)), "Class", "Room / campus", pastelYellow())
            } + selectedReminders.map {
                PlanTile(it.title, timeFormat.format(Date(it.startTimeMillis)), "Reminder", "Alert set", Color.WHITE)
            }
            parent.addView(planTileGrid(planTiles))
        }
        parent.addView(sectionTitle("Quick Actions"))
        parent.addView(actionButton("Add class") { addClassDialog() })
        parent.addView(actionButton("Add expense") { addExpenseDialog() })
        parent.addView(actionButton("Add reminder") { addReminderDialog() })
    }

    private fun metricRow(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(metricCard("Classes", scheduleItems.size.toString(), pastelGreen()), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0, 0, dp(7), dp(10)) })
        addView(metricCard("Tasks", (tasks.size + reminderItems.size).toString(), pastelLavender()), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(7), 0, dp(7), dp(10)) })
        addView(metricCard("Balance", money(currentBalance()), pastelYellow()), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(7), 0, 0, dp(10)) })
    }

    private fun scheduleScreen(parent: LinearLayout) {
        parent.addView(sectionTitle("Schedule Planner"))
        parent.addView(actionButton("Add class or study session") { addClassDialog() })
        if (scheduleItems.isEmpty()) {
            parent.addView(emptyStateCard("No classes yet", "Add your first class, lab, or study session.", "Add class") { addClassDialog() })
        }
        scheduleItems.sortedBy { it.startTimeMillis }.forEach { scheduleItem ->
            val index = scheduleItems.indexOf(scheduleItem)
            parent.addView(editableItemCard(
                title = formatSchedule(scheduleItem),
                label = "Class / Study",
                onEdit = { editClassDialog(index) },
                onDelete = { confirmDelete("Delete schedule item?", scheduleItem.title) { deleteClass(index) } },
                onCalendar = { saveClassToCalendar(scheduleItem) }
            ))
        }
    }

    private fun expensesScreen(parent: LinearLayout) {
        val spent = expenses.sumOf { it.amount }
        parent.addView(walletCard(spent))
        parent.addView(actionButton("Add daily expense") { addExpenseDialog() })
        parent.addView(actionButton("Add income") { addIncomeDialog() })
        parent.addView(actionButton("Set starting balance") { setBalanceDialog() })
        parent.addView(financeChartsCard())
        parent.addView(sectionTitle("Income history"))
        if (incomes.isEmpty()) {
            parent.addView(emptyStateCard("No income yet", "Add allowance, salary, scholarship, or another income source.", "Add income") { addIncomeDialog() })
        }
        incomes.forEachIndexed { index, income ->
            parent.addView(editableItemCard(
                title = "${income.type} - ${money(income.amount)}\n${income.note.ifBlank { dateFormat.format(Date(income.createdAtMillis)) }}",
                label = "Income",
                onEdit = { editIncomeDialog(index) },
                onDelete = { confirmDelete("Delete income?", income.type) { deleteIncome(index) } }
            ))
        }
        parent.addView(sectionTitle("Expenses"))
        if (expenses.isEmpty()) {
            parent.addView(emptyStateCard("No expenses yet", "Track your first cost by choosing a category.", "Add expense") { addExpenseDialog() })
        }
        expenses.forEachIndexed { index, expense ->
            parent.addView(editableItemCard(
                title = "${expense.name} - ${money(expense.amount)}\n${expense.category}",
                label = "Transaction",
                onEdit = { editExpenseDialog(index) },
                onDelete = { confirmDelete("Delete expense?", expense.name) { deleteExpense(index) } }
            ))
        }
    }

    private fun remindersScreen(parent: LinearLayout) {
        parent.addView(sectionTitle("Reminder System"))
        parent.addView(actionButton("Add reminder") { addReminderDialog() })
        parent.addView(actionButton("Add task") { addTaskDialog() })
        if (reminderItems.isEmpty()) {
            parent.addView(emptyStateCard("No reminders yet", "Create your first deadline, payment alert, or event reminder.", "Add reminder") { addReminderDialog() })
        }
        reminderItems.sortedBy { it.startTimeMillis }.forEach { reminder ->
            val index = reminderItems.indexOf(reminder)
            parent.addView(editableItemCard(
                title = formatReminder(reminder),
                label = "Reminder",
                onEdit = { editReminderDialog(index) },
                onDelete = { confirmDelete("Delete reminder?", reminder.title) { deleteReminder(index) } },
                onCalendar = { saveReminderToCalendar(reminder) }
            ))
        }
        parent.addView(sectionTitle("Tasks"))
        if (tasks.isEmpty()) {
            parent.addView(emptyStateCard("No tasks yet", "Add a task and keep your study work visible.", "Add task") { addTaskDialog() })
        }
        tasks.forEachIndexed { index, task ->
            parent.addView(editableItemCard(
                title = "${if (task.completed) "Done: " else ""}${task.title}",
                label = "Task",
                onEdit = { editTaskDialog(index) },
                onDelete = { confirmDelete("Delete task?", task.title) { deleteTask(index) } },
                onCalendar = { toggleTask(index) },
                actionLabel = if (task.completed) "Open" else "Done"
            ))
        }
    }

    private fun servicesScreen(parent: LinearLayout) {
        parent.addView(mapPreviewCard())
        parent.addView(sectionTitle("Nearby Student Services"))
        parent.addView(serviceTileGrid())
    }

    private fun settingsScreen(parent: LinearLayout) {
        parent.addView(sectionTitle("Settings & Personalization"))
        parent.addView(profileCard())
        parent.addView(profileEditorPanel())
        parent.addView(actionButton("Log out") {
            isLoggedIn = false
            saveState()
            render()
        })
        parent.addView(sectionTitle("Notifications"))
        parent.addView(CheckBox(this).apply {
            text = "Enable notifications"
            textSize = 17f
            setTextColor(fg())
            isChecked = notificationsEnabled
            setOnCheckedChangeListener { _, checked ->
                notificationsEnabled = checked
                saveState()
                if (checked) scheduleAllNotifications()
                render()
            }
        })
        parent.addView(notificationLeadRow())
        parent.addView(sectionTitle("Data"))
        parent.addView(actionButton("Privacy policy") { showPrivacyDialog() })
        parent.addView(actionButton("Backup / export data") { exportData() })
        parent.addView(CheckBox(this).apply {
            text = "Dark mode"
            textSize = 17f
            setTextColor(fg())
            isChecked = darkMode
            setOnCheckedChangeListener { _, checked ->
                darkMode = checked
                saveState()
                render()
            }
        })
        parent.addView(CheckBox(this).apply {
            text = "Eco mode"
            textSize = 17f
            setTextColor(fg())
            isChecked = ecoMode
            setOnCheckedChangeListener { _, checked ->
                ecoMode = checked
                saveState()
                render()
            }
        })
        parent.addView(actionButton("Switch language: $language") {
            language = if (language == "English") "French" else "English"
            saveState()
            render()
        })
        parent.addView(itemCard("Eco mode reduces motion and uses simpler screen refreshes for battery-friendly demos.", "Mode"))
    }

    private fun profileCard(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(18), dp(22), dp(18))
        background = roundedBackground(Color.WHITE, dp(26))
        addView(TextView(context).apply {
            text = "PROFILE"
            textSize = 11f
            setTypeface(null, 1)
            setTextColor(muted())
        })
        addView(TextView(context).apply {
            text = studentName
            textSize = 26f
            setTypeface(null, 1)
            setTextColor(fg())
            setPadding(0, dp(8), 0, dp(2))
        })
        addView(TextView(context).apply {
            text = "$weatherCity  $weatherTemp"
            textSize = 15f
            setTextColor(muted())
        })
        addView(TextView(context).apply {
            text = listOf(studentEmail, studentSchool, studentCourse, studentCampus, "Currency: $preferredCurrency").filter { it.isNotBlank() }.joinToString("\n")
            textSize = 14f
            setTextColor(muted())
            setPadding(0, dp(8), 0, 0)
        })
    }.withMargins()

    private fun editProfileDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
        }
        val name = EditText(this).apply {
            hint = "Student name"
            setText(studentName)
        }
        val city = EditText(this).apply {
            hint = "Campus or city"
            setText(weatherCity)
        }
        val email = EditText(this).apply {
            hint = "Email"
            setText(studentEmail)
        }
        val school = EditText(this).apply {
            hint = "School"
            setText(studentSchool)
        }
        val course = EditText(this).apply {
            hint = "Course"
            setText(studentCourse)
        }
        val campus = EditText(this).apply {
            hint = "Campus"
            setText(studentCampus)
        }
        layout.addView(name)
        layout.addView(email)
        layout.addView(school)
        layout.addView(course)
        layout.addView(campus)
        layout.addView(city)
        AlertDialog.Builder(this)
            .setTitle("Edit profile")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                studentName = name.text.toString().trim().ifEmpty { "Student" }
                studentEmail = email.text.toString().trim()
                studentSchool = school.text.toString().trim()
                studentCourse = course.text.toString().trim()
                studentCampus = campus.text.toString().trim().ifEmpty { "Campus" }
                weatherCity = city.text.toString().trim().ifEmpty { "Campus" }
                weatherTemp = temperatureForLocation(weatherCity)
                saveState()
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun profileEditorPanel(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(18), dp(22), dp(18))
        background = roundedBackground(Color.WHITE, dp(26))
        addView(TextView(context).apply {
            text = "Edit profile"
            textSize = 20f
            setTypeface(null, 1)
            setTextColor(fg())
        })
        val name = EditText(context).apply {
            hint = "Student name"
            setText(studentName)
        }
        val email = EditText(context).apply {
            hint = "Email"
            setText(studentEmail)
        }
        val school = EditText(context).apply {
            hint = "School"
            setText(studentSchool)
        }
        val course = EditText(context).apply {
            hint = "Course"
            setText(studentCourse)
        }
        val campus = EditText(context).apply {
            hint = "Campus"
            setText(studentCampus)
        }
        val city = EditText(context).apply {
            hint = "Campus or city"
            setText(weatherCity)
        }
        val currency = spinnerFor(listOf("EUR", "USD", "GBP", "XOF", "NGN", "CAD"), preferredCurrency)
        listOf(name, email, school, course, campus, city).forEach { addView(it) }
        addView(TextView(context).apply {
            text = "Preferred currency"
            textSize = 13f
            setTextColor(muted())
            setPadding(0, dp(8), 0, 0)
        })
        addView(currency)
        addView(actionButton("Save profile") {
            studentName = name.text.toString().trim().ifEmpty { "Student" }
            studentEmail = email.text.toString().trim()
            studentSchool = school.text.toString().trim()
            studentCourse = course.text.toString().trim()
            studentCampus = campus.text.toString().trim().ifEmpty { "Campus" }
            weatherCity = city.text.toString().trim().ifEmpty { "Campus" }
            preferredCurrency = selectedSpinnerValue(currency, "EUR")
            weatherTemp = temperatureForLocation(weatherCity)
            saveState()
            render()
        })
    }.withMargins()

    private fun notificationLeadRow(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        listOf(5, 10, 15, 30).forEach { minutes ->
            addView(Button(context).apply {
                text = "${minutes}m"
                textSize = 13f
                setAllCaps(false)
                setTextColor(if (notificationLeadMinutes == minutes) Color.WHITE else Color.BLACK)
                background = roundedBackground(if (notificationLeadMinutes == minutes) Color.BLACK else Color.WHITE, dp(18))
                setOnClickListener {
                    notificationLeadMinutes = minutes
                    saveState()
                    scheduleAllNotifications()
                    render()
                }
            }, LinearLayout.LayoutParams(0, dp(50), 1f).apply { setMargins(dp(3), 0, dp(3), dp(12)) })
        }
    }

    private fun showPrivacyDialog() {
        AlertDialog.Builder(this)
            .setTitle("Privacy policy")
            .setMessage("SUB stores your classes, reminders, tasks, profile, and expenses locally on this device. Calendar export opens your installed calendar app. No Google account, map, or weather data is sent by SUB until external integrations are configured.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun exportData() {
        val export = JSONObject()
            .put("profile", JSONObject()
                .put("name", studentName)
                .put("email", studentEmail)
                .put("school", studentSchool)
                .put("course", studentCourse)
                .put("campus", studentCampus)
                .put("currency", preferredCurrency)
                .put("location", weatherCity))
            .put("walletBalance", walletBalance)
            .put("incomes", JSONArray(incomesToJson()))
            .put("schedules", JSONArray(scheduleItemsToJson()))
            .put("reminders", JSONArray(reminderItemsToJson()))
            .put("tasks", JSONArray(tasksToJson()))
            .put("expenses", JSONArray(expensesToJson()))
            .toString(2)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "SUB backup")
            putExtra(Intent.EXTRA_TEXT, export)
        }, "Export SUB data"))
    }

    private fun addClassDialog() {
        scheduleDialog("Add schedule item", null) { item ->
            scheduleItems.add(item)
            saveState()
            scheduleClassNotification(item)
            render()
        }
    }

    private fun editClassDialog(index: Int) {
        val oldItem = scheduleItems[index]
        scheduleDialog("Edit schedule item", scheduleItems[index]) { item ->
            cancelClassNotification(oldItem)
            scheduleItems[index] = item
            saveState()
            scheduleClassNotification(item)
            render()
        }
    }

    private fun deleteClass(index: Int) {
        cancelClassNotification(scheduleItems[index])
        scheduleItems.removeAt(index)
        saveState()
        render()
    }

    private fun scheduleDialog(title: String, existing: ScheduleItem?, onSave: (ScheduleItem) -> Unit) {
        timedTextDialog(
            dialogTitle = title,
            titleHint = "Class or study session",
            initialTitle = existing?.title.orEmpty(),
            initialTimeMillis = existing?.startTimeMillis ?: defaultTime(9, 0),
            onSave = { itemTitle, startTimeMillis -> onSave(ScheduleItem(existing?.id ?: newItemId(), itemTitle, startTimeMillis)) }
        )
    }

    private fun addExpenseDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 8, 28, 0)
        }
        val name = EditText(this).apply { hint = "Expense name" }
        val category = spinnerFor(expenseCategories, "General")
        val amount = EditText(this).apply {
            hint = "Amount, e.g. 4.50"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        layout.addView(name)
        layout.addView(category)
        layout.addView(amount)
        AlertDialog.Builder(this)
            .setTitle("Add expense")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val parsed = amount.text.toString().toDoubleOrNull()
                if (name.text.isNotBlank() && parsed != null) {
                    expenses.add(Expense(name.text.toString(), parsed, selectedSpinnerValue(category, "General")))
                    saveState()
                    render()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editExpenseDialog(index: Int) {
        val expense = expenses[index]
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 8, 28, 0)
        }
        val name = EditText(this).apply {
            hint = "Expense name"
            setText(expense.name)
        }
        val category = spinnerFor(expenseCategories, expense.category)
        val amount = EditText(this).apply {
            hint = "Amount, e.g. 4.50"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(expense.amount.toString())
        }
        layout.addView(name)
        layout.addView(category)
        layout.addView(amount)
        AlertDialog.Builder(this)
            .setTitle("Edit expense")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val parsed = amount.text.toString().toDoubleOrNull()
                if (name.text.isNotBlank() && parsed != null) {
                    expenses[index] = Expense(name.text.toString(), parsed, selectedSpinnerValue(category, "General"))
                    saveState()
                    render()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteExpense(index: Int) {
        expenses.removeAt(index)
        saveState()
        render()
    }

    private fun addIncomeDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 8, 28, 0)
        }
        val type = spinnerFor(incomeTypes, "Allowance")
        val note = EditText(this).apply { hint = "Note, e.g. May allowance" }
        val input = EditText(this).apply {
            hint = "Income amount, e.g. 100"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        layout.addView(TextView(this).apply {
            text = "Income type"
            textSize = 13f
            setTextColor(muted())
            setPadding(0, 0, 0, dp(4))
        })
        layout.addView(type)
        layout.addView(note)
        layout.addView(input)
        AlertDialog.Builder(this)
            .setTitle("Add income")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                input.text.toString().toDoubleOrNull()?.let {
                    incomes.add(0, Income(it, selectedSpinnerValue(type, "Income"), note.text.toString().trim(), System.currentTimeMillis()))
                    saveState()
                    render()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editIncomeDialog(index: Int) {
        val income = incomes[index]
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 8, 28, 0)
        }
        val type = spinnerFor(incomeTypes, income.type)
        val note = EditText(this).apply {
            hint = "Note"
            setText(income.note)
        }
        val input = EditText(this).apply {
            hint = "Income amount, e.g. 100"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(income.amount.toString())
        }
        layout.addView(type)
        layout.addView(note)
        layout.addView(input)
        AlertDialog.Builder(this)
            .setTitle("Edit income")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                input.text.toString().toDoubleOrNull()?.let {
                    incomes[index] = income.copy(
                        amount = it,
                        type = selectedSpinnerValue(type, "Income"),
                        note = note.text.toString().trim()
                    )
                    saveState()
                    render()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteIncome(index: Int) {
        incomes.removeAt(index)
        saveState()
        render()
    }

    private fun setBalanceDialog() {
        val input = EditText(this).apply {
            hint = "Starting balance, e.g. 450"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(walletBalance.toString())
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle("Set starting balance")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                input.text.toString().toDoubleOrNull()?.let {
                    walletBalance = it
                    saveState()
                    render()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addReminderDialog() {
        reminderDialog("Add reminder", null) { reminder ->
            reminderItems.add(reminder)
            saveState()
            scheduleReminderNotification(reminder)
            render()
        }
    }

    private fun editReminderDialog(index: Int) {
        val oldReminder = reminderItems[index]
        reminderDialog("Edit reminder", reminderItems[index]) { reminder ->
            cancelReminderNotification(oldReminder)
            reminderItems[index] = reminder
            saveState()
            scheduleReminderNotification(reminder)
            render()
        }
    }

    private fun deleteReminder(index: Int) {
        cancelReminderNotification(reminderItems[index])
        reminderItems.removeAt(index)
        saveState()
        render()
    }

    private fun addTaskDialog() {
        inputDialog("Add task", "Example: Read chapter 4") { value ->
            tasks.add(TaskItem(newItemId(), value))
            saveState()
            render()
        }
    }

    private fun editTaskDialog(index: Int) {
        inputDialog("Edit task", "Example: Read chapter 4", tasks[index].title) { value ->
            tasks[index] = tasks[index].copy(title = value)
            saveState()
            render()
        }
    }

    private fun deleteTask(index: Int) {
        tasks.removeAt(index)
        saveState()
        render()
    }

    private fun toggleTask(index: Int) {
        tasks[index] = tasks[index].copy(completed = !tasks[index].completed)
        saveState()
        render()
    }

    private fun inputDialog(title: String, hint: String, initialValue: String = "", onSave: (String) -> Unit) {
        val input = EditText(this).apply {
            this.hint = hint
            setText(initialValue)
            setSelection(text.length)
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) onSave(value)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reminderDialog(title: String, existing: ReminderItem?, onSave: (ReminderItem) -> Unit) {
        timedTextDialog(
            dialogTitle = title,
            titleHint = "Reminder title",
            initialTitle = existing?.title.orEmpty(),
            initialTimeMillis = existing?.startTimeMillis ?: defaultFutureTime(1, 9, 0),
            onSave = { itemTitle, startTimeMillis -> onSave(ReminderItem(existing?.id ?: newItemId(), itemTitle, startTimeMillis)) }
        )
    }

    private fun timedTextDialog(
        dialogTitle: String,
        titleHint: String,
        initialTitle: String,
        initialTimeMillis: Long,
        onSave: (String, Long) -> Unit
    ) {
        val selectedTime = Calendar.getInstance().apply { timeInMillis = initialTimeMillis }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 8, 28, 0)
        }
        val titleInput = EditText(this).apply {
            hint = titleHint
            setText(initialTitle)
            setSelection(text.length)
        }
        val dateButton = Button(this)
        val timeButton = Button(this)

        fun refreshButtons() {
            dateButton.text = "Date: ${dateFormat.format(Date(selectedTime.timeInMillis))}"
            timeButton.text = "Time: ${timeFormat.format(Date(selectedTime.timeInMillis))}"
        }

        dateButton.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedTime.set(Calendar.YEAR, year)
                    selectedTime.set(Calendar.MONTH, month)
                    selectedTime.set(Calendar.DAY_OF_MONTH, day)
                    refreshButtons()
                },
                selectedTime.get(Calendar.YEAR),
                selectedTime.get(Calendar.MONTH),
                selectedTime.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        timeButton.setOnClickListener {
            TimePickerDialog(
                this,
                { _, hour, minute ->
                    selectedTime.set(Calendar.HOUR_OF_DAY, hour)
                    selectedTime.set(Calendar.MINUTE, minute)
                    selectedTime.set(Calendar.SECOND, 0)
                    selectedTime.set(Calendar.MILLISECOND, 0)
                    refreshButtons()
                },
                selectedTime.get(Calendar.HOUR_OF_DAY),
                selectedTime.get(Calendar.MINUTE),
                true
            ).show()
        }
        refreshButtons()

        layout.addView(titleInput)
        layout.addView(dateButton)
        layout.addView(timeButton)
        AlertDialog.Builder(this)
            .setTitle(dialogTitle)
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val itemTitle = titleInput.text.toString().trim()
                if (itemTitle.isNotEmpty()) onSave(itemTitle, selectedTime.timeInMillis)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(title: String, message: String, onDelete: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ -> onDelete() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveClassToCalendar(scheduleItem: ScheduleItem) {
        openCalendarEvent(scheduleItem.title, "Class / Study from StudentUtilityHub", scheduleItem.startTimeMillis, 60)
    }

    private fun saveReminderToCalendar(reminder: ReminderItem) {
        openCalendarEvent(reminder.title, "Reminder from StudentUtilityHub", reminder.startTimeMillis, 30)
    }

    private fun openCalendarEvent(title: String, description: String, beginTimeMillis: Long, durationMinutes: Int) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTimeMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, beginTimeMillis + durationMinutes * 60_000L)
        }
        try {
            startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            AlertDialog.Builder(this)
                .setTitle("Calendar unavailable")
                .setMessage("Install or enable a calendar app to save this item.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun openMapSearch(queryText: String) {
        val query = Uri.encode(queryText)
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query")))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationReceiver.CHANNEL_ID,
                "Student reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Class and reminder alerts from StudentUtilityHub"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationPermissionRequestCode)
        }
    }

    private fun scheduleAllNotifications() {
        if (!notificationsEnabled) return
        scheduleItems.forEach { scheduleClassNotification(it) }
        reminderItems.forEach { scheduleReminderNotification(it) }
    }

    private fun scheduleClassNotification(item: ScheduleItem) {
        if (!notificationsEnabled) return
        scheduleNotification(
            requestCode = classNotificationId(item),
            triggerAtMillis = item.startTimeMillis - notificationLeadMinutes * 60_000L,
            title = "Class soon",
            message = "${item.title} starts at ${timeFormat.format(Date(item.startTimeMillis))}"
        )
    }

    private fun scheduleReminderNotification(item: ReminderItem) {
        if (!notificationsEnabled) return
        scheduleNotification(
            requestCode = reminderNotificationId(item),
            triggerAtMillis = item.startTimeMillis - notificationLeadMinutes * 60_000L,
            title = "Reminder",
            message = item.title
        )
    }

    private fun scheduleNotification(requestCode: Int, triggerAtMillis: Long, title: String, message: String) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        val pendingIntent = notificationPendingIntent(requestCode, title, message)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelClassNotification(item: ScheduleItem) {
        cancelNotification(classNotificationId(item))
    }

    private fun cancelReminderNotification(item: ReminderItem) {
        cancelNotification(reminderNotificationId(item))
    }

    private fun cancelNotification(requestCode: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(notificationPendingIntent(requestCode, "", ""))
    }

    private fun notificationPendingIntent(requestCode: Int, title: String, message: String): PendingIntent {
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(NotificationReceiver.EXTRA_TITLE, title)
            putExtra(NotificationReceiver.EXTRA_MESSAGE, message)
        }
        return PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun classNotificationId(item: ScheduleItem): Int = positiveRequestCode(item.id, 10_000)

    private fun reminderNotificationId(item: ReminderItem): Int = positiveRequestCode(item.id, 20_000)

    private fun positiveRequestCode(id: Long, offset: Int): Int = ((id % 1_000_000L).toInt() + offset).coerceAtLeast(offset)

    private fun weatherHeroCard(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(24), dp(22), dp(24), dp(22))
        background = roundedBackground(if (darkMode) Color.rgb(41, 58, 66) else Color.rgb(232, 244, 222), dp(30))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(context).apply {
                text = weatherTemp
                textSize = 31f
                setTypeface(null, 1)
                setTextColor(Color.rgb(12, 83, 78))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(TextView(context).apply {
                text = weatherCity
                textSize = 15f
                setTypeface(null, 1)
                gravity = Gravity.CENTER
                setTextColor(fg())
                background = roundedStrokeBackground(Color.TRANSPARENT, Color.rgb(166, 191, 154), dp(18), dp(1))
                setPadding(dp(12), dp(7), dp(12), dp(7))
            })
        })
        addView(TextView(context).apply {
            text = "Good ${dayGreeting()}, $studentName!"
            textSize = 24f
            setTypeface(null, 1)
            setTextColor(fg())
            setPadding(0, dp(14), 0, 0)
        })
        addView(TextView(context).apply {
            text = "Tap a day below to view classes, reminders, and campus plans."
            textSize = 14f
            setTextColor(muted())
            setPadding(0, dp(8), 0, dp(14))
        })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(actionPill("Class") { addClassDialog() }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(0, 0, dp(8), 0) })
            addView(actionPill("Reminder") { addReminderDialog() }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(8), 0, 0, 0) })
        })
    }.withMargins()

    private fun dateStrip(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        for (offset in 0..6) {
            addView(dayChip(offset), LinearLayout.LayoutParams(0, dp(78), 1f).apply { setMargins(dp(3), 0, dp(3), dp(12)) })
        }
    }

    private fun dayChip(offset: Int): View = Button(this).apply {
        val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
        text = "${dayNameFormat.format(day.time)}\n${dayNumberFormat.format(day.time)}"
        textSize = 12f
        setAllCaps(false)
        setTextColor(if (offset == selectedDayOffset) Color.WHITE else fg())
        background = roundedBackground(if (offset == selectedDayOffset) Color.BLACK else card(), dp(24))
        setOnClickListener {
            selectedDayOffset = offset
            render()
        }
    }

    private fun planTileGrid(items: List<PlanTile>): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        items.chunked(2).forEach { rowItems ->
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                rowItems.forEachIndexed { index, item ->
                    addView(planTile(item), LinearLayout.LayoutParams(0, dp(174), 1f).apply {
                        setMargins(if (index == 0) 0 else dp(6), 0, if (index == 0) dp(6) else 0, dp(12))
                    })
                }
                if (rowItems.size == 1) {
                    addView(View(context), LinearLayout.LayoutParams(0, dp(174), 1f).apply { setMargins(dp(6), 0, 0, dp(12)) })
                }
            })
        }
    }

    private fun planTile(item: PlanTile): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(16))
        background = roundedBackground(item.color, dp(28))
        addView(TextView(context).apply {
            text = item.label
            textSize = 12f
            setTextColor(accentFor(item.label))
            setTypeface(null, 1)
            background = roundedBackground(Color.argb(70, 255, 255, 255), dp(14))
            setPadding(dp(10), dp(5), dp(10), dp(5))
        })
        addView(TextView(context).apply {
            text = item.title
            textSize = 21f
            setTextColor(fg())
            setTypeface(null, 1)
            setPadding(0, dp(16), 0, dp(8))
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        addView(TextView(context).apply {
            text = "${selectedDayLabel()}\n${item.time}  ${item.detail}"
            textSize = 13f
            setTextColor(fg())
        })
    }

    private fun dashboardShortcuts(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val nextClass = scheduleItems
            .filter { it.startTimeMillis >= System.currentTimeMillis() }
            .minByOrNull { it.startTimeMillis }
        val todaysReminder = reminderItems
            .filter { isSameSelectedDay(it.startTimeMillis) }
            .minByOrNull { it.startTimeMillis }
        val recentTransaction = (incomes.map {
            "Income: ${it.type} ${money(it.amount)}"
        } + expenses.map {
            "Expense: ${it.category} ${money(it.amount)}"
        }).firstOrNull()
        listOf(
            Triple("Next class", nextClass?.let { "${timeFormat.format(Date(it.startTimeMillis))} ${it.title}" } ?: "Add your first class", pastelGreen()),
            Triple("Today", todaysReminder?.title ?: "No reminders today", pastelLavender()),
            Triple("Balance", money(currentBalance()), pastelYellow()),
            Triple("Recent", recentTransaction ?: "No transactions yet", pastelBlue())
        ).chunked(2).forEach { row ->
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                row.forEachIndexed { index, item ->
                    addView(shortcutCard(item.first, item.second, item.third), LinearLayout.LayoutParams(0, dp(116), 1f).apply {
                        setMargins(if (index == 0) 0 else dp(6), 0, if (index == 0) dp(6) else 0, dp(12))
                    })
                }
            })
        }
    }

    private fun shortcutCard(title: String, value: String, color: Int): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = roundedBackground(color, dp(22))
        addView(TextView(context).apply {
            text = title
            textSize = 12f
            setTypeface(null, 1)
            setTextColor(muted())
        })
        addView(TextView(context).apply {
            text = value
            textSize = 17f
            setTypeface(null, 1)
            setTextColor(fg())
            setPadding(0, dp(8), 0, 0)
            maxLines = 2
        })
    }

    private fun emptyStateCard(title: String, message: String, actionLabel: String, action: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(18), dp(22), dp(18))
        background = roundedStrokeBackground(Color.WHITE, Color.rgb(226, 226, 226), dp(24), dp(1))
        addView(TextView(context).apply {
            text = title
            textSize = 20f
            setTypeface(null, 1)
            setTextColor(fg())
        })
        addView(TextView(context).apply {
            text = message
            textSize = 14f
            setTextColor(muted())
            setPadding(0, dp(6), 0, dp(12))
        })
        addView(actionPill(actionLabel, action), LinearLayout.LayoutParams(-1, dp(50)))
    }.withMargins()

    private fun walletCard(spent: Double): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(24), dp(22), dp(24), dp(22))
        background = roundedBackground(if (darkMode) Color.rgb(42, 55, 80) else Color.rgb(186, 213, 252), dp(30))
        addView(TextView(context).apply {
            text = "Student card"
            textSize = 13f
            setTypeface(null, 1)
            setTextColor(Color.rgb(44, 82, 130))
        })
        addView(TextView(context).apply {
            text = money(currentBalance())
            textSize = 34f
            setTypeface(null, 1)
            setTextColor(fg())
            setPadding(0, dp(8), 0, 0)
        })
        addView(TextView(context).apply {
            text = "${money(totalIncome())} income - ${money(spent)} expenses"
            textSize = 14f
            setTextColor(muted())
            setPadding(0, dp(4), 0, dp(16))
        })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(metricCard("Income", money(totalIncome()), pastelGreen()), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0, 0, dp(7), 0) })
            addView(metricCard("Spent", money(spent), pastelCoral()), LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(7), 0, 0, 0) })
        })
    }.withMargins()

    private fun financeChartsCard(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(18), dp(22), dp(18))
        background = roundedBackground(Color.WHITE, dp(26))
        addView(TextView(context).apply {
            text = "Money charts"
            textSize = 20f
            setTypeface(null, 1)
            setTextColor(fg())
        })
        val categories = expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
        if (categories.isEmpty() && incomes.isEmpty()) {
            addView(TextView(context).apply {
                text = "Add income or expenses to see category and cashflow charts."
                textSize = 14f
                setTextColor(muted())
                setPadding(0, dp(8), 0, 0)
            })
            return@apply
        }
        addView(TextView(context).apply {
            text = "Cashflow"
            textSize = 15f
            setTypeface(null, 1)
            setTextColor(fg())
            setPadding(0, dp(14), 0, dp(6))
        })
        val spent = expenses.sumOf { it.amount }
        val maxCash = listOf(totalIncome(), spent, kotlin.math.abs(currentBalance())).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        addView(chartBar("Income", totalIncome(), maxCash, pastelGreen()))
        addView(chartBar("Expenses", spent, maxCash, pastelCoral()))
        addView(chartBar("Balance", currentBalance(), maxCash, pastelYellow()))
        if (categories.isNotEmpty()) {
            addView(TextView(context).apply {
                text = "Categories"
                textSize = 15f
                setTypeface(null, 1)
                setTextColor(fg())
                setPadding(0, dp(16), 0, dp(6))
            })
            val maxCategory = categories.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
            categories.forEach { (category, amount) ->
                addView(chartBar(category, amount, maxCategory, chartColors()[categories.keys.indexOf(category) % chartColors().size]))
            }
        }
    }.withMargins()

    private fun chartBar(label: String, amount: Double, maxAmount: Double, color: Int): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(5), 0, dp(5))
        addView(TextView(context).apply {
            text = "$label  ${money(amount)}"
            textSize = 13f
            setTextColor(fg())
        })
        addView(FrameLayout(context).apply {
            background = roundedBackground(Color.rgb(238, 238, 238), dp(8))
            addView(View(context).apply {
                background = roundedBackground(color, dp(8))
            }, FrameLayout.LayoutParams(((amount.coerceAtLeast(0.0) / maxAmount) * dp(260)).toInt().coerceAtLeast(dp(8)), dp(14)))
        }, LinearLayout.LayoutParams(-1, dp(14)).apply { setMargins(0, dp(4), 0, 0) })
    }

    private fun spendingChartCard(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(18), dp(22), dp(18))
        background = roundedBackground(Color.WHITE, dp(26))
        addView(TextView(context).apply {
            text = "Weekly spending"
            textSize = 20f
            setTypeface(null, 1)
            setTextColor(fg())
        })
        val categories = expenses.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amount } }
        val max = categories.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        if (categories.isEmpty()) {
            addView(TextView(context).apply {
                text = "No transactions yet."
                textSize = 14f
                setTextColor(muted())
                setPadding(0, dp(8), 0, 0)
            })
        } else {
            categories.forEach { (category, amount) ->
                addView(TextView(context).apply {
                    val blocks = ((amount / max) * 12).toInt().coerceAtLeast(1)
                    text = "$category  ${"■".repeat(blocks)}  ${money(amount)}"
                    textSize = 14f
                    setTextColor(fg())
                    setPadding(0, dp(8), 0, 0)
                })
            }
        }
    }.withMargins()

    private fun mapPreviewCard(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(20), dp(22), dp(20))
        background = roundedBackground(if (darkMode) Color.rgb(31, 36, 44) else Color.WHITE, dp(30))
        addView(TextView(context).apply {
            text = "Campus map"
            textSize = 26f
            setTypeface(null, 1)
            setTextColor(fg())
        })
        addView(TextView(context).apply {
            text = "Pick a service below to open nearby places in Maps."
            textSize = 14f
            setTextColor(muted())
            setPadding(0, dp(6), 0, dp(14))
        })
        addView(osmMapPanel())
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(14), 0, 0)
            addView(mapTile("Search", "Find places") { openMapSearch("student services near me") }, LinearLayout.LayoutParams(0, dp(112), 1f).apply { setMargins(0, 0, dp(8), 0) })
            addView(mapTile("Change city", weatherCity) {
                activeTab = "Settings"
                render()
            }, LinearLayout.LayoutParams(0, dp(112), 1f).apply { setMargins(dp(8), 0, 0, 0) })
        })
    }.withMargins()

    private fun osmMapPanel(): View = FrameLayout(this).apply {
        background = roundedBackground(Color.WHITE, dp(24))
        val center = geoPointForLocation(weatherCity)
        val mapView = MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.5)
            controller.setCenter(center)
            addServiceMarkers(this, center)
        }
        osmMapView = mapView
        addView(mapView, FrameLayout.LayoutParams(-1, dp(230)))
    }.apply {
        layoutParams = LinearLayout.LayoutParams(-1, dp(230))
    }

    private fun addServiceMarkers(mapView: MapView, center: GeoPoint) {
        mapView.overlays.clear()
        listOf(
            Triple("Campus", "Main student area", center),
            Triple("Library", "Quiet study", GeoPoint(center.latitude + 0.006, center.longitude - 0.004)),
            Triple("Cafe", "Food break", GeoPoint(center.latitude - 0.004, center.longitude + 0.006)),
            Triple("Bus stop", "Transit", GeoPoint(center.latitude + 0.003, center.longitude + 0.007))
        ).forEach { item ->
            mapView.overlays.add(Marker(mapView).apply {
                position = item.third
                title = item.first
                snippet = item.second
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            })
        }
    }

    private fun geoPointForLocation(location: String): GeoPoint {
        val normalized = location.lowercase(Locale.getDefault())
        return when {
            "paris" in normalized -> GeoPoint(48.8566, 2.3522)
            "new york" in normalized -> GeoPoint(40.7128, -74.0060)
            "london" in normalized -> GeoPoint(51.5072, -0.1276)
            "cotonou" in normalized || "benin" in normalized -> GeoPoint(6.3703, 2.3912)
            "lagos" in normalized -> GeoPoint(6.5244, 3.3792)
            else -> GeoPoint(48.8566, 2.3522)
        }
    }

    private fun serviceTileGrid(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val services = listOf(
            Triple("Cafeterias", "Meals nearby", pastelYellow()),
            Triple("Libraries", "Study spaces", pastelGreen()),
            Triple("Bus stops", "Transit routes", pastelBlue()),
            Triple("ATMs", "Cash points", pastelLavender()),
            Triple("Print shops", "Documents", pastelCoral()),
            Triple("Pharmacy", "Health needs", card())
        )
        services.chunked(2).forEach { rowItems ->
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                rowItems.forEachIndexed { index, service ->
                    addView(serviceTile(service.first, service.second, service.third) {
                        openMapSearch("${service.first} near me")
                    }, LinearLayout.LayoutParams(0, dp(132), 1f).apply {
                        setMargins(if (index == 0) 0 else dp(6), 0, if (index == 0) dp(6) else 0, dp(12))
                    })
                }
            })
        }
    }

    private fun serviceTile(title: String, subtitle: String, color: Int, action: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.BOTTOM
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = roundedBackground(color, dp(24))
        addView(TextView(context).apply {
            text = title.take(1)
            textSize = 16f
            gravity = Gravity.CENTER
            setTypeface(null, 1)
            setTextColor(Color.WHITE)
            background = roundedBackground(Color.BLACK, dp(16))
        }, LinearLayout.LayoutParams(dp(38), dp(38)))
        addView(TextView(context).apply {
            text = title
            textSize = 18f
            setTypeface(null, 1)
            setTextColor(fg())
            setPadding(0, dp(14), 0, dp(2))
        })
        addView(TextView(context).apply {
            text = subtitle
            textSize = 13f
            setTextColor(muted())
        })
        setOnClickListener { action() }
    }

    private fun logoImage(size: Int): ImageView = ImageView(this).apply {
        setImageResource(R.drawable.sb_logo)
        scaleType = ImageView.ScaleType.CENTER_CROP
        background = roundedBackground(Color.WHITE, size / 3)
        setPadding(dp(4), dp(4), dp(4), dp(4))
        layoutParams = LinearLayout.LayoutParams(size, size)
    }

    private fun mapTile(title: String, subtitle: String, action: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.BOTTOM
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = roundedBackground(card(), dp(22))
        addView(TextView(context).apply {
            text = title
            textSize = 19f
            setTypeface(null, 1)
            setTextColor(fg())
        })
        addView(TextView(context).apply {
            text = subtitle
            textSize = 13f
            setTextColor(muted())
        })
        setOnClickListener { action() }
    }

    private fun sectionTitle(textValue: String): View = TextView(this).apply {
        text = textValue
        textSize = 21f
        setTypeface(null, 1)
        setTextColor(fg())
        setPadding(dp(6), dp(20), dp(6), dp(10))
    }

    private fun itemCard(title: String, label: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(18), dp(22), dp(18))
        background = roundedBackground(cardColorFor(label), dp(24))
        addView(TextView(context).apply {
            text = label.uppercase(Locale.getDefault())
            textSize = 11f
            setTextColor(accentFor(label))
            setTypeface(null, 1)
        })
        addView(TextView(context).apply {
            text = title
            textSize = 17f
            setTextColor(fg())
            setPadding(0, dp(6), 0, 0)
        })
    }.withMargins()

    private fun editableItemCard(
        title: String,
        label: String,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onCalendar: (() -> Unit)? = null,
        actionLabel: String = "Calendar"
    ): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(18), dp(22), dp(18))
        background = roundedBackground(cardColorFor(label), dp(24))
        addView(TextView(context).apply {
            text = label.uppercase(Locale.getDefault())
            textSize = 11f
            setTextColor(accentFor(label))
            setTypeface(null, 1)
        })
        addView(TextView(context).apply {
            text = title
            textSize = 17f
            setTextColor(fg())
            setPadding(0, dp(6), 0, dp(10))
        })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            onCalendar?.let { calendarAction ->
                addView(Button(context).apply {
                    text = actionLabel
                    textSize = 13f
                    setAllCaps(false)
                    setTextColor(Color.BLACK)
                    background = roundedStrokeBackground(Color.WHITE, Color.rgb(230, 230, 230), dp(18), dp(1))
                    setOnClickListener { calendarAction() }
                }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(0, 0, dp(8), 0) })
            }
            addView(Button(context).apply {
                text = "Edit"
                textSize = 13f
                setAllCaps(false)
                setTextColor(Color.BLACK)
                background = roundedStrokeBackground(Color.WHITE, Color.rgb(230, 230, 230), dp(18), dp(1))
                setOnClickListener { onEdit() }
            }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(if (onCalendar == null) 0 else dp(8), 0, dp(8), 0) })
            addView(Button(context).apply {
                text = "Delete"
                textSize = 13f
                setAllCaps(false)
                setTextColor(Color.BLACK)
                background = roundedStrokeBackground(Color.WHITE, Color.rgb(230, 230, 230), dp(18), dp(1))
                setOnClickListener { onDelete() }
            }, LinearLayout.LayoutParams(0, dp(52), 1f).apply { setMargins(dp(8), 0, 0, 0) })
        })
    }.withMargins()

    private fun metricCard(label: String, value: String, color: Int): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(10), dp(18), dp(10), dp(18))
        background = roundedBackground(color, dp(22))
        addView(TextView(context).apply {
            text = value
            textSize = 18f
            setTypeface(null, 1)
            setTextColor(fg())
            gravity = Gravity.CENTER
        })
        addView(TextView(context).apply {
            text = label
            textSize = 12f
            setTextColor(muted())
            gravity = Gravity.CENTER
        })
    }

    private fun actionButton(label: String, action: () -> Unit): View = Button(this).apply {
        text = label
        textSize = 15f
        setAllCaps(false)
        setTextColor(Color.BLACK)
        background = roundedStrokeBackground(Color.WHITE, Color.rgb(230, 230, 230), dp(22), dp(1))
        setOnClickListener { action() }
    }.withMargins()

    private fun actionPill(label: String, action: () -> Unit): View = Button(this).apply {
        text = label
        textSize = 14f
        setAllCaps(false)
        setTextColor(Color.BLACK)
        background = roundedStrokeBackground(Color.WHITE, Color.rgb(230, 230, 230), dp(20), dp(1))
        setOnClickListener { action() }
    }

    private fun spinnerFor(options: List<String>, selectedValue: String): Spinner {
        val normalizedOptions = if (selectedValue.isNotBlank() && selectedValue !in options) {
            options + selectedValue
        } else {
            options
        }
        return Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                normalizedOptions
            )
            setSelection(normalizedOptions.indexOf(selectedValue).takeIf { it >= 0 } ?: 0)
        }
    }

    private fun selectedSpinnerValue(spinner: Spinner, fallback: String): String {
        return spinner.selectedItem?.toString()?.trim()?.ifEmpty { fallback } ?: fallback
    }

    private fun View.withMargins(): View {
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, dp(12)) }
        return this
    }

    private fun View.withNavMargins(): View {
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(18), 0, dp(18), dp(14)) }
        return this
    }

    private fun shortName(tab: String): String = when (tab) {
        "Dashboard" -> "Home"
        "Schedule" -> "Plan"
        "Expenses" -> "Money"
        "Reminders" -> "Alerts"
        "Services" -> "Map"
        else -> "Settings"
    }

    private fun activeTitle(): String = when (activeTab) {
        "Schedule" -> "Activities"
        "Expenses" -> "Budget Flow"
        "Reminders" -> "Reminders"
        "Services" -> "Nearby"
        "Settings" -> "Preferences"
        else -> "Student Hub"
    }

    private fun selectedDayMillis(): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, selectedDayOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun selectedDayLabel(): String = monthFormat.format(Date(selectedDayMillis()))

    private fun isSameSelectedDay(timeMillis: Long): Boolean {
        val selected = Calendar.getInstance().apply { timeInMillis = selectedDayMillis() }
        val item = Calendar.getInstance().apply { timeInMillis = timeMillis }
        return selected.get(Calendar.YEAR) == item.get(Calendar.YEAR) &&
            selected.get(Calendar.DAY_OF_YEAR) == item.get(Calendar.DAY_OF_YEAR)
    }

    private fun dayGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 12 -> "Morning"
            hour < 18 -> "Afternoon"
            else -> "Evening"
        }
    }

    private fun temperatureForLocation(location: String): String {
        val normalized = location.lowercase(Locale.getDefault())
        val temp = when {
            "paris" in normalized -> 14
            "new york" in normalized -> 14
            "london" in normalized -> 12
            "cotonou" in normalized || "benin" in normalized -> 29
            "lagos" in normalized -> 30
            "campus" in normalized -> 22
            else -> 21
        }
        return "$temp C"
    }

    private fun formatSchedule(item: ScheduleItem): String = "${timeFormat.format(Date(item.startTimeMillis))} - ${item.title}\n${dateTimeFormat.format(Date(item.startTimeMillis))}"

    private fun formatReminder(item: ReminderItem): String = "${item.title}\n${dateTimeFormat.format(Date(item.startTimeMillis))}"

    private fun totalIncome(): Double = walletBalance + incomes.sumOf { it.amount }

    private fun currentBalance(): Double = totalIncome() - expenses.sumOf { it.amount }

    private fun money(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance(preferredCurrency)
        }.format(value)
    }

    private fun bg(): Int = if (darkMode) Color.rgb(18, 24, 38) else Color.rgb(250, 246, 235)

    private fun card(): Int = if (darkMode) Color.rgb(32, 41, 58) else Color.rgb(255, 251, 241)

    private fun fg(): Int = if (darkMode) Color.WHITE else Color.rgb(24, 32, 48)

    private fun muted(): Int = if (darkMode) Color.rgb(186, 195, 211) else Color.rgb(91, 103, 128)

    private fun primary(): Int = if (ecoMode) Color.rgb(22, 132, 86) else Color.rgb(32, 32, 36)

    private fun pastelPink(): Int = if (darkMode) Color.rgb(70, 47, 68) else Color.rgb(248, 188, 216)

    private fun pastelGreen(): Int = if (darkMode) Color.rgb(46, 72, 61) else Color.rgb(194, 217, 159)

    private fun pastelLavender(): Int = if (darkMode) Color.rgb(55, 56, 91) else Color.rgb(204, 202, 247)

    private fun pastelYellow(): Int = if (darkMode) Color.rgb(82, 72, 40) else Color.rgb(249, 220, 114)

    private fun pastelBlue(): Int = if (darkMode) Color.rgb(45, 66, 86) else Color.rgb(194, 213, 240)

    private fun pastelCoral(): Int = if (darkMode) Color.rgb(91, 51, 58) else Color.rgb(247, 169, 171)

    private fun chartColors(): List<Int> = listOf(
        pastelGreen(),
        pastelCoral(),
        pastelYellow(),
        pastelBlue(),
        pastelLavender(),
        pastelPink(),
        Color.rgb(210, 210, 210)
    )

    private fun cardColorFor(label: String): Int = when {
        darkMode -> card()
        label.contains("Transaction", true) || label.contains("Expense", true) -> Color.WHITE
        label.contains("Schedule", true) || label.contains("Class", true) -> pastelGreen()
        label.contains("Reminder", true) || label.contains("To do", true) -> Color.WHITE
        label.contains("Budget", true) || label.contains("Balance", true) -> pastelYellow()
        label.contains("Mode", true) -> pastelBlue()
        else -> card()
    }

    private fun accentFor(label: String): Int = when {
        label.contains("Transaction", true) || label.contains("Expense", true) -> Color.rgb(79, 88, 108)
        label.contains("Budget", true) -> Color.rgb(135, 96, 12)
        label.contains("Reminder", true) || label.contains("To do", true) -> Color.rgb(83, 74, 166)
        label.contains("Schedule", true) || label.contains("Class", true) -> Color.rgb(50, 111, 79)
        else -> primary()
    }

    private fun roundedBackground(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius.toFloat()
            setColor(color)
        }
    }

    private fun roundedStrokeBackground(color: Int, strokeColor: Int, radius: Int, strokeWidth: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius.toFloat()
            setColor(color)
            setStroke(strokeWidth, strokeColor)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun loadState() {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        darkMode = prefs.getBoolean("darkMode", darkMode)
        ecoMode = prefs.getBoolean("ecoMode", ecoMode)
        language = prefs.getString("language", language) ?: language
        isLoggedIn = prefs.getBoolean("isLoggedIn", isLoggedIn)
        studentName = prefs.getString("studentName", studentName) ?: studentName
        studentEmail = prefs.getString("studentEmail", studentEmail) ?: studentEmail
        studentSchool = prefs.getString("studentSchool", studentSchool) ?: studentSchool
        studentCourse = prefs.getString("studentCourse", studentCourse) ?: studentCourse
        studentCampus = prefs.getString("studentCampus", studentCampus) ?: studentCampus
        preferredCurrency = prefs.getString("preferredCurrency", preferredCurrency) ?: preferredCurrency
        notificationsEnabled = prefs.getBoolean("notificationsEnabled", notificationsEnabled)
        notificationLeadMinutes = prefs.getInt("notificationLeadMinutes", notificationLeadMinutes)
        weatherCity = prefs.getString("weatherCity", weatherCity) ?: weatherCity
        weatherTemp = prefs.getString("weatherTemp", weatherTemp) ?: weatherTemp
        walletBalance = prefs.getFloat("walletBalance", walletBalance.toFloat()).toDouble()
        prefs.getString("tasks", null)?.let { replaceTasks(it) }
        val savedSchedules = database.scheduleDao().getAll()
        val savedReminders = database.reminderDao().getAll()
        val savedExpenses = database.expenseDao().getAll()
        val savedIncomes = database.incomeDao().getAll()
        if (savedSchedules.isNotEmpty() || savedReminders.isNotEmpty() || savedExpenses.isNotEmpty() || savedIncomes.isNotEmpty()) {
            replaceScheduleItems(savedSchedules)
            replaceReminderItems(savedReminders)
            replaceExpenses(savedExpenses)
            replaceIncomes(savedIncomes)
            return
        }
        prefs.getString("classes", null)?.let { replaceScheduleItems(it) }
        prefs.getString("reminders", null)?.let { replaceReminderItems(it) }
        prefs.getString("expenses", null)?.let { replaceExpenses(it) }
        prefs.getString("incomes", null)?.let { replaceIncomes(it) }
    }

    private fun saveState() {
        getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit()
            .putBoolean("darkMode", darkMode)
            .putBoolean("ecoMode", ecoMode)
            .putBoolean("isLoggedIn", isLoggedIn)
            .putString("language", language)
            .putString("studentName", studentName)
            .putString("studentEmail", studentEmail)
            .putString("studentSchool", studentSchool)
            .putString("studentCourse", studentCourse)
            .putString("studentCampus", studentCampus)
            .putString("preferredCurrency", preferredCurrency)
            .putBoolean("notificationsEnabled", notificationsEnabled)
            .putInt("notificationLeadMinutes", notificationLeadMinutes)
            .putString("weatherCity", weatherCity)
            .putString("weatherTemp", weatherTemp)
            .putFloat("walletBalance", walletBalance.toFloat())
            .putString("classes", scheduleItemsToJson())
            .putString("tasks", tasksToJson())
            .putString("reminders", reminderItemsToJson())
            .putString("expenses", expensesToJson())
            .putString("incomes", incomesToJson())
            .apply()
        saveRecordsToDatabase()
    }

    private fun saveRecordsToDatabase() {
        database.runInTransaction {
            database.scheduleDao().deleteAll()
            database.scheduleDao().insertAll(scheduleItems.map { ScheduleEntity(it.id, it.title, it.startTimeMillis) })
            database.reminderDao().deleteAll()
            database.reminderDao().insertAll(reminderItems.map { ReminderEntity(it.id, it.title, it.startTimeMillis) })
            database.expenseDao().deleteAll()
            database.expenseDao().insertAll(expenses.map { ExpenseEntity(name = it.name, amount = it.amount, category = it.category) })
            database.incomeDao().deleteAll()
            database.incomeDao().insertAll(incomes.map { IncomeEntity(amount = it.amount, type = it.type, note = it.note, createdAtMillis = it.createdAtMillis) })
        }
    }

    private fun replaceTasks(json: String) {
        tasks.clear()
        val values = JSONArray(json)
        for (index in 0 until values.length()) {
            val value = values.get(index)
            if (value is JSONObject) {
                tasks.add(TaskItem(value.optLong("id", newItemId()), value.getString("title"), value.optBoolean("completed", false)))
            } else {
                tasks.add(TaskItem(newItemId(), value.toString()))
            }
        }
    }

    private fun replaceScheduleItems(json: String) {
        scheduleItems.clear()
        val values = JSONArray(json)
        for (index in 0 until values.length()) {
            val value = values.get(index)
            if (value is JSONObject) {
                scheduleItems.add(ScheduleItem(
                    value.optLong("id", newItemId()),
                    value.getString("title"),
                    value.getLong("startTimeMillis")
                ))
            } else {
                scheduleItems.add(scheduleItemFromLegacyText(value.toString()))
            }
        }
    }

    private fun replaceScheduleItems(items: List<ScheduleEntity>) {
        scheduleItems.clear()
        items.forEach {
            scheduleItems.add(ScheduleItem(it.id, it.title, it.startTimeMillis))
        }
    }

    private fun replaceReminderItems(json: String) {
        reminderItems.clear()
        val values = JSONArray(json)
        for (index in 0 until values.length()) {
            val value = values.get(index)
            if (value is JSONObject) {
                reminderItems.add(ReminderItem(
                    value.optLong("id", newItemId()),
                    value.getString("title"),
                    value.getLong("startTimeMillis")
                ))
            } else {
                reminderItems.add(ReminderItem(newItemId(), value.toString(), defaultFutureTime(index + 1, 9, 0)))
            }
        }
    }

    private fun replaceReminderItems(items: List<ReminderEntity>) {
        reminderItems.clear()
        items.forEach {
            reminderItems.add(ReminderItem(it.id, it.title, it.startTimeMillis))
        }
    }

    private fun replaceExpenses(json: String) {
        expenses.clear()
        val values = JSONArray(json)
        for (index in 0 until values.length()) {
            val expense = values.getJSONObject(index)
            expenses.add(Expense(expense.getString("name"), expense.getDouble("amount"), expense.optString("category", "General")))
        }
    }

    private fun replaceExpenses(items: List<ExpenseEntity>) {
        expenses.clear()
        items.forEach {
            expenses.add(Expense(it.name, it.amount, it.category))
        }
    }

    private fun replaceIncomes(json: String) {
        incomes.clear()
        val values = JSONArray(json)
        for (index in 0 until values.length()) {
            val income = values.getJSONObject(index)
            incomes.add(Income(
                income.getDouble("amount"),
                income.optString("type", "Income"),
                income.optString("note", ""),
                income.optLong("createdAtMillis", System.currentTimeMillis())
            ))
        }
    }

    private fun replaceIncomes(items: List<IncomeEntity>) {
        incomes.clear()
        items.forEach {
            incomes.add(Income(it.amount, it.type, it.note, it.createdAtMillis))
        }
    }

    private fun tasksToJson(): String {
        val json = JSONArray()
        tasks.forEach {
            json.put(JSONObject().put("id", it.id).put("title", it.title).put("completed", it.completed))
        }
        return json.toString()
    }

    private fun scheduleItemsToJson(): String {
        val json = JSONArray()
        scheduleItems.forEach {
            json.put(JSONObject().put("id", it.id).put("title", it.title).put("startTimeMillis", it.startTimeMillis))
        }
        return json.toString()
    }

    private fun reminderItemsToJson(): String {
        val json = JSONArray()
        reminderItems.forEach {
            json.put(JSONObject().put("id", it.id).put("title", it.title).put("startTimeMillis", it.startTimeMillis))
        }
        return json.toString()
    }

    private fun expensesToJson(): String {
        val json = JSONArray()
        expenses.forEach {
            json.put(JSONObject().put("name", it.name).put("amount", it.amount).put("category", it.category))
        }
        return json.toString()
    }

    private fun incomesToJson(): String {
        val json = JSONArray()
        incomes.forEach {
            json.put(JSONObject()
                .put("amount", it.amount)
                .put("type", it.type)
                .put("note", it.note)
                .put("createdAtMillis", it.createdAtMillis))
        }
        return json.toString()
    }

    private fun scheduleItemFromLegacyText(text: String): ScheduleItem {
        val parts = text.split(" - ", limit = 2)
        val title = parts.getOrNull(1) ?: text
        val timeText = parts.firstOrNull()
        return ScheduleItem(newItemId(), title, millisFromTimeText(timeText))
    }

    private fun millisFromTimeText(timeText: String?): Long {
        val calendar = Calendar.getInstance()
        val match = Regex("""^(\d{1,2}):(\d{2})""").find(timeText.orEmpty())
        if (match != null) {
            calendar.set(Calendar.HOUR_OF_DAY, match.groupValues[1].toInt())
            calendar.set(Calendar.MINUTE, match.groupValues[2].toInt())
        } else {
            calendar.add(Calendar.HOUR_OF_DAY, 1)
            calendar.set(Calendar.MINUTE, 0)
        }
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun defaultTime(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun defaultFutureTime(daysFromNow: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysFromNow)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun newItemId(): Long = System.currentTimeMillis() + (Math.random() * 100_000).toLong()

    data class ScheduleItem(val id: Long, val title: String, val startTimeMillis: Long)

    data class ReminderItem(val id: Long, val title: String, val startTimeMillis: Long)

    data class Expense(val name: String, val amount: Double, val category: String = "General")

    data class Income(val amount: Double, val type: String = "Income", val note: String = "", val createdAtMillis: Long = System.currentTimeMillis())

    data class PlanTile(val title: String, val time: String, val label: String, val detail: String, val color: Int)

    data class TaskItem(val id: Long, val title: String, val completed: Boolean = false)
}
