import React, { useState, useEffect } from "react";

export default function LowStockAlert() {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch("/api/inventory/low-stock")
      .then((r) => r.json())
      .then((data) => { setAlerts(Array.isArray(data) ? data : []); setLoading(false); })
      .catch(() => setLoading(false));

    // Poll every 30 seconds for real-time updates
    const interval = setInterval(() => {
      fetch("/api/inventory/low-stock")
        .then((r) => r.json())
        .then((data) => setAlerts(Array.isArray(data) ? data : []))
        .catch(() => {});
    }, 30000);

    return () => clearInterval(interval);
  }, []);

  if (loading) return null;
  if (alerts.length === 0) return null;

  return (
    <div style={styles.container}>
      <h3 style={styles.title}>⚠️ Low Stock Alerts ({alerts.length})</h3>
      <div style={styles.list}>
        {alerts.map((item) => (
          <div key={item.productId} style={{ ...styles.item, ...(item.status === "OUT_OF_STOCK" ? styles.outOfStock : styles.lowStock) }}>
            <span style={styles.name}>{item.productName}</span>
            <span style={styles.qty}>Qty: {item.quantity}</span>
            <span style={{ ...styles.badge, background: item.status === "OUT_OF_STOCK" ? "#e94560" : "#f39c12" }}>
              {item.status.replace("_", " ")}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

const styles = {
  container: { maxWidth: 1100, margin: "0 auto 0", padding: "0 16px 16px" },
  title: { fontSize: 16, color: "#1a1a2e", marginBottom: 10 },
  list: { display: "flex", flexWrap: "wrap", gap: 10 },
  item: { display: "flex", alignItems: "center", gap: 12, padding: "10px 16px", borderRadius: 8, border: "1px solid #eee" },
  outOfStock: { background: "#fff5f5", borderColor: "#e94560" },
  lowStock: { background: "#fffbf0", borderColor: "#f39c12" },
  name: { fontWeight: "bold", color: "#1a1a2e", fontSize: 14 },
  qty: { color: "#888", fontSize: 13 },
  badge: { color: "#fff", padding: "2px 10px", borderRadius: 12, fontSize: 11, fontWeight: "bold" },
};
