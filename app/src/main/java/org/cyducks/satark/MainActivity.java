package org.cyducks.satark;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewbinding.ViewBinding;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import org.cyducks.satark.auth.viewmodel.AuthViewModel;
import org.cyducks.satark.auth.worker.FetchRoleWorker;
import org.cyducks.satark.dashboard.ui.DriverDashboardFragment;
import org.cyducks.satark.dashboard.ui.OwnerDashboardFragment;
import org.cyducks.satark.dashboard.ui.SelectCityDialog;
import org.cyducks.satark.databinding.CityInputDialogBinding;
import org.cyducks.satark.util.UserRole;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    FirebaseUser currentUser;
    AuthViewModel authViewModel;

    private static final String TAG = "MYAPP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
//        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        AtomicBoolean lock = new AtomicBoolean(false);
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        Log.d(TAG, "In MainActivity");


        authViewModel.getCurrentUser().observe(this, firebaseUser -> {


            if (!lock.get()) {
                this.currentUser = firebaseUser;
                Log.d(TAG, "MainActivity.Observer: " + this.currentUser);

                if (this.currentUser != null) {
                    OneTimeWorkRequest fetchRoleRequest = new OneTimeWorkRequest.Builder(FetchRoleWorker.class)
                            .setInputData(new Data.Builder()
                                    .putString("userId", currentUser.getUid())
                                    .build())
                            .build();



                    WorkManager.getInstance(getApplicationContext())
                            .enqueueUniqueWork(currentUser.getUid(), ExistingWorkPolicy.REPLACE,fetchRoleRequest);

                    WorkManager.getInstance(getApplicationContext())
                            .getWorkInfosForUniqueWorkLiveData(currentUser.getUid())
                            .observe(this, workInfos -> {
                                WorkInfo workInfo = workInfos.get(0);
                                if(workInfo.getState().isFinished() && workInfo.getState().equals(WorkInfo.State.SUCCEEDED)) {
                                    String role = workInfo.getOutputData().getString("role");
                                    if(role == null) {
                                        Log.d(TAG, "MainActivity.onCreate: role not received.");
                                        return;

                                    }
                                    Toast.makeText(this, role, Toast.LENGTH_SHORT).show();

                                    // Check for user's city
                                    if(!SelectCityDialog.isCityStored(this)) {
                                        SelectCityDialog.show(this, city -> {
                                            loadDashboard(role, fragmentTransaction);
                                        });
                                    } else {
                                        loadDashboard(role, fragmentTransaction);
                                    }

                                }
                                else if(workInfo.getState().isFinished() && workInfo.getState().equals(WorkInfo.State.FAILED)) {
                                    if(workInfo.getOutputData().getString("exception").equals("NoSuchElementException")) {
                                        Log.d(TAG, "fetchRoleObserver: " + workInfo.getId() + ": " +workInfo.getState());
                                        Toast.makeText(this, "Redirecting to registration UI...", Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(MainActivity.this, RegistrationActivity.class);
                                        startActivity(intent);
                                        finish();

                                    }
                                    else {
                                        Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "onCreate: " + workInfo.getOutputData().getString("message"));
                                    }
                                }


                            });





                } else {
                    Intent intent = new Intent(this, AuthActivity.class);
                    startActivity(intent);
                    Toast.makeText(this, "You are not logged in", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Not logged in");
                    finish();
                }

                lock.set(true);
            }
        });


        if(getIntent().getExtras() != null) {
            Log.d(TAG, "MainActivity.onCreate: " + getIntent().getExtras().keySet());
            for (String key :
                    getIntent().getExtras().keySet())    {
                Log.d(TAG, key + "=>" + getIntent().getExtras().get(key));
            }
        }

    }

    private void loadDashboard(String role, FragmentTransaction fragmentTransaction) {
        switch (UserRole.valueOf(role.toUpperCase(Locale.getDefault()))) {
            case MODERATOR:
                fragmentTransaction.replace(R.id.dashboard_container, new OwnerDashboardFragment());
                break;
            case CIVILIAN:
                fragmentTransaction.replace(R.id.dashboard_container, DriverDashboardFragment.newInstance(UserRole.CIVILIAN));
                break;
            default:
                Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "MainActivity.onCreate: invalid role - [ " + role + " ]");
        }

        fragmentTransaction.commit();
    }

}

