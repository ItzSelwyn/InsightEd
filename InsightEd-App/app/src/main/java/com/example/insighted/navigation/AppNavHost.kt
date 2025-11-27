package com.example.insighted.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.insighted.ui.dashboard.DashboardScreen
import com.example.insighted.faceauth.FaceAuthScreen
import com.example.insighted.ui.login.LoginScreen
import com.example.insighted.ui.welcome.WelcomeScreen
import com.example.insighted.ui.attendance.AttendanceScreen
import com.example.insighted.ui.report.ReportScreen
import com.example.insighted.ui.calendar.CalendarScreen
import com.example.insighted.ui.profile.ProfileScreen
import com.example.insighted.viewmodels.UserViewModel

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object FaceAuth : Screen("faceauth")  // Added FaceAuth screen
    object Dashboard : Screen("dashboard")
    object Attendance : Screen("attendance")
    object Report : Screen("report")
    object Calendar : Screen("calendar")
    object Profile : Screen("profile")
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController(), userViewModel: UserViewModel) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(onContinue = {
                navController.navigate(Screen.Login.route)
            })
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { uuid ->
                    userViewModel.uuid.value = uuid  // Save UUID to ViewModel
                    // Navigate to FaceAuth screen first after login success
                    navController.navigate(Screen.FaceAuth.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.FaceAuth.route) {
            FaceAuthScreen(
                onFaceAuthSuccess = {
                    // After face auth success, navigate to Dashboard
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(userViewModel = userViewModel,
                onAttendanceClick = { navController.navigate(Screen.Attendance.route) },
                onReportClick = { navController.navigate(Screen.Report.route) },
                onCalendarClick = { navController.navigate(Screen.Calendar.route) },
                onProfileClick = { navController.navigate(Screen.Profile.route) }
            )
        }
        composable(Screen.Attendance.route) {
            AttendanceScreen(userViewModel = userViewModel, onBack = { navController.popBackStack() })
        }
        composable(Screen.Report.route) {
            ReportScreen(userViewModel = userViewModel, onBack = { navController.popBackStack() },
                onAttendanceClick = { navController.navigate(Screen.Attendance.route) },
                onReportClick = { navController.navigate(Screen.Report.route) },
                onCalendarClick = { navController.navigate(Screen.Calendar.route) },
                onProfileClick = { navController.navigate(Screen.Profile.route) })
        }
        composable(Screen.Calendar.route) {
            CalendarScreen(onBack = { navController.popBackStack() },
                onAttendanceClick = { navController.navigate(Screen.Attendance.route) },
                onReportClick = { navController.navigate(Screen.Report.route) },
                onCalendarClick = { navController.navigate(Screen.Calendar.route) },
                onProfileClick = { navController.navigate(Screen.Profile.route) })
        }
        composable(Screen.Profile.route) {
            ProfileScreen(userViewModel = userViewModel, onBack = { navController.popBackStack() },
                onAttendanceClick = { navController.navigate(Screen.Attendance.route) },
                onReportClick = { navController.navigate(Screen.Report.route) },
                onCalendarClick = { navController.navigate(Screen.Calendar.route) },
                onProfileClick = { navController.navigate(Screen.Profile.route) })
        }
    }
}
