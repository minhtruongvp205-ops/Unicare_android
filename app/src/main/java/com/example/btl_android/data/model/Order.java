package com.example.btl_android.data.model;

public class Order {
    public String id;
    public String prescId;
    public String patientId;
    public String address;
    public String paymentMethod; // CASH, CARD, E_WALLET
    public double totalAmount;
    public long timestamp; // epoch milliseconds
}
