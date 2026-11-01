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

/** Chỉ cho phép tự đăng ký tài khoản BỆNH NHÂN. */
public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout tilName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText edtName, edtEmail, edtPassword, edtConfirmPassword;
    private MaterialButton btnRegister;
    private TextView txtMessage, btnBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        App.applyInsets(this);
        mapping();

        if (!App.configured(this)) {
            busy(true);
            return;
        }
        btnRegister.setOnClickListener(v -> {
            if (validateData()) registerPatient();
        });
        btnBackToLogin.setOnClickListener(v -> finish());
    }

    private void mapping() {
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
        txtMessage = findViewById(R.id.txtMessage);
    }

    private boolean validateData() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        txtMessage.setText("");
        String name = App.value(edtName);
        String email = App.value(edtEmail);
        String password = App.value(edtPassword);
        String confirm = App.value(edtConfirmPassword);
        boolean valid = true;

        if (name.length() < 2) {
            tilName.setError("Vui lòng nhập họ tên ít nhất 2 ký tự");
            valid = false;
        }
        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập email");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không đúng định dạng");
            valid = false;
        }
        if (password.length() < 6) {
            tilPassword.setError("Mật khẩu cần ít nhất 6 ký tự");
            valid = false;
        }
        if (confirm.isEmpty() || !confirm.equals(password)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            valid = false;
        }
        return valid;
    }

    private void busy(boolean isBusy) {
        btnRegister.setEnabled(!isBusy);
        btnBackToLogin.setEnabled(!isBusy);
    }

    private void registerPatient() {
        busy(true);
        txtMessage.setText("Đang tạo tài khoản bệnh nhân...");
        String email = App.value(edtEmail);
        String password = App.value(edtPassword);
        String name = App.value(edtName);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {
                    if (result.getUser() == null) {
                        busy(false);
                        txtMessage.setText("Không đọc được tài khoản vừa tạo.");
                        return;
                    }
                    String uid = result.getUser().getUid();
                    Map<String, Object> data = new HashMap<>();
                    data.put("role", "patient");
                    data.put("email", email);
                    data.put("name", name);
                    data.put("phone", "");
                    data.put("dob", "");
                    data.put("gender", "");
                    data.put("address", "");
                    data.put("medicalHistory", "");
                    data.put("createdAt", FieldValue.serverTimestamp());

                    App.db().collection("users").document(uid).set(data)
                            .addOnSuccessListener(unused -> {
                                FirebaseAuth.getInstance().signOut();
                                App.note(this, "Đăng ký thành công. Hãy đăng nhập.");
                                Intent intent = new Intent(this, AuthActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            })
                            .addOnFailureListener(error -> {
                                // Firebase Authentication đã tạo user nhưng hồ sơ chưa lưu được.
                                FirebaseAuth.getInstance().signOut();
                                busy(false);
                                txtMessage.setText("Tài khoản Auth đã tạo nhưng hồ sơ bệnh nhân chưa lưu: "
                                        + error.getLocalizedMessage());
                            });
                })
                .addOnFailureListener(error -> {
                    busy(false);
                    txtMessage.setText("Đăng ký thất bại: " + error.getLocalizedMessage());
                });
    }
}
