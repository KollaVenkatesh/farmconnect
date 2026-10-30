import { useEffect, useMemo, useState } from "react";
import { apiRequest } from "../api.js";
import { indianLanguages, translate } from "../i18n.js";
import FarmConnectIcon from "./FarmConnectIcon";
import AiCookAssistant from "./AiCookAssistant";
import AiChatWidget from "./AiChatWidget";

const productImages = {
  tomato: "https://images.unsplash.com/photo-1727480155110-0d510faed341?auto=format&fit=crop&w=900&q=85",
  rice: "https://hariomatta.com/cdn/shop/collections/ricecatagory.png?v=1749561039&width=900",
  mango: "https://cdn.shopify.com/s/files/1/0039/6096/3118/files/best_hapus_in_bangalore.jpg?v=1743313710",
  chilli: "https://images.unsplash.com/photo-1756364898071-537c252977a1?auto=format&fit=crop&w=900&q=85",
  nuts: "https://images.unsplash.com/photo-1756694414422-30c2c8da75d2?auto=format&fit=crop&w=900&q=85",
};

const itemImages = {
  "red onion": "https://images.unsplash.com/photo-1508747703725-719777637510?auto=format&fit=crop&w=900&q=85",
  "potato": "https://images.unsplash.com/photo-1566055803687-0665ef48df90?auto=format&fit=crop&w=900&q=85",
  "carrot": "https://images.unsplash.com/photo-1445282768818-728615cc910a?auto=format&fit=crop&w=900&q=85",
  "cabbage": "/cabbage.jpg",
  "cauliflower": "https://images.unsplash.com/photo-1692956706779-576c151ec712?auto=format&fit=crop&w=900&h=600&q=85",
  "brinjal": "https://images.unsplash.com/photo-1667355069957-fd5829016331?auto=format&fit=crop&w=900&h=600&q=85",
  "ladies finger": "/ladyfinger.jpg",
  "spinach": "/spinach.jpg",
  "beans": "/beans.png",
  "cucumber": "/cucumber.png",
  "pumpkin": "/pumpkin.webp",
  "banana": "/banana.jpg",
  "papaya": "/papaya.jpg",
  "guava": "/guava.jpeg",
  "watermelon": "/watermelon.jpg",
  "pomegranate": "/pomegranate.jpg",
  "foxtail millet": "/foxtail-millet.png",
  "groundnut": "https://images.unsplash.com/photo-1524594345772-c953a3ae53e8?auto=format&fit=crop&fm=jpg&w=900&q=85",
  "peanut": "/raw-peanuts.jpg",
  "grapes": "https://images.unsplash.com/photo-1535521344776-da06aeead46f?auto=format&fit=crop&w=900&q=85",
};

function getProductColor(product) {
  const name = product.name.toLowerCase();
  if (name.includes("cabbage")) return "cabbage";
  if (name.includes("cauliflower")) return "cauliflower";
  if (name.includes("brinjal") || name.includes("eggplant")) return "brinjal";
  if (name.includes("ladies finger") || name.includes("okra")) return "ladyfinger";
  if (name.includes("tomato")) return "tomato";
  if (name.includes("rice")) return "rice";
  if (name.includes("mango")) return "mango";
  if (name.includes("chilli") || name.includes("chili")) return "chilli";
  if (name.includes("groundnut") || name.includes("peanut")) return "nuts";
  return "tomato";
}

function getProductImage(product) {
  const name = product.name.toLowerCase();
  const matchingItem = Object.keys(itemImages).find((item) => name.includes(item));
  return product.imageUrl || itemImages[matchingItem] || productImages[getProductColor(product)];
}

async function imageFileToDataUrl(file) {
  const bitmap = await createImageBitmap(file);
  const scale = Math.min(1, 1280 / Math.max(bitmap.width, bitmap.height));
  const canvas = document.createElement("canvas");
  canvas.width = Math.max(1, Math.round(bitmap.width * scale));
  canvas.height = Math.max(1, Math.round(bitmap.height * scale));
  const context = canvas.getContext("2d");
  context.fillStyle = "#ffffff";
  context.fillRect(0, 0, canvas.width, canvas.height);
  context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();
  return canvas.toDataURL("image/jpeg", 0.82);
}

const categories = ["All produce", "Vegetables", "Fruits", "Grains", "Nuts", "Pulses", "Spices", "Herbs", "Dairy"];

function loadRazorpayCheckout() {
  if (window.Razorpay) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = "https://checkout.razorpay.com/v1/checkout.js";
    script.onload = resolve;
    script.onerror = () => reject(new Error("Razorpay Checkout could not be loaded. Check your connection and try again."));
    document.body.appendChild(script);
  });
}

function Marketplace({ currentUser, onLogout }) {
  const isFarmer = currentUser.role === "FARMER" || currentUser.role === "Farmer";
  const [language, setLanguage] = useState(() => {
    const savedLanguage = localStorage.getItem("farmconnect_language");
    return indianLanguages.some((option) => option.code === savedLanguage) ? savedLanguage : "en";
  });
  const [preferencesUserId, setPreferencesUserId] = useState(null);
  const t = (key) => translate(language, key);
  const [cart, setCart] = useState({ count: 0, items: [] });
  const [orderHistory, setOrderHistory] = useState([]);
  const [listing, setListing] = useState({ productName: "", category: "", description: "", price: "", stock: "", unit: "kg", imageUrl: "", location: "" });
  const [editingProductId, setEditingProductId] = useState(null);
  const [isPublishing, setIsPublishing] = useState(false);
  const [isLocating, setIsLocating] = useState(false);
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("All produce");
  const [selectedItem, setSelectedItem] = useState("All items");
  const [products, setProducts] = useState([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [cartOpen, setCartOpen] = useState(false);
  async function toggleCart() {
    const opening = !cartOpen;
    setCartOpen(opening);
    if (opening) {
      try { setOrderHistory(await apiRequest("/cart/orders", { cache: "no-store" })); }
      catch { setOrderHistory([]); }
    }
  }
  const [updatingProduct, setUpdatingProduct] = useState(null);
  const [updatingCartItem, setUpdatingCartItem] = useState(null);
  const [deletingProductId, setDeletingProductId] = useState(null);
  const paymentMethods = [
    { value: "CASH_ON_DELIVERY", label: "Cash on Delivery" },
    { value: "UPI", label: "UPI" },
    { value: "CARD", label: "Debit/Credit Card" },
    { value: "NET_BANKING", label: "Net Banking" },
  ];
  const [selectedPaymentMethod, setSelectedPaymentMethod] = useState("UPI");
  const [deliveryAddress, setDeliveryAddress] = useState("");
  const [isCheckingOut, setIsCheckingOut] = useState(false);
  const [checkoutMessage, setCheckoutMessage] = useState("");
  const [payoutAccount, setPayoutAccount] = useState({ loading: true, configured: false, status: "NOT_CONFIGURED" });
  const [payoutForm, setPayoutForm] = useState({
    legalBusinessName: currentUser.name ?? "",
    phone: "",
    pan: "",
    beneficiaryName: currentUser.name ?? "",
    accountNumber: "",
    ifsc: "",
    termsAccepted: false,
  });
  const [payoutError, setPayoutError] = useState("");
  const [isSavingPayout, setIsSavingPayout] = useState(false);

  // â”€â”€ Notification Bell â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifOpen, setNotifOpen] = useState(false);
  const [notifLoading, setNotifLoading] = useState(false);

  useEffect(() => {
    document.documentElement.lang = language;
    document.documentElement.dir = ["ar", "ks", "sd", "ur"].includes(language) ? "rtl" : "ltr";
    localStorage.setItem("farmconnect_language", language);
  }, [language]);

  useEffect(() => {
    let cancelled = false;
    apiRequest("/users/me/preferences")
      .then((preferences) => {
        if (!cancelled) {
          const preferredLanguage = preferences.language;
          if (indianLanguages.some((option) => option.code === preferredLanguage)) {
            setLanguage(preferredLanguage);
          }
        }
      })
      .catch((requestError) => {
        if (!cancelled) setError(`Could not load saved language preference: ${requestError.message}`);
      })
      .finally(() => {
        if (!cancelled) setPreferencesUserId(currentUser.userId);
      });
    return () => { cancelled = true; };
  }, [currentUser.userId]);

  async function changeLanguage(event) {
    const preferredLanguage = event.target.value;
    setLanguage(preferredLanguage);
    setError("");
    try {
      await apiRequest("/users/me/preferences", {
        method: "PUT",
        body: JSON.stringify({ language: preferredLanguage }),
      });
    } catch (requestError) {
      setError(`Language changed for this session but could not be saved: ${requestError.message}`);
    }
  }

  useEffect(() => {
    let cancelled = false;
    let hasLoadedProducts = false;
    async function loadMarketplace() {
      try {
        const productRows = await apiRequest("/products", { cache: "no-store" });
        if (!cancelled) {
          const nextProducts = productRows.map((product) => {
            const color = getProductColor(product);
            return { ...product, color, image: getProductImage(product) };
          });
          setProducts((current) => {
            const unchanged = current.length === nextProducts.length && current.every((product, index) =>
              Object.keys(nextProducts[index]).every((key) => product[key] === nextProducts[index][key]));
            return unchanged ? current : nextProducts;
          });
          hasLoadedProducts = true;
          setError("");
        }
      } catch (requestError) {
        if (!cancelled && !hasLoadedProducts) setError(requestError.message);
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }
    loadMarketplace();
    const refreshTimer = window.setInterval(loadMarketplace, 3000);
    return () => {
      cancelled = true;
      window.clearInterval(refreshTimer);
    };
  }, []);

  useEffect(() => {
    if (isFarmer) return undefined;
    let cancelled = false;
    apiRequest("/cart").then((cartData) => {
      if (!cancelled) setCart(cartData);
    }).catch((requestError) => {
      if (!cancelled) setError(requestError.message);
    });
    return () => { cancelled = true; };
  }, [currentUser.userId, isFarmer]);

  // â”€â”€ Fetch notifications on mount & when notifOpen changes â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
  function fetchNotifications() {
    setNotifLoading(true);
    apiRequest("/notifications")
      .then((data) => {
        setNotifications(data.notifications || []);
        setUnreadCount(data.unreadCount || 0);
      })
      .catch(() => {})
      .finally(() => setNotifLoading(false));
  }

  useEffect(() => {
    const initialFetch = window.setTimeout(fetchNotifications, 0);
    const notifTimer = window.setInterval(fetchNotifications, 30000);
    return () => {
      window.clearTimeout(initialFetch);
      window.clearInterval(notifTimer);
    };
  }, [currentUser.userId]);

  function markNotificationRead(id) {
    apiRequest(`/notifications/${id}/read`, { method: "PUT" })
      .then(() => {
        setNotifications((prev) => prev.map((n) => n.id === id ? { ...n, read: true } : n));
        setUnreadCount((c) => Math.max(0, c - 1));
      })
      .catch(() => {});
  }

  useEffect(() => {
    if (!isFarmer) return undefined;
    let cancelled = false;
    apiRequest("/farmer/payout-account", { cache: "no-store" })
      .then((account) => {
        if (!cancelled) setPayoutAccount({ loading: false, ...account });
      })
      .catch((requestError) => {
        if (!cancelled) {
          setPayoutAccount({ loading: false, configured: false, status: "NOT_CONFIGURED" });
          setPayoutError(requestError.message);
        }
      });
    return () => { cancelled = true; };
  }, [currentUser.userId, isFarmer]);

  const visibleProducts = useMemo(() => products.filter((product) => {
    const matchesSearch = `${product.name} ${product.farmer} ${product.location}`.toLowerCase().includes(search.trim().toLowerCase());
    const matchesCategory = category === "All produce" || product.category === category;
    const matchesItem = selectedItem === "All items" || product.name === selectedItem;
    return matchesSearch && matchesCategory && matchesItem;
  }), [products, search, category, selectedItem]);

  const availableItems = useMemo(() => [...new Set(products
    .filter((product) => category === "All produce" || product.category === category)
    .map((product) => product.name))].sort((first, second) => first.localeCompare(second)), [products, category]);
  const selectedPaymentMethodLabel = paymentMethods.find((method) => method.value === selectedPaymentMethod)?.label ?? "UPI";

  async function addToCart(product) {
    setUpdatingProduct(product.id);
    setError("");
    try {
      const cartData = await apiRequest("/cart", {
        method: "POST",
        body: JSON.stringify({ productId: product.id }),
      });
      setCart(cartData);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setUpdatingProduct(null);
    }
  }

  async function publishListing(event) {
    event.preventDefault();
    setError("");
    setIsPublishing(true);
    try {
      const isEditing = editingProductId !== null;
      const savedProduct = await apiRequest(isEditing ? `/products/${encodeURIComponent(editingProductId)}` : "/products", {
        method: isEditing ? "PUT" : "POST",
        body: JSON.stringify({
          ...listing,
          price: Number(listing.price),
          stock: Number(listing.stock),
          status: "Available",
          farmer: currentUser.name,
        }),
      });
      const color = getProductColor(savedProduct);
      const updatedProduct = { ...savedProduct, color, image: getProductImage(savedProduct) };
      setProducts((current) => isEditing
        ? current.map((product) => product.id === editingProductId ? updatedProduct : product)
        : [updatedProduct, ...current]);
      setListing({ productName: "", category: "", description: "", price: "", stock: "", unit: "kg", imageUrl: "", location: "" });
      setEditingProductId(null);
      setCategory("All produce");
      setSelectedItem("All items");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setIsPublishing(false);
    }
  }

  function startEditingListing(product) {
    setEditingProductId(product.id);
    setListing({
      productName: product.productName ?? product.name ?? "",
      category: product.category ?? "Vegetables",
      description: product.description ?? "",
      price: String(product.price ?? ""),
      stock: String(product.stock ?? ""),
      unit: product.unit ?? "kg",
      imageUrl: product.imageUrl ?? "",
      location: product.location ?? "",
    });
    setError("");
    document.getElementById("farmer-listing-title")?.scrollIntoView({ behavior: "smooth", block: "center" });
  }

  function cancelListingEdit() {
    setEditingProductId(null);
    setListing({ productName: "", category: "", description: "", price: "", stock: "", unit: "kg", imageUrl: "", location: "" });
    setError("");
  }

  async function deleteListing(product) {
    if (!window.confirm(`${t("delete")} "${product.name}"?`)) return;
    setError("");
    setDeletingProductId(product.id);
    try {
      await apiRequest(`/products/${encodeURIComponent(product.id)}`, { method: "DELETE" });
      setProducts((current) => current.filter((item) => item.id !== product.id));
      if (editingProductId === product.id) cancelListingEdit();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setDeletingProductId(null);
    }
  }

  function useCurrentLocation() {
    setError("");
    if (!window.isSecureContext) {
      setError("Current location requires a secure connection. Open the app at http://localhost:5173 or use HTTPS, then allow location access in your browser.");
      return;
    }
    if (!navigator.geolocation) {
      setError("Location is not available in this browser.");
      return;
    }

    setIsLocating(true);
    const saveCoordinates = ({ coords }) => {
      if (!Number.isFinite(coords.latitude) || !Number.isFinite(coords.longitude)) {
        setError("Your browser returned an invalid location. Enter your town or village manually.");
        setIsLocating(false);
        return;
      }
      setListing((current) => ({ ...current, location: `${coords.latitude.toFixed(6)}, ${coords.longitude.toFixed(6)}` }));
      setIsLocating(false);
    };
    const showLocationError = (locationError) => {
      const message = locationError.code === 1
        ? "Location permission is blocked. Allow location access for this site in your browser settings, then try again."
        : locationError.code === 3
          ? "Getting your location timed out. Check your device location and network settings, then try again."
          : "Your location could not be found. Check your device location and network settings, then try again.";
      setError(message);
      setIsLocating(false);
    };
    try {
      navigator.geolocation.getCurrentPosition(saveCoordinates, showLocationError, {
        enableHighAccuracy: false,
        timeout: 20000,
        maximumAge: 60000,
      });
    } catch {
      setError("Your browser could not start location access. Check site permissions or enter your town or village manually.");
      setIsLocating(false);
    }
  }

  async function handlePhotoSelection(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    setError("");
    if (!file.type.startsWith("image/")) {
      setError("Choose an image file for the product photo.");
      event.target.value = "";
      return;
    }
    try {
      const imageUrl = await imageFileToDataUrl(file);
      setListing((current) => ({ ...current, imageUrl }));
    } catch {
      setError("Could not load that photo. Try another image.");
    }
    event.target.value = "";
  }

  async function setCartItemQuantity(item, quantity) {
    setUpdatingCartItem(item.productId);
    setError("");
    try {
      const cartData = quantity < 1
        ? await apiRequest(`/cart/${encodeURIComponent(item.productId)}`, { method: "DELETE" })
        : await apiRequest(`/cart/${encodeURIComponent(item.productId)}`, {
          method: "PUT",
          body: JSON.stringify({ quantity }),
        });
      setCart(cartData);
      if (quantity < 1) setCheckoutMessage("");
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setUpdatingCartItem(null);
    }
  }

  async function handleCheckout() {
    if (!cart.items.length) {
      setError("Your cart is empty.");
      return;
    }
    const isCashOnDelivery = selectedPaymentMethod === "CASH_ON_DELIVERY";
    if (isCashOnDelivery && !deliveryAddress.trim()) {
      setError("Enter a delivery address to place a Cash on Delivery order.");
      return;
    }

    setIsCheckingOut(true);
    setError("");
    setCheckoutMessage("");
    try {
      const order = await apiRequest("/cart/checkout", {
        method: "POST",
        body: JSON.stringify({
          paymentMethod: selectedPaymentMethod,
          ...(isCashOnDelivery && { deliveryAddress: deliveryAddress.trim() }),
        }),
      });
      if (order.status === "COD_PENDING") {
        setCart({ count: 0, items: [], total: 0 });
        setOrderHistory(await apiRequest("/cart/orders", { cache: "no-store" }));
        setDeliveryAddress("");
        setCheckoutMessage(order.message);
        setIsCheckingOut(false);
        return;
      }
      await loadRazorpayCheckout();
      const checkout = new window.Razorpay({
        key: order.keyId,
        amount: order.amount,
        currency: order.currency,
        name: "FarmConnect",
        description: "Fresh produce from local farmers",
        order_id: order.orderId,
        prefill: { email: currentUser.email },
        method: {
          upi: selectedPaymentMethod === "UPI",
          card: selectedPaymentMethod === "CARD",
          netbanking: selectedPaymentMethod === "NET_BANKING",
        },
        theme: { color: "#315b34" },
        handler: async (paymentResponse) => {
          try {
            const verification = await apiRequest("/cart/checkout/verify", {
              method: "POST",
              body: JSON.stringify(paymentResponse),
            });
            setCheckoutMessage(verification.message);
            if (verification.status === "PAID") {
              setCart({ count: 0, items: [], total: 0 });
              setOrderHistory(await apiRequest("/cart/orders", { cache: "no-store" }));
            }
          } catch (requestError) {
            setError(requestError.message);
          } finally {
            setIsCheckingOut(false);
          }
        },
        modal: { ondismiss: () => setIsCheckingOut(false) },
      });
      checkout.on("payment.failed", (failure) => {
        setError(failure.error?.description || "Payment failed. Your cart has not been cleared.");
        setIsCheckingOut(false);
      });
      checkout.open();
    } catch (requestError) {
      setError(requestError.message);
      setIsCheckingOut(false);
    }
  }

  async function savePayoutAccount(event) {
    event.preventDefault();
    setPayoutError("");
    setIsSavingPayout(true);
    try {
      const account = await apiRequest("/farmer/payout-account", {
        method: "POST",
        body: JSON.stringify(payoutForm),
      });
      setPayoutAccount({ loading: false, ...account });
      setPayoutForm((current) => ({ ...current, pan: "", accountNumber: "", ifsc: "", termsAccepted: false }));
    } catch (requestError) {
      setPayoutError(requestError.message);
    } finally {
      setIsSavingPayout(false);
    }
  }

  return (
    <div className="shop-page">
      <header className="shop-header">
        <a className="brand" href="#market"><span className="brand-mark"><FarmConnectIcon /></span><span className="brand-name">Farm<span className="brand-accent">Connect</span></span></a>
        <nav>
          <a className="shop-link-active" href="#market">{t("marketplace")}</a>
          <a href="#market-products">{t("produce")}</a>
          {!isFarmer && <a href="#market-ai-cook">AI Chef & Recipes</a>}
        </nav>
        <div className="shop-tools">
          <label className="shop-language"><span>{t("language")}</span><select aria-label={t("language")} value={language} disabled={preferencesUserId !== currentUser.userId} onChange={changeLanguage}>{indianLanguages.map((option) => <option key={option.code} value={option.code}>{option.name}</option>)}</select></label>
          <label className="shop-search"><span aria-hidden="true">&#8981;</span><input type="search" aria-label={t("search")} placeholder={t("search")} value={search} onChange={(event) => setSearch(event.target.value)} /></label>
          {!isFarmer && <button className="shop-cart" type="button" onClick={toggleCart} aria-expanded={cartOpen} aria-label={`Cart, ${cart.count} ${cart.count === 1 ? "item" : "items"}`}>
            <svg className="shop-cart-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M3 4h2l2.2 10.1a2 2 0 0 0 2 1.6h8.9a2 2 0 0 0 1.9-1.4L22 8H6" /><circle cx="10" cy="20" r="1.2" /><circle cx="18" cy="20" r="1.2" /></svg>
            <span className="shop-cart-label">{t("cart")}</span><span className="shop-cart-count">{cart.count}</span>
          </button>}
          <button className="shop-home" onClick={onLogout}>{t("home")}</button>
          <button className="shop-logout" onClick={onLogout}>{t("logout")}</button>
          {/* â”€â”€ Notification Bell â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
          <div className="notif-bell-wrapper" style={{ position: "relative" }}>
            <button
              className="notif-bell-btn"
              type="button"
              aria-label={`Notifications${unreadCount > 0 ? `, ${unreadCount} unread` : ""}`}
              onClick={() => { setNotifOpen((o) => !o); setCartOpen(false); fetchNotifications(); }}
              style={{ position: "relative", background: "none", border: "none", cursor: "pointer", padding: "6px 8px", color: "#fff", fontSize: "20px", lineHeight: 1 }}
            >
              ðŸ””
              {unreadCount > 0 && (
                <span style={{ position: "absolute", top: 0, right: 0, background: "#ef4444", color: "#fff", borderRadius: "50%", fontSize: "10px", minWidth: "17px", height: "17px", display: "flex", alignItems: "center", justifyContent: "center", fontWeight: 700, padding: "0 3px" }}>
                  {unreadCount > 9 ? "9+" : unreadCount}
                </span>
              )}
            </button>
            {notifOpen && (
              <div style={{ position: "absolute", top: "calc(100% + 8px)", right: 0, width: "340px", background: "#fff", borderRadius: "12px", boxShadow: "0 8px 32px rgba(0,0,0,0.18)", zIndex: 9999, overflow: "hidden", border: "1px solid #e5e7eb" }}>
                <div style={{ padding: "14px 16px", background: "#1e3a1f", color: "#fff", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <strong style={{ fontSize: "14px" }}>ðŸ”” Notifications</strong>
                  <button onClick={() => setNotifOpen(false)} style={{ background: "none", border: "none", color: "#fff", cursor: "pointer", fontSize: "16px", lineHeight: 1 }}>âœ•</button>
                </div>
                <div style={{ maxHeight: "380px", overflowY: "auto" }}>
                  {notifLoading ? (
                    <p style={{ padding: "20px", textAlign: "center", color: "#6b7280", fontSize: "13px" }}>Loadingâ€¦</p>
                  ) : notifications.length === 0 ? (
                    <p style={{ padding: "20px", textAlign: "center", color: "#6b7280", fontSize: "13px" }}>No notifications yet</p>
                  ) : notifications.map((notif) => (
                    <div
                      key={notif.id}
                      onClick={() => !notif.read && markNotificationRead(notif.id)}
                      style={{ padding: "12px 16px", borderBottom: "1px solid #f3f4f6", background: notif.read ? "#fff" : "#f0fdf4", cursor: notif.read ? "default" : "pointer", transition: "background 0.2s" }}
                    >
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "8px" }}>
                        <span style={{ fontSize: "12px", fontWeight: notif.read ? 400 : 700, color: "#1f2937", flex: 1 }}>
                          {notif.type === "ORDER_CONFIRMATION" ? "ðŸ›’" : notif.type === "FARMER_NEW_ORDER" ? "ðŸŒ¾" : notif.type === "WELCOME_SIGNUP" ? "ðŸ‘‹" : "ðŸ””"} {notif.subject}
                        </span>
                        {!notif.read && <span style={{ background: "#16a34a", borderRadius: "50%", width: "8px", height: "8px", flexShrink: 0, marginTop: "4px" }} />}
                      </div>
                      <p style={{ margin: "4px 0 0", fontSize: "11px", color: "#6b7280" }}>
                        {new Date(notif.createdAt).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" })}
                        {" &#183; "}<span style={{ color: notif.status === "SENT" ? "#16a34a" : notif.status === "LOGGED" ? "#d97706" : "#ef4444", fontWeight: 600 }}>{notif.status === "SENT" ? "âœ‰ Emailed" : notif.status === "LOGGED" ? "ðŸ“‹ Saved" : "âš  Failed"}</span>
                      </p>
                    </div>
                  ))}
                </div>
                <div style={{ padding: "10px 16px", borderTop: "1px solid #f3f4f6", background: "#f9fafb", textAlign: "center" }}>
                  <small style={{ color: "#6b7280", fontSize: "11px" }}>Click unread notifications to mark as read</small>
                </div>
              </div>
            )}
          </div>
          {!isFarmer && cartOpen && <div className="cart-popover"><h3>Your cart</h3>{cart.items.length ? <><ul className="cart-items">{cart.items.map((item) => <li className="cart-item" key={item.productId}>
            <img className="cart-item-image" src={getProductImage({ name: item.name, imageUrl: item.imageUrl })} alt={item.name} />
            <div className="cart-item-content"><div className="cart-item-heading"><span>{item.name}</span><strong>&#8377;{item.unitPrice} / {item.unit}</strong></div>
            <div className="cart-item-actions">
              <div className="cart-quantity-control" aria-label={`Quantity for ${item.name}`}>
                <button type="button" onClick={() => setCartItemQuantity(item, item.quantity - 1)} disabled={updatingCartItem === item.productId} aria-label={`Remove one ${item.name}`}>&#8722;</button>
                <span>{item.quantity}</span>
                <button type="button" onClick={() => setCartItemQuantity(item, item.quantity + 1)} disabled={updatingCartItem === item.productId || (item.stock != null && item.quantity >= item.stock)} aria-label={`Add one ${item.name}`}>+</button>
              </div>
              <button type="button" className="cart-remove-button" onClick={() => setCartItemQuantity(item, 0)} disabled={updatingCartItem === item.productId}>Remove</button>
            </div></div>
          </li>)}</ul><div className="cart-total"><span>Subtotal</span><strong>&#8377;{Number(cart.total || 0).toFixed(2)}</strong></div>{cart.items.length > 0 && <div className="checkout-panel">
            <div className="payment-methods" role="radiogroup" aria-label="Payment method">
              {paymentMethods.map((method) => <label key={method.value} className={selectedPaymentMethod === method.value ? "payment-option selected" : "payment-option"}>
                <input type="radio" name="paymentMethod" value={method.value} checked={selectedPaymentMethod === method.value} onChange={(event) => setSelectedPaymentMethod(event.target.value)} />
                <span>{method.label}</span>
              </label>)}
            </div>
            {selectedPaymentMethod === "CASH_ON_DELIVERY" && <label className="delivery-address-field">
              Delivery address
              <textarea required maxLength={500} rows={3} value={deliveryAddress} onChange={(event) => setDeliveryAddress(event.target.value)} placeholder="House/flat, street, town or village, PIN code" />
            </label>}
            <button type="button" className="checkout-button" onClick={handleCheckout} disabled={isCheckingOut}>{isCheckingOut ? "Processing..." : selectedPaymentMethod === "CASH_ON_DELIVERY" ? "Place COD order" : `Pay with ${selectedPaymentMethodLabel}`}</button>
            {selectedPaymentMethod !== "CASH_ON_DELIVERY" && <p className="payment-provider-note">Card and net banking availability depends on your Razorpay account settings.</p>}
            {checkoutMessage && <p className="order-success">{checkoutMessage}</p>}
          </div>}</> : <p>Your cart is empty.</p>}{orderHistory.length > 0 && <section className="order-history"><h4>Recent orders</h4>{orderHistory.map((order) => <div className="order-history-row" key={order.id}><span>{new Date(order.createdAt).toLocaleDateString("en-IN")} · {String(order.status).replaceAll("_", " ")}</span><strong>&#8377;{Number(order.amountRupees).toFixed(2)}</strong></div>)}</section>}</div>}
        </div>
      </header>
      <main id="market">
        {isFarmer && <section className="farmer-listing-panel" aria-labelledby="farmer-listing-title">
          <div className="farmer-listing-intro"><span className="eyebrow">{t("farmerDashboard")}</span><h2 id="farmer-listing-title">{editingProductId ? t("edit") : t("shareHarvest")}</h2><p>{editingProductId ? "Update your details or add a photo, then save your changes." : "Add your available harvest to the marketplace. No photo yet? Publish now and add one later by editing your listing."}</p></div>
          <section className="payout-setup" aria-labelledby="payout-setup-title">
            <div className="payout-setup-heading"><h3 id="payout-setup-title">Farmer payouts</h3><p>Verified payouts are sent to your bank through Razorpay after a buyer's online payment is captured.</p></div>
            {payoutAccount.loading ? <p className="payout-status" role="status">Checking payout setupâ€¦</p> : payoutAccount.configured ? <p className="payout-status" role="status">
              {payoutAccount.status === "VERIFIED" ? "Bank account verified" : `Bank verification: ${String(payoutAccount.status).toLowerCase().replaceAll("_", " ")}`}
              {payoutAccount.accountEnding && <span> &#183; Account ending {payoutAccount.accountEnding}</span>}
            </p> : <form className="payout-form" onSubmit={savePayoutAccount}>
              <label>Legal name<input required minLength="4" maxLength="200" autoComplete="name" value={payoutForm.legalBusinessName} onChange={(event) => setPayoutForm({ ...payoutForm, legalBusinessName: event.target.value })} /></label>
              <label>Phone number<input required type="tel" autoComplete="tel" maxLength="16" placeholder="+91 9876543210" value={payoutForm.phone} onChange={(event) => setPayoutForm({ ...payoutForm, phone: event.target.value })} /></label>
              <label>PAN<input required autoComplete="off" maxLength="10" pattern="[A-Za-z]{5}[0-9]{4}[A-Za-z]" value={payoutForm.pan} onChange={(event) => setPayoutForm({ ...payoutForm, pan: event.target.value.toUpperCase() })} /></label>
              <label>Bank account holder<input required maxLength="100" autoComplete="off" value={payoutForm.beneficiaryName} onChange={(event) => setPayoutForm({ ...payoutForm, beneficiaryName: event.target.value })} /></label>
              <label>Bank account number<input required inputMode="numeric" autoComplete="off" minLength="5" maxLength="20" pattern="[0-9]{5,20}" value={payoutForm.accountNumber} onChange={(event) => setPayoutForm({ ...payoutForm, accountNumber: event.target.value })} /></label>
              <label>IFSC code<input required autoComplete="off" maxLength="11" pattern="[A-Za-z]{4}0[A-Za-z0-9]{6}" value={payoutForm.ifsc} onChange={(event) => setPayoutForm({ ...payoutForm, ifsc: event.target.value.toUpperCase() })} /></label>
              <label className="payout-consent"><input required type="checkbox" checked={payoutForm.termsAccepted} onChange={(event) => setPayoutForm({ ...payoutForm, termsAccepted: event.target.checked })} /><span>I confirm these details are accurate and consent to Razorpay verifying this account for payouts.</span></label>
              <button type="submit" className="payout-submit" disabled={isSavingPayout}>{isSavingPayout ? "Submittingâ€¦" : "Set up bank payouts"}</button>
            </form>}
            {payoutError && <p className="form-error payout-error" role="alert">{payoutError}</p>}
          </section>
          <form className="farmer-listing-form" onSubmit={publishListing}>
            <label>{t("itemName")}<input required maxLength="80" placeholder="e.g. Fresh mangoes" value={listing.productName} onChange={(event) => setListing({ ...listing, productName: event.target.value })} /></label>
            <label>{t("category")}<select required value={listing.category} onChange={(event) => setListing({ ...listing, category: event.target.value })}><option value="" disabled>Select a category</option><option>Vegetables</option><option>Fruits</option><option>Grains</option><option>Nuts</option><option>Pulses</option><option>Spices</option><option>Herbs</option><option>Dairy</option></select></label>
            <label>{t("price")} (â‚¹)<input required type="number" min="0.01" step="0.01" value={listing.price} onChange={(event) => setListing({ ...listing, price: event.target.value })} /></label>
            <label>{t("quantity")}<input required type="number" min="1" step="1" value={listing.stock} onChange={(event) => setListing({ ...listing, stock: event.target.value })} /></label>
            <label>{t("unit")}<select value={listing.unit} onChange={(event) => setListing({ ...listing, unit: event.target.value })}><option>kg</option><option>grams</option><option>tons</option><option>piece</option><option>dozen</option><option>bunch</option><option>bag</option></select></label>
            <label>{t("location")}<input required maxLength="100" placeholder="Town or village" value={listing.location} onChange={(event) => setListing({ ...listing, location: event.target.value })} /><button className="use-current-location" type="button" onClick={useCurrentLocation} disabled={isLocating}>{isLocating ? "Getting locationâ€¦" : "Use current location"}</button></label>
            <div className="farmer-listing-image-field farmer-photo-field"><span className="farmer-photo-label">{t("photo")} <span>(optional)</span></span><div className="farmer-photo-actions"><label className="photo-picker">Upload pics<input type="file" accept="image/*" onChange={handlePhotoSelection} /></label></div><input aria-label="Or enter a photo URL" type="url" placeholder="Or paste a photo URL" value={listing.imageUrl.startsWith("data:") ? "" : listing.imageUrl} onChange={(event) => setListing({ ...listing, imageUrl: event.target.value })} />{listing.imageUrl.startsWith("data:") && <img className="farmer-photo-preview" src={listing.imageUrl} alt="Selected product preview" />}</div>
            <label className="farmer-listing-description">{t("description")} <span>(optional)</span><input maxLength="300" placeholder="Freshly harvested today" value={listing.description} onChange={(event) => setListing({ ...listing, description: event.target.value })} /></label>
            <button type="submit" className="add-to-cart farmer-publish-button" disabled={isPublishing}>{isPublishing ? (editingProductId ? "Savingâ€¦" : "Publishingâ€¦") : (editingProductId ? t("save") : t("publish"))}</button>
            {editingProductId && <button type="button" className="farmer-cancel-edit" onClick={cancelListingEdit} disabled={isPublishing}>{t("cancel")}</button>}
          </form>
          {error && <p className="form-error market-error" role="alert">{error}</p>}
        </section>}
        <section className="market-hero"><div><span className="eyebrow"><span className="eyebrow-dot" /> THE FARMERS&#8217; MARKET</span><h1>Fresh from the<br /><em>people who grow it.</em></h1><p>Good things are growing nearby. Find your next favorite from local farms.</p></div><div className="market-hero-note"><span>&#10033;</span><p><strong>Harvested with care.</strong><br />Shared with your community.</p></div></section>
        {!isFarmer && (
          <div id="market-ai-cook">
            <AiCookAssistant
              onCartUpdated={(nextCart) => setCart(nextCart)}
              onOpenCart={() => setCartOpen(true)}
              getProductImage={getProductImage}
            />
          </div>
        )}
        <section className="market-content" id="market-products">
          <div className="market-title-row"><div><span className="eyebrow">GOOD FOOD, GROWN CLOSER</span><h2>Shop the harvest</h2></div><span className="results-count">{visibleProducts.length} fresh picks</span></div>
          <div className="category-list" aria-label="Filter by category">{categories.map((item) => <button type="button" key={item} className={category === item ? "category-chip selected" : "category-chip"} onClick={() => { setCategory(item); setSelectedItem("All items"); }}>{item}</button>)}</div>
          {category !== "All produce" && availableItems.length > 0 && <div className="item-filter-section">
            <h3 className="item-filter-heading">{category} items</h3>
            <div className="category-list item-list" aria-label={`Filter ${category.toLowerCase()} by item`}>
              {["All items", ...availableItems].map((item) => <button type="button" key={item} className={selectedItem === item ? "category-chip selected" : "category-chip"} onClick={() => setSelectedItem(item)}>{item}</button>)}
            </div>
          </div>}
          {error && <p className="form-error market-error" role="alert">{error}</p>}
        {isLoading ? <p className="empty-results" role="status">Loading produce from the farm database&#8230;</p> : visibleProducts.length ? <div className="product-grid">{visibleProducts.map((product) => <article className="product-card" key={product.id}><div className={`product-image product-${product.color}`}><img src={product.image} alt={product.name} /><span className="product-origin">LOCAL HARVEST</span></div><div className="product-details"><div className="product-meta"><span>{product.category}</span><span>&#183;</span><a href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(product.location)}`} target="_blank" rel="noreferrer" aria-label={`View ${product.location} on map`}>{product.location} &#183; View on map</a></div><h3>{product.name}</h3><p className="farmer-name">Grown by <strong>{product.farmer}</strong></p><div className="product-buy"><strong>&#8377;{product.price}<small> / {product.unit}</small></strong>{isFarmer ? <>{(product.ownedByCurrentUser || product.farmer === currentUser.name) ? <div className="farmer-listing-actions"><button type="button" className="farmer-edit-listing" onClick={() => startEditingListing(product)}>{t("edit")}</button><button type="button" className="farmer-delete-listing" onClick={() => deleteListing(product)} disabled={deletingProductId === product.id}>{deletingProductId === product.id ? "â€¦" : t("delete")}</button></div> : <span className="product-stock">{product.stock} {product.unit} available</span>}</> : <button type="button" className="add-to-cart" disabled={updatingProduct === product.id || product.stock < 1} onClick={() => addToCart(product)} aria-label={`${t("addToCart")}: ${product.name}`}>{product.stock < 1 ? t("soldOut") : updatingProduct === product.id ? "â€¦" : t("addToCart")}</button>}</div></div></article>)}</div> : !error && <p className="empty-results" role="status">No harvest matches that search. Try another name or category.</p>}
        </section>
      </main>
      <footer className="shop-footer"><span>FarmConnect &#183; Good food, grown closer.</span><button onClick={onLogout}>Back to home &#8593;</button></footer>
      {/* â”€â”€ AI Chat Widget (floating bottom-right) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ */}
      <AiChatWidget />
    </div>
  );
}

export default Marketplace;
