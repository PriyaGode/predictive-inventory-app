import React, { useState } from "react";
import { useCart } from "../context/CartContext";

export default function Cart({ setPage }) {
  const { cart, dispatch } = useCart();
  const total = cart.reduce((sum, i) => sum + i.price * i.qty, 0);

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [orderResult, setOrderResult] = useState(null);
  const [error, setError] = useState(null);

  const handleCheckout = async () => {
    if (!name.trim() || !email.trim()) { setError("Name and email are required."); return; }
    setError(null);
    setLoading(true);
    try {
      const body = {
        customerName: name,
        customerEmail: email,
        items: cart.map((i) => ({
          productId: i.id,
          productName: i.name,
          price: i.price,
          quantity: i.qty,
        })),
      };
      const res = await fetch("/api/orders", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      if (!res.ok) throw new Error("Order failed: " + res.status);
      const data = await res.json();
      setOrderResult(data);
      dispatch({ type: "CLEAR" });
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  if (orderResult) {
    const status = orderResult.order?.status;
    const isConfirmed = status === "CONFIRMED";
    const isCancelled = status === "CANCELLED";
    return (
      <div style={styles.container}>
        <div style={{ ...styles.resultBox, borderColor: isCancelled ? "#e94560" : "#2e7d32" }}>
          <p style={styles.resultIcon}>{isCancelled ? "❌" : "✅"}</p>
          <h2 style={{ color: isCancelled ? "#e94560" : "#2e7d32" }}>
            {isCancelled ? "Order Cancelled" : isConfirmed ? "Order Confirmed!" : "Order Placed!"}
          </h2>
          <p style={styles.resultSub}>Order ID: <strong>#{orderResult.order?.id}</strong></p>
          <p style={styles.resultSub}>Status: <strong>{status}</strong></p>
          <p style={styles.resultSub}>Saga ID: <code style={styles.code}>{orderResult.sagaId}</code></p>
          <p style={styles.resultSub}>Flow: <strong>{orderResult.sagaType}</strong></p>
          {isCancelled && <p style={{ color: "#e94560", fontSize: 13 }}>Stock was unavailable. Your order was automatically compensated.</p>}
          <div style={styles.resultBtns}>
            <button style={styles.btn} onClick={() => setPage("home")}>Continue Shopping</button>
            <button style={styles.btnOutline} onClick={() => setPage("orders")}>View My Orders</button>
          </div>
        </div>
      </div>
    );
  }

  if (cart.length === 0)
    return (
      <div style={styles.empty}>
        <p>🛒 Your cart is empty.</p>
        <button style={styles.btn} onClick={() => setPage("home")}>Shop Now</button>
      </div>
    );

  return (
    <div style={styles.container}>
      <h2 style={styles.heading}>Your Cart</h2>
      {cart.map((item) => (
        <div key={item.id} style={styles.item}>
          <img src={item.image} alt={item.name} style={styles.img} />
          <div style={styles.info}>
            <p style={styles.name}>{item.name}</p>
            <p style={styles.price}>${item.price}</p>
          </div>
          <div style={styles.controls}>
            <button style={styles.qtyBtn} onClick={() => item.qty === 1 ? dispatch({ type: "REMOVE", id: item.id }) : dispatch({ type: "UPDATE_QTY", id: item.id, qty: item.qty - 1 })}>−</button>
            <span style={styles.qty}>{item.qty}</span>
            <button style={styles.qtyBtn} onClick={() => dispatch({ type: "UPDATE_QTY", id: item.id, qty: item.qty + 1 })}>+</button>
          </div>
          <p style={styles.subtotal}>${(item.price * item.qty).toFixed(2)}</p>
          <button style={styles.remove} onClick={() => dispatch({ type: "REMOVE", id: item.id })}>✕</button>
        </div>
      ))}

      <div style={styles.checkoutSection}>
        <h3 style={styles.checkoutTitle}>Customer Details</h3>
        <input style={styles.input} placeholder="Full Name" value={name} onChange={(e) => setName(e.target.value)} />
        <input style={styles.input} placeholder="Email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        {error && <p style={styles.errorMsg}>❌ {error}</p>}
      </div>

      <div style={styles.footer}>
        <span style={styles.total}>Total: <strong>${total.toFixed(2)}</strong></span>
        <button style={{ ...styles.checkout, opacity: loading ? 0.7 : 1 }} onClick={handleCheckout} disabled={loading}>
          {loading ? "Placing Order..." : "Checkout"}
        </button>
      </div>
    </div>
  );
}

const styles = {
  container: { maxWidth: 800, margin: "0 auto", padding: "24px 16px" },
  heading: { fontSize: 26, color: "#1a1a2e", marginBottom: 20 },
  item: { display: "flex", alignItems: "center", gap: 16, background: "#fff", borderRadius: 10, padding: 16, marginBottom: 12, boxShadow: "0 1px 6px rgba(0,0,0,0.08)" },
  img: { width: 70, height: 70, objectFit: "cover", borderRadius: 8 },
  info: { flex: 1 },
  name: { fontWeight: "bold", color: "#1a1a2e", margin: 0 },
  price: { color: "#888", margin: "4px 0 0", fontSize: 13 },
  controls: { display: "flex", alignItems: "center", gap: 10 },
  qtyBtn: { width: 28, height: 28, borderRadius: "50%", border: "1px solid #ddd", background: "#f5f5f5", cursor: "pointer", fontSize: 16 },
  qty: { fontSize: 15, fontWeight: "bold" },
  subtotal: { fontWeight: "bold", color: "#e94560", minWidth: 60, textAlign: "right" },
  remove: { background: "none", border: "none", color: "#aaa", cursor: "pointer", fontSize: 16 },
  checkoutSection: { background: "#fff", borderRadius: 10, padding: 20, marginTop: 16, boxShadow: "0 1px 6px rgba(0,0,0,0.08)", display: "flex", flexDirection: "column", gap: 12 },
  checkoutTitle: { margin: "0 0 4px", color: "#1a1a2e", fontSize: 16 },
  input: { padding: "10px 12px", borderRadius: 8, border: "1px solid #ddd", fontSize: 14 },
  errorMsg: { color: "#e94560", fontSize: 13, margin: 0 },
  footer: { display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 24, padding: "16px 0", borderTop: "2px solid #eee" },
  total: { fontSize: 20, color: "#1a1a2e" },
  checkout: { padding: "12px 32px", background: "#e94560", color: "#fff", border: "none", borderRadius: 8, cursor: "pointer", fontSize: 16 },
  empty: { textAlign: "center", padding: 80, fontSize: 20, color: "#888" },
  btn: { marginTop: 16, padding: "10px 28px", background: "#1a1a2e", color: "#fff", border: "none", borderRadius: 8, cursor: "pointer", fontSize: 15 },
  btnOutline: { marginTop: 16, padding: "10px 28px", background: "#fff", color: "#1a1a2e", border: "2px solid #1a1a2e", borderRadius: 8, cursor: "pointer", fontSize: 15 },
  resultBox: { background: "#fff", borderRadius: 12, padding: 40, textAlign: "center", boxShadow: "0 2px 12px rgba(0,0,0,0.1)", border: "2px solid" },
  resultIcon: { fontSize: 48, margin: "0 0 8px" },
  resultSub: { color: "#555", fontSize: 14, margin: "6px 0" },
  code: { background: "#f5f5f5", padding: "2px 8px", borderRadius: 4, fontSize: 12 },
  resultBtns: { display: "flex", gap: 12, justifyContent: "center", marginTop: 24 },
};
