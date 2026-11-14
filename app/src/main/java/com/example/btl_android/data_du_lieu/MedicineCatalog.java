package com.example.btl_android.data_du_lieu;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.example.btl_android.core_he_thong.App;
import com.example.btl_android.utils_tien_ich.PriceText;

import androidx.annotation.NonNull;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Danh mục giá cố định; ưu tiên dữ liệu medicine_catalog trên Firestore.
 */
public final class MedicineCatalog {
    private MedicineCatalog() {}

    public static class Medicine {
        public final String id, name, unit;
        public final long price;
        public Medicine(String id, String name, String unit, long price) {
            this.id = id;
            this.name = name;
            this.unit = unit;
            this.price = price;
        }
        @NonNull @Override public String toString() {
            return name + "  •  " + PriceText.vnd(price) + "/" + unit;
        }
    }

    public interface Callback {
        void onLoaded(List<Medicine> medicines, boolean demo);
    }

    public static void load(Callback callback) {
        App.db().collection("medicine_catalog").get()
                .addOnSuccessListener(snap -> {
                    List<Medicine> medicines = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        if (Boolean.FALSE.equals(d.getBoolean("active"))) continue;
                        String name = d.getString("name");
                        String unit = d.getString("unit");
                        Long price = d.getLong("priceVnd");
                        if (name == null || name.trim().isEmpty() || price == null || price < 0) continue;
                        medicines.add(new Medicine(d.getId(), name.trim(),
                                unit == null || unit.trim().isEmpty() ? "đơn vị" : unit.trim(), price));
                    }
                    Collections.sort(medicines, Comparator.comparing(m -> m.name));
                    if (medicines.isEmpty()) callback.onLoaded(demoMedicines(), true);
                    else callback.onLoaded(medicines, false);
                })
                .addOnFailureListener(e -> callback.onLoaded(demoMedicines(), true));
    }

    private static List<Medicine> demoMedicines() {
        List<Medicine> medicines = new ArrayList<>();
        medicines.add(new Medicine("demo_paracetamol", "Paracetamol 500 mg", "viên", 1500));
        medicines.add(new Medicine("demo_cetirizine", "Cetirizine 10 mg", "viên", 2500));
        medicines.add(new Medicine("demo_loratadine", "Loratadine 10 mg", "viên", 3000));
        medicines.add(new Medicine("demo_ors", "Oresol gói bù nước", "gói", 5000));
        medicines.add(new Medicine("demo_saline", "Natri clorid 0,9% 10 ml", "lọ", 6000));
        medicines.add(new Medicine("demo_vitamin_c", "Vitamin C 500 mg", "viên", 1200));
        medicines.add(new Medicine("demo_omeprazole", "Omeprazole 20 mg", "viên", 3500));
        medicines.add(new Medicine("demo_amoxicillin", "Amoxicillin 500 mg", "viên", 3200));
        return medicines;
    }
}
