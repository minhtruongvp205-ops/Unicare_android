package com.example.btl_android.ui_giao_dien.medical_kham_benh.adapter;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.List;

/**
 * Lớp tương thích ngược.
 * Mã nguồn chính đặt tại com.example.btl_android.ui_giao_dien.medical_kham_benh.adapter_bo_dieu_hop.RecordAdapter.
 */
public class RecordAdapter extends com.example.btl_android.ui_giao_dien.medical_kham_benh.adapter_bo_dieu_hop.RecordAdapter {
    public RecordAdapter(List<DocumentSnapshot> records, OnItemClickListener listener) {
        super(records, listener);
    }
}
