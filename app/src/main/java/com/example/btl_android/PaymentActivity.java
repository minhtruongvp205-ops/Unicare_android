package com.example.btl_android;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btl_android.model.Order;
import com.example.btl_android.model.Prescription;
import com.example.btl_android.model.Prescription.MedicationItem;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity xử lý thanh toán cho đơn thuốc.
 */
public class PaymentActivity extends AppCompatActivity {

    public static final String EXTRA_PRESC_ID = "prescId";

    private RecyclerView rvItems;
    private TextView tvTotalAmount;
    private EditText etAddress;
    private RadioGroup rgPaymentMethod;
    private Button btnConfirm;

    private List<MedicationItem> medicationList = new ArrayList<>();
    private double totalAmount = 0.0;
    private String prescId;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        App.applyInsets(this);
        initViews();

        prescId = getIntent().getStringExtra(EXTRA_PRESC_ID);
        if (TextUtils.isEmpty(prescId)) {
            Toast.makeText(this, "Mã đơn thuốc không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadPrescription();
        btnConfirm.setOnClickListener(v -> confirmPayment());
    }

    private void initViews() {
        rvItems = findViewById(R.id.rvPaymentItems);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        etAddress = findViewById(R.id.etAddress);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        btnConfirm = findViewById(R.id.btnConfirmPayment);
    }

    private void loadPrescription() {
        CollectionReference ref = db.collection("prescriptions");
        ref.document(prescId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    Prescription presc = task.getResult().toObject(Prescription.class);
                    if (presc != null && presc.items != null) {
                        medicationList.clear();
                        medicationList.addAll(presc.items);
                        calculateTotal();
                        rvItems.setAdapter(new PaymentItemAdapter(medicationList));
                    }
                } else {
                    Toast.makeText(PaymentActivity.this, "Không tải được đơn thuốc", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void calculateTotal() {
        totalAmount = 0;
        for (MedicationItem item : medicationList) {
            totalAmount += item.price;
        }
        tvTotalAmount.setText(String.format("Tổng: %.0f ₫", totalAmount));
    }

    private void confirmPayment() {
        String address = etAddress.getText().toString().trim();
        if (!address.matches("^[a-zA-Z0-9\\s\\-\\/]+$") || address.length() < 5) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        int checkedId = rgPaymentMethod.getCheckedRadioButtonId();
        String method;
        if (checkedId == R.id.rbCash) {
            method = "CASH";
        } else if (checkedId == R.id.rbCard) {
            method = "CARD";
        } else if (checkedId == R.id.rbWallet) {
            method = "E_WALLET";
        } else {
            method = "CASH";
        }
        Order order = new Order();
        order.id = db.collection("orders").document().getId();
        order.prescId = prescId;
        order.patientId = "CURRENT_USER_ID"; // TODO: thay bằng thực tế
        order.address = address;
        order.paymentMethod = method;
        order.totalAmount = totalAmount;
        order.timestamp = System.currentTimeMillis();
        db.collection("orders").document(order.id).set(order)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(PaymentActivity.this, "Thanh toán thành công! Mã đơn: "+order.id, Toast.LENGTH_LONG).show();
                    // Mở Invoice
                    startActivity(new Intent(PaymentActivity.this, InvoiceActivity.class)
                            .putExtra(InvoiceActivity.EXTRA_ORDER_ID, order.id));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(PaymentActivity.this, "Lưu đơn thất bại: "+e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
