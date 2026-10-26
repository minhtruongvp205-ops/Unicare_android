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

/** Màn hình ĐĂNG NHẬP cho bệnh nhân và bác sĩ. */
public class AuthActivity extends AppCompatActivity {
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText edtEmail, edtPassword;
    private MaterialButton btnLogin, btnGoToRegister;
    private TextView txtMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        App.applyInsets(this);
        mapping();

        btnLogin.setOnClickListener(v -> {
            if (validateData()) login();
        });
        btnGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        if (!App.configured(this)) {
            busy(true);
            return;
        }
        if (App.user() != null) {
            busy(true);
            openByRole();
        }
    }

    private void mapping() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);
        txtMessage = findViewById(R.id.txtMessage);
    }

    private boolean validateData() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        txtMessage.setText("");
        String email = App.value(edtEmail);
        String password = App.value(edtPassword);
        boolean valid = true;
        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập email");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không đúng định dạng");
            valid = false;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            valid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Mật khẩu cần ít nhất 6 ký tự");
            valid = false;
        }
        return valid;
    }

    private void busy(boolean isBusy) {
        btnLogin.setEnabled(!isBusy);
        btnGoToRegister.setEnabled(!isBusy);
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

    private void openByRole() {
        if (App.user() == null) {
            busy(false);
            return;
        }
        App.db().collection("users").document(App.uid()).get()
                .addOnSuccessListener(document -> {
                    String role = document.exists() ? App.str(document, "role") : "";
                    if (!"patient".equals(role) && !"doctor".equals(role)) {
                        FirebaseAuth.getInstance().signOut();
                        txtMessage.setText("Tài khoản chưa có vai trò hợp lệ trong Firestore.");
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
