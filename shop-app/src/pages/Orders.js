import React, { useState } from "react";

const STATUS_STYLE = {
  CONFIRMED: { bg: "#e8f5e9", color: "#2e7d32" },
  CANCELLED: { bg: "#ffebee", color: "#c62828" },
  PENDING:   { bg: "#fff8e1", color: "#f57f17" },
};

export default function Orders({ setPage }) {
  const [email, setEmail] = useState("");
  const [orders, setOrders] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchOrders = async () => {
    if (!email.trim()) return;
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`/api/orders/customer?email=${encodeURIComponent(email)}`);
      if (!res.ok) throw new Error(`Server error ${res.status}: ${res.statusText}`);
      const data = await res.json();
      setOrders(Array.isArray(data) ? data : []);
    } catch (e) {
      if (e.message === "Failed to fetch") {
        setError("Cannot reach order-service. Make sure it is running on port 8082 and api-gateway on port 8080.");
      } else {
        setError(e.message);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={s.container}>
      <button style={s.back} onClick={() => setPage("home")}>← Back</button>
      <h2 style={s.heading}>My Orders</h2>

      <div style={s.searchRow}>
        <input style={s.input} placeholder="Enter your email" value={email} onChange={(e) => setEmail(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && fetchOrders()} />
        <button style={s.btn} onClick={fetchOrders} disabled={loading}>{loading ? "Loading..." : "Search"}</button>
      </div>

      {error && <p style={s.error}>❌ {error}</p>}

      {orders && orders.length === 0 && <p style={s.empty}>No orders found for this email.</p>}

      {orders && orders.map((order) => {
        const ss = STATUS_STYLE[order.status] || STATUS_STYLE.PENDING;
        return (
          <div key={order.id} style={s.card}>
            <div style={s.cardHeader}>
              <span style={s.orderId}>Order #{order.id}</span>
              <span style={{ ...s.badge, background: ss.bg, color: ss.color }}>{order.status}</span>
              <span style={s.date}>{new Date(order.createdAt).toLocaleString()}</span>
            </div>
            <div style={s.items}>
              {order.items?.map((item, i) => (
                <div key={i} style={s.itemRow}>
                  <span style={s.itemName}>{item.productName}</span>
                  <span style={s.itemQty}>x{item.quantity}</span>
                  <span style={s.itemPrice}>${(item.price * item.quantity).toFixed(2)}</span>
                </div>
              ))}
            </div>
            <div style={s.cardFooter}>
              <span>Total: <strong style={{ color: "#e94560" }}>${order.totalAmount?.toFixed(2)}</strong></span>
              <span style={s.customer}>{order.customerName}</span>
            </div>
          </div>
        );
      })}
    </div>
  );
}

const s = {
  container: { maxWidth: 800, margin: "0 auto", padding: "24px 16px" },
  back: { background: "none", border: "none", color: "#e94560", fontSize: 15, cursor: "pointer", marginBottom: 16 },
  heading: { fontSize: 26, color: "#1a1a2e", marginBottom: 20 },
  searchRow: { display: "flex", gap: 12, marginBottom: 24 },
  input: { flex: 1, padding: "10px 14px", borderRadius: 8, border: "1px solid #ddd", fontSize: 14 },
  btn: { padding: "10px 24px", background: "#1a1a2e", color: "#fff", border: "none", borderRadius: 8, cursor: "pointer", fontSize: 14 },
  error: { color: "#e94560", fontSize: 14 },
  empty: { textAlign: "center", color: "#888", padding: 40 },
  card: { background: "#fff", borderRadius: 10, padding: 20, marginBottom: 16, boxShadow: "0 2px 8px rgba(0,0,0,0.08)" },
  cardHeader: { display: "flex", alignItems: "center", gap: 12, marginBottom: 14, flexWrap: "wrap" },
  orderId: { fontWeight: "bold", color: "#1a1a2e", fontSize: 16 },
  badge: { padding: "4px 12px", borderRadius: 12, fontSize: 12, fontWeight: 600 },
  date: { color: "#aaa", fontSize: 12, marginLeft: "auto" },
  items: { borderTop: "1px solid #f5f5f5", paddingTop: 12, display: "flex", flexDirection: "column", gap: 8 },
  itemRow: { display: "flex", alignItems: "center", gap: 12, fontSize: 14 },
  itemName: { flex: 1, color: "#333" },
  itemQty: { color: "#888" },
  itemPrice: { fontWeight: "bold", color: "#1a1a2e" },
  cardFooter: { display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 14, paddingTop: 12, borderTop: "1px solid #f5f5f5", fontSize: 14 },
  customer: { color: "#888", fontSize: 13 },
};
