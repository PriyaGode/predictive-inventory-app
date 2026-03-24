import React, { useState } from 'react';
import { CartProvider } from './context/CartContext';
import Navbar from './components/Navbar';
import Home from './pages/Home';
import ProductDetail from './pages/ProductDetail';
import Cart from './pages/Cart';
import InventoryDashboard from './pages/InventoryDashboard';
import Orders from './pages/Orders';
import ForecastDashboard from './pages/ForecastDashboard';

function App() {
  const [page, setPage] = useState('home');
  const [selected, setSelected] = useState(null);

  return (
    <CartProvider>
      <div style={{ minHeight: '100vh', background: '#f5f5f5', fontFamily: 'sans-serif' }}>
        <Navbar page={page} setPage={setPage} />
        {page === 'home' && <Home setPage={setPage} setSelected={setSelected} />}
        {page === 'detail' && <ProductDetail product={selected} setPage={setPage} />}
        {page === 'cart' && <Cart setPage={setPage} />}
        {page === 'inventory' && <InventoryDashboard />}
        {page === 'orders' && <Orders setPage={setPage} />}
        {page === 'forecast' && <ForecastDashboard />}
      </div>
    </CartProvider>
  );
}

export default App;
