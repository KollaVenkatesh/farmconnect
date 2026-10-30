import { useState } from "react";
import { apiRequest } from "../api.js";
import FarmConnectIcon from "./FarmConnectIcon";

function Register({ onBack, onSignIn }) {
  const [form, setForm] = useState({ fullName: "", userId: "", email: "", password: "", confirmPassword: "", role: "Buyer" });
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function updateField(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function handleRegister(event) {
    event.preventDefault();
    setError("");
    if (form.password !== form.confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setIsSubmitting(true);
    try {
      await apiRequest("/auth/register", {
        method: "POST",
        body: JSON.stringify({ fullName: form.fullName, userId: form.userId, email: form.email, password: form.password, role: form.role }),
      });
      onSignIn();
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="login-page login-page-editorial register-page-editorial">
      <header className="login-topbar">
        <div className="login-top-brand"><FarmConnectIcon className="form-brand-icon" /><span>FarmConnect</span></div>
        <button className="login-back" onClick={onBack} type="button">Back to home</button>
      </header>
      <section className="login-stage register-stage">
        <div className="login-editorial">
          <span className="login-welcome">FRESH FOOD, ROOTED NEARBY</span>
          <h1>Join us</h1>
          <p className="login-editorial-copy">Bring your farm or your kitchen closer to the people who make local food special.</p>
          <p className="login-editorial-note">Meet local growers, discover seasonal produce, and make every harvest count.</p>
          <div className="login-art">
            <img className="login-art-image" src="https://www.visitljubljana.com/assets/gallery/trznica-stane-jersic-avgust-2006-1__ScaleMaxWidthWzE5MDBd.jpg" alt="A customer buying fresh vegetables from a vendor at an outdoor farmers market" loading="eager" onError={(event) => { event.currentTarget.onerror = null; event.currentTarget.src = "/farmers-market-illustration.svg"; }} />
            <span className="login-art-label">GROWN CLOSE. SHARED WITH CARE.</span>
          </div>
        </div>
        <div className="login-form-panel register-form-panel">
          <div className="login-form-heading">
            <span className="login-form-kicker">LET’S GET YOU STARTED</span>
            <h2>Create account</h2>
            <p>Choose how you would like to take part in your local food community.</p>
          </div>
          <form className="signup-form" onSubmit={handleRegister}>
            <div className="signup-fields-pair">
              <div><label htmlFor="register-name">Full Name</label><input id="register-name" name="fullName" type="text" autoComplete="name" placeholder="Your name" value={form.fullName} onChange={updateField} required /></div>
              <div><label htmlFor="register-user-id">User ID</label><input id="register-user-id" name="userId" type="text" autoComplete="username" placeholder="Choose a user ID" minLength={3} maxLength={32} pattern="[a-zA-Z0-9][a-zA-Z0-9._-]*" value={form.userId} onChange={updateField} required /></div>
            </div>
            <div><label htmlFor="register-email">Email Address</label><input id="register-email" name="email" type="email" autoComplete="email" placeholder="you@example.com" value={form.email} onChange={updateField} required /></div>
            <div><label htmlFor="register-role">I am joining as</label><select id="register-role" name="role" value={form.role} onChange={updateField}><option value="Buyer">Buyer</option><option value="Farmer">Farmer</option></select></div>
            <div className="signup-fields-pair">
              <div><label htmlFor="register-password">Password</label><input id="register-password" name="password" type="password" autoComplete="new-password" placeholder="At least 10 characters" minLength={10} maxLength={128} value={form.password} onChange={updateField} required /></div>
              <div><label htmlFor="register-confirm-password">Confirm Password</label><input id="register-confirm-password" name="confirmPassword" type="password" autoComplete="new-password" placeholder="Re-enter your password" minLength={10} maxLength={128} value={form.confirmPassword} onChange={updateField} required /></div>
            </div>
            {error && <p className="form-error" role="alert">{error}</p>}
            <button type="submit" className="login-submit" disabled={isSubmitting}>{isSubmitting ? "Creating account..." : "Create Account"}</button>
          </form>
          <div className="login-form-divider"><span>ALREADY A MEMBER?</span></div>
          <button className="login-create register-signin" onClick={onSignIn} type="button">Sign In</button>
          <p className="login-form-footnote">A little closer to your local harvest.</p>
        </div>
      </section>
    </main>
  );
}

export default Register;
