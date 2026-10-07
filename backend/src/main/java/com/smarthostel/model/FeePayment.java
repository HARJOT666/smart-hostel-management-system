package com.smarthostel.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fee_payment")
@Getter
@Setter
@NoArgsConstructor
public class FeePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(nullable = false)
    private String semester;

    @Column(name = "fee_type", nullable = false)
    private String feeType;

    @Column(name = "amount_due", nullable = false)
    private BigDecimal amountDue;

    @Column(name = "amount_paid", nullable = false)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "payment_mode")
    private String paymentMode;

    @Column(name = "transaction_ref")
    private String transactionRef;

    /** Status is derived, not stored (avoids a transitive dependency in the table). */
    public String getStatus() {
        if (amountPaid.compareTo(amountDue) >= 0) return "Paid";
        if (amountPaid.signum() > 0) return "Partial";
        return "Pending";
    }
}
