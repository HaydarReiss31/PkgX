package com.haydareiss.pkgx;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.hamondev.shevery.api.SheveryPrivilege;

import com.haydareiss.pkgx.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_PERMISSION = 1;
    private static final String MANAGER_PACKAGE = "com.hamondev.shevery";
    private static final Pattern PACKAGE_NAME = Pattern.compile("^[A-Za-z0-9_.]+$");

    private ActivityMainBinding binding;
    private AppAdapter adapter;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<AppEntry> all = new ArrayList<>();

    private final SheveryPrivilege.OnBinderReceivedListener binderReceived = this::updateState;
    private final SheveryPrivilege.OnBinderDeadListener binderDead = this::updateState;
    private final SheveryPrivilege.OnPermissionResultListener permissionResult =
            (requestCode, grantResult) -> updateState();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        adapter = new AppAdapter(this::showActions);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);

        binding.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.cbSystem.setOnCheckedChangeListener((b, checked) -> applyFilter());
        binding.btnAction.setOnClickListener(v -> onActionButton());

        // Fast-pull: sunucu çalışıyorsa binder tek IPC çağrısıyla alınır.
        try {
            SheveryPrivilege.connectFast(this);
        } catch (Throwable ignored) {
        }
        SheveryPrivilege.addBinderReceivedListenerSticky(binderReceived);
        SheveryPrivilege.addBinderDeadListener(binderDead);
        SheveryPrivilege.addPermissionResultListener(permissionResult);

        updateState();
        loadApps();
    }

    @Override
    protected void onDestroy() {
        SheveryPrivilege.removeBinderReceivedListener(binderReceived);
        SheveryPrivilege.removeBinderDeadListener(binderDead);
        SheveryPrivilege.removePermissionResultListener(permissionResult);
        io.shutdownNow();
        super.onDestroy();
    }

    // ---------------------------------------------------------------- bağlantı durumu

    private boolean isReady() {
        try {
            return SheveryPrivilege.pingBinder() && SheveryPrivilege.isGranted();
        } catch (Throwable t) {
            return false;
        }
    }

    private void updateState() {
        runOnUiThread(() -> {
            if (binding == null) return;
            boolean alive = SheveryPrivilege.pingBinder();
            if (!alive) {
                binding.status.setText(R.string.state_no_server);
                binding.btnAction.setText(R.string.btn_retry);
                binding.btnAction.setVisibility(View.VISIBLE);
                return;
            }
            boolean granted = false;
            try {
                granted = SheveryPrivilege.isGranted();
            } catch (Throwable ignored) {
            }
            if (granted) {
                int uid = -1;
                try {
                    uid = SheveryPrivilege.getUid();
                } catch (Throwable ignored) {
                }
                binding.status.setText(getString(R.string.state_ready, uid));
                binding.btnAction.setVisibility(View.GONE);
            } else {
                binding.status.setText(R.string.state_no_permission);
                binding.btnAction.setText(R.string.btn_request);
                binding.btnAction.setVisibility(View.VISIBLE);
            }
        });
    }

    private void onActionButton() {
        if (!SheveryPrivilege.pingBinder()) {
            try {
                SheveryPrivilege.connectFast(this);
            } catch (Throwable ignored) {
            }
            updateState();
            return;
        }
        try {
            if (SheveryPrivilege.shouldShowRequestPermissionRationale()) {
                // Kullanıcı "bir daha sorma" ile reddetmiş: izin Shevery'den elle verilmeli.
                Toast.makeText(this, R.string.denied_hint, Toast.LENGTH_LONG).show();
                return;
            }
            SheveryPrivilege.requestPermission(REQ_PERMISSION);
        } catch (Throwable t) {
            Toast.makeText(this, getString(R.string.failed, String.valueOf(t.getMessage())),
                    Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------------- liste

    private void loadApps() {
        binding.progress.setVisibility(View.VISIBLE);
        io.execute(() -> {
            PackageManager pm = getPackageManager();
            @SuppressWarnings("deprecation")
            List<ApplicationInfo> infos = pm.getInstalledApplications(0);
            List<AppEntry> list = new ArrayList<>(infos.size());
            for (ApplicationInfo ai : infos) {
                CharSequence label = ai.loadLabel(pm);
                boolean system = (ai.flags & (ApplicationInfo.FLAG_SYSTEM
                        | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
                list.add(new AppEntry(
                        label != null ? label.toString() : ai.packageName,
                        ai.packageName,
                        ai.loadIcon(pm),
                        system,
                        ai.enabled));
            }
            Collections.sort(list, (a, b) -> a.label.compareToIgnoreCase(b.label));
            main.post(() -> {
                if (isDestroyed()) return;
                all.clear();
                all.addAll(list);
                binding.progress.setVisibility(View.GONE);
                applyFilter();
            });
        });
    }

    private void applyFilter() {
        String q = binding.search.getText().toString().trim().toLowerCase(Locale.ROOT);
        boolean showSystem = binding.cbSystem.isChecked();
        List<AppEntry> filtered = new ArrayList<>();
        for (AppEntry e : all) {
            if (e.system && !showSystem) continue;
            if (!q.isEmpty()
                    && !e.label.toLowerCase(Locale.ROOT).contains(q)
                    && !e.packageName.toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            filtered.add(e);
        }
        adapter.submit(filtered);
        binding.count.setText(getString(R.string.count, filtered.size()));
    }

    // ---------------------------------------------------------------- işlemler

    private boolean isProtected(String pkg) {
        return pkg.equals(getPackageName()) || pkg.equals(MANAGER_PACKAGE) || pkg.equals("android");
    }

    private void showActions(AppEntry e) {
        String[] items = {
                getString(R.string.act_force_stop),
                getString(R.string.act_clear),
                getString(e.enabled ? R.string.act_disable : R.string.act_enable),
                getString(R.string.act_uninstall),
                getString(R.string.act_uninstall_keep),
                getString(R.string.act_info)
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle(e.label)
                .setItems(items, (d, which) -> {
                    if (which == 5) {
                        openAppInfo(e);
                        return;
                    }
                    if (isProtected(e.packageName)) {
                        Toast.makeText(this, R.string.protected_app, Toast.LENGTH_LONG).show();
                        return;
                    }
                    final String pkg = e.packageName;
                    switch (which) {
                        case 0:
                            runCommand(e, R.string.act_force_stop, "am", "force-stop", pkg);
                            break;
                        case 1:
                            confirm(e, () -> runCommand(e, R.string.act_clear, "pm", "clear", pkg));
                            break;
                        case 2:
                            if (e.enabled) {
                                confirm(e, () -> runCommand(e, R.string.act_disable,
                                        "pm", "disable-user", "--user", "0", pkg));
                            } else {
                                runCommand(e, R.string.act_enable, "pm", "enable", "--user", "0", pkg);
                            }
                            break;
                        case 3:
                            confirm(e, () -> runCommand(e, R.string.act_uninstall,
                                    "pm", "uninstall", "--user", "0", pkg));
                            break;
                        case 4:
                            confirm(e, () -> runCommand(e, R.string.act_uninstall_keep,
                                    "pm", "uninstall", "-k", "--user", "0", pkg));
                            break;
                        default:
                            break;
                    }
                })
                .show();
    }

    private void confirm(AppEntry e, Runnable onYes) {
        int message = e.system ? R.string.confirm_system : R.string.confirm_normal;
        new MaterialAlertDialogBuilder(this)
                .setTitle(e.label)
                .setMessage(getString(message, e.label))
                .setPositiveButton(R.string.yes, (d, w) -> onYes.run())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void runCommand(AppEntry e, int labelRes, String... cmd) {
        if (!isReady()) {
            Toast.makeText(this, R.string.not_ready, Toast.LENGTH_LONG).show();
            return;
        }
        if (!PACKAGE_NAME.matcher(e.packageName).matches()) {
            Toast.makeText(this, R.string.protected_app, Toast.LENGTH_LONG).show();
            return;
        }
        binding.progress.setVisibility(View.VISIBLE);
        io.execute(() -> {
            PrivilegedShell.Result r = PrivilegedShell.run(cmd);
            main.post(() -> {
                if (isDestroyed()) return;
                binding.progress.setVisibility(View.GONE);
                String msg = r.ok()
                        ? getString(R.string.done, getString(labelRes), e.label)
                        : getString(R.string.failed, r.summary());
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                loadApps();
            });
        });
    }

    private void openAppInfo(AppEntry e) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", e.packageName, null));
        try {
            startActivity(intent);
        } catch (Throwable t) {
            Toast.makeText(this, getString(R.string.failed, String.valueOf(t.getMessage())),
                    Toast.LENGTH_LONG).show();
        }
    }
}
