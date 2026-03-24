import React from "react";
import { useCart } from "../context/CartContext";

export default function ProductCard({ product, onClick }) {
  const { dispatch } = useCart();

  return (
    <div style={styles.card}>
      <img src={product.image} alt={product.name} style={styles.img} onClick={() => onClick(product)} />
      <div style={styles.body}>
        <span style={styles.category}>{product.category}</span>
        <h3 style={styles.name} onClick={() => onClick(product)}>{product.name}</h3>
        <div style={styles.row}>
          <span style={styles.price}>${product.price}</span>
          <span style={styles.rating}>⭐ {product.rating}</span>
        </div>
        <button style={styles.btn} onClick={() => dispatch({ type: "ADD", product })}>Add to Cart</button>
      </div>
    </div>
  );
}

const styles = {
  card: { background: "#fff", borderRadius: 12, overflow: "hidden", boxShadow: "0 2px 12px rgba(0,0,0,0.1)", transition: "transform 0.2s", cursor: "pointer" },
  img: { width: "100%", height: 200, objectFit: "cover" },
  body: { padding: 16 },
  category: { fontSize: 11, color: "#888", textTransform: "uppercase", letterSpacing: 1 },
  name: { margin: "6px 0", fontSize: 16, color: "#1a1a2e" },
  row: { display: "flex", justifyContent: "space-between", alignItems: "center", margin: "8px 0" },
  price: { fontSize: 18, fontWeight: "bold", color: "#e94560" },
  rating: { fontSize: 13, color: "#555" },
  btn: { width: "100%", padding: "9px 0", background: "#1a1a2e", color: "#fff", border: "none", borderRadius: 8, cursor: "pointer", fontSize: 14 },
};
