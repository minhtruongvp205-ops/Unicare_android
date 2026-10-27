package com.example.btl_android;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private EditText name, phone, dob, gender, address, history;
    private Button save;
    private TextView message;
    private String role = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        App.requireLogin(this);
        FirebaseUser currentUser = App.user();
        if (currentUser == null) return;

        setContentView(R.layout.activity_profile);
        App.applyInsets(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        name = findViewById(R.id.edtName);
        phone = findViewById(R.id.edtPhone);
        dob = findViewById(R.id.edtDob);
        gender = findViewById(R.id.edtGender);
        address = findViewById(R.id.edtAddress);
        history = findViewById(R.id.edtMedicalHistory);
        save = findViewById(R.id.btnSaveProfile);
        message = findViewById(R.id.txtMessage);

        if (save != null) save.setEnabled(false);

        // Date picker for DOB
        if (dob != null) {
            dob.setOnClickListener(v -> showDatePicker());
        }
        TextInputLayout tilDob = findViewById(R.id.tilDob);
        if (tilDob != null) {
            tilDob.setEndIconOnClickListener(v -> showDatePicker());
        }

        // Gender picker (Nam, Nữ, Khác)
        if (gender != null) {
            gender.setOnClickListener(v -> showGenderPicker());
        }
        TextInputLayout tilGender = findViewById(R.id.tilGender);
        if (tilGender != null) {
            tilGender.setOnClickListener(v -> showGenderPicker());
            tilGender.setEndIconOnClickListener(v -> showGenderPicker());
        }

        TextView txtEmail = findViewById(R.id.txtEmail);
        if (txtEmail != null) {
            String emailStr = currentUser.getEmail();
            txtEmail.setText(emailStr != null ? emailStr : "Chưa có");
        }

        App.db().collection("users").document(App.uid()).get()
                .addOnSuccessListener(d -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (!d.exists()) {
                        if (message != null) message.setText("Không có hồ sơ người dùng");
                        return;
                    }
                    role = App.str(d, "role");
                    TextView txtRole = findViewById(R.id.txtRole);
                    if (txtRole != null) {
                        txtRole.setText(role.equals("doctor") ? "👨‍⚕️ Bác sĩ" : "🧑 Bệnh nhân");
                    }
                    if (name != null) name.setText(App.str(d, "name"));
                    if (phone != null) phone.setText(App.str(d, "phone"));
                    if (dob != null) dob.setText(App.str(d, "dob"));
                    if (gender != null) gender.setText(App.str(d, "gender"));
                    if (address != null) address.setText(App.str(d, "address"));
                    if (history != null) history.setText(App.str(d, "medicalHistory"));
                    if (save != null) save.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    App.error(this, e);
                });

        if (save != null) {
            save.setOnClickListener(v -> saveProfile());
        }
    }

    private void showDatePicker() {
        if (isFinishing() || isDestroyed()) return;
        Calendar c = Calendar.getInstance();
        int curYear = c.get(Calendar.YEAR);
        int year = curYear - 20;
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        String currentVal = dob != null ? App.value(dob) : "";
        if (!currentVal.isEmpty() && currentVal.contains("/")) {
            try {
                String[] parts = currentVal.split("/");
                if (parts.length == 3) {
                    int parsedDay = Integer.parseInt(parts[0].trim());
                    int parsedMonth = Integer.parseInt(parts[1].trim()) - 1;
                    int parsedYear = Integer.parseInt(parts[2].trim());
                    if (parsedYear >= 1900 && parsedYear <= curYear
                            && parsedMonth >= 0 && parsedMonth <= 11
                            && parsedDay >= 1 && parsedDay <= 31) {
                        day = parsedDay;
                        month = parsedMonth;
                        year = parsedYear;
                    }
                }
            } catch (Exception ignored) { }
        }

        try {
            DatePickerDialog dpd = new DatePickerDialog(this, (view, y, m, d) -> {
                String selected = String.format(Locale.getDefault(), "%02d/%02d/%04d", d, m + 1, y);
                if (dob != null) dob.setText(selected);
            }, year, month, day);

            dpd.getDatePicker().setMaxDate(System.currentTimeMillis());
            dpd.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showGenderPicker() {
        if (isFinishing() || isDestroyed()) return;
        String[] options = new String[]{"Nam", "Nữ", "Khác"};
        String current = gender != null ? App.value(gender) : "";
        int selectedIndex = -1;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(current)) {
                selectedIndex = i;
                break;
            }
        }

        try {
            new AlertDialog.Builder(this)
                    .setTitle("Chọn giới tính")
                    .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                        if (gender != null) gender.setText(options[which]);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveProfile() {
        if (name == null || App.value(name).length() < 2) {
            if (name != null) name.setError("Họ tên quá ngắn");
            return;
        }
        if (phone != null && !App.value(phone).isEmpty() && !App.value(phone).matches("^0[0-9]{9}$")) {
            phone.setError("SĐT phải gồm 10 số bắt đầu bằng 0");
            return;
        }
        Map<String, Object> changes = new HashMap<>();
        changes.put("name", App.value(name));
        changes.put("phone", phone != null ? App.value(phone) : "");
        changes.put("dob", dob != null ? App.value(dob) : "");
        changes.put("gender", gender != null ? App.value(gender) : "");
        changes.put("address", address != null ? App.value(address) : "");
        changes.put("medicalHistory", history != null ? App.value(history) : "");

        if (save != null) save.setEnabled(false);
        App.db().collection("users").document(App.uid()).update(changes)
                .addOnSuccessListener(x -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (save != null) save.setEnabled(true);
                    if (message != null) message.setText("Đã lưu hồ sơ thành công!");
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (save != null) save.setEnabled(true);
                    App.error(this, e);
                });
    }
}
