import React, { useState, useEffect, useCallback } from "react";

const STATUS_COLOR = {
  IN_STOCK:     { bg: "#e8f5e9", color: "#2e7d32", label: "In Stock" },
  LOW_STOCK:    { bg: "#fff8e1", color: "#f57f17", label: "Low Stock" },
  OUT_OF_STOCK: { bg: "#ffebee", color: "#c62828", label: "Out of Stock" },
};

const URGENCY = (days) => {
  if (!days) return { label: "No Supplier", color: "#aaa", bg: "#f5f5f5" };
  if (days <= 3)  return { label: "Critical",  color: "#c62828", bg: "#ffebee" };
  if (days <= 7)  return { label: "Urgent",    color: "#e65100", bg: "#fff3e0" };
  if (days <= 14) return { label: "Soon",      color: "#f57f17", bg: "#fff8e1" };
  return           { label: "Planned",    color: "#2e7d32", bg: "#e8f5e9" };
};

export default function ForecastDashboard() {
  const [timelines, setTimelines]   = useState([]);
  const [suppliers, setSuppliers]   = useState([]);
  const [loading, setLoading]       = useState(true);
  const [tab, setTab]               = useState("Replenishment");
  const [filter, setFilter]         = useState("ALL");
  const [showForm, setShowForm]     = useState(false);
  const [toast, setToast]           = useState(null);
  const [form, setForm]             = useState({
    supplierName: "", productId: "", leadTimeDays: "", reorderQuantity: "", contactEmail: ""
  });

  const showToast = (msg, type = "success") => {
    setToast({ msg, type });
    setTimeout(() => setToast(null), 3000);
  };

  const fetchAll = useCallback(() => {
    setLoading(true);
    Promise.all([
      fetch("/api/inventory/replenishment").then(r => r.json()).catch(() => []),
      fetch("/api/inventory/suppliers").then(r => r.json()).catch(() => []),
    ]).then(([t, s]) => {
      setTimelines(Array.isArray(t) ? t : []);
      setSuppliers(Array.isArray(s) ? s : []);
      setLoading(false);
    });
  }, []);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  const handleAddSupplier = async () => {
    const { supplierName, productId, leadTimeDays, reorderQuantity, contactEmail } = form;
    if (!supplierName || !productId || !leadTimeDays || !reorderQuantity) {
      showToast("All fields except email are required", "error"); return;
    }
    try {
      const res = await fetch("/api/inventory/suppliers", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          supplierName, contactEmail,
          productId: parseInt(productId),
          leadTimeDays: parseInt(leadTimeDays),
          reorderQuantity: parseInt(reorderQuantity),
        }),
      });
      if (!res.ok) throw new Error("Failed");
      showToast(`Supplier "${supplierName}" added!`);
      setForm({ supplierName: "", productId: "", leadTimeDays: "", reorderQuantity: "", contactEmail: "" });
      setShowForm(false);
      fetchAll();
    } catch {
      showToast("Failed to add supplier", "error");
    }
  };

  const filtered = timelines.filter(t =>
    filter === "ALL" ? true :
    filter === "NEEDS_REORDER" ? t.replenishmentNeeded :
    t.stockStatus === filter
  );

  // Stats
  const needsReorder  = timelines.filter(t => t.replenishmentNeeded).length;
  const critical      = timelines.filter(t => t.replenishmentNeeded && t.leadTimeDays <= 3).length;
  const noSupplier    = timelines.filter(t => !t.leadTimeDays || t.leadTimeDays === 0).length;
  const avgLeadTime   = suppliers.length
    ? Math.round(suppliers.reduce((s, x) => s + x.leadTimeDays, 0) / suppliers.length)
    : 0;

  return (
    <div style={s.page}>
      {toast && (
        <div style={{ ...s.toast, background: toast.type === "error" ? "#e94560" : "#2e7d32" }}>
          {toast.type === "error" ? "❌" : "✅"} {toast.msg}
        </div>
      )}

      {/* Header */}
      <div style={s.header}>
        <div>
          <h2 style={s.title}>📈 Demand Forecasting & Replenishment</h2>
          <p style={s.sub}>Supplier lead times · Replenishment timelines · Stock forecasting</p>
        </div>
        <div style={s.headerBtns}>
          <button style={s.refreshBtn} onClick={fetchAll}>🔄 Refresh</button>
          <button style={s.addBtn} onClick={() => setShowForm(!showForm)}>➕ Add Supplier</button>
        </div>
      </div>

      {/* Stats */}
      <div style={s.statsRow}>
        {[
          { icon: "🔄", label: "Needs Reorder",  value: needsReorder,  color: "#e65100" },
          { icon: "🚨", label: "Critical (≤3d)",  value: critical,      color: "#c62828" },
          { icon: "🏭", label: "Suppliers",        value: suppliers.length, color: "#1a1a2e" },
          { icon: "⏱",  label: "Avg Lead Time",   value: `${avgLeadTime}d`, color: "#1565c0" },
          { icon: "⚠️", label: "No Supplier",      value: noSupplier,   color: "#f57f17" },
        ].map(c => (
          <div key={c.label} style={s.statCard}>
            <span style={s.statIcon}>{c.icon}</span>
            <span style={{ ...s.statVal, color: c.color }}>{c.value}</span>
            <span style={s.statLabel}>{c.label}</span>
          </div>
        ))}
      </div>

      {/* Add Supplier Form */}
      {showForm && (
        <div style={s.formCard}>
          <h3 style={s.formTitle}>🏭 Add Supplier</h3>
          <div style={s.formGrid}>
            {[
              { key: "supplierName",   label: "Supplier Name *",    placeholder: "e.g. TechSupply Co" },
              { key: "productId",      label: "Product ID *",        placeholder: "e.g. 1", type: "number" },
              { key: "leadTimeDays",   label: "Lead Time (days) *",  placeholder: "e.g. 7",  type: "number" },
              { key: "reorderQuantity",label: "Reorder Quantity *",  placeholder: "e.g. 100", type: "number" },
              { key: "contactEmail",   label: "Contact Email",       placeholder: "supplier@example.com", type: "email" },
            ].map(({ key, label, placeholder, type = "text" }) => (
              <div key={key} style={s.formGroup}>
                <label style={s.label}>{label}</label>
                <input style={s.input} type={type} placeholder={placeholder}
                  value={form[key]} onChange={e => setForm({ ...form, [key]: e.target.value })} />
              </div>
            ))}
          </div>
          <div style={s.formActions}>
            <button style={s.cancelBtn} onClick={() => setShowForm(false)}>Cancel</button>
            <button style={s.confirmBtn} onClick={handleAddSupplier}>Save Supplier</button>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div style={s.tabBar}>
        {["Replenishment", "Suppliers"].map(t => (
          <button key={t} style={{ ...s.tabBtn, ...(tab === t ? s.activeTab : {}) }} onClick={() => setTab(t)}>{t}</button>
        ))}
      </div>

      {/* ── TAB 1: Replenishment Timelines ── */}
      {tab === "Replenishment" && (
        <>
          <div style={s.filters}>
            {[
              { key: "ALL",          label: "All" },
              { key: "NEEDS_REORDER",label: "Needs Reorder" },
              { key: "LOW_STOCK",    label: "Low Stock" },
              { key: "OUT_OF_STOCK", label: "Out of Stock" },
              { key: "IN_STOCK",     label: "In Stock" },
            ].map(f => (
              <button key={f.key} style={{ ...s.filterBtn, ...(filter === f.key ? s.activeFilter : {}) }}
                onClick={() => setFilter(f.key)}>{f.label}</button>
            ))}
          </div>

          {loading ? <p style={s.loading}>Loading...</p> : (
            <div style={s.tableWrap}>
              <table style={s.table}>
                <thead>
                  <tr style={s.thead}>
                    {["Product", "Current Stock", "Status", "Supplier", "Lead Time", "Reorder Qty", "Replenishment Date", "Urgency"].map(h => (
                      <th key={h} style={s.th}>{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((t, i) => {
                    const sc  = STATUS_COLOR[t.stockStatus] || STATUS_COLOR.IN_STOCK;
                    const urg = URGENCY(t.replenishmentNeeded ? t.leadTimeDays : null);
                    const pct = Math.min((t.currentStock / ((t.lowStockThreshold || 1) * 3)) * 100, 100);
                    return (
                      <tr key={i} style={s.tr}>
                        <td style={s.td}><strong>{t.productName}</strong><br /><span style={s.pid}>ID: {t.productId}</span></td>
                        <td style={s.td}>
                          <strong style={{ color: sc.color, fontSize: 16 }}>{t.currentStock}</strong>
                          <div style={s.miniTrack}><div style={{ ...s.miniFill, width: `${pct}%`, background: sc.color }} /></div>
                        </td>
                        <td style={s.td}><span style={{ ...s.badge, background: sc.bg, color: sc.color }}>{sc.label}</span></td>
                        <td style={s.td}>{t.supplierName}</td>
                        <td style={s.td}>{t.leadTimeDays ? `${t.leadTimeDays} days` : "—"}</td>
                        <td style={s.td}>{t.reorderQuantity || "—"}</td>
                        <td style={s.td}>{t.replenishmentDate
                          ? new Date(t.replenishmentDate).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" })
                          : "—"}</td>
                        <td style={s.td}>
                          {t.replenishmentNeeded
                            ? <span style={{ ...s.badge, background: urg.bg, color: urg.color }}>{urg.label}</span>
                            : <span style={{ ...s.badge, background: "#e8f5e9", color: "#2e7d32" }}>OK</span>}
                        </td>
                      </tr>
                    );
                  })}
                  {filtered.length === 0 && (
                    <tr><td colSpan={8} style={s.empty}>No items found</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}

      {/* ── TAB 2: Suppliers ── */}
      {tab === "Suppliers" && (
        <div style={s.tableWrap}>
          <table style={s.table}>
            <thead>
              <tr style={s.thead}>
                {["Supplier Name", "Product ID", "Lead Time", "Reorder Qty", "Contact"].map(h => (
                  <th key={h} style={s.th}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {suppliers.map((sup, i) => (
                <tr key={i} style={s.tr}>
                  <td style={s.td}><strong>{sup.supplierName}</strong></td>
                  <td style={s.td}>{sup.productId}</td>
                  <td style={s.td}>
                    <span style={{ ...s.badge, ...URGENCY(sup.leadTimeDays) }}>
                      {sup.leadTimeDays} days
                    </span>
                  </td>
                  <td style={s.td}>{sup.reorderQuantity}</td>
                  <td style={s.td}>{sup.contactEmail || "—"}</td>
                </tr>
              ))}
              {suppliers.length === 0 && (
                <tr><td colSpan={5} style={s.empty}>No suppliers added yet. Click ➕ Add Supplier to get started.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

const s = {
  page:        { maxWidth: 1200, margin: "0 auto", padding: "24px 16px", fontFamily: "sans-serif", position: "relative" },
  toast:       { position: "fixed", top: 20, right: 20, color: "#fff", padding: "12px 24px", borderRadius: 8, fontSize: 14, zIndex: 9999, boxShadow: "0 4px 12px rgba(0,0,0,0.2)" },
  header:      { display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 20 },
  title:       { fontSize: 24, color: "#1a1a2e", margin: 0 },
  sub:         { fontSize: 12, color: "#888", margin: "4px 0 0" },
  headerBtns:  { display: "flex", gap: 10 },
  refreshBtn:  { padding: "8px 18px", background: "#1a1a2e", color: "#fff", border: "none", borderRadius: 8, cursor: "pointer", fontSize: 13 },
  addBtn:      { padding: "8px 18px", background: "#e94560", color: "#fff", border: "none", borderRadius: 8, cursor: "pointer", fontSize: 13 },
  statsRow:    { display: "grid", gridTemplateColumns: "repeat(5,1fr)", gap: 14, marginBottom: 20 },
  statCard:    { background: "#fff", borderRadius: 12, padding: "16px 12px", boxShadow: "0 2px 8px rgba(0,0,0,0.08)", display: "flex", flexDirection: "column", alignItems: "center", gap: 4 },
  statIcon:    { fontSize: 22 },
  statVal:     { fontSize: 26, fontWeight: "bold" },
  statLabel:   { fontSize: 11, color: "#888", textAlign: "center" },
  formCard:    { background: "#fff", borderRadius: 12, padding: 24, marginBottom: 20, boxShadow: "0 2px 8px rgba(0,0,0,0.08)" },
  formTitle:   { margin: "0 0 16px", color: "#1a1a2e", fontSize: 17 },
  formGrid:    { display: "grid", gridTemplateColumns: "repeat(3,1fr)", gap: 16, marginBottom: 16 },
  formGroup:   { display: "flex", flexDirection: "column" },
  label:       { fontSize: 12, color: "#555", marginBottom: 5, fontWeight: 600 },
  input:       { padding: "9px 12px", borderRadius: 8, border: "1px solid #ddd", fontSize: 14 },
  formActions: { display: "flex", gap: 10, justifyContent: "flex-end" },
  cancelBtn:   { padding: "9px 24px", background: "#f5f5f5", border: "none", borderRadius: 8, cursor: "pointer", fontSize: 14 },
  confirmBtn:  { padding: "9px 24px", background: "#e94560", color: "#fff", border: "none", borderRadius: 8, cursor: "pointer", fontSize: 14 },
  tabBar:      { display: "flex", gap: 4, marginBottom: 20, borderBottom: "2px solid #eee" },
  tabBtn:      { padding: "10px 24px", border: "none", background: "none", cursor: "pointer", fontSize: 14, color: "#888", borderBottom: "2px solid transparent", marginBottom: -2 },
  activeTab:   { color: "#1a1a2e", fontWeight: "bold", borderBottom: "2px solid #e94560" },
  filters:     { display: "flex", gap: 8, marginBottom: 16, flexWrap: "wrap" },
  filterBtn:   { padding: "6px 14px", borderRadius: 20, border: "1px solid #ddd", background: "#fff", cursor: "pointer", fontSize: 12 },
  activeFilter:{ background: "#1a1a2e", color: "#fff", border: "1px solid #1a1a2e" },
  loading:     { textAlign: "center", padding: 40, color: "#888" },
  tableWrap:   { background: "#fff", borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.08)", overflowX: "auto" },
  table:       { width: "100%", borderCollapse: "collapse" },
  thead:       { background: "#f8f9fa" },
  th:          { padding: "12px 14px", textAlign: "left", fontSize: 11, color: "#888", fontWeight: 700, textTransform: "uppercase", letterSpacing: 0.5, borderBottom: "2px solid #eee", whiteSpace: "nowrap" },
  tr:          { borderBottom: "1px solid #f5f5f5" },
  td:          { padding: "12px 14px", fontSize: 14, color: "#333" },
  pid:         { fontSize: 11, color: "#aaa" },
  miniTrack:   { width: 70, height: 5, background: "#f0f0f0", borderRadius: 4, overflow: "hidden", marginTop: 4 },
  miniFill:    { height: "100%", borderRadius: 4 },
  badge:       { padding: "3px 10px", borderRadius: 12, fontSize: 11, fontWeight: 600 },
  empty:       { textAlign: "center", padding: 40, color: "#aaa" },
};
