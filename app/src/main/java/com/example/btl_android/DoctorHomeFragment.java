package com.example.btl_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class DoctorHomeFragment extends Fragment {

    private ListenerRegistration statListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_home, container, false);
        
        App.db().collection("users").document(App.uid()).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists()) return;
                    String name = App.str(d, "name");
                    TextView tvWelcome = view.findViewById(R.id.txtWelcome);
                    if (tvWelcome != null) {
                        tvWelcome.setText("Xin chào, " + (name.isEmpty() ? "bác sĩ" : name.split(" ")[name.split(" ").length - 1]) + " 👋");
                    }
                });

        wireDoctorButtons(view);
        loadDoctorStats(view);
        return view;
    }

    private void wireDoctorButtons(View view) {
        safeClick(view, R.id.drStatCardPatients, AppointmentsActivity.class);
        View cardToday = view.findViewById(R.id.drStatCardToday);
        if (cardToday != null) cardToday.setOnClickListener(v -> {
            Intent i = new Intent(getContext(), AppointmentsActivity.class);
            i.putExtra("filterToday", true);
            startActivity(i);
        });
        safeClick(view, R.id.drStatCardRating, DoctorStatsActivity.class);

        safeClick(view, R.id.btnDrAppointments, AppointmentsActivity.class);
        // Bác sĩ bắt đầu khám từ lịch đã xác nhận. Trong phiếu khám có đầy đủ
        // chỉ số, chẩn đoán, đơn thuốc, lịch uống và thao tác hoàn tất.
        View btnStart = view.findViewById(R.id.btnDrStartExam);
        if (btnStart != null) btnStart.setOnClickListener(v -> {
            Intent i = new Intent(getContext(), AppointmentsActivity.class);
            i.putExtra("filterStatus", "CONFIRMED");
            startActivity(i);
        });
        safeClick(view, R.id.btnDrRecords, RecordsActivity.class);
        
        int[] doctorButtons = {R.id.btnDrAppointments, R.id.btnDoctorStats, R.id.btnDoctorSchedule, R.id.btnDrRecords};
        for (int id : doctorButtons) {
            View button = view.findViewById(id);
            if (button != null && button.getParent() instanceof View) {
                View row = (View) button.getParent();
                row.setOnClickListener(v -> button.performClick());
            }
        }
        safeClick(view, R.id.btnDoctorStats, DoctorStatsActivity.class);
        safeClick(view, R.id.btnDoctorSchedule, DoctorScheduleActivity.class);
    }

    private void loadDoctorStats(View view) {
        TextView tvPatients = view.findViewById(R.id.drStatCountPatients);
        TextView tvToday    = view.findViewById(R.id.drStatCountToday);
        TextView tvRating   = view.findViewById(R.id.drStatCountRating);
        if (tvPatients == null) return;

        statListener = App.db().collection("appointments")
                .whereEqualTo("doctorId", App.uid())
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    Calendar startOfDay = Calendar.getInstance();
                    startOfDay.set(Calendar.HOUR_OF_DAY, 0);
                    startOfDay.set(Calendar.MINUTE, 0);
                    startOfDay.set(Calendar.SECOND, 0);
                    startOfDay.set(Calendar.MILLISECOND, 0);

                    int todayCount = 0;
                    Set<String> patientIds = new HashSet<>();
                    for (QueryDocumentSnapshot d : snap) {
                        String pid = d.getString("patientId");
                        if (pid != null) patientIds.add(pid);
                        Timestamp ts = d.getTimestamp("startAt");
                        if (ts != null && ts.toDate().after(startOfDay.getTime())) todayCount++;
                    }
                    tvPatients.setText(String.valueOf(patientIds.size()));
                    tvToday.setText(String.valueOf(todayCount));
                });

        App.db().collection("ratings").whereEqualTo("doctorId", App.uid()).get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || snap.isEmpty()) { tvRating.setText("--"); return; }
                    double total = 0; int count = 0;
                    for (QueryDocumentSnapshot d : snap) {
                        Long stars = d.getLong("stars");
                        if (stars != null) { total += stars; count++; }
                    }
                    if (count > 0) tvRating.setText(String.format(Locale.US, "%.1f★", total / count));
                    else tvRating.setText("--");
                });
    }

    private void safeClick(View view, int viewId, Class<?> activityClass) {
        View v = view.findViewById(viewId);
        if (v != null) v.setOnClickListener(x -> startActivity(new Intent(getContext(), activityClass)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (statListener != null) { statListener.remove(); statListener = null; }
    }
}