"use client";

import { useEffect, useState } from "react";
import HeroSection from "@/components/landing/HeroSection";
import FeaturesSection from "@/components/landing/FeaturesSection";
import MobileFlowSection from "@/components/landing/MobileFlowSection";
import ProblemSolutionSection from "@/components/landing/ProblemSolutionSection";
import AuthPage from "@/components/auth/AuthPage";
import DashboardView from "@/components/dashboard/DashboardView";

interface AuthUser {
  id: string;
  email: string;
  role?: string;
  first_name?: string;
  last_name?: string;
}

interface AuthResponse {
  token: string;
  user: AuthUser;
}

type EntryView = "landing" | "auth" | "dashboard";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:3001/api";
const TOKEN_KEY = "adapt_web_token";
const USER_KEY = "adapt_web_user";
const AUTH_REQUEST_TIMEOUT_MS = 12000;

const postAuthRequest = async (path: string, payload: Record<string, unknown>) => {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), AUTH_REQUEST_TIMEOUT_MS);

  try {
    const response = await fetch(`${API_BASE}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
      signal: controller.signal,
      cache: "no-store",
    });

    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error((data as { error?: string }).error || `Request failed (${response.status})`);
    }

    return data as AuthResponse;
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw new Error("Sign-in request timed out. Please verify backend API is live and try again.");
    }

    if (error instanceof TypeError) {
      throw new Error("Cannot reach backend API at http://localhost:3001. Start backend and try again.");
    }

    throw error;
  } finally {
    window.clearTimeout(timeoutId);
  }
};

const Home = () => {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [entryView, setEntryView] = useState<EntryView>("landing");
  const [authLoading, setAuthLoading] = useState(false);
  const [authError, setAuthError] = useState("");

  const isLoggedIn = Boolean(token && user);

  const persistSession = (payload: AuthResponse) => {
    setToken(payload.token);
    setUser(payload.user);
    setEntryView("dashboard");
    localStorage.setItem(TOKEN_KEY, payload.token);
    localStorage.setItem(USER_KEY, JSON.stringify(payload.user));
  };

  const clearSession = () => {
    setToken(null);
    setUser(null);
    setEntryView("landing");
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  };

  const openLoginPage = () => {
    setToken(null);
    setUser(null);
    setAuthError("");
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setEntryView("auth");
  };

  useEffect(() => {
    const savedToken = localStorage.getItem(TOKEN_KEY);
    const savedUser = localStorage.getItem(USER_KEY);

    if (!savedToken || !savedUser) {
      return;
    }

    try {
      setToken(savedToken);
      setUser(JSON.parse(savedUser));
      setEntryView("dashboard");
    } catch {
      clearSession();
    }
  }, []);

  const handleLogin = async (email: string, password: string) => {
    setAuthLoading(true);
    setAuthError("");
    try {
      const payload = await postAuthRequest("/auth/login", {
        email,
        password,
        platform: "WEB",
      });
      persistSession(payload);
    } catch (e) {
      setAuthError(e instanceof Error ? e.message : "Unable to sign in.");
    } finally {
      setAuthLoading(false);
    }
  };

  const handleRegister = async (
    email: string,
    password: string,
    firstName: string,
    lastName: string
  ) => {
    setAuthLoading(true);
    setAuthError("");
    try {
      const payload = await postAuthRequest("/auth/register", {
        email,
        password,
        first_name: firstName,
        last_name: lastName,
        platform: "WEB",
      });
      persistSession(payload);
    } catch (e) {
      setAuthError(e instanceof Error ? e.message : "Unable to create account.");
    } finally {
      setAuthLoading(false);
    }
  };

  if (entryView === "auth") {
    return (
      <AuthPage
        onBack={() => setEntryView("landing")}
        onLogin={handleLogin}
        onRegister={handleRegister}
        onDashboard={() => setEntryView("dashboard")}
        isLoggedIn={isLoggedIn}
        userEmail={user?.email}
        authLoading={authLoading}
        authError={authError}
      />
    );
  }

  if (entryView === "dashboard" && isLoggedIn) {
    return (
      <DashboardView
        token={token!}
        user={user!}
        apiBase={API_BASE}
        onLogout={clearSession}
      />
    );
  }

  return (
    <div className="min-h-screen">
      <HeroSection
        onLogin={openLoginPage}
        onOpenDashboard={() => setEntryView("dashboard")}
        isLoggedIn={isLoggedIn}
      />
      <FeaturesSection />
      <ProblemSolutionSection />
      <MobileFlowSection />
      <footer className="py-12 bg-primary text-primary-foreground">
        <div className="container max-w-7xl mx-auto px-6 text-center space-y-2">
          <p className="font-heading font-bold text-lg">ADAPT Care Platform</p>
          <p className="text-sm text-primary-foreground/70">
            Cognitive assistance for caregiver workflows
          </p>
        </div>
      </footer>
    </div>
  );
};

export default Home;
