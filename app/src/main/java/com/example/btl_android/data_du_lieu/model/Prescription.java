package com.example.btl_android.data_du_lieu.model;

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

import java.util.List;

public class Prescription {
    public String id;
    public String doctorId;
    public String patientId;
    public List<MedicationItem> items;

    public static class MedicationItem {
        public String name;
        public String dosage;
        public String imageUrl; // URL to medication image stored in Firebase Storage
        public double price;
    }
}
