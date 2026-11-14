package com.example.btl_android.ui_giao_dien.medical_kham_benh.adapter;

import com.example.btl_android.R;
import com.example.btl_android.core_he_thong.*;
import com.example.btl_android.core_he_thong.manager.*;
import com.example.btl_android.data_du_lieu.*;
import com.example.btl_android.data_du_lieu.model.*;
import com.example.btl_android.utils_tien_ich.*;
import com.example.btl_android.ui_giao_dien.auth_dang_nhap.*;
import com.example.btl_android.ui_giao_dien.dashboard_trang_chu.*;
import com.example.btl_android.ui_giao_dien.appointment_lich_hen.*;
import com.example.btl_android.ui_giao_dien.medical_kham_benh.*;
import com.example.btl_android.ui_giao_dien.medical_kham_benh.adapter.*;
import com.example.btl_android.ui_giao_dien.payment_thanh_toan.*;
import com.example.btl_android.ui_giao_dien.payment_thanh_toan.adapter.*;
import com.example.btl_android.ui_giao_dien.info_tin_tuc_ho_tro.*;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

/** Adapter tách riêng khỏi Activity để dễ học, dễ sử dụng lại. */
public class RecordAdapter extends RecyclerView.Adapter<RecordViewHolder> {
    public interface OnItemClickListener {
        void onViewDetail(DocumentSnapshot document);
    }

    private final List<DocumentSnapshot> records;
    private final OnItemClickListener listener;
    private String role = "patient";

    public RecordAdapter(List<DocumentSnapshot> records, OnItemClickListener listener) {
        this.records = records;
        this.listener = listener;
    }

    public void setRole(String role) {
        this.role = role;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecordViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        DocumentSnapshot document = records.get(position);
        boolean isDoctor = "doctor".equals(role);
        holder.txtTitle.setText(isDoctor
                ? "Bệnh nhân: " + App.str(document, "patientName")
                : "Bác sĩ: " + App.str(document, "doctorName"));
        holder.txtInfo.setText("Ngày khám: " + App.date(document.getTimestamp("visitAt"))
                + "\nChẩn đoán: " + App.str(document, "diagnosis"));
        holder.btnDetail.setOnClickListener(v -> listener.onViewDetail(document));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }
}
