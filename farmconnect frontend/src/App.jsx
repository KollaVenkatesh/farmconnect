import { useEffect, useState } from "react";
import { clearAuthSession, getAuthSession } from "./api.js";
import Home from "./components/Home";
import Login from "./components/Login";
import Register from "./components/Register";
import Marketplace from "./components/Marketplace";
import AiChatWidget from "./components/AiChatWidget";

import "./App.css";

function App() {
  const [session, setSession] = useState(() => getAuthSession());
  const [page, setPage] = useState(() => getAuthSession()?.user ? "marketplace" : "home");
  const [currentUser, setCurrentUser] = useState(() => getAuthSession()?.user ?? null);

  useEffect(() => {
    function handleUnauthorized() {
      clearAuthSession();
      setSession(null);
      setCurrentUser(null);
      setPage("login");
    }
    window.addEventListener("farmconnect:unauthorized", handleUnauthorized);
    return () => window.removeEventListener("farmconnect:unauthorized", handleUnauthorized);
  }, []);

  return (
    <div className="app">
      {page === "home" && <Home onSignIn={() => setPage("login")} onSignUp={() => setPage("register")} />}
      {page === "login" && <Login onBack={() => setPage("home")} onSignUp={() => setPage("register")} onLoginSuccess={(user) => { setCurrentUser(user); setSession(getAuthSession()); setPage("marketplace"); }} />}
      {page === "register" && <Register onBack={() => setPage("home")} onSignIn={() => setPage("login")} />}
      {page === "marketplace" && currentUser && session && <Marketplace currentUser={currentUser} onLogout={() => { clearAuthSession(); setSession(null); setCurrentUser(null); setPage("home"); }} />}
      <AiChatWidget />
    </div>
  );
}

export default App;
