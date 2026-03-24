import React from "react";
import { useCart } from "../context/CartContext";

export default function Navbar({ page, setPage }) {
  const { cart } = useCart();
  const totalItems = cart.reduce((sum, i) => sum + i.qty, 0);

  return (
    <nav style={styles.nav}>
      <span style={styles.logo} onClick={() => setPage("home")}>🛍️ ShopApp</span>
      <div style={styles.navLinks}>
        <button style={styles.navBtn} onClick={() => setPage("inventory")}>📦 Inventory</button>
        <button style={styles.navBtn} onClick={() => setPage("forecast")}>📈 Forecast</button>
        <button style={styles.navBtn} onClick={() => setPage("orders")}>📋 My Orders</button>
        <button style={styles.cartBtn} onClick={() => setPage("cart")}>
          🛒 Cart {totalItems > 0 && <span style={styles.badge}>{totalItems}</span>}
        </button>
      </div>
    </nav>
  );
}

const styles = {
  nav: { display: "flex", justifyContent: "space-between", alignItems: "center", padding: "14px 32px", background: "#1a1a2e", color: "#fff" },
  logo: { fontSize: 22, fontWeight: "bold", cursor: "pointer" },
  navLinks: { display: "flex", alignItems: "center", gap: 12 },
  navBtn: { background: "transparent", color: "#fff", border: "1px solid rgba(255,255,255,0.3)", padding: "8px 18px", borderRadius: 20, cursor: "pointer", fontSize: 14 },
  cartBtn: { background: "#e94560", color: "#fff", border: "none", padding: "8px 18px", borderRadius: 20, cursor: "pointer", fontSize: 15, position: "relative" },
  badge: { background: "#fff", color: "#e94560", borderRadius: "50%", padding: "1px 6px", fontSize: 12, marginLeft: 6, fontWeight: "bold" },
};
