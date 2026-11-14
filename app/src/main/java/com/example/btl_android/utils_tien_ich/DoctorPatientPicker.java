package com.example.btl_android.utils_tien_ich;

import com.example.btl_android.R;
import com.example.btl_android.core_he_thong.*;
import com.example.btl_android.core_he_thong.manager.*;
import com.example.btl_android.data_du_lieu.*;
import com.example.btl_android.data_du_lieu.model.*;
import com.example.btl_android.utils_tien_ich.*;
import com.example.btl_android.ui_giao_dien_giao_dien.auth_dang_nhap.*;
import com.example.btl_android.ui_giao_dien_giao_dien.dashboard_trang_chu.*;
import com.example.btl_android.ui_giao_dien_giao_dien.appointment_lich_hen.*;
import com.example.btl_android.ui_giao_dien_giao_dien.medical_kham_benh.*;
import com.example.btl_android.ui_giao_dien_giao_dien.medical_kham_benh.adapter.*;
import com.example.btl_android.ui_giao_dien_giao_dien.payment_thanh_toan.*;
import com.example.btl_android.ui_giao_dien_giao_dien.payment_thanh_toan.adapter.*;
import com.example.btl_android.ui_giao_dien_giao_dien.info_tin_tuc_ho_tro.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Choose a patient with an existing booking assigned to the logged-in doctor. */
public final class DoctorPatientPicker {
    private DoctorPatientPicker() {}
    public static void open(Activity activity, Class<?> target) {
        App.db().collection("appointments").whereEqualTo("doctorId", App.uid()).get()
            .addOnSuccessListener(snapshot -> {
                Map<String, QueryDocumentSnapshot> choices = new LinkedHashMap<>();
                for (QueryDocumentSnapshot d : snapshot) {
                    String id = App.str(d,"patientId");
                    String status = App.str(d,"status");
                    if (!id.isEmpty() && ("CONFIRMED".equals(status) || "COMPLETED".equals(status))) {
                        choices.put(id,d);
                    }
                }
                if (choices.isEmpty()) {
                    App.note(activity,"Chưa có bệnh nhân có lịch khám đã xác nhận");return;
                }
                ArrayList<QueryDocumentSnapshot> documents = new ArrayList<>(choices.values());
                String[] labels = new String[documents.size()];
                for (int i=0;i<documents.size();i++) labels[i] = App.str(documents.get(i),"patientName");
                new AlertDialog.Builder(activity).setTitle("Chọn bệnh nhân")
                    .setItems(labels, (dialog,position) -> {
                        QueryDocumentSnapshot d = documents.get(position);
                        Intent intent = new Intent(activity,target);
                        intent.putExtra("patientId",App.str(d,"patientId"));
                        intent.putExtra("appointmentId",d.getId());
                        intent.putExtra("patientName",App.str(d,"patientName"));
                        activity.startActivity(intent);
                    }).setNegativeButton("Hủy",null).show();
            }).addOnFailureListener(e -> App.error(activity,e));
    }
}
