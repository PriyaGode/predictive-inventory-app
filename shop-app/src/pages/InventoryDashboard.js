import React, { useState, useEffect, useCallback } from "react";

const STATUS_COLOR = {
  IN_STOCK: { bg: "#e8f5e9", color: "#2e7d32", label: "In Stock" },
  LOW_STOCK: { bg: "#fff8e1", color: "#f57f17", label: "Low Stock" },
  OUT_OF_STOCK: { bg: "#ffebee", color: "#c62828", label: "Out of Stock" },
};

const TABS = ["Overview", "Stock Table", "Restock", "Add Product"];

const emptyProduct = { productName: "", category: "", price: "", description: "", image: "", quantity: "", lowStockThreshold: "" };

export default function InventoryDashboard() {
  const [tab, setTab] = useState("Overview");
  const [inventory, setInventory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("ALL");
  const [updateModal, setUpdateModal] = useState(null);
  const [stockChange, setStockChange] = useState("");
  const [reason, setReason] = useState("RESTOCK");
  const [toast, setToast] = useState(null);
  const [lastRefresh, setLastRefresh] = useState(new Date());
  const [newProduct, setNewProduct] = useState(emptyProduct);
  const [addLoading, setAddLoading] = useState(false);
  const [restockQtys, setRestockQtys] = useState({});
  const [restockLoading, setRestockLoading] = useState({});
  const [restockFilter, setRestockFilter] = useState("NEEDS_RESTOCK");

  const fetchInventory = useCallback(() => {
    fetch("http://localhost:8083/api/inventory")
      .then((r) => r.json())
      .then((data) => { setInventory(Array.isArray(data) ? data : []); setLoading(false); setLastRefresh(new Date()); })
      .catch(() => setLoading(false));
  }, []);

  useEffect(() => {
    fetchInventory();
    const interval = setInterval(fetchInventory, 15000);
    return () => clearInterval(interval);
  }, [fetchInventory]);

  const showToast = (msg, type = "success") => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3000);
  };

  const handleStockUpdate = () => {
    if (!stockChange || isNaN(stockChange)) return;
    const change = reason === "ORDER_PLACED" ? -Math.abs(parseInt(stockChange)) : Math.abs(parseInt(stockChange));
    fetch("http://localhost:8083/api/inventory/update-stock", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ productId: updateModal.productId, productName: updateModal.productName, quantityChange: change, reason }),
    })
      .then((r) => r.text())
      .then((msg) => { showToast(msg); setUpdateModal(null); setStockChange(""); fetchInventory(); })
      .catch(() => showToast("Update failed", "error"));
  };

  const handleAddProduct = async () => {
    const { productName, category, price, description, image, quantity, lowStockThreshold } = newProduct;
    if (!productName || !category || !price || !quantity || !lowStockThreshold) {
      showToast("All fields are required", "error"); return;
    }
    setAddLoading(true);
    try {
      // Step 1: Add to product-service so it shows in the shop
      const productRes = await fetch("http://localhost:8081/api/products", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: productName, category, price: parseFloat(price), description, image, rating: 0 }),
      });
      if (!productRes.ok) throw new Error("Product service failed");
      const savedProduct = await productRes.json();

      // Step 2: Add to inventory-service with the new product's ID
      const invRes = await fetch("http://localhost:8083/api/inventory", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          productId: savedProduct.id,
          productName,
          quantity: parseInt(quantity),
          lowStockThreshold: parseInt(lowStockThreshold),
        }),
      });
      if (!invRes.ok) throw new Error("Inventory service failed");

      showToast(`"${productName}" added to shop & inventory!`);
      setNewProduct(emptyProduct);
      fetchInventory();
      setTab("Stock Table");
    } catch (e) {
      showToast(e.message || "Failed to add product", "error");
    } finally {
      setAddLoading(false);
    }
  };

  const filtered = inventory
    .filter((i) => filter === "ALL" || i.status === filter)
    .filter((i) => i.productName.toLowerCase().includes(search.toLowerCase()));

  const stats = {
    total: inventory.length,
    inStock: inventory.filter((i) => i.status === "IN_STOCK").length,
    lowStock: inventory.filter((i) => i.status === "LOW_STOCK").length,
    outOfStock: inventory.filter((i) => i.status === "OUT_OF_STOCK").length,
  };

  return (
    <div style={s.page}>
      {toast && (
        <div style={{ ...s.toast, background: toast.type === "error" ? "#e94560" : "#2e7d32" }}>
          {toast.type === "success" ? "✅" : "❌"} {toast.msg}
        </div>
      )}

      {/* Header */}
      <div style={s.header}>
        <div>
          <h2 style={s.title}>📦 Inventory Dashboard</h2>
          <p style={s.subtitle}>Last updated: {lastRefresh.toLocaleTimeString()} · Auto-refreshes every 15s</p>
        </div>
        <button style={s.refreshBtn} onClick={fetchInventory}>🔄 Refresh</button>
      </div>

      {/* Tabs */}
      <div style={s.tabBar}>
        {TABS.map((t) => (
          <button key={t} style={{ ...s.tabBtn, ...(tab === t ? s.activeTab : {}) }} onClick={() => setTab(t)}>{t}</button>
        ))}
      </div>

      {/* ── TAB 1: Overview ── */}
      {tab === "Overview" && (
        <>
          <div style={s.statsRow}>
            {[
              { label: "Total Products", value: stats.total, color: "#1a1a2e", icon: "📦" },
              { label: "In Stock", value: stats.inStock, color: "#2e7d32", icon: "✅" },
              { label: "Low Stock", value: stats.lowStock, color: "#f57f17", icon: "⚠️" },
              { label: "Out of Stock", value: stats.outOfStock, color: "#c62828", icon: "❌" },
            ].map((c) => (
              <div key={c.label} style={s.statCard}>
                <span style={s.statIcon}>{c.icon}</span>
                <span style={{ ...s.statValue, color: c.color }}>{c.value}</span>
                <span style={s.statLabel}>{c.label}</span>
              </div>
            ))}
          </div>

          {/* Distribution Bar */}
          <div style={s.card}>
            <p style={s.cardTitle}>Stock Distribution</p>
            <div style={s.barTrack}>
              {stats.total > 0 && (
                <>
                  <div style={{ ...s.barFill, width: `${(stats.inStock / stats.total) * 100}%`, background: "#2e7d32" }} />
                  <div style={{ ...s.barFill, width: `${(stats.lowStock / stats.total) * 100}%`, background: "#f57f17" }} />
                  <div style={{ ...s.barFill, width: `${(stats.outOfStock / stats.total) * 100}%`, background: "#c62828" }} />
                </>
              )}
            </div>
            <div style={s.legend}>
              {[["#2e7d32", "In Stock"], ["#f57f17", "Low Stock"], ["#c62828", "Out of Stock"]].map(([c, l]) => (
                <span key={l} style={s.legendItem}><span style={{ ...s.dot, background: c }} />{l}</span>
              ))}
            </div>
          </div>

          {/* Low Stock Alerts */}
          {inventory.filter((i) => i.status !== "IN_STOCK").length > 0 && (
            <div style={s.card}>
              <p style={s.cardTitle}>⚠️ Alerts</p>
              <div style={s.alertList}>
                {inventory.filter((i) => i.status !== "IN_STOCK").map((item) => {
                  const sc = STATUS_COLOR[item.status];
                  return (
                    <div key={item.id} style={{ ...s.alertItem, borderLeft: `4px solid ${sc.color}`, background: sc.bg }}>
                      <span style={s.alertName}>{item.productName}</span>
                      <span style={s.alertQty}>Qty: <strong>{item.quantity}</strong></span>
                      <span style={{ ...s.badge, background: sc.bg, color: sc.color }}>{sc.label}</span>
                      <button style={s.smallBtn} onClick={() => { setUpdateModal(item); setStockChange(""); setReason("RESTOCK"); setTab("Stock Table"); }}>
                        Update
                      </button>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </>
      )}

      {/* ── TAB 2: Stock Table ── */}
      {tab === "Stock Table" && (
        <>
          <div style={s.controls}>
            <input style={s.search} placeholder="🔍 Search product..." value={search} onChange={(e) => setSearch(e.target.value)} />
            <div style={s.filters}>
              {["ALL", "IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"].map((f) => (
                <button key={f} style={{ ...s.filterBtn, ...(filter === f ? s.activeFilter : {}) }} onClick={() => setFilter(f)}>
                  {f.replace(/_/g, " ")}
                </button>
              ))}
            </div>
          </div>

          {loading ? <p style={s.loading}>Loading...</p> : (
            <div style={s.tableWrap}>
              <table style={s.table}>
                <thead>
                  <tr style={s.thead}>
                    {["#", "Product Name", "Product ID", "Quantity", "Threshold", "Stock Level", "Status", "Last Updated", "Actions"].map((h) => (
                      <th key={h} style={s.th}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((item, idx) => {
                    const pct = Math.min((item.quantity / (item.lowStockThreshold * 3)) * 100, 100);
                    const sc = STATUS_COLOR[item.status] || STATUS_COLOR.IN_STOCK;
                    return (
                      <tr key={item.id} style={s.tr}>
                        <td style={s.td}>{idx + 1}</td>
                        <td style={s.td}><strong>{item.productName}</strong></td>
                        <td style={s.td}>{item.productId}</td>
                        <td style={s.td}><strong style={{ color: sc.color, fontSize: 16 }}>{item.quantity}</strong></td>
                        <td style={s.td}>{item.lowStockThreshold}</td>
                        <td style={s.td}>
                          <div style={s.miniTrack}>
                            <div style={{ ...s.miniFill, width: `${pct}%`, background: sc.color }} />
                          </div>
                          <span style={{ fontSize: 10, color: "#aaa" }}>{Math.round(pct)}%</span>
                        </td>
                        <td style={s.td}>
                          <span style={{ ...s.badge, background: sc.bg, color: sc.color }}>{sc.label}</span>
                        </td>
                        <td style={s.td}>{new Date(item.lastUpdated).toLocaleString()}</td>
                        <td style={s.td}>
                          <button style={s.updateBtn} onClick={() => { setUpdateModal(item); setStockChange(""); setReason("RESTOCK"); }}>
                            ✏️ Update
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                  {filtered.length === 0 && (
                    <tr><td colSpan={9} style={s.empty}>No items found</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {/* ── TAB 3: Restock ── */}
      {tab === "Restock" && (() => {
        const restockItems = restockFilter === "NEEDS_RESTOCK"
          ? inventory.filter((i) => i.status !== "IN_STOCK")
          : inventory;
        return (
          <>
            <div style={s.controls}>
              <div style={s.filters}>
                {[["NEEDS_RESTOCK", "⚠️ Needs Restock"], ["ALL", "All Products"]].map(([f, label]) => (
                  <button key={f} style={{ ...s.filterBtn, ...(restockFilter === f ? s.activeFilter : {}) }}
                    onClick={() => setRestockFilter(f)}>{label}</button>
                ))}
              </div>
              <span style={{ fontSize: 13, color: "#888", marginLeft: "auto" }}>
                {restockItems.length} item{restockItems.length !== 1 ? "s" : ""}
              </span>
            </div>

            {restockItems.length === 0 ? (
              <div style={{ textAlign: "center", padding: 60, color: "#888" }}>
                <p style={{ fontSize: 32 }}>✅</p>
                <p>All products are fully stocked!</p>
              </div>
            ) : (
              <div style={s.tableWrap}>
                <table style={s.table}>
                  <thead>
                    <tr style={s.thead}>
                      {["Product", "Current Qty", "Threshold", "Status", "Add Stock", "Action"].map((h) => (
                        <th key={h} style={s.th}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {restockItems.map((item) => {
                      const sc = STATUS_COLOR[item.status] || STATUS_COLOR.IN_STOCK;
                      const qty = restockQtys[item.productId] || "";
                      const isLoading = restockLoading[item.productId];

                      const doRestock = () => {
                        const amount = parseInt(qty);
                        if (!amount || amount <= 0) return;
                        setRestockLoading((p) => ({ ...p, [item.productId]: true }));
                        fetch("/api/inventory/update-stock", {
                          method: "POST",
                          headers: { "Content-Type": "application/json" },
                          body: JSON.stringify({
                            productId: item.productId,
                            productName: item.productName,
                            quantityChange: amount,
                            reason: "RESTOCK",
                          }),
                        })
                          .then((r) => r.text())
                          .then(() => {
                            showToast(`✅ Restocked "${item.productName}" +${amount}`);
                            setRestockQtys((p) => ({ ...p, [item.productId]: "" }));
                            fetchInventory();
                          })
                          .catch(() => showToast("Restock failed", "error"))
                          .finally(() => setRestockLoading((p) => ({ ...p, [item.productId]: false })));
                      };

                      return (
                        <tr key={item.id} style={s.tr}>
                          <td style={s.td}>
                            <strong>{item.productName}</strong>
                            <br /><span style={{ fontSize: 11, color: "#aaa" }}>ID: {item.productId}</span>
                          </td>
                          <td style={s.td}>
                            <strong style={{ color: sc.color, fontSize: 16 }}>{item.quantity}</strong>
                          </td>
                          <td style={s.td}>{item.lowStockThreshold}</td>
                          <td style={s.td}>
                            <span style={{ ...s.badge, background: sc.bg, color: sc.color }}>{sc.label}</span>
                          </td>
                          <td style={s.td}>
                            <input
                              style={{ ...s.input, width: 90, padding: "7px 10px" }}
                              type="number" min="1" placeholder="Qty"
                              value={qty}
                              onChange={(e) => setRestockQtys((p) => ({ ...p, [item.productId]: e.target.value }))}
                              onKeyDown={(e) => e.key === "Enter" && doRestock()}
                            />
                          </td>
                          <td style={s.td}>
                            <button
                              style={{ ...s.updateBtn, background: qty && parseInt(qty) > 0 ? "#2e7d32" : "#ccc", opacity: isLoading ? 0.7 : 1 }}
                              onClick={doRestock}
                              disabled={!qty || parseInt(qty) <= 0 || isLoading}
                            >
                              {isLoading ? "..." : "➕ Restock"}
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </>
        );
      })()}

      {/* ── TAB 4: Add Product ── */}
      {tab === "Add Product" && (
        <div style={s.formWrap}>
          <div style={s.formCard}>
            <h3 style={s.formTitle}>➕ Add New Product to Inventory</h3>
            <p style={s.formSub}>Fill in the details to add a new product to the inventory system.</p>

            <div style={s.formGrid}>
              {[
                { label: "Product Name *", key: "productName", type: "text", placeholder: "e.g. Bluetooth Speaker", span: 2 },
                { label: "Category *", key: "category", type: "text", placeholder: "e.g. Electronics" },
                { label: "Price ($) *", key: "price", type: "number", placeholder: "e.g. 49.99" },
                { label: "Initial Quantity *", key: "quantity", type: "number", placeholder: "e.g. 100" },
                { label: "Low Stock Threshold *", key: "lowStockThreshold", type: "number", placeholder: "e.g. 10" },
                { label: "Image URL", key: "image", type: "text", placeholder: "https://...", span: 2 },
                { label: "Description", key: "description", type: "text", placeholder: "Short product description", span: 2 },
              ].map(({ label, key, type, placeholder, span }) => (
                <div key={key} style={{ ...s.formGroup, gridColumn: span === 2 ? "1 / -1" : undefined }}>
                  <label style={s.label}>{label}</label>
                  <input
                    style={s.input}
                    type={type}
                    placeholder={placeholder}
                    value={newProduct[key]}
                    onChange={(e) => setNewProduct({ ...newProduct, [key]: e.target.value })}
                  />
                </div>
              ))}
            </div>

            {/* Preview */}
            {newProduct.productName && newProduct.quantity && (
              <div style={s.preview}>
                <p style={s.previewTitle}>Preview</p>
                <div style={s.previewRow}>
                  {newProduct.image && <img src={newProduct.image} alt="" style={{ width: 48, height: 48, objectFit: "cover", borderRadius: 6 }} onError={(e) => e.target.style.display="none"} />}
                  <span>📦 <strong>{newProduct.productName}</strong></span>
                  {newProduct.category && <span>🏷 {newProduct.category}</span>}
                  {newProduct.price && <span>💲{newProduct.price}</span>}
                  <span>Qty: {newProduct.quantity}</span>
                  <span style={{ ...s.badge, ...STATUS_COLOR[parseInt(newProduct.quantity) === 0 ? "OUT_OF_STOCK" : parseInt(newProduct.quantity) <= parseInt(newProduct.lowStockThreshold || 0) ? "LOW_STOCK" : "IN_STOCK"] }}>
                    {parseInt(newProduct.quantity) === 0 ? "Out of Stock" : parseInt(newProduct.quantity) <= parseInt(newProduct.lowStockThreshold || 0) ? "Low Stock" : "In Stock"}
                  </span>
                </div>
              </div>
            )}

            <div style={s.formActions}>
              <button style={s.cancelBtn} onClick={() => setNewProduct(emptyProduct)}>🗑 Clear</button>
              <button style={{ ...s.confirmBtn, opacity: addLoading ? 0.7 : 1 }} onClick={handleAddProduct} disabled={addLoading}>
                {addLoading ? "Adding..." : "✅ Add Product"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Update Stock Modal */}
      {updateModal && (
        <div style={s.overlay}>
          <div style={s.modal}>
            <h3 style={s.modalTitle}>✏️ Update Stock</h3>
            <p style={s.modalSub}><strong>{updateModal.productName}</strong> · Current Qty: <strong style={{ color: "#e94560" }}>{updateModal.quantity}</strong></p>
            <label style={s.label}>Reason</label>
            <select style={s.select} value={reason} onChange={(e) => setReason(e.target.value)}>
              <option value="RESTOCK">📦 Restock (+)</option>
              <option value="ORDER_PLACED">🛒 Order Placed (−)</option>
              <option value="ORDER_CANCELLED">↩️ Order Cancelled (+)</option>
              <option value="ADJUSTMENT">🔧 Manual Adjustment</option>
            </select>
            <label style={s.label}>Quantity</label>
            <input style={s.input} type="number" min="1" placeholder="Enter quantity" value={stockChange} onChange={(e) => setStockChange(e.target.value)} />
            <div style={s.modalBtns}>
              <button style={s.cancelBtn} onClick={() => setUpdateModal(null)}>Cancel</button>
              <button style={s.confirmBtn} onClick={handleStockUpdate}>Confirm</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const s = {
  page: { maxWidth: 1200, margin: "0 auto", padding: "24px 16px", fontFamily: "sans-serif", position: "relative" },
  toast: { position: "fixed", top: 20, right: 20, color: "#fff", padding: "12px 24px", borderRadius: 8, fontSize: 14, zIndex: 9999, boxShadow: "0 4px 12px rgba(0,0,0,0.2)" },
  header: { display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 },
  title: { fontSize: 24, color: "#1a1a2e", margin: 0 },
  subtitle: { fontSize: 12, color: "#888", margin: "4px 0 0" },
  refreshBtn: { padding: "8px 18px", background: "#1a1a2e", color: "#fff", border: "none", borderRadius: 8, cursor: "pointer", fontSize: 13 },
  tabBar: { display: "flex", gap: 4, marginBottom: 24, borderBottom: "2px solid #eee", paddingBottom: 0 },
  tabBtn: { padding: "10px 24px", border: "none", background: "none", cursor: "pointer", fontSize: 14, color: "#888", borderBottom: "2px solid transparent", marginBottom: -2 },
  activeTab: { color: "#1a1a2e", fontWeight: "bold", borderBottom: "2px solid #e94560" },
  statsRow: { display: "grid", gridTemplateColumns: "repeat(4,1fr)", gap: 16, marginBottom: 20 },
  statCard: { background: "#fff", borderRadius: 12, padding: "20px 16px", boxShadow: "0 2px 8px rgba(0,0,0,0.08)", display: "flex", flexDirection: "column", alignItems: "center", gap: 6 },
  statIcon: { fontSize: 24 },
  statValue: { fontSize: 32, fontWeight: "bold" },
  statLabel: { fontSize: 12, color: "#888" },
  card: { background: "#fff", borderRadius: 12, padding: 20, marginBottom: 20, boxShadow: "0 2px 8px rgba(0,0,0,0.08)" },
  cardTitle: { margin: "0 0 14px", fontWeight: "bold", color: "#1a1a2e", fontSize: 15 },
  barTrack: { display: "flex", height: 16, borderRadius: 8, overflow: "hidden", background: "#f0f0f0" },
  barFill: { height: "100%", transition: "width 0.5s" },
  legend: { display: "flex", gap: 20, marginTop: 10 },
  legendItem: { display: "flex", alignItems: "center", gap: 6, fontSize: 12, color: "#555" },
  dot: { width: 10, height: 10, borderRadius: "50%", display: "inline-block" },
  alertList: { display: "flex", flexDirection: "column", gap: 10 },
  alertItem: { display: "flex", alignItems: "center", gap: 16, padding: "10px 16px", borderRadius: 8 },
  alertName: { flex: 1, fontWeight: "bold", fontSize: 14, color: "#1a1a2e" },
  alertQty: { fontSize: 13, color: "#555" },
  smallBtn: { padding: "5px 12px", background: "#1a1a2e", color: "#fff", border: "none", borderRadius: 6, cursor: "pointer", fontSize: 12 },
  controls: { display: "flex", gap: 16, marginBottom: 16, flexWrap: "wrap", alignItems: "center" },
  search: { padding: "9px 14px", borderRadius: 8, border: "1px solid #ddd", fontSize: 14, flex: 1, minWidth: 200 },
  filters: { display: "flex", gap: 8, flexWrap: "wrap" },
  filterBtn: { padding: "7px 14px", borderRadius: 20, border: "1px solid #ddd", background: "#fff", cursor: "pointer", fontSize: 12 },
  activeFilter: { background: "#1a1a2e", color: "#fff", border: "1px solid #1a1a2e" },
  loading: { textAlign: "center", padding: 40, color: "#888" },
  tableWrap: { background: "#fff", borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.08)", overflowX: "auto" },
  table: { width: "100%", borderCollapse: "collapse" },
  thead: { background: "#f8f9fa" },
  th: { padding: "13px 14px", textAlign: "left", fontSize: 11, color: "#888", fontWeight: 700, textTransform: "uppercase", letterSpacing: 0.5, borderBottom: "2px solid #eee", whiteSpace: "nowrap" },
  tr: { borderBottom: "1px solid #f5f5f5" },
  td: { padding: "13px 14px", fontSize: 14, color: "#333" },
  miniTrack: { width: 80, height: 6, background: "#f0f0f0", borderRadius: 4, overflow: "hidden", marginBottom: 2 },
  miniFill: { height: "100%", borderRadius: 4, transition: "width 0.4s" },
  badge: { padding: "4px 10px", borderRadius: 12, fontSize: 11, fontWeight: 600 },
  updateBtn: { padding: "6px 14px", background: "#1a1a2e", color: "#fff", border: "none", borderRadius: 6, cursor: "pointer", fontSize: 12 },
  input: { padding: "10px 12px", borderRadius: 8, border: "1px solid #ddd", fontSize: 14, outline: "none", boxSizing: "border-box" },
  empty: { textAlign: "center", padding: 40, color: "#aaa" },
  formWrap: { display: "flex", justifyContent: "center" },
  formCard: { background: "#fff", borderRadius: 12, padding: 36, width: "100%", maxWidth: 600, boxShadow: "0 2px 12px rgba(0,0,0,0.1)" },
  formTitle: { margin: "0 0 6px", color: "#1a1a2e", fontSize: 20 },
  formSub: { color: "#888", fontSize: 13, marginBottom: 28 },
  formGrid: { display: "grid", gridTemplateColumns: "1fr 1fr", gap: 20, marginBottom: 24 },
  formGroup: { display: "flex", flexDirection: "column" },
  label: { fontSize: 12, color: "#555", marginBottom: 6, fontWeight: 600 },
  input: { padding: "10px 12px", borderRadius: 8, border: "1px solid #ddd", fontSize: 14, outline: "none", boxSizing: "border-box" },
  preview: { background: "#f8f9fa", borderRadius: 8, padding: 16, marginBottom: 24 },
  previewTitle: { fontSize: 12, color: "#888", margin: "0 0 10px", fontWeight: 600 },
  previewRow: { display: "flex", alignItems: "center", gap: 16, flexWrap: "wrap", fontSize: 14, color: "#333" },
  formActions: { display: "flex", gap: 12 },
  overlay: { position: "fixed", inset: 0, background: "rgba(0,0,0,0.5)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000 },
  modal: { background: "#fff", borderRadius: 12, padding: 32, width: 380, boxShadow: "0 8px 32px rgba(0,0,0,0.2)" },
  modalTitle: { margin: "0 0 4px", color: "#1a1a2e", fontSize: 18 },
  modalSub: { color: "#888", fontSize: 13, marginBottom: 20 },
  select: { width: "100%", padding: "9px 12px", borderRadius: 8, border: "1px solid #ddd", fontSize: 14, marginBottom: 16 },
  modalBtns: { display: "flex", gap: 12 },
  cancelBtn: { flex: 1, padding: "10px 0", background: "#f5f5f5", border: "none", borderRadius: 8, cursor: "pointer", fontSize: 14 },
  confirmBtn: { flex: 1, padding: "10px 0", background: "#e94560", color: "#fff", border: "none", borderRadius: 8, cursor: "pointer", fontSize: 14 },
};
