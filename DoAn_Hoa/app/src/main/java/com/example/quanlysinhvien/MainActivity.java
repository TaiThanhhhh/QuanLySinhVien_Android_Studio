package com.example.quanlysinhvien;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.quanlysinhvien.auth.SessionManager;
import com.example.quanlysinhvien.data.model.User;
import com.example.quanlysinhvien.data.repo.UserRepository;
import com.example.quanlysinhvien.databinding.ActivityMainBinding;
import com.example.quanlysinhvien.ui.auth.AuthActivity;
import com.google.android.material.navigation.NavigationView;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private UserRepository userRepository;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        userRepository = new UserRepository(this);

        if (sessionManager.getUserId() == -1) {
            redirectToLogin();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        navController = navHostFragment.getNavController();

        String userRole = sessionManager.getRole();
        Set<Integer> topLevelDestinations = new HashSet<>();

        if ("ADMIN".equals(userRole)) {
            navController.setGraph(R.navigation.admin_navigation);
            navigationView.inflateMenu(R.menu.drawer_menu);
            topLevelDestinations.add(R.id.nav_dashboard);
            topLevelDestinations.add(R.id.nav_class_management);
            topLevelDestinations.add(R.id.nav_student_management);
            topLevelDestinations.add(R.id.nav_reports);
            topLevelDestinations.add(R.id.nav_account);
        } else {
            navController.setGraph(R.navigation.student_navigation);
            navigationView.inflateMenu(R.menu.student_drawer_menu);
            topLevelDestinations.add(R.id.nav_student_dashboard);
            topLevelDestinations.add(R.id.nav_student_history);
            topLevelDestinations.add(R.id.nav_student_qr_scan);
        }

        mAppBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations)
                .setOpenableLayout(drawer)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        // Intercept all navigation item clicks ourselves for full control
        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);
        updateNavHeader(navigationView);

        if (getIntent().getBooleanExtra("START_FACE_ENROLL", false)) {
            navController.navigate(R.id.nav_face_enrollment);
        }
    }

    private void updateNavHeader(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null)
            return;
        TextView headerName = headerView.findViewById(R.id.tv_user_name_header);
        TextView headerEmail = headerView.findViewById(R.id.tv_user_email_header);
        long userId = sessionManager.getUserId();
        User user = userRepository.getUserById(userId);
        if (user != null) {
            if (headerName != null)
                headerName.setText(user.getName());
            if (headerEmail != null)
                headerEmail.setText(user.getMssv());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    private boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        binding.drawerLayout.closeDrawers();

        if (id == R.id.nav_logout) {
            sessionManager.clear();
            redirectToLogin();
            return true;
        }

        // nav_account is declared in the graph but not a nested graph root, navigate
        // directly
        if (id == R.id.nav_account) {
            safeNavigate(R.id.nav_account);
            item.setChecked(true);
            return true;
        }

        // Student sub-items not in topLevelDestinations
        if (id == R.id.nav_student_change_password) {
            safeNavigate(R.id.nav_student_change_password);
            item.setChecked(true);
            return true;
        }

        // All top-level destinations (dashboard, class_management, student_management,
        // reports, student_dashboard, etc.)
        // NavigationUI.onNavDestinationSelected handles launchSingleTop automatically
        boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
        if (!handled) {
            // Fallback: try navigating directly
            safeNavigate(id);
        }
        return true;
    }

    /**
     * Navigate safely, catching any IllegalArgumentException if destination not
     * found
     */
    private void safeNavigate(int destinationId) {
        try {
            navController.navigate(destinationId);
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở màn hình này", Toast.LENGTH_SHORT).show();
        }
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, AuthActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
