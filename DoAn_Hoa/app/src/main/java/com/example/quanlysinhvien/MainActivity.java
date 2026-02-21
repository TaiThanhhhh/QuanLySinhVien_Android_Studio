package com.example.quanlysinhvien;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

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

        // --- Correctly obtain NavController ---
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();
        // -------------------------------------

        // Define top-level destinations
        Set<Integer> topLevelDestinations = new HashSet<>();
        String userRole = sessionManager.getRole();

        // Set the graph and menu based on the user's role
        if ("ADMIN".equals(userRole)) {
            navController.setGraph(R.navigation.admin_navigation);
            navigationView.inflateMenu(R.menu.drawer_menu);
            topLevelDestinations.add(R.id.nav_dashboard);
            topLevelDestinations.add(R.id.nav_class_management);
            topLevelDestinations.add(R.id.nav_student_management);
            topLevelDestinations.add(R.id.nav_attendance_management);
            topLevelDestinations.add(R.id.nav_reports);
            topLevelDestinations.add(R.id.nav_account);
        } else { // Student
            navController.setGraph(R.navigation.student_navigation);
            navigationView.inflateMenu(R.menu.student_drawer_menu);
            topLevelDestinations.add(R.id.nav_student_dashboard);
            topLevelDestinations.add(R.id.nav_student_history);
        }

        mAppBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations)
                .setOpenableLayout(binding.drawerLayout)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        updateNavHeader(navigationView);
    }

    private void updateNavHeader(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        TextView headerName = headerView.findViewById(R.id.tv_user_name_header);
        TextView headerEmail = headerView.findViewById(R.id.tv_user_email_header);

        long userId = sessionManager.getUserId();
        User user = userRepository.getUserById(userId);

        if (user != null) {
            headerName.setText(user.getName());
            headerEmail.setText(user.getMssv());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.overflow, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.nav_logout) {
            sessionManager.clear();
            redirectToLogin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
