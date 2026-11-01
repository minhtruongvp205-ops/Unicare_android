package com.example.btl_android.model;

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
