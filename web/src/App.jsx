import React, { useState, useEffect, useRef } from 'react';
import {
  Download,
  ShieldCheck,
  Percent,
  LayoutGrid,
  WifiOff,
  Clock,
  Award,
  CalendarDays,
  Ticket,
  Globe,
  Mail
} from 'lucide-react';

// Direct edge CDN download hosted directly on the site (Zero GitHub dependence)
const DIRECT_DOWNLOAD_URL = './downloads/NSync.apk';

// Dedicated high-resolution artwork: Pink Sunset Clouds (matching reference)
const HERO_ARTWORK = './assets/artworks/sunset-clouds.jpg';

// Developer Profile & Social Data
const DEVELOPER_INFO = {
  name: 'Rohit Dahiya',
  handle: '@senkuthegreat',
  avatar: 'https://avatars.githubusercontent.com/u/126081063?v=4',
  portfolio: 'https://senkuthegreat.me',
  github: 'https://github.com/senkuthegreat',
  linkedin: 'https://www.linkedin.com/in/rohit-dahiya-14b180240/',
  instagram: 'https://instagram.com/senkuthegreat',
  email: 'mailto:rohitdahiya245@gmail.com'
};

function GithubIcon({ size = 16, className = '' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className}>
      <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"/>
    </svg>
  );
}

function LinkedinIcon({ size = 16, className = '' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className}>
      <path d="M19 3a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h14m-.5 15.5v-5.3a3.26 3.26 0 0 0-3.26-3.26c-.85 0-1.84.52-2.28 1.3v-1.11h-2.79v8.37h2.79v-4.93c0-.77.62-1.4 1.39-1.4a1.4 1.4 0 0 1 1.4 1.4v4.93h2.75M6.46 10.9h2.79v8.37H6.46v-8.37M7.86 6.54a1.63 1.63 0 1 0 0 3.26 1.63 1.63 0 0 0 0-3.26z"/>
    </svg>
  );
}

function InstagramIcon({ size = 16, className = '' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" className={className}>
      <path d="M12 2.163c3.204 0 3.584.012 4.85.07 3.252.148 4.771 1.691 4.919 4.919.058 1.265.069 1.645.069 4.849 0 3.205-.012 3.584-.069 4.849-.149 3.225-1.664 4.771-4.919 4.919-1.266.058-1.644.07-4.85.07-3.204 0-3.584-.012-4.849-.07-3.26-.149-4.771-1.699-4.919-4.92-.058-1.265-.07-1.644-.07-4.849 0-3.204.013-3.583.07-4.849.149-3.227 1.664-4.771 4.919-4.919 1.266-.057 1.645-.069 4.849-.069zm0-2.163c-3.259 0-3.667.014-4.947.072-4.358.2-6.78 2.618-6.98 6.98-.059 1.281-.073 1.689-.073 4.948 0 3.259.014 3.668.072 4.948.2 4.358 2.618 6.78 6.98 6.98 1.281.058 1.689.072 4.948.072 3.259 0 3.668-.014 4.948-.072 4.354-.2 6.782-2.618 6.979-6.98.059-1.28.073-1.689.073-4.948 0-3.259-.014-3.667-.072-4.947-.196-4.354-2.617-6.78-6.979-6.98-1.281-.059-1.69-.073-4.949-.073zm0 5.838c-3.403 0-6.162 2.759-6.162 6.162s2.759 6.163 6.162 6.163 6.162-2.759 6.162-6.163c0-3.403-2.759-6.162-6.162-6.162zm0 10.162c-2.209 0-4-1.79-4-4 0-2.209 1.791-4 4-4s4 1.791 4 4c0 2.21-1.791 4-4 4zm6.406-11.845c-.796 0-1.441.645-1.441 1.44s.645 1.44 1.441 1.44c.795 0 1.439-.645 1.439-1.44s-.644-1.44-1.439-1.44z"/>
    </svg>
  );
}

export default function App() {
  const [scrolled, setScrolled] = useState(false);
  const [contactOpen, setContactOpen] = useState(false);
  const contactRef = useRef(null);

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Click outside and Escape key handler for Contact popover
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (contactRef.current && !contactRef.current.contains(event.target)) {
        setContactOpen(false);
      }
    };
    const handleEscape = (event) => {
      if (event.key === 'Escape') {
        setContactOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleEscape);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
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

          {/* Top Clean Header: Logo on Left, Contact Pill + Download Pill on Right */}
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
              {/* Clean "Contact" Pill with Interactive Glass Dropdown */}
              <div className="nav-contact-wrapper" ref={contactRef}>
                <button
                  type="button"
                  onClick={() => setContactOpen(!contactOpen)}
                  className={`nav-btn-contact ${contactOpen ? 'active' : ''}`}
                  aria-expanded={contactOpen}
                  aria-label="Contact Developer"
                >
                  <Mail size={15} />
                  <span>Contact</span>
                </button>

                {contactOpen && (
                  <div className="contact-dropdown-card">
                    <div className="contact-popover-header">
                      <img
                        src={DEVELOPER_INFO.avatar}
                        alt={DEVELOPER_INFO.name}
                        className="contact-popover-avatar"
                      />
                      <div className="contact-popover-meta">
                        <h4>{DEVELOPER_INFO.name}</h4>
                        <span className="contact-popover-handle">{DEVELOPER_INFO.handle}</span>
                      </div>
                    </div>

                    <div className="contact-popover-divider"></div>

                    <div className="contact-popover-links">
                      <a
                        href={DEVELOPER_INFO.portfolio}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="popover-link-item portfolio"
                      >
                        <div className="popover-link-icon">
                          <Globe size={16} />
                        </div>
                        <div className="popover-link-text">
                          <span className="title">Portfolio</span>
                          <span className="sub">senkuthegreat.me</span>
                        </div>
                      </a>

                      <a
                        href={DEVELOPER_INFO.github}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="popover-link-item github"
                      >
                        <div className="popover-link-icon">
                          <GithubIcon size={16} />
                        </div>
                        <div className="popover-link-text">
                          <span className="title">GitHub</span>
                          <span className="sub">@senkuthegreat</span>
                        </div>
                      </a>

                      <a
                        href={DEVELOPER_INFO.linkedin}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="popover-link-item linkedin"
                      >
                        <div className="popover-link-icon">
                          <LinkedinIcon size={16} />
                        </div>
                        <div className="popover-link-text">
                          <span className="title">LinkedIn</span>
                          <span className="sub">Rohit Dahiya</span>
                        </div>
                      </a>

                      <a
                        href={DEVELOPER_INFO.instagram}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="popover-link-item instagram"
                      >
                        <div className="popover-link-icon">
                          <InstagramIcon size={16} />
                        </div>
                        <div className="popover-link-text">
                          <span className="title">Instagram</span>
                          <span className="sub">@senkuthegreat</span>
                        </div>
                      </a>

                      <a
                        href={DEVELOPER_INFO.email}
                        className="popover-link-item email"
                      >
                        <div className="popover-link-icon">
                          <Mail size={16} />
                        </div>
                        <div className="popover-link-text">
                          <span className="title">Email</span>
                          <span className="sub">rohitdahiya245@gmail.com</span>
                        </div>
                      </a>
                    </div>
                  </div>
                )}
              </div>

              {/* Download Pill in Header */}
              <a
                href={DIRECT_DOWNLOAD_URL}
                download="NSync.apk"
                className="nav-btn-pill"
              >
                <Download size={15} />
                <span>Download</span>
              </a>
            </div>
          </header>

          {/* Hero Headlines & Single Prominent CTA */}
          <div className="hero-hero-content">
            <h1 className="hero-main-title">Academic Life, Seamlessly Synced.</h1>
            <p className="hero-main-subtitle">
              Smart 75% attendance calculator, daily timetables, semester results,
              academic calendar, and exam seating — crafted for NSUT.
            </p>

            <div className="hero-cta-wrapper">
              <a
                href={DIRECT_DOWNLOAD_URL}
                download="NSync.apk"
                className="hero-get-started-btn"
              >
                <span>Download for Android</span>
                <Download size={17} />
              </a>
              <div style={{ marginTop: '10px', fontSize: '13px', color: 'rgba(255, 255, 255, 0.9)', fontWeight: 500 }}>
                <span>v1.0.44 • Latest Release</span>
              </div>
            </div>
          </div>
        </div>

        {/* Section 2: Features Bento Grid (Clean Light SaaS Palette) */}
        <section id="features" className="content-section features-area">
          <div className="section-title-wrap">
            <span className="section-pill">Intelligence & Design</span>
            <h2>Engineered for zero friction.</h2>
            <p>
              All your college vitals in one fast, captcha-free app.
            </p>
            <div className="mobile-swipe-hint">
              <span>Swipe cards ➔</span>
            </div>
          </div>

          <div className="features-grid">
            <div className="feature-box wide">
              <div className="icon-badge">
                <ShieldCheck size={24} />
              </div>
              <h3>Persistent IMS Sessions</h3>
              <p>
                Log in once. Your session stays cached securely on-device with
                zero repeated logins or captchas.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <Percent size={24} />
              </div>
              <h3>Smart 75% Projections</h3>
              <p>
                Real-time projection showing exact <code>+X Bunks Safe</code> or <code>Need X Classes</code> per subject.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <Clock size={24} />
              </div>
              <h3>Daily Timetables & Cal</h3>
              <p>
                Period timings, room numbers, and 1-tap export to Google Calendar with reminders.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <Award size={24} />
              </div>
              <h3>Results & CGPA Tracker</h3>
              <p>
                Semester SGPA breakdowns, cumulative CGPA tracking, and grade sheets delivered instantly.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <CalendarDays size={24} />
              </div>
              <h3>Official Academic Calendar</h3>
              <p>
                Track mid-terms, end-sems, official holidays, and semester breaks in one place.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <Ticket size={24} />
              </div>
              <h3>Admit Card & Exam Seating</h3>
              <p>
                Instant roll number-wise hall allocations, desk numbers, and exam notices.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <LayoutGrid size={24} />
              </div>
              <h3>Widget Studio</h3>
              <p>
                Glanceable Android home screen widgets for your next class and live attendance.
              </p>
            </div>

            <div className="feature-box">
              <div className="icon-badge">
                <WifiOff size={24} />
              </div>
              <h3>100% Offline Architecture</h3>
              <p>
                Local database caching keeps your timetable and attendance accessible with zero signal.
              </p>
            </div>
          </div>
        </section>

        {/* Section 3: Setup Guide */}
        <section id="setup" className="content-section setup-area">
          <div className="section-title-wrap">
            <span className="section-pill">Installation</span>
            <h2>Up and running in seconds.</h2>
            <p>Compatible with Android 7.0 and newer.</p>
            <div className="mobile-swipe-hint">
              <span>Swipe steps ➔</span>
            </div>
          </div>

          <div className="setup-steps-row">
            <div className="setup-step-card">
              <div className="step-badge">01</div>
              <h4>Download Package</h4>
              <p>
                Grab <code>NSync.apk</code> directly from this page with a single tap.
              </p>
            </div>

            <div className="setup-step-card">
              <div className="step-badge">02</div>
              <h4>Approve Installation</h4>
              <p>
                Open the APK and confirm install. 100% ad-free and tracker-free.
              </p>
            </div>

            <div className="setup-step-card">
              <div className="step-badge">03</div>
              <h4>Instant Sync</h4>
              <p>
                Sign in once with IMS credentials. All your data syncs automatically.
              </p>
            </div>
          </div>
        </section>

        {/* Section 4: Final Download Banner */}
        <section className="cta-banner-wrapper">
          <div className="cta-banner-card">
            <h2>Experience NSync today.</h2>
            <p>
              Fast, offline-first, and private — built exclusively for NSUT students.
            </p>
            <div className="cta-dual-actions">
              <a
                href={DIRECT_DOWNLOAD_URL}
                download="NSync.apk"
                className="hero-get-started-btn"
              >
                <span>Download for Android</span>
                <Download size={18} />
              </a>
            </div>
          </div>
        </section>

        {/* Comprehensive Developer & Contact Showcase + Footer */}
        <footer className="canvas-footer-area">
          <div className="developer-contact-card">
            <div className="dev-card-left">
              <div className="dev-avatar-container">
                <img
                  src={DEVELOPER_INFO.avatar}
                  alt={DEVELOPER_INFO.name}
                  className="dev-avatar-img"
                />
                <span className="dev-active-indicator" title="Active"></span>
              </div>
              <div className="dev-meta-content">
                <span className="dev-badge-tag">Developer & Architecture</span>
                <h3 className="dev-name-title">
                  {DEVELOPER_INFO.name} <span className="dev-handle-inline">({DEVELOPER_INFO.handle})</span>
                </h3>
                <p className="dev-tagline">
                  Crafted with care for NSUT. Connect, share feedback, or explore more work.
                </p>
              </div>
            </div>

            <div className="dev-links-container">
              <a
                href={DEVELOPER_INFO.portfolio}
                target="_blank"
                rel="noopener noreferrer"
                className="contact-pill-btn portfolio"
              >
                <Globe size={16} />
                <span>Portfolio</span>
              </a>

              <a
                href={DEVELOPER_INFO.github}
                target="_blank"
                rel="noopener noreferrer"
                className="contact-pill-btn github"
              >
                <GithubIcon size={16} />
                <span>GitHub</span>
              </a>

              <a
                href={DEVELOPER_INFO.linkedin}
                target="_blank"
                rel="noopener noreferrer"
                className="contact-pill-btn linkedin"
              >
                <LinkedinIcon size={16} />
                <span>LinkedIn</span>
              </a>

              <a
                href={DEVELOPER_INFO.instagram}
                target="_blank"
                rel="noopener noreferrer"
                className="contact-pill-btn instagram"
              >
                <InstagramIcon size={16} />
                <span>Instagram</span>
              </a>

              <a
                href={DEVELOPER_INFO.email}
                className="contact-pill-btn email"
              >
                <Mail size={16} />
                <span>Email</span>
              </a>
            </div>
          </div>

          <div className="canvas-footer-bottom">
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
                Student companion for NSUT. Not officially affiliated with university administration.
              </p>
            </div>

            <div className="footer-right">
              <a href="#features">Features</a>
              <a href="#setup">Setup</a>
              <a href={DIRECT_DOWNLOAD_URL} download="NSync.apk">
                Download APK
              </a>
            </div>
          </div>
        </footer>
      </div>
    </div>
  );
}
