import React, { useState, useEffect } from "react";
import ProductCard from "../components/ProductCard";
import LowStockAlert from "../components/LowStockAlert";

const API = "/api/products";

export default function Home({ setPage, setSelected }) {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState(["All"]);
  const [active, setActive] = useState("All");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch(API)
      .then((r) => {
        if (!r.ok) throw new Error("Server error: " + r.status);
        return r.json();
      })
      .then((data) => {
        setProducts(data);
        setCategories(["All", ...new Set(data.map((p) => p.category))]);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });
  }, []);

  const filtered = active === "All" ? products : products.filter((p) => p.category === active);

  return (
    <div style={styles.container}>
      <h2 style={styles.heading}>Our Products</h2>
      <LowStockAlert />
      <div style={styles.filters}>
        {categories.map((c) => (
          <button key={c} style={{ ...styles.filter, ...(active === c ? styles.activeFilter : {}) }} onClick={() => setActive(c)}>{c}</button>
        ))}
      </div>
      {error && <p style={{ color: "red" }}>❌ {error}</p>}
      {loading ? <p>Loading products...</p> : (
        <div style={styles.grid}>
          {filtered.map((p) => (
            <ProductCard key={p.id} product={p} onClick={(p) => { setSelected(p); setPage("detail"); }} />
          ))}
        </div>
      )}
    </div>
  );
}

const styles = {
  container: { maxWidth: 1100, margin: "0 auto", padding: "24px 16px" },
  heading: { fontSize: 26, color: "#1a1a2e", marginBottom: 16 },
  filters: { display: "flex", gap: 10, marginBottom: 24, flexWrap: "wrap" },
  filter: { padding: "7px 18px", borderRadius: 20, border: "1px solid #ddd", background: "#fff", cursor: "pointer", fontSize: 13 },
  activeFilter: { background: "#1a1a2e", color: "#fff", border: "1px solid #1a1a2e" },
  grid: { display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))", gap: 24 },
};
