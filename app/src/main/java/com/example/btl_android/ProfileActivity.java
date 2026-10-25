package com.example.btl_android;

import android.os.Bundle;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import java.util.Calendar;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    TextInputEditText name,phone,dob,gender,address,history;
    TextInputLayout tilName,tilPhone,tilDob;
    MaterialButton save;
    TextView message;
    String role="";
    @Override protected void onCreate(Bundle b){
        super.onCreate(b); App.requireLogin(this); if(App.user()==null)return;
        setContentView(R.layout.activity_profile); App.applyInsets(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        tilName = findViewById(R.id.tilName);
        tilPhone = findViewById(R.id.tilPhone);
        tilDob = findViewById(R.id.tilDob);
        name=findViewById(R.id.edtName);phone=findViewById(R.id.edtPhone); dob=findViewById(R.id.edtDob);
        gender=findViewById(R.id.edtGender);address=findViewById(R.id.edtAddress);
        history=findViewById(R.id.edtMedicalHistory);save=findViewById(R.id.btnSaveProfile);
        MaterialButton logout=findViewById(R.id.btnLogout);
        logout.setOnClickListener(v -> App.logout(this));
        message=findViewById(R.id.txtMessage);
        save.setEnabled(false);
        ((TextView)findViewById(R.id.txtEmail)).setText("Email: "+App.user().getEmail());
        App.db().collection("users").document(App.uid()).get().addOnSuccessListener(d -> {
            if(!d.exists()){message.setText("Không có hồ sơ người dùng");return;}
            role=App.str(d,"role");
            ((TextView)findViewById(R.id.txtRole)).setText("Vai trò: "+role);
            name.setText(App.str(d,"name"));phone.setText(App.str(d,"phone"));
            dob.setText(App.str(d,"dob"));gender.setText(App.str(d,"gender"));
            address.setText(App.str(d,"address"));history.setText(App.str(d,"medicalHistory"));
            save.setEnabled(true);
        }).addOnFailureListener(e->App.error(this,e));
        // M3 DatePicker đúng mẫu bài học: chọn ngày, không phải gõ bằng tay.
        dob.setFocusable(false);
        dob.setOnClickListener(v -> showDatePickerDialog());
        save.setOnClickListener(v -> {
            if (validateData()) saveProfile();
        });
    }
    private boolean validateData() {
        boolean isValid = true;
        tilName.setError(null);
        tilPhone.setError(null);
        if (App.value(name).length() < 2) {
            tilName.setError("Họ tên phải có ít nhất 2 ký tự");
            isValid = false;
        }
        if (!App.value(phone).isEmpty() && !App.value(phone).matches("^0[0-9]{9}$")) {
            tilPhone.setError("Số điện thoại gồm 10 số, bắt đầu bằng 0");
            isValid = false;
        }
        return isValid;
    }

    private void showDatePickerDialog() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointBackward.now()).build();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày sinh")
                .setCalendarConstraints(constraints)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(selection);
            SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            dob.setText(fmt.format(utc.getTime()));
            tilDob.setError(null);
        });
        picker.show(getSupportFragmentManager(), "PICK_DOB");
    }

    private void saveProfile(){
        Map<String,Object> changes=new HashMap<>();
        changes.put("name",App.value(name)); changes.put("phone",App.value(phone));
        changes.put("dob",App.value(dob));changes.put("gender",App.value(gender));
        changes.put("address",App.value(address));changes.put("medicalHistory",App.value(history));
        // No client can change their role by updating profile.
        save.setEnabled(false);
        App.db().collection("users").document(App.uid()).update(changes)
                .addOnSuccessListener(x->{save.setEnabled(true);message.setText("Đã lưu hồ sơ");})
                .addOnFailureListener(e->{save.setEnabled(true);App.error(this,e);});
    }
}
