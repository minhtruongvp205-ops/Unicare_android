package com.example.btl_android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

/** Màn hình đăng nhập/đăng ký: mapping(), validateData() theo bài mẫu trên lớp. */
public class AuthActivity extends AppCompatActivity {
    private TextInputLayout tilEmail, tilPassword, tilName;
    private TextInputEditText edtEmail, edtPassword, edtName;
    private MaterialButton btnLogin, btnRegister;
    private TextView txtMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        App.applyInsets(this);
        mapping();

        if (!App.configured(this)) {
            busy(true);
            return;
        }
        if (App.user() != null) openByRole();

        btnLogin.setOnClickListener(v -> {
            if (validateData(false)) login();
        });
        btnRegister.setOnClickListener(v -> {
            if (validateData(true)) register();
        });
    }

    private void mapping() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilName = findViewById(R.id.tilName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtName = findViewById(R.id.edtName);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        txtMessage = findViewById(R.id.txtMessage);
    }

    private boolean validateData(boolean isRegister) {
        boolean isValid = true;
        String email = App.value(edtEmail);
        String password = App.value(edtPassword);
        String name = App.value(edtName);

        tilEmail.setError(null);
        tilPassword.setError(null);
        tilName.setError(null);

        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập email");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không đúng định dạng");
            isValid = false;
        }

        if (password.isEmpty()) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Mật khẩu cần ít nhất 6 ký tự");
            isValid = false;
        }

        if (isRegister && name.length() < 2) {
            tilName.setError("Vui lòng nhập đầy đủ họ tên");
            isValid = false;
        }
        return isValid;
    }

    private void busy(boolean isBusy) {
        btnLogin.setEnabled(!isBusy);
        btnRegister.setEnabled(!isBusy);
    }

    private void login() {
        busy(true);
        txtMessage.setText("Đang đăng nhập...");
        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(App.value(edtEmail), App.value(edtPassword))
                .addOnSuccessListener(result -> openByRole())
                .addOnFailureListener(error -> {
                    busy(false);
                    txtMessage.setText("Đăng nhập thất bại: " + error.getLocalizedMessage());
                });
    }

    private void register() {
        busy(true);
        txtMessage.setText("Đang tạo tài khoản...");
        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(App.value(edtEmail), App.value(edtPassword))
                .addOnSuccessListener(result -> {
                    Map<String, Object> data = new HashMap<>();
                    // Người dùng chỉ được tự đăng ký vai trò bệnh nhân.
                    data.put("role", "patient");
                    data.put("email", App.value(edtEmail));
                    data.put("name", App.value(edtName));
                    data.put("phone", "");
                    data.put("dob", "");
                    data.put("gender", "");
                    data.put("address", "");
                    data.put("medicalHistory", "");
                    data.put("createdAt", FieldValue.serverTimestamp());

                    App.db().collection("users").document(App.uid()).set(data)
                            .addOnSuccessListener(ref -> {
                                busy(false);
                                openByRole();
                            })
                            .addOnFailureListener(error -> {
                                busy(false);
                                txtMessage.setText("Đã tạo tài khoản nhưng chưa lưu hồ sơ: "
                                        + error.getLocalizedMessage());
                                FirebaseAuth.getInstance().signOut();
                            });
                })
                .addOnFailureListener(error -> {
                    busy(false);
                    txtMessage.setText("Lỗi đăng ký: " + error.getLocalizedMessage());
                });
    }

    private void openByRole() {
        if (App.user() == null) return;
        App.db().collection("users").document(App.uid()).get()
                .addOnSuccessListener(document -> {
                    String role = App.str(document, "role");
                    if (!document.exists() || (!"patient".equals(role) && !"doctor".equals(role))) {
                        FirebaseAuth.getInstance().signOut();
                        txtMessage.setText("Tài khoản chưa được cấp vai trò hợp lệ trong Firestore.");
                        busy(false);
                        return;
                    }
                    Intent intent = new Intent(this, DashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .addOnFailureListener(error -> {
                    busy(false);
                    App.error(this, error);
                });
    }
}
