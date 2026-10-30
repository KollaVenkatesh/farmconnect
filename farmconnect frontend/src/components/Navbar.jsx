
import { useState } from "react";

function Navbar({
  cartCount = 0,
  activePage = "Home",
  onNavigate = () => {}
}) {
  const [searchText, setSearchText] = useState("");

  function handleSearch(e) {
    e.preventDefault();

    onNavigate("Products", searchText);
  }

  return (
    <header className="navbar">
      <div className="nav-container">

        {/* LOGO */}
        <button
          className="brand"
          onClick={() => onNavigate("Home")}
        >
          <span className="brand-icon">🌾</span>
          <span>FarmConnect</span>
        </button>

        {/* SEARCH BAR */}
        <form className="search-form" onSubmit={handleSearch}>
          <span className="search-icon">⌕</span>

          <input
            type="text"
            placeholder="Search fresh products..."
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
          />

          <button type="submit" className="search-button">
            Search
          </button>
        </form>

        {/* NAVIGATION */}
        <nav className="nav-links">

          <button
            className={activePage === "Home" ? "active" : ""}
            onClick={() => onNavigate("Home")}
          >
            Home
          </button>

          <button
            className={activePage === "Products" ? "active" : ""}
            onClick={() => onNavigate("Products")}
          >
            Products
          </button>

          <button
            onClick={() => onNavigate("Login")}
          >
            Login
          </button>

          <button
            className="register-nav-btn"
            onClick={() => onNavigate("Register")}
          >
            Register
          </button>

          <button
            className="cart-button"
            onClick={() => onNavigate("Cart")}
          >
            🛒 Cart ({cartCount})
          </button>

        </nav>

      </div>
    </header>
  );
}

export default Navbar;