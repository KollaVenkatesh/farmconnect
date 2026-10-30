import { useState } from "react";
import { apiRequest, saveAuthSession } from "../api.js";
import FarmConnectIcon from "./FarmConnectIcon";

function Login({ onBack, onSignUp, onLoginSuccess }) {
  const [loginIdentifier, setLoginIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleLogin(event) {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);
    try {
      const rawIdentifier = loginIdentifier.trim();
      const normalizedIdentifier = rawIdentifier.toLowerCase();
      const isEmail = normalizedIdentifier.includes("@");
      const result = await apiRequest("/auth/login", {
        method: "POST",
        body: JSON.stringify({
          email: isEmail ? rawIdentifier : null,
          userId: isEmail ? null : rawIdentifier,
          password,
        }),
      });
      saveAuthSession({ token: result.token, user: result.user });
      onLoginSuccess(result.user);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="login-page login-page-editorial">
      <header className="login-topbar">
        <div className="login-top-brand"><FarmConnectIcon className="form-brand-icon" /><span>FarmConnect</span></div>
        <button className="login-back" onClick={onBack} type="button">Back to home</button>
      </header>
      <section className="login-stage">
        <div className="login-editorial">
          <span className="login-welcome">FRESH FROM YOUR LOCAL COMMUNITY</span>
          <h1>Welcome</h1>
          <p className="login-editorial-copy">Your farmers, your favourite seasonal produce, and a better way to shop local are waiting.</p>
          <p className="login-editorial-note">Discover seasonal produce and support the farmers who grow it.</p>
          <div className="login-art">
            <img className="login-art-image" src="https://images.unsplash.com/photo-1734261780213-765e29537e1f?auto=format&amp;fit=crop&amp;w=1200&amp;q=80" alt="Farmers harvesting leafy greens together in a field" />
            <span className="login-art-label">GROWN CLOSE. SHARED WITH CARE.</span>
          </div>
        </div>
        <div className="login-form-panel">
          <div className="login-form-heading">
            <span className="login-form-kicker">YOUR ACCOUNT</span>
            <h2>Sign in</h2>
            <p>Pick up where your local shopping left off.</p>
          </div>
          <form onSubmit={handleLogin}>
            <label htmlFor="login-user-id">Email or User ID</label>
            <input id="login-user-id" type="text" autoComplete="username" placeholder="Enter your email or user ID" value={loginIdentifier} onChange={(event) => setLoginIdentifier(event.target.value)} required />
            <label htmlFor="login-password">Password</label>
            <input id="login-password" type="password" autoComplete="current-password" placeholder="Enter your password" value={password} onChange={(event) => setPassword(event.target.value)} required />
            {error && <p className="form-error" role="alert">{error}</p>}
            <button type="submit" className="login-submit" disabled={isSubmitting}>{isSubmitting ? "Signing in..." : "Sign In"}</button>
          </form>
          <div className="login-form-divider"><span>NEW TO FARMCONNECT</span></div>
          <button className="login-create" onClick={onSignUp} type="button">Create Account</button>
          <p className="login-form-footnote">A little closer to your local harvest.</p>
        </div>
      </section>
    </main>
  );
}

export default Login;
