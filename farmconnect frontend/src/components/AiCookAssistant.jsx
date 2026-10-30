import { useState, useEffect } from "react";
import { apiRequest } from "../api.js";

const fallbackSuggestions = [
  { id: "sambar", name: "South Indian Sambar", icon: "🍲", tagline: "Farm vegetables simmered in aromatic dal" },
  { id: "biryani", name: "Vegetable Biryani", icon: "🍛", tagline: "Fragrant rice layered with fresh garden produce" },
  { id: "rasam", name: "Tomato Rasam / Soup", icon: "🍅", tagline: "Tangy soothing broth with pepper and chillies" },
  { id: "aloo-gobi", name: "Aloo Gobi Masala", icon: "🥔", tagline: "Tender potatoes and fresh cauliflower florets" },
  { id: "salad", name: "Fresh Garden Salad", icon: "🥗", tagline: "Crisp cucumbers, tomatoes, carrots and onions" },
  { id: "dal-palak", name: "Garlic Spinach Dal", icon: "🥬", tagline: "Iron-rich spinach tempered with garlic and cumin" },
  { id: "fruit-bowl", name: "Seasonal Fruit Platter", icon: "🍉", tagline: "Alphonso mangoes, papaya, watermelon and pomegranate" },
  { id: "millet-khichdi", name: "Millet Khichdi", icon: "🌾", tagline: "High-fiber foxtail millet cooked with diced vegetables" }
];

export default function AiCookAssistant({ onCartUpdated, onOpenCart, getProductImage }) {
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState(fallbackSuggestions);
  const [recipe, setRecipe] = useState(null);
  const [selectedItems, setSelectedItems] = useState({});
  const [isLoading, setIsLoading] = useState(false);
  const [isAddingToCart, setIsAddingToCart] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    let cancelled = false;
    apiRequest("/recipes/suggestions")
      .then((data) => {
        if (!cancelled && Array.isArray(data) && data.length > 0) {
          setSuggestions(data);
        }
      })
      .catch(() => {
        // Fallback already set
      });
    return () => { cancelled = true; };
  }, []);

  async function handleFindRecipe(dishName) {
    const targetDish = dishName || query.trim();
    if (!targetDish) return;
    setIsLoading(true);
    setErrorMessage("");
    setSuccessMessage("");

    try {
      const data = await apiRequest("/recipes/match", {
        method: "POST",
        body: JSON.stringify({ dish: targetDish }),
      });
      setRecipe(data);
      // Automatically select all matched farm products by default!
      const initialSelection = {};
      if (Array.isArray(data.matchedProducts)) {
        data.matchedProducts.forEach((item) => {
          initialSelection[item.productId] = true;
        });
      }
      setSelectedItems(initialSelection);
    } catch (err) {
      setErrorMessage(err.message || "Could not match recipe ingredients. Please try again.");
    } finally {
      setIsLoading(false);
    }
  }

  function toggleItemSelection(productId) {
    setSelectedItems((prev) => ({
      ...prev,
      [productId]: !prev[productId],
    }));
  }

  const activeMatchedItems = recipe?.matchedProducts?.filter((item) => selectedItems[item.productId]) || [];
  const selectedTotal = activeMatchedItems.reduce((acc, item) => acc + (item.price || 0) * (item.suggestedQuantity || 1), 0);

  async function handleAddAllToCart() {
    if (activeMatchedItems.length === 0) return;
    setIsAddingToCart(true);
    setErrorMessage("");
    setSuccessMessage("");

    try {
      const payload = {
        items: activeMatchedItems.map((item) => ({
          productId: item.productId,
          quantity: item.suggestedQuantity || 1,
        })),
      };

      const cartData = await apiRequest("/cart/batch", {
        method: "POST",
        body: JSON.stringify(payload),
      });

      if (onCartUpdated) onCartUpdated(cartData);
      setSuccessMessage(`✓ Added ${activeMatchedItems.length} fresh ingredients to your cart!`);

      if (onOpenCart) {
        setTimeout(() => onOpenCart(), 600);
      }
    } catch (err) {
      setErrorMessage(err.message || "Failed to add items to cart.");
    } finally {
      setIsAddingToCart(false);
    }
  }

  return (
    <section className="ai-cook-assistant" aria-labelledby="ai-cook-heading">
      <div className="ai-cook-header">
        <div className="ai-cook-badge">
          <span className="ai-sparkle">✨</span>
          <span>AI RECIPE TO CART</span>
        </div>
        <h2 id="ai-cook-heading">What would you like to cook?</h2>
        <p className="ai-cook-tagline">
          Tell us any recipe or meal idea. FarmConnect AI will automatically pick all the fresh farm ingredients available in the store and add them straight into your cart!
        </p>

        <form
          className="ai-cook-search-form"
          onSubmit={(e) => {
            e.preventDefault();
            handleFindRecipe();
          }}
        >
          <div className="ai-input-wrapper">
            <span className="ai-input-icon">🍳</span>
            <input
              type="text"
              className="ai-cook-input"
              placeholder="e.g. Vegetable Sambar, Veg Biryani, Tomato Soup, Aloo Gobi, Fruit Salad..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
            {query && (
              <button
                type="button"
                className="ai-clear-btn"
                onClick={() => setQuery("")}
                aria-label="Clear search"
              >
                ✕
              </button>
            )}
          </div>
          <button type="submit" className="ai-cook-submit-btn" disabled={isLoading || !query.trim()}>
            {isLoading ? "Matching Produce..." : "Auto-Select Items ✨"}
          </button>
        </form>

        <div className="ai-suggestions-bar">
          <span className="ai-suggestions-label">Popular ideas:</span>
          <div className="ai-chips-list">
            {suggestions.map((sug) => (
              <button
                key={sug.id || sug.name}
                type="button"
                className="ai-chip"
                onClick={() => {
                  setQuery(sug.name);
                  handleFindRecipe(sug.name);
                }}
              >
                <span>{sug.icon || "🥗"}</span>
                <span>{sug.name}</span>
              </button>
            ))}
          </div>
        </div>
      </div>

      {errorMessage && <p className="ai-error-banner" role="alert">{errorMessage}</p>}

      {recipe && (
        <div className="ai-recipe-result-card">
          <div className="ai-recipe-header">
            <div className="ai-recipe-title-group">
              <span className="ai-recipe-eyebrow">RECIPE & INGREDIENT MATCH</span>
              <h3>{recipe.dish}</h3>
              <p className="ai-recipe-desc">{recipe.description}</p>
            </div>
            <div className="ai-recipe-meta-badges">
              {recipe.prepTime && <span className="ai-meta-badge">⏱ Prep: {recipe.prepTime}</span>}
              {recipe.cookTime && <span className="ai-meta-badge">🔥 Cook: {recipe.cookTime}</span>}
              {recipe.servings && <span className="ai-meta-badge">👥 {recipe.servings}</span>}
              <button
                type="button"
                className="ai-close-recipe-btn"
                onClick={() => setRecipe(null)}
                title="Close recipe"
              >
                ✕
              </button>
            </div>
          </div>

          <div className="ai-recipe-content-grid">
            <div className="ai-ingredients-column">
              <div className="ai-section-title-row">
                <h4>
                  🌱 Fresh Farm Produce <span>({activeMatchedItems.length} selected of {recipe.matchedProducts?.length || 0})</span>
                </h4>
                <small>Uncheck if you already have it at home</small>
              </div>

              {recipe.matchedProducts?.length ? (
                <div className="ai-matched-products-list">
                  {recipe.matchedProducts.map((item) => {
                    const isSelected = !!selectedItems[item.productId];
                    const itemImage = getProductImage
                      ? getProductImage({ name: item.productName, imageUrl: item.imageUrl })
                      : item.imageUrl;

                    return (
                      <div
                        key={item.productId}
                        className={`ai-ingredient-row ${isSelected ? "selected" : "deselected"}`}
                        onClick={() => toggleItemSelection(item.productId)}
                      >
                        <input
                          type="checkbox"
                          className="ai-ingredient-checkbox"
                          checked={isSelected}
                          onChange={() => toggleItemSelection(item.productId)}
                          onClick={(e) => e.stopPropagation()}
                          aria-label={`Select ${item.productName}`}
                        />
                        {itemImage && (
                          <img
                            src={itemImage}
                            alt={item.productName}
                            className="ai-ingredient-thumb"
                          />
                        )}
                        <div className="ai-ingredient-details">
                          <strong>{item.productName}</strong>
                          <span className="ai-ingredient-role">{item.role}</span>
                          <span className="ai-ingredient-farmer">
                            Grown by {item.farmer} · {item.location}
                          </span>
                        </div>
                        <div className="ai-ingredient-price-block">
                          <span className="ai-ingredient-price">₹{item.price}</span>
                          <small className="ai-ingredient-unit">per {item.unit}</small>
                        </div>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <p className="ai-empty-notice">No direct farm items found for this query.</p>
              )}

              {recipe.pantryItems?.length > 0 && (
                <div className="ai-pantry-section">
                  <h5>🧂 Pantry Staples Needed:</h5>
                  <div className="ai-pantry-tags">
                    {recipe.pantryItems.map((pantryItem, idx) => (
                      <span key={idx} className="ai-pantry-tag">
                        {pantryItem}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="ai-instructions-column">
              <h4>👨‍🍳 Step-by-Step Cooking Guide</h4>
              <ol className="ai-instructions-list">
                {recipe.instructions?.map((step, idx) => (
                  <li key={idx} className="ai-instruction-step">
                    <span className="ai-step-number">{idx + 1}</span>
                    <p>{step}</p>
                  </li>
                ))}
              </ol>

              <div className="ai-autocart-action-box">
                <div className="ai-autocart-summary">
                  <span className="ai-summary-label">Estimated Total:</span>
                  <span className="ai-summary-amount">₹{selectedTotal.toFixed(2)}</span>
                </div>
                <button
                  type="button"
                  className="ai-add-all-cart-btn"
                  onClick={handleAddAllToCart}
                  disabled={isAddingToCart || activeMatchedItems.length === 0}
                >
                  {isAddingToCart ? (
                    "Adding to Cart..."
                  ) : (
                    <>
                      <span>🛒 Add Selected Ingredients ({activeMatchedItems.length} items)</span>
                      <strong>₹{selectedTotal.toFixed(2)}</strong>
                    </>
                  )}
                </button>
                {successMessage && <p className="ai-success-banner">{successMessage}</p>}
              </div>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

