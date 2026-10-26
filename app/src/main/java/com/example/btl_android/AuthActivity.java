package com.example.btl_android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class AuthActivity extends AppCompatActivity {
    private TextInputLayout tilEmail, tilPassword;
    private EditText email, password;
    private Button btnLogin, btnGoToRegister;
    private TextView message;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_auth); App.applyInsets(this);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        email = findViewById(R.id.edtEmail);
        password = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);
        message = findViewById(R.id.txtMessage);

        if (!App.configured(this)) { btnLogin.setEnabled(false); if (btnGoToRegister != null) btnGoToRegister.setEnabled(false); return; }
        if (App.user() != null) { openByRole(); }

        btnLogin.setOnClickListener(v -> login());
        if (btnGoToRegister != null) {
            btnGoToRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        }
    }

    private boolean validate() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        if (message != null) message.setText("");

        String e = App.value(email), p = App.value(password);
        boolean valid = true;

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
        btnLogin.setEnabled(!busy);
        if (btnGoToRegister != null) btnGoToRegister.setEnabled(!busy);
    }

    private void login() {
        if (!validate()) return;
        busy(true); message.setText("Đang đăng nhập...");
        FirebaseAuth.getInstance().signInWithEmailAndPassword(App.value(email), App.value(password))
            .addOnSuccessListener(r -> openByRole())
            .addOnFailureListener(e -> { busy(false); message.setText("Đăng nhập thất bại: " + e.getLocalizedMessage()); });
    }

    private void openByRole() {
        if (App.user() == null) return;
        App.db().collection("users").document(App.uid()).get()
            .addOnSuccessListener(d -> {
                String role = App.str(d, "role");
                if (!d.exists() || (!role.equals("patient") && !role.equals("doctor"))) {
                    FirebaseAuth.getInstance().signOut();
                    message.setText("Tài khoản chưa được cấp hồ sơ/vai trò hợp lệ trong Firestore.");
                    busy(false); return;
                }
                Intent i = new Intent(this, DashboardActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            }).addOnFailureListener(e -> { busy(false); App.error(this, e); });
    }
}
