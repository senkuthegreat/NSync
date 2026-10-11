import React, { useState, useEffect } from 'react';
import {
  Download,
  ShieldCheck,
  Percent,
  LayoutGrid,
  WifiOff,
  Sparkles,
  Search,
  Plus,
  Clock,
  MapPin,
  Calendar,
  Layers
} from 'lucide-react';

// Direct package download hosted locally on the site CDN (Zero GitHub links exposed)
const LATEST_DOWNLOAD_URL = './downloads/NSync.apk';

// Dedicated high-resolution artwork: Pink Sunset Clouds (matching reference)
const HERO_ARTWORK = './assets/artworks/sunset-clouds.jpg';

export default function App() {
  const [activeSidebarTab, setActiveSidebarTab] = useState('attendance');
  const [scrolled, setScrolled] = useState(false);

  // Header scroll state
  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="page-wrapper">
      {/* Outer Floating Device Window Container (Matching Reference Aesthetic) */}
      <div className="canvas-frame">
        {/* Hero Section with Immersive Artwork Background */}
        <div
          className="hero-container"
          style={{ backgroundImage: `url(${HERO_ARTWORK})` }}
        >
          {/* Subtle Contrast Gradient Overlay for Typography Clarity */}
          <div className="hero-gradient-overlay"></div>

          {/* Top Glass Navigation Bar */}
          <header className={`hero-nav ${scrolled ? 'nav-scrolled' : ''}`}>
            <a href="#" className="brand-group">
              <img
                src="./assets/logo.png"
                alt="NSync Logo"
                className="brand-logo"
              />
              <span className="brand-title">
                N<span>Sync</span>
              </span>
            </a>

            <nav className="nav-links">
              <a href="#dashboard">Dashboard</a>
              <a href="#features">Features</a>
              <a href="#setup">Setup</a>
              <a href="#overview">Overview</a>
            </nav>

            <div className="nav-actions">
              <a
                href={LATEST_DOWNLOAD_URL}
                download="NSync.apk"
                className="nav-btn-pill"
              >
                <Download size={15} />
                <span>Download</span>
              </a>
            </div>
          </header>

          {/* Hero Headlines & CTA */}
          <div className="hero-hero-content">
            <h1 className="hero-main-title">From Chaos to Clarity</h1>
            <p className="hero-main-subtitle">
              Manage projects and academics effortlessly. Track attendance with smart bunk
              projections, live timetable sync, and exam hall seating.
            </p>

            <div className="hero-cta-wrapper">
              <a
                href={LATEST_DOWNLOAD_URL}
                download="NSync.apk"
                className="hero-get-started-btn"
              >
                <span>Download for Android</span>
                <Download size={17} />
              </a>
            </div>
          </div>

          {/* Floating Application Dashboard Card (Protruding from Hero into Content) */}
          <div id="dashboard" className="floating-dashboard-card">
            {/* Left Sidebar */}
            <aside className="dash-sidebar">
              <div className="dash-sidebar-header">
                <span className="dash-team-title">My Campus</span>
              </div>

              <nav className="dash-nav-list">
                <button
                  className={`dash-nav-item ${
                    activeSidebarTab === 'attendance' ? 'active' : ''
                  }`}
                  onClick={() => setActiveSidebarTab('attendance')}
                >
                  <Layers size={16} />
                  <span>Dashboard</span>
                </button>

                <button
                  className={`dash-nav-item ${
                    activeSidebarTab === 'timetable' ? 'active' : ''
                  }`}
                  onClick={() => setActiveSidebarTab('timetable')}
                >
                  <Clock size={16} />
                  <span>Timeline</span>
                </button>

                <button
                  className={`dash-nav-item ${
                    activeSidebarTab === 'exams' ? 'active' : ''
                  }`}
                  onClick={() => setActiveSidebarTab('exams')}
                >
                  <Calendar size={16} />
                  <span>Exams & Halls</span>
                </button>

                <button
                  className={`dash-nav-item ${
                    activeSidebarTab === 'preferences' ? 'active' : ''
                  }`}
                  onClick={() => setActiveSidebarTab('preferences')}
                >
                  <ShieldCheck size={16} />
                  <span>IMS Portal</span>
                </button>
              </nav>

              <div className="dash-sidebar-footer">
                <div className="offline-indicator">
                  <span className="pulse-dot"></span>
                  <span>Offline Ready</span>
                </div>
              </div>
            </aside>

            {/* Main Board Content */}
            <main className="dash-main-board">
              {/* Board Header */}
              <div className="board-header">
                <div className="board-title-group">
                  <h3>Academic Overview</h3>
                  <div className="board-avatar-stack">
                    <span className="avatar-chip">COE</span>
                    <span className="avatar-chip accent">6th Sem</span>
                    <span className="avatar-count">NSUT</span>
                  </div>
                </div>

                <div className="board-search-group">
                  <div className="board-search-bar">
                    <Search size={15} />
                    <input
                      type="text"
                      placeholder="Search subject, hall..."
                      readOnly
                      value="COE-601 Compiler Design"
                    />
                  </div>
                  <button className="board-add-btn" title="Add quick task">
                    <Plus size={16} />
                  </button>
                </div>
              </div>

              {/* 3 Pastel Overview Columns (Matching Reference Card Style) */}
              <div className="board-columns-grid">
                {/* Column 1: Attendance Projections (Pastel Rose Tint) */}
                <div className="board-col col-rose">
                  <div className="col-header">
                    <span className="col-title">Attendance</span>
                    <span className="col-count">5 Subjects</span>
                  </div>

                  <div className="board-card rose-card">
                    <div className="card-top-row">
                      <span className="card-item-title">Overall Status</span>
                      <span className="badge-safe">83.3% SAFE</span>
                    </div>

                    <div className="circular-gauge-container">
                      <svg className="gauge-svg" viewBox="0 0 100 100">
                        <circle className="gauge-bg" cx="50" cy="50" r="40" />
                        <circle
                          className="gauge-progress"
                          cx="50"
                          cy="50"
                          r="40"
                          style={{ strokeDashoffset: '42' }}
                        />
                      </svg>
                      <div className="gauge-inner">
                        <span className="gauge-pct">83.3%</span>
                        <span className="gauge-sub">Required: 75%</span>
                      </div>
                    </div>

                    <p className="bunk-advice">
                      You can safely skip <strong>4 lecture(s)</strong> above
                      the 75% threshold.
                    </p>

                    <div className="sub-breakdown-list">
                      <div className="sub-row">
                        <span>Compiler Design</span>
                        <span className="sub-pct">91.2%</span>
                      </div>
                      <div className="sub-row">
                        <span>Computer Networks</span>
                        <span className="sub-pct">85.7%</span>
                      </div>
                      <div className="sub-row warning">
                        <span>Machine Learning</span>
                        <span className="sub-pct">76.0%</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Column 2: Today's Schedule (Pastel Amber/Sand Tint) */}
                <div className="board-col col-amber">
                  <div className="col-header">
                    <span className="col-title">Today's Schedule</span>
                    <span className="col-count">Wed • 3 Classes</span>
                  </div>

                  <div className="board-card amber-card">
                    <div className="schedule-entry">
                      <div className="entry-time">
                        <Clock size={13} />
                        <span>09:00 - 10:00 AM</span>
                      </div>
                      <div className="entry-title">Cloud Computing (L)</div>
                      <div className="entry-meta">
                        <MapPin size={12} />
                        <span>Hall: APJ-102 • Dr. Verma</span>
                      </div>
                    </div>

                    <div className="entry-divider"></div>

                    <div className="schedule-entry">
                      <div className="entry-time">
                        <Clock size={13} />
                        <span>10:00 - 11:00 AM</span>
                      </div>
                      <div className="entry-title">Distributed Systems</div>
                      <div className="entry-meta">
                        <MapPin size={12} />
                        <span>Hall: APJ-104 • Prof. Gupta</span>
                      </div>
                    </div>

                    <div className="entry-divider"></div>

                    <div className="schedule-entry">
                      <div className="entry-time">
                        <Clock size={13} />
                        <span>11:15 - 01:15 PM</span>
                      </div>
                      <div className="entry-title">Networks Lab (P2)</div>
                      <div className="entry-meta">
                        <MapPin size={12} />
                        <span>Computer Centre • Lab 4</span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Column 3: Exams & Hall Tickets (Pastel Sky/Mint Tint) */}
                <div className="board-col col-sky">
                  <div className="col-header">
                    <span className="col-title">Exam Seating</span>
                    <span className="col-count">3 Upcoming</span>
                  </div>

                  <div className="board-card sky-card">
                    <div className="exam-ticket">
                      <div className="ticket-date">18 NOV 2026 • 10:00 AM</div>
                      <div className="ticket-subject">
                        Mid-Term: Artificial Intelligence
                      </div>
                      <div className="ticket-seating">
                        <span>Room: SPS-04</span>
                        <span>Row: B • Desk: 14</span>
                      </div>
                    </div>

                    <div className="entry-divider"></div>

                    <div className="exam-ticket">
                      <div className="ticket-date">21 NOV 2026 • 02:00 PM</div>
                      <div className="ticket-subject">
                        Mid-Term: Compiler Design
                      </div>
                      <div className="ticket-seating">
                        <span>Room: APJ-201</span>
                        <span>Row: C • Desk: 08</span>
                      </div>
                    </div>

                    <div className="entry-divider"></div>

                    <div className="exam-ticket">
                      <div className="ticket-date">24 NOV 2026 • 10:00 AM</div>
                      <div className="ticket-subject">
                        Mid-Term: Operating Systems
                      </div>
                      <div className="ticket-seating">
                        <span>Room: SPS-12</span>
                        <span>Row: A • Desk: 02</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </main>
          </div>
        </div>

        {/* Section 2: Features Bento Grid (Clean Light SaaS Palette) */}
        <section id="features" className="content-section features-area">
          <div className="section-title-wrap">
            <span className="section-pill">Intelligence & Design</span>
            <h2>Engineered to eliminate friction.</h2>
            <p>
              Everything you need to navigate your college lifecycle with zero
              stress and zero captcha friction.
            </p>
          </div>

          <div className="features-grid">
            <div className="feature-box wide">
              <div className="icon-badge">
                <ShieldCheck size={24} />
              </div>
              <h3>Persistent IMS Sessions</h3>
              <p>
                Log in once with official captcha verification. Your session is
                safely preserved on the backend so you never have to re-enter
                captchas when refreshing your data or moving between screens.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <Percent size={24} />
              </div>
              <h3>Smart 75% Projections</h3>
              <p>
                Real-time safety calculations show exactly how many classes you
                can safely skip or need to attend to protect your eligibility.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <LayoutGrid size={24} />
              </div>
              <h3>Glance Widgets</h3>
              <p>
                Check your next lecture hall and live attendance directly from
                your Android home screen without opening the app.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <WifiOff size={24} />
              </div>
              <h3>100% Offline Architecture</h3>
              <p>
                Full local database caching ensures your timetable and syllabus
                are available instantly, even in basements with zero cell reception.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <Sparkles size={24} />
              </div>
              <h3>Direct Fast Updates</h3>
              <p>
                One-tap fast downloads ensure you always stay in sync with the
                latest verified university timetable changes.
              </p>
            </div>
          </div>
        </section>

        {/* Section 3: Setup Guide */}
        <section id="setup" className="content-section setup-area">
          <div className="section-title-wrap">
            <span className="section-pill">Installation</span>
            <h2>Up and running in moments.</h2>
            <p>Ready for any Android device running Android 7.0 or newer.</p>
          </div>

          <div className="setup-steps-row">
            <div className="setup-step-card">
              <div className="step-badge">01</div>
              <h4>Download Package</h4>
              <p>
                Download the official <code>NSync.apk</code> package directly
                from this website with one tap.
              </p>
            </div>

            <div className="setup-step-card">
              <div className="step-badge">02</div>
              <h4>Approve Installation</h4>
              <p>
                Open the file and allow install when prompted. Zero ads, zero
                third-party trackers.
              </p>
            </div>

            <div className="setup-step-card">
              <div className="step-badge">03</div>
              <h4>Instant Sync</h4>
              <p>
                Sign in once with your IMS credentials. Your attendance and
                timetable sync automatically.
              </p>
            </div>
          </div>
        </section>

        {/* Section 4: Final Download Banner */}
        <section className="cta-banner-wrapper">
          <div className="cta-banner-card">
            <h2>Experience NSync today.</h2>
            <p>
              Handcrafted exclusively for students of Netaji Subhas University of
              Technology.
            </p>
            <a
              href={LATEST_DOWNLOAD_URL}
              download="NSync.apk"
              className="hero-get-started-btn"
              style={{ margin: '0 auto' }}
            >
              <span>Download for Android</span>
              <Download size={18} />
            </a>
          </div>
        </section>

        {/* Clean, Professional Footer (Zero GitHub References) */}
        <footer className="canvas-footer">
          <div className="footer-left">
            <div className="brand-group">
              <img
                src="./assets/logo.png"
                alt="NSync Logo"
                className="brand-logo small"
              />
              <span className="brand-title">
                N<span>Sync</span>
              </span>
            </div>
            <p className="footer-disclaimer">
              Student companion for NSUT. Not officially affiliated with
              university administration.
            </p>
          </div>

          <div className="footer-right">
            <a href="#dashboard">Dashboard</a>
            <a href="#features">Features</a>
            <a href="#setup">Setup</a>
            <a href={LATEST_DOWNLOAD_URL} download="NSync.apk">
              Download APK
            </a>
          </div>
        </footer>
      </div>
    </div>
  );
}
