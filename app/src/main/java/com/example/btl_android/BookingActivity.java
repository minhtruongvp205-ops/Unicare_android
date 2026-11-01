package com.example.btl_android;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import java.util.TimeZone;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {
    private static class DoctorOption {
        String uid,name,specialty;
        DoctorOption(String uid,String name,String specialty){this.uid=uid;this.name=name;this.specialty=specialty;}
        @Override public String toString(){return name+" • "+specialty;}
    }
    private final List<DoctorOption> doctors = new ArrayList<>();
    private Calendar selected = Calendar.getInstance();
    private boolean dateChosen=false,timeChosen=false;
    private Spinner doctorSpinner;
    private TextView when, message;
    private TextInputEditText reason;
    private TextInputLayout tilReason;
    private MaterialButton submit;
    @Override protected void onCreate(Bundle b){
        super.onCreate(b); App.requireLogin(this); if(App.user()==null) return;
        setContentView(R.layout.activity_booking); App.applyInsets(this);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        doctorSpinner=findViewById(R.id.spinnerDoctors);
        when=findViewById(R.id.txtWhen); message=findViewById(R.id.txtMessage);
        reason=findViewById(R.id.edtReason); submit=findViewById(R.id.btnBookNow);
        tilReason=findViewById(R.id.tilReason);
        submit.setEnabled(false);
        findViewById(R.id.btnChooseDate).setOnClickListener(v -> showDatePickerDialog());
        findViewById(R.id.btnChooseTime).setOnClickListener(v -> {
            Calendar now=Calendar.getInstance();
            new TimePickerDialog(this,(picker,h,m)->{
                selected.set(Calendar.HOUR_OF_DAY,h); selected.set(Calendar.MINUTE,m); selected.set(Calendar.SECOND,0); selected.set(Calendar.MILLISECOND,0);
                timeChosen=true;updateDateLabel();
            },now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE),true).show();
        });
        submit.setOnClickListener(v -> book());
        App.db().collection("users").document(App.uid()).get().addOnSuccessListener(d -> {
            if(!"patient".equals(App.str(d,"role"))){message.setText("Chỉ bệnh nhân được đặt lịch.");return;}
            loadDoctors();
        });
    }
    // Mẫu MaterialDatePicker của thầy; chuyển đúng ngày UTC sang Calendar địa phương.
    private void showDatePickerDialog() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now()).build();
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Chọn ngày khám")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(constraints).build();
        datePicker.addOnPositiveButtonClickListener(value -> {
            Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTimeInMillis(value);
            selected.set(Calendar.YEAR, utc.get(Calendar.YEAR));
            selected.set(Calendar.MONTH, utc.get(Calendar.MONTH));
            selected.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH));
            dateChosen = true;
            updateDateLabel();
        });
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private boolean validateData() {
        tilReason.setError(null);
        if (doctors.isEmpty() || doctorSpinner.getSelectedItemPosition() < 0
                || !dateChosen || !timeChosen) {
            App.note(this, "Chọn bác sĩ, ngày và giờ khám");
            return false;
        }
        if (selected.getTimeInMillis() < System.currentTimeMillis() + 5 * 60_000) {
            App.note(this, "Giờ khám phải cách hiện tại ít nhất 5 phút");
            return false;
        }
        if (App.value(reason).length() < 3) {
            tilReason.setError("Vui lòng nhập lý do khám (ít nhất 3 ký tự)");
            return false;
        }
        return true;
    }

    private void updateDateLabel(){
        when.setText((dateChosen?new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selected.getTime()):"Chưa chọn ngày")+" • "+(timeChosen?new SimpleDateFormat("HH:mm",Locale.getDefault()).format(selected.getTime()):"Chưa chọn giờ"));
    }
    private void loadDoctors(){
        message.setText("Đang tải danh sách bác sĩ...");
        App.db().collection("doctors").get().addOnSuccessListener(s -> {
            doctors.clear();
            for(QueryDocumentSnapshot d:s) doctors.add(new DoctorOption(d.getId(),App.str(d,"name"),App.str(d,"specialty")));
            ArrayAdapter<DoctorOption> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,doctors);
            a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); doctorSpinner.setAdapter(a);
            submit.setEnabled(!doctors.isEmpty());
            message.setText(doctors.isEmpty()?"Chưa có bác sĩ. Hãy tạo tài khoản bác sĩ và hồ sơ doctors trong Firebase Console theo README.":"");
        }).addOnFailureListener(e -> message.setText("Không tải được danh sách bác sĩ: "+e.getLocalizedMessage()));
    }
    private void book(){
        if (!validateData()) return;
        String why = App.value(reason);
        DoctorOption doc=doctors.get(doctorSpinner.getSelectedItemPosition());
        submit.setEnabled(false);
        App.db().collection("users").document(App.uid()).get().addOnSuccessListener(profile -> {
            if(!"patient".equals(App.str(profile,"role"))){submit.setEnabled(true);App.note(this,"Tài khoản không hợp lệ");return;}
            // Each doctor + time slot is reserved in a Firestore transaction: two patients cannot get the same slot.
            String slotId=doc.uid+"_"+new SimpleDateFormat("yyyyMMdd_HHmm",Locale.US).format(selected.getTime());
            DocumentReference slot=App.db().collection("slots").document(slotId);
            DocumentReference appt=App.db().collection("appointments").document();
            Map<String,Object> a=new HashMap<>();
            a.put("patientId",App.uid()); a.put("patientName",App.str(profile,"name"));
            a.put("doctorId",doc.uid); a.put("doctorName",doc.name); a.put("specialty",doc.specialty);
            a.put("startAt", new Timestamp(new Date(selected.getTimeInMillis()))); a.put("reason",why);
            a.put("slotId",slotId); a.put("status","PENDING");
            a.put("createdAt",FieldValue.serverTimestamp()); a.put("updatedAt",FieldValue.serverTimestamp());
            Map<String,Object> sl=new HashMap<>();
            sl.put("appointmentId",appt.getId()); sl.put("doctorId",doc.uid);
            sl.put("createdAt",FieldValue.serverTimestamp());
            App.db().runTransaction(tr -> {
                        if(tr.get(slot).exists()) throw new IllegalStateException("Khung giờ đã có người đặt, vui lòng chọn giờ khác");
                        tr.set(appt,a); tr.set(slot,sl); return null;
                    }).addOnSuccessListener(x -> {submit.setEnabled(true);App.note(this,"Đã đặt lịch, đang chờ bác sĩ xác nhận"); finish();})
                    .addOnFailureListener(e -> {submit.setEnabled(true);App.error(this,e);});
        }).addOnFailureListener(e -> {submit.setEnabled(true);App.error(this,e);});
    }
}
