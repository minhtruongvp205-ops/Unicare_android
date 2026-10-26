package com.example.btl_android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout tilName, tilEmail, tilPassword;
    private EditText email, password, name;
    private Button btnRegister;
    private TextView message;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_register); App.applyInsets(this);
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        name = findViewById(R.id.edtName);
        email = findViewById(R.id.edtEmail);
        password = findViewById(R.id.edtPassword);
        btnRegister = findViewById(R.id.btnRegister);
        TextView btnGoToLogin = findViewById(R.id.btnGoToLogin);
        message = findViewById(R.id.txtMessage);

        if (!App.configured(this)) { btnRegister.setEnabled(false); return; }

        btnRegister.setOnClickListener(v -> register());
        if (btnGoToLogin != null) {
            btnGoToLogin.setOnClickListener(v -> finish());
        }
    }

    private boolean validate() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        if (message != null) message.setText("");

        String n = App.value(name), e = App.value(email), p = App.value(password);
        boolean valid = true;

        if (n.isEmpty()) {
            tilName.setError("Vui lòng nhập họ tên");
            valid = false;
        } else if (n.length() < 2) {
            tilName.setError("Họ tên cần từ 2 ký tự trở lên");
            valid = false;
        }

        if (e.isEmpty()) {
            tilEmail.setError("Vui lòng nhập email");
            valid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()) {
            tilEmail.setError("Email không đúng định dạng");
            valid = false;
        }

        if (p.isEmpty()) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            valid = false;
        } else if (p.length() < 6) {
            tilPassword.setError("Mật khẩu cần ít nhất 6 ký tự");
            valid = false;
        }

        return valid;
    }

    private void busy(boolean busy) {
        btnRegister.setEnabled(!busy);
    }

    private void register() {
        if (!validate()) return;
        busy(true); message.setText("Đang tạo tài khoản...");
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(App.value(email), App.value(password))
            .addOnSuccessListener(r -> {
                Map<String, Object> data = new HashMap<>();
                data.put("role", "patient"); // Do NOT allow client to create doctor accounts.
                data.put("email", App.value(email));
                data.put("name", App.value(name));
                data.put("phone", ""); data.put("dob", ""); data.put("gender", "");
                data.put("address", ""); data.put("medicalHistory", "");
                data.put("createdAt", FieldValue.serverTimestamp());
                App.db().collection("users").document(App.uid()).set(data)
                    .addOnSuccessListener(v -> { busy(false); openDashboard(); })
                    .addOnFailureListener(e -> {
                        busy(false); message.setText("Đã tạo tài khoản Auth nhưng chưa lưu hồ sơ: " + e.getLocalizedMessage() + ". Liên hệ quản trị viên để bổ sung hồ sơ.");
                        FirebaseAuth.getInstance().signOut();
                    });
            }).addOnFailureListener(e -> { busy(false); message.setText("Lỗi đăng ký: " + e.getLocalizedMessage()); });
    }

    private void openDashboard() {
        Intent i = new Intent(this, DashboardActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}
