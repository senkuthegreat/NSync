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
  const [activePhoneTab, setActivePhoneTab] = useState('attendance');
  const [activeDay, setActiveDay] = useState('Wed');
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
            <h1 className="hero-main-title">Academic Life, Seamlessly Synced.</h1>
            <p className="hero-main-subtitle">
              Attendance tracking with 75% criteria calculator, daily timetables,
              and exam hall seating — built exclusively for NSUT students.
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

          {/* Interactive Mobile Device Stage (Flagship Phone + Live Flanking Widgets) */}
          <div id="showcase" className="hero-device-stage">
            {/* Interactive Screen Selector Tabs */}
            <div className="stage-tabs-pill">
              <button
                className={`stage-tab-btn ${activePhoneTab === 'attendance' ? 'active' : ''}`}
                onClick={() => setActivePhoneTab('attendance')}
              >
                <Percent size={14} />
                <span>Attendance</span>
              </button>
              <button
                className={`stage-tab-btn ${activePhoneTab === 'schedule' ? 'active' : ''}`}
                onClick={() => setActivePhoneTab('schedule')}
              >
                <Clock size={14} />
                <span>Schedule</span>
              </button>
              <button
                className={`stage-tab-btn ${activePhoneTab === 'exams' ? 'active' : ''}`}
                onClick={() => setActivePhoneTab('exams')}
              >
                <MapPin size={14} />
                <span>Exam Seating</span>
              </button>
              <button
                className={`stage-tab-btn ${activePhoneTab === 'login' ? 'active' : ''}`}
                onClick={() => setActivePhoneTab('login')}
              >
                <ShieldCheck size={14} />
                <span>IMS Portal</span>
              </button>
            </div>

            {/* Stage Centerpiece: Central Phone + Flanking Floating Glass Widgets */}
            <div className="stage-centerpiece">
              {/* Left Flanking Glass Card: Attendance Guard */}
              <div className="flanking-card left-card">
                <div className="flank-badge safe">
                  <Percent size={12} />
                  <span>75% Attendance Guard</span>
                </div>
                <div className="flank-stat">
                  <span className="flank-number">83.3%</span>
                  <span className="flank-tag safe">Safe</span>
                </div>
                <p className="flank-desc">
                  You can safely skip <strong>4 lecture(s)</strong> above the 75% threshold.
                </p>
                <div className="flank-mini-bars">
                  <div className="mini-bar-row">
                    <span>Compiler Design</span>
                    <span className="mini-val">91.2%</span>
                  </div>
                  <div className="mini-track">
                    <div className="mini-fill" style={{ width: '91.2%' }}></div>
                  </div>
                </div>
              </div>

              {/* Central Titanium Flagship Smartphone */}
              <div className="device-phone">
                <div className="device-screen">
                  {/* Status Bar with Dynamic Island */}
                  <div className="phone-status-bar">
                    <span>09:41</span>
                    <div className="phone-island"></div>
                    <span>5G • 100%</span>
                  </div>

                  {/* In-App Header */}
                  <div className="phone-app-header">
                    <div className="phone-student-info">
                      <h4>Hey, Senku 👋</h4>
                      <p>COE • 6th Semester • NSUT</p>
                    </div>
                    <div className="phone-sync-pill">
                      <span className="phone-sync-dot"></span>
                      <span>Synced</span>
                    </div>
                  </div>

                  {/* Dynamic Tab Screen Container */}
                  <div className="phone-content-area">
                    {/* Screen 1: Attendance Guardian */}
                    {activePhoneTab === 'attendance' && (
                      <div className="phone-tab-screen fade-in">
                        <div className="phone-gauge-card">
                          <div className="phone-circular-gauge">
                            <svg className="phone-gauge-svg" viewBox="0 0 100 100">
                              <circle className="gauge-bg" cx="50" cy="50" r="40" />
                              <circle
                                className="gauge-progress"
                                cx="50"
                                cy="50"
                                r="40"
                                style={{ strokeDashoffset: '42' }}
                              />
                            </svg>
                            <div className="gauge-center-text">
                              <span className="gauge-big-num">83.3%</span>
                              <span className="gauge-status-badge">SAFE</span>
                            </div>
                          </div>
                          <p className="gauge-advice-text">
                            You can safely skip <strong>4 lecture(s)</strong> above 75%.
                          </p>
                        </div>

                        <div className="phone-subject-list">
                          <div className="phone-subject-item">
                            <div className="subj-title-row">
                              <span>Compiler Design</span>
                              <span className="subj-pct safe">91.2%</span>
                            </div>
                            <div className="subj-bar-track">
                              <div className="subj-bar-fill safe" style={{ width: '91.2%' }}></div>
                            </div>
                          </div>

                          <div className="phone-subject-item">
                            <div className="subj-title-row">
                              <span>Computer Networks</span>
                              <span className="subj-pct safe">85.7%</span>
                            </div>
                            <div className="subj-bar-track">
                              <div className="subj-bar-fill safe" style={{ width: '85.7%' }}></div>
                            </div>
                          </div>

                          <div className="phone-subject-item">
                            <div className="subj-title-row">
                              <span>Machine Learning</span>
                              <span className="subj-pct warn">76.0%</span>
                            </div>
                            <div className="subj-bar-track">
                              <div className="subj-bar-fill warn" style={{ width: '76.0%' }}></div>
                            </div>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Screen 2: Today's Schedule */}
                    {activePhoneTab === 'schedule' && (
                      <div className="phone-tab-screen fade-in">
                        <div className="phone-day-tabs">
                          {['Mon', 'Tue', 'Wed', 'Thu', 'Fri'].map((day) => (
                            <button
                              key={day}
                              className={`day-tab-pill ${activeDay === day ? 'active' : ''}`}
                              onClick={() => setActiveDay(day)}
                            >
                              {day}
                            </button>
                          ))}
                        </div>

                        <div className="phone-schedule-list">
                          <div className="phone-schedule-card active-now">
                            <div className="sched-time-badge">
                              <Clock size={11} />
                              <span>09:00 - 10:00 AM</span>
                            </div>
                            <h5>Cloud Computing (L)</h5>
                            <div className="sched-meta-loc">
                              <MapPin size={11} />
                              <span>Hall: APJ-102 • Dr. Verma</span>
                            </div>
                          </div>

                          <div className="phone-schedule-card">
                            <div className="sched-time-badge">
                              <Clock size={11} />
                              <span>10:00 - 11:00 AM</span>
                            </div>
                            <h5>Distributed Systems</h5>
                            <div className="sched-meta-loc">
                              <MapPin size={11} />
                              <span>Hall: APJ-104 • Prof. Gupta</span>
                            </div>
                          </div>

                          <div className="phone-schedule-card">
                            <div className="sched-time-badge">
                              <Clock size={11} />
                              <span>11:15 - 01:15 PM</span>
                            </div>
                            <h5>Networks Lab (P2)</h5>
                            <div className="sched-meta-loc">
                              <MapPin size={11} />
                              <span>Computer Centre • Lab 4</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Screen 3: Exam Seating */}
                    {activePhoneTab === 'exams' && (
                      <div className="phone-tab-screen fade-in">
                        <div className="phone-exam-search">
                          <Search size={13} />
                          <span>COE-601 Compiler Design</span>
                        </div>

                        <div className="phone-exam-list">
                          <div className="phone-ticket-card">
                            <div className="ticket-date-chip">18 NOV • 10:00 AM</div>
                            <h5>Mid-Term: Artificial Intelligence</h5>
                            <div className="ticket-seat-grid">
                              <div className="seat-cell">
                                <span className="cell-label">Room</span>
                                <span className="cell-value">SPS-04</span>
                              </div>
                              <div className="seat-cell">
                                <span className="cell-label">Row</span>
                                <span className="cell-value">B</span>
                              </div>
                              <div className="seat-cell highlight">
                                <span className="cell-label">Desk</span>
                                <span className="cell-value">14</span>
                              </div>
                            </div>
                          </div>

                          <div className="phone-ticket-card">
                            <div className="ticket-date-chip">21 NOV • 02:00 PM</div>
                            <h5>Mid-Term: Compiler Design</h5>
                            <div className="ticket-seat-grid">
                              <div className="seat-cell">
                                <span className="cell-label">Room</span>
                                <span className="cell-value">APJ-201</span>
                              </div>
                              <div className="seat-cell">
                                <span className="cell-label">Row</span>
                                <span className="cell-value">C</span>
                              </div>
                              <div className="seat-cell highlight">
                                <span className="cell-label">Desk</span>
                                <span className="cell-value">08</span>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Screen 4: IMS Login */}
                    {activePhoneTab === 'login' && (
                      <div className="phone-tab-screen fade-in">
                        <div className="phone-login-card">
                          <div className="login-logo-mini">
                            <img src="./assets/logo.png" alt="NSync" />
                            <span>IMS Authentication</span>
                          </div>

                          <div className="login-mock-field">
                            <label>Roll Number / User ID</label>
                            <div className="mock-input-box">2024UCO1500</div>
                          </div>

                          <div className="login-mock-field">
                            <label>IMS Password</label>
                            <div className="mock-input-box">••••••••••••</div>
                          </div>

                          <div className="login-mock-captcha">
                            <div className="captcha-preview-box">
                              <span>4 4 8 9 5</span>
                            </div>
                            <div className="captcha-input-box">44895</div>
                          </div>

                          <div className="login-mock-btn">
                            Sign In & Sync
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Right Flanking Glass Card: Today's Schedule */}
              <div className="flanking-card right-card">
                <div className="flank-badge schedule">
                  <Clock size={12} />
                  <span>Next Lecture Up</span>
                </div>
                <div className="flank-stat">
                  <span className="flank-class-title">Cloud Computing (L)</span>
                </div>
                <div className="flank-loc-row">
                  <MapPin size={13} />
                  <span>Hall APJ-102 • 09:00 AM</span>
                </div>
                <div className="flank-countdown-pill">
                  <span className="pulse-dot"></span>
                  <span>Starts in 15 minutes</span>
                </div>
                <div className="flank-offline-note">
                  <WifiOff size={12} />
                  <span>Available 100% offline in basements</span>
                </div>
              </div>
            </div>
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
