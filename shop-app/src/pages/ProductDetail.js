import React from "react";
import { useCart } from "../context/CartContext";

export default function ProductDetail({ product, setPage }) {
  const { dispatch } = useCart();

  if (!product) return null;

  return (
    <div style={styles.container}>
      <button style={styles.back} onClick={() => setPage("home")}>← Back</button>
      <div style={styles.card}>
        <img src={product.image} alt={product.name} style={styles.img} />
        <div style={styles.info}>
          <span style={styles.category}>{product.category}</span>
          <h2 style={styles.name}>{product.name}</h2>
          <p style={styles.desc}>{product.description}</p>
          <div style={styles.row}>
            <span style={styles.price}>${product.price}</span>
            <span style={styles.rating}>⭐ {product.rating} / 5</span>
          </div>
          <button style={styles.btn} onClick={() => { dispatch({ type: "ADD", product }); setPage("cart"); }}>
            Add to Cart &amp; View Cart
          </button>
        </div>
      </div>
    </div>
  );
}

const styles = {
  container: { maxWidth: 900, margin: "0 auto", padding: "24px 16px" },
  back: { background: "none", border: "none", color: "#e94560", fontSize: 15, cursor: "pointer", marginBottom: 16 },
  card: { display: "flex", gap: 32, background: "#fff", borderRadius: 12, overflow: "hidden", boxShadow: "0 2px 12px rgba(0,0,0,0.1)", flexWrap: "wrap" },
  img: { width: 360, height: 360, objectFit: "cover" },
  info: { flex: 1, padding: 32, display: "flex", flexDirection: "column", justifyContent: "center" },
  category: { fontSize: 12, color: "#888", textTransform: "uppercase", letterSpacing: 1 },
  name: { fontSize: 26, color: "#1a1a2e", margin: "8px 0" },
  desc: { color: "#555", lineHeight: 1.6, marginBottom: 16 },
  row: { display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 24 },
  price: { fontSize: 28, fontWeight: "bold", color: "#e94560" },
  rating: { fontSize: 15, color: "#555" },
  btn: { padding: "12px 0", background: "#e94560", color: "#fff", border: "none", borderRadius: 8, cursor: "pointer", fontSize: 16 },
};
